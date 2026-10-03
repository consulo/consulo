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
package consulo.execution.profiler;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

/**
 * A snapshot format the profiler can open: perf output, a NYTProf directory, collapsed stacks and so on.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ProfilerDumpParserProvider {
    String getId();

    LocalizeValue getName();

    /**
     * @return the extension, without a dot, of the files this format is stored in, or null when any file or a directory is accepted
     */
    @Nullable String getRequiredFileExtension();

    /**
     * @return whether every file with {@link #getRequiredFileExtension()} is a snapshot of this format, so such files open in the
     * profiler snapshot editor instead of the default editor. Return false for a generic extension, such as {@code gz}: those files
     * open as snapshots only from the profiler
     */
    default boolean isExclusiveExtension() {
        return false;
    }

    ProfilerDumpFileParser createParser(Project project);
}
