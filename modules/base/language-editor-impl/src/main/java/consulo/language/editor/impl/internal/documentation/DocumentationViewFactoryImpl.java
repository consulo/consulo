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
package consulo.language.editor.impl.internal.documentation;

import consulo.annotation.component.ServiceImpl;
import consulo.colorScheme.EditorColorsManager;
import consulo.language.editor.internal.DocumentationViewFactory;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
@ServiceImpl
@Singleton
public class DocumentationViewFactoryImpl implements DocumentationViewFactory {
    private final Project myProject;
    private final DocumentationSettings mySettings;
    private final EditorColorsManager myEditorColorsManager;

    @Inject
    public DocumentationViewFactoryImpl(Project project, DocumentationSettings settings, EditorColorsManager editorColorsManager) {
        myProject = project;
        mySettings = settings;
        myEditorColorsManager = editorColorsManager;
    }

    @RequiredUIAccess
    @Override
    public DocumentationViewImpl create() {
        return create(browser -> List.of());
    }

    @RequiredUIAccess
    public DocumentationViewImpl create(@Nullable Function<DocumentationBrowser, List<? extends AnAction>> secondaryActions) {
        return new DocumentationViewImpl(myProject, mySettings, myEditorColorsManager, secondaryActions);
    }
}
