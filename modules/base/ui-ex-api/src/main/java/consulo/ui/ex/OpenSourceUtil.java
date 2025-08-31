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

public class OpenSourceUtil {

  private OpenSourceUtil() {
  }

  public static void openSourcesFrom(DataContext context, boolean requestFocus) {
    navigate(requestFocus, context.getData(Navigatable.KEY_OF_ARRAY));
  }

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
  public static void navigate(Navigatable...navigatables) {
    navigate(true, navigatables);
  }

  public static void navigate(boolean requestFocus, Navigatable...navigatables) {
    if (navigatables == null) return;
    for (Navigatable navigatable : navigatables) {
      if (navigatable.canNavigate()) {
        navigatable.navigate(requestFocus);
      }
    }
  }

  public static void navigate(boolean requestFocus, boolean tryNotToScroll, Navigatable...navigatables) {
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
}
