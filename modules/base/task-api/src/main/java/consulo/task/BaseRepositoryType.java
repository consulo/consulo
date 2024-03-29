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
package consulo.task;

import consulo.project.Project;
import consulo.task.ui.BaseRepositoryEditor;
import consulo.task.ui.TaskRepositoryEditor;

import jakarta.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author Dmitry Avdeev
 */
public abstract class BaseRepositoryType<T extends BaseRepository> extends TaskRepositoryType<T> {
  @Override
  @Nonnull
  public TaskRepositoryEditor createEditor(final T repository, Project project, final Consumer<T> changeListener) {
    return new BaseRepositoryEditor<T>(project, repository, changeListener);
  }
}
