/*
 * Copyright 2013-2016 consulo.io
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

import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.TextItemPresentation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
class WebTabImpl implements Tab {
  private BiConsumer<Tab, TextItemPresentation> myRender = (tab, presentation) -> presentation.append(toString());
  private BiConsumer<Tab, Component> myCloseHandler;
  private WebTabbedLayoutImpl myTabbedLayout;

  private com.vaadin.flow.component.tabs.Tab myVaadinTab;

  public WebTabImpl(WebTabbedLayoutImpl tabbedLayout) {
    myTabbedLayout = tabbedLayout;
  }

  @Override
  public void setRender(@Nonnull BiConsumer<Tab, TextItemPresentation> render) {
    myRender = render;
  }

  @Override
  public void setCloseHandler(@Nullable BiConsumer<Tab, Component> closeHandler) {
    myCloseHandler = closeHandler;
  }

  @Override
  public void update() {
    WebItemPresentationImpl presentation = new WebItemPresentationImpl();
    myRender.accept(this, presentation);

    myVaadinTab.removeAll();
    myVaadinTab.add(presentation.toComponent());
  }

  public BiConsumer<Tab, TextItemPresentation> getRender() {
    return myRender;
  }

  @Override
  public void select() {
    // TODO
  }

  public BiConsumer<Tab, Component> getCloseHandler() {
    return myCloseHandler;
  }

  public void setVaadinTab(com.vaadin.flow.component.tabs.Tab vaadinTab) {
    myVaadinTab = vaadinTab;
  }
}
