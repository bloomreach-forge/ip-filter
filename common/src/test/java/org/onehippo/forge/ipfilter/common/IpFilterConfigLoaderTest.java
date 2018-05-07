/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import com.google.common.collect.Multimap;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.junit.Assert.*;

public class IpFilterConfigLoaderTest {
    private static final Logger log = LoggerFactory.getLogger(IpFilterConfigLoaderTest.class);

    @Test
    public void testSystemLoading() {
        final TestConfigLoader configLoader = new TestConfigLoader();
        Multimap<String, String> globalSettings = configLoader.loadGlobalSettings();
        assertEquals(0, globalSettings.size());
        final File file = new File("");
        final String absolutePath = file.getAbsolutePath() + File.separator
                + "src" + File.separator
                + "test" + File.separator
                + "resources";
        System.setProperty("catalina.base", absolutePath);
        globalSettings = configLoader.loadGlobalSettings();
        assertEquals(4, globalSettings.size());
        // reset
        System.setProperty("catalina.base", "");
        globalSettings = configLoader.loadGlobalSettings();
        assertEquals(0, globalSettings.size());
        System.setProperty("catalina.home", absolutePath);
        globalSettings = configLoader.loadGlobalSettings();
        assertEquals(4, globalSettings.size());
        // reset
        System.setProperty("catalina.home", "");
        globalSettings = configLoader.loadGlobalSettings();
        assertEquals(0, globalSettings.size());
        System.setProperty(IpFilterConstants.IP_FILTER_PROPERTY_NAME,
                absolutePath + File.separator
                        + "conf" + File.separator
                        + IpFilterConstants.IP_FILTER_PROPERTY_NAME + File.separator
        );
        globalSettings = configLoader.loadGlobalSettings();
        assertEquals(4, globalSettings.size());
        //
        System.setProperty(IpFilterConstants.IP_FILTER_PROPERTY_NAME,"file:"+
                absolutePath + File.separator
                        + "conf" + File.separator
                        + IpFilterConstants.IP_FILTER_PROPERTY_NAME + File.separator
        );
        globalSettings = configLoader.loadGlobalSettings();
        assertEquals(4, globalSettings.size());


    }
}