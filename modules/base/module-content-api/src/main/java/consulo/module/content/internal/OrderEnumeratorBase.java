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

import consulo.application.util.function.Processor;
import consulo.content.OrderRootType;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.library.Library;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.*;
import consulo.module.content.layer.orderEntry.*;
import consulo.project.Project;
import consulo.util.collection.SmartList;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author nik
 */
public  abstract class OrderEnumeratorBase extends OrderEnumerator implements OrderEnumeratorSettings {
  private static final Logger LOG = Logger.getInstance(OrderEnumeratorBase.class);
  private boolean myProductionOnly;
  private boolean myCompileOnly;
  private boolean myRuntimeOnly;
  private boolean myWithoutJdk;
  private boolean myWithoutLibraries;
  protected boolean myWithoutDepModules;
  private boolean myWithoutModuleSourceEntries;
  protected boolean myRecursively;
  protected boolean myRecursivelyExportedOnly;
  private boolean myExportedOnly;
  private Predicate<OrderEntry> myCondition;
  private final List<OrderEnumerationPolicy> myPolicies;
  protected RootModelProvider myModulesProvider;
  private final OrderRootsCache myCache;

  public OrderEnumeratorBase(@Nullable Module module, @Nonnull Project project, @Nullable OrderRootsCache cache) {
    myCache = cache;
    List<OrderEnumerationPolicy> customHandlers = null;
    for (OrderEnumerationPolicy policy : project.getApplication().getExtensionList(OrderEnumerationPolicy.class)) {
      if (policy.isApplicable(project) && (module == null || policy.isApplicable(module))) {
        if (customHandlers == null) {
          customHandlers = new SmartList<>();
        }
        customHandlers.add(policy);
      }
    }
    this.myPolicies = customHandlers == null ? Collections.<OrderEnumerationPolicy>emptyList() : customHandlers;
  }

  @Override
  public OrderEnumerator productionOnly() {
    myProductionOnly = true;
    return this;
  }

  @Override
  public OrderEnumerator compileOnly() {
    myCompileOnly = true;
    return this;
  }

  @Override
  public OrderEnumerator runtimeOnly() {
    myRuntimeOnly = true;
    return this;
  }

  @Override
  public OrderEnumerator withoutSdk() {
    myWithoutJdk = true;
    return this;
  }

  @Override
  public OrderEnumerator withoutLibraries() {
    myWithoutLibraries = true;
    return this;
  }

  @Override
  public OrderEnumerator withoutDepModules() {
    myWithoutDepModules = true;
    return this;
  }

  @Override
  public OrderEnumerator withoutModuleSourceEntries() {
    myWithoutModuleSourceEntries = true;
    return this;
  }

  @Override
  public OrderEnumerator recursively() {
    myRecursively = true;
    return this;
  }

  @Override
  public OrderEnumerator exportedOnly() {
    if (myRecursively) {
      myRecursivelyExportedOnly = true;
    }
    else {
      myExportedOnly = true;
    }
    return this;
  }

  @Override
  public OrderEnumerator satisfying(Predicate<OrderEntry> condition) {
    myCondition = condition;
    return this;
  }

  @Override
  public OrderEnumerator using(@Nonnull RootModelProvider provider) {
    myModulesProvider = provider;
    return this;
  }

  @Override
  public OrderRootsEnumerator classes() {
    return new OrderRootsEnumeratorImpl(this, BinariesOrderRootType.getInstance());
  }

  @Override
  public OrderRootsEnumerator sources() {
    return new OrderRootsEnumeratorImpl(this, SourcesOrderRootType.getInstance());
  }

  @Override
  public OrderRootsEnumerator roots(@Nonnull OrderRootType rootType) {
    return new OrderRootsEnumeratorImpl(this, rootType);
  }

  @Override
  public OrderRootsEnumerator roots(@Nonnull Function<OrderEntry, OrderRootType> rootTypeProvider) {
    return new OrderRootsEnumeratorImpl(this, rootTypeProvider);
  }

  ModuleRootModel getRootModel(Module module) {
    if (myModulesProvider != null) {
      return myModulesProvider.getRootModel(module);
    }
    return ModuleRootManager.getInstance(module);
  }

  public OrderRootsCache getCache() {
    LOG.assertTrue(myCache != null, "Caching is not supported for ModifiableRootModel");
    LOG.assertTrue(myCondition == null, "Caching not supported for OrderEnumerator with 'satisfying(Condition)' option");
    LOG.assertTrue(myModulesProvider == null, "Caching not supported for OrderEnumerator with 'using(ModulesProvider)' option");
    return myCache;
  }

  public int getFlags() {
    int flags = 0;
    if (myProductionOnly) flags |= 1;
    flags <<= 1;
    if (myCompileOnly) flags |= 1;
    flags <<= 1;
    if (myRuntimeOnly) flags |= 1;
    flags <<= 1;
    if (myWithoutJdk) flags |= 1;
    flags <<= 1;
    if (myWithoutLibraries) flags |= 1;
    flags <<= 1;
    if (myWithoutDepModules) flags |= 1;
    flags <<= 1;
    if (myWithoutModuleSourceEntries) flags |= 1;
    flags <<= 1;
    if (myRecursively) flags |= 1;
    flags <<= 1;
    if (myRecursivelyExportedOnly) flags |= 1;
    flags <<= 1;
    if (myExportedOnly) flags |= 1;
    return flags;
  }

