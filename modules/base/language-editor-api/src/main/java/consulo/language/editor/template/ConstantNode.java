// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
/*
 * Copyright 2013-2026 consulo.io
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

package consulo.language.editor.template;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

public class ConstantNode extends Expression {
  private final @Nullable Result myValue;
  private final LookupElement[] myLookupElements;

  private final @Nullable String myPopupAdvertisement;

  public ConstantNode(String value) {
    this(new TextResult(value));
  }

  public ConstantNode(@Nullable Result value) {
    this(value, LookupElement.EMPTY_ARRAY);
  }

  private ConstantNode(@Nullable Result value, LookupElement... lookupElements) {
    this(value, null, lookupElements);
  }

  private ConstantNode(@Nullable Result value, @Nullable String popupAdvertisement, LookupElement... lookupElements) {
    myValue = value;
    myPopupAdvertisement = popupAdvertisement;
    myLookupElements = lookupElements;
  }

  public ConstantNode withLookupItems(LookupElement... lookupElements) {
    return new ConstantNode(myValue, myPopupAdvertisement, lookupElements);
  }

  public ConstantNode withLookupItems(Collection<? extends LookupElement> lookupElements) {
    return new ConstantNode(myValue, myPopupAdvertisement, lookupElements.toArray(LookupElement.EMPTY_ARRAY));
  }

  public ConstantNode withLookupStrings(String... lookupElements) {
    return new ConstantNode(myValue, myPopupAdvertisement,
                            ContainerUtil.map2Array(lookupElements, LookupElement.class, LookupElementBuilder::create));
  }

  public ConstantNode withLookupStrings(Collection<String> lookupElements) {
    return new ConstantNode(myValue, myPopupAdvertisement,
                            ContainerUtil.map2Array(lookupElements, LookupElement.EMPTY_ARRAY, LookupElementBuilder::create));
  }

  public ConstantNode withPopupAdvertisement(@Nullable String popupAdvertisement) {
    return new ConstantNode(myValue, popupAdvertisement, myLookupElements);
  }

  @Override
  public @Nullable Result calculateResult(ExpressionContext context) {
    return myValue;
  }

  @Override
  public @Nullable Result calculateQuickResult(ExpressionContext context) {
    return myValue;
  }

  @Override
  public boolean requiresCommittedPSI() {
    return false;
  }

  @Override
  public LookupElement[] calculateLookupItems(ExpressionContext context) {
    return myLookupElements;
  }

  @Override
  public @Nullable String getAdvertisingText() {
    return myPopupAdvertisement;
  }
}
