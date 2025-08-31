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

package consulo.ide.impl.idea.codeInspection.ex;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.ServiceManager;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import jakarta.inject.Singleton;

@Singleton
@State(name = "AppInspectionProfilesVisibleTreeState", storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml")})
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class AppInspectionProfilesVisibleTreeState implements PersistentStateComponent<VisibleTreeStateComponent> {
  private final VisibleTreeStateComponent myComponent = new VisibleTreeStateComponent();

  public static AppInspectionProfilesVisibleTreeState getInstance() {
    return ServiceManager.getService(AppInspectionProfilesVisibleTreeState.class);
  }

  @Override
  public VisibleTreeStateComponent getState() {
    return myComponent;
  }

  @Override
  public void loadState(VisibleTreeStateComponent state) {
    myComponent.copyFrom(state);
  }

  public VisibleTreeState getVisibleTreeState(InspectionProfile profile) {
    return myComponent.getVisibleTreeState(profile);
  }
}
