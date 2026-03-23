/*
 * Copyright 2013-2022 consulo.io
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
package consulo.versionControlSystem.impl.internal.change;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.IgnoredBeanFactory;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.change.IgnoredFileDescriptor;
import consulo.versionControlSystem.change.IgnoredFileProvider;
import consulo.versionControlSystem.change.shelf.ShelveChangesManager;
import consulo.versionControlSystem.impl.internal.change.shelf.ShelveChangesManagerImpl;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 24-Jun-22
 */
@ExtensionImpl(order = "last")
public class DefaultIgnoredFileProvider implements IgnoredFileProvider {
  private final Project myProject;

  @Inject
  public DefaultIgnoredFileProvider(Project project) {
    myProject = project;
  }

  @Override
  public boolean isIgnoredFilePath(FilePath filePath) {
    VirtualFile workspaceFile = myProject.getWorkspaceFile();
    if (workspaceFile != null && workspaceFile.getPath().equals(filePath.getPath())) {
      return true; // workspace.xml
    }

    if (isShelfDirOrInsideIt(filePath)) {
      return true; // shelf directory
    }

    return false;
  }

  private boolean isShelfDirOrInsideIt(FilePath filePath) {
    ShelveChangesManager shelveManager = myProject.getInstance(ShelveChangesManager.class);
    if (shelveManager instanceof ShelveChangesManagerImpl impl) {
      File shelfDir = impl.getShelfResourcesDirectory();
      return FileUtil.isAncestor(shelfDir.getAbsolutePath(), filePath.getPath(), false);
    }
    return false;
  }

  @Override
  public Set<IgnoredFileDescriptor> getIgnoredFiles() {
    Set<IgnoredFileDescriptor> ignored = new LinkedHashSet<>();

    ShelveChangesManager shelveManager = myProject.getInstance(ShelveChangesManager.class);
    if (shelveManager instanceof ShelveChangesManagerImpl impl) {
      File shelfDir = impl.getShelfResourcesDirectory();
      ignored.add(IgnoredBeanFactory.ignoreUnderDirectory(shelfDir.getAbsolutePath(), myProject));
    }

    VirtualFile workspaceFile = myProject.getWorkspaceFile();
    if (workspaceFile != null) {
      ignored.add(IgnoredBeanFactory.ignoreFile(workspaceFile.getPath(), myProject));
    }

    return Collections.unmodifiableSet(ignored);
  }

  @Override
  public String getIgnoredGroupDescription() {
    return VcsLocalize.ignoredGroupDescriptionDefaultIdeFiles().get();
  }
}
