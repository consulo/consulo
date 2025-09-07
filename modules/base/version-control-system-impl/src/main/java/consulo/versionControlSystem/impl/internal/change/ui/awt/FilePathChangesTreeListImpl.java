/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change.ui.awt;

import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.tree.DefaultTreeModel;
import java.util.List;

public class FilePathChangesTreeListImpl extends ChangesTreeListImpl<FilePath> {

  public FilePathChangesTreeListImpl(@Nonnull Project project, @Nonnull List<FilePath> originalFiles,
                                     boolean showCheckboxes, boolean highlightProblems,
                                     @Nullable Runnable inclusionListener, @Nullable ChangeNodeDecorator nodeDecorator) {
    super(project, originalFiles, showCheckboxes, highlightProblems, inclusionListener, nodeDecorator);
  }

  @Override
  protected DefaultTreeModel buildTreeModel(List<FilePath> changes, ChangeNodeDecorator changeNodeDecorator) {
    return TreeModelBuilder.buildFromFilePaths(myProject, isShowFlatten(), changes);
  }

  @Override
  protected List<FilePath> getSelectedObjects(ChangesBrowserNode<FilePath> node) {
    return node.getAllFilePathsUnder();
  }

  @Override
  @Nullable
  protected FilePath getLeadSelectedObject(ChangesBrowserNode node) {
    Object userObject = node.getUserObject();
    return userObject instanceof FilePath ? (FilePath)userObject : null;
  }
}
