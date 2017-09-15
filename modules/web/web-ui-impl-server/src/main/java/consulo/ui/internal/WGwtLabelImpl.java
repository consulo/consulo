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
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Size;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class WGwtLabelImpl extends AbstractComponent implements Label, VaadinWrapper {
  public WGwtLabelImpl(String text) {
    getState().caption = text;
  }

  @NotNull
  @Override
  public String getText() {
    return getState().caption;
  }

  @RequiredUIAccess
  @Override
  public void setText(@NotNull String text) {
    getState().caption = text;
    markAsDirty();
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return null;
  }

  @RequiredUIAccess
  @Override
  public void setSize(@NotNull Size size) {

  }
}