/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.navigation;

import consulo.annotation.access.RequiredReadAction;
import consulo.ui.UIAccess;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link Navigatable} interface which actually doesn't allow navigation. Its {@link #INSTANCE} can be passed to methods which
 * expect non-null instance of {@link Navigatable} if you cannot provide a real implementation.
 */
public final class NonNavigatable implements Navigatable {
  public static final Navigatable INSTANCE = new NonNavigatable();

  private NonNavigatable() {
  }

  @RequiredReadAction
  @Override
  public NavigateOptions getNavigateOptions() {
    return NavigateOptions.CANT_NAVIGATE;
  }

    @Override
    public CompletableFuture<?> navigateAsync(UIAccess uiAccess, boolean requestFocus) {
        return CompletableFuture.completedFuture(null);
    }
}
