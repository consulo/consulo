/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import consulo.vcs.FilePath;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 10/7/11
 * Time: 6:40 PM
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class VcsFileListenerContextHelper {
  // to ignore by listeners
  private final Set<FilePath> myDeletedContext;
  private final Set<VirtualFile> myAddContext;

  @Inject
  VcsFileListenerContextHelper(final Project project) {
    myDeletedContext = new HashSet<>();
    myAddContext = new HashSet<>();
  }

  public static VcsFileListenerContextHelper getInstance(final Project project) {
    return ServiceManager.getService(project, VcsFileListenerContextHelper.class);
  }

  public void ignoreDeleted(final FilePath filePath) {
    myDeletedContext.add(filePath);
  }

  public boolean isDeletionIgnored(final FilePath filePath) {
    return myDeletedContext.contains(filePath);
  }

  public void ignoreAdded(final VirtualFile virtualFile) {
    myAddContext.add(virtualFile);
  }

  public boolean isAdditionIgnored(final VirtualFile virtualFile) {
    return myAddContext.contains(virtualFile);
  }

  public void possiblySwitchActivation(final boolean isActive) {
    /*if (myActive != isActive) {
      final CommandProcessor cp = CommandProcessor.getInstance();
      if (isActive) {
        cp.addCommandListener(this);
      } else {
        cp.removeCommandListener(this);
      }
    }
    myActive = isActive;*/
  }

  public void clearContext() {
    myAddContext.clear();
    myDeletedContext.clear();
  }
}
