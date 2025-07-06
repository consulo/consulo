/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.test.sm.action;

import consulo.execution.test.TestStateStorage;
import consulo.execution.test.sm.TestHistoryConfiguration;
import consulo.execution.test.sm.localize.SMTestLocalize;
import consulo.execution.test.sm.runner.SMTRunnerConsoleProperties;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import consulo.util.collection.ContainerUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.*;

public class ImportTestsGroup extends ActionGroup {
    private SMTRunnerConsoleProperties myProperties;

    public ImportTestsGroup() {
        super(
            SMTestLocalize.smTestRunnerImportTestGroupHistory(),
            SMTestLocalize.smTestRunnerImportTestGroupOpenRecentSession(),
            PlatformIconGroup.vcsHistory()
        );
        setPopup(true);
    }

    public ImportTestsGroup(SMTRunnerConsoleProperties properties) {
        this();
        myProperties = properties;
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        if (e == null) {
            return EMPTY_ARRAY;
        }
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return EMPTY_ARRAY;
        }
        Collection<String> filePaths = TestHistoryConfiguration.getInstance(project).getFiles();
        File testHistoryRoot = TestStateStorage.getTestHistoryRoot(project);
        List<File> fileNames = new ArrayList<>(ContainerUtil.map(filePaths, fileName -> new File(testHistoryRoot, fileName)));
        Collections.sort(fileNames, (f1, f2) -> f1.lastModified() > f2.lastModified() ? -1 : 1);
        int historySize = fileNames.size();
        AnAction[] actions = new AnAction[historySize + 2];
        for (int i = 0; i < historySize; i++) {
            actions[i] = new ImportTestsFromHistoryAction(myProperties, project, fileNames.get(i).getName());
        }
        actions[historySize] = AnSeparator.getInstance();
        actions[historySize + 1] = new ImportTestsFromFileAction(myProperties);
        return actions;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getData(Project.KEY) != null);
    }
}
