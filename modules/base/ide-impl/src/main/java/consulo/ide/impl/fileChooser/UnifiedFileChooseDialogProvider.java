/*
 * Copyright 2013-2018 consulo.io
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
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDialog;
import consulo.fileChooser.PathChooserDialog;
import consulo.project.Project;
import consulo.fileChooser.provider.FileChooseDialogProvider;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2018-06-28
 */
@ExtensionImpl(order = "last", profiles = ComponentProfiles.UNIFIED)
public class UnifiedFileChooseDialogProvider implements FileChooseDialogProvider {
  @Nonnull
  @Override
  public FileChooserDialog createFileChooser(@Nonnull FileChooserDescriptor descriptor, @Nullable ComponentManager project, @Nullable Component parent) {
    return new UnifiedChooserDialog((Project)project, descriptor);
  }

  @Nonnull
  @Override
  public PathChooserDialog createPathChooser(@Nonnull FileChooserDescriptor descriptor, @Nullable ComponentManager project, @Nullable Component parent) {
    return new UnifiedChooserDialog((Project)project, descriptor);
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
