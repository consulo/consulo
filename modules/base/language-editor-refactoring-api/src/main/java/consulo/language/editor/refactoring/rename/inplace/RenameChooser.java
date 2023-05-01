/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.rename.inplace;

import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.document.util.TextRange;
import consulo.language.editor.refactoring.RefactoringSettings;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupAdapter;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * User: anna
 * Date: 1/11/12
 */
abstract class RenameChooser {
  private static final String CODE_OCCURRENCES = "Rename code occurrences";
  private static final String ALL_OCCURRENCES = "Rename all occurrences";
  private final Set<RangeHighlighter> myRangeHighlighters = new HashSet<RangeHighlighter>();
  private final Editor myEditor;
  private final TextAttributes myAttributes;

  public RenameChooser(Editor editor) {
    myEditor = editor;
    myAttributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
  }

  protected abstract void runRenameTemplate(Collection<Pair<PsiElement, TextRange>> stringUsages);

  public void showChooser(final Collection<PsiReference> refs, final Collection<Pair<PsiElement, TextRange>> stringUsages) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      runRenameTemplate(RefactoringSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_FOR_FILE ? stringUsages : new ArrayList<Pair<PsiElement, TextRange>>());
      return;
    }

    JBPopup popup = JBPopupFactory.getInstance().createPopupChooserBuilder(ContainerUtil.newArrayList(CODE_OCCURRENCES, ALL_OCCURRENCES)).setTitle("String occurrences found").setMovable(false)
            .setResizable(false).setRequestFocus(true).setItemSelectedCallback(selectedValue -> {
              dropHighlighters();
              final MarkupModel markupModel = myEditor.getMarkupModel();

              if (ALL_OCCURRENCES.equals(selectedValue)) {
                for (Pair<PsiElement, TextRange> pair : stringUsages) {
                  final TextRange textRange = pair.second.shiftRight(pair.first.getTextOffset());
                  final RangeHighlighter rangeHighlighter =
                          markupModel.addRangeHighlighter(textRange.getStartOffset(), textRange.getEndOffset(), HighlighterLayer.SELECTION - 1, myAttributes, HighlighterTargetArea.EXACT_RANGE);
                  myRangeHighlighters.add(rangeHighlighter);
                }
              }

              for (PsiReference reference : refs) {
                final PsiElement element = reference.getElement();
                if (element == null) continue;
                final TextRange textRange = element.getTextRange();
                final RangeHighlighter rangeHighlighter =
                        markupModel.addRangeHighlighter(textRange.getStartOffset(), textRange.getEndOffset(), HighlighterLayer.SELECTION - 1, myAttributes, HighlighterTargetArea.EXACT_RANGE);
                myRangeHighlighters.add(rangeHighlighter);
              }

              runRenameTemplate(ALL_OCCURRENCES.equals(selectedValue) ? stringUsages : new ArrayList<>());
            }).addListener(new JBPopupAdapter() {
              @Override
              public void onClosed(LightweightWindowEvent event) {
                dropHighlighters();
              }
            }).createPopup();

    myEditor.showPopupInBestPositionFor(popup);
  }


  private void dropHighlighters() {
    for (RangeHighlighter highlight : myRangeHighlighters) {
      highlight.dispose();
    }
    myRangeHighlighters.clear();
  }
}
