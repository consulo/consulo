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
package consulo.language.psi.stub;

import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * Decides, for a stub index query, which stored variant of a file the reader wants: the option the reader expects
 * {@code file} to have been indexed under for the given provider. A file whose stored variants contain no match is
 * served through its current variant. Installed for the duration of a computation with
 * {@link ModuleAwareIndexOptions#withSelector}.
 */
@FunctionalInterface
public interface IndexOptionSelector {
    /**
     * Every stored variant of every file takes part in the query; elements of variants other than the current one
     * are served through a PSI copy parsed under that variant.
     */
    IndexOptionSelector ALL_VARIANTS = (providerId, file) -> null;

    @Nullable IndexOption select(String providerId, VirtualFile file);
}
