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
import consulo.ui.web.servlet.WebImageMapper;
import consulo.web.gwt.shared.ui.state.image.FoldedImageState;
import consulo.web.gwt.shared.ui.state.image.MultiImageState;

/**
 * @author VISTALL
 * @since 2019-02-09
 */
public class WebResizeImageImpl implements Image, WebImageWithVaadinState {
  private final Image myOriginal;
  private final int myHeight;
  private final int myWidth;

  public WebResizeImageImpl(Image original, int width, int height) {
    myOriginal = original;
    myHeight = height;
    myWidth = width;
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
  public void toState(MultiImageState m) {
    m.myFoldedImageState = new FoldedImageState();
    m.myFoldedImageState.myChildren = new MultiImageState[]{WebImageMapper.map(myOriginal).getState()};

    m.myHeight = myHeight;
    m.myWidth = myWidth;
  }
}