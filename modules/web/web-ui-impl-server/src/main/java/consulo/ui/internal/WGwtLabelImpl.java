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

import com.vaadin.ui.AbstractComponent;
import consulo.ui.Label;
import consulo.ui.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.shared.ColorValue;
import consulo.ui.shared.HorizontalAlignment;
import consulo.web.gwt.shared.ui.state.LabelState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class WGwtLabelImpl extends AbstractComponent implements Label, VaadinWrapper {
  private HorizontalAlignment myHorizontalAlignment = HorizontalAlignment.LEFT;

  public WGwtLabelImpl(String text) {
    getState().caption = text;
  }

  @Nonnull
  @Override
  public String getText() {
    return getState().caption;
  }

  @Nonnull
  @RequiredUIAccess
  @Override
  public Label setText(@Nonnull String text) {
    getState().caption = text;
    markAsDirty();
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
    myHorizontalAlignment = horizontalAlignment;
    getState().myHorizontalAlignment = horizontalAlignment;
    markAsDirty();
    return this;
  }

  @Nonnull
  @Override
  public HorizontalAlignment getHorizontalAlignment() {
    return myHorizontalAlignment;
  }

  @Nonnull
  @Override
  public Label setForeground(@Nonnull Supplier<ColorValue> colorValueSupplier) {
    return this;
  }

  @Nonnull
  @Override
  public Label setImage(@Nullable Image icon) {
    return this;
  }

  @Override
  public Image getImage() {
    return null;
  }

  @Override
  protected LabelState getState() {
    return (LabelState)super.getState();
  }
}