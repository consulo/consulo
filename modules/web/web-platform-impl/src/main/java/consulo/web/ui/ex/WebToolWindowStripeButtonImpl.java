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
package consulo.web.ui.ex;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.WindowInfo;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ToolWindowStripeButton;
import consulo.ui.image.Image;
import consulo.ui.web.internal.base.VaadinComponentDelegate;
import consulo.ui.web.internal.base.VaadinComponent;
import consulo.ui.web.servlet.WebImageMapper;
import consulo.web.gwt.shared.ui.ex.state.toolWindow.ToolWindowStripeButtonRpc;
import consulo.web.gwt.shared.ui.ex.state.toolWindow.ToolWindowStripeButtonState;
import consulo.web.wm.impl.WebToolWindowInternalDecorator;
import consulo.wm.impl.ToolWindowBase;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public class WebToolWindowStripeButtonImpl extends VaadinComponentDelegate<WebToolWindowStripeButtonImpl.Vaadin> implements ToolWindowStripeButton {
  public static class Vaadin extends VaadinComponent {
    private ToolWindowStripeButtonRpc myRpc = () -> {
      WebToolWindowStripeButtonImpl component = toUIComponent();

      if (isSelected()) {
        component.myDecorator.fireHidden();
      }
      else {
        component.myDecorator.fireActivated();
      }
    };

    public Vaadin() {
      registerRpc(myRpc);
    }

    @Override
    public void beforeClientResponse(boolean initial) {
      super.beforeClientResponse(initial);

      WebToolWindowStripeButtonImpl component = toUIComponent();

      WindowInfo windowInfo = component.getWindowInfo();
      ToolWindowStripeButtonState state = getState();
      ToolWindow toolWindow = component.myDecorator.getToolWindow();

      state.mySecondary = windowInfo.isSplit();
      state.caption = windowInfo.getId();

      Image icon = toolWindow.getIcon();

      try {
        state.myImageState = icon == null ? null : WebImageMapper.map(icon).getState();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }

    public boolean isSelected() {
      return getState().mySelected;
    }

    public void setSelected(boolean selected) {
      getState().mySelected = selected;
    }

    @Nonnull
    @Override
    public WebToolWindowStripeButtonImpl toUIComponent() {
      return (WebToolWindowStripeButtonImpl)super.toUIComponent();
    }

    @Override
    public ToolWindowStripeButtonState getState() {
      return (ToolWindowStripeButtonState)super.getState();
    }
  }

  private WebToolWindowInternalDecorator myDecorator;

  public WebToolWindowStripeButtonImpl(WebToolWindowInternalDecorator decorator, WebToolWindowPanelImpl toolWindowPanel) {
    myDecorator = decorator;
  }

  @Nonnull
  @Override
  public WindowInfo getWindowInfo() {
    return myDecorator.getWindowInfo();
  }

  @Override
  public void apply(@Nonnull WindowInfo info) {
    setSelected(info.isVisible() || info.isActive());
    updateState();
    getVaadinComponent().markAsDirty();
  }

  @RequiredUIAccess
  private void updateState() {
    ToolWindowBase window = (ToolWindowBase)myDecorator.getToolWindow();
    boolean toShow = window.isAvailable() || window.isPlaceholderMode();
    if (UISettings.getInstance().ALWAYS_SHOW_WINDOW_BUTTONS) {
      setVisible(window.isShowStripeButton() || isSelected());
    }
    else {
      setVisible(toShow && (window.isShowStripeButton() || isSelected()));
    }
    setEnabled(toShow && !window.isPlaceholderMode());
  }

  public boolean isSelected() {
    return getVaadinComponent().isSelected();
  }

  public void setSelected(boolean selected) {
    getVaadinComponent().setSelected(selected);
  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @Nonnull
  @Override
  public Component getComponent() {
    return this;
  }

  @RequiredUIAccess
  @Override
  public void updatePresentation() {

  }
}
