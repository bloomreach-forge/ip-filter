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

import static org.junit.jupiter.api.Assertions.*;

class StatusTest {

    @Test
    void values_containsAllExpectedStatuses() {
        Status[] statuses = Status.values();
        assertNotNull(statuses);
        assertTrue(statuses.length > 0);
    }

    @Test
    void valueOf_ok_returnsOkStatus() {
        assertEquals(Status.OK, Status.valueOf("OK"));
    }

    @Test
    void valueOf_forbidden_returnsForbiddenStatus() {
        assertEquals(Status.FORBIDDEN, Status.valueOf("FORBIDDEN"));
    }

    @Test
    void valueOf_unauthorized_returnsUnauthorizedStatus() {
        assertEquals(Status.UNAUTHORIZED, Status.valueOf("UNAUTHORIZED"));
    }
}
