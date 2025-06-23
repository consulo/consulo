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
package consulo.task;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;

/**
 * @author Dmitry Avdeev
 * @since 2012-03-23
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@State(name = "TaskSettings", storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml")})
public class TaskSettings implements PersistentStateComponent<TaskSettings> {

  public boolean ALWAYS_DISPLAY_COMBO = false;
  public int CONNECTION_TIMEOUT = 5000;

  public static TaskSettings getInstance() {
    return Application.get().getInstance(TaskSettings.class);
  }

  @Override
  public TaskSettings getState() {
    return this;
  }

  @Override
  public void loadState(TaskSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
