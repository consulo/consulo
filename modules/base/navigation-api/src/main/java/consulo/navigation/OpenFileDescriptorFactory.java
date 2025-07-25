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
package consulo.navigation;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.ComponentManager;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19-Feb-22
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface OpenFileDescriptorFactory {
  @Nonnull
  static OpenFileDescriptorFactory getInstance(@Nonnull ComponentManager project) {
    return project.getInstance(OpenFileDescriptorFactory.class);
  }

  interface Builder {
    @Nonnull
    Builder offset(int offset);

    @Nonnull
    Builder line(int line);

    @Nonnull
    Builder column(int column);

    @Nonnull
    Builder persist();

    @Nonnull
    Builder useCurrentWindow(boolean useCurrentWindow);

    @Nonnull
    OpenFileDescriptor build();
  }

  @Nonnull
  @Deprecated
  @DeprecationInfo("Use #newBuilder")
  default Builder builder(@Nonnull VirtualFile file) {
    return newBuilder(file);
  }

  @Nonnull
  Builder newBuilder(@Nonnull VirtualFile file);
}
