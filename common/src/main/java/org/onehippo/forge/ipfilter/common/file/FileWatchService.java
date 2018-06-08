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
package org.onehippo.forge.ipfilter.common.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class FileWatchService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(FileWatchService.class);
    private final ConcurrentMap<WatchKey, Path> keys = new ConcurrentHashMap<>();
    private final Set<String> directories;
    private final ExecutorService executor;
    private final Set<String> filenames;
    private final FileChangeObserver observer;
    private final boolean valid;

    private WatchService watcher;

    public FileWatchService(final FileChangeObserver observer, final Set<String> directories, final Set<String> filenames) throws IOException {
        this.observer = observer;
        this.directories = directories;
        this.filenames = filenames;
        watcher = FileSystems.getDefault().newWatchService();
        executor = Executors.newSingleThreadExecutor();
        if (directories.isEmpty()) {
            valid = false;
            close();
        } else {
            valid = true;
        }
        start();
    }

    @Override
    public void close() {
        try {
            watcher.close();
        } catch (IOException e) {
            log.error("Error closing watcher service", e);
        }
        executor.shutdown();
    }


    public boolean isValid() {
        return valid;
    }

    private void start() throws IOException {
        if (!valid) {
            log.warn("Cannot start watch service, no directories found for watching");
            return;
        }

        log.info("Starting file watcher");
        final Consumer<Path> register = path -> {
            try {
                if (validDirectory(path.toFile())) {
                    log.info("Registering file for change watching: {}", path);
                    registerDirectory(path);
                }
            } catch (IOException e) {
                log.error("Error registering file {}", e);
            }
        };

        for (String directory : directories) {
            final File file = new File(directory);
            register.accept(file.toPath());

        }
        executor.submit(() -> {
            while (true) {
                final WatchKey key;
                try {
                    // wait for a key to be available
                    key = watcher.take();
                } catch (InterruptedException ex) {
                    return;
                }

                final Path dir = keys.get(key);
                if (dir == null) {
                    log.warn("WatchKey {} not recognized!", key);
                    continue;
                }
                key.pollEvents().stream()
                        .filter(e -> (e.kind() != OVERFLOW))
                        .map(WatchEvent::context)
                        .forEach(p -> {
                            final Path absPath = dir.resolve((Path) p);
                            if (!absPath.toFile().isDirectory()) {
                                final File file = absPath.toFile();
                                log.debug("Detected file change in {}", file.getAbsolutePath());
                                if (filenames.contains(file.getName())) {
                                    fireEvent(file);
                                }
                            }
                        });

                //must be reset after processed
                final boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        });
    }

    private void registerDirectory(Path dir) throws IOException {
        final WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, dir);
    }
    
    public void fireEvent(final File file) {
        observer.update(file);
    }

    private boolean validDirectory(final File file) {
        if (!file.exists()) {
            log.warn("File does not exists: {}", file);
            return false;
        }
        if (!file.isDirectory()) {
            log.warn("File is not a directory: {}", file);
            return false;
        }

        return true;
    }

}
