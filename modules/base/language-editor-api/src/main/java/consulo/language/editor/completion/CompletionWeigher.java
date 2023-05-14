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
package consulo.language.editor.completion;

import consulo.language.Weigher;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
public abstract class CompletionWeigher extends Weigher<LookupElement, CompletionLocation> {

  @Override
  public abstract Comparable weigh(@Nonnull final LookupElement element, @Nonnull final CompletionLocation location);

  @Nonnull
  @Override
  public Key<?> getKey() {
    return CompletionService.RELEVANCE_KEY;
  }
}
