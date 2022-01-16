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

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtTwoComponentSplitLayoutImpl extends DesktopSwtLayoutComponent implements TwoComponentSplitLayout {
  private final SplitLayoutPosition myPosition;

  public DesktopSwtTwoComponentSplitLayoutImpl(SplitLayoutPosition position) {
    myPosition = position;
  }

  @Override
  protected Composite createSWT(Composite parent) {
    return new SashForm(parent, (myPosition == SplitLayoutPosition.VERTICAL ? SWT.VERTICAL : SWT.HORIZONTAL) | SWT.SMOOTH);
  }

  @Override
  protected Layout createLayout() {
    return new FillLayout(SWT.HORIZONTAL | SWT.VERTICAL);
  }

  @Override
  protected void initialize(Composite component) {
    super.initialize(component);

    ((SashForm) component).setSashWidth(1);
  }

  @Override
  public void setProportion(int percent) {

  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public TwoComponentSplitLayout setFirstComponent(@Nonnull Component component) {
    add(component, "first");
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public TwoComponentSplitLayout setSecondComponent(@Nonnull Component component) {
    add(component, "second");
    return this;
  }

  @Override
  protected Object convertLayoutData(Object layoutData) {
    return null;
  }
}
