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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.RealEditor;
import consulo.document.Document;
import consulo.document.internal.DocumentFactory;
import consulo.it.internal.editor.HeadlessEditor;
import consulo.language.editor.internal.EditorFactoryImpl;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Headless {@code EditorFactory}: reuses the real {@link EditorFactoryImpl} (documents, editor event
 * multicaster — which {@code DaemonListeners}/{@code IdeDocumentHistoryImpl} subscribe to at project
 * startup); editor creation answers a {@link HeadlessEditor}, so a test reaches one through this service
 * rather than constructing the implementation itself.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessEditorFactory extends EditorFactoryImpl {
    @Inject
    public HeadlessEditorFactory(Application application, DocumentFactory documentFactory) {
        super(application, documentFactory);
    }

    @Override
    protected RealEditor createEditorImpl(Document document, boolean isViewer, Project project, EditorKind kind) {
        return new HeadlessEditor(document, isViewer, project, kind);
    }
}
