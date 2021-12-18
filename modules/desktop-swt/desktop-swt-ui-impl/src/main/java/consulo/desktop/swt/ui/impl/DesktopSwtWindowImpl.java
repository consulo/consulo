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

import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.BitUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtWindowImpl extends SWTComponentDelegate<Shell> implements Window {
  private SWTComponentDelegate myContent;

  public DesktopSwtWindowImpl(String title, WindowOptions options) {
    int flags = SWT.SHELL_TRIM;
    flags = BitUtil.set(flags, SWT.CLOSE, options.isClosable());
    flags = BitUtil.set(flags, SWT.RESIZE, options.isResizable());
    flags = BitUtil.set(flags, SWT.MAX, options.isResizable());

    Shell parent = null;
    Window owner = options.getOwner();
    if(owner instanceof DesktopSwtWindowImpl swtWindow) {
      parent = swtWindow.toSWTComponent();
      flags = BitUtil.set(flags, SWT.APPLICATION_MODAL, true);
    }

    myComponent = new Shell(parent, flags);
    myComponent.setData(UI_COMPONENT_KEY, this);

    myComponent.setText(title);
    FillLayout layout = new FillLayout();
    layout.type = SWT.VERTICAL;
    myComponent.setLayout(layout);

    Monitor primaryMonitor = Display.getCurrent().getPrimaryMonitor();
    Rectangle screenSize = primaryMonitor.getBounds();

    myComponent.setLocation((screenSize.width - myComponent.getBounds().width) / 2, (screenSize.height - myComponent.getBounds().height) / 2);
  }

  @Override
  protected Shell createSWT(Composite parent) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void initialize(Shell component) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public Window getParent() {
    return (Window)super.getParent();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@Nonnull Size size) {
    myComponent.setSize(size.getWidth(), size.getHeight());
  }

  @RequiredUIAccess
  @Override
  public void setTitle(@Nonnull String title) {
    myComponent.setText(title);
  }

  @RequiredUIAccess
  @Override
  public void setContent(@Nonnull Component content) {
    myContent = (SWTComponentDelegate)content;
  }

  @RequiredUIAccess
  @Override
  public void setMenuBar(@Nullable MenuBar menuBar) {

  }

  @RequiredUIAccess
  @Override
  public void show() {
    if (myContent != null) {
      myContent.bind(getComposite(), null);
    }
    myComponent.open();
  }

  @RequiredUIAccess
  @Override
  public void close() {
    myComponent.close();
  }
}
