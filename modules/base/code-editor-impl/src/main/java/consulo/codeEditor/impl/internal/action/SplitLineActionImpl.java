/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.codeEditor.impl.internal.action;

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.action.*;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.util.lang.CharArrayUtil;
import jakarta.annotation.Nonnull;

/**
 * @author max
 */
@ActionImpl(id = IdeActions.ACTION_EDITOR_SPLIT)
public class SplitLineActionImpl extends EditorAction {
    private static class Handler extends EditorWriteActionHandler {
        public Handler() {
            super(true);
        }

        @Override
        public boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
            return getEnterHandler().isEnabled(editor, caret, dataContext)
                && !((EditorEx) editor).isEmbeddedIntoDialogWrapper();
        }

        @Override
        @RequiredWriteAction
        public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
            CopyPasteManager.getInstance().stopKillRings();
            Document document = editor.getDocument();
            RangeMarker rangeMarker =
                document.createRangeMarker(editor.getCaretModel().getOffset(), editor.getCaretModel().getOffset());
            CharSequence chars = document.getCharsSequence();

            int offset = editor.getCaretModel().getOffset();
            int lineStart = document.getLineStartOffset(document.getLineNumber(offset));

            CharSequence beforeCaret = chars.subSequence(lineStart, offset);

            if (CharArrayUtil.containsOnlyWhiteSpaces(beforeCaret)) {
                String strToInsert = "";
                if (beforeCaret != null) {
                    strToInsert += beforeCaret.toString();
                }
                strToInsert += "\n";
                document.insertString(lineStart, strToInsert);
                editor.getCaretModel().moveToOffset(offset);
            }
            else {
                DataManager.getInstance().saveInDataContext(dataContext, SplitLineAction.SPLIT_LINE_KEY, true);
                try {
                    getEnterHandler().execute(editor, caret, dataContext);
                }
                finally {
                    DataManager.getInstance().saveInDataContext(dataContext, SplitLineAction.SPLIT_LINE_KEY, null);
                }

                editor.getCaretModel().moveToOffset(Math.min(document.getTextLength(), rangeMarker.getStartOffset()));
                editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
            }

        }

        private static EditorActionHandler getEnterHandler() {
            return EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_ENTER);
        }
    }

    public SplitLineActionImpl() {
        super(ActionLocalize.actionEditorsplitlineText(), new Handler());
        setEnabledInModalContext(false);
    }
}
