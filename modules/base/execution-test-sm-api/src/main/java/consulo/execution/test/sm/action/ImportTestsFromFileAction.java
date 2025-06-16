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

import consulo.execution.test.sm.runner.SMTRunnerConsoleProperties;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.IdeaFileChooser;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ImportTestsFromFileAction extends AbstractImportTestsAction {
    public ImportTestsFromFileAction(SMTRunnerConsoleProperties properties) {
        super(
            properties,
            LocalizeValue.localizeTODO((properties == null ? "" : "Import ") + "From File ..."),
            LocalizeValue.localizeTODO("Import tests from file"),
            null
        );
    }

    @Nullable
    @Override
    public VirtualFile getFile(@Nonnull Project project) {
        FileChooserDescriptor xmlDescriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
            .withFileFilter(virtualFile -> "xml".equals(virtualFile.getExtension()));
        xmlDescriptor.withTitleValue(LocalizeValue.localizeTODO("Choose a File with Tests Result"));
        return IdeaFileChooser.chooseFile(xmlDescriptor, project, null);
    }
}
