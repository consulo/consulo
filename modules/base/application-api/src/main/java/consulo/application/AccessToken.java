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
package consulo.application;

import jakarta.annotation.Nonnull;

public abstract class AccessToken implements AutoCloseable {
  @Override
  public final void close() {
    finish();
  }

  public abstract void finish();

  public static final AccessToken EMPTY_ACCESS_TOKEN = new AccessToken() {
    @Override
    public void finish() {
    }
  };

  @Nonnull
  public static AccessToken of() {
    return EMPTY_ACCESS_TOKEN;
  }

  @Nonnull
  public static AccessToken of(@Nonnull Runnable r) {
    return new AccessToken() {
      @Override
      public void finish() {
        r.run();
      }
    };
  }
}
