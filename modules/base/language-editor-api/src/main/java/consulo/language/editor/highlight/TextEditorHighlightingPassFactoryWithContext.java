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
package consulo.language.editor.highlight;

import consulo.codeEditor.Editor;
import consulo.language.psi.PsiFile;
import consulo.ui.annotation.RequiredUIAccess;

import org.jspecify.annotations.Nullable;

/**
 * Extension of {@link TextEditorHighlightingPassFactory} for factories that require
 * state captured on the EDT before pass creation on a background thread.
 * <p>
 * The highlighting framework calls {@link #getContextFromUI} on the EDT once per
 * highlighting cycle and stores the result in the
 * {@link consulo.language.editor.impl.highlight.HighlightingSession}.
 * The context is then passed directly to {@link #createHighlightingPass(PsiFile, Editor, Object)}
 * on the background thread — no session lookup needed inside the factory.
 *
 * @param <C> the type of the EDT-captured context
 * @author VISTALL
 * @since 2026-04-04
 */
public interface TextEditorHighlightingPassFactoryWithContext<C> extends TextEditorHighlightingPassFactory {
    /**
     * Called on the EDT to capture UI-only state (e.g. undo availability, caret selection)
     * needed when creating the highlighting pass on a background thread.
     *
     * @param editor the editor being highlighted
     * @return an opaque context object passed to {@link #createHighlightingPass(PsiFile, Editor, Object)};
     *         may be {@code null} if no EDT context is needed for this cycle
     */
    @RequiredUIAccess
    @Nullable C getContextFromUI(Editor editor);

    /**
     * Creates the highlighting pass on a background thread, with the EDT-captured context
     * passed directly as a parameter.
     *
     * @param file    the PSI file being highlighted
     * @param editor  the editor being highlighted
     * @param context the value returned by {@link #getContextFromUI} on the EDT, or {@code null}
     * @return the highlighting pass, or {@code null} to skip this pass
     */
    @Nullable TextEditorHighlightingPass createHighlightingPass(PsiFile file, Editor editor, @Nullable C context);

    @Override
    default @Nullable TextEditorHighlightingPass createHighlightingPass(PsiFile file, Editor editor) {
        return createHighlightingPass(file, editor, null);
    }
}
