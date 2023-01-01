/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.externalSystem.service.project.manage;

import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.Key;
import consulo.externalSystem.service.project.manage.ProjectDataService;
import consulo.externalSystem.util.DisposeAwareProjectChange;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.externalSystem.util.ExternalSystemConstants;
import consulo.externalSystem.util.Order;
import consulo.externalSystem.model.ProjectKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.project.ProjectData;
import consulo.ide.impl.idea.openapi.externalSystem.util.*;
import consulo.project.Project;
import consulo.project.internal.ProjectEx;
import consulo.ui.annotation.RequiredUIAccess;
import javax.annotation.Nonnull;

import java.util.Collection;

/**
 * @author Denis Zhdanov
 * @since 2/21/13 2:40 PM
 */
@Order(ExternalSystemConstants.BUILTIN_SERVICE_ORDER)
public class ProjectDataServiceImpl implements ProjectDataService<ProjectData, Project> {
  
  @Nonnull
  @Override
  public Key<ProjectData> getTargetDataKey() {
    return ProjectKeys.PROJECT;
  }

  @Override
  public void importData(@Nonnull Collection<DataNode<ProjectData>> toImport, @Nonnull Project project, boolean synchronous) {
    if (toImport.size() != 1) {
      throw new IllegalArgumentException(String.format("Expected to get a single project but got %d: %s", toImport.size(), toImport));
    }
    DataNode<ProjectData> node = toImport.iterator().next();
    ProjectData projectData = node.getData();
    
    if (!ExternalSystemApiUtil.isNewProjectConstruction() && !ExternalSystemUtil.isOneToOneMapping(project, node)) {
      return;
    }
    
    if (!project.getName().equals(projectData.getInternalName())) {
      renameProject(projectData.getInternalName(), projectData.getOwner(), project, synchronous);
    }
  }

  @Override
  public void removeData(@Nonnull Collection<? extends Project> toRemove, @Nonnull Project project, boolean synchronous) {
  }

  @SuppressWarnings("MethodMayBeStatic")
  public void renameProject(@Nonnull final String newName,
                            @Nonnull final ProjectSystemId externalSystemId,
                            @Nonnull final Project project,
                            boolean synchronous)
  {
    if (!(project instanceof ProjectEx) || newName.equals(project.getName())) {
      return;
    }
    ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(project) {
      @RequiredUIAccess
      @Override
      public void execute() {
        String oldName = project.getName();
        ((ProjectEx)project).setProjectName(newName);
        ExternalSystemApiUtil.getSettings(project, externalSystemId).getPublisher().onProjectRenamed(oldName, newName);
      }
    });
  }

}
