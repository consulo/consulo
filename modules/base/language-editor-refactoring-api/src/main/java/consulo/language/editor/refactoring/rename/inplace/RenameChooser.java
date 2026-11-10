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
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.ex.popup.event.JBPopupAdapter;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.util.lang.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author anna
 * @since 2012-01-11
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

  protected void chooserCanceled() {
  }

  public void showChooser(Collection<PsiReference> refs, Collection<Pair<PsiElement, TextRange>> stringUsages) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      runRenameTemplate(RefactoringSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_FOR_FILE ? stringUsages : new ArrayList<Pair<PsiElement, TextRange>>());
      return;
    }

    BaseListPopupStep<String> step = new BaseListPopupStep<>("String occurrences found", CODE_OCCURRENCES, ALL_OCCURRENCES) {
      @Override
      public PopupStep onChosen(String selectedValue, boolean finalChoice) {
        return doFinalStep(() -> runRenameTemplate(ALL_OCCURRENCES.equals(selectedValue) ? stringUsages : new ArrayList<>()));
      }

      @Override
      public void canceled() {
        chooserCanceled();
      }
    };

    highlightOccurrences(refs, stringUsages, false);

    ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);
    popup.addSelectionListener(selectedValue -> highlightOccurrences(refs, stringUsages, ALL_OCCURRENCES.equals(selectedValue)));
    popup.addListener(new JBPopupAdapter() {
      @Override
      public void onClosed(LightweightWindowEvent event) {
        dropHighlighters();
      }
    });

    myEditor.showPopupInBestPositionFor(popup);
  }

  private void highlightOccurrences(Collection<PsiReference> refs, Collection<Pair<PsiElement, TextRange>> stringUsages, boolean all) {
    dropHighlighters();
    MarkupModel markupModel = myEditor.getMarkupModel();

    if (all) {
      for (Pair<PsiElement, TextRange> pair : stringUsages) {
        TextRange textRange = pair.second.shiftRight(pair.first.getTextOffset());
        RangeHighlighter rangeHighlighter =
                markupModel.addRangeHighlighter(textRange.getStartOffset(), textRange.getEndOffset(), HighlighterLayer.SELECTION - 1, myAttributes, HighlighterTargetArea.EXACT_RANGE);
        myRangeHighlighters.add(rangeHighlighter);
      }
    }

    for (PsiReference reference : refs) {
      PsiElement element = reference.getElement();
      if (element == null) continue;
      TextRange textRange = element.getTextRange();
      RangeHighlighter rangeHighlighter =
              markupModel.addRangeHighlighter(textRange.getStartOffset(), textRange.getEndOffset(), HighlighterLayer.SELECTION - 1, myAttributes, HighlighterTargetArea.EXACT_RANGE);
      myRangeHighlighters.add(rangeHighlighter);
    }
  }

  private void dropHighlighters() {
    for (RangeHighlighter highlight : myRangeHighlighters) {
      highlight.dispose();
    }
    myRangeHighlighters.clear();
  }
}
