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
package consulo.project.ui.internal;

import consulo.util.concurrent.ActionCallback;
import consulo.util.dataholder.Key;
import consulo.project.ui.wm.IdeFrame;

import jakarta.annotation.Nonnull;

public interface IdeFrameEx extends IdeFrame {
  public static final Key<Boolean> SHOULD_OPEN_IN_FULL_SCREEN = Key.create("should.open.in.full.screen");

  default boolean isInFullScreen() {
    return false;
  }

  default void storeFullScreenStateIfNeeded() {
    storeFullScreenStateIfNeeded(isInFullScreen());
  }

  default void storeFullScreenStateIfNeeded(boolean value) {
  }

  @Nonnull
  default ActionCallback toggleFullScreen(boolean state) {
    return ActionCallback.REJECTED;
  }

  default void updateView() {
  }
}
