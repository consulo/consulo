// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.reactions;

import consulo.application.util.HtmlChunk;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.GraphicsUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextLayout;
import java.util.List;
import java.util.stream.Collectors;

public final class CodeReviewReactionsUIUtil {
    private static final List<String> PREFERRED_EMOJI_FONTS = List.of(
        "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji"
    );

    static final @Nullable Font EMOJI_FONT = findEmojiFont();

    private static @Nullable Font findEmojiFont() {
        Font foundFont = null;
        int foundPriority = Integer.MAX_VALUE;
        for (Font font : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
            String name = font.getName();
            int priority = PREFERRED_EMOJI_FONTS.indexOf(name);
            if (priority < 0 && name.toLowerCase().contains("emoji")) {
                priority = Integer.MAX_VALUE;
            }
            if (priority >= 0 && (foundFont == null || priority < foundPriority)) {
                foundFont = font;
                foundPriority = priority;
            }
        }
        return foundFont;
    }

    static final String VARIATION_SELECTOR = "\uFE0F";

    public static final int BUTTON_HEIGHT = 24;
    public static final int HORIZONTAL_GAP = 8;
    public static final int ICON_SIZE = 20;
    public static final float COUNTER_FONT_SIZE = 11f;

    private CodeReviewReactionsUIUtil() {
    }

    public static final class Picker {
        public static final int WIDTH = 358;
        public static final int HEIGHT = 415;
        public static final int BLOCK_PADDING = 5;

        private Picker() {
        }
    }

    public static @Nonnull Icon createUnicodeEmojiIcon(@Nonnull String text, int size) {
        return new UnicodeEmojiIcon(text, size);
    }

    public static @Nls @Nonnull String createTooltipText(@Nonnull List<String> users, @Nonnull String reactionName) {
        StringBuilder reactorsBuilder = new StringBuilder();
        for (int i = 0; i < users.size(); i += 3) {
            List<String> chunk = users.subList(i, Math.min(i + 3, users.size()));
            String line = chunk.stream()
                .map(reactorName -> HtmlChunk.text(reactorName).bold().toString())
                .collect(Collectors.joining(", "));
            reactorsBuilder.append(line);
            if (i + 3 < users.size()) {
                reactorsBuilder.append(HtmlChunk.br());
            }
        }
        reactorsBuilder.append(HtmlChunk.br());

        return new HtmlBuilder()
            .appendRaw(CollaborationToolsLocalize.reviewCommentsReactionTooltip(reactorsBuilder.toString(), reactionName))
            .wrapWith(HtmlChunk.div("text-align: center"))
            .wrapWith(HtmlChunk.body())
            .wrapWith(HtmlChunk.html())
            .toString();
    }
}