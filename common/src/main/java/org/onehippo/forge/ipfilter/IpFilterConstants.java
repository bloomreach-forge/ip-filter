package org.onehippo.forge.ipfilter;

public final class IpFilterConstants {


    public static final String IP_REQUEST_EVENT = "IpRequestEvent";
    public static final String CONFIG_ALLOWED_IP_RANGES = "allowed-ip-ranges";
    public static final String CONFIG_ALLOW_CMS_USERS = "allow-cms-users";
    public static final String CONFIG_ENABLED = "enabled";
    public static final String CONFIG_HOSTNAME = "hostnames";
    public static final String CONFIG_MATCH_ALL = "match-all";
    public static final String DATA = "ip-data";
    public static final String APPLICATION = "ip-filter-app";
    public static final AuthObject INVALID_AUTH_OBJECT = new AuthObject(false);

    public static final String ATTR_ALLOWED = "ip-filter-allowed";
    public static final int BASIC_AUTH_PREFIX_LENGTH = "Basic ".length();
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String IP_FILTER_LISTENER_CHANNEL = "ip-filter-listener";
    public static final String IP_FILTER_MODULE_CHANNEL = "ip-filter--module-listener";
    public static final String REPOSITORY_ADDRESS_PARAM = "repository-address";
    /**
     * Config parameter for setting the realm for the basic http authentication
     */
    public static final String REALM_PARAM = "realm";
    static final String DEFAULT_REPOSITORY_ADDRESS = "vm://";

    private IpFilterConstants() {
    }
}
