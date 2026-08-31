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
package consulo.language.editor.inject;

import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.imaginary.ImaginaryEditor;
import consulo.document.DocumentWindow;
import consulo.language.psi.PsiFile;
import consulo.project.Project;

/**
 * The imaginary (check-free) counterpart of an injected editor window: an {@link ImaginaryEditor}
 * over the injected {@link DocumentWindow}. Used by background update paths, where the real
 * {@code EditorWindow} cannot be created (it requires a real host editor) and no UI is needed -
 * only the injected file, the injected document and offset translation.
 */
public class ImaginaryEditorWindow extends ImaginaryEditor implements EditorWindow {
    private final Editor myDelegate;
    private final PsiFile myInjectedFile;
    private final DocumentWindow myDocumentWindow;

    public ImaginaryEditorWindow(Project project, Editor delegate, PsiFile injectedFile, DocumentWindow documentWindow) {
        super(project, documentWindow);
        myDelegate = delegate;
        myInjectedFile = injectedFile;
        myDocumentWindow = documentWindow;
    }

    @Override
    public boolean isValid() {
        return myDocumentWindow.isValid();
    }

    @Override
    public PsiFile getInjectedFile() {
        return myInjectedFile;
    }

    @Override
    public Editor getDelegate() {
        return myDelegate;
    }

    @Override
    public DocumentWindow getDocument() {
        return myDocumentWindow;
    }

    @Override
    public LogicalPosition hostToInjected(LogicalPosition hPos) {
        int hostOffset = myDelegate.logicalPositionToOffset(hPos);
        return offsetToLogicalPosition(myDocumentWindow.hostToInjected(hostOffset));
    }

    @Override
    public LogicalPosition injectedToHost(LogicalPosition pos) {
        int injectedOffset = logicalPositionToOffset(pos);
        return myDelegate.offsetToLogicalPosition(myDocumentWindow.injectedToHost(injectedOffset));
    }
}
