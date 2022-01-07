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
package consulo.ui.app;

import com.intellij.openapi.util.Couple;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-06-29
 */
public abstract class WholeLeftWindowWrapper extends WindowWrapper {
  public WholeLeftWindowWrapper(@Nonnull String title) {
    super(title);
  }

  @Override
  @Nonnull
  @RequiredUIAccess
  protected Layout buildRootLayout() {
    TwoComponentSplitLayout layout = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
    layout.setProportion(30);

    Couple<Component> compoents = createComponents();

    layout.setFirstComponent(compoents.getFirst());

    DockLayout baseRoot = DockLayout.create();
    baseRoot.center(compoents.getSecond());
    baseRoot.bottom(buildButtonsLayout());

    layout.setSecondComponent(baseRoot);
    return layout;
  }

  @Nonnull
  @RequiredUIAccess
  protected abstract Couple<Component> createComponents();

  @RequiredUIAccess
  @Nonnull
  @Override
  protected final Component createCenterComponent() {
    throw new UnsupportedOperationException();
  }
}
