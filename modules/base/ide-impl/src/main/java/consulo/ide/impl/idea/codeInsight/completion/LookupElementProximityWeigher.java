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
package consulo.ide.impl.idea.codeInsight.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.completion.CompletionLocation;
import consulo.language.editor.completion.CompletionWeigher;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.ide.impl.idea.openapi.util.NullableComputable;
import consulo.language.psi.PsiElement;
import consulo.ide.impl.psi.util.proximity.PsiProximityComparator;
import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
@ExtensionImpl(id = "proximity", order = "after explicitProximity")
public class LookupElementProximityWeigher extends CompletionWeigher {

  @Override
  public Comparable weigh(@Nonnull final LookupElement item, @Nonnull final CompletionLocation location) {
    if (item.getObject() instanceof PsiElement) {
      return PsiProximityComparator.getProximity((NullableComputable<PsiElement>)() -> item.getPsiElement(), location.getCompletionParameters().getPosition(), location.getProcessingContext());
    }
    return null;
  }
}
