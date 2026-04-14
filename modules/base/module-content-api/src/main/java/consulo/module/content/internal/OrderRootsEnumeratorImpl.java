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
package consulo.module.content.internal;

import consulo.annotation.ReviewAfterIssueFix;
import consulo.application.Application;
import consulo.application.util.function.CommonProcessors;
import consulo.content.ContentFolderTypeProvider;
import consulo.content.OrderRootType;
import consulo.content.ProductionLikeContentFolderTypeProvider;
import consulo.content.TestLikeContentFolderTypeProvider;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModuleRootModel;
import consulo.module.content.layer.OrderRootsEnumerator;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleSourceOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.PathsList;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author nik
 */
public class OrderRootsEnumeratorImpl implements OrderRootsEnumerator {
    private static final BiFunction<ContentEntry, Predicate<ContentFolderTypeProvider>, VirtualFile[]>
        SOURCES_TO_FILE_FUNC = ContentEntry::getFolderFiles;

    private static final BiFunction<ContentEntry, Predicate<ContentFolderTypeProvider>, String[]>
        SOURCES_TO_URL_FUNC = ContentEntry::getFolderUrls;

    @ReviewAfterIssueFix(value = "github.com/uber/NullAway/issues/1500", todo = "Remove explicit casts")
    private static final BiFunction<ModuleRootModel, Predicate<ContentFolderTypeProvider>, VirtualFile[]> RUNTIME_TO_FILE_FUNC =
        (t, v) -> {
            RuntimeRootProvider compilerPathsManager = RuntimeRootProvider.getInstance(t.getModule());
            return VirtualFileUtil.toVirtualFileArray(
                Application.get().getExtensionPoint(ContentFolderTypeProvider.class)
                    .collectMapped((Function<ContentFolderTypeProvider, @Nullable VirtualFile>) provider -> v.test(provider) ? compilerPathsManager.getCompilerOutput(provider) : null)
            );
        };

    @ReviewAfterIssueFix(value = "github.com/uber/NullAway/issues/1500", todo = "Remove explicit casts")
    private static final BiFunction<ModuleRootModel, Predicate<ContentFolderTypeProvider>, String[]> RUNTIME_TO_URL_FUNC =
        (t, v) -> {
            RuntimeRootProvider compilerPathsManager = RuntimeRootProvider.getInstance(t.getModule());
            return ArrayUtil.toStringArray(
                Application.get().getExtensionPoint(ContentFolderTypeProvider.class)
                    .collectMapped((Function<ContentFolderTypeProvider, @Nullable String>) provider -> v.test(provider) ? compilerPathsManager.getCompilerOutputUrl(provider) : null)
            );
        };

    private static final Logger LOG = Logger.getInstance(OrderRootsEnumeratorImpl.class);
    private final OrderEnumeratorBase myOrderEnumerator;
    private final @Nullable OrderRootType myRootType;
    private final @Nullable Function<OrderEntry, OrderRootType> myRootTypeProvider;
    private boolean myUsingCache;
    private @Nullable Function<OrderEntry, VirtualFile[]> myCustomRootProvider;
    private boolean myWithoutSelfModuleOutput;

    public OrderRootsEnumeratorImpl(OrderEnumeratorBase orderEnumerator, OrderRootType rootType) {
        myOrderEnumerator = orderEnumerator;
        myRootType = rootType;
        myRootTypeProvider = null;
    }

    public OrderRootsEnumeratorImpl(OrderEnumeratorBase orderEnumerator, Function<OrderEntry, OrderRootType> rootTypeProvider) {
        myOrderEnumerator = orderEnumerator;
        myRootTypeProvider = rootTypeProvider;
        myRootType = null;
    }

    @Override
    public VirtualFile[] getRoots() {
        if (myUsingCache && myRootType != null) {
            checkCanUseCache();
            OrderRootsCache cache = myOrderEnumerator.getCache();
            if (cache != null) {
                int flags = myOrderEnumerator.getFlags();
                VirtualFile[] cached = cache.getCachedRoots(myRootType, flags);
                if (cached == null) {
                    return cache.setCachedRoots(myRootType, flags, computeRootsUrls()).getFiles();
                }
                else {
                    return cached;
                }
            }
        }
        return VirtualFileUtil.toVirtualFileArray(computeRoots());
    }

