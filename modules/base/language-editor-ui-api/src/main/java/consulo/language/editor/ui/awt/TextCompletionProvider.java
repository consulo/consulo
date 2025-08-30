/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.editor.ui.awt;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.CharFilter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Base interface for text completion.
 * Default implementations are: {@link ValuesCompletionProvider} for completion from a fixed set of elements
 * and {@link TextFieldCompletionProvider} for other cases.
 * <p>
 * Use {@link TextFieldWithCompletion} to create a text field component with completion.
 */
public interface TextCompletionProvider {
  @Nullable
  String getAdvertisement();

  @Nullable
  String getPrefix(@Nonnull String text, int offset);

  @Nonnull
  CompletionResultSet applyPrefixMatcher(@Nonnull CompletionResultSet result, @Nonnull String prefix);

  @Nullable
  CharFilter.Result acceptChar(char c);

  void fillCompletionVariants(@Nonnull CompletionParameters parameters, @Nonnull String prefix, @Nonnull CompletionResultSet result);
}
