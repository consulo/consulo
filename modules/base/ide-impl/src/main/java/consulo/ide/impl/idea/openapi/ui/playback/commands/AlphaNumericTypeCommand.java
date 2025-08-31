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
package consulo.ide.impl.idea.openapi.ui.playback.commands;

import consulo.application.ApplicationManager;
import consulo.ui.ex.TypingTarget;
import consulo.ide.impl.idea.openapi.ui.playback.PlaybackContext;
import consulo.util.concurrent.ActionCallback;
import consulo.application.ui.wm.IdeFocusManager;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class AlphaNumericTypeCommand extends TypeCommand {

  public AlphaNumericTypeCommand(String text, int line) {
    super(text, line);
  }

  public ActionCallback _execute(PlaybackContext context) {
    return type(context, getText());
  }

  protected ActionCallback type(final PlaybackContext context, final String text) {
    final ActionCallback result = new ActionCallback();

    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(new Runnable() {
      @Override
      public void run() {
        TypingTarget typingTarget = findTarget(context);
        if (typingTarget != null) {
          typingTarget.type(text).doWhenDone(result.createSetDoneRunnable()).doWhenRejected(new Runnable() {
            public void run() {
              typeByRobot(context.getRobot(), text).notify(result);
            }
          });
        } else {
          typeByRobot(context.getRobot(), text).notify(result);
        }
      }
    });

    return result;
  }

  private ActionCallback typeByRobot(final Robot robot, final String text) {
    final ActionCallback result = new ActionCallback();

    Runnable typeRunnable = new Runnable() {
      public void run() {
        for (int i = 0; i < text.length(); i++) {
          char each = text.charAt(i);
          if ('\\' == each && i + 1 < text.length()) {
            char next = text.charAt(i + 1);
            boolean processed = true;
            switch (next) {
              case 'n':
                type(robot, KeyEvent.VK_ENTER, 0);
                break;
              case 't':
                type(robot, KeyEvent.VK_TAB, 0);
                break;
              case 'r':
                type(robot, KeyEvent.VK_ENTER, 0);
                break;
              default:
                processed = false;
            }

            if (processed) {
              i++;
              continue;
            }
          }
          type(robot, get(each));
        }

        result.setDone();
      }
    };

    if (SwingUtilities.isEventDispatchThread()) {
      ApplicationManager.getApplication().executeOnPooledThread(typeRunnable);
    } else {
      typeRunnable.run();
    }

    return result;
  }

  @Nullable
  public static TypingTarget findTarget(PlaybackContext context) {
    if (!context.isUseTypingTargets()) return null;

    Component each = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

    while (each != null) {
      if (each instanceof TypingTarget) {
        return (TypingTarget)each;
      }

      each = each.getParent();
    }

    return null;
  }


}