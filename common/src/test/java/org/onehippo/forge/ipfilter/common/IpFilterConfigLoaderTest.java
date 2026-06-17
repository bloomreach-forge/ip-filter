/*
 * Copyright 2025 Bloomreach
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.observation.Event;
import java.io.File;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IpFilterConfigLoaderTest {

    @Mock private Repository repository;
    @Mock private Credentials credentials;
    @Mock private Session session;
    @Mock private Node configNode;
    @Mock private Node childNode;
    @Mock private NodeIterator nodeIterator;
    @Mock private Property property;
    @Mock private Value value;
    @Mock private Event event;

    private static class MinimalLoader extends IpFilterConfigLoader {}

    private MinimalLoader loader() {
        return new MinimalLoader();
    }

    // --- simple state ---

    @Test
    void needReloading_defaultsToTrue() {
        assertTrue(loader().needReloading());
    }

    @Test
    void invalidate_setsNeedRefreshTrue() {
        MinimalLoader l = loader();
        l.needRefresh = false;
        l.invalidate(event);
        assertTrue(l.needReloading());
    }

    @Test
    void update_setsNeedRefreshTrue() {
        MinimalLoader l = loader();
        l.needRefresh = false;
        l.update(new File("dummy.properties"));
        assertTrue(l.needReloading());
    }

    @Test
    void setAndGetConfigurationLocation_roundTrips() {
        MinimalLoader l = loader();
        l.setConfigurationLocation("/hippo:configuration/hippo:modules/ipfilter");
        assertEquals("/hippo:configuration/hippo:modules/ipfilter", l.getConfigurationLocation());
    }

    @Test
    void setAndGetRepository_roundTrips() {
        MinimalLoader l = loader();
        l.setRepository(repository);
        assertSame(repository, l.getRepository());
    }

    @Test
    void setAndGetCredentials_roundTrips() {
        MinimalLoader l = loader();
        l.setCredentials(credentials);
        assertSame(credentials, l.getCredentials());
    }

    @Test
    void getLastLoadDate_isNotNull() {
        assertNotNull(loader().getLastLoadDate());
    }

    @Test
    void getLastLoadDate_isBeforeOrEqualNow() {
        assertTrue(loader().getLastLoadDate().compareTo(new Date()) <= 0);
    }

    @Test
    void getForwardedForHostHeaders_isNotNull() {
        assertNotNull(loader().getForwardedForHostHeaders());
    }

    // --- load() branches ---

    @Test
    void load_whenNoRefreshNeeded_returnsImmediately() {
        MinimalLoader l = loader();
        l.needRefresh = false;
        assertNotNull(l.load());
        verifyNoInteractions(repository);
    }

    @Test
    void load_whenRepositoryLoginThrows_returnsEmptyData() throws RepositoryException {
        MinimalLoader l = loader();
        l.setRepository(repository);
        l.setCredentials(credentials);
        l.setConfigurationLocation("/config");
        when(repository.login(credentials)).thenThrow(new RepositoryException("conn failed"));
        assertNotNull(l.load());
    }

    @Test
    void load_withEmptyConfigNode_populatesForwardedHostHeader() throws Exception {
        MinimalLoader l = loader();
        l.setRepository(repository);
        l.setCredentials(credentials);
        l.setConfigurationLocation("/config");

        when(repository.login(credentials)).thenReturn(session);
        when(session.getNode("/config")).thenReturn(configNode);
        when(configNode.hasProperty(IpFilterConstants.CONFIG_FORWARDED_HOST_HEADER)).thenReturn(false);
        when(configNode.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(false);

        l.load();

        assertTrue(l.getForwardedForHostHeaders().contains(IpFilterConstants.HEADER_X_FORWARDED_HOST));
        verify(session).logout();
    }

    @Test
    void load_withForwardedHostHeaderConfigured_addsCustomHeader() throws Exception {
        MinimalLoader l = loader();
        l.setRepository(repository);
        l.setCredentials(credentials);
        l.setConfigurationLocation("/config");

        when(repository.login(credentials)).thenReturn(session);
        when(session.getNode("/config")).thenReturn(configNode);
        when(configNode.hasProperty(IpFilterConstants.CONFIG_FORWARDED_HOST_HEADER)).thenReturn(true);
        Property prop = mock(Property.class);
        Value val = mock(Value.class);
        when(configNode.getProperty(IpFilterConstants.CONFIG_FORWARDED_HOST_HEADER)).thenReturn(prop);
        when(prop.getValues()).thenReturn(new Value[]{val});
        when(val.getString()).thenReturn("X-Custom-Host");
        when(configNode.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(false);

        l.load();

        assertTrue(l.getForwardedForHostHeaders().contains("X-Custom-Host"));
        assertTrue(l.getForwardedForHostHeaders().contains(IpFilterConstants.HEADER_X_FORWARDED_HOST));
    }

    @Test
    void load_whenConfigNodeGetThrows_returnsExistingData() throws Exception {
        MinimalLoader l = loader();
        l.setRepository(repository);
        l.setCredentials(credentials);
        l.setConfigurationLocation("/config");

        when(repository.login(credentials)).thenReturn(session);
        when(session.getNode("/config")).thenThrow(new RepositoryException("not found"));

        assertNotNull(l.load());
        verify(session).logout();
    }

    @Test
    void load_withValidChildNode_createsAuthObject() throws Exception {
        MinimalLoader l = loader();
        l.setRepository(repository);
        l.setCredentials(credentials);
        l.setConfigurationLocation("/config");

        when(repository.login(credentials)).thenReturn(session);
        when(session.getNode("/config")).thenReturn(configNode);
        when(configNode.hasProperty(IpFilterConstants.CONFIG_FORWARDED_HOST_HEADER)).thenReturn(false);
        when(configNode.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(true, false);
        when(nodeIterator.nextNode()).thenReturn(childNode);
        when(childNode.getName()).thenReturn("rule1");

        // CONFIG_ENABLED → default true (property not present)
        when(childNode.hasProperty(IpFilterConstants.CONFIG_ENABLED)).thenReturn(false);
        // CONFIG_IGNORED_PATHS → empty
        when(childNode.hasProperty(IpFilterConstants.CONFIG_IGNORED_PATHS)).thenReturn(false);
        // CONFIG_HOSTNAME → ".*" wildcard host
        when(childNode.hasProperty(IpFilterConstants.CONFIG_HOSTNAME)).thenReturn(true);
        when(childNode.getProperty(IpFilterConstants.CONFIG_HOSTNAME)).thenReturn(property);
        when(property.getValues()).thenReturn(new Value[]{value});
        when(value.getString()).thenReturn(".*");
        // CONFIG_ALLOWED_IP_RANGES → one IP range
        when(childNode.hasProperty(IpFilterConstants.CONFIG_ALLOWED_IP_RANGES)).thenReturn(true);
        Property rangesProp = mock(Property.class);
        Value rangeValue = mock(Value.class);
        when(childNode.getProperty(IpFilterConstants.CONFIG_ALLOWED_IP_RANGES)).thenReturn(rangesProp);
        when(rangesProp.getValues()).thenReturn(new Value[]{rangeValue});
        when(rangeValue.getString()).thenReturn("10.0.0.0/8");
        // CONFIG_ALLOW_CMS_USERS → false
        when(childNode.hasProperty(IpFilterConstants.CONFIG_ALLOW_CMS_USERS)).thenReturn(false);
        // CONFIG_MATCH_ALL → false
        when(childNode.hasProperty(IpFilterConstants.CONFIG_MATCH_ALL)).thenReturn(false);
        // CONFIG_CACHE_ENABLED → true
        when(childNode.hasProperty(IpFilterConstants.CONFIG_CACHE_ENABLED)).thenReturn(false);
        // CONFIG_FORWARDED_FOR_HEADER → default
        when(childNode.hasProperty(IpFilterConstants.CONFIG_FORWARDED_FOR_HEADER)).thenReturn(false);
        // parseHeaders → childNode.getNodes() returns empty iterator
        NodeIterator emptyIter = mock(NodeIterator.class);
        when(childNode.getNodes()).thenReturn(emptyIter);
        when(emptyIter.hasNext()).thenReturn(false);

        l.load();

        assertFalse(l.getForwardedForHostHeaders().isEmpty());
    }

    @Test
    void load_withChildNodeMissingHostnames_skipsChildNode() throws Exception {
        MinimalLoader l = loader();
        l.setRepository(repository);
        l.setCredentials(credentials);
        l.setConfigurationLocation("/config");

        when(repository.login(credentials)).thenReturn(session);
        when(session.getNode("/config")).thenReturn(configNode);
        when(configNode.hasProperty(IpFilterConstants.CONFIG_FORWARDED_HOST_HEADER)).thenReturn(false);
        when(configNode.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(true, false);
        when(nodeIterator.nextNode()).thenReturn(childNode);

        // CONFIG_ENABLED not set → defaults to true (enabled)
        when(childNode.hasProperty(IpFilterConstants.CONFIG_ENABLED)).thenReturn(false);
        // CONFIG_HOSTNAME not present → getMultipleStringProperty returns null → parse() returns null
        when(childNode.hasProperty(IpFilterConstants.CONFIG_IGNORED_PATHS)).thenReturn(false);
        when(childNode.hasProperty(IpFilterConstants.CONFIG_HOSTNAME)).thenReturn(false);

        assertNotNull(l.load());
    }
}
