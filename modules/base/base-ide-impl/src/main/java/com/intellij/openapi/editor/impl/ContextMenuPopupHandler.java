// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.impl;

import com.intellij.ide.ui.customization.CustomActionsSchema;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.ex.EditorPopupHandler;
import consulo.ui.event.details.InputDetails;
import consulo.util.lang.ObjectUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Implementation of {@link EditorPopupHandler} showing a context menu for some {@link ActionGroup} (which can depend on click location).
 *
 * @since 2019.1
 */
public abstract class ContextMenuPopupHandler implements EditorPopupHandler {
  @Nullable
  public abstract ActionGroup getActionGroup(@Nonnull EditorMouseEvent event);

  @Override
  public boolean handlePopup(@Nonnull EditorMouseEvent event) {
    ActionGroup group = getActionGroup(event);
    if (group != null) {
      ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.EDITOR_POPUP, group);

      InputDetails inputDetails = event.getInputDetails();
      if (inputDetails != null) {
        consulo.ui.Component uiComponent = event.getEditor().getUIComponent();

        popupMenu.show(uiComponent, inputDetails.getX(), inputDetails.getY());
      }
      // obsolete implementation
      else {
        MouseEvent e = event.getMouseEvent();
        final Component c = e.getComponent();
        if (c != null && c.isShowing()) {
          popupMenu.getComponent().show(c, e.getX(), e.getY());
        }
      }

      event.consume();
    }
    return true;
  }

  @Nullable
  private static ActionGroup getGroupForId(@Nullable String groupId) {
    return groupId == null ? null : ObjectUtil.tryCast(CustomActionsSchema.getInstance().getCorrectedAction(groupId), ActionGroup.class);
  }

  /**
   * {@link ContextMenuPopupHandler} specification, which uses an action group registered in {@link ActionManager} under given id.
   */
  public abstract static class ById extends ContextMenuPopupHandler {
    @Nullable
    @Override
    public ActionGroup getActionGroup(@Nonnull EditorMouseEvent event) {
      return ContextMenuPopupHandler.getGroupForId(getActionGroupId(event));
    }

    @Nullable
    public abstract String getActionGroupId(@Nonnull EditorMouseEvent event);
  }

  /**
   * Popup handler which always shows context menu for the same action group (regardless of mouse click location).
   */
  public static class Simple extends ContextMenuPopupHandler {
    private final ActionGroup myActionGroup;

    public Simple(ActionGroup actionGroup) {
      myActionGroup = actionGroup;
    }

    public Simple(String groupId) {
      this(ContextMenuPopupHandler.getGroupForId(groupId));
    }

    @Nullable
    @Override
    public ActionGroup getActionGroup(@Nonnull EditorMouseEvent event) {
      return myActionGroup;
    }
  }
}
