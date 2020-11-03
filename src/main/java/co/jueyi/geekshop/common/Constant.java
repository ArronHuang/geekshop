package co.jueyi.geekshop.common;

/**
 * Created on Nov, 2020 by @author bobo
 */
public abstract class Constant {
    public static final String DB_NAME_H2 = "h2";
    public static final String DB_NAME_MYSQL = "mysql";

    public static final String USERNAME_ANONYMOUSE = "anonymous";
    public static final String USERNAME_SYSTEM = "system";

    public static final String REQUEST_ATTRIBUTE_CURRENT_USER = "GEEKSHOP_REQUEST_CURRENT_USER";

    public static final String COOKIE_NAME_TOKEN = "token";

    public static final String HTTP_HEADER_AUTHORIZATION = "Authorization";

    public static final String DEFAULT_ANONYMOUS_SESSION_DURATION = "1y";
    public static final String DEFAULT_REMEMBER_ME_DURATION = "1y";

    public static final int PASSWORD_LENGTH_MIN = 8;
    public static final int PASSWORD_LENGTH_MAX = 20;
}