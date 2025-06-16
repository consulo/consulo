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

import consulo.application.util.DateFormatUtil;
import consulo.execution.test.TestStateStorage;
import consulo.execution.test.sm.TestHistoryConfiguration;
import consulo.execution.test.sm.runner.SMTRunnerConsoleProperties;
import consulo.execution.test.sm.ui.SMTestRunnerResultsForm;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImportTestsFromHistoryAction extends AbstractImportTestsAction {
    private String myFileName;

    public ImportTestsFromHistoryAction(@Nullable SMTRunnerConsoleProperties properties, Project project, String name) {
        super(properties, getPresentableText(project, name), getPresentableText(project, name), getIcon(project, name));
        myFileName = name;
    }

    @Nullable
    private static Image getIcon(Project project, String name) {
        return TestHistoryConfiguration.getInstance(project).getIcon(name);
    }

    private static LocalizeValue getPresentableText(Project project, String name) {
        String nameWithoutExtension = FileUtil.getNameWithoutExtension(name);
        int lastIndexOf = nameWithoutExtension.lastIndexOf(" - ");
        if (lastIndexOf > 0) {
            String date = nameWithoutExtension.substring(lastIndexOf + 3);
            try {
                Date creationDate = new SimpleDateFormat(SMTestRunnerResultsForm.HISTORY_DATE_FORMAT).parse(date);
                String configurationName = TestHistoryConfiguration.getInstance(project).getConfigurationName(name);
                return LocalizeValue.localizeTODO(
                    (configurationName != null ? configurationName : nameWithoutExtension.substring(0, lastIndexOf)) +
                        " (" + DateFormatUtil.formatDateTime(creationDate) + ")"
                );
            }
            catch (ParseException ignore) {
            }
        }
        return LocalizeValue.of(nameWithoutExtension);
    }

    @Nullable
    @Override
    public VirtualFile getFile(@Nonnull Project project) {
        return LocalFileSystem.getInstance().findFileByPath(TestStateStorage.getTestHistoryRoot(project).getPath() + "/" + myFileName);
    }
}
