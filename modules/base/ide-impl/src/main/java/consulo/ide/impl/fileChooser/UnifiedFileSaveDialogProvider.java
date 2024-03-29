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
package consulo.ide.impl.fileChooser;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ExtensionImpl;
import consulo.component.ComponentManager;
import consulo.fileChooser.FileSaverDescriptor;
import consulo.fileChooser.FileSaverDialog;
import consulo.fileChooser.provider.FileSaveDialogProvider;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;

/**
 * @author VISTALL
 * @since 10/07/2021
 */
@ExtensionImpl(order = "last", profiles = ComponentProfiles.UNIFIED)
public class UnifiedFileSaveDialogProvider implements FileSaveDialogProvider {
  @Nonnull
  @Override
  public FileSaverDialog createSaveFileDialog(@Nonnull FileSaverDescriptor descriptor, @Nullable ComponentManager project, @Nullable Component parent) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public String getId() {
    return APPLICATION_ID;
  }

  @Nonnull
  @Override
  public String getName() {
    return "application";
  }
}
