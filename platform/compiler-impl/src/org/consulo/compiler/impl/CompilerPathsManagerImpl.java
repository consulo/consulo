package org.consulo.compiler.impl;

import com.intellij.ProjectTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.ModuleAdapter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ContentFolderType;
import com.intellij.openapi.roots.WatchedRootsProvider;
import com.intellij.openapi.roots.impl.ProjectRootManagerImpl;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.HashSet;
import org.consulo.compiler.CompilerPathsManager;
import org.consulo.lombok.annotations.Logger;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 17:14/26.05.13
 */
@Logger
@State(
  name = "CompilerPathsManager",
  storages = {@Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/compiler.xml", scheme = StorageScheme.DIRECTORY_BASED)})
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

  private static class CompileInfo {
    private boolean inherit = true;
    private boolean exclude = true;

    @NotNull
    private Map<ContentFolderType, VirtualFilePointer> virtualFilePointers = new LinkedHashMap<ContentFolderType, VirtualFilePointer>();

    CompileInfo() {
      for (ContentFolderType contentFolderType : ContentFolderType.ALL_SOURCE_ROOTS) {
        virtualFilePointers.put(contentFolderType, null);
      }
    }
    //private LocalFileSystem.WatchRequest myCompilerOutputWatchRequest;
  }

  @NonNls
  private static final String OUTPUT_TAG = "output";
  @NonNls
  private static final String MODULE_OUTPUT_TAG = "module-output";
  @NonNls
  private static final String URL = "url";
  @NonNls
  private static final String NAME = "name";
  @NonNls
  private static final String TYPE = "type";
  @NonNls
  private static final String EXCLUDE = "exclude";

  @NotNull
  private final Project myProject;

  private VirtualFilePointer myProjectVirtualFilePointer;
  private CompilerPathsManager myDefaultCompilerPathsManager;

  private final Map<Module, CompileInfo> myModulesToVirtualFilePoints = new LinkedHashMap<Module, CompileInfo>();

  public CompilerPathsManagerImpl(@NotNull Project project) {
    myProject = project;
    myDefaultCompilerPathsManager = CompilerPathsManager.getInstance(ProjectManager.getInstance().getDefaultProject());

    final ModuleAdapter handler = new ModuleAdapter() {
      @Override
      public void moduleAdded(Project project, Module module) {
        createDefaultForModule(module);
      }

      @Override
      public void moduleRemoved(Project project, Module module) {
        myModulesToVirtualFilePoints.remove(module);
      }
    };

    myProject.getMessageBus().connect().subscribe(ProjectTopics.MODULES, handler);
    myProject.getMessageBus().connect().subscribe(ProjectTopics.MODIFIABLE_MODEL_FOR_MODULES, handler);

    createDefaults();
  }

  private void createDefaults() {

    myProjectVirtualFilePointer = VirtualFilePointerManager.getInstance()
      .create(myProject.getBaseDir().getUrl() + "/" + myDefaultCompilerPathsManager.getCompilerOutputUrl(), myProject, null);
    ModuleManager moduleManager = ModuleManager.getInstance(myProject);
    for (Module module : moduleManager.getModules()) {
      if (!myModulesToVirtualFilePoints.containsKey(module)) {
        createDefaultForModule(module);
      }
    }
  }

  private void createDefaultForModule(Module module) {
    CompileInfo compileInfo = new CompileInfo();
    for (Map.Entry<ContentFolderType, VirtualFilePointer> entry : compileInfo.virtualFilePointers.entrySet()) {
      entry.setValue(createDefaultPointerForModule(module, entry.getKey()));
    }
    myModulesToVirtualFilePoints.put(module, compileInfo);
  }

  @Override
  @Nullable
  public VirtualFile getCompilerOutput() {
    return myProjectVirtualFilePointer.getFile();
  }

  @Override
  @Nullable
  public String getCompilerOutputUrl() {
    return myProjectVirtualFilePointer.getUrl();
  }

  @Override
  public VirtualFilePointer getCompilerOutputPointer() {
    return myProjectVirtualFilePointer;
  }

  @Override
  public void setCompilerOutputUrl(@Nullable String compilerOutputUrl) {
    if (compilerOutputUrl == null) {
      compilerOutputUrl = myProject.getBasePath() + "/" + myDefaultCompilerPathsManager.getCompilerOutputUrl();
    }
    myProjectVirtualFilePointer = VirtualFilePointerManager.getInstance().create(compilerOutputUrl, myProject, null);
    //myCompilerOutputWatchRequest = LocalFileSystem.getInstance().replaceWatchedRoot(myCompilerOutputWatchRequest, compilerOutputUrl, true);
  }

  @Override
  public boolean isInheritedCompilerOutput(@NotNull Module module) {
    final CompileInfo compileInfo = myModulesToVirtualFilePoints.get(module);
    assert compileInfo != null;
    return compileInfo.inherit;
  }

  @Override
  public void setInheritedCompilerOutput(@NotNull Module module, boolean val) {
    final CompileInfo compileInfo = myModulesToVirtualFilePoints.get(module);
    assert compileInfo != null;
    compileInfo.inherit = val;


    for (ContentFolderType entry : compileInfo.virtualFilePointers.keySet()) {
      compileInfo.virtualFilePointers.put(entry, createDefaultPointerForModule(module, entry));
    }
  }

  private VirtualFilePointer createDefaultPointerForModule(@NotNull Module module, @NotNull ContentFolderType contentFolderType) {
    final VirtualFilePointerManager instance = VirtualFilePointerManager.getInstance();
    return instance.create(myProject.getBaseDir().getUrl() +
                           "/" +
                           myDefaultCompilerPathsManager.getCompilerOutputUrl() +
                           "/" +
                           contentFolderType.name().toLowerCase() +
                           "/" +
                           module.getName() +
                           "/", module, null);
  }

  @Override
  public boolean isExcludeOutput(@NotNull Module module) {
    final CompileInfo compileInfo = myModulesToVirtualFilePoints.get(module);
    assert compileInfo != null;
    return compileInfo.exclude;
  }

  @Override
  public void setExcludeOutput(@NotNull Module module, boolean val) {
    final CompileInfo compileInfo = myModulesToVirtualFilePoints.get(module);
    assert compileInfo != null;
    compileInfo.exclude = val;
  }

  @Override
  public void setCompilerOutputUrl(@NotNull Module module,
                                   @NotNull ContentFolderType contentFolderType,
                                   @Nullable String compilerOutputUrl) {
    final CompileInfo compileInfo = myModulesToVirtualFilePoints.get(module);
    assert compileInfo != null;

    if (compilerOutputUrl == null) {
      compileInfo.virtualFilePointers.put(contentFolderType, createDefaultPointerForModule(module, contentFolderType));
    }
    else {
      compileInfo.virtualFilePointers
        .put(contentFolderType, VirtualFilePointerManager.getInstance().create(compilerOutputUrl, myProject, null));
    }
  }

  @Override
  public String getCompilerOutputUrl(@NotNull Module module, @NotNull ContentFolderType contentFolderType) {
    final CompileInfo compileInfo = myModulesToVirtualFilePoints.get(module);
    assert compileInfo != null;
    return compileInfo.virtualFilePointers.get(contentFolderType).getUrl();
  }

  @Nullable
  @Override
  public VirtualFile getCompilerOutput(@NotNull Module module, @NotNull ContentFolderType contentFolderType) {
    final CompileInfo compileInfo = myModulesToVirtualFilePoints.get(module);
    assert compileInfo != null;
    return compileInfo.virtualFilePointers.get(contentFolderType).getFile();
  }

  @NotNull
  @Override
  public VirtualFilePointer getCompilerOutputPointer(@NotNull Module module, @NotNull ContentFolderType contentFolderType) {
    final CompileInfo compileInfo = myModulesToVirtualFilePoints.get(module);
    assert compileInfo != null;
    return compileInfo.virtualFilePointers.get(contentFolderType);
  }

  @NotNull
  private Set<String> getRootsToWatch() {
    final Set<String> rootsToWatch = new HashSet<String>();
    for (CompileInfo compileInfo : myModulesToVirtualFilePoints.values()) {
      for (VirtualFilePointer pointer : compileInfo.virtualFilePointers.values()) {
        rootsToWatch.add(ProjectRootManagerImpl.extractLocalPath(pointer.getUrl()));
      }
    }
    rootsToWatch.add(ProjectRootManagerImpl.extractLocalPath(myProjectVirtualFilePointer.getUrl()));
    return rootsToWatch;
  }

  @Nullable
  @Override
  public Element getState() {
    Element element = new Element("state");
    element.setAttribute(URL, myProjectVirtualFilePointer.getUrl());

    for (Map.Entry<Module, CompileInfo> entry : myModulesToVirtualFilePoints.entrySet()) {
      final Module key = entry.getKey();
      final CompileInfo value = entry.getValue();

      if (value.inherit) {
        continue;
      }

      Element moduleElement = new Element(MODULE_OUTPUT_TAG);
      moduleElement.setAttribute(NAME, key.getName());
      moduleElement.setAttribute(EXCLUDE, String.valueOf(value.exclude));
      for (Map.Entry<ContentFolderType, VirtualFilePointer> tempEntry : value.virtualFilePointers.entrySet()) {
        final Element elementForOutput = createElementForOutput(tempEntry.getValue());
        elementForOutput.setAttribute(TYPE, tempEntry.getKey().name());
        moduleElement.addContent(elementForOutput);
      }

      element.addContent(moduleElement);
    }
    return element;
  }

  private static Element createElementForOutput(VirtualFilePointer virtualFilePointer) {
    final Element pathElement = new Element(OUTPUT_TAG);
    pathElement.setAttribute(URL, virtualFilePointer.getUrl());
    return pathElement;
  }

  @Override
  public void loadState(Element element) {
    PathMacroManager.getInstance(myProject).expandPaths(element);

    final ModuleManager moduleManager = ModuleManager.getInstance(myProject);
    String url = element.getAttributeValue(URL);
    if (url != null) {
      for (Element child : element.getChildren()) {
        final String name = child.getAttributeValue(NAME);
        if (name == null) {
          continue;
        }
        Module module = moduleManager.findModuleByName(name);
        if (module == null) {
          continue;
        }
        CompileInfo compileInfo = new CompileInfo();
        compileInfo.inherit = false;
        compileInfo.exclude = Boolean.valueOf(child.getAttributeValue(EXCLUDE));
        for (Element child2 : child.getChildren()) {
          final String moduleUrl = child2.getAttributeValue(URL);
          final ContentFolderType type = ContentFolderType.valueOf(child2.getAttributeValue(TYPE));
          compileInfo.virtualFilePointers.put(type, VirtualFilePointerManager.getInstance().create(moduleUrl, module, null));
        }

        myModulesToVirtualFilePoints.put(module, compileInfo);
      }
    }

    setCompilerOutputUrl(url);

    for (Module module : moduleManager.getModules()) {
      if (!myModulesToVirtualFilePoints.containsKey(module)) {
        createDefaultForModule(module);
      }
    }

    createIfNotExists();
  }

  private void createIfNotExists() {
    try {
      String path = PathUtil.toPresentableUrl(getCompilerOutputUrl());
      FileUtil.createDirectory(new File(path));

      for (ContentFolderType folderType : ContentFolderType.ALL_SOURCE_ROOTS) {
        String contentRootPath = path + "/" + folderType.name().toLowerCase();
        FileUtil.createDirectory(new File(contentRootPath));
      }
    }
    catch (Exception e) {
      if (ApplicationManager.getApplication().isInternal()) {
        LOGGER.error(e);
      }
    }
  }
}
