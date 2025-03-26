/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.options.colors.pages;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.HighlighterColors;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorDescriptor;
import consulo.configurable.internal.ConfigurableWeight;
import consulo.configurable.localize.ConfigurableLocalize;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.language.editor.highlight.DefaultSyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.internal.ColorPageWeights;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.SeveritiesProvider;
import consulo.language.editor.template.TemplateColors;
import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtensionImpl(id = "general", order = "first")
public class GeneralColorsPage implements ColorSettingsPage, ConfigurableWeight {
    private static final List<AttributesDescriptor> ATT_DESCRIPTORS = List.of(
        new AttributesDescriptor(
            ConfigurableLocalize.optionsGeneralAttributeDescriptorDefaultText(),
            HighlighterColors.TEXT
        ),
        new AttributesDescriptor(
            ConfigurableLocalize.optionsGeneralAttributeDescriptorFoldedText(),
            EditorColors.FOLDED_TEXT_ATTRIBUTES
        ),
        new AttributesDescriptor(
            ConfigurableLocalize.optionsGeneralAttributeDescriptorDefaultText(),
            EditorColors.DELETED_TEXT_ATTRIBUTES
        ),
        new AttributesDescriptor(
            ConfigurableLocalize.optionsGeneralAttributeDescriptorSearchResult(),
            EditorColors.SEARCH_RESULT_ATTRIBUTES
        ),
        new AttributesDescriptor(
            ConfigurableLocalize.optionsGeneralAttributeDescriptorSearchResultWriteAccess(),
            EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES
        ),
        new AttributesDescriptor(
            ConfigurableLocalize.optionsGeneralAttributeDescriptiorIdentifierUnderCaret(),
            EditorColors.IDENTIFIER_UNDER_CARET_ATTRIBUTES
        ),
        new AttributesDescriptor(
            ConfigurableLocalize.optionsGeneralAttributeDescriptiorIdentifierUnderCaretWrite(),
            EditorColors.WRITE_IDENTIFIER_UNDER_CARET_ATTRIBUTES
        ),
        new AttributesDescriptor(
            ConfigurableLocalize.optionsGeneralAttributeDescriptorTextSearchResult(),
            EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES
        ),

        new AttributesDescriptor(
            ConfigurableLocalize.optionsGeneralAttributeDescriptorLiveTemplate(),
            EditorColors.LIVE_TEMPLATE_ATTRIBUTES
        ),
        new AttributesDescriptor(
            ConfigurableLocalize.optionsGeneralAttributeDescriptorTemplateVariable(),
            TemplateColors.TEMPLATE_VARIABLE_ATTRIBUTES
        ),
        new AttributesDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorInjectedLanguageFragment(),
            EditorColors.INJECTED_LANGUAGE_FRAGMENT
        ),

