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

import consulo.application.progress.ProgressIndicator;

import java.io.File;
import java.io.IOException;

/**
 * Saves profiler data as a snapshot file which a {@link ProfilerDumpParserProvider} can open again.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public interface ProfilerDumpWriter {
    /**
     * @return the file name offered when the user saves the snapshot
     */
    String getDumpFileName();

    /**
     * Writes the snapshot. Called on a background thread.
     */
    void writeDump(File file, ProgressIndicator indicator) throws IOException;
}
