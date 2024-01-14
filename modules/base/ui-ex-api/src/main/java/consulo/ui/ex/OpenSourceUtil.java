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
package consulo.ui.ex;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.navigation.Navigatable;
import consulo.navigation.StatePreservingNavigatable;
import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

public class OpenSourceUtil {

  private OpenSourceUtil() {
  }

  @Nonnull
  public static CompletableFuture<?> openSourcesFromAsync(DataContext context, boolean requestFocus) {
    return navigateAsync(requestFocus, context.getData(Navigatable.KEY_OF_ARRAY));
  }

  @Nonnull
  public static CompletableFuture<?> navigateAsync(final boolean requestFocus, final Navigatable... navigatables) {
    if (navigatables == null) return CompletableFuture.completedFuture(null);
    CompletableFuture[] futures = new CompletableFuture[navigatables.length];
    for (int i = 0; i < navigatables.length; i++) {
      Navigatable navigatable = navigatables[i];
      futures[i] = navigatable.canNavigate() ? navigatable.navigateAsync(requestFocus) : CompletableFuture.completedFuture(null);
    }
    return CompletableFuture.allOf(futures);
  }

  // region Deprecated Stuff

  @Deprecated
  public static void openSourcesFrom(DataContext context, boolean requestFocus) {
    navigate(requestFocus, context.getData(Navigatable.KEY_OF_ARRAY));
  }

  @Deprecated
  public static void openSourcesFrom(DataProvider context, boolean requestFocus) {
    navigate(requestFocus, context.getDataUnchecked(Navigatable.KEY_OF_ARRAY));
  }

  /**
   * Equivalent to navigate(true, navigatables)
   *
   * @param navigatables elements navigate to
   *
   * @see OpenSourceUtil#navigate(boolean, Navigatable...)
   */
  @Deprecated
  public static void navigate(final Navigatable...navigatables) {
    navigate(true, navigatables);
  }

  @Deprecated
  public static void navigate(final boolean requestFocus, final Navigatable...navigatables) {
    if (navigatables == null) return;
    for (Navigatable navigatable : navigatables) {
      if (navigatable.canNavigate()) {
        navigatable.navigate(requestFocus);
      }
    }
  }

  @Deprecated
  public static void navigate(final boolean requestFocus, final boolean tryNotToScroll, final Navigatable...navigatables) {
    if (navigatables == null) return;
    for (Navigatable navigatable : navigatables) {
      if (navigatable.canNavigate()) {
        if (tryNotToScroll && navigatable instanceof StatePreservingNavigatable) {
          ((StatePreservingNavigatable)navigatable).navigate(requestFocus, true);
        } else {
          navigatable.navigate(requestFocus);
        }
      }
    }
  }
  // endregion
}
