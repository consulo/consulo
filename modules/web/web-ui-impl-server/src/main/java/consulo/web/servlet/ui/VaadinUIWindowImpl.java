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
package consulo.web.servlet.ui;

import com.vaadin.ui.UI;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Size;
import consulo.ui.Window;
import consulo.ui.internal.WGwtRootPanelImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EventListener;

/**
 * @author VISTALL
 * @since 11-Sep-17
 * <p>
 * TODO [VISTALL] support menu bar
 */
class VaadinUIWindowImpl implements Window {
  private final UI myUI;
  private WGwtRootPanelImpl myRootPanel = new WGwtRootPanelImpl();

  public VaadinUIWindowImpl(UI ui) {
    myUI = ui;
    myUI.setSizeFull();
    myUI.setContent(myRootPanel);
  }

  @Override
  public boolean isVisible() {
    return myUI.isVisible();
  }

  @RequiredUIAccess
  @Override
  public void setVisible(boolean value) {
    myUI.setVisible(value);
  }

  @Override
  public boolean isEnabled() {
    return myUI.isEnabled();
  }

  @RequiredUIAccess
  @Override
  public void setEnabled(boolean value) {
    myUI.setEnabled(value);
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return (Component)myUI.getParent();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@NotNull Size size) {

  }

  @NotNull
  @Override
  public <T extends EventListener> T getListenerDispatcher(@NotNull Class<T> eventClass) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public <T extends EventListener> Runnable addListener(@NotNull Class<T> eventClass, @NotNull T listener) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void setContent(@NotNull Component content) {
    myRootPanel.setCenterComponent((com.vaadin.ui.Component)content);
  }

  @RequiredUIAccess
  @Override
  public void setMenuBar(@Nullable MenuBar menuBar) {
    myRootPanel.setMenuBar(menuBar);
  }
}
