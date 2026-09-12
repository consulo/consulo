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

import consulo.language.ast.ElementTypeAsPsiFactory;
import consulo.language.ast.IElementType;
import consulo.sandboxPlugin.lang.SandLanguage;

/**
 * @author VISTALL
 * @since 19.03.14
 */
public interface SandElements {
  IElementType CLASS = SandStubTokenType.CLASS;
  IElementType DEF = new ElementTypeAsPsiFactory("DEF", SandLanguage.INSTANCE, SandDef::new);
  IElementType STRING_EXPRESSION = new ElementTypeAsPsiFactory("STRING_EXPRESSION", SandLanguage.INSTANCE, SandStringExpression::new);
  IElementType EXTENDS_REF = new ElementTypeAsPsiFactory("EXTENDS_REF", SandLanguage.INSTANCE, SandExtendsRef::new);
  IElementType FLAG_DIRECTIVE = new IElementType("FLAG_DIRECTIVE", SandLanguage.INSTANCE);
  IElementType UNDEF_DIRECTIVE = new IElementType("UNDEF_DIRECTIVE", SandLanguage.INSTANCE);
  IElementType INCLUDE_DIRECTIVE = new ElementTypeAsPsiFactory("INCLUDE_DIRECTIVE", SandLanguage.INSTANCE, SandIncludeDirective::new);
  IElementType IF_DIRECTIVE = new IElementType("IF_DIRECTIVE", SandLanguage.INSTANCE);
  IElementType IFNDEF_DIRECTIVE = new IElementType("IFNDEF_DIRECTIVE", SandLanguage.INSTANCE);
  IElementType ELIF_DIRECTIVE = new IElementType("ELIF_DIRECTIVE", SandLanguage.INSTANCE);
  IElementType ELSE_DIRECTIVE = new IElementType("ELSE_DIRECTIVE", SandLanguage.INSTANCE);
  IElementType END_DIRECTIVE = new IElementType("END_DIRECTIVE", SandLanguage.INSTANCE);
  IElementType RAW_BLOCK = new IElementType("RAW_BLOCK", SandLanguage.INSTANCE);
}
