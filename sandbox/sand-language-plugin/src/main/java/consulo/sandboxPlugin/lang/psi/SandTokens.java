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
package consulo.sandboxPlugin.lang.psi;

import consulo.language.ast.IElementType;
import consulo.language.ast.StandardTokenTypes;
import consulo.sandboxPlugin.lang.SandLanguage;

/**
 * @author VISTALL
 * @since 19.03.14
 */
public interface SandTokens extends StandardTokenTypes {
  IElementType IDENTIFIER = new IElementType("IDENTIFIER", SandLanguage.INSTANCE);
  IElementType CLASS_KEYWORD = new IElementType("CLASS_KEYWORD", SandLanguage.INSTANCE);
  IElementType LINE_COMMENT = new IElementType("LINE_COMMENT", SandLanguage.INSTANCE);
  IElementType LBRACE = new IElementType("LBRACE", SandLanguage.INSTANCE);
  IElementType RBRACE = new IElementType("RBRACE", SandLanguage.INSTANCE);
  IElementType STRING_LITERAL = new IElementType("STRING_LITERAL", SandLanguage.INSTANCE);
  IElementType COLON = new IElementType("COLON", SandLanguage.INSTANCE);
  IElementType FLAG_KEYWORD = new IElementType("FLAG_KEYWORD", SandLanguage.INSTANCE);
  IElementType UNDEF_KEYWORD = new IElementType("UNDEF_KEYWORD", SandLanguage.INSTANCE);
  IElementType IF_KEYWORD = new IElementType("IF_KEYWORD", SandLanguage.INSTANCE);
  IElementType IFNDEF_KEYWORD = new IElementType("IFNDEF_KEYWORD", SandLanguage.INSTANCE);
  IElementType ELIF_KEYWORD = new IElementType("ELIF_KEYWORD", SandLanguage.INSTANCE);
  IElementType ELSE_KEYWORD = new IElementType("ELSE_KEYWORD", SandLanguage.INSTANCE);
  IElementType END_KEYWORD = new IElementType("END_KEYWORD", SandLanguage.INSTANCE);
  IElementType INCLUDE_KEYWORD = new IElementType("INCLUDE_KEYWORD", SandLanguage.INSTANCE);
  IElementType NON_ACTIVE_SYMBOL = new IElementType("NON_ACTIVE_SYMBOL", SandLanguage.INSTANCE);
}
