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
package consulo.it.internal.editor;

import consulo.codeEditor.impl.CodeEditorBase;
import consulo.codeEditor.impl.CodeEditorCaretModelBase;

/**
 * Headless caret model. Everything the model does - carets, listeners, caret merging, caret states -
 * is document driven and already implemented by {@link CodeEditorCaretModelBase}.
 * <p/>
 * Only a single caret is advertised: caret merging identifies carets by visual position, which is
 * degenerate without a rendering surface, so secondary carets could not be kept apart reliably.
 *
 * @author VISTALL
 */
public class HeadlessCaretModel extends CodeEditorCaretModelBase<HeadlessCaret> {
    public HeadlessCaretModel(CodeEditorBase editor) {
        super(editor);
    }

    @Override
    protected HeadlessCaret createCaret(CodeEditorBase editor, CodeEditorCaretModelBase<HeadlessCaret> model) {
        return new HeadlessCaret(editor, model);
    }

    @Override
    public boolean supportsMultipleCarets() {
        return false;
    }
}
