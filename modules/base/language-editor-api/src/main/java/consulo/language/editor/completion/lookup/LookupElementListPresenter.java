// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.completion.lookup;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
