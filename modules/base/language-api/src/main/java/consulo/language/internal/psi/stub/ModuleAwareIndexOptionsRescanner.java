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
package consulo.language.internal.psi.stub;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.psi.stub.IndexOption;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * Implementation side of {@link consulo.language.psi.stub.ModuleAwareIndexOptions}.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ModuleAwareIndexOptionsRescanner {
    void optionsChanged(Project project, Collection<VirtualFile> files, String reason);

    byte @Nullable [] getRecordedOptionPayload(VirtualFile file, String providerId);

    byte[] payloadOf(IndexOption option);

    boolean applyViewOptions(Project project, VirtualFile file, @Nullable Map<String, byte[]> payloads);

    @Nullable IndexOption getDefaultOptions(Project project, VirtualFile file, String providerId);
}
