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

import consulo.ui.Hyperlink;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.event.HyperlinkListener;
import consulo.ui.image.Image;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtHyperlinkImpl extends SWTComponentDelegate<Link> implements Hyperlink {
  private String myText;

  public DesktopSwtHyperlinkImpl(String text) {
    myText = text;
  }

  @Override
  protected Link createSWT(Composite parent) {
    return new Link(parent, SWT.NONE);
  }

  @Override
  protected void initialize(Link component) {
    component.setText("<a href=\"click\">" + myText + "</a>");

    component.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        getListenerDispatcher(HyperlinkListener.class).navigate(new HyperlinkEvent(DesktopSwtHyperlinkImpl.this, ""));
      }
    });
  }

  @Nonnull
  @Override
  public String getText() {
    return myText;
  }

  @RequiredUIAccess
  @Override
  public void setText(@Nonnull String text) {
    myText = text;
  }

  @Override
  public void setImage(@Nullable Image icon) {

  }

  @Nullable
  @Override
  public Image getImage() {
    return null;
  }
}
