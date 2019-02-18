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

import consulo.ui.ItemPresentation;
import consulo.ui.TextAttribute;
import consulo.ui.image.Image;
import consulo.ui.web.servlet.WebImageUrlCache;
import consulo.web.gwt.shared.ui.state.combobox.ComboBoxState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebItemPresentationImpl implements ItemPresentation {
  private ComboBoxState.Item myItem = new ComboBoxState.Item();

  @Override
  public void setIcon(@Nullable Image image) {
    myItem.myImageState = image == null ? null : WebImageUrlCache.map(image).getState();

    after();
  }

  @Override
  public void append(@Nonnull String text) {
    ComboBoxState.ItemSegment segment = new ComboBoxState.ItemSegment();
    segment.myText = text;
    myItem.myItemSegments.add(segment);

    after();
  }

  @Override
  public void append(@Nonnull String text, @Nonnull TextAttribute textAttribute) {
    ComboBoxState.ItemSegment segment = new ComboBoxState.ItemSegment();
    segment.myText = text;
    //TODO [VISTALL] style!
    myItem.myItemSegments.add(segment);

    after();
  }

  @Override
  public void clearText() {
    myItem.myItemSegments.clear();

    after();
  }

  public ComboBoxState.Item getItem() {
    return myItem;
  }

  protected void after() {
  }
}
