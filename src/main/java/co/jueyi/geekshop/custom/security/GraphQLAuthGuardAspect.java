package co.jueyi.geekshop.custom.security;

import co.jueyi.geekshop.common.ApiType;
import co.jueyi.geekshop.common.Constant;
import co.jueyi.geekshop.common.RequestContext;
import co.jueyi.geekshop.config.session_cache.CachedSession;
import co.jueyi.geekshop.config.session_cache.CachedSessionUser;
import co.jueyi.geekshop.custom.graphql.CustomGraphQLServletContext;
import co.jueyi.geekshop.exception.ErrorCode;
import co.jueyi.geekshop.exception.InternalServerError;
import co.jueyi.geekshop.exception.UnauthorizedException;
import co.jueyi.geekshop.service.ConfigService;
import co.jueyi.geekshop.service.SessionService;
import co.jueyi.geekshop.types.common.Permission;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Aspect
@Component
@Order(1) // 最先执行的切面
@RequiredArgsConstructor
public class GraphQLAuthGuardAspect {
    private final ConfigService configService;
    private final SessionService sessionService;

    @Before("allGraphQLResolverMethods() && isDefinedInApplication()")
    public void doAuthGuard(JoinPoint joinPoint) {
        DataFetchingEnvironment dfe = (DataFetchingEnvironment) Arrays.stream(joinPoint.getArgs())
                .filter(o -> o instanceof DataFetchingEnvironment).findFirst().orElse(null);
        if (dfe == null) {
            throw new InternalServerError(ErrorCode.NO_DATA_FETCHING_ENVIRONMENT);
        }

        CustomGraphQLServletContext graphQLServletContext = dfe.getContext();
        HttpServletRequest req = graphQLServletContext.getHttpServletRequest();
        HttpServletResponse res = graphQLServletContext.getHttpServletResponse();

        boolean authDisabled = this.configService.getAuthOptions().isDisableAuth();
        List<Permission> allowedPermissions = this.reflectAllowedPermissions(joinPoint);
        boolean isPublic = allowedPermissions.contains(Permission.Public);
        boolean hasOwnerPermission = allowedPermissions.contains(Permission.Owner);

        CachedSession session = this.getSession(req, res, hasOwnerPermission);

        // 构建RequestContext对象
        RequestContext ctx = new RequestContext();
        ctx.setSession(session);
        ctx.setApiType(this.reflectApiType(joinPoint));
        graphQLServletContext.setRequestContext(ctx);

        // 用于审计
        if (RequestContextHolder.getRequestAttributes() != null && session.getUser() != null) {
            RequestContextHolder.getRequestAttributes()
                    .setAttribute(Constant.REQUEST_ATTRIBUTE_CURRENT_USER,
                            session.getUser(), RequestAttributes.SCOPE_REQUEST);
        }

        if (authDisabled || allowedPermissions.size() == 0 || isPublic) {
            return; // pass
        }

        boolean isAuthorized = session.getUser() != null && isUserAuthorized(session.getUser(), allowedPermissions);
        ctx.setAuthorized(isAuthorized);
        if (RequestContextHolder.getRequestAttributes() != null) {
            if (!isAuthorized && hasOwnerPermission) {
                ctx.setAuthorizedAsOwnerOnly(true);
            } else {
                ctx.setAuthorizedAsOwnerOnly(false);
            }
        }

        if (!isAuthorized && !hasOwnerPermission) {
            throw new UnauthorizedException();
        }
    }

    private boolean isUserAuthorized(CachedSessionUser user, List<Permission> allowedPermissions) {
        return user.getPermissions().containsAll(allowedPermissions);
    }

    private ApiType reflectApiType(JoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        if (className.contains("." + ApiType.ADMIN.name().toLowerCase() + ".")) {
            return ApiType.ADMIN;
        } else if (className.contains("." + ApiType.SHOP.name().toLowerCase() + ".")) {
            return ApiType.SHOP;
        } else {
            return ApiType.CUSTOM;
        }
    }

    private List<Permission> reflectAllowedPermissions(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Allow allow =  method.getAnnotation(Allow.class);
        if (allow == null) return new ArrayList<>();
        return Arrays.asList(allow.value());
    }

    /**
     * Matches all beans that implement {@link graphql.kickstart.tools.GraphQLResolver}
     * note: {@code GraphQLMutationResolver}, {@code GraphQLQueryResolver} etc
     * extend base GraphQLResolver interface
     */
    @Pointcut("target(graphql.kickstart.tools.GraphQLResolver)")
    public void allGraphQLResolverMethods() {

    }

    /**
     * Matches all beans in co.jueyi.geekshop package
     * resolvers must be in this package (subpackages)
     */
    @Pointcut("within(co.jueyi.geekshop..*)")
    private void isDefinedInApplication() {

    }

    private CachedSession getSession(HttpServletRequest req, HttpServletResponse res, boolean hasOwnerPermission) {
        String sessionToken = SessionTokenHelper.extractSessionToken(
                req,
                this.configService.getAuthOptions().getTokenMethod()
        );
        CachedSession serializedSession = null;
        if (!StringUtils.isEmpty(sessionToken)) {
            serializedSession = this.sessionService.getSessionFromToken(sessionToken);
            if (serializedSession != null) return serializedSession;
            // if there is a token but it cannot be validated to a Session,
            // then the token is no longer valid and should be unset.
            SessionTokenHelper.setSessionToken(
                    "",
                    false,
                    this.configService.getAuthOptions(),
                    req,
                    res
            );
        }

        if (hasOwnerPermission && serializedSession == null) {
            serializedSession = this.sessionService.createAnonymousSession();
            SessionTokenHelper.setSessionToken(
                    serializedSession.getToken(),
                    true,
                    this.configService.getAuthOptions(),
                    req,
                    res
            );
        }
        return serializedSession;
    }
}