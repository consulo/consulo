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


import org.jspecify.annotations.Nullable;
import consulo.ui.Component;

public class DiffDialogHints {
  public static final DiffDialogHints DEFAULT = new DiffDialogHints(null);
  public static final DiffDialogHints FRAME = new DiffDialogHints(DiffWindowMode.FRAME);
  public static final DiffDialogHints MODAL = new DiffDialogHints(DiffWindowMode.MODAL);
  public static final DiffDialogHints NON_MODAL = new DiffDialogHints(DiffWindowMode.NON_MODAL);

  private final @Nullable DiffWindowMode myMode;
  private final @Nullable Component myParent;

  public DiffDialogHints(@Nullable DiffWindowMode mode) {
    this(mode, null);
  }

  public DiffDialogHints(@Nullable DiffWindowMode mode, @Nullable Component parent) {
    myMode = mode;
    myParent = parent;
  }

  public @Nullable DiffWindowMode getMode() {
    return myMode;
  }

  public @Nullable Component getParent() {
    return myParent;
  }
}
