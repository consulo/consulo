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
package consulo.ide.impl.idea.openapi.vfs.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.event.AsyncFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.pointer.VirtualFilePointerManager;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import java.util.List;

@ExtensionImpl
final class VirtualFilePointerAsyncFileListenerImpl implements AsyncFileListener {
  @Inject
  VirtualFilePointerAsyncFileListenerImpl() {
  }

  @Override
  public ChangeApplier prepareChange(@Nonnull List<? extends VFileEvent> events) {
    return ((VirtualFilePointerManagerImpl)VirtualFilePointerManager.getInstance()).prepareChange(events);
  }
}
