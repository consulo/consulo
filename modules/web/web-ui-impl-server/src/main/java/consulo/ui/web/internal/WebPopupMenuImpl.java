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
package consulo.ui.web.internal;

import com.vaadin.ui.AbstractComponent;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.web.internal.base.VaadinComponent;
import consulo.ui.web.internal.base.VaadinComponentDelegate;
import consulo.ui.web.internal.contextmenu.ContextMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 18/08/2021
 */
public class WebPopupMenuImpl extends VaadinComponentDelegate<WebPopupMenuImpl.Vaadin> implements PopupMenu {

  public static class Vaadin extends VaadinComponent {
    // hack for generic, not used
  }

  private final Component myParent;

  private List<MenuItem> myItems = new ArrayList<>();

  public WebPopupMenuImpl(Component parent) {
    super(true);
    myParent = parent;
  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void show(int relativeX, int relativeY) {
    ContextMenu contextMenu = new ContextMenu((AbstractComponent)TargetVaddin.to(myParent), false);

    for (MenuItem item : myItems) {
      if (item instanceof MenuSeparator) {
        contextMenu.addSeparator();
      }
      else {
        String text = item.getText();

        // TODO [VISTALL] unsupported menu
        contextMenu.addItem(text);
      }
    }

    contextMenu.open(relativeX, relativeY);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Menu add(@Nonnull MenuItem menuItem) {
    myItems.add(menuItem);
    return this;
  }

  @Nonnull
  @Override
  public String getText() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setIcon(@Nullable Image icon) {
    throw new UnsupportedOperationException();
  }
}
