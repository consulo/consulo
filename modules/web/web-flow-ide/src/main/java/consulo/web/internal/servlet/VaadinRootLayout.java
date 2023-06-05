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
package consulo.web.internal.servlet;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLayout;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import jakarta.annotation.Nullable;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 26/05/2023
 */
public class VaadinRootLayout extends HorizontalLayout implements RouterLayout, FromVaadinComponentWrapper {
  private UIWindowOverRouterLayout myUIWindow = new UIWindowOverRouterLayout(this);

  public VaadinRootLayout() {
  }

  public void update(Component newContent) {
    removeAll();

    ((HasSize)newContent).setSizeFull();

    add(newContent);
  }

  @Override
  @RequiredUIAccess
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);

    UIServlet.RootUIInfo data = ComponentUtil.getData(UI.getCurrent(), UIServlet.RootUIInfo.class);
    if (data == null) {
      return;
    }

    Supplier<UIBuilder> builder = data.builder();

    UIBuilder uiBuilder = builder.get();

    uiBuilder.build(myUIWindow);
  }

  @Nullable
  @Override
  public consulo.ui.Component toUIComponent() {
    return myUIWindow;
  }
}
