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

import com.vaadin.flow.component.orderedlayout.FlexLayout;
import consulo.ui.Component;
import consulo.ui.ex.toolWindow.ToolWindowStripeButton;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaddin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public class WebToolWindowStripeImpl extends VaadinComponentDelegate<WebToolWindowStripeImpl.Vaadin> {
  public class Vaadin extends FlexLayout implements FromVaadinComponentWrapper {
    private List<ToolWindowStripeButton> myButtons = new ArrayList<>();

    @Nullable
    @Override
    public Component toUIComponent() {
      return WebToolWindowStripeImpl.this;
    }
  }

  public WebToolWindowStripeImpl(WebToolWindowStripePosition constraint) {
    Vaadin vaadin = toVaadinComponent();
    vaadin.addClassName(getClassNamePrefix() + "-" + constraint.name().toLowerCase(Locale.ROOT));
  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  public void markAsDirtyRecursive() {
    ///getVaadinComponent().markAsDirtyRecursive();
  }

  public void addButton(ToolWindowStripeButton button, Comparator<ToolWindowStripeButton> comparator) {
    Vaadin vaadinComponent = getVaadinComponent();

    vaadinComponent.removeAll();

    vaadinComponent.myButtons.add(button);

    vaadinComponent.myButtons.sort(comparator::compare);

    for (ToolWindowStripeButton stripeButton : vaadinComponent.myButtons) {
      vaadinComponent.add(TargetVaddin.to(stripeButton.getComponent()));
    }
  }
}
