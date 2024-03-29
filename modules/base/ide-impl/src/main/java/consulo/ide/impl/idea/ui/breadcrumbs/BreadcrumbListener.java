// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.breadcrumbs;

import consulo.ide.impl.idea.ui.components.breadcrumbs.Crumb;
import jakarta.annotation.Nonnull;


public interface BreadcrumbListener {
  /**
   * Listener for breadcrumbs updates
   *
   * @param crumbs updated collection of breadcrumbs
   */
  void breadcrumbsChanged(@Nonnull Iterable<? extends Crumb> crumbs);
}
