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
import consulo.project.Project;
import jakarta.inject.Singleton;

@Singleton
@State(name = "ProjectInspectionProfilesVisibleTreeState", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class ProjectInspectionProfilesVisibleTreeState implements PersistentStateComponent<VisibleTreeStateComponent> {
  private final VisibleTreeStateComponent myComponent = new VisibleTreeStateComponent();

  public static ProjectInspectionProfilesVisibleTreeState getInstance(Project project) {
    return ServiceManager.getService(project, ProjectInspectionProfilesVisibleTreeState.class);
  }

  @Override
  public VisibleTreeStateComponent getState() {
    return myComponent;
  }

  @Override
  public void loadState(final VisibleTreeStateComponent state) {
    myComponent.copyFrom(state);
  }

  public VisibleTreeState getVisibleTreeState(InspectionProfile profile) {
    return myComponent.getVisibleTreeState(profile);
  }
}
