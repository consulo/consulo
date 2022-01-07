/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.openapi.roots.impl;

import com.google.common.base.Predicate;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.ex.ProjectRootManagerEx;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Processor;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.module.extension.ModuleExtension;
import consulo.roots.ContentFolderTypeProvider;
import consulo.roots.ModuleRootLayer;
import consulo.roots.types.BinariesOrderRootType;
import consulo.roots.types.SourcesOrderRootType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@Singleton
public class ModuleRootManagerImpl extends ModuleRootManager implements Disposable {
  private static final Logger LOGGER = Logger.getInstance(ModuleRootManagerImpl.class);

  private final Module myModule;
  private RootModelImpl myRootModel;
  private boolean myIsDisposed;
  private boolean isModuleAdded;
  private final OrderRootsCache myOrderRootsCache;
  private final Map<RootModelImpl, Throwable> myModelCreations = new HashMap<>();

  @Inject
  public ModuleRootManagerImpl(Module module) {
    myModule = module;
    myRootModel = new RootModelImpl(this);
    myOrderRootsCache = new OrderRootsCache(module);
  }

  @Nonnull
  @Override
  public Project getProject() {
    return myModule.getProject();
  }

  @Override
  @Nonnull
  public Module getModule() {
    return myModule;
  }

  @Override
  @Nonnull
  public ModuleFileIndex getFileIndex() {
    return ServiceManager.getService(myModule, ModuleFileIndex.class);
  }

  @Override
  public void dispose() {
    myRootModel.dispose();
    myIsDisposed = true;

    if (Disposer.isDebugMode()) {
      final Set<Map.Entry<RootModelImpl, Throwable>> entries = myModelCreations.entrySet();
      for (final Map.Entry<RootModelImpl, Throwable> entry : new ArrayList<>(entries)) {
        System.err.println("***********************************************************************************************");
        System.err.println("***                        R O O T   M O D E L   N O T   D I S P O S E D                    ***");
        System.err.println("***********************************************************************************************");
        System.err.println("Created at:");
        entry.getValue().printStackTrace(System.err);
        entry.getKey().dispose();
      }
    }
  }


  @Override
  @Nonnull
  @RequiredReadAction
  public ModifiableRootModel getModifiableModel() {
    return getModifiableModel(new RootConfigurationAccessor());
  }

