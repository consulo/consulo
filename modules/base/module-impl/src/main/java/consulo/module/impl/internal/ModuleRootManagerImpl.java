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

package consulo.module.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.content.ContentFolderTypeProvider;
import consulo.content.OrderRootType;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModifiableModelCommitter;
import consulo.module.content.ModuleFileIndex;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.internal.ProjectRootManagerEx;
import consulo.module.content.internal.RootConfigurationAccessor;
import consulo.module.content.layer.*;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.RootPolicy;
import consulo.module.extension.ModuleExtension;
import consulo.module.impl.internal.layer.RootModelImpl;
import consulo.module.impl.internal.layer.orderEntry.ModuleOrderEnumerator;
import consulo.module.impl.internal.layer.orderEntry.OrderRootsCache;
import consulo.project.Project;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

@Singleton
@ServiceImpl
public class ModuleRootManagerImpl extends ModuleRootManager implements Disposable {
  private static final Logger LOG = Logger.getInstance(ModuleRootManagerImpl.class);

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
    return myModule.getInstance(ModuleFileIndex.class);
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
      }
    };
    if (Disposer.isDebugMode()) {
      myModelCreations.put(model, new Throwable());
    }
    return model;
  }

  public void makeRootsChange(@Nonnull Runnable runnable) {
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
  public boolean iterateContentEntries(@Nonnull Predicate<ContentEntry> processor) {
    return myRootModel.iterateContentEntries(processor);
  }

  @Override
  @Nonnull
  public OrderEntry[] getOrderEntries() {
    return myRootModel.getOrderEntries();
  }

  @RequiredWriteAction
  public void commitModel(RootModelImpl rootModel) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    LOG.assertTrue(rootModel.myModuleRootManager == this);

    final Project project = myModule.getProject();
    final ModifiableModuleModel moduleModel = ModuleManager.getInstance(project).getModifiableModel();
    ModifiableModelCommitter.getInstance(getProject()).multiCommit(new ModifiableRootModel[]{rootModel}, moduleModel);
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
    LOG.assertTrue(!myIsDisposed);
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
    LOG.assertTrue(!myIsDisposed);
    return myRootModel.getContentRoots();
  }

  @Override
  @Nonnull
  public String[] getContentRootUrls() {
    LOG.assertTrue(!myIsDisposed);
    return myRootModel.getContentRootUrls();
  }

  @Nonnull
  @Override
  public String[] getContentFolderUrls(@Nonnull Predicate<ContentFolderTypeProvider> predicate) {
    LOG.assertTrue(!myIsDisposed);
    return myRootModel.getContentFolderUrls(predicate);
  }

  @Nonnull
  @Override
  public VirtualFile[] getContentFolderFiles(@Nonnull Predicate<ContentFolderTypeProvider> predicate) {
    LOG.assertTrue(!myIsDisposed);
    return myRootModel.getContentFolderFiles(predicate);
  }

  @Nonnull
  @Override
  public ContentFolder[] getContentFolders(@Nonnull Predicate<ContentFolderTypeProvider> predicate) {
    LOG.assertTrue(!myIsDisposed);
    return myRootModel.getContentFolders(predicate);
  }

  @Override
  @Nonnull
  public String[] getExcludeRootUrls() {
    LOG.assertTrue(!myIsDisposed);
    return myRootModel.getExcludeRootUrls();
  }

  @Override
  @Nonnull
  public VirtualFile[] getExcludeRoots() {
    LOG.assertTrue(!myIsDisposed);
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
    LOG.assertTrue(!myIsDisposed);
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
    LOG.assertTrue(!myIsDisposed);
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
    LOG.assertTrue(!myIsDisposed);
    return myRootModel.findLayerByName(name);
  }

  @Nonnull
  @Override
  public Map<String, ModuleRootLayer> getLayers() {
    LOG.assertTrue(!myIsDisposed);
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
        makeRootsChange(() -> newModel.doCommitAndDispose());
      }
      else {
        myRootModel = newModel;
      }
    }
    catch (InvalidDataException e) {
      LOG.error(e);
    }
  }
}
