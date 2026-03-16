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

import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.navigation.Navigatable;
import consulo.navigation.StatePreservingNavigatable;
import consulo.ui.UIAccess;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import consulo.util.concurrent.coroutine.step.Iteration;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OpenSourceUtil {

  private OpenSourceUtil() {
  }

  public static void openSourcesFrom(DataContext context, boolean requestFocus) {
    navigate(requestFocus, context.getData(Navigatable.KEY_OF_ARRAY));
  }

  public static CompletableFuture<?> openSourcesFromAsync(UIAccess uiAccess, DataContext context, boolean requestFocus) {
    return navigateAsync(uiAccess, requestFocus, context.getData(Navigatable.KEY_OF_ARRAY));
  }

  /**
   * Equivalent to navigate(true, navigatables)
   *
   * @param navigatables elements navigate to
   *
   * @see OpenSourceUtil#navigate(boolean, Navigatable...)
   */
  public static void navigate(Navigatable...navigatables) {
    navigate(true, navigatables);
  }

  public static void navigate(boolean requestFocus, Navigatable...navigatables) {
    if (navigatables == null) return;
    for (Navigatable navigatable : navigatables) {
      if (navigatable.getNavigateOptions().canNavigate()) {
        navigatable.navigate(requestFocus);
      }
    }
  }

  public static void navigate(boolean requestFocus, boolean tryNotToScroll, Navigatable...navigatables) {
    if (navigatables == null) return;
    for (Navigatable navigatable : navigatables) {
      if (navigatable.getNavigateOptions().canNavigate()) {
        if (tryNotToScroll && navigatable instanceof StatePreservingNavigatable) {
          ((StatePreservingNavigatable)navigatable).navigate(requestFocus, true);
        } else {
          navigatable.navigate(requestFocus);
        }
      }
    }
  }

  public static CompletableFuture<?> navigateAsync(UIAccess uiAccess, Navigatable... navigatables) {
    return navigateAsync(uiAccess, true, navigatables);
  }

  @SuppressWarnings("unchecked")
  public static CompletableFuture<?> navigateAsync(UIAccess uiAccess, boolean requestFocus, Navigatable... navigatables) {
    if (navigatables == null || navigatables.length == 0) {
      return CompletableFuture.completedFuture(null);
    }

    return CoroutineScope.launchAsync(Application.get().coroutineContext(), () -> {
      return Coroutine
          .first(CodeExecution.<Void, List<Navigatable>>supply(() ->
              Arrays.stream(navigatables)
                  .filter(n -> n.getNavigateOptions().canNavigate())
                  .toList()))
          .then(Iteration.forEach(
              CompletableFutureStep.<Navigatable, Object>await(nav ->
                  (CompletableFuture<Object>) nav.navigateAsync(uiAccess, requestFocus))));
    }).toFuture();
  }
}
