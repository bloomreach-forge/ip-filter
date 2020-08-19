/*
 * Copyright 2018-2020 Bloomreach
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

package org.onehippo.forge.ipfilter.common.file;

import java.io.File;

public class ExampleFileObserver implements FileChangeObserver {

    private File changed;

    @Override
    public void update(final File file) {

        changed = file;
    }

    public File getChanged() {
        return changed;
    }

    @Override
    public String toString() {
        return "ExampleFileObserver{" +
                "changed=" + changed +
                '}';
    }
}
