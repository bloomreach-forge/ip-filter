/*
 * Copyright 2018-2022 Bloomreach
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

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;

public enum WatchEventType {

    CREATE(StandardWatchEventKinds.ENTRY_CREATE),

    MODIFY(StandardWatchEventKinds.ENTRY_MODIFY),

    DELETE(StandardWatchEventKinds.ENTRY_DELETE);

    private final WatchEvent.Kind<Path> kind;

    WatchEventType(WatchEvent.Kind<Path> kind) {
        this.kind = kind;
    }

    public WatchEvent.Kind<Path> getKind() {
        return kind;
    }
}
