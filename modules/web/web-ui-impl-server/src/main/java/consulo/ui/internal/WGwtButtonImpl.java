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
package consulo.ui.internal;

import com.vaadin.ui.AbstractComponent;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.shared.Size;
import consulo.ui.image.Image;
import consulo.web.gwt.shared.ui.state.button.ButtonRpc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 13-Sep-17
 */
public class WGwtButtonImpl extends AbstractComponent implements Button, VaadinWrapper {
  private final ButtonRpc myRpc = new ButtonRpc() {
    @Override
    public void onClick() {
      getListenerDispatcher(ClickHandler.class).onClick();
    }
  };

  public WGwtButtonImpl(String text, Image image) {
    registerRpc(myRpc);
    getState().caption = text;
  }

  @NotNull
  @Override
  public String getText() {
    return getState().caption;
  }

  @RequiredUIAccess
  @Override
  public void setText(@NotNull String text) {
    getState().caption = text;
    markAsDirty();
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return (Component)getParent();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@NotNull Size size) {

  }
}
