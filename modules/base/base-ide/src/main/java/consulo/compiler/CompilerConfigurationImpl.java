/*
 * Copyright 2013-2016 consulo.io
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
package consulo.compiler;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.WatchedRootsProvider;
import com.intellij.openapi.roots.impl.ProjectRootManagerImpl;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.impl.LightFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.util.io.URLUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.AccessRule;
import consulo.compiler.impl.ModuleCompilerPathsManagerImpl;
import consulo.roots.ContentFolderScopes;
import consulo.roots.ContentFolderTypeProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 13:05/10.06.13
 */
@Singleton
public class CompilerConfigurationImpl extends CompilerConfiguration {
  public static class MyWatchedRootsProvider implements WatchedRootsProvider {
    private final Project myProject;

    @Inject
    public MyWatchedRootsProvider(final Project project) {
      myProject = project;
    }

    @Nonnull
    @Override
    public Set<String> getRootsToWatch() {
      return ((CompilerConfigurationImpl)CompilerConfiguration.getInstance(myProject)).getRootsToWatch();
    }
  }

  private static final String DEFAULT_OUTPUT_URL = "out";
  private static final String URL = "url";

  private final Project myProject;
  private final ModuleManager myModuleManager;
  private VirtualFilePointer myOutputDirPointer;
  private LocalFileSystem.WatchRequest myCompilerOutputWatchRequest;

  @Inject
  public CompilerConfigurationImpl(@Nonnull Project project, @Nonnull ModuleManager moduleManager) {
    myProject = project;
    myModuleManager = moduleManager;
  }

  @Nullable
  @Override
  public VirtualFile getCompilerOutput() {
    if (myOutputDirPointer == null) {
      VirtualFile baseDir = myProject.getBaseDir();
      if (baseDir == null) {
        return null;
      }
      VirtualFile outDir = baseDir.findFileByRelativePath(DEFAULT_OUTPUT_URL);

      return outDir == null ? null : outDir;
    }
    return myOutputDirPointer.getFile();
  }

  @Nonnull
  @RequiredReadAction
  private Set<String> getRootsToWatch() {
    final Set<String> rootsToWatch = new HashSet<>();
    ModuleManager moduleManager = ModuleManager.getInstance(myProject);
    for (Module module : moduleManager.getModules()) {
      ModuleCompilerPathsManager moduleCompilerPathsManager = ModuleCompilerPathsManager.getInstance(module);

      for (ContentFolderTypeProvider folderType : ContentFolderTypeProvider.filter(ContentFolderScopes.all(false))) {
        String compilerOutputUrl = moduleCompilerPathsManager.getCompilerOutputUrl(folderType);
        assert compilerOutputUrl != null : module.getName() + ":" + folderType + " url is null";
        rootsToWatch.add(ProjectRootManagerImpl.extractLocalPath(compilerOutputUrl));
      }
    }

    rootsToWatch.add(ProjectRootManagerImpl.extractLocalPath(getCompilerOutputUrl()));
    return rootsToWatch;
  }

  @Nonnull
  @Override
  public String getCompilerOutputUrl() {
    if (myOutputDirPointer == null) {
      return VirtualFileManager.constructUrl(URLUtil.FILE_PROTOCOL, FileUtil.toSystemIndependentName(myProject.getBasePath()) + "/" + DEFAULT_OUTPUT_URL);
    }
    return myOutputDirPointer.getUrl();
  }

  @Override
  public VirtualFilePointer getCompilerOutputPointer() {
    if (myOutputDirPointer == null) {
      return new LightFilePointer(getCompilerOutputUrl());
    }
    return myOutputDirPointer;
  }

  @Override
  public void setCompilerOutputUrl(@Nullable String compilerOutputUrl) {
    myOutputDirPointer = compilerOutputUrl == null ? null : VirtualFilePointerManager.getInstance().create(compilerOutputUrl, myProject, null);

    myCompilerOutputWatchRequest =
            compilerOutputUrl == null ? null : LocalFileSystem.getInstance().replaceWatchedRoot(myCompilerOutputWatchRequest, ProjectRootManagerImpl.extractLocalPath(compilerOutputUrl), true);
  }

  @RequiredReadAction
  public void getState(Element stateElement) {
    if (myOutputDirPointer != null) {
      stateElement.setAttribute(URL, myOutputDirPointer.getUrl());
    }

    for (Module module : myModuleManager.getModules()) {
      ModuleCompilerPathsManagerImpl moduleCompilerPathsManager = (ModuleCompilerPathsManagerImpl)ModuleCompilerPathsManager.getInstance(module);
      Element state = moduleCompilerPathsManager.getState();
      if (state != null) {
        stateElement.addContent(state);
      }
    }
  }

  public void loadState(Element element) {
    String url = element.getAttributeValue(URL);
    if (url != null) {
      setCompilerOutputUrl(url);
    }

    for (Element moduleElement : element.getChildren("module")) {
      String name = moduleElement.getAttributeValue("name");
      if (name == null) {
        continue;
      }
      Module module = AccessRule.read(() -> myModuleManager.findModuleByName(name));
      if (module != null) {
        ModuleCompilerPathsManagerImpl moduleCompilerPathsManager = (ModuleCompilerPathsManagerImpl)ModuleCompilerPathsManager.getInstance(module);
        moduleCompilerPathsManager.loadState(moduleElement);
      }
    }
  }
}
