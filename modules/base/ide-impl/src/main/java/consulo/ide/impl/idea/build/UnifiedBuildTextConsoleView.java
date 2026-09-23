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
package consulo.ide.impl.idea.build;

import consulo.execution.impl.internal.console.UnifiedConsoleViewImpl;
import consulo.execution.ui.console.Filter;
import consulo.process.util.AnsiEscapeDecoder;
import consulo.project.Project;

import java.util.List;

/**
 * Analog of {@link BuildTextConsoleView} for the frontends which render {@link consulo.ui} components rather
 * than swing - the same text, written by {@link BuildTextConsole}, into a console they can draw.
 *
 * @author VISTALL
 * @since 2026-09-23
 */
public class UnifiedBuildTextConsoleView extends UnifiedConsoleViewImpl implements BuildTextConsole {
    private final AnsiEscapeDecoder myAnsiEscapeDecoder = new AnsiEscapeDecoder();

    public UnifiedBuildTextConsoleView(Project project, boolean viewer, List<Filter> executionFilters) {
        super(project, viewer);
        executionFilters.forEach(this::addMessageFilter);
    }

    @Override
    public AnsiEscapeDecoder getAnsiEscapeDecoder() {
        return myAnsiEscapeDecoder;
    }
}
