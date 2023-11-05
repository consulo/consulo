/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.application.impl.internal.progress;

import consulo.application.internal.ProgressIndicatorEx;
import consulo.application.progress.ProgressManager;
import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

public interface BlockingProgressIndicator extends ProgressIndicatorEx {
  /**
   * @deprecated Do not use, it's too low level and dangerous. Instead, consider using run* methods in {@link ProgressManager} or {@link ProgressRunner}
   */
  @Deprecated
  void startBlocking(@Nonnull Runnable init, @Nonnull CompletableFuture<?> stopCondition);
}