/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.internal.wm.toolWindow;

import consulo.ide.impl.idea.util.EventDispatcher;
import consulo.project.ui.impl.internal.wm.UnifiedToolWindowImpl;
import consulo.project.Project;
import consulo.project.ui.internal.WindowInfoImpl;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.toolWindow.InternalDecoratorListener;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowInternalDecorator;
import consulo.ui.ex.toolWindow.WindowInfo;
import consulo.ui.layout.DockLayout;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 12-Oct-17
 */
public class WebToolWindowInternalDecorator implements ToolWindowInternalDecorator {
  private final WindowInfoImpl myWindowInfo;
  private final UnifiedToolWindowImpl myToolWindow;
  private final EventDispatcher<InternalDecoratorListener> myDispatcher = EventDispatcher.create(InternalDecoratorListener.class);

  private DockLayout myLayout;

  private WebToolWindowHeader myHeader;

  @RequiredUIAccess
  public WebToolWindowInternalDecorator(Project project, WindowInfoImpl windowInfo, UnifiedToolWindowImpl toolWindow, boolean canWorkInDumbMode) {
    myWindowInfo = windowInfo;
    myToolWindow = toolWindow;
    myToolWindow.setDecorator(this);

    myHeader = new WebToolWindowHeader(toolWindow);

    myLayout = DockLayout.create();
    myLayout.top(myHeader.getComponent());
    myLayout.center(toolWindow.getUIComponent());
  }

  @Nonnull
  public Component getComponent() {
    return myLayout;
  }

  @Nonnull
  @Override
  public WindowInfo getWindowInfo() {
    return myWindowInfo;
  }

  @Override
  public void apply(@Nonnull WindowInfo windowInfo) {

  }

  @Nonnull
  @Override
  public ToolWindow getToolWindow() {
    return myToolWindow;
  }

  @Override
  public void addInternalDecoratorListener(InternalDecoratorListener l) {
    myDispatcher.addListener(l);
  }

  @Override
  public void removeInternalDecoratorListener(InternalDecoratorListener l) {
    myDispatcher.removeListener(l);
  }

  @Override
  public void fireActivated() {
    myDispatcher.getMulticaster().activated(this);
  }

  @Override
  public void fireHidden() {
    myDispatcher.getMulticaster().hidden(this);
  }

  @Override
  public void fireHiddenSide() {
    myDispatcher.getMulticaster().hiddenSide(this);
  }

  @Nonnull
  @Override
  public ActionGroup createPopupGroup() {
    return new DefaultActionGroup();
  }

  @Override
  public boolean isFocused() {
    return false;
  }

  @Override
  public boolean hasFocus() {
    return false;
  }

  @Override
  public void dispose() {

  }
}
