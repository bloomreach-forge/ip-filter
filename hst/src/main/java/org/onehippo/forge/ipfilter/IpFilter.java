/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 */
package org.onehippo.forge.ipfilter;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.forge.ipfilter.IpFilterConstants.HEADER_AUTHORIZATION;
import static org.onehippo.forge.ipfilter.IpFilterConstants.REPOSITORY_ADDRESS_PARAM;

/**
 * Filter allowing only access for IP ranges that are configured
 */
public class IpFilter extends IpFilterCommon {

    private static final Logger log = LoggerFactory.getLogger(IpFilter.class);

    
    @Override
    protected Status authenticate(final HttpServletRequest request) {
        final UserCredentials credentials = new UserCredentials(request.getHeader(HEADER_AUTHORIZATION));
        if (!credentials.valid()) {
            log.debug("Invalid credentials, null or empty");
            return Status.UNAUTHORIZED;
        }
        final Boolean cached = userCache.getUnchecked(credentials.getUsername());
        if (cached != null && cached) {
            log.debug("Cached user: {}", credentials);
            return Status.OK;
        }
        Session session = null;
        try {
            // try to authenticate:
            session = getSession(credentials);
            if (session == null) {
                log.debug("No valid session for user: {}", credentials.getUsername());
                return Status.UNAUTHORIZED;
            }
            log.debug("Successfully validated user: {}", credentials.getUsername());
            userCache.put(credentials.getUsername(), Boolean.TRUE);
            return Status.OK;
        } finally {
            closeSession(session);
        }
    }

    private void closeSession(Session session) {
        if (session != null && session.isLive()) {
            session.logout();
        }
    }

    @Override
    protected void initializeConfigManger() {

        if (HstServices.isAvailable()) {
            final ComponentManager componentManager = HstServices.getComponentManager();
            configLoader = componentManager.getComponent(IpFilterConfigLoader.class.getName(), IpFilterConfigLoader.class.getPackage().getName());
            if (configLoader == null) {
                log.error("Configuration loader was null");
            } else {
                initialized = true;
            }
        } else {
            log.info("HstService not available yet...waiting..");
        }

    }

    private Session getSession(final UserCredentials credentials) {
        HippoRepository hippoRepository = getHippoRepository(repositoryAddress);
        if (hippoRepository == null) {
            return null;
        }
        try {
            return hippoRepository.login(credentials.getUsername(), credentials.getPassword().toCharArray());
        } catch (LoginException e) {
            log.debug("Invalid credentials for username '{}'", credentials.getUsername());
            return null;
        } catch (RepositoryException e) {
            log.error("Error during authentication", e);
            return null;
        }
    }

    private HippoRepository getHippoRepository(String address) {
        if (address == null || address.length() == 0) {
            log.error("Repository address (parameter " + REPOSITORY_ADDRESS_PARAM
                    + " not set. Unable to perform authorization. Return unauthorized.");
            return null;
        }
        try {
            return HippoRepositoryFactory.getHippoRepository(address);
        } catch (RepositoryException e) {
            log.error("Error while obtaining repository: ", e);
            return null;
        }
    }
}
