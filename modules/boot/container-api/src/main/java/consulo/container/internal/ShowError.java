/*
 * Copyright 2013-2022 consulo.io
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
package consulo.container.internal;

import consulo.container.StartupError;

/**
 * @author VISTALL
 * @since 16-Jul-22
 */
public abstract class ShowError {
  public static ShowError INSTANCE = new ShowError() {
    @Override
    public void showErrorDialogImpl(String title, String message, Throwable t) {

    }
  };

  public static void showErrorDialog(String title, String message, Throwable t) {
    StartupError.hasStartupError = true;

    System.out.println(title + ": " + message);
    if (t != null) {
      t.printStackTrace();
    }

    ShowError.INSTANCE.showErrorDialogImpl(title, message, t);
  }

  public abstract void showErrorDialogImpl(String title, String message, Throwable t);
}