    @Override
    public String[] getUrls() {
        if (myUsingCache && myRootType != null) {
            checkCanUseCache();
            OrderRootsCache cache = myOrderEnumerator.getCache();
            if (cache != null) {
                int flags = myOrderEnumerator.getFlags();
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
        LOG.assertTrue(
            myCustomRootProvider == null,
            "Caching not supported for OrderRootsEnumerator with 'usingCustomRootProvider' option"
        );
        LOG.assertTrue(!myWithoutSelfModuleOutput, "Caching not supported for OrderRootsEnumerator with 'withoutSelfModuleOutput' option");
    }

    private Collection<VirtualFile> computeRoots() {
        Collection<VirtualFile> result = new LinkedHashSet<>();
        myOrderEnumerator.forEach(orderEntry -> {
            OrderRootType type = getRootType(orderEntry);

            if (orderEntry instanceof ModuleSourceOrderEntry moduleSourceOrderEntry) {
                collectRoots(
                    type,
                    moduleSourceOrderEntry.getRootModel(),
                    result,
                    true,
                    !myOrderEnumerator.isProductionOnly(),
                    SOURCES_TO_FILE_FUNC,
                    RUNTIME_TO_FILE_FUNC
                );
            }
            else if (orderEntry instanceof ModuleOrderEntry moduleOrderEntry) {
                Module module = moduleOrderEntry.getModule();
                if (module != null) {
                    ModuleRootModel rootModel = myOrderEnumerator.getRootModel(module);
                    boolean productionOnTests = moduleOrderEntry.isProductionOnTestDependency();
                    boolean includeTests = !myOrderEnumerator.isProductionOnly()
                        && myOrderEnumerator.shouldIncludeTestsFromDependentModulesToTestClasspath()
                        || productionOnTests;
                    collectRoots(type, rootModel, result, !productionOnTests, includeTests, SOURCES_TO_FILE_FUNC, RUNTIME_TO_FILE_FUNC);
                }
            }
            else {
                if (myCustomRootProvider != null) {
                    Collections.addAll(result, myCustomRootProvider.apply(orderEntry));
                    return true;
                }
                Collections.addAll(result, orderEntry.getFiles(type));
            }
            return true;
        });
        return result;
    }

    private Collection<String> computeRootsUrls() {
        Collection<String> result = new LinkedHashSet<>();
        myOrderEnumerator.forEach(orderEntry -> {
            OrderRootType type = getRootType(orderEntry);

            if (orderEntry instanceof ModuleSourceOrderEntry moduleSourceOrderEntry) {
                collectRootUrls(
                    type,
                    moduleSourceOrderEntry.getRootModel(),
                    result,
                    true,
                    !myOrderEnumerator.isProductionOnly(),
                    SOURCES_TO_URL_FUNC,
                    RUNTIME_TO_URL_FUNC
                );
            }
            else if (orderEntry instanceof ModuleOrderEntry moduleOrderEntry) {
                Module module = moduleOrderEntry.getModule();
                if (module != null) {
                    ModuleRootModel rootModel = myOrderEnumerator.getRootModel(module);
                    boolean productionOnTests = moduleOrderEntry.isProductionOnTestDependency();
                    boolean includeTests = !myOrderEnumerator.isProductionOnly()
                        && myOrderEnumerator.shouldIncludeTestsFromDependentModulesToTestClasspath()
                        || productionOnTests;
                    collectRootUrls(type, rootModel, result, !productionOnTests, includeTests, SOURCES_TO_URL_FUNC, RUNTIME_TO_URL_FUNC);
                }
            }
            else {
                Collections.addAll(result, orderEntry.getUrls(type));
            }
            return true;
        });
        return result;
    }

    @Override
    public PathsList getPathsList() {
        PathsList list = new PathsList();
        collectPaths(list);
        return list;
    }

    @Override
    public void collectPaths(PathsList list) {
        list.addVirtualFiles(getRoots());
    }

    @Override
    public OrderRootsEnumerator usingCache() {
        myUsingCache = true;
        return this;
    }

    @Override
    public OrderRootsEnumerator withoutSelfModuleOutput() {
        myWithoutSelfModuleOutput = true;
        return this;
    }

    @Override
    public OrderRootsEnumerator usingCustomRootProvider(Function<OrderEntry, VirtualFile[]> provider) {
        myCustomRootProvider = provider;
        return this;
    }

    private void collectRoots(
        OrderRootType type,
        ModuleRootModel rootModel,
        Collection<VirtualFile> result,
        boolean includeProduction,
        boolean includeTests,
        BiFunction<ContentEntry, Predicate<ContentFolderTypeProvider>, VirtualFile[]> funForSources,
        BiFunction<ModuleRootModel, Predicate<ContentFolderTypeProvider>, VirtualFile[]> funForRuntime
    ) {
        ModuleRootsProcessor rootsProcessor = ModuleRootsProcessor.findRootsProcessor(rootModel);
        if (type.equals(SourcesOrderRootType.getInstance())) {
            if (includeProduction) {
                Predicate<ContentFolderTypeProvider> predicate =
                    includeTests ? productionAndTestFolderFilter() : productionFolderFilter();
                if (rootsProcessor != null) {
                    rootsProcessor.processFiles(rootModel, predicate, new CommonProcessors.CollectProcessor<>(result));
                }
                else {
                    for (ContentEntry entry : rootModel.getContentEntries()) {
                        Collections.addAll(result, funForSources.apply(entry, predicate));
                    }
                }
            }
            else if (rootsProcessor != null) {
                rootsProcessor.processFiles(
                    rootModel,
                    testFolderFilter(),
                    new CommonProcessors.CollectProcessor<>(result)
                );
            }
            else {
                for (ContentEntry entry : rootModel.getContentEntries()) {
                    Collections.addAll(result, funForSources.apply(entry, testFolderFilter()));
                }
            }
        }
        else if (type.equals(BinariesOrderRootType.getInstance())) {
            if (myWithoutSelfModuleOutput && myOrderEnumerator.isRootModuleModel(rootModel)) {
                if (includeTests && includeProduction) {
                    Collections.addAll(result, funForRuntime.apply(rootModel, productionAndTestFolderFilter()));
                }
            }
            else {
                Collections.addAll(
                    result,
                    funForRuntime.apply(
                        rootModel,
                        includeTests ? productionAndTestFolderFilter() : productionFolderFilter()
                    )
                );
            }
        }
    }

    private void collectRootUrls(
        OrderRootType type,
        ModuleRootModel rootModel,
        Collection<String> result,
        boolean includeProduction,
        boolean includeTests,
        BiFunction<ContentEntry, Predicate<ContentFolderTypeProvider>, String[]> funForSources,
        BiFunction<ModuleRootModel, Predicate<ContentFolderTypeProvider>, String[]> funForRuntime
    ) {
        ModuleRootsProcessor rootsProcessor = ModuleRootsProcessor.findRootsProcessor(rootModel);
        if (type.equals(SourcesOrderRootType.getInstance())) {
            if (includeProduction) {
                Predicate<ContentFolderTypeProvider> predicate = includeTests ? productionAndTestFolderFilter() : productionFolderFilter();
                if (rootsProcessor != null) {
                    rootsProcessor.processFileUrls(rootModel, predicate, new CommonProcessors.CollectProcessor<>(result));
                }
                else {
                    for (ContentEntry entry : rootModel.getContentEntries()) {
                        Collections.addAll(result, funForSources.apply(entry, predicate));
                    }
                }
            }
            else if (rootsProcessor != null) {
                rootsProcessor.processFileUrls(
                    rootModel,
                    testFolderFilter(),
                    new CommonProcessors.CollectProcessor<>(result)
                );
            }
            else {
                for (ContentEntry entry : rootModel.getContentEntries()) {
                    Collections.addAll(result, funForSources.apply(entry, testFolderFilter()));
                }
            }
        }
        else if (type.equals(BinariesOrderRootType.getInstance())) {
            if (myWithoutSelfModuleOutput && myOrderEnumerator.isRootModuleModel(rootModel)) {
                if (includeTests && includeProduction) {
                    Collections.addAll(result, funForRuntime.apply(rootModel, productionAndTestFolderFilter()));
                }
            }
            else {
                Collections.addAll(
                    result,
                    funForRuntime.apply(
                        rootModel,
                        includeTests ? productionAndTestFolderFilter() : productionFolderFilter()
                    )
                );
            }
        }
    }

    private OrderRootType getRootType(OrderEntry e) {
        return myRootType != null ? myRootType : Objects.requireNonNull(myRootTypeProvider).apply(e);
    }

    private Predicate<ContentFolderTypeProvider> productionFolderFilter() {
        return t -> t instanceof ProductionLikeContentFolderTypeProvider;
    }

    private Predicate<ContentFolderTypeProvider> testFolderFilter() {
        return t -> t instanceof TestLikeContentFolderTypeProvider;
    }

    private Predicate<ContentFolderTypeProvider> productionAndTestFolderFilter() {
        return t -> t instanceof ProductionLikeContentFolderTypeProvider || t instanceof TestLikeContentFolderTypeProvider;
    }
}
