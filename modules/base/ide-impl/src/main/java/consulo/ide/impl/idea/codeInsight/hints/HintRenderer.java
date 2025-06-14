// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.codeEditor.*;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.codeEditor.impl.FontInfo;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorFontType;
import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.document.util.Segment;
import consulo.ide.impl.desktop.awt.editor.DesktopAWTEditor;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.ui.paint.EffectPainter;
import consulo.language.editor.impl.internal.inlay.param.HintUtils;
import consulo.language.editor.inlay.HintWidthAdjustment;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.GraphicsConfig;
import consulo.ui.ex.awt.JBCurrentTheme;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.DesktopAntialiasingTypeUtil;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import org.intellij.lang.annotations.JdkConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;

public class HintRenderer implements EditorCustomElementRenderer {
    private String text;
    private HintWidthAdjustment widthAdjustment;

    public HintRenderer(String text) {
        this.text = text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setWidthAdjustment(HintWidthAdjustment adjustment) {
        this.widthAdjustment = adjustment;
    }

    @Override
    public int calcWidthInPixels(Inlay<?> inlay) {
        return calcWidthInPixels(inlay.getEditor(), text, widthAdjustment, useEditorFont());
    }

    protected TextAttributes getTextAttributes(Editor editor) {
        return editor.getColorsScheme().getAttributes(DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT);
    }

    @Override
    public void paint(Inlay<?> inlay, @Nonnull Graphics g, @Nonnull Rectangle r, @Nonnull TextAttributes textAttributes) {
        Editor editor = inlay.getEditor();
        if (!(editor instanceof RealEditor impl)) {
            return;
        }

        Segment focus = impl.getFocusModeModel().getFocusModeRange();
        TextAttributes attrs = getTextAttributes(editor);
        if (focus != null && (inlay.getOffset() <= focus.getStartOffset() || focus.getEndOffset() <= inlay.getOffset())) {
            TextAttributes fm = impl.getUserData(FocusModeModel.FOCUS_MODE_ATTRIBUTES);
            if (fm != null) {
                attrs = fm;
            }
        }
        paintHint(g, impl, r, text, attrs, textAttributes, widthAdjustment, useEditorFont());
    }

    protected boolean useEditorFont() {
        return useEditorFontFromSettings();
    }

    public static int calcWidthInPixels(Editor editor, String text, HintWidthAdjustment adjustment) {
        return calcWidthInPixels(editor, text, adjustment, useEditorFontFromSettings());
    }

    public static int calcWidthInPixels(Editor editor,
                                        String text,
                                        HintWidthAdjustment adjustment,
                                        boolean useEditorFont) {
        MyFontMetrics metrics = getFontMetrics(editor, useEditorFont);
        return calcHintTextWidth(text, metrics.metrics)
            + calcWidthAdjustment(text, editor, metrics.metrics, adjustment);
    }

    public static void paintHint(Graphics g,
                                 RealEditor editor,
                                 Rectangle r,
                                 String text,
                                 TextAttributes attrs,
                                 TextAttributes surroundingAttrs,
                                 HintWidthAdjustment adjustment) {
        paintHint(g, editor, r, text, attrs, surroundingAttrs, adjustment, useEditorFontFromSettings());
    }

    public static void paintHint(Graphics g,
                                 RealEditor editor,
                                 Rectangle r,
                                 String text,
                                 TextAttributes attrs,
                                 TextAttributes surroundingAttrs,
                                 HintWidthAdjustment adjustment,
                                 boolean useEditorFont) {
        Graphics2D g2d = (Graphics2D) g;
        int ascent = editor.getAscent();
        int descent = editor.getDescent();

        if (text != null && attrs != null) {
            MyFontMetrics fm = getFontMetrics(editor, useEditorFont);
            int gap = r.height < fm.lineHeight + 2 ? 1 : 2;
            ColorValue bg = attrs.getBackgroundColor();
            if (bg != null) {
                float alpha = isInsufficientContrast(attrs, surroundingAttrs) ? 1.0f : BACKGROUND_ALPHA;
                GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
                GraphicsUtil.paintWithAlpha(g, alpha);
                g.setColor(TargetAWT.to(bg));
                g.fillRoundRect(r.x + 2, r.y + gap, r.width - 4, r.height - gap * 2, 8, 8);
                config.restore();
            }
            ColorValue fg = attrs.getForegroundColor();
            if (fg != null) {
                Object oldAA = g2d.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
                Rectangle clip = g.getClipBounds();
                g.setColor(TargetAWT.to(fg));
                g.setFont(fm.metrics.getFont());
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    DesktopAntialiasingTypeUtil.getKeyForCurrentScope(false));
                g.clipRect(r.x + 3, r.y + 2, r.width - 6, r.height - 4);

                int startX = r.x + 7;
                int startY = r.y + Math.max(ascent,
                    (r.height + fm.metrics.getAscent() - fm.metrics.getDescent()) / 2) - 1;

                int adjust = calcWidthAdjustment(text, editor, fm.metrics, adjustment);
                if (adjust == 0) {
                    g.drawString(text, startX, startY);
                }
                else {
                    int pos = adjustment.getAdjustmentPosition();
                    String first = text.substring(0, pos);
                    String second = text.substring(pos);
                    g.drawString(first, startX, startY);
                    g.drawString(second, startX + fm.metrics.stringWidth(first) + adjust, startY);
                }

                g.setClip(clip);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldAA);
            }
        }

