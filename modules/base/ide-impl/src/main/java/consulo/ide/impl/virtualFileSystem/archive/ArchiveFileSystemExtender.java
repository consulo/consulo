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
package consulo.ide.impl.virtualFileSystem.archive;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.ComponentManager;
import consulo.component.extension.ExtensionExtender;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.archive.ArchiveFileSystemProvider;

import jakarta.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 10-Aug-22
 */
@ExtensionImpl
public class ArchiveFileSystemExtender implements ExtensionExtender<VirtualFileSystem> {
  @Override
  public void extend(@Nonnull ComponentManager componentManager, @Nonnull Consumer<VirtualFileSystem> consumer) {
    for (ArchiveFileSystemProvider provider : componentManager.getExtensionList(ArchiveFileSystemProvider.class)) {
      consumer.accept(new ArchiveFileSystemByProvider(provider));
    }
  }

  @Nonnull
  @Override
  public Class<VirtualFileSystem> getExtensionClass() {
    return VirtualFileSystem.class;
  }

  @Override
  public boolean hasAnyExtensions(ComponentManager componentManager) {
    // we not interest for this check
    return true;
  }
}
