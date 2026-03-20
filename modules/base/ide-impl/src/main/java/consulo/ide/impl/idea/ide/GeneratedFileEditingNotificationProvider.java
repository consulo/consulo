/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.FileEditor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.content.GeneratedSourcesFilter;
import consulo.ui.NotificationType;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;

import java.util.function.Supplier;

/**
 * @author nik
 */
@ExtensionImpl
public class GeneratedFileEditingNotificationProvider implements EditorNotificationProvider, DumbAware {
    private final Project myProject;

    @Inject
    public GeneratedFileEditingNotificationProvider(Project project) {
        myProject = project;
    }

    
    @Override
    public String getId() {
        return "file-is-generated";
    }

    @RequiredReadAction
    @Override
    public @Nullable EditorNotificationBuilder buildNotification(VirtualFile file, FileEditor fileEditor, Supplier<EditorNotificationBuilder> builderFactory) {
        if (!GeneratedSourcesFilter.isGeneratedSourceByAnyFilter(file, myProject)) {
            return null;
        }

        EditorNotificationBuilder builder = builderFactory.get();
        builder.withType(NotificationType.WARNING);
        builder.withText(LocalizeValue.localizeTODO("Generated source files should not be edited. The changes will be lost when sources are regenerated."));
        return builder;
    }
}
