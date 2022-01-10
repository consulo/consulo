// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.breadcrumbs;

import com.intellij.ui.components.breadcrumbs.Crumb;
import javax.annotation.Nonnull;


public interface BreadcrumbListener {
  /**
   * Listener for breadcrumbs updates
   *
   * @param crumbs updated collection of breadcrumbs
   */
  void breadcrumbsChanged(@Nonnull Iterable<? extends Crumb> crumbs);
}
