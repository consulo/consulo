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
import consulo.language.editor.completion.lookup.LookupItem;
import consulo.language.editor.completion.lookup.PrioritizedLookupElement;

import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
@ExtensionImpl(id = "priority", order = "first")
public class PriorityWeigher extends CompletionWeigher {
  @Override
  public Double weigh(@Nonnull LookupElement element, @Nonnull CompletionLocation location) {
    final PrioritizedLookupElement prioritized = element.as(PrioritizedLookupElement.CLASS_CONDITION_KEY);
    if (prioritized != null) {
      return prioritized.getPriority();
    }

    final LookupItem item = element.as(LookupItem.CLASS_CONDITION_KEY);
    if (item != null) {
      return item.getPriority();
    }
    return 0.0;
  }
}
