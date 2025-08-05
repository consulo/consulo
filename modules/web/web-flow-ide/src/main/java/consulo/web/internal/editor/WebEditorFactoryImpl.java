/*
 * Copyright 2013-2020 consulo.io
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
package consulo.web.internal.editor;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.RealEditor;
import consulo.document.Document;
import consulo.document.internal.DocumentFactory;
import consulo.language.editor.internal.EditorFactoryImpl;
import consulo.project.Project;
import consulo.web.internal.ui.editor.WebEditorImpl;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 06/12/2020
 */
@Singleton
@ServiceImpl
public class WebEditorFactoryImpl extends EditorFactoryImpl {
    @Inject
    public WebEditorFactoryImpl(Application application, DocumentFactory documentFactory) {
        super(application, documentFactory);
    }

    @Nonnull
    @Override
    protected RealEditor createEditorImpl(@Nonnull Document document, boolean isViewer, Project project, @Nonnull EditorKind kind) {
        return new WebEditorImpl(document, isViewer, project, kind);
    }
}
