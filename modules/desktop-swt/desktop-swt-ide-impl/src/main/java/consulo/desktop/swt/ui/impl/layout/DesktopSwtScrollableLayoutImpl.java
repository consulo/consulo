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
import consulo.ui.layout.ScrollableLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtScrollableLayoutImpl extends DesktopSwtLayoutComponent implements ScrollableLayout {
  public DesktopSwtScrollableLayoutImpl(Component component) {
    add(component, null);
  }

  @Override
  protected Composite createSWT(Composite parent) {
    return new Composite(parent, SWT.NO_SCROLL);
  }

  @Override
  protected Layout createLayout() {
    return new FillLayout(SWT.VERTICAL | SWT.HORIZONTAL);
  }
}
