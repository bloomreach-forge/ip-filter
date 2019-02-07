/*
 * Copyright 2017-2019 BloomReach Inc. (http://www.bloomreach.com)
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

public class IpHostPair {

    private final String ip;
    private final String host;

    public IpHostPair(final String ip, final String host) {
        this.ip = ip;
        this.host = host;
    }

    public String getIp() {
        return ip;
    }

    public String getHost() {
        return host;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final IpHostPair that = (IpHostPair) o;
        return ip.equals(that.ip) && host.equals(that.host);
    }

    @Override
    public int hashCode() {
        int result = ip.hashCode();
        result = 31 * result + host.hashCode();
        return result;
    }
}
