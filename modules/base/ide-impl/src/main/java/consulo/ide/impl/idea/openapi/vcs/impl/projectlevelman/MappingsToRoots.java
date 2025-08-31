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
package consulo.ide.impl.idea.openapi.vcs.impl.projectlevelman;

import consulo.application.ApplicationManager;
import consulo.ide.ServiceManager;
import consulo.versionControlSystem.FilePathComparator;
import consulo.project.Project;
import consulo.language.content.FileIndexFacade;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.VcsDirectoryMapping;
import consulo.ide.impl.idea.openapi.vcs.impl.DefaultVcsRootPolicy;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MappingsToRoots {
  private final NewMappings myMappings;
  private final Project myProject;

  public MappingsToRoots(NewMappings mappings, Project project) {
    myMappings = mappings;
    myProject = project;
  }

  @Nonnull
  public VirtualFile[] getRootsUnderVcs(@Nonnull AbstractVcs vcs) {
    List<VirtualFile> result = myMappings.getMappingsAsFilesUnderVcs(vcs);

    AbstractVcs.RootsConvertor convertor = vcs.getCustomConvertor();
    if (convertor != null) {
      result = convertor.convertRoots(result);
    }

    Collections.sort(result, FilePathComparator.getInstance());
    if (! vcs.allowsNestedRoots()) {
      FileIndexFacade facade = ServiceManager.getService(myProject, FileIndexFacade.class);
      List<VirtualFile> finalResult = result;
      ApplicationManager.getApplication().runReadAction(() -> {
        int i=1;
        while(i < finalResult.size()) {
          VirtualFile previous = finalResult.get(i - 1);
          VirtualFile current = finalResult.get(i);
          if (facade.isValidAncestor(previous, current)) {
            finalResult.remove(i);
          }
          else {
            i++;
          }
        }
      });
    }
    result.removeIf(file -> !file.isDirectory());
    return VfsUtilCore.toVirtualFileArray(result);
  }

  // not only set mappings, but include all modules inside: modules might have different settings
  public List<VirtualFile> getDetailedVcsMappings(AbstractVcs vcs) {
    // same as above, but no compression
    List<VirtualFile> result = myMappings.getMappingsAsFilesUnderVcs(vcs);

    boolean addInnerModules = true;
    String vcsName = vcs.getName();
    List<VcsDirectoryMapping> directoryMappings = myMappings.getDirectoryMappings(vcsName);
    for (VcsDirectoryMapping directoryMapping : directoryMappings) {
      if (directoryMapping.isDefaultMapping()) {
        addInnerModules = false;
        break;
      }
    }

    Collections.sort(result, FilePathComparator.getInstance());
    if (addInnerModules) {
      FileIndexFacade facade = ServiceManager.getService(myProject, FileIndexFacade.class);
      Collection<VirtualFile> modules = DefaultVcsRootPolicy.getInstance(myProject).getDefaultVcsRoots(myMappings, vcsName);
      ApplicationManager.getApplication().runReadAction(() -> {
        Iterator<VirtualFile> iterator = modules.iterator();
        while (iterator.hasNext()) {
          VirtualFile module = iterator.next();
          boolean included = false;
          for (VirtualFile root : result) {
            if (facade.isValidAncestor(root, module)) {
              included = true;
              break;
            }
          }
          if (! included) {
            iterator.remove();
          }
        }
      });
      result.addAll(modules);
    }
    result.removeIf(file -> !file.isDirectory());
    return result;
  }
}
