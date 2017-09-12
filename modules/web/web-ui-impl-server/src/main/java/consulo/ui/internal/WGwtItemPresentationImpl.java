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
package consulo.ui.internal;

import consulo.ui.ItemPresentation;
import consulo.ui.TextStyle;
import consulo.ui.image.Image;
import consulo.ui.internal.image.WGwtImageUrlCache;
import consulo.web.gwt.shared.ui.state.combobox.ComboBoxState;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
class WGwtItemPresentationImpl implements ItemPresentation {
  private ComboBoxState.Item myItem = new ComboBoxState.Item();

  @Override
  public void setIcon(@NotNull Image image) {
    myItem.myImageState = WGwtImageUrlCache.fixSwingImageRef(image).getState();
  }

  @Override
  public void append(@NotNull String text) {
    ComboBoxState.ItemSegment segment = new ComboBoxState.ItemSegment();
    segment.myText = text;
    myItem.myItemSegments.add(segment);
  }

  @Override
  public void append(@NotNull String text, @NotNull TextStyle... styles) {
    ComboBoxState.ItemSegment segment = new ComboBoxState.ItemSegment();
    segment.myText = text;
    //TODO [VISTALL] style!
    myItem.myItemSegments.add(segment);
  }

  public ComboBoxState.Item getItem() {
    return myItem;
  }
}
