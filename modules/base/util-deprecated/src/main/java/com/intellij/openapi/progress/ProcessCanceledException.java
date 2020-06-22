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
package com.intellij.openapi.progress;

import com.intellij.openapi.diagnostic.ControlFlowException;

import javax.annotation.Nullable;

public class ProcessCanceledException extends RuntimeException implements ControlFlowException {
  private static final boolean ourDebugCreationTrace = Boolean.getBoolean("consulo.debug.creationg.trace");

  private final Exception myCreationTrace;

  public ProcessCanceledException() {
    this(null);
  }

  public ProcessCanceledException(@Nullable Throwable cause) {
    super(cause);

    if (ourDebugCreationTrace) {
      myCreationTrace = new Exception();
    }
    else {
      myCreationTrace = null;
    }
  }

  @Nullable
  public Exception getCreationTrace() {
    return myCreationTrace;
  }
}
