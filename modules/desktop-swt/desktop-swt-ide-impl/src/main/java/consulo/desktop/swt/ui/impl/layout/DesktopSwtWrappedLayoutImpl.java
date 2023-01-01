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
import consulo.ui.layout.WrappedLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Layout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 11/12/2021
 */
public class DesktopSwtWrappedLayoutImpl extends DesktopSwtLayoutComponent implements WrappedLayout {
  @Nullable
  @Override
  protected Layout createLayout() {
    return new FillLayout(SWT.HORIZONTAL | SWT.VERTICAL);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public WrappedLayout set(@Nullable Component component) {
    add(component, null);
    return this;
  }
}
