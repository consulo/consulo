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

import consulo.localize.LocalizeValue;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.web.internal.base.UIComponentWithVaadinComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public abstract class WebLabelBase<V extends VaadinLabelComponentBase> extends UIComponentWithVaadinComponent<V> implements Label {
  public WebLabelBase(LocalizeValue text) {
    setText(text);
  }

  @Nonnull
  @Override
  public LocalizeValue getText() {
    return getVaadinComponent().getTextValue();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Label setText(@Nonnull LocalizeValue text) {
    getVaadinComponent().setTextValue(text);
    return this;
  }

  @Nullable
  @Override
  public String getTooltipText() {
    return null;
  }

  @Nonnull
  @Override
  public Label setToolTipText(@Nullable String text) {
    return this;
  }

  @Nonnull
  @Override
  public Label setHorizontalAlignment(@Nonnull HorizontalAlignment horizontalAlignment) {
    getVaadinComponent().setHorizontalAlignment(horizontalAlignment);
    return this;
  }

  @Nonnull
  @Override
  public HorizontalAlignment getHorizontalAlignment() {
    return getVaadinComponent().getHorizontalAlignment();
  }

  @Nonnull
  @Override
  public Label setImage(@Nullable Image icon) {
    toVaadinComponent().setImage(icon);
    return this;
  }

  @Override
  public Image getImage() {
    return toVaadinComponent().getImage();
  }
}
