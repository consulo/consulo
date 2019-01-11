/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ui.web.internal;

import com.vaadin.ui.AbstractComponent;
import consulo.ui.Hyperlink;
import consulo.ui.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.web.internal.image.WGwtImageUrlCache;
import consulo.web.gwt.shared.ui.state.button.ButtonRpc;
import consulo.web.gwt.shared.ui.state.button.ButtonState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-05-11
 */
public class WGwtHyperlinkImpl extends AbstractComponent implements Hyperlink, VaadinWrapper {
  private final ButtonRpc myRpc = new ButtonRpc() {
    @Override
    public void onClick() {
      getListenerDispatcher(ClickHandler.class).onClick();
    }
  };

  private Image myImage;

  public WGwtHyperlinkImpl(String text) {
    registerRpc(myRpc);

    getState(false).caption = text;
  }

  @Override
  protected ButtonState getState() {
    return (ButtonState)super.getState();
  }

  @Override
  public void beforeClientResponse(boolean initial) {
    super.beforeClientResponse(initial);

    getState().myImageState = myImage == null ? null : WGwtImageUrlCache.map(myImage).getState();
  }

  @Nonnull
  @Override
  public String getText() {
    return getState().caption;
  }

  @RequiredUIAccess
  @Override
  public void setText(@Nonnull String text) {
    getState().caption = text;
  }

  @Override
  public void setImage(@Nullable Image icon) {
    myImage = icon;
    markAsDirty();
  }

  @Nullable
  @Override
  public Image getImage() {
    return myImage;
  }
}