        ColorValue effectColor = surroundingAttrs.getEffectColor();
        EffectType effectType = surroundingAttrs.getEffectType();
        if (effectColor != null) {
            g.setColor(TargetAWT.to(effectColor));
            int xStart = r.x;
            int xEnd = r.x + r.width;
            int y = r.y + ascent;
            Font font = editor.getColorsScheme().getFont(EditorFontType.PLAIN);
            switch (effectType) {
                case LINE_UNDERSCORE:
                    EffectPainter.LINE_UNDERSCORE.paint(g2d, xStart, y, xEnd - xStart, descent, font);
                    break;
                case BOLD_LINE_UNDERSCORE:
                    EffectPainter.BOLD_LINE_UNDERSCORE.paint(g2d, xStart, y, xEnd - xStart, descent, font);
                    break;
                case STRIKEOUT:
                    EffectPainter.STRIKE_THROUGH.paint(g2d, xStart, y, xEnd - xStart, editor.getCharHeight(), font);
                    break;
                case WAVE_UNDERSCORE:
                    EffectPainter.WAVE_UNDERSCORE.paint(g2d, xStart, y, xEnd - xStart, descent, font);
                    break;
                case BOLD_DOTTED_LINE:
                    EffectPainter.BOLD_DOTTED_UNDERSCORE.paint(g2d, xStart, y, xEnd - xStart, descent, font);
                    break;
                default:
                    break;
            }
        }
    }

    private static boolean isInsufficientContrast(TextAttributes attrs, TextAttributes surrounding) {
        ColorValue bgHint = surrounding.getBackgroundColor();
        if (bgHint == null || attrs.getForegroundColor() == null) {
            return false;
        }
        Color blended = srcOverBlend(TargetAWT.to(attrs.getBackgroundColor()), TargetAWT.to(bgHint), BACKGROUND_ALPHA);
        double grayBg = toGray(blended);
        double grayText = toGray(TargetAWT.to(attrs.getForegroundColor()));
        return Math.abs(grayBg - grayText) < 10;
    }

    private static double toGray(Color c) {
        return 0.30 * c.getRed() + 0.59 * c.getGreen() + 0.11 * c.getBlue();
    }

    private static Color srcOverBlend(Color fg, Color bg, float alpha) {
        int r = Math.round(fg.getRed() * alpha + bg.getRed() * (1 - alpha));
        int g = Math.round(fg.getGreen() * alpha + bg.getGreen() * (1 - alpha));
        int b = Math.round(fg.getBlue() * alpha + bg.getBlue() * (1 - alpha));
        return new Color(r, g, b);
    }

    private static int calcWidthAdjustment(String text, Editor editor, FontMetrics fm, HintWidthAdjustment adjustment) {
        if (adjustment == null || !(editor instanceof DesktopAWTEditor impl)) {
            return 0;
        }
        int editorTextWidth = impl.getFontMetrics(Font.PLAIN).stringWidth(adjustment.getEditorTextToMatch());
        int hintTextWidth = calcHintTextWidth(adjustment.getHintTextToMatch(), fm);
        int actualTextWidth = calcHintTextWidth(text, fm);
        return Math.max(0, editorTextWidth + hintTextWidth - actualTextWidth);
    }

    public static class MyFontMetrics {
        public final FontMetrics metrics;
        public final int lineHeight;

        public MyFontMetrics(Editor editor, int size, @JdkConstants.FontStyle int fontStyle, boolean useEditorFont) {
            Font font;
            if (useEditorFont) {
                font = EditorUtil.getEditorFont().deriveFont(fontStyle, size);
            }
            else {
                String family = UIManager.getFont("Label.font").getFamily();
                font = UIUtil.getFontWithFallback(family, fontStyle, size);
            }
            FontRenderContext ctx = new FontRenderContext(font.getTransform(),
                DesktopAntialiasingTypeUtil.getKeyForCurrentScope(false),
                InlayTextMetrics.getEditorFractionalMetricsHint());
            this.metrics = FontInfo.getFontMetrics(font, ctx);
            this.lineHeight = (int) Math.ceil(font.createGlyphVector(ctx, "Ap").getVisualBounds().getHeight());
        }

        public boolean isActual(Editor editor, float size, int fontStyle, String family) {
            Font f = metrics.getFont();
            if (!family.equals(f.getFamily()) || size != f.getSize2D() || fontStyle != f.getStyle()) {
                return false;
            }
            FontRenderContext current = new FontRenderContext(metrics.getFontRenderContext().getTransform(),
                DesktopAntialiasingTypeUtil.getKeyForCurrentScope(false),
                InlayTextMetrics.getEditorFractionalMetricsHint());
            return current.equals(metrics.getFontRenderContext());
        }
    }

    public static MyFontMetrics getFontMetrics(Editor editor, boolean useEditorFont) {
        RealEditor impl = (RealEditor) editor;
        int size = HintUtils.getSize(editor);
        MyFontMetrics cached = impl.getUserData(HINT_FONT_METRICS);
        TextAttributes attrs = editor.getColorsScheme().getAttributes(DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT);
        int fontType = attrs.getFontType();
        String family = useEditorFont ?
            EditorColorsManager.getInstance().getGlobalScheme().getEditorFontName() :
            UIUtil.getLabelFont().getFamily();
        if (cached != null && cached.isActual(editor, size, fontType, family)) {
            return cached;
        }
        MyFontMetrics metrics = new MyFontMetrics(editor, size, fontType, useEditorFont);
        impl.putUserData(HINT_FONT_METRICS, metrics);
        return metrics;
    }

    public static boolean useEditorFontFromSettings() {
        return EditorSettingsExternalizable.getInstance().isUseEditorFontInInlays();
    }

    protected static int calcHintTextWidth(String text, FontMetrics fm) {
        return text == null ? 0 : fm.stringWidth(text) + 14;
    }

    private static final Key<MyFontMetrics> HINT_FONT_METRICS = Key.create("ParameterHintFontMetrics");
    private static final float BACKGROUND_ALPHA = JBCurrentTheme.Popup.DEFAULT_HINT_OPACITY;
}
