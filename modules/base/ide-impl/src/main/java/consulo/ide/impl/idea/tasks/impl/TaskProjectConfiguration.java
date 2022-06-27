/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.tasks.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.impl.idea.tasks.TaskManager;
import consulo.ide.impl.idea.tasks.TaskRepository;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Property;
import consulo.util.xml.serializer.annotation.Tag;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
* @author Dmitry Avdeev
*/
@Singleton
@State(name = "TaskProjectConfiguration", storages = @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/misc.xml"))
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class TaskProjectConfiguration implements PersistentStateComponent<TaskProjectConfiguration> {

  @Tag("server")
  public static class SharedServer {
    @Attribute("type")
    public String type;
    @Attribute("url")
    public String url;
  }

  @Property(surroundWithTag = false)
  @AbstractCollection(surroundWithTag = false, elementTag = "server")
  public List<SharedServer> servers = new ArrayList<SharedServer>();

  private final TaskManagerImpl myManager;

  // for serialization
  public TaskProjectConfiguration() {
    myManager = null;
  }

  @Inject
  public TaskProjectConfiguration(@Nonnull TaskManager manager) {
    myManager = (TaskManagerImpl) manager;
  }

  public TaskProjectConfiguration getState() {
    servers.clear();
      for (TaskRepository repository : myManager.getAllRepositories()) {
        if (repository.isShared()) {
          SharedServer server = new SharedServer();
          server.type = repository.getRepositoryType().getName();
          server.url = repository.getUrl();
          servers.add(server);
        }
      }
    return this;
  }

  public void loadState(TaskProjectConfiguration state) {
    servers.clear();
    for (final SharedServer server : state.servers) {
      if (server.url == null || server.type == null) {
        continue;
      }
      servers.add(server);
    }
  }

}
