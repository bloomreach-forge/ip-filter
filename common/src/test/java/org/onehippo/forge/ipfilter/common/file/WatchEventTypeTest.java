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
package org.onehippo.forge.ipfilter.common.file;

import org.junit.jupiter.api.Test;

import java.nio.file.StandardWatchEventKinds;

import static org.junit.jupiter.api.Assertions.*;

class WatchEventTypeTest {

    @Test
    void values_containsThreeEntries() {
        assertEquals(3, WatchEventType.values().length);
    }

    @Test
    void valueOf_create_returnsCreateType() {
        assertEquals(WatchEventType.CREATE, WatchEventType.valueOf("CREATE"));
    }

    @Test
    void valueOf_modify_returnsModifyType() {
        assertEquals(WatchEventType.MODIFY, WatchEventType.valueOf("MODIFY"));
    }

    @Test
    void valueOf_delete_returnsDeleteType() {
        assertEquals(WatchEventType.DELETE, WatchEventType.valueOf("DELETE"));
    }

    @Test
    void create_getKind_returnsEntryCreate() {
        assertEquals(StandardWatchEventKinds.ENTRY_CREATE, WatchEventType.CREATE.getKind());
    }

    @Test
    void modify_getKind_returnsEntryModify() {
        assertEquals(StandardWatchEventKinds.ENTRY_MODIFY, WatchEventType.MODIFY.getKind());
    }

    @Test
    void delete_getKind_returnsEntryDelete() {
        assertEquals(StandardWatchEventKinds.ENTRY_DELETE, WatchEventType.DELETE.getKind());
    }
}
