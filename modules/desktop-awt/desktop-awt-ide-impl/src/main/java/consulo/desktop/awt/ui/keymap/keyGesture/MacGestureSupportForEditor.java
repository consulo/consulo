/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.desktop.awt.ui.keymap.keyGesture;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.ui.keymap.IdeKeyEventDispatcher;
import consulo.desktop.awt.ui.keymap.IdeMouseEventDispatcher;
import consulo.eawt.wrapper.GestureUtilitiesWrapper;
import consulo.eawt.wrapper.event.PressureEventWrapper;
import consulo.eawt.wrapper.event.PressureListenerWrapper;
import consulo.ui.ex.action.BasePresentationFactory;
import consulo.ide.impl.idea.openapi.keymap.impl.KeymapManagerImpl;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * @author denis
 */
public class MacGestureSupportForEditor {

  private final ArrayList<AnAction> myActions = new ArrayList<>(1);

  public MacGestureSupportForEditor(JComponent component) {
    GestureUtilitiesWrapper.addGestureListenerTo(component, new PressureListenerWrapper() {
      @Override
      @RequiredUIAccess
      public void pressure(PressureEventWrapper e) {
        if (IdeMouseEventDispatcher.isForceTouchAllowed() && e.getStage() == 2) {
          MouseShortcut shortcut = new PressureShortcut(e.getStage());
          fillActionsList(shortcut, IdeKeyEventDispatcher.isModalContext(component));
          ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
          AnAction[] actions = myActions.toArray(new AnAction[myActions.size()]);
          for (AnAction action : actions) {
            DataContext dataContext = DataManager.getInstance().getDataContext(component);
            Presentation presentation = myPresentationFactory.getPresentation(action);
            AnActionEvent actionEvent = new AnActionEvent(null, dataContext, ActionPlaces.MAIN_MENU, presentation,
                                                          ActionManager.getInstance(),
                                                          0);
            action.beforeActionPerformedUpdate(actionEvent);

            if (presentation.isEnabled()) {
              actionManager.fireBeforeActionPerformed(action, dataContext, actionEvent);
              final Component context = dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT);

              if (context != null && !context.isShowing()) continue;

              action.actionPerformed(actionEvent);

            }
          }
          e.consume();
          IdeMouseEventDispatcher.forbidForceTouch();
        }
      }
    });
  }


  private final BasePresentationFactory myPresentationFactory = new BasePresentationFactory();

  private void fillActionsList(MouseShortcut mouseShortcut, boolean isModalContext) {
    myActions.clear();

    // search in main keymap
    if (KeymapManagerImpl.ourKeymapManagerInitialized) {
      final KeymapManager keymapManager = KeymapManager.getInstance();
      final Keymap keymap = keymapManager.getActiveKeymap();
      final String[] actionIds = keymap.getActionIds(mouseShortcut);

      ActionManager actionManager = ActionManager.getInstance();
      for (String actionId : actionIds) {
        AnAction action = actionManager.getAction(actionId);

        if (action == null) continue;

        if (isModalContext && !action.isEnabledInModalContext()) continue;

        if (!myActions.contains(action)) {
          myActions.add(action);
        }
      }
    }
  }
}
