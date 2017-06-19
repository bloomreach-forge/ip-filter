/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.forge.ipfilter;

public final class IpFilterConstants {


    public static final String IP_REQUEST_EVENT = "IpRequestEvent";
    public static final String CONFIG_ALLOWED_IP_RANGES = "allowed-ip-ranges";
    public static final String CONFIG_ALLOW_CMS_USERS = "allow-cms-users";
    public static final String CONFIG_ENABLED = "enabled";
    public static final String CONFIG_HOSTNAME = "hostnames";
    public static final String CONFIG_IGNORED_PATHS = "ignored-paths";
    public static final String CONFIG_IGNORED_HEADER = "ignored-header";
    public static final String CONFIG_IGNORED_HEADER_VALUES = "ignored-header-values";
    public static final String CONFIG_MATCH_ALL = "match-all";
    public static final String CONFIG_FORWARDED_FOR_HEADER = "forwarded-for-header";
    public static final String DATA = "ip-data";
    public static final String APPLICATION = "ip-filter-app";
    public static final AuthObject INVALID_AUTH_OBJECT = new AuthObject(false);

    public static final int BASIC_AUTH_PREFIX_LENGTH = "Basic ".length();
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String HEADER_X_FORWARDED_HOST = "X-Forwarded-Host";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String IP_FILTER_LISTENER_CHANNEL = "ip-filter-listener";
    public static final String IP_FILTER_MODULE_CHANNEL = "ip-filter--module-listener";
    public static final String REPOSITORY_ADDRESS_PARAM = "repository-address";
    /**
     * Config parameter for setting the realm for the basic http authentication
     */
    public static final String REALM_PARAM = "realm";
    public static final int CACHE_SITE = 100;
    public static final int CACHE_EXPIRE_IN_MINUTES = 30;
    public static final int CACHE_EXPIRES_IN_DAYS = 30;
    public static final String DEFAULT_REPOSITORY_ADDRESS = "vm://";
    /**
     * waiting time between data request event is sent.
     */
    public static final int EVENT_REQUEST_DELAY = 5000;
    /**
     * Limit number of event requests
     */
    public static final int EVENT_REQUEST_LIMIT_COUNT = 25;


    private IpFilterConstants() {
    }
}
