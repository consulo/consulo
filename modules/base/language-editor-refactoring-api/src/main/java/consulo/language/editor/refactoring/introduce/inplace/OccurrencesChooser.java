// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.refactoring.introduce.inplace;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.EditorPopupHelper;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.document.util.TextRange;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.awt.popup.GroupedItemsListRenderer;
import consulo.ui.ex.awt.popup.ListItemDescriptorAdapter;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Consumer;

// Please do not make this class concrete<PsiElement>.
// This prevents languages with polyadic expressions or sequences
// from reusing it, use simpleChooser instead.
public abstract class OccurrencesChooser<T> {
    public interface BaseReplaceChoice {
        /**
         * @return true if more than one element is selected
         */
        boolean isAll();

        /**
         * @param occurrencesCount number of occurrences
         * @return user-readable description of given choice
         */
        @Nls
        String formatDescription(int occurrencesCount);
    }

    public enum ReplaceChoice implements BaseReplaceChoice {
        NO,
        NO_WRITE,
        ALL;

        @Override
        public boolean isAll() {
            return this != NO;
        }

        @Override
        public @Nls String formatDescription(int occurrencesCount) {
            return switch (this) {
                case NO -> RefactoringBundle.message("replace.this.occurrence.only");
                case NO_WRITE -> RefactoringBundle.message("replace.all.occurrences.but.write");
                case ALL -> RefactoringBundle.message("replace.all.occurrences", occurrencesCount);
                default -> throw new IllegalStateException("Unexpected value: " + this);
            };
        }
    }

    public static <T extends PsiElement> OccurrencesChooser<T> simpleChooser(Editor editor) {
        return new OccurrencesChooser<>(editor) {
            @Override
            protected TextRange getOccurrenceRange(T occurrence) {
                return occurrence.getTextRange();
            }
        };
    }

    private final Set<RangeHighlighter> myRangeHighlighters = new HashSet<>();
    private final Editor myEditor;

    public OccurrencesChooser(Editor editor) {
        myEditor = editor;
    }

    public void showChooser(final T selectedOccurrence, final List<T> allOccurrences, final Consumer<? super ReplaceChoice> callback) {
        if (allOccurrences.size() == 1) {
            callback.accept(ReplaceChoice.ALL);
        }
        else {
            Map<ReplaceChoice, List<T>> occurrencesMap = new LinkedHashMap<>();
            occurrencesMap.put(ReplaceChoice.NO, Collections.singletonList(selectedOccurrence));
            occurrencesMap.put(ReplaceChoice.ALL, allOccurrences);
            showChooser(occurrencesMap, callback);
        }
    }

    /**
     * use {@link #showChooser(Map, String, Consumer)}
     */
    @Deprecated
    public void showChooser(final Consumer<? super ReplaceChoice> callback, final Map<ReplaceChoice, List<T>> occurrencesMap) {
        showChooser(occurrencesMap, RefactoringBundle.message("replace.multiple.occurrences.found"), callback);
    }

    public void showChooser(final Map<ReplaceChoice, List<T>> occurrencesMap, @Nonnull Consumer<? super ReplaceChoice> callback) {
        showChooser(occurrencesMap, RefactoringBundle.message("replace.multiple.occurrences.found"), callback);
    }

    public <C extends BaseReplaceChoice> void showChooser(final Map<C, List<T>> occurrencesMap,
                                                          @Nls String title,
                                                          Consumer<? super C> callback) {
        if (occurrencesMap.size() == 1) {
            callback.accept(occurrencesMap.keySet().iterator().next());
            return;
        }
        List<C> model = new ArrayList<>(occurrencesMap.keySet());

        JBPopup popup = JBPopupFactory.getInstance()
            .createPopupChooserBuilder(model)
            .setRenderer(new GroupedItemsListRenderer<C>(new ListItemDescriptorAdapter<C>() {
                @Override
                public @Nullable String getTextFor(C value) {
                    if (value == null) {
                        return "";
                    }
                    return value.formatDescription(occurrencesMap.get(value).size());
                }
            }))
            .setItemSelectedCallback(value -> {
                if (value == null) {
                    return;
                }
                dropHighlighters();
                final MarkupModel markupModel = myEditor.getMarkupModel();
                final List<T> occurrenceList = occurrencesMap.get(value);
                for (T occurrence : occurrenceList) {
                    final TextRange textRange = getOccurrenceRange(occurrence);
                    final RangeHighlighter rangeHighlighter = markupModel.addRangeHighlighter(
                        EditorColors.SEARCH_RESULT_ATTRIBUTES, textRange.getStartOffset(), textRange.getEndOffset(), HighlighterLayer.SELECTION - 1,
                        HighlighterTargetArea.EXACT_RANGE);
                    myRangeHighlighters.add(rangeHighlighter);
                }
            })
            .setTitle(title)
            .setMovable(true)
            .setResizable(false)
            .setRequestFocus(true)
            .setItemChosenCallback(callback)
            .addListener(new JBPopupListener() {
                @Override
                public void onClosed(@Nonnull LightweightWindowEvent event) {
                    dropHighlighters();
                }
            })
            .createPopup();

        EditorPopupHelper.getInstance().showPopupInBestPositionFor(myEditor, popup);
    }

    protected abstract TextRange getOccurrenceRange(T occurrence);

    private void dropHighlighters() {
        for (RangeHighlighter highlight : myRangeHighlighters) {
            highlight.dispose();
        }
        myRangeHighlighters.clear();
    }
}
