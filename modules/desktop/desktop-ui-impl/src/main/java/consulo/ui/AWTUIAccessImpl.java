/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui;

import javax.annotation.Nonnull;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
class AWTUIAccessImpl implements UIAccess {
  public static UIAccess ourInstance = new AWTUIAccessImpl();

  private AWTUIAccessImpl() {
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public void give(@RequiredUIAccess @Nonnull Runnable runnable) {
    SwingUtilities.invokeLater(runnable);
  }

  @Override
  public void giveAndWait(@Nonnull Runnable runnable) {
    try {
      SwingUtilities.invokeAndWait(runnable);
    }
    catch (InterruptedException | InvocationTargetException e) {
      //
    }
  }
}
