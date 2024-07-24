// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.action;

import consulo.dataContext.DataManager;
import consulo.ui.ex.action.BasePresentationFactory;
import consulo.ide.impl.idea.openapi.actionSystem.impl.WeakTimerListener;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.ui.ex.action.*;
import consulo.ui.ex.util.TextWithMnemonic;
import consulo.dataContext.DataContext;
import consulo.ide.impl.dataContext.BaseDataManager;

import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class ButtonToolbarImpl extends JPanel {
  private final DataManager myDataManager;
  private final String myPlace;
  private final BasePresentationFactory myPresentationFactory;
  private final List<ActionJButton> myActions = new ArrayList<>();

  public ButtonToolbarImpl(@Nonnull String place, @Nonnull ActionGroup actionGroup) {
    super(new GridBagLayout());
    myPlace = place;
    myPresentationFactory = new BasePresentationFactory();
    myDataManager = DataManager.getInstance();

    initButtons(actionGroup);

    updateActions();
    ActionManagerEx.getInstanceEx().addTimerListener(500, new WeakTimerListener(new MyTimerListener()));
    enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
  }

  private void initButtons(@Nonnull ActionGroup actionGroup) {
    final AnAction[] actions = actionGroup.getChildren(null);

    if (actions.length > 0) {
      int gridx = 0;


      add(// left strut
          Box.createHorizontalGlue(), new GridBagConstraints(gridx++, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0));
      JPanel buttonsPanel = createButtons(actions);
      //noinspection UnusedAssignment
      add(buttonsPanel, new GridBagConstraints(gridx++, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0));
    }
  }

  private JPanel createButtons(AnAction[] actions) {
    JPanel buttonsPanel = new JPanel(new GridLayout(1, actions.length, 5, 0));
    for (final AnAction action : actions) {
      JButton button = createButton(action);
      myActions.add((ActionJButton)button);
      buttonsPanel.add(button);
    }
    return buttonsPanel;
  }

  protected JButton createButton(AnAction action) {
    return new ActionJButton(action);
  }

  public JComponent getComponent() {
    return this;
  }

  private class ActionJButton extends JButton {
    private final AnAction myAction;

    ActionJButton(final AnAction action) {
      myAction = action;
      TextWithMnemonic textWithMnemonic = TextWithMnemonic.parse(action.getTemplatePresentation().getTextValue().getValue());

      setText(textWithMnemonic.getText());
      setMnemonic(textWithMnemonic.getMnemonic());
      setDisplayedMnemonicIndex(textWithMnemonic.getMnemonicIndex());

      addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          AnActionEvent event = new AnActionEvent(null, ((BaseDataManager)DataManager.getInstance()).getDataContextTest(ButtonToolbarImpl.this), myPlace, myPresentationFactory.getPresentation(action),
                                                  ActionManager.getInstance(), e.getModifiers());
          if (ActionUtil.lastUpdateAndCheckDumb(action, event, false)) {
            ActionUtil.performActionDumbAware(action, event);
          }
        }
      });

    }

    public void updateAction(final DataContext dataContext) {
      AnActionEvent event = new AnActionEvent(null, dataContext, myPlace, myPresentationFactory.getPresentation(myAction), ActionManager.getInstance(), 0);
      event.setInjectedContext(myAction.isInInjectedContext());
      myAction.update(event);
      setVisible(event.getPresentation().isVisible());
      setEnabled(event.getPresentation().isEnabled());
    }
  }

  private final class MyTimerListener implements TimerListener {
    @Override
    public IdeaModalityState getModalityState() {
      return IdeaModalityState.stateForComponent(ButtonToolbarImpl.this);
    }

    @Override
    public void run() {
      if (!isShowing()) {
        return;
      }

      Window mywindow = SwingUtilities.windowForComponent(ButtonToolbarImpl.this);
      if (mywindow != null && !mywindow.isActive()) return;

      // do not update when a popup menu is shown (if popup menu contains action which is also in the toolbar, it should not be enabled/disabled)
      final MenuSelectionManager menuSelectionManager = MenuSelectionManager.defaultManager();
      final MenuElement[] selectedPath = menuSelectionManager.getSelectedPath();
      if (selectedPath.length > 0) {
        return;
      }

      // don't update toolbar if there is currently active modal dialog

      final Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
      if (window instanceof Dialog) {
        final Dialog dialog = (Dialog)window;
        if (dialog.isModal() && !SwingUtilities.isDescendingFrom(ButtonToolbarImpl.this, dialog)) {
          return;
        }
      }

      updateActions();
    }
  }

  private void updateActions() {
    final DataContext dataContext = ((BaseDataManager)myDataManager).getDataContextTest(this);
    for (ActionJButton action : myActions) {
      action.updateAction(dataContext);
    }

    repaint();
  }
}
