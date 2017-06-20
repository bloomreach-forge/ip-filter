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
package org.onehippo.forge.ipfilter.common;

import com.google.common.base.Objects;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

/**
 * Checks if IP addresses matches with the IP range. Relies on org.springframework.security.web.util.IpAddressMatcher
 */
public final class IpMatcher {

    /**
     * Returns the IP matcher representation of the IP range argument
     *
     * @param ipRange IP range
     * @return IP matcher
     */
    public static IpMatcher valueOf(String ipRange) {
        String ipRangeTrimmedToNull = StringUtils.trimToNull(ipRange);
        if (ipRangeTrimmedToNull == null) {
            return null;
        }
        return new IpMatcher(ipRangeTrimmedToNull);
    }

    /**
     * IP matcher
     */
    private final IpAddressMatcher ipMatcher;

    /**
     * IP range
     */
    private final String ipRange;

    /**
     * Constructor
     *
     * @param ipRange IP range
     * @throws IllegalArgumentException If the IP range is malformed
     */
    private IpMatcher(String ipRange) {
        ipMatcher = new IpAddressMatcher(ipRange);
        this.ipRange = ipRange;
    }


    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof IpMatcher && Objects.equal(getIpRange(), ((IpMatcher) o).getIpRange());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getIpRange());
    }

    @Override
    public String toString() {
        return getIpRange();
    }

    /**
     * IP range getter
     *
     * @return IP range
     */
    public String getIpRange() {
        return ipRange;
    }

    /**
     * Checks if the IP range matches the IP address
     *
     * @param ipAddress IP address
     * @return TRUE if the IP is in the IP range
     * @throws IllegalArgumentException If the IP is malformed
     */
    public boolean matches(String ipAddress) {
        String ipAddressTrimmed = StringUtils.trimToNull(ipAddress);
        return ipAddressTrimmed != null && ipMatcher.matches(ipAddressTrimmed);
    }


}
