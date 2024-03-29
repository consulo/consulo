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
package consulo.builtinWebServer.custom;

import jakarta.annotation.Nullable;

public abstract class CustomPortServerManagerBase extends CustomPortServerManager {
  @Nullable
  protected CustomPortService manager;

  @Override
  public void setManager(@Nullable CustomPortService manager) {
    this.manager = manager;
  }

  public boolean isBound() {
    return manager != null && manager.isBound();
  }

  public void portChanged() {
    if (manager != null) {
      manager.rebind();
    }
  }
}