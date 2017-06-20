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
package org.onehippo.forge.ipfilter.cms;

import javax.jcr.Session;

public class IpFilterServiceImpl implements IpFilterService {

    private final Session session;

    // NOTE: default must be true to load initial data
    private boolean changed  = true;

    public IpFilterServiceImpl(final Session session) {
        this.session = session;
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public boolean configurationChanged() {
        return changed;
    }

    @Override
    public void setConfigurationChanged(final boolean configurationChanged) {
        changed = configurationChanged;
    }

}


