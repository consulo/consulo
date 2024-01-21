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
package consulo.navigation;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

/**
 * Navigatable that saves cursor position in the editor when navigating to
 * already opened documents
 *
 * @author Konstantin Bulenkov
 */
public interface StatePreservingNavigatable extends Navigatable {
  @Override
  default void navigate(boolean requestFocus) {
    navigate(requestFocus, false);
  }

  void navigate(boolean requestFocus, boolean preserveState);

  @Nonnull
  default CompletableFuture<?> navigateAsync(boolean requestFocus, boolean preserveState) {
    navigate(requestFocus, preserveState);
    return CompletableFuture.completedFuture(null);
  }
}
