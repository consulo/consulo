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

import com.vaadin.ui.AbstractSingleComponentContainer;
import consulo.ui.Component;
import consulo.ui.LabeledLayout;
import consulo.ui.RequiredUIAccess;
import consulo.ui.shared.Size;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 15-Jun-16
 */
public class WGwtLabeledLayoutImpl extends AbstractSingleComponentContainer implements LabeledLayout, VaadinWrapper {
  public WGwtLabeledLayoutImpl(String text) {
    getState().caption = text;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public LabeledLayout set(@Nonnull Component component) {
    setContent((com.vaadin.ui.Component)component);
    markAsDirtyRecursive();
    return this;
  }

  @javax.annotation.Nullable
  @Override
  public Component getParentComponent() {
    return (Component)getParent();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@Nonnull Size size) {

  }
}