  @Nonnull
  @RequiredReadAction
  public ModifiableRootModel getModifiableModel(final RootConfigurationAccessor accessor) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    final RootModelImpl model = new RootModelImpl(myRootModel, this, accessor) {
      @Override
      public void dispose() {
        super.dispose();
        if (Disposer.isDebugMode()) {
          myModelCreations.remove(this);
        }

        for (OrderEntry entry : ModuleRootManagerImpl.this.getOrderEntries()) {
          assert !((BaseModuleRootLayerChild)entry).isDisposed();
        }
      }
    };
    if (Disposer.isDebugMode()) {
      myModelCreations.put(model, new Throwable());
    }
    return model;
  }

  void makeRootsChange(@Nonnull Runnable runnable) {
    ProjectRootManagerEx projectRootManagerEx = (ProjectRootManagerEx)ProjectRootManager.getInstance(myModule.getProject());
    // IMPORTANT: should be the first listener!
    projectRootManagerEx.makeRootsChange(runnable, false, isModuleAdded);
  }

  public RootModelImpl getRootModel() {
    return myRootModel;
  }

  @Override
  public ContentEntry[] getContentEntries() {
    return myRootModel.getContentEntries();
  }

  @Override
  public boolean iterateContentEntries(@Nonnull Processor<ContentEntry> processor) {
    return myRootModel.iterateContentEntries(processor);
  }

  @Override
  @Nonnull
  public OrderEntry[] getOrderEntries() {
    return myRootModel.getOrderEntries();
  }

  @RequiredWriteAction
  void commitModel(RootModelImpl rootModel) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    LOGGER.assertTrue(rootModel.myModuleRootManager == this);

    final Project project = myModule.getProject();
    final ModifiableModuleModel moduleModel = ModuleManager.getInstance(project).getModifiableModel();
    ModifiableModelCommitter.multiCommit(new ModifiableRootModel[]{rootModel}, moduleModel);
  }

  @Override
  @Nonnull
  public Module[] getDependencies() {
    return myRootModel.getModuleDependencies();
  }

  @Nonnull
  @Override
  public Module[] getDependencies(boolean includeTests) {
    return myRootModel.getModuleDependencies(includeTests);
  }

  @Nonnull
  @Override
  public Module[] getModuleDependencies() {
    return myRootModel.getModuleDependencies();
  }

  @Nonnull
  @Override
  public Module[] getModuleDependencies(boolean includeTests) {
    return myRootModel.getModuleDependencies(includeTests);
  }

  @Override
  public boolean isDependsOn(Module module) {
    return myRootModel.isDependsOn(module);
  }

  @Override
  @Nonnull
  public String[] getDependencyModuleNames() {
    return myRootModel.getDependencyModuleNames();
  }

  @Nullable
  @Override
  public <T extends ModuleExtension> T getExtension(Class<T> clazz) {
    return myRootModel.getExtension(clazz);
  }

  @Override
  public <T extends ModuleExtension> T getExtension(@Nonnull String key) {
    return myRootModel.getExtension(key);
  }

  @Nullable
  @Override
  public <T extends ModuleExtension> T getExtensionWithoutCheck(Class<T> clazz) {
    return myRootModel.getExtensionWithoutCheck(clazz);
  }

  @Nullable
  @Override
  public <T extends ModuleExtension> T getExtensionWithoutCheck(@Nonnull String key) {
    return myRootModel.getExtensionWithoutCheck(key);
  }

  @Nonnull
  @Override
  public List<ModuleExtension> getExtensions() {
    return myRootModel.getExtensions();
  }

  @Override
  public <R> R processOrder(RootPolicy<R> policy, R initialValue) {
    LOGGER.assertTrue(!myIsDisposed);
    return myRootModel.processOrder(policy, initialValue);
  }

  @Nonnull
  @Override
  public OrderEnumerator orderEntries() {
    return new ModuleOrderEnumerator(myRootModel, myOrderRootsCache);
  }

  public static OrderRootsEnumerator getCachingEnumeratorForType(OrderRootType type, Module module) {
    return getEnumeratorForType(type, module).usingCache();
  }

  @Nonnull
  private static OrderRootsEnumerator getEnumeratorForType(OrderRootType type, Module module) {
    OrderEnumerator base = OrderEnumerator.orderEntries(module);
    if (type == BinariesOrderRootType.getInstance()) {
      return base.exportedOnly().withoutModuleSourceEntries().recursively().classes();
    }
    if (type == SourcesOrderRootType.getInstance()) {
      return base.exportedOnly().recursively().sources();
    }
    return base.roots(type);
  }

  @Override
  @Nonnull
  public VirtualFile[] getContentRoots() {
    LOGGER.assertTrue(!myIsDisposed);
    return myRootModel.getContentRoots();
  }

  @Override
  @Nonnull
  public String[] getContentRootUrls() {
    LOGGER.assertTrue(!myIsDisposed);
    return myRootModel.getContentRootUrls();
  }

  @Nonnull
  @Override
  public String[] getContentFolderUrls(@Nonnull Predicate<ContentFolderTypeProvider> predicate) {
    LOGGER.assertTrue(!myIsDisposed);
    return myRootModel.getContentFolderUrls(predicate);
  }

  @Nonnull
  @Override
  public VirtualFile[] getContentFolderFiles(@Nonnull Predicate<ContentFolderTypeProvider> predicate) {
    LOGGER.assertTrue(!myIsDisposed);
    return myRootModel.getContentFolderFiles(predicate);
  }

  @Nonnull
  @Override
  public ContentFolder[] getContentFolders(@Nonnull Predicate<ContentFolderTypeProvider> predicate) {
    LOGGER.assertTrue(!myIsDisposed);
    return myRootModel.getContentFolders(predicate);
  }

  @Override
  @Nonnull
  public String[] getExcludeRootUrls() {
    LOGGER.assertTrue(!myIsDisposed);
    return myRootModel.getExcludeRootUrls();
  }

  @Override
  @Nonnull
  public VirtualFile[] getExcludeRoots() {
    LOGGER.assertTrue(!myIsDisposed);
    return myRootModel.getExcludeRoots();
  }

  @Override
  @Nonnull
  public String[] getSourceRootUrls() {
    return getSourceRootUrls(true);
  }

  @Nonnull
  @Override
  public String[] getSourceRootUrls(boolean includingTests) {
    LOGGER.assertTrue(!myIsDisposed);
    return myRootModel.getSourceRootUrls(includingTests);
  }

  @Override
  @Nonnull
  public VirtualFile[] getSourceRoots() {
    return getSourceRoots(true);
  }

  @Override
  @Nonnull
  public VirtualFile[] getSourceRoots(final boolean includingTests) {
    LOGGER.assertTrue(!myIsDisposed);
    return myRootModel.getSourceRoots(includingTests);
  }

  @Nonnull
  @Override
  public ModuleRootLayer getCurrentLayer() {
    return myRootModel.getCurrentLayer();
  }

  @Nonnull
  @Override
  public String getCurrentLayerName() {
    return myRootModel.getCurrentLayerName();
  }

  @Nullable
  @Override
  public ModuleRootLayer findLayerByName(@Nonnull String name) {
    LOGGER.assertTrue(!myIsDisposed);
    return myRootModel.findLayerByName(name);
  }

  @Nonnull
  @Override
  public Map<String, ModuleRootLayer> getLayers() {
    LOGGER.assertTrue(!myIsDisposed);
    return myRootModel.getLayers();
  }

  public void moduleAdded() {
    isModuleAdded = true;
  }

  public void dropCaches() {
    myOrderRootsCache.clearCache();
  }

  public void saveState(Element parent) {
    myRootModel.putState(parent);
  }

  @RequiredReadAction
  public void loadState(Element parent, @Nullable ProgressIndicator indicator) {
    loadState(parent, indicator, myRootModel != null);
  }

  @RequiredReadAction
  protected void loadState(Element element, @Nullable ProgressIndicator indicator, boolean throwEvent) {
    try {
      final RootModelImpl newModel = new RootModelImpl(element, indicator, this, throwEvent);

      if (throwEvent) {
        makeRootsChange(() -> newModel.doCommitAndDispose(false));
      }
      else {
        myRootModel = newModel;
      }
    }
    catch (InvalidDataException e) {
      LOGGER.error(e);
    }
  }
}
