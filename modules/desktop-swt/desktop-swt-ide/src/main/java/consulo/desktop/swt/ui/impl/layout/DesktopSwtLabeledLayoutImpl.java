/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.ui.impl.layout;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LabeledLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Layout;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtLabeledLayoutImpl extends DesktopSwtLayoutComponent implements LabeledLayout {
  private final LocalizeValue myLabel;

  public DesktopSwtLabeledLayoutImpl(LocalizeValue label) {
    myLabel = label;
  }

  @Override
  protected void initialize(Composite component) {
    super.initialize(component);

    ((Group) component).setText(myLabel.get());
  }

  @Override
  protected Composite createSWT(Composite parent) {
    return new Group(parent, SWT.SHADOW_NONE);
  }

  @Override
  protected Layout createLayout() {
    return new FillLayout();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public LabeledLayout set(@Nonnull Component component) {
    add(component, null);
    return this;
  }
}
