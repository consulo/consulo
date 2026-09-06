/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.it.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.module.Module;
import consulo.module.content.FilePropertyPusher;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * File property pusher used by the scanning integration tests. It is inert until a test enables it; while enabled it
 * pushes a per-module value to every file and records how many times each file was offered to it and which value was
 * persisted for it.
 *
 * @author VISTALL
 */
@ExtensionImpl
public class HeadlessFilePropertyPusher implements FilePropertyPusher<String> {
    public static final Key<String> KEY = Key.create("HEADLESS_TEST_FILE_PROPERTY");
    public static final String DEFAULT_VALUE = "default";

    private static volatile boolean ourEnabled;
    private static final Map<VirtualFile, AtomicInteger> ourAcceptedFiles = new ConcurrentHashMap<>();
    private static final Map<VirtualFile, String> ourPersistedValues = new ConcurrentHashMap<>();

    public static void setEnabled(boolean enabled) {
        ourEnabled = enabled;
        reset();
    }

    public static void reset() {
        ourAcceptedFiles.clear();
        ourPersistedValues.clear();
    }

    public static int getAcceptCount(VirtualFile file) {
        AtomicInteger count = ourAcceptedFiles.get(file);
        return count == null ? 0 : count.get();
    }

    public static @Nullable String getPersistedValue(VirtualFile file) {
        return ourPersistedValues.get(file);
    }

    public static String moduleValue(Module module) {
        return "module:" + module.getName();
    }

    @Override
    public Key<String> getFileDataKey() {
        return KEY;
    }

    @Override
    public boolean pushDirectoriesOnly() {
        return false;
    }

    @Override
    public String getDefaultValue() {
        return DEFAULT_VALUE;
    }

    @Override
    public @Nullable String getImmediateValue(Module module) {
        return ourEnabled ? moduleValue(module) : null;
    }

    @Override
    public @Nullable String getImmediateValue(Project project, @Nullable VirtualFile file) {
        return null;
    }

    @Override
    public boolean acceptsFile(VirtualFile file, Project project) {
        if (!ourEnabled) {
            return false;
        }
        ourAcceptedFiles.computeIfAbsent(file, ignored -> new AtomicInteger()).incrementAndGet();
        return true;
    }

    @Override
    public boolean acceptsDirectory(VirtualFile file, Project project) {
        return ourEnabled;
    }

    @Override
    public void persistAttribute(Project project, VirtualFile fileOrDir, String value) {
        if (!fileOrDir.isDirectory()) {
            ourPersistedValues.put(fileOrDir, value);
        }
    }
}
