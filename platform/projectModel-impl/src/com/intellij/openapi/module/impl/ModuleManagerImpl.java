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
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.StateStorageException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.*;
import com.intellij.openapi.progress.ProcessCanceledException;
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
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashMap;
import com.intellij.util.containers.HashSet;
import com.intellij.util.containers.StringInterner;
import com.intellij.util.graph.*;
import com.intellij.util.messages.MessageBus;
import consulo.annotations.RequiredDispatchThread;
import consulo.annotations.RequiredReadAction;
import consulo.annotations.RequiredWriteAction;
import consulo.module.ModuleDirIsNotExistsException;
import gnu.trove.THashMap;
import gnu.trove.TObjectHashingStrategy;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author max
 */
public abstract class ModuleManagerImpl extends ModuleManager implements ProjectComponent, PersistentStateComponent<Element>, ModificationTracker {
  public static final Logger LOGGER = Logger.getInstance(ModuleManagerImpl.class);

  public static class ModuleLoadItem {
    private final String myDirUrl;
    private final String myName;
    private final String[] myGroups;
    private final Element myElement;

    public ModuleLoadItem(@NotNull String name, @Nullable String dirUrl, @NotNull Element element) {
      myDirUrl = dirUrl;
      myElement = element;

      if (name.contains(MODULE_GROUP_SEPARATOR)) {
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

    @Nullable
    public String[] getGroups() {
      return myGroups;
    }

    @Nullable
    public String getDirUrl() {
      return myDirUrl;
    }

    @NotNull
    public String getName() {
      return myName;
    }

    @NotNull
    public Element getElement() {
      return myElement;
    }
  }

  private static class ModuleGroupInterner {
    private final StringInterner groups = new StringInterner();
    private final Map<String[], String[]> paths = new THashMap<>(new TObjectHashingStrategy<String[]>() {
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

  private final List<ModuleLoadItem> myFailedModulePaths = new ArrayList<>();

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
  @RequiredReadAction
  public Element getState() {
    final Element e = new Element("state");
    getState0(e);
    return e;
  }

  @Override
  @RequiredWriteAction
  public void loadState(Element state) {
    final Element modules = state.getChild(ELEMENT_MODULES);
    if (modules != null) {
      myModuleLoadItems = new ArrayList<>();
      for (final Element moduleElement : modules.getChildren(ELEMENT_MODULE)) {
        final String name = moduleElement.getAttributeValue(ATTRIBUTE_NAME);
        if (name == null) {
          continue;
        }
        final String dirUrl = moduleElement.getAttributeValue(ATTRIBUTE_DIRURL);

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

    List<ModuleLoadingErrorDescription> errors = new ArrayList<>();

    for (ModuleLoadItem moduleLoadItem : myModuleLoadItems) {
      if(progressIndicator != null) {
        progressIndicator.checkCanceled();
      }

      try {
        final Module module = moduleModel.loadModuleInternal(moduleLoadItem, progressIndicator);
        final String[] groups = moduleLoadItem.getGroups();
        if (groups != null) {
          groupInterner.setModuleGroupPath(moduleModel, module, groups); //model should be updated too
        }

        myFailedModulePaths.remove(moduleLoadItem);
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (ModuleWithNameAlreadyExistsException | ModuleDirIsNotExistsException e) {
        errors.add(ModuleLoadingErrorDescription.create(e.getMessage(), moduleLoadItem, this));
      }
      catch (Exception e) {
        errors.add(ModuleLoadingErrorDescription
                           .create(ProjectBundle.message("module.cannot.load.error", moduleLoadItem.getName(), ExceptionUtil.getThrowableText(e)),
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
      String dirUrl = error.getModuleLoadItem().getDirUrl();
      if (dirUrl == null) {
        continue;
      }
      final Module module = myModuleModel.removeModuleByDirUrl(dirUrl);
      if (module != null) {
        ApplicationManager.getApplication().invokeLater(() -> Disposer.dispose(module));
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

  @RequiredReadAction
  @Override
  @NotNull
  public ModifiableModuleModel getModifiableModel() {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    return new ModuleModelImpl(myModuleModel);
  }

  @RequiredReadAction
  public void getState0(Element element) {
    final Element modulesElement = new Element(ELEMENT_MODULES);
    final Module[] modules = getModules();

    for (Module module : modules) {
      Element moduleElement = new Element(ELEMENT_MODULE);
      String name = module.getName();
      final String[] moduleGroupPath = getModuleGroupPath(module);
      if (moduleGroupPath != null) {
        name = StringUtil.join(moduleGroupPath, MODULE_GROUP_SEPARATOR) + MODULE_GROUP_SEPARATOR + name;
      }
      moduleElement.setAttribute(ATTRIBUTE_NAME, name);
      String moduleDirUrl = module.getModuleDirUrl();
      if (moduleDirUrl != null) {
        moduleElement.setAttribute(ATTRIBUTE_DIRURL, moduleDirUrl);
      }

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
   */
  private static void collapseOrExpandMacros(Module module, Element element, boolean collapse) {
    final PathMacroManager pathMacroManager = PathMacroManager.getInstance(module);
    for (Element child : element.getChildren()) {
      if (collapse) {
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

  @RequiredReadAction
  @Override
  @NotNull
  public Module[] getModules() {
    if (myModuleModel.myIsWritable) {
      ApplicationManager.getApplication().assertReadAccessAllowed();
    }
    return myModuleModel.getModules();
  }

  private Module[] myCachedSortedModules = null;

  @RequiredReadAction
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

  @RequiredReadAction
  @Override
  public Module findModuleByName(@NotNull String name) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    return myModuleModel.findModuleByName(name);
  }

  private Comparator<Module> myCachedModuleComparator = null;

  @RequiredReadAction
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

  @RequiredReadAction
  @Override
  @NotNull
  public Graph<Module> moduleGraph() {
    return moduleGraph(true);
  }

  @RequiredReadAction
  @NotNull
  @Override
  public Graph<Module> moduleGraph(boolean includeTests) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    return myModuleModel.moduleGraph(includeTests);
  }

  @RequiredReadAction
  @Override
  @NotNull
  public List<Module> getModuleDependentModules(@NotNull Module module) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    return myModuleModel.getModuleDependentModules(module);
  }

  @RequiredReadAction
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
    for (final Module module : myModuleModel.myModules) {
      TransactionGuard.getInstance().submitTransactionAndWait(() -> fireModuleAddedInWriteAction(module));
    }
  }

  @RequiredDispatchThread
  protected void fireModuleAddedInWriteAction(final Module module) {
    ApplicationManager.getApplication().runWriteAction(() -> {
      ((ModuleEx)module).moduleAdded();
      fireModuleAdded(module);
    });
  }

  @Override
  public void projectClosed() {
    myModuleModel.projectClosed();
  }

  @RequiredWriteAction
  public static void commitModelWithRunnable(ModifiableModuleModel model, Runnable runnable) {
    ((ModuleModelImpl)model).commitWithRunnable(runnable);
  }

  @NotNull
  protected abstract ModuleEx createModule(@NotNull String name, @Nullable String dirUrl, ProgressIndicator progressIndicator);

  @NotNull
  protected ModuleEx createAndLoadModule(@NotNull final ModuleLoadItem moduleLoadItem,
                                         @NotNull ModuleModelImpl moduleModel,
                                         @Nullable final ProgressIndicator progressIndicator) {
    final ModuleEx module = createModule(moduleLoadItem.getName(), moduleLoadItem.getDirUrl(), progressIndicator);
    moduleModel.initModule(module);

    collapseOrExpandMacros(module, moduleLoadItem.getElement(), false);

    final ModuleRootManagerImpl moduleRootManager = (ModuleRootManagerImpl)ModuleRootManager.getInstance(module);
    ApplicationManager.getApplication().runReadAction(() -> moduleRootManager.loadState(moduleLoadItem.getElement(), progressIndicator));

    return module;
  }

  class ModuleModelImpl implements ModifiableModuleModel {
    private Set<Module> myModules = new LinkedHashSet<>();
    private Module[] myModulesCache;

    private final List<Module> myModulesToDispose = new ArrayList<>();
    private final Map<Module, String> myModuleToNewName = new HashMap<>();
    private final Map<String, Module> myNewNameToModule = new HashMap<>();
    private boolean myIsWritable;
    private Map<Module, String[]> myModuleGroupPath;

    ModuleModelImpl() {
      myIsWritable = false;
    }

    ModuleModelImpl(@NotNull ModuleModelImpl that) {
      myModules.addAll(that.myModules);
      final Map<Module, String[]> groupPath = that.myModuleGroupPath;
      if (groupPath != null) {
        myModuleGroupPath = new THashMap<>();
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
        myModulesCache = ContainerUtil.toArray(myModules, Module.ARRAY_FACTORY);
      }
      return myModulesCache;
    }

    private Module[] getSortedModules() {
      Module[] allModules = getModules().clone();
      Arrays.sort(allModules, moduleDependencyComparator());
      return allModules;
    }

    @Override
    public void renameModule(@NotNull Module module, @NotNull String newName) throws ModuleWithNameAlreadyExistsException {
      final Module oldModule = getModuleByNewName(newName);
      myNewNameToModule.remove(myModuleToNewName.get(module));
      if (module.getName().equals(newName)) { // if renaming to itself, forget it altogether
        myModuleToNewName.remove(module);
        myNewNameToModule.remove(newName);
      }
      else {
        myModuleToNewName.put(module, newName);
        myNewNameToModule.put(newName, module);
      }

      if (oldModule != null) {
        throw new ModuleWithNameAlreadyExistsException(ProjectBundle.message("module.already.exists.error", newName), newName);
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
    public Module newModule(@NotNull @NonNls String name, @Nullable @NonNls String dirPath) {
      return newModule(name, dirPath, null);
    }

    @Override
    @NotNull
    public Module newModule(@NotNull @NonNls String name, @Nullable @NonNls String dirPath, @Nullable Map<String, String> options) {
      assertWritable();

      final String dirUrl = dirPath == null ? null : VirtualFileManager.constructUrl(StandardFileSystems.FILE_PROTOCOL, dirPath);

      ModuleEx moduleEx = null;
      if (dirUrl != null) {
        moduleEx = getModuleByDirUrl(dirUrl);
      }

      if (moduleEx == null) {
        moduleEx = createModule(name, dirUrl, null);
        if (options != null) {
          for (Map.Entry<String, String> option : options.entrySet()) {
            moduleEx.setOption(option.getKey(), option.getValue());
          }
        }
        initModule(moduleEx);
      }
      return moduleEx;
    }

    @Nullable
    private ModuleEx getModuleByDirUrl(@NotNull String dirUrl) {
      for (Module module : myModules) {
        if (FileUtil.pathsEqual(dirUrl, module.getModuleDirUrl())) {
          return (ModuleEx)module;
        }
      }
      return null;
    }

    @Nullable
    private Module removeModuleByDirUrl(@NotNull String dirUrl) {
      Module toRemove = null;
      for (Module module : myModules) {
        if (FileUtil.pathsEqual(dirUrl, module.getModuleDirUrl())) {
          toRemove = module;
        }
      }
      myModules.remove(toRemove);
      return toRemove;
    }

    @NotNull
    private Module loadModuleInternal(@NotNull ModuleLoadItem item, @Nullable ProgressIndicator progressIndicator)
            throws ModuleWithNameAlreadyExistsException, ModuleDirIsNotExistsException, StateStorageException {

      final String moduleName = item.getName();
      if (progressIndicator != null) {
        progressIndicator.setText2(moduleName);
      }

      for (Module module : myModules) {
        if (module.getName().equals(moduleName)) {
          throw new ModuleWithNameAlreadyExistsException(ProjectBundle.message("module.already.exists.error", moduleName), moduleName);
        }
      }

      ModuleEx oldModule = null;

      String dirUrl = item.getDirUrl();
      if (dirUrl != null) {
        Ref<VirtualFile> ref = Ref.create();
        ApplicationManager.getApplication().invokeAndWait(() -> ref.set(VirtualFileManager.getInstance().refreshAndFindFileByUrl(dirUrl)));
        VirtualFile moduleDir = ref.get();

        if (moduleDir == null || !moduleDir.exists() || !moduleDir.isDirectory()) {
          throw new ModuleDirIsNotExistsException(
                  ProjectBundle.message("module.dir.does.not.exist.error", FileUtil.toSystemDependentName(VirtualFileManager.extractPath(dirUrl))));
        }

        oldModule = getModuleByDirUrl(moduleDir.getUrl());
      }

      if (oldModule == null) {
        oldModule = createAndLoadModule(item, this, progressIndicator);
      }
      return oldModule;
    }

    private void initModule(ModuleEx module) {
      myModulesCache = null;
      myModules.add(module);

      module.loadModuleComponents();
      module.init();
    }

    @Override
    public void disposeModule(@NotNull Module module) {
      assertWritable();
      myModulesCache = null;
      if (myModules.contains(module)) {
        myModules.remove(module);
        myModulesToDispose.add(module);
      }
      if (myModuleGroupPath != null) {
        myModuleGroupPath.remove(module);
      }
    }

    @Override
    public Module findModuleByName(@NotNull String name) {
      for (Module module : myModules) {
        if (!module.isDisposed() && module.getName().equals(name)) {
          return module;
        }
      }
      return null;
    }

    private Comparator<Module> moduleDependencyComparator() {
      DFSTBuilder<Module> builder = new DFSTBuilder<>(moduleGraph(true));
      return builder.comparator();
    }

    private Graph<Module> moduleGraph(final boolean includeTests) {
      return GraphGenerator.generate(CachingSemiGraph.cache(new InboundSemiGraph<Module>() {
        @Override
        public Collection<Module> getNodes() {
          return myModules;
        }

        @Override
        public Iterator<Module> getIn(Module m) {
          Module[] dependentModules = ModuleRootManager.getInstance(m).getDependencies(includeTests);
          return Arrays.asList(dependentModules).iterator();
        }
      }));
    }

    @NotNull
    private List<Module> getModuleDependentModules(@NotNull Module module) {
      List<Module> result = new ArrayList<>();
      for (Module aModule : myModules) {
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

    @RequiredWriteAction
    public void commitWithRunnable(Runnable runnable) {
      commitModel(this, runnable);
      clearRenamingStuff();
    }

    private void clearRenamingStuff() {
      myModuleToNewName.clear();
      myNewNameToModule.clear();
    }

    @RequiredWriteAction
    @Override
    public void dispose() {
      assertWritable();
      ApplicationManager.getApplication().assertWriteAccessAllowed();
      final Collection<Module> list = myModuleModel.myModules;
      final Collection<Module> thisModules = myModules;
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
      Set<Module> thisModules = new HashSet<>(myModules);
      Set<Module> thatModules = new HashSet<>(myModuleModel.myModules);
      return !thisModules.equals(thatModules) || !Comparing.equal(myModuleModel.myModuleGroupPath, myModuleGroupPath);
    }

    private void disposeModel() {
      myModulesCache = null;
      for (final Module module : myModules) {
        Disposer.dispose(module);
      }
      myModules.clear();
      myModuleGroupPath = null;
    }

    public void projectOpened() {
      for (final Module aCollection : myModules) {
        ModuleEx module = (ModuleEx)aCollection;
        module.projectOpened();
      }
    }

    public void projectClosed() {
      for (final Module aCollection : myModules) {
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
        myModuleGroupPath = new THashMap<>();
      }
      if (groupPath == null) {
        myModuleGroupPath.remove(module);
      }
      else {
        myModuleGroupPath.put(module, groupPath);
      }
    }
  }

  @RequiredWriteAction
  private void commitModel(final ModuleModelImpl moduleModel, final Runnable runnable) {
    myModuleModel.myModulesCache = null;
    myModificationCount++;
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    final Collection<Module> oldModules = myModuleModel.myModules;
    final Collection<Module> newModules = moduleModel.myModules;
    final List<Module> removedModules = new ArrayList<>(oldModules);
    removedModules.removeAll(newModules);
    final List<Module> addedModules = new ArrayList<>(newModules);
    addedModules.removeAll(oldModules);

    ProjectRootManagerEx.getInstanceEx(myProject).makeRootsChange(() -> {
      for (Module removedModule : removedModules) {
        fireBeforeModuleRemoved(removedModule);
        cleanCachedStuff();
      }

      List<Module> neverAddedModules = new ArrayList<>(moduleModel.myModulesToDispose);
      neverAddedModules.removeAll(myModuleModel.myModules);
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
      final List<Module> modules = new ArrayList<>();
      for (final Module moduleToBeRenamed : modulesToBeRenamed) {
        ModuleEx module = (ModuleEx)moduleToBeRenamed;
        moduleModel.myModules.remove(moduleToBeRenamed);
        modules.add(moduleToBeRenamed);
        module.rename(modulesToNewNamesMap.get(moduleToBeRenamed));
        moduleModel.myModules.add(module);
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
    }, false, true);
  }

  @RequiredReadAction
  @Override
  public String[] getModuleGroupPath(@NotNull Module module) {
    return myModuleModel.getModuleGroupPath(module);
  }

  public void setModuleGroupPath(Module module, String[] groupPath) {
    myModuleModel.setModuleGroupPath(module, groupPath);
  }
}

