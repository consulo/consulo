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
package consulo.component.store.impl.internal.storage;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.annotation.component.TopicBroadcastDirection;
import consulo.virtualFileSystem.event.VirtualFileEvent;

import jakarta.annotation.Nonnull;

/**
* @author VISTALL
* @since 19-Jun-22
*/
// FIXME [VISTALL] looks like it's called in App scope too
@TopicAPI(value = ComponentScope.PROJECT, direction = TopicBroadcastDirection.NONE)
public interface StateStorageListener {
  void storageFileChanged(@Nonnull VirtualFileEvent event, @Nonnull StateStorage storage);
}
