/*
 * Copyright 2013-2023 consulo.io
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
package consulo.desktop.util.windows.defender;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.eap.EarlyAccessProgramDescriptor;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 21/08/2023
 */
@ExtensionImpl
public class WindowsDefenderCheckerEarlyAccessDescriptor extends EarlyAccessProgramDescriptor {
  @Override
  public boolean isAvailable() {
    return Platform.current().os().isWindows();
  }

  @Nonnull
  @Override
  public LocalizeValue getName() {
    return LocalizeValue.localizeTODO("Windows Defender Checker");
  }

  @Nonnull
  @Override
  public LocalizeValue getDescription() {
    return LocalizeValue.localizeTODO("Enable checking for protecting source files by Windows Defender which can slow running IDE");
  }
}
