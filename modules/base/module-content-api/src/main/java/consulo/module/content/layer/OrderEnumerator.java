/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.module.content.layer;

import consulo.application.util.function.Processor;
import consulo.content.OrderRootType;
import consulo.content.library.Library;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.RootPolicy;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.PathsList;

import jakarta.annotation.Nonnull;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Interface for convenient processing dependencies of a module or a project. Allows to process {@link OrderEntry}s and collect classes
 * and source roots.<p>
 * <p/>
 * Use {@link #orderEntries(Module)} or {@link ModuleRootModel#orderEntries()} to process dependencies of a module
 * and use {@link #orderEntries(Project)} to process dependencies of all modules in a project.<p>
 * <p/>
 * Note that all configuration methods modify {@link OrderEnumerator} instance instead of creating a new one.
 *
 * @author nik
 * @since 10.0
 */
public abstract class OrderEnumerator {
  /**
   * Skip test dependencies
   *
   * @return this instance
   */
  public abstract OrderEnumerator productionOnly();

  /**
   * Skip runtime-only dependencies
   *
   * @return this instance
   */
  public abstract OrderEnumerator compileOnly();

  /**
   * Skip compile-only dependencies
   *
   * @return this instance
   */
  public abstract OrderEnumerator runtimeOnly();

  public abstract OrderEnumerator withoutSdk();

  public abstract OrderEnumerator withoutLibraries();

  public abstract OrderEnumerator withoutDepModules();

  /**
   * Skip root module's entries
   * @return this
   */
  public abstract OrderEnumerator withoutModuleSourceEntries();

  public OrderEnumerator librariesOnly() {
    return withoutSdk().withoutDepModules().withoutModuleSourceEntries();
  }

  public OrderEnumerator sdkOnly() {
    return withoutDepModules().withoutLibraries().withoutModuleSourceEntries();
  }

  public VirtualFile[] getAllLibrariesAndSdkClassesRoots() {
    return withoutModuleSourceEntries().recursively().exportedOnly().classes().usingCache().getRoots();
  }

  public VirtualFile[] getAllSourceRoots() {
    return recursively().exportedOnly().sources().usingCache().getRoots();
  }

  /**
   * Recursively process modules on which the module depends
   *
   * @return this instance
   */
  public abstract OrderEnumerator recursively();

  /**
   * Skip not exported dependencies. If this method is called after {@link #recursively()} direct non-exported dependencies won't be skipped
   *
   * @return this instance
   */
  public abstract OrderEnumerator exportedOnly();

  /**
   * Process only entries which satisfies the specified condition
   *
   * @param condition filtering condition
   * @return this instance
   */
  public abstract OrderEnumerator satisfying(Predicate<OrderEntry> condition);

  /**
   * Use <code>provider.getRootModel()</code> to process module dependencies
   *
   * @param provider provider
   * @return this instance
   */
  public abstract OrderEnumerator using(@Nonnull RootModelProvider provider);

  /**
   * @return {@link OrderRootsEnumerator} instance for processing classes roots
   */
  public abstract OrderRootsEnumerator classes();

  /**
   * @return {@link OrderRootsEnumerator} instance for processing source roots
   */
  public abstract OrderRootsEnumerator sources();

  /**
   * @param rootType root type
   * @return {@link OrderRootsEnumerator} instance for processing roots of the specified type
   */
  public abstract OrderRootsEnumerator roots(@Nonnull OrderRootType rootType);

  /**
   * @param rootTypeProvider custom root type provider
   * @return {@link OrderRootsEnumerator} instance for processing roots of the provided type
   */
  public abstract OrderRootsEnumerator roots(@Nonnull Function<OrderEntry, OrderRootType> rootTypeProvider);

  /**
   * @return classes roots for all entries processed by this enumerator
   */
  public VirtualFile[] getClassesRoots() {
    return classes().getRoots();
  }

  /**
   * @return source roots for all entries processed by this enumerator
   */
  public VirtualFile[] getSourceRoots() {
    return sources().getRoots();
  }

  /**
   * @return list containing classes roots for all entries processed by this enumerator
   */
  public PathsList getPathsList() {
    return classes().getPathsList();
  }

  /**
   * @return list containing source roots for all entries processed by this enumerator
   */
  public PathsList getSourcePathsList() {
    return sources().getPathsList();
  }

  /**
   * Runs <code>processor.process()</code> for each entry processed by this enumerator.
   *
   * @param processor processor
   */
  public abstract void forEach(@Nonnull Processor<OrderEntry> processor);

  /**
   * Runs <code>processor.process()</code> for each library processed by this enumerator.
   *
   * @param processor processor
   */
  public abstract void forEachLibrary(@Nonnull Processor<Library> processor);

  /**
   * Runs <code>processor.process()</code> for each module processed by this enumerator.
   *
   * @param processor processor
   */
  public abstract void forEachModule(@Nonnull Processor<Module> processor);

  /**
   * Passes order entries to the specified visitor.
   *
   * @param policy       the visitor to accept.
   * @param initialValue the default value to be returned by the visit process.
   * @return the value returned by the visitor.
   * @see OrderEntry#accept(RootPolicy, Object)
   */
  public abstract <R> R process(@Nonnull RootPolicy<R> policy, R initialValue);

  /**
   * Creates new enumerator instance to process dependencies of <code>module</code>
   *
   * @param module module
   * @return new enumerator instance
   */
  @Nonnull
  public static OrderEnumerator orderEntries(@Nonnull Module module) {
    return ModuleRootManager.getInstance(module).orderEntries();
  }

  /**
   * Creates new enumerator instance to process dependencies of all modules in <code>project</code>. Only first level dependencies of
   * modules are processed so {@link #recursively()} option is ignored and {@link #withoutDepModules()} option is forced
   *
   * @param project project
   * @return new enumerator instance
   */
  @Nonnull
  public static OrderEnumerator orderEntries(@Nonnull Project project) {
    return ProjectRootManager.getInstance(project).orderEntries();
  }
}
