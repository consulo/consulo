/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ui.desktop.internal.alert;

import com.intellij.ui.messages.SheetMessage;
import com.intellij.ui.messages.SheetMessageUtil;
import com.intellij.util.ui.UIUtil;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.impl.BaseAlert;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 10/07/2021
 */
public class DesktopMacAlertImpl<V> extends BaseAlert<V> {
  @RequiredUIAccess
  @Nonnull
  @Override
  public AsyncResult<V> showAsync(@Nullable Window component) {
    AsyncResult<V> result = AsyncResult.undefined();

    if (component == null) {
      component = SheetMessageUtil.getForemostWindow(null);
    }

    final Window finalComponent = component;
    UIAccess.current().give(() -> {
      Map<String, ButtonImpl> buttonMap = new HashMap<>();

      String[] buttons = new String[myButtons.size()];
      String defaultBtn = null;
      for (int i = 0; i < buttons.length; i++) {
        ButtonImpl button = myButtons.get(i);
        String btnText = getText(button).get();
        buttons[i] = btnText;

        buttonMap.put(btnText, button);

        if (button.myDefault) {
          defaultBtn = btnText;
        }
      }

      SheetMessage sheetMessage = new SheetMessage(finalComponent, myTitle.getValue(), myText.get(), getIcon(), buttons, null, defaultBtn, defaultBtn);

      String sheetMessageResultText = sheetMessage.getResult();
      if (sheetMessageResultText == null) {
        result.setRejected(myExitValue.get());
      }
      else {
        ButtonImpl button = buttonMap.get(sheetMessageResultText);
        if (button != null) {
          result.setDone(button.myValue.get());
        }
        else {
          result.setRejected(myExitValue.get());
        }
      }
    });
    return result;
  }

  @Nullable
  private Image getIcon() {
    switch (myType) {
      case INFO:
        return UIUtil.getInformationIcon();
      case WARNING:
        return UIUtil.getWarningIcon();
      case ERROR:
        return UIUtil.getErrorIcon();
      case QUESTION:
        return UIUtil.getQuestionIcon();
      default:
        throw new UnsupportedOperationException(myType.name());
    }
  }
}
