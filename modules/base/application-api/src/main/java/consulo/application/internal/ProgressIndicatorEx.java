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
package consulo.application.internal;

import consulo.annotation.DeprecationInfo;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.TaskInfo;

import jakarta.annotation.Nonnull;

public interface ProgressIndicatorEx extends ProgressIndicator {
  @Deprecated
  @DeprecationInfo("Prefer #addListener(ProgressIndicatorListener)")
  void addStateDelegate(@Nonnull ProgressIndicatorEx delegate);

  void finish(@Nonnull TaskInfo task);

  boolean isFinished(@Nonnull TaskInfo task);

  boolean wasStarted();

  void processFinish();

  void initStateFrom(@Nonnull ProgressIndicator indicator);
}
