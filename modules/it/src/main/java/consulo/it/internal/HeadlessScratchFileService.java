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
import consulo.container.boot.ContainerPathManager;
import consulo.language.Language;
import consulo.language.scratch.RootType;
import consulo.language.scratch.ScratchFileService;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.PerFileMappings;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Headless {@code ScratchFileService}: the production impl lives in {@code ide-impl}. A headless run creates
 * no scratch files, so no file belongs to a scratch root; the code style indent detection asks that for
 * every editor it measures.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessScratchFileService extends ScratchFileService {
    private final Map<VirtualFile, Language> myMappings = new ConcurrentHashMap<>();

    @Override
    public String getRootPath(RootType rootId) {
        return ContainerPathManager.get().getScratchPath().replace('\\', '/') + "/" + rootId.getId();
    }

    @Override
    public @Nullable RootType getRootType(@Nullable VirtualFile file) {
        return null;
    }

    @Override
    public @Nullable VirtualFile findFile(RootType rootType, String pathName, Option option) {
        return null;
    }

    @Override
    public PerFileMappings<Language> getScratchesMapping() {
        return new PerFileMappings<>() {
            @Override
            public void setMapping(@Nullable VirtualFile file, @Nullable Language value) {
                if (file == null) {
                    return;
                }
                if (value == null) {
                    myMappings.remove(file);
                }
                else {
                    myMappings.put(file, value);
                }
            }

            @Override
            public @Nullable Language getMapping(@Nullable VirtualFile file) {
                return file == null ? null : myMappings.get(file);
            }
        };
    }
}
