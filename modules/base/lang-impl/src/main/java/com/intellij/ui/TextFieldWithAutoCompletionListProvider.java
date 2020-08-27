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
package com.intellij.ui;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author Roman.Chernyatchik
 */
public abstract class TextFieldWithAutoCompletionListProvider<T> implements Comparator<T> {
  @Nonnull
  private Collection<T> myVariants;
  @Nullable
  private String myCompletionAdvertisement;

  @Nullable
  protected abstract Image getIcon(@Nonnull final T item);

  @Nonnull
  protected abstract String getLookupString(@Nonnull final T item);

  @Nullable
  protected abstract String getTailText(@Nonnull final T item);

  @Nullable
  protected abstract String getTypeText(@Nonnull final T item);

  @Override
  public abstract int compare(T item1, T item2);

  protected TextFieldWithAutoCompletionListProvider(@Nullable final Collection<T> variants) {
    setItems(variants);
    myCompletionAdvertisement = null;
  }

  public void setItems(@Nullable final Collection<T> variants) {
    myVariants = (variants != null) ? variants : Collections.<T>emptyList();
  }

  @Nonnull
  public Collection<T> getItems(String prefix, boolean cached, CompletionParameters parameters) {
    if (prefix == null) {
      return Collections.emptyList();
    }

    final List<T> items = new ArrayList<T>(myVariants);

    Collections.sort(items, this);
    return items;
  }

  /**
   * Completion list advertisement for documentation popup
   *
   * @param shortcut
   * @return text
   */
  @Nullable
  public String getQuickDocHotKeyAdvertisement(@Nonnull final String shortcut) {
    final String advertisementTail = getQuickDocHotKeyAdvertisementTail(shortcut);
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
  @Nullable
  public String getAdvertisement() {
    return myCompletionAdvertisement;
  }

  public void setAdvertisement(@Nullable final String completionAdvertisement) {
    myCompletionAdvertisement = completionAdvertisement;
  }

  @Nullable
  public PrefixMatcher createPrefixMatcher(@Nonnull final String prefix) {
    return new PlainPrefixMatcher(prefix);
  }

  public LookupElementBuilder createLookupBuilder(@Nonnull final T item) {
    LookupElementBuilder builder = LookupElementBuilder.create(item, getLookupString(item))
      .withIcon(getIcon(item));

    final InsertHandler<LookupElement> handler = createInsertHandler(item);
    if (handler != null) {
      builder = builder.withInsertHandler(handler);
    }

    final String tailText = getTailText(item);
    if (tailText != null) {
      builder = builder.withTailText(tailText, true);
    }

    final String typeText = getTypeText(item);
    if (typeText != null) {
      builder = builder.withTypeText(typeText);
    }
    return builder;
  }

  @Nullable
  public String getPrefix(@Nonnull final CompletionParameters parameters) {
    return getCompletionPrefix(parameters);
  }

  public static String getCompletionPrefix(CompletionParameters parameters) {
    String text = parameters.getOriginalFile().getText();
    int i = text.lastIndexOf(' ', parameters.getOffset() - 1) + 1;
    return text.substring(i, parameters.getOffset());
  }

  @Nullable
  protected String getQuickDocHotKeyAdvertisementTail(@Nonnull final String shortcut) {
    return null;
  }

  @Nullable
  protected InsertHandler<LookupElement> createInsertHandler(@Nonnull final T item) {
    return null;
  }
}
