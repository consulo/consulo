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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.NotNullFunction;
import com.intellij.util.PathsList;
import com.intellij.util.containers.ContainerUtil;
import consulo.compiler.ModuleCompilerPathsManager;
import consulo.logging.Logger;
import consulo.roots.ContentFolderScopes;
import consulo.roots.ContentFolderTypeProvider;
import consulo.roots.impl.ModuleRootsProcessor;
import consulo.roots.types.BinariesOrderRootType;
import consulo.roots.types.SourcesOrderRootType;
import consulo.util.NotNullPairFunction;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author nik
 */
public class OrderRootsEnumeratorImpl implements OrderRootsEnumerator {
  private static final NotNullPairFunction<ContentEntry, Predicate<ContentFolderTypeProvider>, VirtualFile[]> ourSourcesToFileFunc =
    new NotNullPairFunction<ContentEntry, Predicate<ContentFolderTypeProvider>, VirtualFile[]>() {
      @Nonnull
      @Override
      public VirtualFile[] fun(ContentEntry t, Predicate<ContentFolderTypeProvider> v) {
        return t.getFolderFiles(v);
      }
    };

  private static final NotNullPairFunction<ContentEntry, Predicate<ContentFolderTypeProvider>, String[]> ourSourcesToUrlFunc =
    new NotNullPairFunction<ContentEntry, Predicate<ContentFolderTypeProvider>, String[]>() {
      @Nonnull
      @Override
      public String[] fun(ContentEntry t, Predicate<ContentFolderTypeProvider> v) {
        return t.getFolderUrls(v);
      }
    };

  private static final NotNullPairFunction<ModuleRootModel, Predicate<ContentFolderTypeProvider>, VirtualFile[]> ourRuntimeToFileFunc =
    new NotNullPairFunction<ModuleRootModel, Predicate<ContentFolderTypeProvider>, VirtualFile[]>() {
      @Nonnull
      @Override
      public VirtualFile[] fun(ModuleRootModel t, Predicate<ContentFolderTypeProvider> v) {
        List<VirtualFile> files = new ArrayList<>(5);
        ModuleCompilerPathsManager compilerPathsManager = ModuleCompilerPathsManager.getInstance(t.getModule());
        for (ContentFolderTypeProvider contentFolderTypeProvider : ContentFolderTypeProvider.EP_NAME.getExtensionList()) {
          if(v.apply(contentFolderTypeProvider)) {
            ContainerUtil.addIfNotNull(files, compilerPathsManager.getCompilerOutput(contentFolderTypeProvider));
          }
        }
        return VfsUtilCore.toVirtualFileArray(files);
      }
    };

  private static final NotNullPairFunction<ModuleRootModel, Predicate<ContentFolderTypeProvider>, String[]> ourRuntimeToUrlFunc =
    new NotNullPairFunction<ModuleRootModel, Predicate<ContentFolderTypeProvider>, String[]>() {
      @Nonnull
      @Override
      public String[] fun(ModuleRootModel t, Predicate<ContentFolderTypeProvider> v) {
        List<String> urls = new ArrayList<>(5);
        ModuleCompilerPathsManager compilerPathsManager = ModuleCompilerPathsManager.getInstance(t.getModule());
        for (ContentFolderTypeProvider contentFolderTypeProvider : ContentFolderTypeProvider.EP_NAME.getExtensionList()) {
          if(v.apply(contentFolderTypeProvider)) {
            ContainerUtil.addIfNotNull(urls, compilerPathsManager.getCompilerOutputUrl(contentFolderTypeProvider));
          }
        }
        return ArrayUtil.toStringArray(urls);
      }
    };

  private static final Logger LOG = Logger.getInstance(OrderRootsEnumeratorImpl.class);
  private final OrderEnumeratorBase myOrderEnumerator;
  private final OrderRootType myRootType;
  private final NotNullFunction<OrderEntry, OrderRootType> myRootTypeProvider;
  private boolean myUsingCache;
  private NotNullFunction<OrderEntry, VirtualFile[]> myCustomRootProvider;
  private boolean myWithoutSelfModuleOutput;

  public OrderRootsEnumeratorImpl(OrderEnumeratorBase orderEnumerator, @Nonnull OrderRootType rootType) {
    myOrderEnumerator = orderEnumerator;
    myRootType = rootType;
    myRootTypeProvider = null;
  }

  public OrderRootsEnumeratorImpl(OrderEnumeratorBase orderEnumerator,
                                  @Nonnull NotNullFunction<OrderEntry, OrderRootType> rootTypeProvider) {
    myOrderEnumerator = orderEnumerator;
    myRootTypeProvider = rootTypeProvider;
    myRootType = null;
  }

