// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor;

import consulo.logging.Logger;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.action.*;

import org.jspecify.annotations.Nullable;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link EditorPopupHandler} showing a context menu for some {@link ActionGroup} (which can depend on click location).
 *
 * @since 2019.1
 */
public abstract class ContextMenuPopupHandler implements EditorPopupHandler {
    private static final Logger LOG = Logger.getInstance(ContextMenuPopupHandler.class);

  public abstract CompletableFuture<@Nullable ActionGroup> getActionGroupAsync(EditorMouseEvent event);

  @Override
  public boolean handlePopup(EditorMouseEvent event) {
    UIAccess uiAccess = UIAccess.current();
    getActionGroupAsync(event).whenComplete((group, throwable) -> {
        if (throwable != null) {
            LOG.error("Failed to resolve the editor context menu group", throwable);
            return;
        }
        uiAccess.giveIfNeed(() -> showPopup(event, group));
    });
    return true;
  }

  @RequiredUIAccess
  private static void showPopup(EditorMouseEvent event, @Nullable ActionGroup group) {
    if (group == null) {
      return;
    }
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

  private static CompletableFuture<@Nullable ActionGroup> getGroupForIdAsync(@Nullable String groupId) {
    return groupId == null ? CompletableFuture.completedFuture(null) : CustomActionsSchema.getCorrectedGroupAsync(groupId);
  }

  /**
   * {@link ContextMenuPopupHandler} specification, which uses an action group registered in {@link ActionManager} under given id.
   */
  public abstract static class ById extends ContextMenuPopupHandler {
    @Override
    public CompletableFuture<@Nullable ActionGroup> getActionGroupAsync(EditorMouseEvent event) {
      return ContextMenuPopupHandler.getGroupForIdAsync(getActionGroupId(event));
    }

    public abstract @Nullable String getActionGroupId(EditorMouseEvent event);
  }

  /**
   * Popup handler which always shows context menu for the same action group (regardless of mouse click location).
   */
  public static class Simple extends ContextMenuPopupHandler {
    private final @Nullable ActionGroup myActionGroup;
    private final @Nullable String myGroupId;

    public Simple(@Nullable ActionGroup actionGroup) {
      myActionGroup = actionGroup;
      myGroupId = null;
    }

    public Simple(@Nullable String groupId) {
      myActionGroup = null;
      myGroupId = groupId;
    }

    @Override
    public CompletableFuture<@Nullable ActionGroup> getActionGroupAsync(EditorMouseEvent event) {
      return myActionGroup != null
        ? CompletableFuture.completedFuture(myActionGroup)
        : ContextMenuPopupHandler.getGroupForIdAsync(myGroupId);
    }
  }
}
