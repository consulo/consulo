/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.actions;

import consulo.language.editor.PlatformDataKeys;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ExporterToTextFile;
import consulo.ide.impl.idea.ide.util.ExportToFileUtil;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

public class ExportToTextFileAction extends AnAction {
    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        ExporterToTextFile exporterToTextFile = getExporter(e.getDataContext());
        if (exporterToTextFile == null) {
            return;
        }
        if (!exporterToTextFile.canExport()) {
            return;
        }

        export(project, exporterToTextFile);
    }

    @RequiredUIAccess
    public static void export(Project project, ExporterToTextFile exporter) {
        final ExportToFileUtil.ExportDialogBase dlg = new ExportToFileUtil.ExportDialogBase(project, exporter);

        dlg.show();
        if (!dlg.isOK()) {
            return;
        }

        ExportToFileUtil.exportTextToFile(project, dlg.getFileName(), dlg.getText());
        exporter.exportedTo(dlg.getFileName());
    }

    protected ExporterToTextFile getExporter(DataContext dataContext) {
        return dataContext.getData(PlatformDataKeys.EXPORTER_TO_TEXT_FILE);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        ExporterToTextFile exporterToTextFile = getExporter(e.getDataContext());
        presentation.setEnabled(e.hasData(Project.KEY) && exporterToTextFile != null && exporterToTextFile.canExport());
    }
}
