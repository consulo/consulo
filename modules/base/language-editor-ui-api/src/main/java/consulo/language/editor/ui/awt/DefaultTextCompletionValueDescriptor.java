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

import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.util.lang.StringUtil;
import consulo.ui.image.Image;

import org.jspecify.annotations.Nullable;

public abstract class DefaultTextCompletionValueDescriptor<T> implements TextCompletionValueDescriptor<T> {
  
  protected abstract String getLookupString(T item);

  protected @Nullable Image getIcon(T item) {
    return null;
  }

  protected @Nullable String getTailText(T item) {
    return null;
  }

  protected @Nullable String getTypeText(T item) {
    return null;
  }

  protected @Nullable InsertHandler<LookupElement> createInsertHandler(T item) {
    return null;
  }

  @Override
  public int compare(T item1, T item2) {
    return StringUtil.compare(getLookupString(item1), getLookupString(item2), false);
  }

  
  @Override
  public LookupElementBuilder createLookupBuilder(T item) {
    LookupElementBuilder builder = LookupElementBuilder.create(item, getLookupString(item))
            .withIcon(getIcon(item));

    InsertHandler<LookupElement> handler = createInsertHandler(item);
    if (handler != null) {
      builder = builder.withInsertHandler(handler);
    }

    String tailText = getTailText(item);
    if (tailText != null) {
      builder = builder.withTailText(tailText, true);
    }

    String typeText = getTypeText(item);
    if (typeText != null) {
      builder = builder.withTypeText(typeText);
    }
    return builder;
  }

  public static class StringValueDescriptor extends DefaultTextCompletionValueDescriptor<String> {
    
    @Override
    public String getLookupString(String item) {
      return item;
    }
  }
}
