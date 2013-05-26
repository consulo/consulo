package org.consulo.compiler.impl;

import com.intellij.openapi.components.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentFolderType;
import com.intellij.openapi.roots.WatchedRootsProvider;
import com.intellij.openapi.roots.impl.ProjectRootManagerImpl;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.util.containers.HashSet;
import org.consulo.compiler.CompilerPathsManager;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 17:14/26.05.13
 */
@State(
  name = "CompilerPathsManager",
  storages = {@Storage(file = StoragePathMacros.PROJECT_FILE),
    @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/compiler.xml", scheme = StorageScheme.DIRECTORY_BASED)})
public class CompilerPathsManagerImpl extends CompilerPathsManager implements PersistentStateComponent<Element> {
  public static class MyWatchedRootsProvider implements WatchedRootsProvider {
    private final Project myProject;

    public MyWatchedRootsProvider(final Project project) {
      myProject = project;
    }

    @NotNull
    @Override
    public Set<String> getRootsToWatch() {
      return ((CompilerPathsManagerImpl)CompilerPathsManager.getInstance(myProject)).getRootsToWatch();
    }
  }

  @NonNls
  private static final String OUTPUT_TAG = "output";
  @NonNls
  private static final String URL = "url";

  @NotNull
  private final Project myProject;
  private VirtualFilePointer myCompilerOutput;
  private LocalFileSystem.WatchRequest myCompilerOutputWatchRequest;

  private Map<Module, VirtualFilePointer> myModulesToVirtualFilePoints = new HashMap<Module, VirtualFilePointer>();

  public CompilerPathsManagerImpl(@NotNull Project project) {
    myProject = project;
    myCompilerOutput = VirtualFilePointerManager.getInstance()
      .create(PathMacroManager.getInstance(project).expandPath("file://$PROJECT_DIR$/out/"), myProject, null);
  }

  @Override
  @Nullable
  public VirtualFile getCompilerOutput() {
    if (myCompilerOutput == null) return null;
    return myCompilerOutput.getFile();
  }

  @Override
  @Nullable
  public String getCompilerOutputUrl() {
    if (myCompilerOutput == null) return null;
    return myCompilerOutput.getUrl();
  }

  @Override
  public VirtualFilePointer getCompilerOutputPointer() {
    return myCompilerOutput;
  }

  @Override
  public void setCompilerOutputPointer(VirtualFilePointer pointer) {
    myCompilerOutput = pointer;
  }

  @Override
  public void setCompilerOutputUrl(String compilerOutputUrl) {
    VirtualFilePointer pointer = VirtualFilePointerManager.getInstance().create(compilerOutputUrl, myProject, null);
    setCompilerOutputPointer(pointer);
    myCompilerOutputWatchRequest = LocalFileSystem.getInstance().replaceWatchedRoot(myCompilerOutputWatchRequest, compilerOutputUrl, true);
  }

  @Override
  public String getCompilerOutputUrl(@NotNull Module module, @NotNull ContentFolderType contentFolderType) {
    final VirtualFilePointer filePointer = getOrCreateVirtualFilePointer(module, contentFolderType);
    return filePointer.getUrl();
  }

  @Nullable
  @Override
  public VirtualFile getCompilerOutput(@NotNull Module module, @NotNull ContentFolderType contentFolderType) {
    final VirtualFilePointer filePointer = getOrCreateVirtualFilePointer(module, contentFolderType);
    return filePointer.getFile();
  }

  private VirtualFilePointer getOrCreateVirtualFilePointer(Module module, ContentFolderType contentFolderType) {
    final VirtualFilePointer virtualFilePointer = myModulesToVirtualFilePoints.get(module);
    if (virtualFilePointer == null) {
      final VirtualFilePointer temp = VirtualFilePointerManager.getInstance()
        .create(PathMacroManager.getInstance(myProject).expandPath("file://$PROJECT_DIR$/out/" + contentFolderType.name().toLowerCase() + "/" + module.getName()), myProject, null);
      myModulesToVirtualFilePoints.put(module, temp);
      return temp;
    }
    else {
      return virtualFilePointer;
    }
  }
 /* @Override
  public void readExternal(Element element) throws InvalidDataException {
    final Element outputPathChild = element.getChild(OUTPUT_TAG);
    if (outputPathChild != null) {
      String outputPath = outputPathChild.getAttributeValue(URL);
      assert myCompilerOutput == null;
      myCompilerOutput = VirtualFilePointerManager.getInstance().create(outputPath, myProject, null);
    }
    else {
      myCompilerOutput = VirtualFilePointerManager.getInstance().create("file://$PROJECT_DIR$/out", myProject, null);
    }
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    if (myCompilerOutput != null) {
      final Element pathElement = new Element(OUTPUT_TAG);
      pathElement.setAttribute(URL, myCompilerOutput.getUrl());
      element.addContent(pathElement);
    }
  }   */

  @NotNull
  private Set<String> getRootsToWatch() {
    final Set<String> rootsToWatch = new HashSet<String>();
    Module[] modules = ModuleManager.getInstance(myProject).getModules();
    for (Module module : modules) {
      VirtualFilePointer temp = getOrCreateVirtualFilePointer(module, ContentFolderType.SOURCE);
      rootsToWatch.add(ProjectRootManagerImpl.extractLocalPath(temp.getUrl()));

      temp = getOrCreateVirtualFilePointer(module, ContentFolderType.TEST);
      rootsToWatch.add(ProjectRootManagerImpl.extractLocalPath(temp.getUrl()));
    }

    if (myCompilerOutput != null) {
      final String url = myCompilerOutput.getUrl();
      rootsToWatch.add(ProjectRootManagerImpl.extractLocalPath(url));
    }
    return rootsToWatch;
  }

  @Nullable
  @Override
  public Element getState() {
    return new Element("test");
  }

  @Override
  public void loadState(Element state) {

  }
}
