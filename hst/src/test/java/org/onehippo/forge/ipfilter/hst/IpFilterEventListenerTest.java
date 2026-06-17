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
package org.onehippo.forge.ipfilter.hst;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onehippo.forge.ipfilter.common.IpFilterConfigLoader;

import javax.jcr.observation.Event;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IpFilterEventListenerTest {

    @Mock private IpFilterConfigLoader configLoader;
    @Mock private Event event;

    private IpFilterEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new IpFilterEventListener();
        listener.setConfigLoader(configLoader);
    }

    @Test
    void onNodeAdded_delegatesToConfigLoaderInvalidate() {
        listener.onNodeAdded(event);
        verify(configLoader).invalidate(event);
    }

    @Test
    void onNodeRemoved_delegatesToConfigLoaderInvalidate() {
        listener.onNodeRemoved(event);
        verify(configLoader).invalidate(event);
    }

    @Test
    void onPropertyAdded_delegatesToConfigLoaderInvalidate() {
        listener.onPropertyAdded(event);
        verify(configLoader).invalidate(event);
    }

    @Test
    void onPropertyChanged_delegatesToConfigLoaderInvalidate() {
        listener.onPropertyChanged(event);
        verify(configLoader).invalidate(event);
    }

    @Test
    void onPropertyRemoved_delegatesToConfigLoaderInvalidate() {
        listener.onPropertyRemoved(event);
        verify(configLoader).invalidate(event);
    }
}
