// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupFocusDegree;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author yole
 */
public interface LookupElementListPresenter {
  @Nonnull
  String getAdditionalPrefix();

  @Nullable
  LookupElement getCurrentItem();

  @Nullable
  LookupElement getCurrentItemOrEmpty();

  boolean isSelectionTouched();

  int getSelectedIndex();

  int getLastVisibleIndex();

  LookupFocusDegree getLookupFocusDegree();

  boolean isShown();
}
