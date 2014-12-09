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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.types.BinariesOrderRootType;
import com.intellij.openapi.roots.types.SourcesOrderRootType;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import org.consulo.compiler.ModuleCompilerPathsManager;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.roots.ContentFolderScopes;
import org.mustbe.consulo.roots.ContentFolderTypeProvider;

import java.util.*;

/**
 * @author nik
 */
public class OrderRootsEnumeratorImpl implements OrderRootsEnumerator {
  private static final NotNullPairFunction<ContentEntry, Predicate<ContentFolderTypeProvider>, VirtualFile[]> ourSourcesToFileFunc =
    new NotNullPairFunction<ContentEntry, Predicate<ContentFolderTypeProvider>, VirtualFile[]>() {
      @NotNull
      @Override
      public VirtualFile[] fun(ContentEntry t, Predicate<ContentFolderTypeProvider> v) {
        return t.getFolderFiles(v);
      }
    };

  private static final NotNullPairFunction<ContentEntry, Predicate<ContentFolderTypeProvider>, String[]> ourSourcesToUrlFunc =
    new NotNullPairFunction<ContentEntry, Predicate<ContentFolderTypeProvider>, String[]>() {
      @NotNull
      @Override
      public String[] fun(ContentEntry t, Predicate<ContentFolderTypeProvider> v) {
        return t.getFolderUrls(v);
      }
    };

  private static final NotNullPairFunction<ModuleRootModel, Predicate<ContentFolderTypeProvider>, VirtualFile[]> ourRuntimeToFileFunc =
    new NotNullPairFunction<ModuleRootModel, Predicate<ContentFolderTypeProvider>, VirtualFile[]>() {
      @NotNull
      @Override
      public VirtualFile[] fun(ModuleRootModel t, Predicate<ContentFolderTypeProvider> v) {
        List<VirtualFile> files = new ArrayList<VirtualFile>(5);
        ModuleCompilerPathsManager compilerPathsManager = ModuleCompilerPathsManager.getInstance(t.getModule());
        for (ContentFolderTypeProvider contentFolderTypeProvider : ContentFolderTypeProvider.EP_NAME.getExtensions()) {
          if(v.apply(contentFolderTypeProvider)) {
            ContainerUtil.addIfNotNull(files, compilerPathsManager.getCompilerOutput(contentFolderTypeProvider));
          }
        }
        return VfsUtilCore.toVirtualFileArray(files);
      }
    };

  private static final NotNullPairFunction<ModuleRootModel, Predicate<ContentFolderTypeProvider>, String[]> ourRuntimeToUrlFunc =
    new NotNullPairFunction<ModuleRootModel, Predicate<ContentFolderTypeProvider>, String[]>() {
      @NotNull
      @Override
      public String[] fun(ModuleRootModel t, Predicate<ContentFolderTypeProvider> v) {
        List<String> urls = new ArrayList<String>(5);
        ModuleCompilerPathsManager compilerPathsManager = ModuleCompilerPathsManager.getInstance(t.getModule());
        for (ContentFolderTypeProvider contentFolderTypeProvider : ContentFolderTypeProvider.EP_NAME.getExtensions()) {
          if(v.apply(contentFolderTypeProvider)) {
            ContainerUtil.addIfNotNull(urls, compilerPathsManager.getCompilerOutputUrl(contentFolderTypeProvider));
          }
        }
        return ArrayUtil.toStringArray(urls);
      }
    };

  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.roots.impl.OrderRootsEnumeratorImpl");
  private final OrderEnumeratorBase myOrderEnumerator;
  private final OrderRootType myRootType;
  private final NotNullFunction<OrderEntry, OrderRootType> myRootTypeProvider;
  private boolean myUsingCache;
  private NotNullFunction<OrderEntry, VirtualFile[]> myCustomRootProvider;
  private boolean myWithoutSelfModuleOutput;

  public OrderRootsEnumeratorImpl(OrderEnumeratorBase orderEnumerator, @NotNull OrderRootType rootType) {
    myOrderEnumerator = orderEnumerator;
    myRootType = rootType;
    myRootTypeProvider = null;
  }

  public OrderRootsEnumeratorImpl(OrderEnumeratorBase orderEnumerator,
                                  @NotNull NotNullFunction<OrderEntry, OrderRootType> rootTypeProvider) {
    myOrderEnumerator = orderEnumerator;
    myRootTypeProvider = rootTypeProvider;
    myRootType = null;
  }

  @NotNull
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

  @NotNull
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
    final Collection<VirtualFile> result = new LinkedHashSet<VirtualFile>();
    myOrderEnumerator.forEach(new Processor<OrderEntry>() {
      @Override
      public boolean process(OrderEntry orderEntry) {
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
      }
    });
    return result;
  }

  @NotNull
  private Collection<String> computeRootsUrls() {
    final Collection<String> result = new LinkedHashSet<String>();
    myOrderEnumerator.forEach(new Processor<OrderEntry>() {
      @Override
      public boolean process(OrderEntry orderEntry) {
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
      }
    });
    return result;
  }

  @NotNull
  @Override
  public PathsList getPathsList() {
    final PathsList list = new PathsList();
    collectPaths(list);
    return list;
  }

  @Override
  public void collectPaths(@NotNull PathsList list) {
    list.addVirtualFiles(getRoots());
  }

  @NotNull
  @Override
  public OrderRootsEnumerator usingCache() {
    myUsingCache = true;
    return this;
  }

  @NotNull
  @Override
  public OrderRootsEnumerator withoutSelfModuleOutput() {
    myWithoutSelfModuleOutput = true;
    return this;
  }

  @NotNull
  @Override
  public OrderRootsEnumerator usingCustomRootProvider(@NotNull NotNullFunction<OrderEntry, VirtualFile[]> provider) {
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
          Collections.addAll(result, rootsProcessor.getFiles(rootModel, predicate));
        }
        else {
          for (ContentEntry entry : rootModel.getContentEntries()) {
            Collections.addAll(result, funForSources.fun(entry, predicate));
          }
        }
      }
      else {
        if(rootsProcessor != null) {
          Collections.addAll(result, rootsProcessor.getFiles(rootModel, ContentFolderScopes.test()));
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
          Collections.addAll(result, rootsProcessor.getUrls(rootModel, predicate));
        }
        else {
          for (ContentEntry entry : rootModel.getContentEntries()) {
            Collections.addAll(result, funForSources.fun(entry, predicate));
          }
        }
      }
      else {
        if(rootsProcessor != null) {
          Collections.addAll(result, rootsProcessor.getUrls(rootModel, ContentFolderScopes.test()));
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
