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
package consulo.desktop.swt.ui.impl;

import consulo.ui.Component;
import consulo.ui.layout.ScrollableLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtScrollableLayoutImpl extends DesktopSwtLayoutComponent implements ScrollableLayout {
  private final DesktopSwtComponent<?> myComponent;

  public DesktopSwtScrollableLayoutImpl(Component component) {
    myComponent = (DesktopSwtComponent<?>)component;
  }

  @Override
  protected void initialize(Composite component) {
    ((DesktopSwtComponent)myComponent).bind(getComposite(), null);

    ((ScrolledComposite)component).setContent(((DesktopSwtComponent)myComponent).myComponent);
    ((ScrolledComposite)component).setExpandHorizontal(true);
    ((ScrolledComposite)component).setExpandVertical(true);
  }

  @Override
  protected void disposeSWT() {
    super.disposeSWT();

    myComponent.disposeSWT();
  }

  @Override
  protected Composite createSWT(Composite parent) {
    return new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
  }

  @Override
  protected Layout createLayout() {
    return new FillLayout();
  }
}
