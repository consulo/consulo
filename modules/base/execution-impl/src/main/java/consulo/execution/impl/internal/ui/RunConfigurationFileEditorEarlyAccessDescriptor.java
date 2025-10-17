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
package consulo.execution.impl.internal.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.eap.EarlyAccessProgramDescriptor;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19/12/2021
 */
@ExtensionImpl
public class RunConfigurationFileEditorEarlyAccessDescriptor extends EarlyAccessProgramDescriptor {
  @Nonnull
  @Override
  public LocalizeValue getName() {
    return LocalizeValue.localizeTODO("Run Configuration editor");
  }

  @Nonnull
  @Override
  public LocalizeValue getDescription() {
    return LocalizeValue.localizeTODO("Replace run configuration dialog with run configuration editor");
  }
}
