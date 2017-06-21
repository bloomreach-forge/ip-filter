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
package org.onehippo.forge.ipfilter.repository;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.ipfilter.cms.IpFilterService;
import org.onehippo.forge.ipfilter.cms.IpFilterServiceImpl;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.modules.ProvidesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ProvidesService(types = IpFilterService.class)
public class IpFilterModule extends AbstractReconfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(IpFilterModule.class);

    private IpFilterService service;

    @Override
    protected void doConfigure(final Node node) throws RepositoryException {
        log.debug("(Re)configuring {}, with service {}", this.getClass().getSimpleName(),
                (service == null) ? "null" : service.getClass().getSimpleName());
        if (service != null) {
            service.setConfigurationChanged(true);
        }
    }
    
    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        service = new IpFilterServiceImpl(session);
        HippoServiceRegistry.registerService(service, IpFilterService.class);
    }
    
    @Override
    protected void doShutdown() {
        HippoServiceRegistry.unregisterService(service, IpFilterService.class);
    }
}