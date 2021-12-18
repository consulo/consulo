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

import consulo.desktop.swt.ui.impl.DesktopSwtTextItemPresentation;
import consulo.desktop.swt.ui.impl.SWTComponentDelegate;
import consulo.ui.Component;
import consulo.ui.Tab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 18/12/2021
 */
public class DesktopSwtTabImpl extends DesktopSwtTextItemPresentation implements Tab {
  private SWTComponentDelegate<? extends Control> myComponent;

  private TabItem myTabItem;

  @Override
  public void setCloseHandler(@Nullable BiConsumer<Tab, Component> closeHandler) {

  }

  @Override
  public void select() {

  }

  public void setComponent(Component component) {
    myComponent = (SWTComponentDelegate<? extends Control>)component;
  }

  public void initialize(TabFolder component) {
    myTabItem = new TabItem(component, SWT.NULL);

    if (myComponent != null) {
      myComponent.bind(component, null);

      myTabItem.setControl(myComponent.toSWTComponent());
    }
  }
}
