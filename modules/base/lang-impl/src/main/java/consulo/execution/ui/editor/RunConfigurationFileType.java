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
package consulo.execution.ui.editor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.FileType;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19/12/2021
 */
public class RunConfigurationFileType implements FileType {
  public static final RunConfigurationFileType INSTANCE = new RunConfigurationFileType();

  @Nonnull
  @Override
  public String getId() {
    return "RUN_CONFIGURATIONS";
  }

  @Nonnull
  @Override
  public LocalizeValue getDescription() {
    return LocalizeValue.of();
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Actions.Execute;
  }
}
