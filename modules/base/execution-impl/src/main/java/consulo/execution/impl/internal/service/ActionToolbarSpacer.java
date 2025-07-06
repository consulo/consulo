// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * Spacer could be combined with ActionToolbar component in order to prevent
 * collapsing of toolbar containing panel when toolbar action group is empty.
 */
public final class ActionToolbarSpacer extends JLabel {
  private static final AnAction EMPTY_ACTION = new DumbAwareAction(Image.empty(Image.DEFAULT_ICON_SIZE)) {
    @Override
    public void update(@Nonnull AnActionEvent e) {
      e.getPresentation().setEnabled(false);
    }

//    @Override
//    public @NotNull ActionUpdateThread getActionUpdateThread() {
//      return ActionUpdateThread.BGT;
//    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
    }
  };

  private static ActionToolbar createPrototypeToolbar(boolean horizontal) {
    DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.add(EMPTY_ACTION);
    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, actionGroup, horizontal);
    toolbar.getComponent().setBorder(JBUI.Borders.empty());
    return toolbar;
  }

  private final boolean myHorizontal;

  public ActionToolbarSpacer(boolean horizontal) {
    myHorizontal = horizontal;
    ActionToolbar prototypeToolbar = createPrototypeToolbar(horizontal);
    prototypeToolbar.setTargetComponent(null);
    prototypeToolbar.updateActionsImmediately();
    if (horizontal) {
      setPreferredSize(new Dimension(0, prototypeToolbar.getComponent().getPreferredSize().height));
    }
    else {
      setPreferredSize(new Dimension(prototypeToolbar.getComponent().getPreferredSize().width, 0));
    }
  }

  public boolean isHorizontal() {
    return myHorizontal;
  }
}
