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
package consulo.ide.impl.idea.tasks;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Extension;
import consulo.component.extension.ExtensionPointName;
import consulo.ide.impl.idea.tasks.config.TaskRepositoryEditor;
import consulo.ide.impl.idea.util.Consumer;
import consulo.project.Project;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * The main extension point for issue tracking integration.
 *
 * @author Dmitry Avdeev
 */
@Extension(ComponentScope.APPLICATION)
public abstract class TaskRepositoryType<T extends TaskRepository> implements TaskRepositorySubtype {

  public static final ExtensionPointName<TaskRepositoryType> EP_NAME = ExtensionPointName.create(TaskRepositoryType.class);
  
  public static List<TaskRepositoryType> getRepositoryTypes() {
    return EP_NAME.getExtensionList();
  }

  @Nonnull
  public abstract String getName();

  @Nonnull
  public abstract Image getIcon();

  @Nullable
  public String getAdvertiser() { return null; }

  @Nonnull
  public abstract TaskRepositoryEditor createEditor(T repository, Project project, Consumer<T> changeListener);

  public List<TaskRepositorySubtype> getAvailableSubtypes() {
    return Arrays.asList((TaskRepositorySubtype)this);
  }

  @Nonnull
  public TaskRepository createRepository(TaskRepositorySubtype subtype) {
    return subtype.createRepository();
  }

  @Nonnull
  public abstract TaskRepository createRepository();

  public abstract Class<T> getRepositoryClass();

  /**
   * @return states that can be set by {@link TaskRepository#setTaskState(Task, TaskState)}
   */
  public EnumSet<TaskState> getPossibleTaskStates() {
    return EnumSet.noneOf(TaskState.class);
  }
}
