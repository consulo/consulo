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
package consulo.desktop.swt.wm.impl;

import consulo.desktop.swt.ui.impl.SWTComponentDelegate;
import consulo.desktop.swt.ui.impl.layout.DesktopSwtLayoutComponent;
import consulo.ui.Size;
import consulo.ui.ex.ToolWindowStripeButton;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;

import javax.annotation.Nullable;
import java.util.Comparator;

/**
 * @author VISTALL
 * @since 12/12/2021
 */
public class DesktopToolWindowStripeImpl extends DesktopSwtLayoutComponent {
  public enum Position {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT
  }

  private final Position myPosition;

  public DesktopToolWindowStripeImpl(Position position) {
    myPosition = position;
  }

  public void addButton(ToolWindowStripeButton button, Comparator<ToolWindowStripeButton> comparator) {
    add((SWTComponentDelegate<?>)button, null);
  }

  @Nullable
  @Override
  protected Layout createLayout() {
    switch (myPosition) {
      case LEFT:
      case RIGHT:
        RowLayout layout = new RowLayout(SWT.VERTICAL);
        layout.spacing = 0;
        layout.wrap = false;
        layout.fill = false;
        return layout;
      case TOP:
      case BOTTOM:
        RowLayout layout2 = new RowLayout(SWT.HORIZONTAL);
        layout2.center = true;
        return layout2;
    }

    throw new UnsupportedOperationException();
  }

  @Override
  protected void initialize(Composite component) {
    super.initialize(component);

    if (myPosition == Position.LEFT || myPosition == Position.RIGHT) {
      setSize(new Size(22, -1));
    }
    else if (myPosition == Position.TOP || myPosition == Position.BOTTOM) {
      setSize(new Size(-1, 22));
    }
  }
}
