// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor;

import consulo.codeEditor.event.EditorMouseEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.action.*;
import consulo.util.lang.ObjectUtil;

import org.jspecify.annotations.Nullable;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Implementation of {@link EditorPopupHandler} showing a context menu for some {@link ActionGroup} (which can depend on click location).
 *
 * @since 2019.1
 */
public abstract class ContextMenuPopupHandler implements EditorPopupHandler {
  public abstract @Nullable ActionGroup getActionGroup(EditorMouseEvent event);

  @Override
  public boolean handlePopup(EditorMouseEvent event) {
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
        Component c = e.getComponent();
        if (c != null && c.isShowing()) {
          popupMenu.getComponent().show(c, e.getX(), e.getY());
        }
      }

      event.consume();
    }
    return true;
  }

  private static @Nullable ActionGroup getGroupForId(@Nullable String groupId) {
    return groupId == null ? null : ObjectUtil.tryCast(CustomActionsSchema.getInstance().getCorrectedAction(groupId), ActionGroup.class);
  }

  /**
   * {@link ContextMenuPopupHandler} specification, which uses an action group registered in {@link ActionManager} under given id.
   */
  public abstract static class ById extends ContextMenuPopupHandler {
    @Nullable
    @Override
    public ActionGroup getActionGroup(EditorMouseEvent event) {
      return ContextMenuPopupHandler.getGroupForId(getActionGroupId(event));
    }

    public abstract @Nullable String getActionGroupId(EditorMouseEvent event);
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
    public ActionGroup getActionGroup(EditorMouseEvent event) {
      return myActionGroup;
    }
  }
}
