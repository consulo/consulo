/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.language.ast;

import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.internal.DefaultTokenSeparatorGenerator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author yole
 */
public interface TokenSeparatorGenerator extends LanguageExtension {
  ExtensionPointCacheKey<TokenSeparatorGenerator, ByLanguageValue<TokenSeparatorGenerator>> KEY =
          ExtensionPointCacheKey.create("TokenSeparatorGenerator", LanguageOneToOne.build(new DefaultTokenSeparatorGenerator()));

  @Nonnull
  static TokenSeparatorGenerator forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(TokenSeparatorGenerator.class).getOrBuildCache(KEY).requiredGet(language);
  }

  @Nullable
  ASTNode generateWhitespaceBetweenTokens(ASTNode left, ASTNode right);
}
