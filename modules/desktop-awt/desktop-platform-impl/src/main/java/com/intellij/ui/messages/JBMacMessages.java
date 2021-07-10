/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.ui.messages;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.mac.MacMessages;
import com.intellij.util.ui.UIUtil;
import consulo.ui.image.Image;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.intellij.ui.messages.SheetMessageUtil.getForemostWindow;

/**
 * Created by Denis Fokin
 */
@Singleton
public class JBMacMessages extends MacMessages {

  @Override
  public int showYesNoCancelDialog(@Nonnull String title,
                                   String message,
                                   @Nonnull String defaultButton,
                                   String alternateButton,
                                   String otherButton,
                                   @Nullable consulo.ui.Window window,
                                   @Nullable DialogWrapper.DoNotAskOption doNotAskOption) {
    if (window == null) {
      window = getForemostWindow(null);
    }
    SheetMessage sheetMessage = new SheetMessage(window, title, message, UIUtil.getQuestionIcon(),
                                                 new String [] {defaultButton, alternateButton, otherButton}, null, defaultButton, alternateButton);
    String resultString = sheetMessage.getResult();
    int result = resultString.equals(defaultButton) ? Messages.YES : resultString.equals(alternateButton) ? Messages.NO : Messages.CANCEL;
    if (doNotAskOption != null) {
      doNotAskOption.setToBeShown(sheetMessage.toBeShown(), result);
    }
    return result;
  }

  @Override
  public int showMessageDialog(@Nonnull String title,
                               String message,
                               @Nonnull String[] buttons,
                               boolean errorStyle,
                               @Nullable consulo.ui.Window window,
                               int defaultOptionIndex,
                               int focusedOptionIndex,
                               @Nullable DialogWrapper.DoNotAskOption doNotAskDialogOption) {
    if (window == null) {
      window = getForemostWindow(null);
    }

    Image icon = errorStyle ? UIUtil.getErrorIcon() : UIUtil.getInformationIcon();

    focusedOptionIndex = (defaultOptionIndex == focusedOptionIndex) ? buttons.length - 1 : focusedOptionIndex;

    SheetMessage sheetMessage = new SheetMessage(window, title, message, icon, buttons, doNotAskDialogOption, buttons[defaultOptionIndex],
                                                 buttons[focusedOptionIndex]);
    String result = sheetMessage.getResult();
    for (int i = 0; i < buttons.length; i++) {
      if (result.equals(buttons[i])) {
        if (doNotAskDialogOption != null) {
          doNotAskDialogOption.setToBeShown(sheetMessage.toBeShown(), i);
        }
        return i;
      }
    }
    return -1;
  }

  @Override
  public void showOkMessageDialog(@Nonnull String title, String message, @Nonnull String okText, @Nullable consulo.ui.Window window) {
    if (window == null) {
      window = getForemostWindow(null);
    }
    new SheetMessage(window, title, message, UIUtil.getInformationIcon(), new String [] {okText}, null, null, okText);
  }

  @Override
  public void showOkMessageDialog(@Nonnull String title, String message, @Nonnull String okText) {
    final consulo.ui.Window foremostWindow = getForemostWindow(null);
    new SheetMessage(foremostWindow, title, message, UIUtil.getInformationIcon(), new String [] {okText},null, null, okText);
  }

  @Override
  public int showYesNoDialog(@Nonnull String title,
                             String message,
                             @Nonnull String yesButton,
                             @Nonnull String noButton,
                             @Nullable consulo.ui.Window window) {
    if (window == null) {
      window = getForemostWindow(null);
    }
    SheetMessage sheetMessage = new SheetMessage(window, title, message, UIUtil.getQuestionIcon(),
                                                 new String [] {yesButton, noButton}, null, yesButton, noButton);
    return sheetMessage.getResult().equals(yesButton) ? Messages.YES : Messages.NO;
  }

  @Override
  public int showYesNoDialog(@Nonnull String title,
                             String message,
                             @Nonnull String yesButton,
                             @Nonnull String noButton,
                             @Nullable consulo.ui.Window window,
                             @Nullable DialogWrapper.DoNotAskOption doNotAskDialogOption) {
    if (window == null) {
      window = getForemostWindow(null);
    }
    SheetMessage sheetMessage = new SheetMessage(window, title, message, UIUtil.getQuestionIcon(),
                                                 new String [] {yesButton, noButton}, doNotAskDialogOption, yesButton, noButton);
    int result = sheetMessage.getResult().equals(yesButton) ? Messages.YES : Messages.NO;
    if (doNotAskDialogOption != null) {
      doNotAskDialogOption.setToBeShown(sheetMessage.toBeShown(), result);
    }
    return result;
  }

  @Override
  public void showErrorDialog(@Nonnull String title, String message, @Nonnull String okButton, @Nullable consulo.ui.Window window) {
    if (window == null) {
      window = getForemostWindow(null);
    }
    new SheetMessage(window, title, message, UIUtil.getErrorIcon(), new String [] {okButton}, null, null, okButton);
  }
}
