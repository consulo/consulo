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
package consulo.ui.web.internal;

import com.vaadin.ui.AbstractComponent;
import consulo.ui.Component;
import consulo.ui.MenuSeparator;
import consulo.ui.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.shared.Size;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class WGwtMenuSeparatorImpl extends AbstractComponent implements MenuSeparator, VaadinWrapper {
  public WGwtMenuSeparatorImpl() {
  }

  @Nonnull
  @Override
  public String getText() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setIcon(@Nullable Image icon) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return (Component)getParent();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@Nonnull Size size) {

  }
}