  @Nonnull
  @Override
  public VirtualFile[] getRoots() {
    if (myUsingCache) {
      checkCanUseCache();
      final OrderRootsCache cache = myOrderEnumerator.getCache();
      if (cache != null) {
        final int flags = myOrderEnumerator.getFlags();
        final VirtualFile[] cached = cache.getCachedRoots(myRootType, flags);
        if (cached == null) {
          return cache.setCachedRoots(myRootType, flags, computeRootsUrls()).getFiles();
        }
        else {
          return cached;
        }
      }
    }
    return VfsUtilCore.toVirtualFileArray(computeRoots());
  }

  @Nonnull
  @Override
  public String[] getUrls() {
    if (myUsingCache) {
      checkCanUseCache();
      final OrderRootsCache cache = myOrderEnumerator.getCache();
      if (cache != null) {
        final int flags = myOrderEnumerator.getFlags();
        String[] cached = cache.getCachedUrls(myRootType, flags);
        if (cached == null) {
          return cache.setCachedRoots(myRootType, flags, computeRootsUrls()).getUrls();
        }
        else {
          return cached;
        }
      }
    }
    return ArrayUtil.toStringArray(computeRootsUrls());
  }

  private void checkCanUseCache() {
    LOG.assertTrue(myRootTypeProvider == null, "Caching not supported for OrderRootsEnumerator with root type provider");
    LOG.assertTrue(myCustomRootProvider == null, "Caching not supported for OrderRootsEnumerator with 'usingCustomRootProvider' option");
    LOG.assertTrue(!myWithoutSelfModuleOutput, "Caching not supported for OrderRootsEnumerator with 'withoutSelfModuleOutput' option");
  }

  private Collection<VirtualFile> computeRoots() {
    final Collection<VirtualFile> result = new LinkedHashSet<>();
    myOrderEnumerator.forEach(orderEntry -> {
      OrderRootType type = getRootType(orderEntry);

      if (orderEntry instanceof ModuleSourceOrderEntry) {
        collectRoots(type, ((ModuleSourceOrderEntry)orderEntry).getRootModel(), result, true, !myOrderEnumerator.isProductionOnly(),
                     ourSourcesToFileFunc, ourRuntimeToFileFunc);
      }
      else if (orderEntry instanceof ModuleOrderEntry) {
        ModuleOrderEntry moduleOrderEntry = (ModuleOrderEntry)orderEntry;
        final Module module = moduleOrderEntry.getModule();
        if (module != null) {
          ModuleRootModel rootModel = myOrderEnumerator.getRootModel(module);
          boolean productionOnTests =
            orderEntry instanceof ModuleOrderEntryImpl && ((ModuleOrderEntryImpl)orderEntry).isProductionOnTestDependency();
          boolean includeTests =
            !myOrderEnumerator.isProductionOnly() && myOrderEnumerator.shouldIncludeTestsFromDependentModulesToTestClasspath() ||
            productionOnTests;
          collectRoots(type, rootModel, result, !productionOnTests, includeTests, ourSourcesToFileFunc, ourRuntimeToFileFunc);
        }
      }
      else {
        if (myCustomRootProvider != null) {
          Collections.addAll(result, myCustomRootProvider.fun(orderEntry));
          return true;
        }
        if (myOrderEnumerator.addCustomRootsForLibrary(orderEntry, type, result)) {
          return true;
        }
        Collections.addAll(result, orderEntry.getFiles(type));
      }
      return true;
    });
    return result;
  }

  @Nonnull
  private Collection<String> computeRootsUrls() {
    final Collection<String> result = new LinkedHashSet<>();
    myOrderEnumerator.forEach(orderEntry -> {
      OrderRootType type = getRootType(orderEntry);

      if (orderEntry instanceof ModuleSourceOrderEntry) {
        collectRootUrls(type, ((ModuleSourceOrderEntry)orderEntry).getRootModel(), result, true, !myOrderEnumerator.isProductionOnly(), ourSourcesToUrlFunc,
                        ourRuntimeToUrlFunc);
      }
      else if (orderEntry instanceof ModuleOrderEntry) {
        ModuleOrderEntry moduleOrderEntry = (ModuleOrderEntry)orderEntry;
        final Module module = moduleOrderEntry.getModule();
        if (module != null) {
          ModuleRootModel rootModel = myOrderEnumerator.getRootModel(module);
          boolean productionOnTests =
            orderEntry instanceof ModuleOrderEntryImpl && ((ModuleOrderEntryImpl)orderEntry).isProductionOnTestDependency();
          boolean includeTests =
            !myOrderEnumerator.isProductionOnly() && myOrderEnumerator.shouldIncludeTestsFromDependentModulesToTestClasspath() ||
            productionOnTests;
          collectRootUrls(type, rootModel, result, !productionOnTests, includeTests, ourSourcesToUrlFunc, ourRuntimeToUrlFunc);
        }
      }
      else {
        if (myOrderEnumerator.addCustomRootUrlsForLibrary(orderEntry, type, result)) {
          return true;
        }
        Collections.addAll(result, orderEntry.getUrls(type));
      }
      return true;
    });
    return result;
  }

  @Nonnull
  @Override
  public PathsList getPathsList() {
    final PathsList list = new PathsList();
    collectPaths(list);
    return list;
  }

