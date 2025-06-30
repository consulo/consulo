// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.compiler.impl.internal;

import consulo.execution.ui.console.Filter;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.project.ui.view.internal.ProjectSettingsService;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;

public class ModuleLinkFilter implements Filter {
    private final Project myProject;
    private static final String TESTS_PREFIX = "tests of "; //NON-NLS

    public ModuleLinkFilter(Project project) {
        myProject = project;
    }

    @Override
    public Result applyFilter(@Nonnull String line, int entireLength) {
        int start = line.indexOf("[");
        if (start == -1) {
            return null;
        }
        int end = line.indexOf(']', start + 1);
        if (end == -1) {
            return null;
        }
        String moduleNameCandidate = line.substring(start + 1, end);
        boolean isTests = moduleNameCandidate.startsWith(TESTS_PREFIX);
        String moduleName = isTests ? moduleNameCandidate.substring(TESTS_PREFIX.length()) : moduleNameCandidate;

        int lineStart = entireLength - line.length();
        return new Result(
            lineStart + start + 1 + (isTests ? TESTS_PREFIX.length() : 0),
            lineStart + end,
            new HyperlinkInfo() {
                @Override
                @RequiredUIAccess
                public void navigate(@Nonnull Project project) {
                    if (project.isDisposed()) {
                        return;
                    }
                    Module module = ModuleManager.getInstance(myProject).findModuleByName(moduleName);
                    if (module == null || module.isDisposed()) {
                        return;
                    }

                    ProjectSettingsService.getInstance(project).openContentEntriesSettings(module);
                }
            }
        );
    }
}
