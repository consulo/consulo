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
package consulo.language.editor.impl.internal.codeBlock;

import consulo.annotation.component.ServiceImpl;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.language.Language;
import consulo.language.editor.codeBlock.CodeBlockHighlighter;
import consulo.language.editor.codeBlock.CodeBlockHtmlBuilder;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.font.Font;
import consulo.ui.util.ColorValueUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Singleton
@ServiceImpl
public class CodeBlockHighlighterImpl implements CodeBlockHighlighter {
    private final EditorColorsManager myEditorColorsManager;

    @Inject
    public CodeBlockHighlighterImpl(EditorColorsManager editorColorsManager) {
        myEditorColorsManager = editorColorsManager;
    }

    @Override
    public @Nullable Language guessLanguage(@Nullable String languageName) {
        if (languageName == null || languageName.isBlank()) {
            return null;
        }
        String name = languageName.trim();

        Language language = findFirst(Language.findInstancesByMimeType(name));
        if (language != null) {
            return language;
        }
        language = findFirst(Language.findInstancesByMimeType("text/" + name));
        if (language != null) {
            return language;
        }
        for (Language registered : Language.getRegisteredLanguages()) {
            if (name.equalsIgnoreCase(registered.getID()) || name.equalsIgnoreCase(registered.getDisplayName().get())) {
                return registered;
            }
        }
        return findLanguageByFileExtension(name);
    }

    private static @Nullable Language findFirst(Iterable<Language> languages) {
        for (Language language : languages) {
            return language;
        }
        return null;
    }

    /**
     * The most basic of the languages of the file type, as several dialects may share one
     */
    private static @Nullable Language findLanguageByFileExtension(String extension) {
        FileType fileType = FileTypeRegistry.getInstance().getFileTypeByExtension(extension);
        Set<Language> candidates = new HashSet<>();
        for (Language language : Language.getRegisteredLanguages()) {
            if (Objects.equals(language.getAssociatedFileType(), fileType)) {
                candidates.add(language);
            }
        }
        for (Language candidate : candidates) {
            boolean basic = true;
            for (Language base = candidate.getBaseLanguage(); base != null; base = base.getBaseLanguage()) {
                if (candidates.contains(base)) {
                    basic = false;
                    break;
                }
            }
            if (basic) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public CodeBlockHtmlBuilder newHtmlBuilder(Project project, Language language, String code) {
        return new CodeBlockHtmlBuilderImpl(project, language, code, myEditorColorsManager.getGlobalScheme());
    }

    @Override
    public StringBuilder appendStyledSpan(
        StringBuilder buffer,
        TextAttributesKey attributesKey,
        @Nullable String value,
        float saturationFactor
    ) {
        TextAttributes attributes = myEditorColorsManager.getGlobalScheme().getAttributes(attributesKey);
        return appendStyledSpan(buffer, attributes == null ? new TextAttributes() : attributes, value, saturationFactor);
    }

    @Override
    public StringBuilder appendStyledSpan(StringBuilder buffer, TextAttributes attributes, @Nullable String value, float saturationFactor) {
        StringBuilder style = new StringBuilder();

        ColorValue foregroundColor = tune(attributes.getForegroundColor(), saturationFactor);
        ColorValue backgroundColor = tune(attributes.getBackgroundColor(), saturationFactor);
        ColorValue effectColor = tune(attributes.getEffectColor(), saturationFactor);

        if (foregroundColor != null) {
            appendProperty(style, "color", toHtmlColor(foregroundColor));
        }
        if (backgroundColor != null) {
            appendProperty(style, "background-color", toHtmlColor(backgroundColor));
        }

        int fontType = attributes.getFontType();
        if ((fontType & Font.BOLD) != 0) {
            appendProperty(style, "font-weight", "bold");
        }
        if ((fontType & Font.ITALIC) != 0) {
            appendProperty(style, "font-style", "italic");
        }

        EffectType effectType = attributes.getEffectType();
        if (attributes.hasEffects() && effectType != null) {
            appendEffect(style, effectType, effectColor);
        }

        buffer.append("<span style=\"").append(style).append("\">").append(StringUtil.notNullize(value)).append("</span>");
        return buffer;
    }

    private static void appendEffect(StringBuilder style, EffectType effectType, @Nullable ColorValue effectColor) {
        switch (effectType) {
            case LINE_UNDERSCORE -> appendProperty(style, "text-decoration-line", "underline");
            case WAVE_UNDERSCORE -> {
                appendProperty(style, "text-decoration-line", "underline");
                appendProperty(style, "text-decoration-style", "wavy");
            }
            case BOLD_LINE_UNDERSCORE -> {
                appendProperty(style, "text-decoration-line", "underline");
                appendProperty(style, "text-decoration-thickness", "2px");
            }
            case BOLD_DOTTED_LINE -> {
                appendProperty(style, "text-decoration-line", "underline");
                appendProperty(style, "text-decoration-thickness", "2px");
                appendProperty(style, "text-decoration-style", "dotted");
            }
            case STRIKEOUT -> appendProperty(style, "text-decoration-line", "line-through");
            case BOXED, SLIGHTLY_WIDER_BOX, SEARCH_MATCH -> appendProperty(style, "border", "1px solid");
            case ROUNDED_BOX -> {
                appendProperty(style, "border", "1px solid");
                appendProperty(style, "border-radius", "2px");
            }
        }

        if (effectColor == null) {
            return;
        }
        switch (effectType) {
            case LINE_UNDERSCORE, WAVE_UNDERSCORE, BOLD_LINE_UNDERSCORE, BOLD_DOTTED_LINE, STRIKEOUT ->
                appendProperty(style, "text-decoration-color", toHtmlColor(effectColor));
            case BOXED, ROUNDED_BOX, SEARCH_MATCH, SLIGHTLY_WIDER_BOX -> appendProperty(style, "border-color", toHtmlColor(effectColor));
        }
    }

    private static @Nullable ColorValue tune(@Nullable ColorValue color, float saturationFactor) {
        return color == null || saturationFactor == 1 ? color : ColorValueUtil.tuneSaturationEspeciallyGrey(color, 1, saturationFactor);
    }

    private static String toHtmlColor(ColorValue color) {
        return "#" + ColorValueUtil.toHex(color);
    }

    private static void appendProperty(StringBuilder builder, String name, String value) {
        builder.append(name).append(':').append(value).append(';');
    }
}
