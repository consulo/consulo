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

package consulo.ide.impl.idea.application.options.colors;

import consulo.codeEditor.*;
import consulo.codeEditor.event.CaretAdapter;
import consulo.codeEditor.event.CaretEvent;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributesKey;
import consulo.disposer.Disposer;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.application.options.colors.highlighting.HighlightData;
import consulo.ide.impl.idea.application.options.colors.highlighting.HighlightsExtractor;
import consulo.language.ast.IElementType;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.language.editor.colorScheme.setting.EditorHighlightingProvidingColorSettingsPage;
import consulo.language.editor.colorScheme.setting.RainbowColorSettingsPage;
import consulo.language.editor.highlight.HighlighterFactory;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.internal.ColorSettingsUtil;
import consulo.language.editor.rawHighlight.RainbowHighlighter;
import consulo.language.editor.util.UsedColors;
import consulo.localize.LocalizeValue;
import consulo.proxy.EventDispatcher;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.JdkConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import java.util.*;

import static consulo.codeEditor.CodeInsightColors.BLINKING_HIGHLIGHTS_ATTRIBUTES;

public class SimpleEditorPreview implements PreviewPanel {
    private static final Map<String, TextAttributesKey> INLINE_ELEMENTS =
        Collections.singletonMap("parameter_hint", DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT);

    private final ColorSettingsPage myPage;

    private final EditorEx myEditor;
    private final Alarm myBlinkingAlarm;
    private final List<HighlightData> myHighlightData = new ArrayList<>();

    private final ColorAndFontOptions myOptions;

    private final EventDispatcher<ColorAndFontSettingsListener> myDispatcher = EventDispatcher.create(ColorAndFontSettingsListener.class);
    private final HighlightsExtractor myHighlightsExtractor;

    public SimpleEditorPreview(ColorAndFontOptions options, ColorSettingsPage page) {
        this(options, page, true);
    }

