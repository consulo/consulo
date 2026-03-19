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

import consulo.ui.ex.awt.ValidationInfo;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;

import org.jspecify.annotations.Nullable;
import javax.swing.*;

public abstract class PushTargetPanel<T extends PushTarget> extends JPanel {

  /**
   * @param isActive true if appropriate repository changes will be pushed, a.e. if repository checked
   */
  abstract public void render(ColoredTreeCellRenderer renderer,
                              boolean isSelected,
                              boolean isActive,
                              @Nullable String forceRenderedText);

  abstract public @Nullable T getValue();

  public abstract void fireOnCancel();

  public abstract void fireOnChange();

  public abstract @Nullable ValidationInfo verify();

  public abstract void setFireOnChangeAction(Runnable action);

  /**
   * Add an ability to track edit field process
   */
  public abstract void addTargetEditorListener(PushTargetEditorListener listener);

  public void forceUpdateEditableUiModel(String forcedText) {
  }
}
