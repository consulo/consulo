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
package consulo.desktop.awt.fileChooser.impl.system.windows2;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.eap.EarlyAccessProgramDescriptor;
import consulo.platform.Platform;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 06/10/2021
 */
@ExtensionImpl
public class IFileDialogEarlyAccessProgramDescriptor extends EarlyAccessProgramDescriptor {
  @Nonnull
  @Override
  public String getName() {
    return "New system look for file choose dialog (Windows)";
  }

  @Override
  public boolean isAvailable() {
    return Platform.current().os().isWindows();
  }
}