    public SimpleEditorPreview(ColorAndFontOptions options, ColorSettingsPage page, boolean navigatable) {
        myOptions = options;
        myPage = page;

        myHighlightsExtractor = new HighlightsExtractor(page.getAdditionalHighlightingTagToDescriptorMap(),
            page.getAdditionalInlineElementToDescriptorMap(),
            page.getAdditionalHighlightingTagToColorKeyMap());

        myEditor = (EditorEx) FontEditorPreview.createPreviewEditor(
            myHighlightsExtractor.extractHighlights(page.getDemoText(), myHighlightData), // text without tags
            10, 3, -1, myOptions, false);

        FontEditorPreview.installTrafficLights(myEditor);
        myBlinkingAlarm = new Alarm().setActivationComponent(myEditor.getComponent());
        if (navigatable) {
            myEditor.getContentComponent().addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    navigate(false, myEditor.xyToLogicalPosition(new Point(e.getX(), e.getY())));
                }
            });

            myEditor.getCaretModel().addCaretListener(new CaretAdapter() {
                @Override
                public void caretPositionChanged(CaretEvent e) {
                    navigate(true, e.getNewPosition());
                }
            });
        }
    }

    public EditorEx getEditor() {
        return myEditor;
    }

    public void setDemoText(String text) {
        myEditor.getSelectionModel().removeSelection();
        myHighlightData.clear();
        myEditor.getDocument().setText(myHighlightsExtractor.extractHighlights(text, myHighlightData));
    }

    private void navigate(boolean select, @Nonnull LogicalPosition pos) {
        int offset = myEditor.logicalPositionToOffset(pos);
        SyntaxHighlighter highlighter = myPage.getHighlighter();

        String type;
        HighlightData highlightData = getDataFromOffset(offset);
        if (highlightData != null) {
            // tag-based navigation first
            type = RainbowHighlighter.isRainbowTempKey(highlightData.getHighlightKey())
                ? RainbowHighlighter.RAINBOW_TYPE
                : highlightData.getHighlightType();
        }
        else {
            // if failed, try the highlighter-based navigation
            type = selectItem(myEditor.getHighlighter().createIterator(offset), highlighter);
        }

        setCursor(type == null ? Cursor.TEXT_CURSOR : Cursor.HAND_CURSOR);

        if (select && type != null) {
            myDispatcher.getMulticaster().selectionInPreviewChanged(type);
        }
    }

    @Nullable
    private HighlightData getDataFromOffset(int offset) {
        for (HighlightData highlightData : myHighlightData) {
            if (offset >= highlightData.getStartOffset() && offset <= highlightData.getEndOffset()) {
                return highlightData;
            }
        }
        return null;
    }

    @Nullable
    private static String selectItem(HighlighterIterator itr, SyntaxHighlighter highlighter) {
        IElementType tokenType = (IElementType) itr.getTokenType();
        if (tokenType == null) {
            return null;
        }

        TextAttributesKey[] highlights = highlighter.getTokenHighlights(tokenType);
        String s = null;
        for (int i = highlights.length - 1; i >= 0; i--) {
            if (highlights[i] != HighlighterColors.TEXT) {
                s = highlights[i].getExternalName();
                break;
            }
        }
        return s == null ? HighlighterColors.TEXT.getExternalName() : s;
    }

    @Override
    public JComponent getPanel() {
        return myEditor.getComponent();
    }

    @Override
    public void updateView() {
        EditorColorsScheme scheme = myOptions.getSelectedScheme();

        myEditor.setColorsScheme(scheme);

        EditorHighlighter highlighter = null;
        if (myPage instanceof EditorHighlightingProvidingColorSettingsPage) {

            highlighter = ((EditorHighlightingProvidingColorSettingsPage) myPage).createEditorHighlighter(scheme);
        }
        if (highlighter == null) {
            SyntaxHighlighter pageHighlighter = myPage.getHighlighter();
            highlighter = HighlighterFactory.createHighlighter(pageHighlighter, scheme);
        }
        myEditor.setHighlighter(highlighter);
        updateHighlighters();

        myEditor.reinitSettings();
    }

    private void updateHighlighters() {
        UIUtil.invokeLaterIfNeeded(() -> {
            if (myEditor.isDisposed()) {
                return;
            }
            removeDecorations(myEditor);
            Map<TextAttributesKey, LocalizeValue> displayText = ColorSettingsUtil.keyToDisplayTextMap(myPage);
            for (HighlightData data : myHighlightData) {
                data.addHighlToView(myEditor, myOptions.getSelectedScheme(), displayText);
            }
        });
    }

    private static void removeDecorations(Editor editor) {
        editor.getMarkupModel().removeAllHighlighters();
        for (Inlay inlay : editor.getInlayModel().getInlineElementsInRange(0, editor.getDocument().getTextLength())) {
            Disposer.dispose(inlay);
        }
    }

    private static final int BLINK_COUNT = 3 * 2;

    @Override
    public void blinkSelectedHighlightType(Object description) {
        if (description instanceof EditorSchemeAttributeDescriptor) {
            String type = ((EditorSchemeAttributeDescriptor) description).getType();

            List<HighlightData> highlights = startBlinkingHighlights(myEditor,
                type,
                myPage.getHighlighter(), true,
                myBlinkingAlarm, BLINK_COUNT, myPage);

            scrollHighlightInView(highlights);
        }
    }

    void scrollHighlightInView(@Nullable List<HighlightData> highlightDatas) {
        if (highlightDatas == null) {
            return;
        }

        boolean needScroll = true;
        int minOffset = Integer.MAX_VALUE;
        for (HighlightData data : highlightDatas) {
            if (isOffsetVisible(data.getStartOffset())) {
                needScroll = false;
                break;
            }
            minOffset = Math.min(minOffset, data.getStartOffset());
        }
        if (needScroll && minOffset != Integer.MAX_VALUE) {
            LogicalPosition pos = myEditor.offsetToLogicalPosition(minOffset);
            myEditor.getScrollingModel().scrollTo(pos, ScrollType.MAKE_VISIBLE);
        }
    }

    private boolean isOffsetVisible(int startOffset) {
        return myEditor
            .getScrollingModel()
            .getVisibleArea()
            .contains(myEditor.logicalPositionToXY(myEditor.offsetToLogicalPosition(startOffset)));
    }

    public void stopBlinking() {
        myBlinkingAlarm.cancelAllRequests();
    }

    private List<HighlightData> startBlinkingHighlights(EditorEx editor,
                                                        String attrKey,
                                                        SyntaxHighlighter highlighter,
                                                        boolean show,
                                                        Alarm alarm,
                                                        int count,
                                                        ColorSettingsPage page) {
        if (show && count <= 0) {
            return Collections.emptyList();
        }
        removeDecorations(editor);
        boolean found = false;
        List<HighlightData> highlights = new ArrayList<>();
        List<HighlightData> matchingHighlights = new ArrayList<>();
        for (HighlightData highlightData : myHighlightData) {
            boolean highlight = show && highlightData.getHighlightType().equals(attrKey);
            highlightData.addToCollection(highlights, highlight);
            if (highlight) {
                matchingHighlights.add(highlightData);
                found = true;
            }
        }
        if (!found && highlighter != null) {
            HighlighterIterator iterator = editor.getHighlighter().createIterator(0);
            do {
                IElementType tokenType = (IElementType) iterator.getTokenType();
                TextAttributesKey[] tokenHighlights = highlighter.getTokenHighlights(tokenType);
                for (TextAttributesKey tokenHighlight : tokenHighlights) {
                    String type = tokenHighlight.getExternalName();
                    if (show && type != null && type.equals(attrKey)) {
                        HighlightData highlightData = new HighlightData(iterator.getStart(), iterator.getEnd(), BLINKING_HIGHLIGHTS_ATTRIBUTES);
                        highlights.add(highlightData);
                        matchingHighlights.add(highlightData);
                    }
                }
                iterator.advance();
            }
            while (!iterator.atEnd());
        }

        Map<TextAttributesKey, LocalizeValue> displayText = ColorSettingsUtil.keyToDisplayTextMap(page);

        // sort highlights to avoid overlappings
        Collections.sort(highlights, Comparator.comparingInt(HighlightData::getStartOffset));
        for (int i = highlights.size() - 1; i >= 0; i--) {
            HighlightData highlightData = highlights.get(i);
            int startOffset = highlightData.getStartOffset();
            HighlightData prevHighlightData = i == 0 ? null : highlights.get(i - 1);
            if (prevHighlightData != null
                && startOffset <= prevHighlightData.getEndOffset()
                && highlightData.getHighlightType().equals(prevHighlightData.getHighlightType())) {
                prevHighlightData.setEndOffset(highlightData.getEndOffset());
            }
            else {
                highlightData.addHighlToView(editor, myOptions.getSelectedScheme(), displayText);
            }
        }
        alarm.cancelAllRequests();
        alarm.addComponentRequest(() -> startBlinkingHighlights(editor, attrKey, highlighter, !show, alarm, count - 1, page), 400);
        return matchingHighlights;
    }


    @Override
    public void addListener(@Nonnull ColorAndFontSettingsListener listener) {
        myDispatcher.addListener(listener);
    }

    @Override
    public void disposeUIResources() {
        EditorFactory editorFactory = EditorFactory.getInstance();
        editorFactory.releaseEditor(myEditor);
        stopBlinking();
    }

    private void setCursor(@JdkConstants.CursorType int type) {
        Cursor cursor = type == Cursor.TEXT_CURSOR ? UIUtil.getTextCursor(TargetAWT.to(myEditor.getBackgroundColor()))
            : Cursor.getPredefinedCursor(type);
        myEditor.getContentComponent().setCursor(cursor);
    }

    public void setupRainbow(@Nonnull EditorColorsScheme colorsScheme, @Nonnull RainbowColorSettingsPage page) {
        List<HighlightData> initialMarkup = new ArrayList<>();
        myHighlightsExtractor.extractHighlights(page.getDemoText(), initialMarkup);

        List<HighlightData> rainbowMarkup = setupRainbowHighlighting(
            page,
            initialMarkup,
            new RainbowHighlighter(colorsScheme).getRainbowTempKeys(),
            RainbowHighlighter.isRainbowEnabledWithInheritance(colorsScheme, page.getLanguage()));

        myHighlightData.clear();
        myHighlightData.addAll(rainbowMarkup);
    }

    @Nonnull
    private List<HighlightData> setupRainbowHighlighting(@Nonnull RainbowColorSettingsPage page,
                                                         @Nonnull List<HighlightData> initialMarkup,
                                                         @Nonnull TextAttributesKey[] rainbowTempKeys,
                                                         boolean isRainbowOn) {
        int colorCount = rainbowTempKeys.length;
        if (colorCount == 0) {
            return initialMarkup;
        }
        List<HighlightData> rainbowMarkup = new ArrayList<>();

        int tempKeyIndex = 0;
        boolean repeatAnchor = true;
        for (HighlightData d : initialMarkup) {
            TextAttributesKey highlightKey = d.getHighlightKey();
            boolean rainbowType = page.isRainbowType(highlightKey);
            boolean rainbowDemoType = highlightKey == RainbowHighlighter.RAINBOW_GRADIENT_DEMO;
            if (rainbowType || rainbowDemoType) {
                HighlightData rainbowAnchor = new HighlightData(d.getStartOffset(), d.getEndOffset(), RainbowHighlighter.RAINBOW_ANCHOR);
                if (isRainbowOn) {
                    // rainbow on
                    HighlightData rainbowTemp;
                    if (rainbowType) {
                        rainbowTemp = getRainbowTemp(rainbowTempKeys, d.getStartOffset(), d.getEndOffset());
                    }
                    else {
                        rainbowTemp = new HighlightData(d.getStartOffset(), d.getEndOffset(), rainbowTempKeys[tempKeyIndex % colorCount]);
                        if (repeatAnchor && tempKeyIndex == colorCount / 2) {
                            // anchor [Color#3] colored twice: it the end and in the beginning of rainbow-demo string
                            repeatAnchor = false;
                        }
                        else {
                            ++tempKeyIndex;
                        }
                    }
                    // TODO: <remove the hack>
                    // At some point highlighting data is applied in reversed order. To ensure rainbow highlighting is always on top, we add it twice.
                    rainbowMarkup.add(rainbowTemp);
                    rainbowMarkup.add(rainbowAnchor);
                    rainbowMarkup.add(d);
                    rainbowMarkup.add(rainbowAnchor);
                    rainbowMarkup.add(rainbowTemp);
                }
                else {
                    // rainbow off
                    if (rainbowType) {
                        // TODO: <remove the hack>
                        // See above
                        rainbowMarkup.add(d);
                        rainbowMarkup.add(rainbowAnchor);
                        rainbowMarkup.add(d);
                    }
                    else {
                        rainbowMarkup.add(rainbowAnchor);
                    }
                }
            }
            else if (!(RainbowHighlighter.isRainbowTempKey(highlightKey) || highlightKey == RainbowHighlighter.RAINBOW_ANCHOR)) {
                // filter rainbow RAINBOW_TEMP and RAINBOW_ANCHOR
                rainbowMarkup.add(d);
            }
        }
        return rainbowMarkup;
    }

    @Nonnull
    private HighlightData getRainbowTemp(@Nonnull TextAttributesKey[] rainbowTempKeys,
                                         int startOffset, int endOffset) {
        String id = myEditor.getDocument().getText(TextRange.create(startOffset, endOffset));
        int index = UsedColors.getOrAddColorIndex(myEditor, id, rainbowTempKeys.length);
        return new HighlightData(startOffset, endOffset, rainbowTempKeys[index]);
    }
}
