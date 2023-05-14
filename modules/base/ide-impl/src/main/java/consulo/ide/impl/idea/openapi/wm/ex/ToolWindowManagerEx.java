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
package consulo.ide.impl.idea.openapi.wm.ex;

import consulo.annotation.DeprecationInfo;
import consulo.disposer.Disposable;
import consulo.project.ui.internal.ToolWindowLayout;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.ToolWindowManagerListener;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.util.lang.function.Condition;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

public abstract class ToolWindowManagerEx extends ToolWindowManager {
  @RequiredUIAccess
  public abstract void initToolWindow(@Nonnull ToolWindowFactory toolWindowFactory);

  public static ToolWindowManagerEx getInstanceEx(final Project project) {
    return (ToolWindowManagerEx)getInstance(project);
  }

  @Deprecated
  @DeprecationInfo("Use ToolWindowManagerListener#TOPIC")
  public abstract void addToolWindowManagerListener(@Nonnull ToolWindowManagerListener l);

  @Deprecated
  @DeprecationInfo("Use ToolWindowManagerListener#TOPIC")
  public abstract void addToolWindowManagerListener(@Nonnull ToolWindowManagerListener l, @Nonnull Disposable parentDisposable);

  @Deprecated
  @DeprecationInfo("Use ToolWindowManagerListener#TOPIC")
  public abstract void removeToolWindowManagerListener(@Nonnull ToolWindowManagerListener l);

  /**
   * @return <code>ID</code> of tool window that was activated last time.
   */
  @Nullable
  @RequiredUIAccess
  public String getLastActiveToolWindowId() {
    return getLastActiveToolWindowId(null);
  }

  /**
   * @return layout of tool windows.
   */
  @RequiredUIAccess
  @Nonnull
  public abstract ToolWindowLayout getLayout();

  public abstract void setLayoutToRestoreLater(ToolWindowLayout layout);

  public abstract ToolWindowLayout getLayoutToRestoreLater();

  /**
   * Copied <code>layout</code> into internal layout and rearranges tool windows.
   */
  @RequiredUIAccess
  public abstract void setLayout(@Nonnull ToolWindowLayout layout);

  public abstract void clearSideStack();

  @RequiredUIAccess
  public abstract void hideToolWindow(@Nonnull String id, boolean hideSide);

  @RequiredUIAccess
  public void hideToolWindow(final String id, final boolean hideSide, final boolean moveFocus) {
    hideToolWindow(id, hideSide);
  }

  public abstract List<String> getIdsOn(@Nonnull ToolWindowAnchor anchor);

  // TODO [VISTALL] AWT & Swing dependency
  // region AWT & Swing dependency
  /**
   * @return <code>ID</code> of tool window which was last activated among tool windows satisfying the current condition
   */
  @Nullable
  @RequiredUIAccess
  @Deprecated
  public String getLastActiveToolWindowId(@Nullable Condition<javax.swing.JComponent> condition) {
    return null;
  }
  // endregion
}
