/*
 * Copyright 2013-2017 consulo.io
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
package consulo.desktop.awt.ui.plaf;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.eap.EarlyAccessProgramDescriptor;
import consulo.platform.Platform;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 6/7/17
 */
@ExtensionImpl
public class GTKPlusEAPDescriptor extends EarlyAccessProgramDescriptor {
  @Nonnull
  @Override
  public String getName() {
    return "Enabled GTK+ theme";
  }

  @Override
  public boolean isAvailable() {
    return Platform.current().os().isLinux();
  }

  @Override
  public boolean isRestartRequired() {
    return true;
  }
}
