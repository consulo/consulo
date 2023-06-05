/*
 * Copyright 2013-2019 consulo.io
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
package consulo.web.internal.ui;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.ThemableLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LabeledLayout;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaddin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebLabeledLayoutImpl extends VaadinComponentDelegate<WebLabeledLayoutImpl.Vaadin> implements LabeledLayout {
  public class Vaadin extends VerticalLayout implements FromVaadinComponentWrapper {
    private LocalizeValue myLabelValue = LocalizeValue.empty();

    private final Span myCaption;

    public Vaadin() {
      setMargin(false);
      myCaption = new Span("");
      myCaption.addClassName(LumoUtility.FontWeight.BOLD);
      myCaption.setWidthFull();
      add(myCaption);

      addClassName(LumoUtility.Border.ALL);
      addClassName(LumoUtility.BorderRadius.SMALL);
      addClassName(LumoUtility.BorderColor.CONTRAST_10);
    }

    public void setLabelValue(LocalizeValue labelValue) {
      myLabelValue = labelValue;

      myCaption.setText(myLabelValue.getValue());
    }

    public void setContent(com.vaadin.flow.component.Component content) {
      removeAll();

      add(myCaption);
      if (content != null) {
        if (content instanceof ThemableLayout themableLayout) {
          themableLayout.setMargin(false);
          themableLayout.setPadding(false);
        }

        add(content);
      }
    }

    @Nullable
    @Override
    public Component toUIComponent() {
      return WebLabeledLayoutImpl.this;
    }
  }

  public WebLabeledLayoutImpl(LocalizeValue label) {
    getVaadinComponent().setLabelValue(label);
  }

  @Override
  public void remove(@Nonnull Component component) {
    if (component.getParent() == this) {
      toVaadinComponent().setContent(null);
    }
  }

  @RequiredUIAccess
  @Override
  public void removeAll() {
    getVaadinComponent().setContent(null);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public LabeledLayout set(@Nonnull Component component) {
    getVaadinComponent().setContent(TargetVaddin.to(component));
    return this;
  }

  @Nonnull
  @Override
  public WebLabeledLayoutImpl.Vaadin createVaadinComponent() {
    return new Vaadin();
  }
}
