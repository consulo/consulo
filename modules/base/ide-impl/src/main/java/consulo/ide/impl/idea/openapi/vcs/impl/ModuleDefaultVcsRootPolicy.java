/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.ide.impl.idea.openapi.vcs.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.ServiceManager;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.language.util.ModuleUtilCore;
import consulo.project.Project;
import consulo.language.content.FileIndexFacade;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectRootManager;
import consulo.application.util.registry.Registry;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsDirectoryMapping;
import consulo.ide.impl.idea.openapi.vcs.ex.ProjectLevelVcsManagerEx;
import consulo.ide.impl.idea.openapi.vcs.impl.projectlevelman.NewMappings;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.project.ProjectCoreUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.application.AccessRule;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static consulo.ide.impl.idea.util.containers.ContainerUtil.newHashSet;

/**
 * @author yole
 */
@Singleton
@ServiceImpl
public class ModuleDefaultVcsRootPolicy extends DefaultVcsRootPolicy {
  private static final Logger LOG = Logger.getInstance(ModuleDefaultVcsRootPolicy.class);
  private final Project myProject;
  private final VirtualFile myBaseDir;
  private final ModuleManager myModuleManager;

  @Inject
  public ModuleDefaultVcsRootPolicy(final Project project) {
    myProject = project;
    myBaseDir = project.getBaseDir();
    myModuleManager = ModuleManager.getInstance(myProject);
  }

  @Override
  @Nonnull
  public Collection<VirtualFile> getDefaultVcsRoots(@Nonnull NewMappings mappingList, @Nonnull String vcsName) {
    Set<VirtualFile> result = newHashSet();
    final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(myProject);
    if (myBaseDir != null && vcsName.equals(mappingList.getVcsFor(myBaseDir))) {
      final AbstractVcs vcsFor = vcsManager.getVcsFor(myBaseDir);
      if (vcsFor != null && vcsName.equals(vcsFor.getName())) {
        result.add(myBaseDir);
      }
    }
    if (ProjectCoreUtil.isDirectoryBased(myProject) && myBaseDir != null) {
      final VirtualFile ideaDir = ProjectCoreUtil.getDirectoryStoreFile(myProject);
      if (ideaDir != null) {
        final AbstractVcs vcsFor = vcsManager.getVcsFor(ideaDir);
        if (vcsFor != null && vcsName.equals(vcsFor.getName())) {
          result.add(ideaDir);
        }
      }
    }
    // assertion for read access inside
    Module[] modules = AccessRule.read(myModuleManager::getModules);
    for (Module module : modules) {
      final VirtualFile[] files = ModuleRootManager.getInstance(module).getContentRoots();
      for (VirtualFile file : files) {
        // if we're currently processing moduleAdded notification, getModuleForFile() will return null, so we pass the module
        // explicitly (we know it anyway)
        VcsDirectoryMapping mapping = mappingList.getMappingFor(file, module);
        final String mappingVcs = mapping != null ? mapping.getVcs() : null;
        if (vcsName.equals(mappingVcs) && file.isDirectory()) {
          result.add(file);
        }
      }
    }
    return result;
  }

  @Override
  public boolean matchesDefaultMapping(@Nonnull final VirtualFile file, final Object matchContext) {
    if (matchContext != null) {
      return true;
    }
    return myBaseDir != null && VfsUtilCore.isAncestor(myBaseDir, file, false);
  }

  @Override
  @javax.annotation.Nullable
  public Object getMatchContext(final VirtualFile file) {
    return ModuleUtilCore.findModuleForFile(file, myProject);
  }

  @Override
  @javax.annotation.Nullable
  public VirtualFile getVcsRootFor(@Nonnull VirtualFile file) {
    FileIndexFacade indexFacade = ServiceManager.getService(myProject, FileIndexFacade.class);
    if (myBaseDir != null && indexFacade.isValidAncestor(myBaseDir, file)) {
      LOG.debug("File " + file + " is under project base dir " + myBaseDir);
      return myBaseDir;
    }
    VirtualFile contentRoot = ProjectRootManager.getInstance(myProject).getFileIndex().getContentRootForFile(file, Registry.is("ide.hide.excluded.files"));
    if (contentRoot != null) {
      LOG.debug("Content root for file " + file + " is " + contentRoot);
      if (contentRoot.isDirectory()) {
        return contentRoot;
      }
      VirtualFile parent = contentRoot.getParent();
      LOG.debug("Content root is not a directory, using its parent " + parent);
      return parent;
    }
    if (ProjectCoreUtil.isDirectoryBased(myProject)) {
      VirtualFile ideaDir = ProjectCoreUtil.getDirectoryStoreFile(myProject);
      if (ideaDir != null && VfsUtilCore.isAncestor(ideaDir, file, false)) {
        LOG.debug("File " + file + " is under .idea");
        return ideaDir;
      }
    }
    LOG.debug("Couldn't find proper root for " + file);
    return null;
  }

  @Nonnull
  @Override
  public Collection<VirtualFile> getDirtyRoots() {
    Collection<VirtualFile> dirtyRoots = newHashSet();

    if (ProjectCoreUtil.isDirectoryBased(myProject)) {
      VirtualFile ideaDir = ProjectCoreUtil.getDirectoryStoreFile(myProject);
      if (ideaDir != null) {
        dirtyRoots.add(ideaDir);
      }
      else {
        LOG.warn(".idea was not found for base dir [" + myBaseDir.getPath() + "]");
      }
    }

    ContainerUtil.addAll(dirtyRoots, getContentRoots());

    String defaultMapping = ((ProjectLevelVcsManagerEx)ProjectLevelVcsManager.getInstance(myProject)).haveDefaultMapping();
    boolean haveDefaultMapping = !StringUtil.isEmpty(defaultMapping);
    if (haveDefaultMapping && myBaseDir != null) {
      dirtyRoots.add(myBaseDir);
    }
    return dirtyRoots;
  }

  @Nonnull
  private Collection<VirtualFile> getContentRoots() {
    Module[] modules = AccessRule.read(myModuleManager::getModules);
    return Arrays.stream(modules)
            .map(module -> ModuleRootManager.getInstance(module).getContentRoots())
            .flatMap(Arrays::stream)
            .collect(Collectors.toSet());
  }
}
