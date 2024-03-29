/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.virtualFileSystem.event;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

import jakarta.annotation.Nonnull;
import java.util.List;

@TopicAPI(ComponentScope.APPLICATION)
public interface BulkFileListener {
  /**
   * @deprecated obsolete, implement {@link BulkFileListener} directly (to be removed in IDEA 2019)
   */
  class Adapter implements BulkFileListener {
    @Override
    public void before(@Nonnull List<? extends VFileEvent> events) { }

    @Override
    public void after(@Nonnull List<? extends VFileEvent> events) { }
  }

  default void before(@Nonnull List<? extends VFileEvent> events) { }

  default void after(@Nonnull List<? extends VFileEvent> events) { }
}