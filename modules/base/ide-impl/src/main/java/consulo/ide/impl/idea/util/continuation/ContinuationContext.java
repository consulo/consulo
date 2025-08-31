/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util.continuation;

import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.function.Consumer;

public interface ContinuationContext extends ContinuationPause {
  void next(TaskDescriptor... next);

  void next(List<TaskDescriptor> next);

  void last(TaskDescriptor... next);

  void last(List<TaskDescriptor> next);

  void after(@Nonnull TaskDescriptor inQueue, TaskDescriptor... next);


  void cancelEverything();

  void cancelCurrent();

  <T extends Exception> void addExceptionHandler(Class<T> clazz, Consumer<T> consumer);
  boolean handleException(Exception e, boolean cancelEveryThing);

  void keepExisting(Object disaster, Object cure);
  void throwDisaster(Object disaster, Object cure);

  void removeNewTasksPatcher(@Nonnull Consumer<TaskDescriptor> consumer);
  void addNewTasksPatcher(@Nonnull Consumer<TaskDescriptor> consumer);
}
