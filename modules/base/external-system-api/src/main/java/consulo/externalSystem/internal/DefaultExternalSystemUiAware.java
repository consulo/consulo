/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.internal;

import consulo.application.Application;
import consulo.externalSystem.ui.ExternalSystemUiAware;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;

/**
 * This class is not singleton but offers single-point-of-usage field - {@link #INSTANCE}.
 * 
 * @author Denis Zhdanov
 * @since 5/15/13 12:45 PM
 */
public class DefaultExternalSystemUiAware implements ExternalSystemUiAware {

  @Nonnull
  public static final DefaultExternalSystemUiAware INSTANCE = new DefaultExternalSystemUiAware();

  @Nonnull
  @Override
  public String getProjectRepresentationName(@Nonnull String targetProjectPath, @Nullable String rootProjectPath) {
    return new File(targetProjectPath).getParentFile().getName();
  }

  @Nullable
  @Override
  public FileChooserDescriptor getExternalProjectConfigDescriptor() {
    return null;
  }

  @Nonnull
  @Override
  public Image getProjectIcon() {
    return Application.get().getIcon();
  }

  @Nonnull
  @Override
  public Image getTaskIcon() {
    return PlatformIconGroup.nodesTask();
  }
}
