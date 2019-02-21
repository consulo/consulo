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
package consulo.ui.web.internal.image;

import consulo.ui.image.Image;
import consulo.web.gwt.shared.ui.state.image.ImageState;
import consulo.web.gwt.shared.ui.state.image.MultiImageState;

/**
 * @author VISTALL
 * @since 2019-02-21
 */
public class WebEmptyImageImpl implements Image, WebImageWithVaadinState {
  private final int myWidth;
  private final int myHeight;

  public WebEmptyImageImpl(int width, int height) {
    myWidth = width;
    myHeight = height;
  }

  @Override
  public int getHeight() {
    return myHeight;
  }

  @Override
  public int getWidth() {
    return myWidth;
  }

  @Override
  public void toState(MultiImageState state) {
    state.myHeight = myHeight;
    state.myWidth = myWidth;
    state.myImageState = new ImageState();
    state.myImageState.myEmpty = true;
  }
}
