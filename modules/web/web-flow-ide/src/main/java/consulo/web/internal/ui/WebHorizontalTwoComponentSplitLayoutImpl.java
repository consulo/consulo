/*
 * Copyright 2013-2023 consulo.io
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

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaddin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 28/05/2023
 */
public class WebHorizontalTwoComponentSplitLayoutImpl extends VaadinComponentDelegate<WebHorizontalTwoComponentSplitLayoutImpl.Vaadin> implements TwoComponentSplitLayout {
  public class Vaadin extends HorizontalLayout implements FromVaadinComponentWrapper {
    public Vaadin() {
      setHeightFull();
    }

    @Nullable
    @Override
    public Component toUIComponent() {
      return WebHorizontalTwoComponentSplitLayoutImpl.this;
    }
  }

  @Override
  public void setProportion(int percent) {

  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public TwoComponentSplitLayout setFirstComponent(@Nonnull Component component) {
    toVaadinComponent().add(TargetVaddin.to(component));
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public TwoComponentSplitLayout setSecondComponent(@Nonnull Component component) {
    toVaadinComponent().add(TargetVaddin.to(component));
    return this;
  }

  @Nonnull
  @Override
  public WebHorizontalTwoComponentSplitLayoutImpl.Vaadin createVaadinComponent() {
    return new Vaadin();
  }
}
