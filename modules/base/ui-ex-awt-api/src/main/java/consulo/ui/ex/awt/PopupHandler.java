/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.application.ApplicationManager;
import consulo.ui.ex.action.*;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;

/**
 * @author Eugene Belyaev
 */
public abstract class PopupHandler extends MouseAdapter {
  public abstract void invokePopup(Component comp, int x, int y);

  @Override
  public void mouseClicked(MouseEvent e) {
    if (e.isPopupTrigger()) {
      invokePopup(e.getComponent(), e.getX(), e.getY());
      e.consume();
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {
    if (e.isPopupTrigger()) {
      invokePopup(e.getComponent(), e.getX(), e.getY());
      e.consume();
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (e.isPopupTrigger()) {
      invokePopup(e.getComponent(), e.getX(), e.getY());
      e.consume();
    }
  }

  public static void installPopupHandler(JComponent component, @NonNls String groupId, @NonNls String place) {
    ActionManager actionManager = ActionManager.getInstance();
    ActionGroup group = (ActionGroup)actionManager.getAction(groupId);
    installPopupHandler(component, group, place, actionManager);
  }

  @Nonnull
  public static MouseListener installPopupHandler(JComponent component, @Nonnull ActionGroup group, @NonNls String place) {
    return installPopupHandler(component, group, place, ActionManager.getInstance());
  }

  @Nonnull
  public static MouseListener installPopupHandler(JComponent component, @Nonnull ActionGroup group, @NonNls String place, ActionManager actionManager) {
    return installPopupHandler(component, group, place, actionManager, null);
  }

  @Nonnull
  public static MouseListener installPopupHandler(@Nonnull JComponent component,
                                                  @Nonnull ActionGroup group,
                                                  @NonNls String place,
                                                  @Nonnull ActionManager actionManager,
                                                  @Nullable PopupMenuListener menuListener) {
    if (ApplicationManager.getApplication() == null) {
      return new MouseAdapter() {
      };
    }
    PopupHandler popupHandler = new PopupHandler() {
      @Override
      public void invokePopup(Component comp, int x, int y) {
        ActionPopupMenu popupMenu = actionManager.createActionPopupMenu(place, group);
        popupMenu.setTargetComponent(component);
        JPopupMenu menu = popupMenu.getComponent();
        if (menuListener != null) menu.addPopupMenuListener(menuListener);
        menu.show(comp, x, y);
      }
    };
    component.addMouseListener(popupHandler);
    return popupHandler;
  }

  public static MouseListener installFollowingSelectionTreePopup(final JTree tree, @Nonnull final ActionGroup group, final String place, final ActionManager actionManager) {
    if (ApplicationManager.getApplication() == null) {
      return new MouseAdapter() {
      };
    }
    PopupHandler handler = new PopupHandler() {
      @Override
      public void invokePopup(Component comp, int x, int y) {
        if (tree.getPathForLocation(x, y) != null && Arrays.binarySearch(tree.getSelectionRows(), tree.getRowForLocation(x, y)) > -1) { //do not show popup menu on rows other than selection
          final ActionPopupMenu popupMenu = actionManager.createActionPopupMenu(place, group);
          popupMenu.getComponent().show(comp, x, y);
        }
      }
    };
    tree.addMouseListener(handler);
    return handler;
  }

  public static MouseListener installUnknownPopupHandler(JComponent component, ActionGroup group, ActionManager actionManager) {
    return installPopupHandler(component, group, ActionPlaces.UNKNOWN, actionManager);
  }

  public static MouseListener installPopupHandlerFromCustomActions(JComponent component, @Nonnull final String groupId, final String place) {
    if (ApplicationManager.getApplication() == null) {
      return new MouseAdapter() {
      };
    }
    PopupHandler popupHandler = new PopupHandler() {
      @Override
      public void invokePopup(Component comp, int x, int y) {
        ActionGroup group = (ActionGroup)CustomActionsSchema.getInstance().getCorrectedAction(groupId);
        final ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(place, group);
        popupMenu.getComponent().show(comp, x, y);
      }
    };
    component.addMouseListener(popupHandler);
    return popupHandler;
  }
}