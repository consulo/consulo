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
import consulo.configurable.OptionsBundle;
import consulo.configurable.internal.ConfigurableWeight;
import consulo.ide.impl.idea.application.options.colors.InspectionColorSettingsPage;
import consulo.language.editor.template.TemplateColors;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.language.editor.highlight.DefaultSyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.SeveritiesProvider;

import jakarta.annotation.Nonnull;
import java.io.IOException;
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

  private static final AttributesDescriptor[] ATT_DESCRIPTORS = {new AttributesDescriptor(OptionsBundle.message("options.general.attribute.descriptor.default.text"), HighlighterColors.TEXT),

          new AttributesDescriptor(OptionsBundle.message("options.general.attribute.descriptor.folded.text"), EditorColors.FOLDED_TEXT_ATTRIBUTES),
          new AttributesDescriptor(OptionsBundle.message("options.general.attribute.descriptor.deleted.text"), EditorColors.DELETED_TEXT_ATTRIBUTES),
          new AttributesDescriptor(OptionsBundle.message("options.general.attribute.descriptor.search.result"), EditorColors.SEARCH_RESULT_ATTRIBUTES),
          new AttributesDescriptor(OptionsBundle.message("options.general.attribute.descriptor.search.result.write.access"), EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES),
          new AttributesDescriptor(OptionsBundle.message("options.general.attribute.descriptior.identifier.under.caret"), EditorColors.IDENTIFIER_UNDER_CARET_ATTRIBUTES),
          new AttributesDescriptor(OptionsBundle.message("options.general.attribute.descriptior.identifier.under.caret.write"), EditorColors.WRITE_IDENTIFIER_UNDER_CARET_ATTRIBUTES),
          new AttributesDescriptor(OptionsBundle.message("options.general.attribute.descriptor.text.search.result"), EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES),

          new AttributesDescriptor(OptionsBundle.message("options.general.attribute.descriptor.live.template"), EditorColors.LIVE_TEMPLATE_ATTRIBUTES),
          new AttributesDescriptor(OptionsBundle.message("options.general.attribute.descriptor.template.variable"), TemplateColors.TEMPLATE_VARIABLE_ATTRIBUTES),
          new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.injected.language.fragment"), EditorColors.INJECTED_LANGUAGE_FRAGMENT),

          new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.hyperlink.new"), CodeInsightColors.HYPERLINK_ATTRIBUTES),
          new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.hyperlink.followed"), CodeInsightColors.FOLLOWED_HYPERLINK_ATTRIBUTES),
          new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.reference.hyperlink"), EditorColors.REFERENCE_HYPERLINK_COLOR),

          new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.matched.brace"), CodeInsightColors.MATCHED_BRACE_ATTRIBUTES),
          new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.unmatched.brace"), CodeInsightColors.UNMATCHED_BRACE_ATTRIBUTES),

          new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.todo.defaults"), CodeInsightColors.TODO_DEFAULT_ATTRIBUTES),
          new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.bookmarks"), CodeInsightColors.BOOKMARKS_ATTRIBUTES),

          new AttributesDescriptor(OptionsBundle.message("options.java.color.descriptor.full.coverage"), CodeInsightColors.LINE_FULL_COVERAGE),
          new AttributesDescriptor(OptionsBundle.message("options.java.color.descriptor.partial.coverage"), CodeInsightColors.LINE_PARTIAL_COVERAGE),
          new AttributesDescriptor(OptionsBundle.message("options.java.color.descriptor.none.coverage"), CodeInsightColors.LINE_NONE_COVERAGE),

          new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.breadcrumbs.default"), EditorColors.BREADCRUMBS_DEFAULT),
          new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.breadcrumbs.hovered"), EditorColors.BREADCRUMBS_HOVERED),
          new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.breadcrumbs.current"), EditorColors.BREADCRUMBS_CURRENT),
          new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.breadcrumbs.inactive"), EditorColors.BREADCRUMBS_INACTIVE)};

  private static final ColorDescriptor[] COLOR_DESCRIPTORS =
          {new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.background.in.readonly.files"), EditorColors.READONLY_BACKGROUND_COLOR, ColorDescriptor.Kind.BACKGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.readonly.fragment.background"), EditorColors.READONLY_FRAGMENT_BACKGROUND_COLOR,
                                      ColorDescriptor.Kind.BACKGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.gutter.background"), EditorColors.GUTTER_BACKGROUND, ColorDescriptor.Kind.BACKGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.notification.background"), EditorColors.NOTIFICATION_BACKGROUND, ColorDescriptor.Kind.BACKGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.selection.background"), EditorColors.SELECTION_BACKGROUND_COLOR, ColorDescriptor.Kind.BACKGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.selection.foreground"), EditorColors.SELECTION_FOREGROUND_COLOR, ColorDescriptor.Kind.FOREGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.caret"), EditorColors.CARET_COLOR, ColorDescriptor.Kind.FOREGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.caret.row"), EditorColors.CARET_ROW_COLOR, ColorDescriptor.Kind.BACKGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.right.margin"), EditorColors.RIGHT_MARGIN_COLOR, ColorDescriptor.Kind.FOREGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.whitespaces"), EditorColors.WHITESPACES_COLOR, ColorDescriptor.Kind.BACKGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.indent.guide"), EditorColors.INDENT_GUIDE_COLOR, ColorDescriptor.Kind.BACKGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.indent.guide.selected"), EditorColors.SELECTED_INDENT_GUIDE_COLOR, ColorDescriptor.Kind.BACKGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.line.number"), EditorColors.LINE_NUMBERS_COLOR, ColorDescriptor.Kind.FOREGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.vcs.annotations"), EditorColors.ANNOTATIONS_COLOR, ColorDescriptor.Kind.FOREGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.tearline"), EditorColors.TEARLINE_COLOR, ColorDescriptor.Kind.FOREGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.tearline.selected"), EditorColors.SELECTED_TEARLINE_COLOR, ColorDescriptor.Kind.FOREGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.added.lines"), EditorColors.ADDED_LINES_COLOR, ColorDescriptor.Kind.BACKGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.modified.lines"), EditorColors.MODIFIED_LINES_COLOR, ColorDescriptor.Kind.BACKGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.deleted.lines"), EditorColors.DELETED_LINES_COLOR, ColorDescriptor.Kind.BACKGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.whitespaces.modified.lines"), EditorColors.WHITESPACES_MODIFIED_LINES_COLOR,
                                      ColorDescriptor.Kind.BACKGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.border.lines"), EditorColors.BORDER_LINES_COLOR, ColorDescriptor.Kind.BACKGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.java.color.descriptor.method.separator.color"), CodeInsightColors.METHOD_SEPARATORS_COLOR, ColorDescriptor.Kind.FOREGROUND),
                  new ColorDescriptor(OptionsBundle.message("options.general.color.soft.wrap.sign"), EditorColors.SOFT_WRAP_SIGN_COLOR, ColorDescriptor.Kind.FOREGROUND),};

  private static final Map<String, TextAttributesKey> ADDITIONAL_HIGHLIGHT_DESCRIPTORS = new HashMap<String, TextAttributesKey>();
  public static final String DISPLAY_NAME = OptionsBundle.message("options.general.display.name");

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
        ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put(getHighlightDescTagName(highlightInfoType), highlightInfoType.getAttributesKey());
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
      return FileUtil.loadTextAndClose(getClass().getResourceAsStream("/colorSettingPage/general.txt"), true) + getCustomSeveritiesDemoText();
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
