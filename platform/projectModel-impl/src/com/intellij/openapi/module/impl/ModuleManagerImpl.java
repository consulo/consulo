/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package com.intellij.openapi.module.impl;

import com.intellij.ProjectTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.StateStorageException;
import com.intellij.openapi.module.*;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ex.ProjectRootManagerEx;
import com.intellij.openapi.roots.impl.ModifiableModelCommitter;
import com.intellij.openapi.roots.impl.ModuleRootManagerImpl;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.containers.HashMap;
import com.intellij.util.containers.HashSet;
import com.intellij.util.containers.StringInterner;
import com.intellij.util.graph.CachingSemiGraph;
import com.intellij.util.graph.DFSTBuilder;
import com.intellij.util.graph.Graph;
import com.intellij.util.graph.GraphGenerator;
import com.intellij.util.messages.MessageBus;
import gnu.trove.THashMap;
import gnu.trove.TObjectHashingStrategy;
import org.consulo.lombok.annotations.Logger;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredWriteAction;

import java.io.IOException;
import java.util.*;

/**
 * @author max
 */
@Logger
public abstract class ModuleManagerImpl extends ModuleManager implements ProjectComponent, PersistentStateComponent<Element>, ModificationTracker {
  public static class ModuleLoadItem {
    private final String myDirUrl;
    private final String myName;
    private final String[] myGroups;
    private final Element myElement;

    public ModuleLoadItem(String name, String dirUrl, Element element) {
      myDirUrl = dirUrl;
      myElement = element;

      if(name.contains(MODULE_GROUP_SEPARATOR)) {
        final String[] split = name.split(MODULE_GROUP_SEPARATOR);
        myName = split[split.length - 1];
        myGroups = new String[split.length - 1];
        System.arraycopy(split, 0, myGroups, 0, myGroups.length);
      }
      else {
        myName = name;
        myGroups = null;
      }
    }

    public String[] getGroups() {
      return myGroups;
    }

    public String getDirUrl() {
      return myDirUrl;
    }

    public String getName() {
      return myName;
    }

    public Element getElement() {
      return myElement;
    }
  }

  private static class ModuleGroupInterner {
    private final StringInterner groups = new StringInterner();
    private final Map<String[], String[]> paths = new THashMap<String[], String[]>(new TObjectHashingStrategy<String[]>() {
      @Override
      public int computeHashCode(String[] object) {
        return Arrays.hashCode(object);
      }

      @Override
      public boolean equals(String[] o1, String[] o2) {
        return Arrays.equals(o1, o2);
      }
    });

    private void setModuleGroupPath(ModifiableModuleModel model, Module module, String[] group) {
      String[] cached = paths.get(group);
      if (cached == null) {
        cached = new String[group.length];
        for (int i = 0; i < group.length; i++) {
          String g = group[i];
          cached[i] = groups.intern(g);
        }
        paths.put(cached, cached);
      }
      model.setModuleGroupPath(module, cached);
    }
  }

  public static final Key<String> DISPOSED_MODULE_NAME = Key.create("DisposedNeverAddedModuleName");
  protected final Project myProject;
  protected final MessageBus myMessageBus;
  protected volatile ModuleModelImpl myModuleModel = new ModuleModelImpl();

  @NonNls
  public static final String COMPONENT_NAME = "ModuleManager";
  private static final String MODULE_GROUP_SEPARATOR = "/";

  private final List<ModuleLoadItem> myFailedModulePaths = new ArrayList<ModuleLoadItem>();

  private List<ModuleLoadItem> myModuleLoadItems = Collections.emptyList();
  @NonNls
  public static final String ELEMENT_MODULES = "modules";
  @NonNls
  public static final String ELEMENT_MODULE = "module";

  @NonNls
  private static final String ATTRIBUTE_DIRURL = "dirurl";
  @NonNls
  private static final String ATTRIBUTE_NAME = "name";

  private long myModificationCount;

  public static ModuleManagerImpl getInstanceImpl(Project project) {
    return (ModuleManagerImpl)getInstance(project);
  }

  protected void cleanCachedStuff() {
    myCachedModuleComparator = null;
    myCachedSortedModules = null;
  }

