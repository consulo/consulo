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
package consulo.desktop.qt.editor.impl.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.EditorFactory;
import consulo.language.editor.ui.EditorBoxBuilder;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.language.editor.ui.internal.EditorBoxBuilderImpl;
import consulo.project.Project;
import consulo.project.ProjectManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
@Singleton
public class DesktopQtEditorBoxBuilderFactory implements EditorBoxBuilderFactory {
    private final EditorFactory myEditorFactory;
    private final ProjectManager myProjectManager;

    @Inject
    public DesktopQtEditorBoxBuilderFactory(EditorFactory editorFactory, ProjectManager projectManager) {
        myEditorFactory = editorFactory;
        myProjectManager = projectManager;
    }

    @Override
    public EditorBoxBuilder create(@Nullable Project project) {
        return new EditorBoxBuilderImpl(
            project,
            myEditorFactory,
            options -> new DesktopQtEditorBoxImpl(options, myEditorFactory, myProjectManager)
        );
    }
}
