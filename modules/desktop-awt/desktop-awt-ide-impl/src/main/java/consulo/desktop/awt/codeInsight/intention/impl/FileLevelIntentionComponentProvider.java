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
package consulo.desktop.awt.codeInsight.intention.impl;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.impl.internal.daemon.FileLevelHighlightComponentBuilder;
import consulo.language.editor.impl.internal.daemon.FileLevelHighlightComponentProvider;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ComponentContainer;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 */
@ServiceImpl(profiles = ComponentProfiles.AWT)
public class FileLevelIntentionComponentProvider implements FileLevelHighlightComponentProvider {
    private final Project myProject;

    @Inject
    public FileLevelIntentionComponentProvider(Project project) {
        myProject = project;
    }

    @Override
    @RequiredUIAccess
    public @Nullable Disposable createComponent(FileEditor fileEditor, FileLevelHighlightComponentBuilder builder) {
        ComponentContainer component = new FileLevelIntentionComponent(builder);
        return FileEditorManager.getInstance(myProject).addTopComponent(fileEditor, component);
    }
}
