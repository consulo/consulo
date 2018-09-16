/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.history;

import javax.annotation.Nonnull;


/**
 * Wrapper for any exception occurred during local history actions and processes
 */
public class LocalHistoryException extends Exception {
  public LocalHistoryException(@Nonnull String message) {
    super(message);
  }

  public LocalHistoryException(@Nonnull String message, @javax.annotation.Nullable Throwable cause) {
    super(message, cause);
  }
}