  @Override
  public void collectPaths(@Nonnull PathsList list) {
    list.addVirtualFiles(getRoots());
  }

  @Nonnull
  @Override
  public OrderRootsEnumerator usingCache() {
    myUsingCache = true;
    return this;
  }

  @Nonnull
  @Override
  public OrderRootsEnumerator withoutSelfModuleOutput() {
    myWithoutSelfModuleOutput = true;
    return this;
  }

  @Nonnull
  @Override
  public OrderRootsEnumerator usingCustomRootProvider(@Nonnull NotNullFunction<OrderEntry, VirtualFile[]> provider) {
    myCustomRootProvider = provider;
    return this;
  }

  private void collectRoots(OrderRootType type,
                                ModuleRootModel rootModel,
                                Collection<VirtualFile> result,
                                final boolean includeProduction,
                                final boolean includeTests,
                                NotNullPairFunction<ContentEntry, Predicate<ContentFolderTypeProvider>, VirtualFile[]> funForSources,
                                NotNullPairFunction<ModuleRootModel, Predicate<ContentFolderTypeProvider>, VirtualFile[]> funForRuntime) {

    ModuleRootsProcessor rootsProcessor = ModuleRootsProcessor.findRootsProcessor(rootModel);
    if (type.equals(SourcesOrderRootType.getInstance())) {
      if (includeProduction) {
        Predicate<ContentFolderTypeProvider> predicate = includeTests ? ContentFolderScopes.productionAndTest() : ContentFolderScopes.production();
        if(rootsProcessor != null) {
          rootsProcessor.processFiles(rootModel, predicate, new CommonProcessors.CollectProcessor<>(result));
        }
        else {
          for (ContentEntry entry : rootModel.getContentEntries()) {
            Collections.addAll(result, funForSources.fun(entry, predicate));
          }
        }
      }
      else {
        if(rootsProcessor != null) {
          rootsProcessor.processFiles(rootModel, ContentFolderScopes.test(), new CommonProcessors.CollectProcessor<>(result));
        }
        else {
          for (ContentEntry entry : rootModel.getContentEntries()) {
            Collections.addAll(result, funForSources.fun(entry, ContentFolderScopes.test()));
          }
        }
      }
    }
    else if (type.equals(BinariesOrderRootType.getInstance())) {
      if (myWithoutSelfModuleOutput && myOrderEnumerator.isRootModuleModel(rootModel)) {
        if (includeTests && includeProduction) {
          Collections.addAll(result, funForRuntime.fun(rootModel, ContentFolderScopes.productionAndTest()));
        }
      }
      else {
        Collections.addAll(result, funForRuntime
          .fun(rootModel, includeTests ? ContentFolderScopes.productionAndTest() : ContentFolderScopes.production()));
      }
    }
  }

  private void collectRootUrls(OrderRootType type,
                                ModuleRootModel rootModel,
                                Collection<String> result,
                                final boolean includeProduction,
                                final boolean includeTests,
                                NotNullPairFunction<ContentEntry, Predicate<ContentFolderTypeProvider>, String[]> funForSources,
                                NotNullPairFunction<ModuleRootModel, Predicate<ContentFolderTypeProvider>, String[]> funForRuntime) {

    ModuleRootsProcessor rootsProcessor = ModuleRootsProcessor.findRootsProcessor(rootModel);
    if (type.equals(SourcesOrderRootType.getInstance())) {
      if (includeProduction) {
        Predicate<ContentFolderTypeProvider> predicate = includeTests ? ContentFolderScopes.productionAndTest() : ContentFolderScopes.production();
        if(rootsProcessor != null) {
          rootsProcessor.processFileUrls(rootModel, predicate, new CommonProcessors.CollectProcessor<>(result));
        }
        else {
          for (ContentEntry entry : rootModel.getContentEntries()) {
            Collections.addAll(result, funForSources.fun(entry, predicate));
          }
        }
      }
      else {
        if(rootsProcessor != null) {
          rootsProcessor.processFileUrls(rootModel, ContentFolderScopes.test(), new CommonProcessors.CollectProcessor<>(result));
        }
        else {
          for (ContentEntry entry : rootModel.getContentEntries()) {
            Collections.addAll(result, funForSources.fun(entry, ContentFolderScopes.test()));
          }
        }
      }
    }
    else if (type.equals(BinariesOrderRootType.getInstance())) {
      if (myWithoutSelfModuleOutput && myOrderEnumerator.isRootModuleModel(rootModel)) {
        if (includeTests && includeProduction) {
          Collections.addAll(result, funForRuntime.fun(rootModel, ContentFolderScopes.productionAndTest()));
        }
      }
      else {
        Collections.addAll(result, funForRuntime
                .fun(rootModel, includeTests ? ContentFolderScopes.productionAndTest() : ContentFolderScopes.production()));
      }
    }
  }

  private OrderRootType getRootType(OrderEntry e) {
    return myRootType != null ? myRootType : myRootTypeProvider.fun(e);
  }
}
