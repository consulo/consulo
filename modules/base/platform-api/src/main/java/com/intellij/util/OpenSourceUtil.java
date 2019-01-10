/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.util;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.pom.Navigatable;
import com.intellij.pom.StatePreservingNavigatable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class OpenSourceUtil {

  private OpenSourceUtil() {
  }

  @Deprecated
  public static void openSourcesFrom(DataContext context, boolean requestFocus) {
    navigate(requestFocus, context.getData(CommonDataKeys.NAVIGATABLE_ARRAY));
  }

  @Nonnull
  public static AsyncResult<Void> openSourcesFromAsync(DataContext context, boolean requestFocus) {
    return navigateAsync(requestFocus, context.getData(CommonDataKeys.NAVIGATABLE_ARRAY));
  }

  /**
   * Equivalent to navigate(true, navigatables)
   *
   * @param navigatables elements navigate to
   * @see OpenSourceUtil#navigate(boolean, com.intellij.pom.Navigatable...)
   */
  @Deprecated
  public static void navigate(final Navigatable... navigatables) {
    navigate(true, navigatables);
  }

  @Deprecated
  public static void navigate(final boolean requestFocus, final Navigatable... navigatables) {
    navigateAsync(requestFocus, navigatables).getResultSync();
  }

  @Nonnull
  public static AsyncResult<Void> navigateAsync(final boolean requestFocus, final Navigatable... navigatables) {
    if (navigatables == null) return AsyncResult.resolved();

    List<AsyncResult<Void>> results = new ArrayList<>(navigatables.length);
    for (Navigatable navigatable : navigatables) {
      if (navigatable.canNavigate()) {
        results.add(navigatable.navigateAsync(requestFocus));
      }
    }
    return AsyncResult.merge(results);
  }

  @Deprecated
  public static void navigate(final boolean requestFocus, final boolean tryNotToScroll, final Navigatable... navigatables) {
    if (navigatables == null) return;
    for (Navigatable navigatable : navigatables) {
      if (navigatable.canNavigate()) {
        if (tryNotToScroll && navigatable instanceof StatePreservingNavigatable) {
          ((StatePreservingNavigatable)navigatable).navigate(requestFocus, true);
        }
        else {
          navigatable.navigate(requestFocus);
        }
      }
    }
  }
}
