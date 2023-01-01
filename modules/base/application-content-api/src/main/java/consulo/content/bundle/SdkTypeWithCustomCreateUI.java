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
package consulo.content.bundle;

import consulo.ui.annotation.RequiredUIAccess;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 20/01/2022
 */
public interface SdkTypeWithCustomCreateUI {
  /**
   * Shows the custom SDK create UI. The returned SDK needs to have the correct name and home path; the framework will call
   * setupSdkPaths() on the returned SDK.
   *
   * @param sdkModel           the list of SDKs currently displayed in the configuration dialog.
   * @param parentComponent    the parent component for showing the dialog.
   * @param sdkCreatedCallback the callback to which the created SDK is passed.
   * @since 12.0
   */
  void showCustomCreateUI(SdkModel sdkModel, JComponent parentComponent, @RequiredUIAccess Consumer<Sdk> sdkCreatedCallback);
}
