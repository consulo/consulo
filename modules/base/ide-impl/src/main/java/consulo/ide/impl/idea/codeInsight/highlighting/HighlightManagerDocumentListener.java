/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.codeInsight.highlighting;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.EditorDocumentListener;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.language.editor.highlight.HighlightManager;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author VISTALL
 * @since 21-Jun-22
 */
@ExtensionImpl
public class HighlightManagerDocumentListener implements EditorDocumentListener {
  private final Provider<HighlightManager> myHighlightManager;

  @Inject
  public HighlightManagerDocumentListener(Provider<HighlightManager> highlightManager) {
    myHighlightManager = highlightManager;
  }

  @Override
  public void documentChanged(DocumentEvent event) {
    HighlightManagerImpl manager = (HighlightManagerImpl)myHighlightManager.get();

    Document document = event.getDocument();
    Editor[] editors = EditorFactory.getInstance().getEditors(document);
    for (Editor editor : editors) {
      Map<RangeHighlighter, HighlightManagerImpl.HighlightInfo> map = manager.getHighlightInfoMap(editor, false);
      if (map == null) return;

      ArrayList<RangeHighlighter> highlightersToRemove = new ArrayList<RangeHighlighter>();
      for (RangeHighlighter highlighter : map.keySet()) {
        HighlightManagerImpl.HighlightInfo info = map.get(highlighter);
        if (!info.editor.getDocument().equals(document)) continue;
        if ((info.flags & HighlightManager.HIDE_BY_TEXT_CHANGE) != 0) {
          highlightersToRemove.add(highlighter);
        }
      }

      for (RangeHighlighter highlighter : highlightersToRemove) {
        manager.removeSegmentHighlighter(editor, highlighter);
      }
    }
  }
}
