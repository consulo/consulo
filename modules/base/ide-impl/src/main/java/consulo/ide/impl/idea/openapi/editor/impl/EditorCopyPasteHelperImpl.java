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
package consulo.ide.impl.idea.openapi.editor.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.codeInsight.editorActions.TextBlockTransferable;
import consulo.ide.impl.idea.codeInsight.editorActions.TextBlockTransferableData;
import consulo.codeEditor.*;
import consulo.logging.Logger;
import consulo.ide.impl.idea.openapi.editor.*;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.codeEditor.impl.util.EditorImplUtil;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.clipboard.DataTransferType;
import consulo.ui.ex.CopyPasteManager;
import consulo.document.util.TextRange;
import consulo.application.util.LineTokenizer;
import jakarta.inject.Singleton;

import org.jspecify.annotations.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Singleton
@ServiceImpl
public class EditorCopyPasteHelperImpl extends EditorCopyPasteHelper {
  private static final Logger LOG = Logger.getInstance(EditorCopyPasteHelperImpl.class);

  @Override
  @RequiredUIAccess
  public void copySelectionToClipboard(Editor editor) {
    UIAccess.assertIsUIThread();
    List<TextBlockTransferableData> extraData = new ArrayList<TextBlockTransferableData>();
    String s = editor.getCaretModel().supportsMultipleCarets() ? getSelectedTextForClipboard(editor, extraData)
                                                               : editor.getSelectionModel().getSelectedText();
    if (s == null) return;

    s = TextBlockTransferable.convertLineSeparators(s, "\n", extraData);
    Transferable contents = editor.getCaretModel().supportsMultipleCarets() ? new TextBlockTransferable(s, extraData, null) : new StringSelection(s);
    CopyPasteManager.getInstance().setContents(DataTransfer.builder()
      .put(DataTransferType.TEXT, s)
      .put(EditorImplUtil.TRANSFERABLE, contents)
      .build());
  }

  public static String getSelectedTextForClipboard(Editor editor, Collection<TextBlockTransferableData> extraDataCollector) {
    StringBuilder buf = new StringBuilder();
    String separator = "";
    List<Caret> carets = editor.getCaretModel().getAllCarets();
    int[] startOffsets = new int[carets.size()];
    int[] endOffsets = new int[carets.size()];
    for (int i = 0; i < carets.size(); i++) {
      buf.append(separator);
      String caretSelectedText = carets.get(i).getSelectedText();
      startOffsets[i] = buf.length();
      if (caretSelectedText != null) {
        buf.append(caretSelectedText);
      }
      endOffsets[i] = buf.length();
      separator = "\n";
    }
    extraDataCollector.add(new CaretStateTransferableData(startOffsets, endOffsets));
    return buf.toString();
  }

  @Override
  @RequiredUIAccess
  public TextRange @Nullable [] pasteFromClipboard(Editor editor) {
    // the clipboard answers as a future, and this one has to give a value back - only the payload this process
    // already holds can be pasted here, which is what every caller of this overload means anyway
    DataTransfer contents = CopyPasteManager.getInstance().getLocalContents();
    return contents.isEmpty() ? null : pasteDataTransfer(editor, contents);
  }

  @Override
  public TextRange @Nullable [] pasteTransferable(final Editor editor, Transferable content) {
    String text = getStringContent(content);
    if (text == null) return null;

    return pasteText(editor, text, caretStateOf(content));
  }

  /**
   * The awt free entry point. A payload which came from another application carries text and nothing else, and
   * the caret layout of a multi caret copy only ever exists in a payload this process wrote itself.
   */
  @Override
  public TextRange @Nullable [] pasteDataTransfer(final Editor editor, DataTransfer content) {
    Transferable transferable = content.get(EditorImplUtil.TRANSFERABLE);
    if (transferable != null) {
      return pasteTransferable(editor, transferable);
    }

    String text = content.get(DataTransferType.TEXT);
    if (text == null) return null;

    return pasteText(editor, text, null);
  }

  private static @Nullable CaretStateTransferableData caretStateOf(Transferable content) {
    try {
      return content.isDataFlavorSupported(CaretStateTransferableData.FLAVOR)
             ? (CaretStateTransferableData)content.getTransferData(CaretStateTransferableData.FLAVOR) : null;
    }
    catch (Exception e) {
      LOG.error(e);
      return null;
    }
  }

  private static TextRange @Nullable [] pasteText(
    final Editor editor,
    String text,
    @Nullable CaretStateTransferableData caretData
  ) {

    if (editor.getCaretModel().supportsMultipleCarets()) {
      int caretCount = editor.getCaretModel().getCaretCount();
      if (caretCount == 1 && editor.isColumnMode()) {
        int pastedLineCount = LineTokenizer.calcLineCount(text, true);
        EditorModificationUtil.deleteSelectedText(editor);
        Caret caret = editor.getCaretModel().getPrimaryCaret();
        for (int i = 0; i < pastedLineCount - 1; i++) {
          caret = caret.clone(false);
          if (caret == null) {
            break;
          }
        }
        caretCount = editor.getCaretModel().getCaretCount();
      }
      final TextRange[] ranges = new TextRange[caretCount];
      final Iterator<String> segments = new ClipboardTextPerCaretSplitter().split(text, caretData, caretCount).iterator();
      final int[] index = {0};
      editor.getCaretModel().runForEachCaret(new CaretAction() {
        @Override
        public void perform(Caret caret) {
          String segment = segments.next();
          int caretOffset = caret.getOffset();
          ranges[index[0]++] = new TextRange(caretOffset, caretOffset + segment.length());
          EditorModificationUtil.insertStringAtCaret(editor, segment, false, true);
        }
      });
      return ranges;
    }
    else {
      int caretOffset = editor.getCaretModel().getOffset();
      EditorModificationUtil.insertStringAtCaret(editor, text, false, true);
      return new TextRange[] { new TextRange(caretOffset, caretOffset + text.length())};
    }
  }

  private static @Nullable String getStringContent(Transferable content) {
    RawText raw = RawText.fromTransferable(content);
    if (raw != null) return raw.rawText;

    try {
      return (String)content.getTransferData(DataFlavor.stringFlavor);
    }
    catch (UnsupportedFlavorException ignore) { }
    catch (IOException ignore) { }

    return null;
  }
}
