/*
 * Copyright 2013 must-be.org
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

import com.intellij.openapi.components.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.WatchedRootsProvider;
import com.intellij.openapi.roots.impl.ProjectRootManagerImpl;
import com.intellij.openapi.roots.ui.LightFilePointer;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.util.containers.HashSet;
import lombok.val;
import consulo.compiler.impl.ModuleCompilerPathsManagerImpl;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.roots.ContentFolderScopes;
import consulo.roots.ContentFolderTypeProvider;

import java.util.Set;

/**
 * @author VISTALL
 * @since 13:05/10.06.13
 */
@State(
  name = "CompilerConfiguration",
  storages = {
    @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/compiler.xml", scheme = StorageScheme.DIRECTORY_BASED)
  }
)
public class CompilerConfigurationImpl extends CompilerConfiguration implements PersistentStateComponent<Element>{
  public static class MyWatchedRootsProvider implements WatchedRootsProvider {
    private final Project myProject;

    public MyWatchedRootsProvider(final Project project) {
      myProject = project;
    }

    @NotNull
    @Override
    public Set<String> getRootsToWatch() {
      return ((CompilerConfigurationImpl)CompilerConfiguration.getInstance(myProject)).getRootsToWatch();
    }
  }

  @NonNls
  private static final String DEFAULT_OUTPUT_URL = "out";
  @NonNls
  private static final String URL = "url";

  private final Project myProject;
  private final ModuleManager myModuleManager;
  private VirtualFilePointer myOutputDirPointer;
  private LocalFileSystem.WatchRequest myCompilerOutputWatchRequest;

  public CompilerConfigurationImpl(@NotNull Project project, @NotNull ModuleManager moduleManager) {
    myProject = project;
    myModuleManager = moduleManager;
  }

  @Nullable
  @Override
  public VirtualFile getCompilerOutput() {
    if(myOutputDirPointer == null) {
      VirtualFile baseDir = myProject.getBaseDir();
      if(baseDir == null) {
        return null;
      }
      VirtualFile outDir = baseDir.findFileByRelativePath(DEFAULT_OUTPUT_URL);

      return outDir == null ? null : outDir;
    }
    return myOutputDirPointer.getFile();
  }

  @NotNull
  private Set<String> getRootsToWatch() {
    final Set<String> rootsToWatch = new HashSet<String>();
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

  @Nullable
  @Override
  public String getCompilerOutputUrl() {
    if(myOutputDirPointer == null) {
      VirtualFile baseDir = myProject.getBaseDir();
      assert baseDir != null;
      VirtualFile outDir = baseDir.findFileByRelativePath(DEFAULT_OUTPUT_URL);

      return outDir == null ? myProject.getPresentableUrl() + "/" + DEFAULT_OUTPUT_URL : outDir.getUrl();
    }
    return myOutputDirPointer.getUrl();
  }

  @Override
  public VirtualFilePointer getCompilerOutputPointer() {
    if(myOutputDirPointer == null) {
      return new LightFilePointer(getCompilerOutputUrl());
    }
    return myOutputDirPointer;
  }

  @Override
  public void setCompilerOutputUrl(@Nullable String compilerOutputUrl) {
    myOutputDirPointer = VirtualFilePointerManager.getInstance().create(compilerOutputUrl, myProject, null);

    myCompilerOutputWatchRequest = LocalFileSystem.getInstance().replaceWatchedRoot(myCompilerOutputWatchRequest, compilerOutputUrl, true);
  }

  @Nullable
  @Override
  public Element getState() {
    if(myOutputDirPointer == null) {
      return null;
    }
    Element element = new Element("state");
    element.setAttribute(URL, myOutputDirPointer.getUrl());
    for (Module module : myModuleManager.getModules()) {
      val moduleCompilerPathsManager = (ModuleCompilerPathsManagerImpl)ModuleCompilerPathsManager.getInstance(module);
      Element state = moduleCompilerPathsManager.getState();
      if(state != null) {
        element.addContent(state);
      }
    }
    return element;
  }

  @Override
  public void loadState(Element element) {
    String url = element.getAttributeValue(URL);
    if(url == null) {
      return;
    }
    setCompilerOutputUrl(url);
    for (Element moduleElement : element.getChildren("module")) {
      String name = moduleElement.getAttributeValue("name");
      if(name == null) {
        continue;
      }
      Module module = myModuleManager.findModuleByName(name);
      if(module != null) {
        val moduleCompilerPathsManager = (ModuleCompilerPathsManagerImpl)ModuleCompilerPathsManager.getInstance(module);
        moduleCompilerPathsManager.loadState(moduleElement);
      }
    }
  }
}
