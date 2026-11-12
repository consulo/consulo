// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal;

import consulo.application.ReadAction;
import consulo.language.content.FileIndexFacade;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePathComparator;
import consulo.versionControlSystem.VcsDirectoryMapping;
import consulo.versionControlSystem.internal.DefaultVcsRootPolicy;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class MappingsToRoots {
  private MappingsToRoots() {
  }

  public static VirtualFile[] getRootsUnderVcs(Project project,
                                               NewMappings newMappings,
                                               AbstractVcs vcs) {
    List<VirtualFile> mappings = newMappings.getMappingsAsFilesUnderVcs(vcs);

    AbstractVcs.RootsConvertor convertor = vcs.getCustomConvertor();
    List<VirtualFile> result = convertor != null ? convertor.convertRoots(new ArrayList<>(mappings)) : mappings;

    return filterAllowedRoots(project, result, vcs);
  }

  public static VirtualFile[] filterAllowedRoots(Project project,
                                                 List<VirtualFile> mappings,
                                                 AbstractVcs vcs) {
    if (vcs.allowsNestedRoots()) {
      return VirtualFileUtil.toVirtualFileArray(mappings);
    }

    List<VirtualFile> result = new ArrayList<>(mappings);
    result.sort(FilePathComparator.getInstance());

    ReadAction.run(() -> {
      FileIndexFacade facade = FileIndexFacade.getInstance(project);
      int i = 1;
      while (i < result.size()) {
        VirtualFile previous = result.get(i - 1);
        VirtualFile current = result.get(i);
        if (facade.isValidAncestor(previous, current)) {
          result.remove(i);
        }
        else {
          i++;
        }
      }
    });
    return VirtualFileUtil.toVirtualFileArray(result);
  }

  // not only set mappings, but include all modules inside: modules might have different settings
  public static List<VirtualFile> getDetailedVcsMappings(Project project, NewMappings newMappings, AbstractVcs vcs) {
    // same as above, but no compression
    List<VirtualFile> result = new ArrayList<>(newMappings.getMappingsAsFilesUnderVcs(vcs));

    boolean addInnerModules = true;
    String vcsName = vcs.getId();
    List<VcsDirectoryMapping> directoryMappings = newMappings.getDirectoryMappings(vcsName);
    for (VcsDirectoryMapping directoryMapping : directoryMappings) {
      if (directoryMapping.isDefaultMapping()) {
        addInnerModules = false;
        break;
      }
    }

    result.sort(FilePathComparator.getInstance());
    if (addInnerModules) {
      FileIndexFacade facade = FileIndexFacade.getInstance(project);
      List<VirtualFile> modules = new ArrayList<>(DefaultVcsRootPolicy.getInstance(project).getDefaultVcsRoots());
      ReadAction.run(() -> {
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
          if (!included) {
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
