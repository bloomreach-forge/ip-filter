/*
 * Copyright 2026 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.forge.ipfilter.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestConfigLoader extends IpFilterConfigLoader {


    private static final Logger log = LoggerFactory.getLogger(TestConfigLoader.class);
    private boolean needReloading;

    @Override
    public boolean needReloading() {

        return needReloading;
    }

    @Override
    public synchronized Map<String, AuthObject> load() {

        return new HashMap<>();
    }


    @Override
    public Set<String> getForwardedForHostHeaders() {

        final Set<String> forwardedForHostHeaders = super.getForwardedForHostHeaders();
        if (forwardedForHostHeaders.isEmpty()) {
            forwardedForHostHeaders.add(IpFilterConstants.HEADER_X_FORWARDED_HOST);
        }
        return forwardedForHostHeaders;
    }

    public boolean isNeedReloading() {
        return needReloading;
    }

    public void setNeedReloading(final boolean needReloading) {
        this.needReloading = needReloading;
    }
}
