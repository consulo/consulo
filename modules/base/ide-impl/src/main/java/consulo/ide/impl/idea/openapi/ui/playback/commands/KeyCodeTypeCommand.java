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
import consulo.util.lang.Pair;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.util.concurrent.ActionCallback;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class KeyCodeTypeCommand extends AlphaNumericTypeCommand {

  public static final String PREFIX = CMD_PREFIX + "type";
  public static final String CODE_DELIMITER = ";";
  public static final String MODIFIER_DELIMITER = ":";

  public KeyCodeTypeCommand(String text, int line) {
    super(text, line);
  }

  @Override
  public ActionCallback _execute(final PlaybackContext context) {
    String text = getText().substring(PREFIX.length()).trim();

    int textDelim = text.indexOf(" ");
    
    final String codes;
    if (textDelim >= 0) {
      codes = text.substring(0, textDelim);
    } else {
      codes = text;
    }

    final String unicode;
    if (codes.length() + 1 < text.length()) {
      unicode = text.substring(textDelim + 1);
    } else {
      unicode = "";
    }

    final ActionCallback result = new ActionCallback();


    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(new Runnable() {
      @Override
      public void run() {
        TypingTarget typingTarget = findTarget(context);
        if (typingTarget != null) {
          typingTarget.type(unicode).doWhenDone(result.createSetDoneRunnable()).doWhenRejected(new Runnable() {
            public void run() {
              typeCodes(context, context.getRobot(), codes).notify(result);
            }
          });
        } else {
          typeCodes(context, context.getRobot(), codes).notify(result);
        }
      }
    });

    return result;
  }

  private ActionCallback typeCodes(final PlaybackContext context, final Robot robot, final String codes) {
    final ActionCallback result = new ActionCallback();

    Runnable runnable = new Runnable() {
      public void run() {
        String[] pairs = codes.split(CODE_DELIMITER);
        for (String eachPair : pairs) {
          try {
            String[] splits = eachPair.split(MODIFIER_DELIMITER);
            Integer code = Integer.valueOf(splits[0]);
            Integer modifier = Integer.valueOf(splits[1]);
            type(robot, code.intValue(), modifier.intValue());
          }
          catch (NumberFormatException e) {
            dumpError(context, "Invalid code: " + eachPair);
            result.setRejected();
            return;
          }
        }

        result.setDone();
      }
    };


    if (SwingUtilities.isEventDispatchThread()) {
      ApplicationManager.getApplication().executeOnPooledThread(runnable);
    } else {
      runnable.run();
    }

    return result;
  }

  public static Pair<List<Integer>, List<Integer>> parseKeyCodes(String keyCodesText) {
    ArrayList<Integer> codes = new ArrayList<Integer>();
    ArrayList<Integer> modifiers = new ArrayList<Integer>();

    if (keyCodesText != null) {
      String[] pairs = keyCodesText.split(CODE_DELIMITER);
      for (String each : pairs) {
        String[] strings = each.split(MODIFIER_DELIMITER);
        if (strings.length == 2) {
          codes.add(Integer.valueOf(strings[0]));
          modifiers.add(Integer.valueOf(strings[1]));
        }
      }
    }

    return new Pair<List<Integer>, List<Integer>>(codes, modifiers);
  }

  public static String unparseKeyCodes(Pair<List<Integer>, List<Integer>> pairs) {
    StringBuilder result = new StringBuilder();

    List<Integer> codes = pairs.getFirst();
    List<Integer> modifiers = pairs.getSecond();

    for (int i = 0; i < codes.size(); i++) {
      Integer each = codes.get(i);
      result.append(each.toString());
      result.append(MODIFIER_DELIMITER);
      result.append(modifiers.get(i));
      if (i < codes.size() - 1) {
        result.append(CODE_DELIMITER);
      }
    }

    return result.toString();
  }

}
