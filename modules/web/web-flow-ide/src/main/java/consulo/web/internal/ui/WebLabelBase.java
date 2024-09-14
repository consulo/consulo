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
package consulo.web.internal.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.LabelOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.vaadin.VaadinLabelComponentBase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public abstract class WebLabelBase<V extends VaadinLabelComponentBase> extends VaadinComponentDelegate<V> implements Label {
  private Component myLabeledComponent;

  public WebLabelBase(LocalizeValue text, LabelOptions options) {
    setText(text);
    getVaadinComponent().setHorizontalAlignment(options.getHorizontalAlignment());
  }

  @Override
  public void setForegroundColor(@Nullable ColorValue foreground) {
    getVaadinComponent().setForegroundColor(foreground);
  }

  @Nonnull
  @Override
  public LocalizeValue getText() {
    return getVaadinComponent().getTextValue();
  }

  @RequiredUIAccess
  @Override
  public void setText(@Nonnull LocalizeValue text) {
    getVaadinComponent().setTextValue(text);
  }

  @Nullable
  @Override
  public String getTooltipText() {
    return null;
  }

  @Override
  public void setToolTipText(@Nullable String text) {
  }

  @Override
  public void setImage(@Nullable Image icon) {
    toVaadinComponent().setImage(icon);
  }

  @Override
  public Image getImage() {
    return toVaadinComponent().getImage();
  }

  @Override
  public void setTarget(@Nullable Component component) {
    myLabeledComponent = component;
  }

  @Nullable
  @Override
  public Component getTarget() {
    return myLabeledComponent;
  }
}
