/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.ui.playback.commands;

import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionImplUtil;
import consulo.ide.impl.idea.openapi.ui.playback.PlaybackContext;
import consulo.ui.ex.action.*;
import consulo.util.concurrent.ActionCallback;
import consulo.application.util.registry.Registry;
import consulo.application.ui.wm.IdeFocusManager;

import java.awt.*;
import java.awt.event.InputEvent;

/**
 * @author kirillk
 * @since 2011-08-23
 */
public class ToggleActionCommand extends AbstractCommand {
  
  public static final String PREFIX = CMD_PREFIX + "toggle";

  public static final String ON = "on";
  public static final String OFF = "off";
  
  public ToggleActionCommand(String text, int line) {
    super(text, line);
  }

  @Override
  protected boolean isAwtThread() {
    return true;
  }

  @Override
  protected ActionCallback _execute(PlaybackContext context) {
    String[] args = getText().substring(PREFIX.length()).trim().split(" ");
    String syntaxText = "Syntax error, expected: " + PREFIX + " " + ON + "|" + OFF + " actionName";
    if (args.length != 2) {
      context.error(syntaxText, getLine());
      return new ActionCallback.Rejected();
    }
    
    final boolean on;
    if (ON.equalsIgnoreCase(args[0])) {
      on = true;
    } else if (OFF.equalsIgnoreCase(args[0])) {
      on = false;
    } else {
      context.error(syntaxText, getLine());
      return new ActionCallback.Rejected();
    }
    
    String actionId = args[1];
    final AnAction action = ActionManager.getInstance().getAction(actionId);
    if (action == null) {
      context.error("Unknown action id=" + actionId, getLine());
      return new ActionCallback.Rejected();
    }

    if (!(action instanceof ToggleAction)) {
      context.error("Action is not a toggle action id=" + actionId, getLine());
      return new ActionCallback.Rejected();
    }

    final InputEvent inputEvent = ActionCommand.getInputEvent(actionId);
    final ActionCallback result = new ActionCallback();

    context.getRobot().delay(Registry.intValue("actionSystem.playback.delay"));

    IdeFocusManager fm = IdeFocusManager.getGlobalInstance();
    fm.doWhenFocusSettlesDown(new Runnable() {
      @Override
      public void run() {
        Presentation presentation = (Presentation)action.getTemplatePresentation().clone();
        AnActionEvent event =
            new AnActionEvent(inputEvent, DataManager.getInstance()
                .getDataContext(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()), ActionPlaces.UNKNOWN,
                              presentation, ActionManager.getInstance(), 0);

        ActionImplUtil.performDumbAwareUpdate(action, event, false);

        Boolean state = (Boolean)event.getPresentation().getClientProperty(ToggleAction.SELECTED_PROPERTY);
        if (state.booleanValue() != on) {
          ActionManager.getInstance().tryToExecute(action, inputEvent, null, ActionPlaces.UNKNOWN, true).doWhenProcessed(result.createSetDoneRunnable());
        }
        else {
          result.setDone();
        }
      }
    });


    return result;
  }
}
