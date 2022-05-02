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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FileWatchServiceTest {
    private static final Logger log = LoggerFactory.getLogger(FileWatchServiceTest.class);

    private static final String FILE_WATCH_SERVICE_TEST_PROPERTIES = FileWatchServiceTest.class.getSimpleName() + ".properties";
    private static final int WAIT_TIME = 300;
    private FileWatchService service;
    private File tmpDir;
    private final ExampleFileObserver observer = new ExampleFileObserver();
    final ExecutorService executor = Executors.newFixedThreadPool(2);

    @Before
    public void setUp() throws Exception {

        tmpDir = Files.createTempDir();
        service = new FileWatchService(observer, ImmutableSet.of(tmpDir.getPath()), ImmutableSet.of(FILE_WATCH_SERVICE_TEST_PROPERTIES));

    }

    @Test
    public void testService() throws Exception {
        assertNotNull(service);
        executor.execute(() -> {
            try {
                Thread.sleep(WAIT_TIME);
                log.info("observer {}", observer);
                assertNotNull(observer.getChanged());
            } catch (InterruptedException e) {
                log.error("", e);
            }

        });
        executor.execute(() -> {
            final String absolutePath = tmpDir.getAbsolutePath();
            try {
                final String fileName = absolutePath + File.separator + FILE_WATCH_SERVICE_TEST_PROPERTIES;
                log.info("Creating file: {}", fileName);
                final boolean created = new File(fileName).createNewFile();
                assertTrue(created);
            } catch (IOException e) {
                log.error("Error creating file", e);
            }
        });
        try {
            executor.awaitTermination(WAIT_TIME * 2, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("e {}", e);
        }


    }

    @After
    public void tearDown() throws Exception {
        service.close();
        executor.shutdown();
        File[] contents = tmpDir.listFiles();
        if (contents != null) {
            for (File f : contents) {
                f.delete();
            }
        }
        tmpDir.delete();
    }


}