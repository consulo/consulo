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
package consulo.ui.web.internal.image;

import consulo.ui.image.Image;
import consulo.ui.web.servlet.WebImageMapper;
import consulo.web.gwt.shared.ui.state.image.FoldedImageState;
import consulo.web.gwt.shared.ui.state.image.MultiImageState;

/**
 * @author VISTALL
 * @since 2018-05-08
 */
public class WebTransparentImageImpl implements Image, WebImageWithVaadinState {
  private Image myOriginal;
  private float myAlpha;

  public WebTransparentImageImpl(Image original, float alpha) {
    myOriginal = original;
    myAlpha = alpha;
  }

  @Override
  public int getHeight() {
    return myOriginal.getHeight();
  }

  @Override
  public int getWidth() {
    return myOriginal.getWidth();
  }

  @Override
  public void toState(MultiImageState m) {
    m.myFoldedImageState = new FoldedImageState();
    m.myFoldedImageState.myChildren = new MultiImageState[]{WebImageMapper.map(myOriginal).getState()};

    m.myAlpha = myAlpha;
  }
}
