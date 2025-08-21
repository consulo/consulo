/*
 * Copyright 2013-2021 consulo.io
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
package consulo.sandboxPlugin.tasks;

import consulo.application.progress.ProgressIndicator;
import consulo.task.BaseRepository;
import consulo.task.Task;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 15/12/2021
 */
public class SandTaskRepository extends BaseRepository {
  private Map<String, SandTask> myTasks = new LinkedHashMap<>();

  public SandTaskRepository() {
      myTasks.put("SAND-1", new SandTask("SAND-1", "Test sand", false));
      myTasks.put("SAND-2", new SandTask("SAND-2", "Closed", true));
      myTasks.put("SAND-100", new SandTask("SAND-100", "Hello", false));
  }


  @Override
  public Task[] getIssues(@Nullable String query, int offset, int limit, boolean withClosed, @Nonnull ProgressIndicator cancelled) throws Exception {
    return myTasks.values().toArray(Task[]::new);
  }

  @Nullable
  @Override
  public Task findTask(@Nonnull String id) throws Exception {
    return myTasks.get(id);
  }

  @Nonnull
  @Override
  public BaseRepository clone() {
    SandTaskRepository repository = new SandTaskRepository();
    repository.setRepositoryType(getRepositoryType());
    return repository;
  }

  @Override
  public boolean isConfigured() {
    return true;
  }
}
