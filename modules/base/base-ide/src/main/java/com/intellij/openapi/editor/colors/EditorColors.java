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
package com.intellij.openapi.editor.colors;

import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import consulo.ui.style.StandardColors;

public interface EditorColors {
  EditorColorKey CARET_ROW_COLOR = EditorColorKey.createColorKey("CARET_ROW_COLOR");
  EditorColorKey CARET_COLOR = EditorColorKey.createColorKey("CARET_COLOR");
  EditorColorKey RIGHT_MARGIN_COLOR = EditorColorKey.createColorKey("RIGHT_MARGIN_COLOR");
  EditorColorKey LINE_NUMBERS_COLOR = EditorColorKey.createColorKey("LINE_NUMBERS_COLOR");
  EditorColorKey LINE_NUMBER_ON_CARET_ROW_COLOR = EditorColorKey.createColorKey("LINE_NUMBER_ON_CARET_ROW_COLOR");
  EditorColorKey ANNOTATIONS_COLOR = EditorColorKey.createColorKey("ANNOTATIONS_COLOR");
  EditorColorKey READONLY_BACKGROUND_COLOR = EditorColorKey.createColorKey("READONLY_BACKGROUND");
  EditorColorKey READONLY_FRAGMENT_BACKGROUND_COLOR = EditorColorKey.createColorKey("READONLY_FRAGMENT_BACKGROUND");
  EditorColorKey WHITESPACES_COLOR = EditorColorKey.createColorKey("WHITESPACES");
  EditorColorKey INDENT_GUIDE_COLOR = EditorColorKey.createColorKey("INDENT_GUIDE");
  EditorColorKey SOFT_WRAP_SIGN_COLOR = EditorColorKey.createColorKey("SOFT_WRAP_SIGN_COLOR");
  EditorColorKey SELECTED_INDENT_GUIDE_COLOR = EditorColorKey.createColorKey("SELECTED_INDENT_GUIDE");
  EditorColorKey SELECTION_BACKGROUND_COLOR = EditorColorKey.createColorKey("SELECTION_BACKGROUND");
  EditorColorKey SELECTION_FOREGROUND_COLOR = EditorColorKey.createColorKey("SELECTION_FOREGROUND");

  TextAttributesKey REFERENCE_HYPERLINK_COLOR = TextAttributesKey.createTextAttributesKey("CTRL_CLICKABLE", new TextAttributes(StandardColors.BLUE, null, StandardColors.BLUE, EffectType.LINE_UNDERSCORE, 0));

  TextAttributesKey SEARCH_RESULT_ATTRIBUTES = TextAttributesKey.createTextAttributesKey("SEARCH_RESULT_ATTRIBUTES");
  TextAttributesKey LIVE_TEMPLATE_ATTRIBUTES = TextAttributesKey.createTextAttributesKey("LIVE_TEMPLATE_ATTRIBUTES");
  TextAttributesKey WRITE_SEARCH_RESULT_ATTRIBUTES = TextAttributesKey.createTextAttributesKey("WRITE_SEARCH_RESULT_ATTRIBUTES");
  TextAttributesKey IDENTIFIER_UNDER_CARET_ATTRIBUTES = TextAttributesKey.createTextAttributesKey("IDENTIFIER_UNDER_CARET_ATTRIBUTES");
  TextAttributesKey WRITE_IDENTIFIER_UNDER_CARET_ATTRIBUTES = TextAttributesKey.createTextAttributesKey("WRITE_IDENTIFIER_UNDER_CARET_ATTRIBUTES");
  TextAttributesKey TEXT_SEARCH_RESULT_ATTRIBUTES = TextAttributesKey.createTextAttributesKey("TEXT_SEARCH_RESULT_ATTRIBUTES");

  TextAttributesKey FOLDED_TEXT_ATTRIBUTES = TextAttributesKey.createTextAttributesKey("FOLDED_TEXT_ATTRIBUTES");
  EditorColorKey FOLDED_TEXT_BORDER_COLOR = EditorColorKey.createColorKey("FOLDED_TEXT_BORDER_COLOR");
  TextAttributesKey DELETED_TEXT_ATTRIBUTES = TextAttributesKey.createTextAttributesKey("DELETED_TEXT_ATTRIBUTES");

  EditorColorKey GUTTER_BACKGROUND = EditorColorKey.createColorKey("GUTTER_BACKGROUND");
  @Deprecated
  EditorColorKey LEFT_GUTTER_BACKGROUND = GUTTER_BACKGROUND;
  EditorColorKey NOTIFICATION_BACKGROUND = EditorColorKey.createColorKey("NOTIFICATION_BACKGROUND");

  EditorColorKey TEARLINE_COLOR = EditorColorKey.createColorKey("TEARLINE_COLOR");
  EditorColorKey SELECTED_TEARLINE_COLOR = EditorColorKey.createColorKey("SELECTED_TEARLINE_COLOR");
  @Deprecated
  EditorColorKey FOLDING_TREE_COLOR = TEARLINE_COLOR;
  @Deprecated
  EditorColorKey SELECTED_FOLDING_TREE_COLOR = SELECTED_TEARLINE_COLOR;

  EditorColorKey ADDED_LINES_COLOR = EditorColorKey.createColorKey("ADDED_LINES_COLOR");
  EditorColorKey MODIFIED_LINES_COLOR = EditorColorKey.createColorKey("MODIFIED_LINES_COLOR");
  EditorColorKey DELETED_LINES_COLOR = EditorColorKey.createColorKey("DELETED_LINES_COLOR");
  EditorColorKey WHITESPACES_MODIFIED_LINES_COLOR = EditorColorKey.createColorKey("WHITESPACES_MODIFIED_LINES_COLOR");
  EditorColorKey BORDER_LINES_COLOR = EditorColorKey.createColorKey("BORDER_LINES_COLOR");

  TextAttributesKey BREADCRUMBS_DEFAULT = TextAttributesKey.createTextAttributesKey("BREADCRUMBS_DEFAULT");
  TextAttributesKey BREADCRUMBS_HOVERED = TextAttributesKey.createTextAttributesKey("BREADCRUMBS_HOVERED");
  TextAttributesKey BREADCRUMBS_CURRENT = TextAttributesKey.createTextAttributesKey("BREADCRUMBS_CURRENT");
  TextAttributesKey BREADCRUMBS_INACTIVE = TextAttributesKey.createTextAttributesKey("BREADCRUMBS_INACTIVE");

  TextAttributesKey INJECTED_LANGUAGE_FRAGMENT = TextAttributesKey.createTextAttributesKey("INJECTED_LANGUAGE_FRAGMENT");

  EditorColorKey VISUAL_INDENT_GUIDE_COLOR = EditorColorKey.createColorKey("VISUAL_INDENT_GUIDE");
}
