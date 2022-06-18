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

/*
 * User: anna
 * Date: 08-Dec-2008
 */

package consulo.ide.impl.copyright.impl.actions;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;

@Singleton
@Service(ComponentScope.PROJECT)
@ServiceImpl
@State(name = "UpdateCopyrightCheckinHandler", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
public class UpdateCopyrightCheckinHandlerState implements PersistentStateComponent<UpdateCopyrightCheckinHandlerState> {
  public boolean UPDATE_COPYRIGHT = false;

  public UpdateCopyrightCheckinHandlerState getState() {
    return this;
  }

  public void loadState(UpdateCopyrightCheckinHandlerState state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public static UpdateCopyrightCheckinHandlerState getInstance(Project project) {
    return ServiceManager.getService(project, UpdateCopyrightCheckinHandlerState.class);
  }
}