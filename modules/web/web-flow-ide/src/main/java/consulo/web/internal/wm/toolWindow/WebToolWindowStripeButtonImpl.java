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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import consulo.application.ui.UISettings;
import consulo.ide.impl.wm.impl.ToolWindowBase;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindowStripeButton;
import consulo.ui.ex.toolWindow.WindowInfo;
import consulo.ui.image.Image;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public class WebToolWindowStripeButtonImpl extends VaadinComponentDelegate<WebToolWindowStripeButtonImpl.Vaadin> implements ToolWindowStripeButton {
  public class Vaadin extends Div implements FromVaadinComponentWrapper {
    private boolean mySelected;

    public Vaadin() {
    }

    public void update(LocalizeValue displayName, boolean isSecondary, Image icon) {
      removeAll();

      add(new Span(displayName.get()));

      String selectedPrefix = getClassNamePrefix() + "-selected";
      removeClassName(selectedPrefix);
      if (isSelected()) {
        addClassName(selectedPrefix);
      }

      String secondaryPrefix = getClassNamePrefix() + "-secondary";
      removeClassName(secondaryPrefix);
      if (isSecondary) {
        addClassName(secondaryPrefix);
      }
    }

    public boolean isSelected() {
      return mySelected;
    }

    public void setSelected(boolean selected) {
      mySelected = selected;
    }

    @Nonnull
    @Override
    public WebToolWindowStripeButtonImpl toUIComponent() {
      return WebToolWindowStripeButtonImpl.this;
    }
  }

  private WebToolWindowInternalDecorator myDecorator;

  public WebToolWindowStripeButtonImpl(WebToolWindowInternalDecorator decorator, WebToolWindowPanelImpl toolWindowPanel) {
    myDecorator = decorator;

    toVaadinComponent().addClickListener(event -> {
      if (isSelected()) {
        myDecorator.fireHidden();
      }
      else {
        myDecorator.fireActivated();
      }
    });
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

    toVaadinComponent().update(window.getDisplayName(), window.isSplitMode(), window.getIcon());
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
    updateState();
  }

  @Override
  public void dispose() {

  }
}
