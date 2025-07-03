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
package consulo.language.impl.internal.plain;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.impl.ast.LeafElement;
import consulo.language.impl.ast.ASTLeafFactory;
import consulo.language.version.LanguageVersion;
import consulo.language.plain.ast.PlainTextTokenTypes;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
@ExtensionImpl
public class PlainTextASTLeafFactory implements ASTLeafFactory {
  @Nonnull
  @Override
  public LeafElement createLeaf(@Nonnull IElementType type, @Nonnull LanguageVersion languageVersion, @Nonnull CharSequence text) {
    return new PsiPlainTextImpl(text);
  }

  @Override
  public boolean test(@Nullable IElementType input) {
    return input == PlainTextTokenTypes.PLAIN_TEXT;
  }
}
