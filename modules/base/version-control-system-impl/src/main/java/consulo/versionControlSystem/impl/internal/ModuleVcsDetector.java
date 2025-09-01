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

package consulo.versionControlSystem.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.component.messagebus.MessageBus;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.module.event.ModuleListener;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.ui.ex.awt.Messages;
import consulo.util.io.FileUtil;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.VcsDirectoryMapping;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.util.*;

/**
 * @author yole
 */
@Singleton
@ServiceAPI(value = ComponentScope.PROJECT)
@ServiceImpl
public class ModuleVcsDetector implements Disposable {
  private final Project myProject;
  private final MessageBus myMessageBus;
  private final Provider<ProjectLevelVcsManager> myVcsManager;
  private MessageBusConnection myConnection;

  @Inject
  public ModuleVcsDetector(Project project, Provider<ProjectLevelVcsManager> vcsManager) {
    myProject = project;
    myMessageBus = project.getMessageBus();
    myVcsManager = vcsManager;
  }

  public void startDetecting() {
    myConnection = myMessageBus.connect();
    MyModulesListener listener = new MyModulesListener();
    myConnection.subscribe(ModuleListener.class, listener);
    myConnection.subscribe(ModuleRootListener.class, listener);
  }

  private class MyModulesListener implements ModuleListener, ModuleRootListener {
    private final List<Pair<String, VcsDirectoryMapping>> myMappingsForRemovedModules = new ArrayList<>();

    @Override
    public void beforeRootsChange(ModuleRootEvent event) {
      myMappingsForRemovedModules.clear();
    }

    @Override
    public void rootsChanged(ModuleRootEvent event) {
      for (Pair<String, VcsDirectoryMapping> mapping : myMappingsForRemovedModules) {
        promptRemoveMapping(mapping.first, mapping.second);
      }

      ProjectLevelVcsManagerImpl vcsManager = (ProjectLevelVcsManagerImpl)myVcsManager.get();

      // the check calculates to true only before user has done any change to mappings, i.e. in case modules are detected/added automatically
      // on start etc (look inside)
      if (vcsManager.needAutodetectMappings()) {
        autoDetectVcsMappings(false);
      }
    }

    @Override
    public void moduleAdded(Project project, Module module) {
      myMappingsForRemovedModules.removeAll(getMappings(module));
      autoDetectModuleVcsMapping(module);
    }

    @Override
    public void beforeModuleRemoved(Project project, Module module) {
      myMappingsForRemovedModules.addAll(getMappings(module));
    }
  }

  @Override
  public void dispose() {
    if (myConnection != null) {
      myConnection.disconnect();
    }
  }

  private void autoDetectVcsMappings(boolean tryMapPieces) {
    ProjectLevelVcsManagerImpl vcsManager = (ProjectLevelVcsManagerImpl)myVcsManager.get();

    Set<AbstractVcs> usedVcses = new HashSet<>();
    Map<VirtualFile, AbstractVcs> vcsMap = new HashMap<>();
    ModuleManager moduleManager = ModuleManager.getInstance(myProject);
    for (Module module : moduleManager.getModules()) {
      VirtualFile[] files = ModuleRootManager.getInstance(module).getContentRoots();
      for (VirtualFile file : files) {
        AbstractVcs contentRootVcs = vcsManager.findVersioningVcs(file);
        if (contentRootVcs != null) {
          vcsMap.put(file, contentRootVcs);
        }
        usedVcses.add(contentRootVcs);
      }
    }
    if (usedVcses.size() == 1) {
      // todo I doubt this is correct, see IDEA-50527
      AbstractVcs[] abstractVcses = usedVcses.toArray(new AbstractVcs[1]);
      Module[] modules = moduleManager.getModules();
      Set<String> contentRoots = new HashSet<>();
      for (Module module : modules) {
        VirtualFile[] roots = ModuleRootManager.getInstance(module).getContentRoots();
        for (VirtualFile root : roots) {
          contentRoots.add(root.getPath());
        }
      }

      if (abstractVcses[0] != null) {
        List<VcsDirectoryMapping> vcsDirectoryMappings = new ArrayList<>(vcsManager.getDirectoryMappings());
        for (Iterator<VcsDirectoryMapping> iterator = vcsDirectoryMappings.iterator(); iterator.hasNext(); ) {
          VcsDirectoryMapping mapping = iterator.next();
          if (!contentRoots.contains(mapping.getDirectory())) {
            iterator.remove();
          }
        }
        vcsManager.setAutoDirectoryMapping("", abstractVcses[0].getId());
        for (VcsDirectoryMapping mapping : vcsDirectoryMappings) {
          vcsManager.removeDirectoryMapping(mapping);
        }
        vcsManager.cleanupMappings();
      }
    }
    else if (tryMapPieces) {
      for (Map.Entry<VirtualFile, AbstractVcs> entry : vcsMap.entrySet()) {
        vcsManager.setAutoDirectoryMapping(entry.getKey().getPath(), entry.getValue() == null ? "" : entry.getValue().getId());
      }
      vcsManager.cleanupMappings();
    }
  }

  private void autoDetectModuleVcsMapping(Module module) {
    ProjectLevelVcsManagerImpl vcsManager = (ProjectLevelVcsManagerImpl)myVcsManager.get();

    boolean mappingsUpdated = false;
    VirtualFile[] files = ModuleRootManager.getInstance(module).getContentRoots();
    for (VirtualFile file : files) {
      AbstractVcs vcs = vcsManager.findVersioningVcs(file);
      if (vcs != null && vcs != vcsManager.getVcsFor(file)) {
        vcsManager.setAutoDirectoryMapping(file.getPath(), vcs.getId());
        mappingsUpdated = true;
      }
    }
    if (mappingsUpdated) {
      vcsManager.cleanupMappings();
    }
  }

  private List<Pair<String, VcsDirectoryMapping>> getMappings(Module module) {
    ProjectLevelVcsManagerImpl vcsManager = (ProjectLevelVcsManagerImpl)myVcsManager.get();

    List<Pair<String, VcsDirectoryMapping>> result = new ArrayList<>();
    VirtualFile[] files = ModuleRootManager.getInstance(module).getContentRoots();
    String moduleName = module.getName();
    for (VirtualFile file : files) {
      for (VcsDirectoryMapping mapping : vcsManager.getDirectoryMappings()) {
        if (FileUtil.toSystemIndependentName(mapping.getDirectory()).equals(file.getPath())) {
          result.add(new Pair<>(moduleName, mapping));
          break;
        }
      }
    }
    return result;
  }

  private void promptRemoveMapping(final String moduleName, final VcsDirectoryMapping mapping) {
    ProjectLevelVcsManagerImpl vcsManager = (ProjectLevelVcsManagerImpl)myVcsManager.get();

    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        if (myProject.isDisposed()) return;
        String msg = VcsBundle.message("vcs.root.remove.prompt", FileUtil.toSystemDependentName(mapping.getDirectory()), moduleName);
        int rc = Messages.showYesNoDialog(myProject, msg, VcsBundle.message("vcs.root.remove.title"), Messages.getQuestionIcon());
        if (rc == Messages.YES) {
          vcsManager.removeDirectoryMapping(mapping);
        }
      }
    }, ModalityState.nonModal());
  }
}
