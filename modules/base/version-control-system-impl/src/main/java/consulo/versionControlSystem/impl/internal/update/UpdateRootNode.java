/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.update;

import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.versionControlSystem.update.ActionInfo;
import consulo.versionControlSystem.update.FileGroup;
import consulo.versionControlSystem.update.UpdatedFiles;

import java.util.Collections;
import java.util.List;

public class UpdateRootNode extends GroupTreeNode {

  private final Project myProject;

  public UpdateRootNode(UpdatedFiles updatedFiles, Project project, String rootName, ActionInfo actionInfo) {
    super(rootName, false, SimpleTextAttributes.ERROR_ATTRIBUTES, project, Collections.<String, String>emptyMap(), null);
    myProject = project;

    addGroupsToNode(updatedFiles.getTopLevelGroups(), this, actionInfo);
  }

  private void addGroupsToNode(List<FileGroup> groups, AbstractTreeNode owner, ActionInfo actionInfo) {
    for (FileGroup fileGroup : groups) {
      GroupTreeNode node = addFileGroup(fileGroup, owner, actionInfo);
      if (node != null) {
        addGroupsToNode(fileGroup.getChildren(), node, actionInfo);
      }
    }
  }

  @jakarta.annotation.Nullable
  private GroupTreeNode addFileGroup(FileGroup fileGroup, AbstractTreeNode parent, ActionInfo actionInfo) {
    if (fileGroup.isEmpty()) {
      return null;
    }
    GroupTreeNode group = new GroupTreeNode(actionInfo.getGroupName(fileGroup), fileGroup.getSupportsDeletion(),
                                            fileGroup.getInvalidAttributes(), myProject, fileGroup.getErrorsMap(), fileGroup.getId());
    Disposer.register(this, group);
    parent.add(group);
    for (String s : fileGroup.getFiles()) {
      group.addFilePath(s);
    }
    return group;
  }

  @Override
  public boolean getSupportsDeletion() {
    return false;
  }
}
