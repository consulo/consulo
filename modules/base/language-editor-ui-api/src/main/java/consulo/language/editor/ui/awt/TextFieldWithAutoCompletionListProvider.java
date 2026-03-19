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
package consulo.language.editor.ui.awt;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.application.util.matcher.PlainPrefixMatcher;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.ui.image.Image;

import org.jspecify.annotations.Nullable;
import java.util.*;

/**
 * @author Roman.Chernyatchik
 */
public abstract class TextFieldWithAutoCompletionListProvider<T> implements Comparator<T> {
  
  private Collection<T> myVariants;
  private @Nullable String myCompletionAdvertisement;

  protected abstract @Nullable Image getIcon(T item);

  
  protected abstract String getLookupString(T item);

  protected abstract @Nullable String getTailText(T item);

  protected abstract @Nullable String getTypeText(T item);

  @Override
  public abstract int compare(T item1, T item2);

  protected TextFieldWithAutoCompletionListProvider(@Nullable Collection<T> variants) {
    setItems(variants);
    myCompletionAdvertisement = null;
  }

  public void setItems(@Nullable Collection<T> variants) {
    myVariants = (variants != null) ? variants : Collections.<T>emptyList();
  }

  
  public Collection<T> getItems(String prefix, boolean cached, CompletionParameters parameters) {
    if (prefix == null) {
      return Collections.emptyList();
    }

    List<T> items = new ArrayList<T>(myVariants);

    Collections.sort(items, this);
    return items;
  }

  /**
   * Completion list advertisement for documentation popup
   *
   * @param shortcut
   * @return text
   */
  public @Nullable String getQuickDocHotKeyAdvertisement(String shortcut) {
    String advertisementTail = getQuickDocHotKeyAdvertisementTail(shortcut);
    if (advertisementTail == null) {
      return null;
    }
    return "Pressing " + shortcut + " would show " + advertisementTail;
  }

  /**
   * Completion list advertisement text, if null advertisement for documentation
   * popup will be shown
   *
   * @return text
   */
  public @Nullable String getAdvertisement() {
    return myCompletionAdvertisement;
  }

  public void setAdvertisement(@Nullable String completionAdvertisement) {
    myCompletionAdvertisement = completionAdvertisement;
  }

  public @Nullable PrefixMatcher createPrefixMatcher(String prefix) {
    return new PlainPrefixMatcher(prefix);
  }

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

  public @Nullable String getPrefix(CompletionParameters parameters) {
    return getCompletionPrefix(parameters);
  }

  public static String getCompletionPrefix(CompletionParameters parameters) {
    String text = parameters.getOriginalFile().getText();
    int i = text.lastIndexOf(' ', parameters.getOffset() - 1) + 1;
    return text.substring(i, parameters.getOffset());
  }

  protected @Nullable String getQuickDocHotKeyAdvertisementTail(String shortcut) {
    return null;
  }

  protected @Nullable InsertHandler<LookupElement> createInsertHandler(T item) {
    return null;
  }
}
