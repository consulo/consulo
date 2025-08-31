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
package consulo.language.editor.impl.internal.intention;

import consulo.codeEditor.*;
import consulo.codeEditor.util.RangeBlinker;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.util.Segment;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.undoRedo.util.UndoUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.FileType;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class ActionUsagePanel extends JPanel implements Disposable {
  private static final String SPOT_MARKER = "spot";
  protected final EditorEx myEditor;
  protected final RangeBlinker myRangeBlinker;


  public ActionUsagePanel() {
    myEditor = (EditorEx)createEditor("", 10, 3, -1);
    setLayout(new BorderLayout());
    add(myEditor.getComponent(), BorderLayout.CENTER);
    TextAttributes blinkAttributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.BLINKING_HIGHLIGHTS_ATTRIBUTES);
    myRangeBlinker = new RangeBlinker(myEditor, blinkAttributes, Integer.MAX_VALUE);
  }

  protected static Editor createEditor(String text, int column, int line, int selectedLine) {
    EditorFactory editorFactory = EditorFactory.getInstance();
    Document editorDocument = editorFactory.createDocument(text);
    EditorEx editor = (EditorEx)editorFactory.createViewer(editorDocument);
    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    editor.setColorsScheme(scheme);
    EditorSettings settings = editor.getSettings();
    settings.setWhitespacesShown(true);
    settings.setLineMarkerAreaShown(false);
    settings.setIndentGuidesShown(false);
    settings.setFoldingOutlineShown(false);
    settings.setAdditionalColumnsCount(0);
    settings.setAdditionalLinesCount(0);
    settings.setRightMarginShown(true);
    settings.setRightMargin(60);

    LogicalPosition pos = new LogicalPosition(line, column);
    editor.getCaretModel().moveToLogicalPosition(pos);
    if (selectedLine >= 0) {
      editor.getSelectionModel().setSelection(editorDocument.getLineStartOffset(selectedLine), editorDocument.getLineEndOffset(selectedLine));
    }

    return editor;
  }

  public void reset(String usageText, FileType fileType) {
    reinitViews();
    SwingUtilities.invokeLater(() -> {
      if (myEditor.isDisposed()) return;
      UndoUtil.writeInRunUndoTransparentAction(() -> configureByText(usageText, fileType));
    });
  }

  private void configureByText(String usageText, FileType fileType) {
    Document document = myEditor.getDocument();
    String text = StringUtil.convertLineSeparators(usageText);
    document.replaceString(0, document.getTextLength(), text);
    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    myEditor.setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(fileType, scheme, null));
    setupSpots(document);
  }

  private void setupSpots(Document document) {
    List<Segment> markers = new ArrayList<>();
    while (true) {
      String text = document.getText();
      final int spotStart = text.indexOf("<" + SPOT_MARKER + ">");
      if (spotStart < 0) break;
      final int spotEnd = text.indexOf("</" + SPOT_MARKER + ">", spotStart);
      if (spotEnd < 0) break;

      document.deleteString(spotEnd, spotEnd + SPOT_MARKER.length() + 3);
      document.deleteString(spotStart, spotStart + SPOT_MARKER.length() + 2);
      Segment spotMarker = new Segment() {
        @Override
        public int getStartOffset() {
          return spotStart;
        }

        @Override
        public int getEndOffset() {
          return spotEnd - SPOT_MARKER.length() - 2;
        }
      };
      markers.add(spotMarker);
    }
    myRangeBlinker.resetMarkers(markers);
    if (!markers.isEmpty()) {
      myRangeBlinker.startBlinking();
    }
  }

  @Override
  public void dispose() {
    myRangeBlinker.stopBlinking();
    EditorFactory editorFactory = EditorFactory.getInstance();
    editorFactory.releaseEditor(myEditor);
  }

  private void reinitViews() {
    myEditor.reinitSettings();
    myEditor.getMarkupModel().removeAllHighlighters();
  }
}