  protected void processEntries(final ModuleRootLayer rootModel,
                                Processor<OrderEntry> processor,
                                Set<Module> processed, boolean firstLevel) {
    if (processed != null && !processed.add(rootModel.getModule())) return;

    for (OrderEntry entry : rootModel.getOrderEntries()) {
      if (myCondition != null && !myCondition.test(entry)) continue;

      if (myWithoutJdk && entry instanceof ModuleExtensionWithSdkOrderEntry) continue;
      if (myWithoutLibraries && entry instanceof LibraryOrderEntry) continue;
      if (myWithoutDepModules) {
        if (!myRecursively && entry instanceof ModuleOrderEntry) continue;
        if (entry instanceof ModuleSourceOrderEntry && !isRootModuleModel(((ModuleSourceOrderEntry)entry).getRootModel())) continue;
      }
      if (myWithoutModuleSourceEntries && entry instanceof ModuleSourceOrderEntry) continue;

      boolean exported = !(entry instanceof ModuleExtensionWithSdkOrderEntry);

      if (entry instanceof ExportableOrderEntry) {
        ExportableOrderEntry exportableEntry = (ExportableOrderEntry)entry;
        final DependencyScope scope = exportableEntry.getScope();
        boolean forTestCompile = scope.isForTestCompile() || scope == DependencyScope.RUNTIME && shouldAddRuntimeDependenciesToTestCompilationClasspath();
        if (myCompileOnly && !scope.isForProductionCompile() && !forTestCompile) continue;
        if (myRuntimeOnly && !scope.isForProductionRuntime() && !scope.isForTestRuntime()) continue;
        if (myProductionOnly) {
          if (!scope.isForProductionCompile() && !scope.isForProductionRuntime()
              || myCompileOnly && !scope.isForProductionCompile()
              || myRuntimeOnly && !scope.isForProductionRuntime()) {
            continue;
          }
        }
        exported = exportableEntry.isExported();
      }
      if (!exported) {
        if (myExportedOnly) continue;
        if (myRecursivelyExportedOnly && !firstLevel) continue;
      }

      if (myRecursively && entry instanceof ModuleOrderEntry) {
        ModuleOrderEntry moduleOrderEntry = (ModuleOrderEntry)entry;
        final Module module = moduleOrderEntry.getModule();
        if (module != null && shouldProcessRecursively()) {
          processEntries(getRootModel(module), processor, processed, false);
          continue;
        }
      }

      if (myWithoutDepModules && entry instanceof ModuleOrderEntry) continue;
      if (!processor.process(entry)) {
        return;
      }
    }
  }

  private boolean shouldAddRuntimeDependenciesToTestCompilationClasspath() {
    for (OrderEnumerationPolicy policy : myPolicies) {
      if (policy.shouldAddRuntimeDependenciesToTestCompilationClasspath()) {
        return true;
      }
    }
    return false;
  }

  private boolean shouldProcessRecursively() {
    for (OrderEnumerationPolicy policy : myPolicies) {
      if (!policy.shouldProcessDependenciesRecursively()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void forEachLibrary(@Nonnull final Processor<Library> processor) {
    forEach(orderEntry -> {
      if (orderEntry instanceof LibraryOrderEntry) {
        final Library library = ((LibraryOrderEntry)orderEntry).getLibrary();
        if (library != null) {
          return processor.process(library);
        }
      }
      return true;
    });
  }

  @Override
  public void forEachModule(@Nonnull final Processor<Module> processor) {
    forEach(orderEntry -> {
      if (myRecursively && orderEntry instanceof ModuleSourceOrderEntry) {
        final Module module = ((ModuleSourceOrderEntry)orderEntry).getRootModel().getModule();
        return processor.process(module);
      }
      else if (orderEntry instanceof ModuleOrderEntry && (!myRecursively || !shouldProcessRecursively())) {
        final Module module = ((ModuleOrderEntry)orderEntry).getModule();
        if (module != null) {
          return processor.process(module);
        }
      }
      return true;
    });
  }

  @Override
  public <R> R process(@Nonnull final RootPolicy<R> policy, final R initialValue) {
    final OrderEntryProcessor<R> processor = new OrderEntryProcessor<>(policy, initialValue);
    forEach(processor);
    return processor.myValue;
  }

  boolean shouldIncludeTestsFromDependentModulesToTestClasspath() {
    for (OrderEnumerationPolicy policy : myPolicies) {
      if (!policy.shouldIncludeTestsFromDependentModulesToTestClasspath()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isRuntimeOnly() {
    return myRuntimeOnly;
  }

  @Override
  public boolean isCompileOnly() {
    return myCompileOnly;
  }

  @Override
  public boolean isProductionOnly() {
    return myProductionOnly;
  }

  public boolean isRootModuleModel(@Nonnull ModuleRootModel rootModel) {
    return false;
  }

  /**
   * Runs processor on each module that this enumerator was created on.
   *
   * @param processor processor
   */
  public abstract void processRootModules(@Nonnull Processor<Module> processor);

  private class OrderEntryProcessor<R> implements Processor<OrderEntry> {
    private R myValue;
    private final RootPolicy<R> myPolicy;

    public OrderEntryProcessor(RootPolicy<R> policy, R initialValue) {
      myPolicy = policy;
      myValue = initialValue;
    }

    @Override
    public boolean process(OrderEntry orderEntry) {
      myValue = orderEntry.accept(myPolicy, myValue);
      return true;
    }
  }
}
