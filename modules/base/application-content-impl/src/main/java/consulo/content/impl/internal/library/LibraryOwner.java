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
package consulo.content.impl.internal.library;

import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.content.impl.internal.GlobalLibraryRootListenerProvider;
import consulo.virtualFileSystem.pointer.VirtualFilePointerListener;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 09-Apr-22
 */
public interface LibraryOwner {
  static LibraryOwner APPLICATION = new LibraryOwner() {
    @Nonnull
    @Override
    public VirtualFilePointerListener getListener() {
      return Application.get().getInstance(GlobalLibraryRootListenerProvider.class).getListener();
    }
  };

  @Nonnull
  VirtualFilePointerListener getListener();

  @Nullable
  default ComponentManager getModule() {
    return null;
  }
}
