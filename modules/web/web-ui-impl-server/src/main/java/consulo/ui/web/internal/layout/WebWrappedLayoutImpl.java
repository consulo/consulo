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

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.WrappedLayout;
import consulo.ui.web.internal.TargetVaddin;
import consulo.ui.web.internal.base.VaadinComponentDelegate;
import consulo.ui.web.internal.base.VaadinSingleComponentContainer;
import consulo.ui.web.internal.border.WebBorderBuilder;
import consulo.web.gwt.shared.ui.state.layout.BaseSingleLayoutState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-17
 */
public class WebWrappedLayoutImpl extends VaadinComponentDelegate<WebWrappedLayoutImpl.Vaadin> implements WrappedLayout {
  public static class Vaadin extends VaadinSingleComponentContainer {
    @Override
    public void beforeClientResponse(boolean initial) {
      super.beforeClientResponse(initial);
      WebBorderBuilder.fill(toUIComponent(), getState().myBorderListState);
    }

    @Override
    public BaseSingleLayoutState getState() {
      return (BaseSingleLayoutState)super.getState();
    }
  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @RequiredUIAccess
  @Override
  public void removeAll() {
    getVaadinComponent().setContent(null);
  }

  @Override
  public void remove(@Nonnull Component component) {
    getVaadinComponent().removeIfContent(TargetVaddin.to(component));
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public WrappedLayout set(@Nullable Component component) {
    getVaadinComponent().setContent(TargetVaddin.to(component));
    return this;
  }
}
