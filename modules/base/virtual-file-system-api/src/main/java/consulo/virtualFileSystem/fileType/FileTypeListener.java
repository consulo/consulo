/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.virtualFileSystem.fileType;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

import jakarta.annotation.Nonnull;

import java.util.EventListener;

@TopicAPI(ComponentScope.APPLICATION)
public interface FileTypeListener extends EventListener {
  default void beforeFileTypesChanged(@Nonnull FileTypeEvent event) {
  }

  default void fileTypesChanged(@Nonnull FileTypeEvent event) {
  }

  /**
   * @deprecated Use {@link FileTypeListener}
   */
  @Deprecated
  class Adapter implements FileTypeListener {
  }
}