        new AttributesDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorHyperlinkNew(),
            CodeInsightColors.HYPERLINK_ATTRIBUTES
        ),
        new AttributesDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorHyperlinkFollowed(),
            CodeInsightColors.FOLLOWED_HYPERLINK_ATTRIBUTES
        ),
        new AttributesDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorReferenceHyperlink(),
            EditorColors.REFERENCE_HYPERLINK_COLOR
        ),

        new AttributesDescriptor(
            ConfigurableLocalize.optionsJavaAttributeDescriptorMatchedBrace(),
            CodeInsightColors.MATCHED_BRACE_ATTRIBUTES
        ),
        new AttributesDescriptor(
            ConfigurableLocalize.optionsJavaAttributeDescriptorUnmatchedBrace(),
            CodeInsightColors.UNMATCHED_BRACE_ATTRIBUTES
        ),

        new AttributesDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorTodoDefaults(),
            CodeInsightColors.TODO_DEFAULT_ATTRIBUTES
        ),
        new AttributesDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorBookmarks(),
            CodeInsightColors.BOOKMARKS_ATTRIBUTES
        ),

        new AttributesDescriptor(
            ConfigurableLocalize.optionsJavaColorDescriptorFullCoverage(),
            CodeInsightColors.LINE_FULL_COVERAGE
        ),
        new AttributesDescriptor(
            ConfigurableLocalize.optionsJavaColorDescriptorPartialCoverage(),
            CodeInsightColors.LINE_PARTIAL_COVERAGE
        ),
        new AttributesDescriptor(
            ConfigurableLocalize.optionsJavaColorDescriptorNoneCoverage(),
            CodeInsightColors.LINE_NONE_COVERAGE
        ),

        new AttributesDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorBreadcrumbsDefault(),
            EditorColors.BREADCRUMBS_DEFAULT
        ),
        new AttributesDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorBreadcrumbsHovered(),
            EditorColors.BREADCRUMBS_HOVERED
        ),
        new AttributesDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorBreadcrumbsCurrent(),
            EditorColors.BREADCRUMBS_CURRENT
        ),
        new AttributesDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorBreadcrumbsInactive(),
            EditorColors.BREADCRUMBS_INACTIVE
        )
    );

    private static final ColorDescriptor[] COLOR_DESCRIPTORS = {
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorBackgroundInReadonlyFiles(),
            EditorColors.READONLY_BACKGROUND_COLOR,
            ColorDescriptor.Kind.BACKGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorReadonlyFragmentBackground(),
            EditorColors.READONLY_FRAGMENT_BACKGROUND_COLOR,
            ColorDescriptor.Kind.BACKGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorGutterBackground(),
            EditorColors.GUTTER_BACKGROUND,
            ColorDescriptor.Kind.BACKGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorNotificationInformationBackground(),
            EditorColors.NOTIFICATION_INFORMATION_BACKGROUND,
            ColorDescriptor.Kind.BACKGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorNotificationWarningBackground(),
            EditorColors.NOTIFICATION_WARNING_BACKGROUND,
            ColorDescriptor.Kind.BACKGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorNotificationErrorBackground(),
            EditorColors.NOTIFICATION_ERROR_BACKGROUND,
            ColorDescriptor.Kind.BACKGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorSelectionBackground(),
            EditorColors.SELECTION_BACKGROUND_COLOR,
            ColorDescriptor.Kind.BACKGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorSelectionForeground(),
            EditorColors.SELECTION_FOREGROUND_COLOR,
            ColorDescriptor.Kind.FOREGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorCaret(),
            EditorColors.CARET_COLOR,
            ColorDescriptor.Kind.FOREGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorCaretRow(),
            EditorColors.CARET_ROW_COLOR,
            ColorDescriptor.Kind.BACKGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorRightMargin(),
            EditorColors.RIGHT_MARGIN_COLOR,
            ColorDescriptor.Kind.FOREGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorWhitespaces(),
            EditorColors.WHITESPACES_COLOR,
            ColorDescriptor.Kind.BACKGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorIndentGuide(),
            EditorColors.INDENT_GUIDE_COLOR,
            ColorDescriptor.Kind.BACKGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorIndentGuideSelected(),
            EditorColors.SELECTED_INDENT_GUIDE_COLOR,
            ColorDescriptor.Kind.BACKGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorLineNumber(),
            EditorColors.LINE_NUMBERS_COLOR,
            ColorDescriptor.Kind.FOREGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorVcsAnnotations(),
            EditorColors.ANNOTATIONS_COLOR,
            ColorDescriptor.Kind.FOREGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorTearline(),
            EditorColors.TEARLINE_COLOR,
            ColorDescriptor.Kind.FOREGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorTearlineSelected(),
            EditorColors.SELECTED_TEARLINE_COLOR,
            ColorDescriptor.Kind.FOREGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorAddedLines(),
            EditorColors.ADDED_LINES_COLOR,
            ColorDescriptor.Kind.BACKGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorModifiedLines(),
            EditorColors.MODIFIED_LINES_COLOR,
            ColorDescriptor.Kind.BACKGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorDeletedLines(),
            EditorColors.DELETED_LINES_COLOR,
            ColorDescriptor.Kind.BACKGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorWhitespacesModifiedLines(),
            EditorColors.WHITESPACES_MODIFIED_LINES_COLOR,
            ColorDescriptor.Kind.BACKGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorDescriptorBorderLines(),
            EditorColors.BORDER_LINES_COLOR,
            ColorDescriptor.Kind.BACKGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsJavaColorDescriptorMethodSeparatorColor(),
            CodeInsightColors.METHOD_SEPARATORS_COLOR,
            ColorDescriptor.Kind.FOREGROUND
        ),
        new ColorDescriptor(
            ConfigurableLocalize.optionsGeneralColorSoftWrapSign(),
            EditorColors.SOFT_WRAP_SIGN_COLOR,
            ColorDescriptor.Kind.FOREGROUND
        )
    };

    private static final Map<String, TextAttributesKey> ADDITIONAL_HIGHLIGHT_DESCRIPTORS;

    static {
        Map<String, TextAttributesKey> descriptors = new HashMap<>();
        descriptors.put("folded_text", EditorColors.FOLDED_TEXT_ATTRIBUTES);
        descriptors.put("deleted_text", EditorColors.DELETED_TEXT_ATTRIBUTES);
        descriptors.put("search_result", EditorColors.SEARCH_RESULT_ATTRIBUTES);
        descriptors.put("search_result_wr", EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES);
        descriptors.put("search_text", EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES);
        descriptors.put("identifier", EditorColors.IDENTIFIER_UNDER_CARET_ATTRIBUTES);
        descriptors.put("identifier_write", EditorColors.WRITE_IDENTIFIER_UNDER_CARET_ATTRIBUTES);

        descriptors.put("template_var", TemplateColors.TEMPLATE_VARIABLE_ATTRIBUTES);
        descriptors.put("injected_lang", EditorColors.INJECTED_LANGUAGE_FRAGMENT);

        descriptors.put("todo", CodeInsightColors.TODO_DEFAULT_ATTRIBUTES);
        descriptors.put("hyperlink", CodeInsightColors.HYPERLINK_ATTRIBUTES);
        descriptors.put("hyperlink_f", CodeInsightColors.FOLLOWED_HYPERLINK_ATTRIBUTES);

        descriptors.put("wrong_ref", CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES);
        descriptors.put("deprecated", CodeInsightColors.DEPRECATED_ATTRIBUTES);
        descriptors.put("unused", CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES);
        descriptors.put("error", CodeInsightColors.ERRORS_ATTRIBUTES);
        descriptors.put("warning", CodeInsightColors.WARNINGS_ATTRIBUTES);
        descriptors.put("weak_warning", CodeInsightColors.WEAK_WARNING_ATTRIBUTES);
        descriptors.put("server_error", CodeInsightColors.GENERIC_SERVER_ERROR_OR_WARNING);
        descriptors.put("server_duplicate", CodeInsightColors.DUPLICATE_FROM_SERVER);

        ADDITIONAL_HIGHLIGHT_DESCRIPTORS = Map.copyOf(descriptors);
    }

    private final Application myApplication;

    @Inject
    public GeneralColorsPage(Application application) {
        myApplication = application;
    }

    @Override
    @Nonnull
    public String getDisplayName() {
        return ConfigurableLocalize.optionsGeneralDisplayName().get();
    }

    @Override
    @Nonnull
    public AttributesDescriptor[] getAttributeDescriptors() {
        List<AttributesDescriptor> descriptors = new ArrayList<>(ATT_DESCRIPTORS);

        descriptors.add(new AttributesDescriptor(
            ConfigurableLocalize.optionsJavaAttributeDescriptorUnknownSymbol(),
            CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES
        ));
        descriptors.add(new AttributesDescriptor(
            ConfigurableLocalize.optionsJavaAttributeDescriptorDeprecatedSymbol(),
            CodeInsightColors.DEPRECATED_ATTRIBUTES
        ));
        descriptors.add(new AttributesDescriptor(
            ConfigurableLocalize.optionsJavaAttributeDescriptorUnusedSymbol(),
            CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES
        ));
        descriptors.add(new AttributesDescriptor(
            ConfigurableLocalize.optionsJavaAttributeDescriptorError(),
            CodeInsightColors.ERRORS_ATTRIBUTES
        ));
        descriptors.add(new AttributesDescriptor(
            ConfigurableLocalize.optionsJavaAttributeDescriptorWarning(),
            CodeInsightColors.WARNINGS_ATTRIBUTES
        ));
        descriptors.add(new AttributesDescriptor(
            ConfigurableLocalize.optionsJavaAttributeDescriptorWeakWarning(),
            CodeInsightColors.WEAK_WARNING_ATTRIBUTES
        ));
        descriptors.add(new AttributesDescriptor(
            ConfigurableLocalize.optionsJavaAttributeDescriptorServerProblems(),
            CodeInsightColors.GENERIC_SERVER_ERROR_OR_WARNING
        ));
        descriptors.add(new AttributesDescriptor(
            ConfigurableLocalize.optionsJavaAttributeDescriptorServerDuplicate(),
            CodeInsightColors.DUPLICATE_FROM_SERVER
        ));

        myApplication.getExtensionPoint(SeveritiesProvider.class).forEachExtensionSafe(provider -> {
            for (HighlightInfoType highlightInfoType : provider.getSeveritiesHighlightInfoTypes()) {
                final TextAttributesKey attributesKey = highlightInfoType.getAttributesKey();
                // FIXME [VISTALL] correct? Maybe localized name?
                descriptors.add(new AttributesDescriptor(LocalizeValue.of(toDisplayName(attributesKey)), attributesKey));
            }
        });

        return descriptors.toArray(AttributesDescriptor[]::new);
    }

    @Nonnull
    private static String toDisplayName(@Nonnull TextAttributesKey attributesKey) {
        return StringUtil.capitalize(attributesKey.getExternalName().toLowerCase().replaceAll("_", " "));
    }

    @Override
    @Nonnull
    public ColorDescriptor[] getColorDescriptors() {
        return COLOR_DESCRIPTORS;
    }

    @Override
    @Nonnull
    public SyntaxHighlighter getHighlighter() {
        return new DefaultSyntaxHighlighter();
    }

    @Override
    @Nonnull
    public String getDemoText() {
        return IdeLocalize.colorGeneral().get() + getCustomSeveritiesDemoText();
    }

    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        Map<String, TextAttributesKey> result = new HashMap<>(ADDITIONAL_HIGHLIGHT_DESCRIPTORS);
        myApplication.getExtensionPoint(SeveritiesProvider.class).forEachExtensionSafe(provider -> {
            for (HighlightInfoType highlightInfoType : provider.getSeveritiesHighlightInfoTypes()) {
                result.put(getHighlightDescTagName(highlightInfoType), highlightInfoType.getAttributesKey());
            }
        });
        return result;
    }

    @Override
    public int getConfigurableWeight() {
        return ColorPageWeights.GENERAL;
    }

    private String getCustomSeveritiesDemoText() {
        final StringBuilder buff = new StringBuilder();

        myApplication.getExtensionPoint(SeveritiesProvider.class).forEachExtensionSafe(provider -> {
            for (HighlightInfoType highlightInfoType : provider.getSeveritiesHighlightInfoTypes()) {
                final String tag = getHighlightDescTagName(highlightInfoType);
                buff.append("  <").append(tag).append(">");
                buff.append(tag.toLowerCase());
                buff.append("</").append(tag).append(">").append("\n");
            }
        });
        return buff.toString();
    }

    private static String getHighlightDescTagName(HighlightInfoType highlightInfoType) {
        return highlightInfoType.getSeverity(null).myName;
    }
}