  public ModuleManagerImpl(Project project, MessageBus messageBus) {
    myProject = project;
    myMessageBus = messageBus;
  }


  @Override
  @NotNull
  public String getComponentName() {
    return COMPONENT_NAME;
  }

  @Override
  public void initComponent() {
  }

  @Override
  public void disposeComponent() {
    myModuleModel.disposeModel();
  }

  @Override
  public long getModificationCount() {
    return myModificationCount;
  }

  @Override
  public Element getState() {
    final Element e = new Element("state");
    getState0(e);
    return e;
  }

  @Override
  public void loadState(Element state) {
    List<ModuleLoadItem> prevPaths = myModuleLoadItems;
    loadState0(state);
    if (!prevPaths.isEmpty()) {
      ModifiableModuleModel model = getModifiableModel();

      Module[] existingModules = model.getModules();

      ModuleGroupInterner groupInterner = new ModuleGroupInterner();
      for (Module existingModule : existingModules) {
        ModuleLoadItem correspondingPath = findCorrespondingPath(existingModule);
        if (correspondingPath == null) {
          model.disposeModule(existingModule);
        }
        else {
          myModuleLoadItems.remove(correspondingPath);

          String[] group =  correspondingPath.getGroups() != null ?  correspondingPath.getGroups() : null;
          if (!Arrays.equals(group, model.getModuleGroupPath(existingModule))) {
            groupInterner.setModuleGroupPath(model, existingModule, group);
          }
        }
      }

      loadModules((ModuleModelImpl)model);

      model.commit();
    }
  }

  @Nullable
  private ModuleLoadItem findCorrespondingPath(final Module existingModule) {
    for (ModuleLoadItem modulePath : myModuleLoadItems) {
      if (modulePath.getDirUrl().equals(existingModule.getModuleDirUrl())){
        return modulePath;
      }
    }

    return null;
  }

  private void loadState0(final Element element) {
    final Element modules = element.getChild(ELEMENT_MODULES);
    if (modules != null) {
      myModuleLoadItems = new ArrayList<ModuleLoadItem>();
      for (final Element moduleElement : modules.getChildren(ELEMENT_MODULE)) {
        final String dirUrl = moduleElement.getAttributeValue(ATTRIBUTE_DIRURL);
        if(dirUrl == null) {
          continue;
        }

        final String name = moduleElement.getAttributeValue(ATTRIBUTE_NAME);

        myModuleLoadItems.add(new ModuleLoadItem(name, dirUrl, moduleElement));
      }
    }
    else {
      myModuleLoadItems = Collections.emptyList();
    }
  }

  protected void loadModules(final ModuleModelImpl moduleModel) {
    if (myModuleLoadItems.isEmpty()) {
      return;
    }
    ModuleGroupInterner groupInterner = new ModuleGroupInterner();

    final ProgressIndicator progressIndicator = myProject.isDefault() ? null : ProgressIndicatorProvider.getGlobalProgressIndicator();
    if (progressIndicator != null) {
      progressIndicator.setText("Loading modules...");
      progressIndicator.setText2("");
    }

    myFailedModulePaths.clear();
    myFailedModulePaths.addAll(myModuleLoadItems);

    List<ModuleLoadingErrorDescription> errors = new ArrayList<ModuleLoadingErrorDescription>();

    for (ModuleLoadItem moduleLoadItem : myModuleLoadItems) {
      try {
        final Module module = moduleModel.loadModuleInternal(moduleLoadItem, progressIndicator);
        final String[] groups = moduleLoadItem.getGroups();
        if (groups != null) {
          groupInterner.setModuleGroupPath(moduleModel, module, groups); //model should be updated too
        }

        myFailedModulePaths.remove(moduleLoadItem);
      }
      catch (final IOException e) {
        errors.add(ModuleLoadingErrorDescription.create(ProjectBundle.message("module.cannot.load.error", VirtualFileManager.extractPath(moduleLoadItem.getDirUrl()), e.getMessage()),
                                                        moduleLoadItem, this));
      }
      catch (final ModuleWithNameAlreadyExists moduleWithNameAlreadyExists) {
        errors.add(ModuleLoadingErrorDescription.create(moduleWithNameAlreadyExists.getMessage(), moduleLoadItem, this));
      }
      catch (StateStorageException e) {
        errors.add(ModuleLoadingErrorDescription.create(ProjectBundle.message("module.cannot.load.error", VirtualFileManager.extractPath(
          moduleLoadItem.getDirUrl()), e.getMessage()),
                                                        moduleLoadItem, this));
      }
    }

    fireErrors(errors);
  }


