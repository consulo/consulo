/*
 * Copyright 2013-2025 consulo.io
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
package consulo.fileEditor.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.application.dumb.DumbAware;
import consulo.component.PropertiesComponent;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.ReadMeFileProvider;
import consulo.fileEditor.TextEditorWithPreview;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2025-01-21
 */
@ExtensionImpl(order = "last")
public class ReadMeOpenActivity implements PostStartupActivity, DumbAware {
    private static final String SHOW_TIME_KEY = "ReadMeOpenActivity.showTime";

    @Override
    public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        PropertiesComponent component = ProjectPropertiesComponent.getInstance(project);

        long time = component.getLong(SHOW_TIME_KEY, 0);
        if (time != 0) {
            return;
        }

        Path projectPath = Path.of(project.getBasePath());

        VirtualFile targetFile = project.getExtensionPoint(ReadMeFileProvider.class).computeSafeIfAny(readMeFileProvider -> {
            Path readmePath = readMeFileProvider.resolveFile(projectPath);
            if (readmePath == null || !Files.exists(readmePath)) {
                return null;
            }

            VirtualFile virtualFile = ReadAction.compute(() -> LocalFileSystem.getInstance().refreshAndFindFileByNioFile(readmePath));
            if (virtualFile != null) {
                return virtualFile;
            }

            return null;
        });
        
        if (targetFile == null) {
            return;
        }

        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);

        uiAccess.give(() -> {
            FileEditor[] fileEditors = fileEditorManager.openFile(targetFile, true);
            for (FileEditor fileEditor : fileEditors) {
                if (fileEditor instanceof TextEditorWithPreview textEditorWithPreview) {
                    textEditorWithPreview.switchToPreview();
                }
            }
        });

        component.setValue(SHOW_TIME_KEY, System.currentTimeMillis(), 0);
    }
}
