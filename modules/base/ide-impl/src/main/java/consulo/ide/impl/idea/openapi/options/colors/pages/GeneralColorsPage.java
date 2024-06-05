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
import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.HighlighterColors;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorDescriptor;
import consulo.configurable.internal.ConfigurableWeight;
import consulo.configurable.localize.ConfigurableLocalize;
import consulo.ide.impl.idea.application.options.colors.InspectionColorSettingsPage;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.language.editor.highlight.DefaultSyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.SeveritiesProvider;
import consulo.language.editor.template.TemplateColors;
import consulo.util.io.FileUtil;
import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@ExtensionImpl(id = "general", order = "first")
public class GeneralColorsPage implements ColorSettingsPage, InspectionColorSettingsPage, ConfigurableWeight {
  private static final String ADDITIONAL_DEMO_TEXT = "\n" +
    "<todo>//TODO: Visit Consulo Web resources:</todo>\n" +
    "Consulo Home Page: <hyperlink_f>https://consulo.io</hyperlink_f>\n" +
    "Consulo Developer Community: <hyperlink>https://discuss.consulo.io/</hyperlink>\n" +
    "\n" +
    "Search:\n" +
    "  <search_result_wr>result</search_result_wr> = \"<search_text>text</search_text>, <search_text>text</search_text>, <search_text>text</search_text>\";\n" +
    "  <identifier_write>i</identifier_write> = <search_result>result</search_result>\n" +
    "  return <identifier>i;</identifier>\n" +
    "\n" +
    "<folded_text>Folded text</folded_text>\n" +
    "<deleted_text>Deleted text</deleted_text>\n" +
    "Template <template_var>VARIABLE</template_var>\n" +
    "Injected language: <injected_lang>\\.(gif|jpg|png)$</injected_lang>\n" +
    "\n" +
    "Code Inspections:\n" +
    "  <error>Error</error>\n" +
    "  <warning>Warning</warning>\n" +
    "  <weak_warning>Weak warning</weak_warning>\n" +
    "  <deprecated>Deprecated symbol</deprecated>\n" +
    "  <unused>Unused symbol</unused>\n" +
    "  <wrong_ref>Unknown symbol</wrong_ref>\n" +
    "  <server_error>Problem from server</server_error>\n" +
    "  <server_duplicate>Duplicate from server</server_duplicate>\n" +
    getCustomSeveritiesDemoText();

  private static final AttributesDescriptor[] ATT_DESCRIPTORS = {
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
  };

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
      ConfigurableLocalize.optionsGeneralColorDescriptorNotificationBackground(),
      EditorColors.NOTIFICATION_BACKGROUND,
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

  private static final Map<String, TextAttributesKey> ADDITIONAL_HIGHLIGHT_DESCRIPTORS = new HashMap<>();
  public static final String DISPLAY_NAME = ConfigurableLocalize.optionsGeneralDisplayName().get();

  static {
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("folded_text", EditorColors.FOLDED_TEXT_ATTRIBUTES);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("deleted_text", EditorColors.DELETED_TEXT_ATTRIBUTES);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("search_result", EditorColors.SEARCH_RESULT_ATTRIBUTES);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("search_result_wr", EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("search_text", EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("identifier", EditorColors.IDENTIFIER_UNDER_CARET_ATTRIBUTES);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("identifier_write", EditorColors.WRITE_IDENTIFIER_UNDER_CARET_ATTRIBUTES);

    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("template_var", TemplateColors.TEMPLATE_VARIABLE_ATTRIBUTES);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("injected_lang", EditorColors.INJECTED_LANGUAGE_FRAGMENT);

    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("todo", CodeInsightColors.TODO_DEFAULT_ATTRIBUTES);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("hyperlink", CodeInsightColors.HYPERLINK_ATTRIBUTES);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("hyperlink_f", CodeInsightColors.FOLLOWED_HYPERLINK_ATTRIBUTES);

    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("wrong_ref", CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("deprecated", CodeInsightColors.DEPRECATED_ATTRIBUTES);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("unused", CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("error", CodeInsightColors.ERRORS_ATTRIBUTES);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("warning", CodeInsightColors.WARNINGS_ATTRIBUTES);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("weak_warning", CodeInsightColors.WEAK_WARNING_ATTRIBUTES);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("server_error", CodeInsightColors.GENERIC_SERVER_ERROR_OR_WARNING);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("server_duplicate", CodeInsightColors.DUPLICATE_FROM_SERVER);
    for (SeveritiesProvider provider : SeveritiesProvider.EP_NAME.getExtensionList()) {
      for (HighlightInfoType highlightInfoType : provider.getSeveritiesHighlightInfoTypes()) {
        ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put(
          getHighlightDescTagName(highlightInfoType),
          highlightInfoType.getAttributesKey()
        );
      }
    }
  }

  @Override
  @Nonnull
  public String getDisplayName() {
    return DISPLAY_NAME;
  }

  @Override
  @Nonnull
  public AttributesDescriptor[] getAttributeDescriptors() {
    return ATT_DESCRIPTORS;
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
    try {
      InputStream stream = getClass().getResourceAsStream("/colorSettingPage/general.txt");
      return FileUtil.loadTextAndClose(stream, true) + getCustomSeveritiesDemoText();
    }
    catch (IOException e) {
      return "demo text not found";
    }
  }

  @Override
  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return ADDITIONAL_HIGHLIGHT_DESCRIPTORS;
  }

  @Override
  public int getConfigurableWeight() {
    return Integer.MAX_VALUE;
  }

  private static String getCustomSeveritiesDemoText() {
    final StringBuilder buff = new StringBuilder();

    for (SeveritiesProvider provider : SeveritiesProvider.EP_NAME.getExtensionList()) {
      for (HighlightInfoType highlightInfoType : provider.getSeveritiesHighlightInfoTypes()) {
        final String tag = getHighlightDescTagName(highlightInfoType);
        buff.append("  <").append(tag).append(">");
        buff.append(tag.toLowerCase());
        buff.append("</").append(tag).append(">").append("\n");
      }
    }

    return buff.toString();
  }

  private static String getHighlightDescTagName(HighlightInfoType highlightInfoType) {
    return highlightInfoType.getSeverity(null).myName;
  }
}
