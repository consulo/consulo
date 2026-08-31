/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.qt.editor.impl.internal;

import consulo.colorScheme.EditorColorsScheme;
import consulo.logging.Logger;
import io.qt.gui.QFont;
import io.qt.gui.QFontInfo;
import io.qt.gui.QFontMetricsF;
import io.qt.gui.QGlyphRun;
import io.qt.gui.QTextLayout;
import org.jspecify.annotations.Nullable;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * Line geometry of the editor font.
 * <p>
 * The arithmetic deliberately mirrors the awt {@code EditorViewImpl.initMetricsIfNeeded} rather than taking
 * qt's own line spacing: the platform positions inlay hints, breakpoints, gutter icons and popups off
 * {@link #getLineHeight()}, so the two frontends have to agree on what a line occupies.
 * <p>
 * Metrics are computed lazily and under a lock because the first caller is not always the qt thread - the same
 * reason the awt one is synchronized.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtEditorFontMetrics {
    private static final Logger LOG = Logger.getInstance(DesktopQtEditorFontMetrics.class);

    private final DesktopQtEditorImpl myEditor;

    /**
     * Indexed by the awt {@code Font} style bits the colour scheme speaks in - plain, bold, italic, bold italic.
     */
    private final QFont[] myFonts = new QFont[4];
    private final QFontMetricsF[] myFontMetrics = new QFontMetricsF[4];

    private @Nullable QFont myFont;
    private @Nullable QFontMetricsF myMetrics;
    private int myLineHeight;
    private int myAscent;
    private int myDescent;
    private double mySpaceWidth;

    public DesktopQtEditorFontMetrics(DesktopQtEditorImpl editor) {
        myEditor = editor;
    }

    public synchronized void reset() {
        myFont = null;
    }

    public synchronized QFont getFont() {
        initIfNeeded();
        return myFont;
    }

    public synchronized QFont getFont(int fontType) {
        initIfNeeded();
        return myFonts[fontType & 3];
    }

    public synchronized int getLineHeight() {
        initIfNeeded();
        return myLineHeight;
    }

    public synchronized int getAscent() {
        initIfNeeded();
        return myAscent;
    }

    public synchronized int getDescent() {
        initIfNeeded();
        return myDescent;
    }

    public synchronized double getSpaceWidth() {
        initIfNeeded();
        return mySpaceWidth;
    }

    public synchronized double getTextWidth(CharSequence text) {
        initIfNeeded();
        return myMetrics.horizontalAdvance(text.toString());
    }

    public synchronized double getTextWidth(CharSequence text, int fontType) {
        initIfNeeded();
        return myFontMetrics[fontType & 3].horizontalAdvance(text.toString());
    }

    private void initIfNeeded() {
        if (myFont != null) {
            return;
        }

        EditorColorsScheme scheme = myEditor.getColorsScheme();

        // the scheme's font size is a pixel height - awt's Font takes it as a point size at 72 dpi, which is the
        // same thing. qt would scale a point size by the screen dpi instead and draw the text a third too large
        QFont font = new QFont(scheme.getEditorFontName());
        font.setPixelSize(scheme.getEditorFontSize());

        // the scheme decides, not the typeface - leaving it to the default would ligate under a font which
        // ships them on and not under one which does not. all three are named because a coding ligature is as
        // often a contextual alternate as it is a standard one
        int ligatures = scheme.isUseLigatures() ? 1 : 0;
        font.setFeature("liga", ligatures);
        font.setFeature("clig", ligatures);
        font.setFeature("calt", ligatures);

        QFontMetricsF metrics = new QFontMetricsF(font);

        double fontHeight = metrics.height();
        myLineHeight = Math.max(1, (int) Math.ceil(fontHeight * scheme.getLineSpacing()));

        // the extra room the line spacing added is split above and below the glyphs, so the text stays centred
        // in its line instead of clinging to the top of it
        myDescent = (int) Math.round(metrics.descent() + (myLineHeight - fontHeight) / 2);
        myAscent = myLineHeight - myDescent;
        mySpaceWidth = metrics.horizontalAdvance(' ');

        for (int style = 0; style < myFonts.length; style++) {
            QFont styled = new QFont(font);
            styled.setBold((style & Font.BOLD) != 0);
            styled.setItalic((style & Font.ITALIC) != 0);

            myFonts[style] = styled;
            myFontMetrics[style] = new QFontMetricsF(styled);
        }

        myMetrics = metrics;
        myFont = font;

        // TODO temporary - says whether the scheme asked for ligatures and which font qt actually resolved,
        // then whether shaping actually acts on the feature: the same text under liga on and off
        QFontInfo info = new QFontInfo(font);
        LOG.info("qt editor font: requested=%s resolved=%s pixelSize=%d exactMatch=%s ligatures=%s liga=%d plainLiga=%d".formatted(
            scheme.getEditorFontName(),
            info.family(),
            info.pixelSize(),
            info.exactMatch(),
            scheme.isUseLigatures(),
            font.featureValue("liga"),
            myFonts[0].featureValue("liga")
        ));

        QFont off = new QFont(font);
        off.setFeature("liga", 0);
        off.setFeature("clig", 0);
        off.setFeature("calt", 0);

        LOG.info("qt editor shaping: '->' styled=%s plain=%s ligaOff=%s || '==' styled=%s ligaOff=%s".formatted(
            glyphIndexes("->", font),
            glyphIndexes("->", myFonts[0]),
            glyphIndexes("->", off),
            glyphIndexes("==", font),
            glyphIndexes("==", off)
        ));
    }

    private static String glyphIndexes(String text, QFont font) {
        QTextLayout layout = new QTextLayout(text, font, null);
        layout.beginLayout();
        layout.createLine();
        layout.endLayout();

        List<Integer> indexes = new ArrayList<>();
        for (QGlyphRun run : layout.glyphRuns(-1, -1)) {
            indexes.addAll(run.glyphIndexes());
        }
        return indexes.toString();
    }
}
