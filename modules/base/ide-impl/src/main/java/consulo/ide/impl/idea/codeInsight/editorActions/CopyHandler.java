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

package consulo.ide.impl.idea.codeInsight.editorActions;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.*;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.openapi.editor.actions.CopyAction;
import consulo.ide.impl.idea.openapi.editor.impl.EditorCopyPasteHelperImpl;
import consulo.language.editor.action.CopyPastePreProcessor;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.CopyPasteManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;

@ExtensionImpl
public class CopyHandler extends EditorActionHandler implements ExtensionEditorActionHandler {
  private EditorActionHandler myOriginalAction;

  @Override
  public void init(@Nullable EditorActionHandler originalHandler) {
    myOriginalAction = originalHandler;
  }

  @Nonnull
  @Override
  public String getActionId() {
    return IdeActions.ACTION_EDITOR_COPY;
  }

  @Override
  public void doExecute(final Editor editor, Caret caret, final DataContext dataContext) {
    assert caret == null : "Invocation of 'copy' operation for specific caret is not supported";
    final Project project = DataManager.getInstance().getDataContext(editor.getComponent()).getData(Project.KEY);
    if (project == null) {
      if (myOriginalAction != null) {
        myOriginalAction.execute(editor, null, dataContext);
      }
      return;
    }
    final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) {
      if (myOriginalAction != null) {
        myOriginalAction.execute(editor, null, dataContext);
      }
      return;
    }

    final SelectionModel selectionModel = editor.getSelectionModel();
    if (!selectionModel.hasSelection(true)) {
      if (Registry.is(CopyAction.SKIP_COPY_AND_CUT_FOR_EMPTY_SELECTION_KEY)) {
        return;
      }
      editor.getCaretModel().runForEachCaret(caret1 -> selectionModel.selectLineAtCaret());
      if (!selectionModel.hasSelection(true)) return;
      editor.getCaretModel().runForEachCaret(caret12 -> EditorActionUtil.moveCaretToLineStartIgnoringSoftWraps(editor));
    }

    PsiDocumentManager.getInstance(project).commitAllDocuments();

    final int[] startOffsets = selectionModel.getBlockSelectionStarts();
    final int[] endOffsets = selectionModel.getBlockSelectionEnds();

    List<TextBlockTransferableData> transferableDatas = new ArrayList<TextBlockTransferableData>();
    for (CopyPastePostProcessor<? extends TextBlockTransferableData> processor : CopyPastePostProcessor.EP_NAME.getExtensionList()) {
      transferableDatas.addAll(processor.collectTransferableData(file, editor, startOffsets, endOffsets));
    }

    String text = editor.getCaretModel().supportsMultipleCarets() ? EditorCopyPasteHelperImpl.getSelectedTextForClipboard(editor, transferableDatas) : selectionModel.getSelectedText();
    String rawText = TextBlockTransferable.convertLineSeparators(text, "\n", transferableDatas);
    String escapedText = null;
    for (CopyPastePreProcessor processor : CopyPastePreProcessor.EP_NAME.getExtensionList()) {
      escapedText = processor.preprocessOnCopy(file, startOffsets, endOffsets, rawText);
      if (escapedText != null) {
        break;
      }
    }
    final Transferable transferable = new TextBlockTransferable(escapedText != null ? escapedText : rawText, transferableDatas, escapedText != null ? new RawText(rawText) : null);
    CopyPasteManager.getInstance().setContents(transferable);
    if (editor instanceof EditorEx) {
      EditorEx ex = (EditorEx)editor;
      if (ex.isStickySelection()) {
        ex.setStickySelection(false);
      }
    }
  }
}
