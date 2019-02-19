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
package consulo.ui.web.internal.layout;

import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.web.internal.WebItemPresentationImpl;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
class WebTabImpl extends WebItemPresentationImpl implements Tab {
  private BiConsumer<Tab, Component> myCloseHandler;
  private WebTabbedLayoutImpl myTabbedLayout;
  private int myIndex;

  public WebTabImpl(int index, WebTabbedLayoutImpl tabbedLayout) {
    myIndex = index;
    myTabbedLayout = tabbedLayout;
  }

  public void setIndex(int index) {
    myIndex = index;
  }

  public int getIndex() {
    return myIndex;
  }

  @Override
  public void setCloseHandler(@Nullable BiConsumer<Tab, Component> closeHandler) {
    myCloseHandler = closeHandler;
  }

  @Override
  public void select() {
    if (myIndex == -1) {
      throw new UnsupportedOperationException("Tab is not added to layout");
    }
    myTabbedLayout.selectTab(myIndex);
  }

  public BiConsumer<Tab, Component> getCloseHandler() {
    return myCloseHandler;
  }

  @Override
  protected void after() {
    myTabbedLayout.markAsDirty();
  }
}
