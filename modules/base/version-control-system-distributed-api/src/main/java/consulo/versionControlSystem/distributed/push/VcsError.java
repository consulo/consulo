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
package consulo.versionControlSystem.distributed.push;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class VcsError {
  @Nonnull
  String myErrorText;
  @Nullable private final VcsErrorHandler myErrorHandleListener;

  public VcsError(@Nonnull String text) {
    this(text, null);
  }

  public VcsError(@Nonnull String text, @Nullable VcsErrorHandler listener) {
    myErrorText = text;
    myErrorHandleListener = listener;
  }

  public String getText() {
    return myErrorText;
  }

  public void handleError(@Nonnull CommitLoader loader) {
    if (myErrorHandleListener != null) {
      myErrorHandleListener.handleError(loader);
    }
  }

  public static VcsError createEmptyTargetError(@Nonnull String name) {
    return new VcsError("Please, specify not empty remote push path for repository " + name + ".");
  }
}
