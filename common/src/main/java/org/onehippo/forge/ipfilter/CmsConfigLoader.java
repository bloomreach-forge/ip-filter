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

import javax.jcr.Session;

public class CmsConfigLoader extends IpFilterConfigLoader{

    private final Session session;
    private final IpFilterService service;

    public CmsConfigLoader(final Session session, final IpFilterService service) {
        this.session = session;
        this.service = service;
        setConfigurationLocation("/hippo:configuration/hippo:modules/ipfilter-module/hippo:moduleconfig");
    }


    @Override
    public boolean needReloading() {
        if (service.configurationChanged()) {
            needRefresh = true;
            service.setConfigurationChanged(false);
        }
        return needRefresh;

    }

    @Override
    protected void closeSession(final Session session) {
        // ignore for CMS module, we share one session
    }

    @Override
    public Session getSession() {
        return session;
    }
}
