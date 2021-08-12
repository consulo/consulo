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
package consulo.ui.web.internal.layout;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.web.internal.TargetVaddin;
import consulo.ui.web.internal.base.VaadinComponentDelegate;
import consulo.ui.web.internal.base.VaadinSingleComponentContainer;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebLabeledLayoutImpl extends VaadinComponentDelegate<WebLabeledLayoutImpl.Vaadin> implements LabeledLayout {
  public static class Vaadin extends VaadinSingleComponentContainer {
    private LocalizeValue myLabelValue = LocalizeValue.empty();

    @Override
    public void beforeClientResponse(boolean initial) {
      super.beforeClientResponse(initial);

      getState().caption = myLabelValue.getValue();
    }

    public void setLabelValue(LocalizeValue labelValue) {
      myLabelValue = labelValue;
    }
  }

  public WebLabeledLayoutImpl(LocalizeValue label) {
    getVaadinComponent().setLabelValue(label);
  }

  @Override
  public void remove(@Nonnull Component component) {
    getVaadinComponent().removeIfContent(TargetVaddin.to(component));
  }

  @RequiredUIAccess
  @Override
  public void removeAll() {
    getVaadinComponent().setComponent(null);
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
