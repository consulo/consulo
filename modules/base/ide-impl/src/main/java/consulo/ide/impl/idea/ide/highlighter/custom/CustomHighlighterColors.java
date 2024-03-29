/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ide.highlighter.custom;

import consulo.colorScheme.TextAttributesKey;

public interface CustomHighlighterColors {
  TextAttributesKey CUSTOM_KEYWORD1_ATTRIBUTES = TextAttributesKey.createTextAttributesKey("CUSTOM_KEYWORD1_ATTRIBUTES");
  TextAttributesKey CUSTOM_KEYWORD2_ATTRIBUTES = TextAttributesKey.createTextAttributesKey("CUSTOM_KEYWORD2_ATTRIBUTES");
  TextAttributesKey CUSTOM_KEYWORD3_ATTRIBUTES = TextAttributesKey.createTextAttributesKey("CUSTOM_KEYWORD3_ATTRIBUTES");
  TextAttributesKey CUSTOM_KEYWORD4_ATTRIBUTES = TextAttributesKey.createTextAttributesKey("CUSTOM_KEYWORD4_ATTRIBUTES");
  TextAttributesKey CUSTOM_NUMBER_ATTRIBUTES = TextAttributesKey.createTextAttributesKey("CUSTOM_NUMBER_ATTRIBUTES");
  TextAttributesKey CUSTOM_STRING_ATTRIBUTES = TextAttributesKey.createTextAttributesKey("CUSTOM_STRING_ATTRIBUTES");
  TextAttributesKey CUSTOM_LINE_COMMENT_ATTRIBUTES = TextAttributesKey.createTextAttributesKey("CUSTOM_LINE_COMMENT_ATTRIBUTES");
  TextAttributesKey CUSTOM_MULTI_LINE_COMMENT_ATTRIBUTES = TextAttributesKey.createTextAttributesKey("CUSTOM_MULTI_LINE_COMMENT_ATTRIBUTES");
  TextAttributesKey CUSTOM_VALID_STRING_ESCAPE = TextAttributesKey.createTextAttributesKey("CUSTOM_VALID_STRING_ESCAPE_ATTRIBUTES");
  TextAttributesKey CUSTOM_INVALID_STRING_ESCAPE = TextAttributesKey.createTextAttributesKey("CUSTOM_INVALID_STRING_ESCAPE_ATTRIBUTES");
}
