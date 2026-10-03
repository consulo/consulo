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
import org.jspecify.annotations.Nullable;

import java.io.File;

/**
 * Reads a profiler dump into {@link ProfilerData}.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public interface ProfilerDumpFileParser {
    /**
     * @return the help topic shown with the parsed data, or null for none
     */
    default @Nullable String getHelpId() {
        return null;
    }

    /**
     * Parses a dump on a background thread. A parser whose provider requires no file extension may be given a directory.
     * Bad input is reported as a {@link Failure}, not thrown.
     */
    ProfilerDumpFileParsingResult parse(File fileOrDirectory, ProgressIndicator indicator);
}
