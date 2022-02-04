/*
 * Copyright 2013-2021 consulo.io
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
package com.intellij.codeWithMe;

import javax.annotation.Nonnull;

// from kotlin - stub
public class ClientId {
  public static class Companion {
    public static boolean getPropagateAcrossThreads() {
      return false;
    }
  }

  public static void withClientId(ClientId clientId, Runnable runnable) {
    runnable.run();
  }

  public static <T> T withClientId(ClientId clientId, T action) {
    return action;
  }

  public static <T> T decorateFunction(T func) {
    return func;
  }

  public static <T> T decorateBiConsumer(T b) {
    return b;
  }

  private static ClientId ourLocal = new ClientId();

  @Nonnull
  public static ClientId getCurrent() {
    return ourLocal;
  }

  public static boolean isCurrentlyUnderLocalId() {
    return true;
  }
}
