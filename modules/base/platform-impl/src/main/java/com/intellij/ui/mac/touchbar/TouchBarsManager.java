/*
 * Copyright 2013-2019 consulo.io
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
package com.intellij.ui.mac.touchbar;

import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.popup.AbstractPopup;
import consulo.disposer.Disposable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * TODO [VISTALL] stub
 */
public class TouchBarsManager {
  public static boolean isTouchBarEnabled() {
    return false;
  }

  public static void showStopRunningBar(List<? extends Pair<RunContentDescriptor, Runnable>> stoppableDescriptors) {
  }

  public static void onFocusEvent(AWTEvent e) {
    // nothing
  }

  public static void onUpdateEditorHeader(@Nonnull Editor editor, JComponent header) {
    // nothing
  }

  public static Disposable showPopupBar(AbstractPopup abstractPopup, JComponent content) {
    // nothing
    return null;
  }
}
