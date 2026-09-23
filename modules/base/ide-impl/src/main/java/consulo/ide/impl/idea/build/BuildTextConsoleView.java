// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import consulo.execution.ui.console.Filter;
import consulo.ide.impl.idea.execution.impl.ConsoleViewImpl;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.process.util.AnsiEscapeDecoder;
import consulo.project.Project;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class BuildTextConsoleView extends ConsoleViewImpl implements BuildTextConsole {
    private final AnsiEscapeDecoder myAnsiEscapeDecoder = new AnsiEscapeDecoder();

    public BuildTextConsoleView(Project project, boolean viewer, List<Filter> executionFilters) {
        super(project, GlobalSearchScope.allScope(project), viewer, true);
        executionFilters.forEach(this::addMessageFilter);
    }

    @Override
    public AnsiEscapeDecoder getAnsiEscapeDecoder() {
        return myAnsiEscapeDecoder;
    }
}
