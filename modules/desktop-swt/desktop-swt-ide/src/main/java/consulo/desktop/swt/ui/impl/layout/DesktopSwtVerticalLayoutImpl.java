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
import consulo.ui.layout.VerticalLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Layout;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtVerticalLayoutImpl extends DesktopSwtLayoutComponent implements VerticalLayout {
  private final int myVGap;

  public DesktopSwtVerticalLayoutImpl(int vGap) {
    myVGap = vGap;
  }

  @Override
  protected Layout createLayout() {
    RowLayout layout = new RowLayout(SWT.VERTICAL);
    layout.spacing = myVGap;
    layout.wrap = false;
    layout.fill = true;
    return layout;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public VerticalLayout add(@Nonnull Component component) {
    add(component, null);
    return this;
  }
}