  protected void fireModuleAdded(Module module) {
    myMessageBus.syncPublisher(ProjectTopics.MODULES).moduleAdded(myProject, module);
  }

  protected void fireModuleRemoved(Module module) {
    myMessageBus.syncPublisher(ProjectTopics.MODULES).moduleRemoved(myProject, module);
  }

  protected void fireBeforeModuleRemoved(Module module) {
    myMessageBus.syncPublisher(ProjectTopics.MODULES).beforeModuleRemoved(myProject, module);
  }

  protected void fireModulesRenamed(List<Module> modules) {
    if (!modules.isEmpty()) {
      myMessageBus.syncPublisher(ProjectTopics.MODULES).modulesRenamed(myProject, modules);
    }
  }

  private void fireErrors(final List<ModuleLoadingErrorDescription> errors) {
    if (errors.isEmpty()) return;

    myModuleModel.myModulesCache = null;
    for (ModuleLoadingErrorDescription error : errors) {
      final Module module = myModuleModel.myPathToModule.remove(error.getModuleLoadItem().getDirUrl());
      if (module != null) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            Disposer.dispose(module);
          }
        });
      }
    }

    if (ApplicationManager.getApplication().isHeadlessEnvironment()) {
      throw new RuntimeException(errors.get(0).getDescription());
    }

    ProjectLoadingErrorsNotifier.getInstance(myProject).registerErrors(errors);
  }

  public void removeFailedModulePath(@NotNull ModuleManagerImpl.ModuleLoadItem modulePath) {
    myFailedModulePaths.remove(modulePath);
  }

  @Override
  @NotNull
  public ModifiableModuleModel getModifiableModel() {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    return new ModuleModelImpl(myModuleModel);
  }

  public void getState0(Element element) {
    final Element modulesElement = new Element(ELEMENT_MODULES);
    final Module[] modules = getModules();

    for(Module module : modules) {
      Element moduleElement = new Element(ELEMENT_MODULE);
      String name = module.getName();
      final String[] moduleGroupPath = getModuleGroupPath(module);
      if(moduleGroupPath != null) {
        name = StringUtil.join(moduleGroupPath, MODULE_GROUP_SEPARATOR) + MODULE_GROUP_SEPARATOR + name;
      }
      moduleElement.setAttribute(ATTRIBUTE_NAME, name);
      moduleElement.setAttribute(ATTRIBUTE_DIRURL, module.getModuleDirUrl());

      final ModuleRootManagerImpl moduleRootManager = (ModuleRootManagerImpl)ModuleRootManager.getInstance(module);
      moduleRootManager.saveState(moduleElement);

      collapseOrExpandMacros(module, moduleElement, true);

      modulesElement.addContent(moduleElement);
    }

    for (ModuleLoadItem failedModulePath : myFailedModulePaths) {
      final Element clone = failedModulePath.getElement().clone();
      modulesElement.addContent(clone);
    }

    element.addContent(modulesElement);
  }

  /**
   * Method expand or collapse element children.  This is need because PathMacroManager affected to attributes to.
   * If dirurl equals file://$PROJECT_DIR$ it ill replace to  file://$MODULE_DIR$, and after restart it ill throw error directory not found
   *
   * @param module
   * @param element
   * @param collapse
   */
  private static void collapseOrExpandMacros(Module module, Element element, boolean collapse) {
    final PathMacroManager pathMacroManager = PathMacroManager.getInstance(module);
    for (Element child : element.getChildren()) {
      if(collapse) {
        pathMacroManager.collapsePaths(child);
      }
      else {
        pathMacroManager.expandPaths(child);
      }
    }
  }

  @Override
  @NotNull
  @RequiredWriteAction
  public Module newModule(@NotNull @NonNls String name, @NotNull @NonNls String dirPath) {
    myModificationCount++;
    final ModifiableModuleModel modifiableModel = getModifiableModel();
    final Module module = modifiableModel.newModule(name, dirPath);
    modifiableModel.commit();
    return module;
  }

  @Override
  @RequiredWriteAction
  public void disposeModule(@NotNull final Module module) {
    final ModifiableModuleModel modifiableModel = getModifiableModel();
    modifiableModel.disposeModule(module);
    modifiableModel.commit();
  }

  @Override
  @NotNull
  public Module[] getModules() {
    if (myModuleModel.myIsWritable) {
      ApplicationManager.getApplication().assertReadAccessAllowed();
    }
    return myModuleModel.getModules();
  }

  private Module[] myCachedSortedModules = null;

  @Override
  @NotNull
  public Module[] getSortedModules() {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    deliverPendingEvents();
    if (myCachedSortedModules == null) {
      myCachedSortedModules = myModuleModel.getSortedModules();
    }
    return myCachedSortedModules;
  }

  @Override
  public Module findModuleByName(@NotNull String name) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    return myModuleModel.findModuleByName(name);
  }

  private Comparator<Module> myCachedModuleComparator = null;

  @Override
  @NotNull
  public Comparator<Module> moduleDependencyComparator() {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    deliverPendingEvents();
    if (myCachedModuleComparator == null) {
      myCachedModuleComparator = myModuleModel.moduleDependencyComparator();
    }
    return myCachedModuleComparator;
  }

  protected void deliverPendingEvents() {
  }

  @Override
  @NotNull
  public Graph<Module> moduleGraph() {
    return moduleGraph(true);
  }

  @NotNull
  @Override
  public Graph<Module> moduleGraph(boolean includeTests) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    return myModuleModel.moduleGraph(includeTests);
  }

  @Override
  @NotNull public List<Module> getModuleDependentModules(@NotNull Module module) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    return myModuleModel.getModuleDependentModules(module);
  }

  @Override
  public boolean isModuleDependent(@NotNull Module module, @NotNull Module onModule) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    return myModuleModel.isModuleDependent(module, onModule);
  }

  @Override
  public void projectOpened() {
    fireModulesAdded();

    myModuleModel.projectOpened();
  }

  protected void fireModulesAdded() {
    for (final Module module : myModuleModel.myPathToModule.values()) {
      fireModuleAddedInWriteAction(module);
    }
  }

  protected void fireModuleAddedInWriteAction(final Module module) {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        ((ModuleEx)module).moduleAdded();
        fireModuleAdded(module);
      }
    });
  }

  @Override
  public void projectClosed() {
    myModuleModel.projectClosed();
  }

  public static void commitModelWithRunnable(ModifiableModuleModel model, Runnable runnable) {
    ((ModuleModelImpl)model).commitWithRunnable(runnable);
  }

  protected abstract ModuleEx createModule(String name, String dirUrl);

  protected ModuleEx createAndLoadModule(final ModuleLoadItem moduleLoadItem, ModuleModelImpl moduleModel) throws IOException {
    final ModuleEx module = createModule(moduleLoadItem.getName(), moduleLoadItem.getDirUrl());
    moduleModel.initModule(module);

    collapseOrExpandMacros(module, moduleLoadItem.getElement(), false);

    final ModuleRootManagerImpl moduleRootManager = (ModuleRootManagerImpl) ModuleRootManager.getInstance(module);
    ApplicationManager.getApplication().runReadAction(new Runnable() {
      @Override
      public void run() {
        moduleRootManager.loadState(moduleLoadItem.getElement());
      }
    });

    return module;
  }

  class ModuleModelImpl implements ModifiableModuleModel {
    final Map<String, Module> myPathToModule = new LinkedHashMap<String, Module>();
    private Module[] myModulesCache;

    private final List<Module> myModulesToDispose = new ArrayList<Module>();
    private final Map<Module, String> myModuleToNewName = new HashMap<Module, String>();
    private final Map<String, Module> myNewNameToModule = new HashMap<String, Module>();
    private boolean myIsWritable;
    private Map<Module, String[]> myModuleGroupPath;

    ModuleModelImpl() {
      myIsWritable = false;
    }

    ModuleModelImpl(ModuleModelImpl that) {
      myPathToModule.putAll(that.myPathToModule);
      final Map<Module, String[]> groupPath = that.myModuleGroupPath;
      if (groupPath != null){
        myModuleGroupPath = new THashMap<Module, String[]>();
        myModuleGroupPath.putAll(that.myModuleGroupPath);
      }
      myIsWritable = true;
    }

    private void assertWritable() {
      LOGGER.assertTrue(myIsWritable, "Attempt to modify committed ModifiableModuleModel");
    }

    @Override
    @NotNull
    public Module[] getModules() {
      if (myModulesCache == null) {
        Collection<Module> modules = myPathToModule.values();
        myModulesCache = modules.toArray(new Module[modules.size()]);
      }
      return myModulesCache;
    }

    private Module[] getSortedModules() {
      Module[] allModules = getModules().clone();
      Arrays.sort(allModules, moduleDependencyComparator());
      return allModules;
    }

    @Override
    public void renameModule(@NotNull Module module, @NotNull String newName) throws ModuleWithNameAlreadyExists {
      final Module oldModule = getModuleByNewName(newName);
      myNewNameToModule.remove(myModuleToNewName.get(module));
      if(module.getName().equals(newName)){ // if renaming to itself, forget it altogether
        myModuleToNewName.remove(module);
        myNewNameToModule.remove(newName);
      } else {
        myModuleToNewName.put(module, newName);
        myNewNameToModule.put(newName, module);
      }

      if (oldModule != null) {
        throw new ModuleWithNameAlreadyExists(ProjectBundle.message("module.already.exists.error", newName), newName);
      }
    }

    @Override
    public Module getModuleToBeRenamed(@NotNull String newName) {
      return myNewNameToModule.get(newName);
    }

    @Nullable
    public Module getModuleByNewName(String newName) {
      final Module moduleToBeRenamed = getModuleToBeRenamed(newName);
      if (moduleToBeRenamed != null) {
        return moduleToBeRenamed;
      }
      final Module moduleWithOldName = findModuleByName(newName);
      if (myModuleToNewName.get(moduleWithOldName) == null) {
        return moduleWithOldName;
      }
      else {
        return null;
      }
    }

    @Override
    public String getNewName(@NotNull Module module) {
      return myModuleToNewName.get(module);
    }

    @Override
    @NotNull
    public Module newModule(@NotNull @NonNls String name, @NotNull @NonNls String dirPath) {
      return newModule(name, dirPath, null);
    }

    @Override
    @NotNull
    public Module newModule(@NotNull @NonNls String name, @NotNull @NonNls String dirPath, @Nullable Map<String, String> options) {
      assertWritable();

      final String dirUrl = VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, dirPath);
      ModuleEx module = getModuleByDirUrl(dirUrl);
      if (module == null) {
        module = createModule(name, dirUrl);
        if (options != null) {
          for ( Map.Entry<String,String> option : options.entrySet()) {
            module.setOption(option.getKey(),option.getValue());
          }
        }
        initModule(module);
      }
      return module;
    }

    @Nullable
    private ModuleEx getModuleByDirUrl(String dirUrl) {
      final Collection<Module> modules = myPathToModule.values();
      for (Module module : modules) {
        if (FileUtil.pathsEqual(dirUrl, module.getModuleDirPath())) {
          return (ModuleEx)module;
        }
      }
      return null;
    }

    private Module loadModuleInternal(@NotNull ModuleLoadItem item, @Nullable ProgressIndicator progressIndicator)
      throws ModuleWithNameAlreadyExists, IOException, StateStorageException {

      final VirtualFile moduleDir = VirtualFileManager.getInstance().findFileByUrl(item.getDirUrl());
      if (moduleDir == null || !moduleDir.exists() || !moduleDir.isDirectory()) {
        throw new IOException(ProjectBundle.message("module.dir.does.not.exist.error", item.getDirUrl()));
      }

      final String moduleName = item.getName();
      if (progressIndicator != null) {
        progressIndicator.setText2(moduleName);
      }

      for (Module module : myPathToModule.values()) {
        if (module.getName().equals(moduleName)) {
          throw new ModuleWithNameAlreadyExists(ProjectBundle.message("module.already.exists.error", moduleName), moduleName);
        }
      }

      ModuleEx module = getModuleByDirUrl(moduleDir.getUrl());
      if (module == null) {
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
          @Override
          public void run() {
            moduleDir.refresh(false, false);
          }
        }, ModalityState.defaultModalityState());
        module = createAndLoadModule(item, this);
      }
      return module;
    }

    private void initModule(ModuleEx module) {
      myModulesCache = null;
      myPathToModule.put(module.getModuleDirUrl(), module);

      module.loadModuleComponents();
      module.init();
    }

    @Override
    public void disposeModule(@NotNull Module module) {
      assertWritable();
      myModulesCache = null;
      if (myPathToModule.values().contains(module)) {
        myPathToModule.remove(module.getModuleDirUrl());
        myModulesToDispose.add(module);
      }
      if (myModuleGroupPath != null){
        myModuleGroupPath.remove(module);
      }
    }

    @Override
    public Module findModuleByName(@NotNull String name) {
      for (Module module : myPathToModule.values()) {
        if (!module.isDisposed() && module.getName().equals(name)) {
          return module;
        }
      }
      return null;
    }

    private Comparator<Module> moduleDependencyComparator() {
      DFSTBuilder<Module> builder = new DFSTBuilder<Module>(moduleGraph(true));
      return builder.comparator();
    }

    private Graph<Module> moduleGraph(final boolean includeTests) {
      return GraphGenerator.create(CachingSemiGraph.create(new GraphGenerator.SemiGraph<Module>() {
        @Override
        public Collection<Module> getNodes() {
          return myPathToModule.values();
        }

        @Override
        public Iterator<Module> getIn(Module m) {
          Module[] dependentModules = ModuleRootManager.getInstance(m).getDependencies(includeTests);
          return Arrays.asList(dependentModules).iterator();
        }
      }));
    }

    @NotNull private List<Module> getModuleDependentModules(Module module) {
      List<Module> result = new ArrayList<Module>();
      for (Module aModule : myPathToModule.values()) {
        if (isModuleDependent(aModule, module)) {
          result.add(aModule);
        }
      }
      return result;
    }

    private boolean isModuleDependent(Module module, Module onModule) {
      return ModuleRootManager.getInstance(module).isDependsOn(onModule);
    }

    @Override
    @RequiredWriteAction
    public void commit() {
      ModifiableRootModel[] rootModels = new ModifiableRootModel[0];
      ModifiableModelCommitter.multiCommit(rootModels, this);
    }

    public void commitWithRunnable(Runnable runnable) {
      commitModel(this, runnable);
      clearRenamingStuff();
    }

    private void clearRenamingStuff() {
      myModuleToNewName.clear();
      myNewNameToModule.clear();
    }

    @Override
    public void dispose() {
      assertWritable();
      ApplicationManager.getApplication().assertWriteAccessAllowed();
      final Collection<Module> list = myModuleModel.myPathToModule.values();
      final Collection<Module> thisModules = myPathToModule.values();
      for (Module thisModule : thisModules) {
        if (!list.contains(thisModule)) {
          Disposer.dispose(thisModule);
        }
      }
      for (Module moduleToDispose : myModulesToDispose) {
        if (!list.contains(moduleToDispose)) {
          Disposer.dispose(moduleToDispose);
        }
      }
      clearRenamingStuff();
    }

    @Override
    public boolean isChanged() {
      if (!myIsWritable) {
        return false;
      }
      Set<Module> thisModules = new HashSet<Module>(myPathToModule.values());
      Set<Module> thatModules = new HashSet<Module>(myModuleModel.myPathToModule.values());
      return !thisModules.equals(thatModules) || !Comparing.equal(myModuleModel.myModuleGroupPath, myModuleGroupPath);
    }

    private void disposeModel() {
      myModulesCache = null;
      for (final Module module : myPathToModule.values()) {
        Disposer.dispose(module);
      }
      myPathToModule.clear();
      myModuleGroupPath = null;
    }

    public void projectOpened() {
      final Collection<Module> collection = myPathToModule.values();
      for (final Module aCollection : collection) {
        ModuleEx module = (ModuleEx)aCollection;
        module.projectOpened();
      }
    }

    public void projectClosed() {
      final Collection<Module> collection = myPathToModule.values();
      for (final Module aCollection : collection) {
        ModuleEx module = (ModuleEx)aCollection;
        module.projectClosed();
      }
    }

    @Override
    @Nullable
    public String[] getModuleGroupPath(Module module) {
      return myModuleGroupPath == null ? null : myModuleGroupPath.get(module);
    }

    @Override
    public boolean hasModuleGroups() {
      return myModuleGroupPath != null && !myModuleGroupPath.isEmpty();
    }

    @Override
    public void setModuleGroupPath(Module module, String[] groupPath) {
      if (myModuleGroupPath == null) {
        myModuleGroupPath = new THashMap<Module, String[]>();
      }
      if (groupPath == null) {
        myModuleGroupPath.remove(module);
      }
      else {
        myModuleGroupPath.put(module, groupPath);
      }
    }

    @Override
    public void setModuleFilePath(Module module, String oldPath, String newFilePath) {
      myPathToModule.remove(oldPath);
      myPathToModule.put(newFilePath, module);
    }
  }

  private void commitModel(final ModuleModelImpl moduleModel, final Runnable runnable) {
    myModuleModel.myModulesCache = null;
    myModificationCount++;
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    final Collection<Module> oldModules = myModuleModel.myPathToModule.values();
    final Collection<Module> newModules = moduleModel.myPathToModule.values();
    final List<Module> removedModules = new ArrayList<Module>(oldModules);
    removedModules.removeAll(newModules);
    final List<Module> addedModules = new ArrayList<Module>(newModules);
    addedModules.removeAll(oldModules);

    ProjectRootManagerEx.getInstanceEx(myProject).makeRootsChange(new Runnable() {
      @Override
      public void run() {
        for (Module removedModule : removedModules) {
          fireBeforeModuleRemoved(removedModule);
          cleanCachedStuff();
        }

        List<Module> neverAddedModules = new ArrayList<Module>(moduleModel.myModulesToDispose);
        neverAddedModules.removeAll(myModuleModel.myPathToModule.values());
        for (final Module neverAddedModule : neverAddedModules) {
          neverAddedModule.putUserData(DISPOSED_MODULE_NAME, neverAddedModule.getName());
          Disposer.dispose(neverAddedModule);
        }

        if (runnable != null) {
          runnable.run();
        }

        final Map<Module, String> modulesToNewNamesMap = moduleModel.myModuleToNewName;
        final Set<Module> modulesToBeRenamed = modulesToNewNamesMap.keySet();
        modulesToBeRenamed.removeAll(moduleModel.myModulesToDispose);
        final List<Module> modules = new ArrayList<Module>();
        for (final Module moduleToBeRenamed : modulesToBeRenamed) {
          ModuleEx module = (ModuleEx)moduleToBeRenamed;
          moduleModel.myPathToModule.remove(moduleToBeRenamed.getModuleDirUrl());
          modules.add(moduleToBeRenamed);
          module.rename(modulesToNewNamesMap.get(moduleToBeRenamed));
          moduleModel.myPathToModule.put(moduleToBeRenamed.getModuleDirUrl(), module);
        }

        moduleModel.myIsWritable = false;
        myModuleModel = moduleModel;

        for (Module module : removedModules) {
          fireModuleRemoved(module);
          cleanCachedStuff();
          Disposer.dispose(module);
          cleanCachedStuff();
        }

        for (Module addedModule : addedModules) {
          ((ModuleEx)addedModule).moduleAdded();
          cleanCachedStuff();
          fireModuleAdded(addedModule);
          cleanCachedStuff();
        }
        cleanCachedStuff();
        fireModulesRenamed(modules);
        cleanCachedStuff();
      }
    }, false, true);
  }

  void fireModuleRenamedByVfsEvent(@NotNull final Module module) {
    ProjectRootManagerEx.getInstanceEx(myProject).makeRootsChange(new Runnable() {
      @Override
      public void run() {
        fireModulesRenamed(Collections.singletonList(module));
      }
    }, false, true);
  }

  @Override
  public String[] getModuleGroupPath(@NotNull Module module) {
    return myModuleModel.getModuleGroupPath(module);
  }

  public void setModuleGroupPath(Module module, String[] groupPath) {
    myModuleModel.setModuleGroupPath(module, groupPath);
  }
}

