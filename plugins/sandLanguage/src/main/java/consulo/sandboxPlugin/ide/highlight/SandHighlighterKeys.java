/*
 * Copyright 2013-2016 consulo.io
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
package consulo.sandboxPlugin.ide.highlight;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import consulo.sandboxPlugin.lang.SandLanguage;

/**
 * @author VISTALL
 * @since 19.03.14
 */
public interface SandHighlighterKeys {
  TextAttributesKey KEYWORD = TextAttributesKey.createTextAttributesKey(SandLanguage.INSTANCE, DefaultLanguageHighlighterColors.KEYWORD);
  TextAttributesKey LINE_COMMENT = TextAttributesKey.createTextAttributesKey(SandLanguage.INSTANCE, DefaultLanguageHighlighterColors.LINE_COMMENT);
}
