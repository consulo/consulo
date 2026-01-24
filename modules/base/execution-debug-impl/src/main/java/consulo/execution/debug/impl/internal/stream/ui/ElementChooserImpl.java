// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.impl.internal.stream.ui;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.EditorPopupHelper;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.stream.ui.ChooserOption;
import consulo.execution.debug.stream.ui.ElementChooser;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.popup.AWTPopupFactory;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Vitaliy.Bibaev
 */
public class ElementChooserImpl<T extends ChooserOption> implements ElementChooser<T> {
    private static final int HIGHLIGHT_LAYER = HighlighterLayer.SELECTION + 1;
    private final Set<RangeHighlighter> myRangeHighlighters = new HashSet<>();
    private final Editor myEditor;
    private final TextAttributes myAttributes;

    public ElementChooserImpl(@Nonnull Editor editor) {
        myEditor = editor;
        TextAttributes searchResultAttributes =
            EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
        ColorValue foreground = editor.getColorsScheme().getDefaultForeground();
        myAttributes = new TextAttributes(foreground, searchResultAttributes.getBackgroundColor(), null,
            searchResultAttributes.getEffectType(), searchResultAttributes.getFontType());
    }

    @Override
    public void show(@Nonnull List<T> options, @Nonnull CallBack<T> callBack) {
        DefaultListModel<T> model = new DefaultListModel<>();
        int maxOffset = -1;
        for (T option : options) {
            model.addElement(option);
            maxOffset = Math.max(maxOffset, option.rangeStream().mapToInt(TextRange::getEndOffset).max().orElse(-1));
        }

        JBList<T> list = new JBList<>(model);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                //noinspection unchecked
                setText(((T) value).getText());
                return this;
            }
        });

        list.addListSelectionListener(e -> {
            T selectedValue = list.getSelectedValue();
            if (selectedValue == null) {
                return;
            }
            dropHighlighters();
            selectedValue.rangeStream().forEach(x -> {
                RangeHighlighter highlighter = myEditor.getMarkupModel()
                    .addRangeHighlighter(x.getStartOffset(), x.getEndOffset(), HIGHLIGHT_LAYER, myAttributes, HighlighterTargetArea.EXACT_RANGE);
                myRangeHighlighters.add(highlighter);
            });
        });

        JBPopup popup = ((AWTPopupFactory) JBPopupFactory.getInstance()).createListPopupBuilder(list)
            .setTitle(XDebuggerLocalize.multipleChainsPopupTitle().get())
            .setMovable(true)
            .setResizable(false)
            .setRequestFocus(true)
            .setItemChosenCallback((v) -> callBack.chosen(v))
            .addListener(new JBPopupListener() {
                @Override
                public void onClosed(@Nonnull LightweightWindowEvent event) {
                    dropHighlighters();
                }
            })
            .createPopup();

        if (maxOffset != -1) {
            myEditor.getCaretModel().moveToOffset(maxOffset);
        }

        EditorPopupHelper.getInstance().showPopupInBestPositionFor(myEditor, popup);
    }

    private void dropHighlighters() {
        myRangeHighlighters.forEach(RangeMarker::dispose);
        myRangeHighlighters.clear();
    }
}
