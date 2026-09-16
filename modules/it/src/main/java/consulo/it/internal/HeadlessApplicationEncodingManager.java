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

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.encoding.EncodingProjectManager;
import consulo.virtualFileSystem.impl.internal.encoding.BaseApplicationEncodingManager;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The production {@code EncodingManagerImpl} runs on the {@link ComponentProfiles#PRODUCTION} profile only, because it
 * reacts to editor and document changes; content loading during indexing needs the application encoding manager, so the
 * headless application reuses the half of it that answers from state alone. There is no project-level encoding
 * configuration in a headless run, so per-file encodings are always unset.
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessApplicationEncodingManager extends BaseApplicationEncodingManager {
    private final Map<String, AtomicInteger> myEncodingLookups = new ConcurrentHashMap<>();

    /**
     * How often a per-file encoding was asked for {@code file} since the last reset. Counted per file on purpose: the
     * whole suite shares this application, so a background scan of an unrelated file must not be mistaken for a lookup
     * the test provoked.
     */
    public int getEncodingLookupCount(VirtualFile file) {
        AtomicInteger count = myEncodingLookups.get(file.getPath());
        return count == null ? 0 : count.get();
    }

    public void resetEncodingLookupCount() {
        myEncodingLookups.clear();
    }

    @Override
    protected @Nullable EncodingProjectManager getProjectEncodingManager(Project project) {
        return null;
    }

    @Override
    public @Nullable Charset getEncoding(@Nullable VirtualFile virtualFile, boolean useParentDefaults) {
        if (virtualFile != null) {
            myEncodingLookups.computeIfAbsent(virtualFile.getPath(), path -> new AtomicInteger()).incrementAndGet();
        }
        return super.getEncoding(virtualFile, useParentDefaults);
    }
}
