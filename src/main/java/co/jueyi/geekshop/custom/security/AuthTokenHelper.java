package co.jueyi.geekshop.custom.security;

import co.jueyi.geekshop.common.Constant;
import co.jueyi.geekshop.common.utils.TimeSpanUtil;
import co.jueyi.geekshop.properties.AuthOptions;
import co.jueyi.geekshop.properties.TokenMethod;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Get the session token from either the cookie or the Authorization header, depending
 * on the configured tokenMethod.
 *
 * Created on Nov, 2020 by @author bobo
 */
public abstract class AuthTokenHelper {
    private static String patternString = "bearer\\s+(.+)$";
    private static Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);

    public static String extractAuthToken(HttpServletRequest req, TokenMethod tokenMethod) {
        if (tokenMethod == TokenMethod.cookie) {
            Cookie cookie = WebUtils.getCookie(req, Constant.COOKIE_NAME_TOKEN);
            if (cookie == null) return null;
            return cookie.getValue();
        } else {
            final String authHeader = req.getHeader(Constant.HTTP_HEADER_AUTHORIZATION);
            if (!StringUtils.isEmpty(authHeader)) {
                Matcher matcher = pattern.matcher(authHeader);
                if (matcher.matches()) {
                    return matcher.group(1);
                }
            }
        }
        return null;
    }

    public static void setAuthToken(String authToken,
                                    boolean rememberMe,
                                    AuthOptions authOptions,
                                    HttpServletRequest req,
                                    HttpServletResponse res) {
        if (authOptions.getTokenMethod() == TokenMethod.cookie) {
            Cookie cookie = new Cookie(Constant.COOKIE_NAME_TOKEN, authToken);
            if (rememberMe) {
                int durationInSeconds =
                        (int) (TimeSpanUtil.toMs(Constant.DEFAULT_REMEMBER_ME_DURATION) / 1000);
                cookie.setMaxAge(durationInSeconds);
            }
            res.addCookie(cookie);
        } else {
            res.addHeader(authOptions.getAuthTokenHeaderKey(), authToken);
        }
    }
}