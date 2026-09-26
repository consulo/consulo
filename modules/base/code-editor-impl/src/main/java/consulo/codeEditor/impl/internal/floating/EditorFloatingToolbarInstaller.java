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
package consulo.codeEditor.impl.internal.floating;

import consulo.application.concurrent.coroutine.DisposableCoroutineScope;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.toolbar.floating.FloatingToolbarComponent;
import consulo.codeEditor.toolbar.floating.FloatingToolbarProvider;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.FileDocumentManager;
import consulo.project.Project;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;

public final class EditorFloatingToolbarInstaller {
    private EditorFloatingToolbarInstaller() {
    }

    public static boolean mayShowToolbar(Editor editor) {
        return !editor.isOneLineMode() && editor.getEditorKind() != EditorKind.DIFF && isFileEditor(editor);
    }

    private static boolean isFileEditor(Editor editor) {
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        VirtualFile virtualFile = documentManager.getFile(editor.getDocument());
        return virtualFile != null && virtualFile.isValid();
    }

    @RequiredUIAccess
    public static void install(Editor editor, Project project, Disposable parentDisposable, EditorFloatingToolbarFactory factory) {
        project.getExtensionPoint(FloatingToolbarProvider.class).forEach(provider -> {
            DataContext dataContext = editor.getDataContext();
            Disposable providerDisposable = Disposable.newDisposable(provider.getClass().getName());
            Disposer.register(parentDisposable, providerDisposable);
            DisposableCoroutineScope.launchAsync(
                project.coroutineContext(),
                providerDisposable,
                () -> provider.isApplicableAsync(dataContext)
                    .then(UIAction.<Boolean, Void>apply(applicable -> {
                        if (Boolean.TRUE.equals(applicable) && !Disposer.isDisposed(providerDisposable)) {
                            FloatingToolbarComponent component = factory.createToolbar(provider, dataContext, providerDisposable);
                            provider.register(dataContext, component, providerDisposable);
                        }
                        return null;
                    }))
            );
        });
    }
}
