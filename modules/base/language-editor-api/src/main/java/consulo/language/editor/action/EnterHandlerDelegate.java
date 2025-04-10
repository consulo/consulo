/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.editor.action;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.component.extension.ExtensionPointName;
import consulo.util.lang.ref.Ref;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface EnterHandlerDelegate {
    enum Result {
        Default,
        Continue,
        DefaultForceIndent,
        DefaultSkipIndent,
        Stop
    }

    /**
     * Called before the actual Enter processing is done.
     * <b>Important Note: A document associated with the editor may have modifications which are not reflected yet in the PSI file. If any
     * operations with PSI are needed including a search for PSI elements, the document must be committed first to update the PSI.
     * For example:</b>
     * <code><pre>
     *   PsiDocumentManager.getInstance(file.getProject()).commitDocument(editor.getDocument);
     * </pre></code>
     *
     * @param file            The PSI file associated with the document.
     * @param editor          The editor.
     * @param caretOffset     A reference to the current caret offset in the document.
     * @param caretAdvance    A reference to the number of columns by which the caret must be moved forward.
     * @param dataContext     The data context passed to the enter handler.
     * @param originalHandler The original handler.
     * @return One of <code>{@link Result} values.</code>
     */
    Result preprocessEnter(
        @Nonnull final PsiFile file,
        @Nonnull final Editor editor,
        @Nonnull final Ref<Integer> caretOffset,
        @Nonnull final Ref<Integer> caretAdvance,
        @Nonnull final DataContext dataContext,
        @Nullable final EditorActionHandler originalHandler
    );

    /**
     * Called at the end of Enter handling after line feed insertion and indentation adjustment.
     * <p>
     * <b>Important Note: A document associated with the editor has modifications which are not reflected yet in the PSI file. If any
     * operations with PSI are needed including a search for PSI elements, the document must be committed first to update the PSI.
     * For example:</b>
     * <code><pre>
     *   PsiDocumentManager.getInstance(file.getProject()).commitDocument(editor.getDocument);
     * </pre></code>
     *
     * @param file        The PSI file associated with the document.
     * @param editor      The editor.
     * @param dataContext The data context passed to the Enter handler.
     * @return One of <code>{@link Result} values.</code>
     * @see DataContext
     * @see PsiDocumentManager
     */
    Result postProcessEnter(@Nonnull PsiFile file, @Nonnull Editor editor, @Nonnull DataContext dataContext);
}
