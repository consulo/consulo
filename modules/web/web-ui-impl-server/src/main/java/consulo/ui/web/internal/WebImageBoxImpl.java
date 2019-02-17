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
package consulo.ui.web.internal;

import consulo.ui.ImageBox;
import consulo.ui.image.Image;
import consulo.ui.web.internal.base.UIComponentWithVaadinComponent;
import consulo.ui.web.internal.base.VaadinComponent;
import consulo.ui.web.servlet.WebImageUrlCache;
import consulo.web.gwt.shared.ui.state.ImageBoxState;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-02-17
 */
public class WebImageBoxImpl extends UIComponentWithVaadinComponent<WebImageBoxImpl.Vaadin> implements ImageBox {
  protected static class Vaadin extends VaadinComponent<WebImageBoxImpl> {
    public Vaadin(WebImageBoxImpl component, Image image) {
      super(component);

      getState().myImageState = WebImageUrlCache.map(image).getState();
    }

    @Override
    protected ImageBoxState getState() {
      return (ImageBoxState)super.getState();
    }
  }

  private final Image myImage;

  public WebImageBoxImpl(Image image) {
    myImage = image;
    myVaadinComponent = new Vaadin(this, image);
  }

  @Nonnull
  @Override
  public Image getImage() {
    return myImage;
  }
}
