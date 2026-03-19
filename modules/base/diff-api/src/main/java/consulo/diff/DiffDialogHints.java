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
package consulo.diff;

import consulo.ui.ex.awt.WindowWrapper;

import org.jspecify.annotations.Nullable;
import java.awt.*;
import java.util.function.Consumer;

public class DiffDialogHints {
 
  public static final DiffDialogHints DEFAULT = new DiffDialogHints(null);
 
  public static final DiffDialogHints FRAME = new DiffDialogHints(WindowWrapper.Mode.FRAME);
 
  public static final DiffDialogHints MODAL = new DiffDialogHints(WindowWrapper.Mode.MODAL);
 
  public static final DiffDialogHints NON_MODAL = new DiffDialogHints(WindowWrapper.Mode.NON_MODAL);

  private final WindowWrapper.@Nullable Mode myMode;
  private final @Nullable Component myParent;
  private final @Nullable Consumer<WindowWrapper> myWindowConsumer;

  public DiffDialogHints(WindowWrapper.@Nullable Mode mode) {
    this(mode, null);
  }

  public DiffDialogHints(WindowWrapper.@Nullable Mode mode, @Nullable Component parent) {
    this(mode, parent, null);
  }

  public DiffDialogHints(WindowWrapper.@Nullable Mode mode, @Nullable Component parent, @Nullable Consumer<WindowWrapper> windowConsumer) {
    myMode = mode;
    myParent = parent;
    myWindowConsumer = windowConsumer;
  }

  public WindowWrapper.@Nullable Mode getMode() {
    return myMode;
  }

  public @Nullable Component getParent() {
    return myParent;
  }

  /**
   * NB: Consumer might not be called at all (ex: for external diff/merge tools, that do not spawn WindowWrapper)
   */
  public @Nullable Consumer<WindowWrapper> getWindowConsumer() {
    return myWindowConsumer;
  }
}
