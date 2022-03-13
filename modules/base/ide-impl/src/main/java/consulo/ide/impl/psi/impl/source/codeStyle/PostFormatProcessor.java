/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.psi.impl.source.codeStyle;

import consulo.component.extension.ExtensionPointName;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.codeStyle.CodeStyleSettings;
import javax.annotation.Nonnull;

public interface PostFormatProcessor {
  ExtensionPointName<PostFormatProcessor> EP_NAME = ExtensionPointName.create("consulo.postFormatProcessor");

  PsiElement processElement(@Nonnull PsiElement source, @Nonnull CodeStyleSettings settings);
  TextRange processText(@Nonnull PsiFile source, @Nonnull TextRange rangeToReformat, @Nonnull CodeStyleSettings settings);
}
