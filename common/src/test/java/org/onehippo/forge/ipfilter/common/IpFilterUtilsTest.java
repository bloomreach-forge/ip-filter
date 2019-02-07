/*
 * Copyright 2018-2019 BloomReach Inc. (http://www.bloomreach.com)
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

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class IpFilterUtilsTest {


    @Test
    public void testGetIp() {
        final Set<String> E = Collections.emptySet();
        final AuthObject object = new AuthObject(E, E, E, Collections.emptyMap(), true, null, true);
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getRemoteAddr()).andReturn("127.0.0.1").anyTimes();
        expect(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_FOR)).andReturn(null).anyTimes();
        replay(request);
        String header = IpFilterUtils.getIp(request, object.getForwardedForHeader());
        assertEquals("127.0.0.1", header);
        // recreate
        request = createMock(HttpServletRequest.class);
        expect(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_FOR)).andReturn("localhost").anyTimes();
        replay(request);
        header = IpFilterUtils.getIp(request, object.getForwardedForHeader());
        assertEquals("localhost", header);

    }

    @Test
    public void testGetHost() {
        final Set<String> E = Collections.emptySet();
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getRemoteHost()).andReturn("127.0.0.1").anyTimes();
        expect(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_HOST)).andReturn(null).anyTimes();
        replay(request);
        String host = IpFilterUtils.getHost(request);
        assertEquals("127.0.0.1", host);
        // recreate
        request = createMock(HttpServletRequest.class);
        expect(request.getHeader(IpFilterConstants.HEADER_X_FORWARDED_HOST)).andReturn("localhost").anyTimes();
        replay(request);
        host = IpFilterUtils.getHost(request);
        assertEquals("localhost", host);

    }


}