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
package consulo.desktop.awt.editor.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.impl.internal.LaterInvocator;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.RealEditor;
import consulo.document.Document;
import consulo.document.internal.DocumentFactory;
import consulo.language.editor.internal.EditorFactoryImpl;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 06/12/2020
 */
@Singleton
@ServiceImpl
public class DesktopEditorFactoryImpl extends EditorFactoryImpl {
    @Inject
    public DesktopEditorFactoryImpl(Application application, DocumentFactory documentFactory) {
        super(application, documentFactory);

        LaterInvocator.addModalityStateListener((entering, modalEntity) -> {
            for (Editor editor : myEditors) {
                ((DesktopEditorImpl) editor).beforeModalityStateChanged();
            }
        }, application);
    }

    @Nonnull
    @Override
    protected RealEditor createEditorImpl(@Nonnull Document document, boolean isViewer, Project project, @Nonnull EditorKind kind) {
        return new DesktopEditorImpl(document, isViewer, project, kind);
    }
}
