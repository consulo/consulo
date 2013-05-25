/*
 * Copyright 2013 Consulo.org
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
package org.consulo.compiler.impl;

import com.intellij.compiler.impl.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.compiler.Compiler;
import com.intellij.openapi.components.*;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.consulo.compiler.CompilerProvider;
import org.consulo.compiler.CompilerSettings;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * @author VISTALL
 * @since 10:44/21.05.13
 */
@State(
  name = "CompilerManager",
  storages = {@Storage(file = StoragePathMacros.PROJECT_FILE),
    @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/compiler.xml", scheme = StorageScheme.DIRECTORY_BASED)}
)
public class CompilerManagerImpl extends CompilerManager implements PersistentStateComponent<Element> {
  private class ListenerNotificator implements CompileStatusNotification {
    private final @Nullable CompileStatusNotification myDelegate;

    private ListenerNotificator(@Nullable CompileStatusNotification delegate) {
      myDelegate = delegate;
    }

    @Override
    public void finished(boolean aborted, int errors, int warnings, final CompileContext compileContext) {
      myEventPublisher.compilationFinished(aborted, errors, warnings, compileContext);
      if (myDelegate != null) {
        myDelegate.finished(aborted, errors, warnings, compileContext);
      }
    }
  }

  private Map<Compiler, CompilerSettings> myCompilers = new LinkedHashMap<Compiler, CompilerSettings>();
  private final Project myProject;
  private final CompilationStatusListener myEventPublisher;
  private final Set<LocalFileSystem.WatchRequest> myWatchRoots;
  private final List<CompileTask> myBeforeTasks = new ArrayList<CompileTask>();
  private final List<CompileTask> myAfterTasks = new ArrayList<CompileTask>();
  private final Semaphore myCompilationSemaphore = new Semaphore(1, true);

  public CompilerManagerImpl(final Project project, final MessageBus messageBus) {
    myProject = project;
    myEventPublisher = messageBus.syncPublisher(CompilerTopics.COMPILATION_STATUS);
    final CompilerProvider[] extensions = CompilerProvider.EP_NAME.getExtensions();
    for (CompilerProvider extension : extensions) {
      myCompilers.put(extension.createCompiler(project), extension.createSettings(project));
    }

    final File projectGeneratedSrcRoot = CompilerPaths.getGeneratedDataDirectory(project);
    projectGeneratedSrcRoot.mkdirs();
    final LocalFileSystem lfs = LocalFileSystem.getInstance();
    myWatchRoots = lfs.addRootsToWatch(Collections.singletonList(FileUtil.toCanonicalPath(projectGeneratedSrcRoot.getPath())), true);
    Disposer.register(project, new Disposable() {
      @Override
      public void dispose() {
        lfs.removeWatchedRoots(myWatchRoots);
        if (ApplicationManager.getApplication()
          .isUnitTestMode()) {    // force cleanup for created compiler system directory with generated sources
          FileUtil.delete(CompilerPaths.getCompilerSystemDirectory(project));
        }
      }
    });
  }

  @Override
  public void addCompiler(@NotNull com.intellij.openapi.compiler.Compiler compiler) {

  }

  @Override
  public void addTranslatingCompiler(@NotNull TranslatingCompiler compiler, Set<FileType> inputTypes, Set<FileType> outputTypes) {

  }

  @NotNull
  @Override
  public Set<FileType> getRegisteredInputTypes(@NotNull TranslatingCompiler compiler) {
    return Collections.emptySet();
  }

  @NotNull
  @Override
  public Set<FileType> getRegisteredOutputTypes(@NotNull TranslatingCompiler compiler) {
    return Collections.emptySet();
  }

  @Override
  public void removeCompiler(@NotNull Compiler compiler) {

  }

  @NotNull
  @Override
  public Compiler[] getAllCompilers() {
    return myCompilers.keySet().toArray(new Compiler[myCompilers.size()]);
  }

  @Override
  @NotNull
  public <T extends Compiler> T[] getCompilers(@NotNull Class<T> compilerClass) {
    return getCompilers(compilerClass, CompilerFilter.ALL);
  }

  @Override
  @NotNull
  public <T extends Compiler> T[] getCompilers(@NotNull Class<T> compilerClass, CompilerFilter filter) {
    final List<T> compilers = new ArrayList<T>(myCompilers.size());
    for (final Compiler item : myCompilers.keySet()) {
      if (compilerClass.isAssignableFrom(item.getClass()) && filter.acceptCompiler(item)) {
        compilers.add((T)item);
      }
    }
    final T[] array = (T[])Array.newInstance(compilerClass, compilers.size());
    return compilers.toArray(array);
  }

  @Override
  public void addCompilableFileType(@NotNull FileType type) {

  }

  @Override
  public void removeCompilableFileType(@NotNull FileType type) {

  }

  @Override
  public boolean isCompilableFileType(@NotNull FileType type) {
    return false;
  }

  @Override
  public final void addBeforeTask(@NotNull CompileTask task) {
    myBeforeTasks.add(task);
  }

  @Override
  public final void addAfterTask(@NotNull CompileTask task) {
    myAfterTasks.add(task);
  }

  @Override
  @NotNull
  public CompileTask[] getBeforeTasks() {
    return myBeforeTasks.toArray(new CompileTask[myBeforeTasks.size()]);
  }

  @Override
  @NotNull
  public CompileTask[] getAfterTasks() {
    return myAfterTasks.toArray(new CompileTask[myAfterTasks.size()]);
  }

  @Override
  public void compile(@NotNull VirtualFile[] files, CompileStatusNotification callback) {
    compile(createFilesCompileScope(files), callback);
  }

  @Override
  public void compile(@NotNull Module module, CompileStatusNotification callback) {
    new CompileDriver(myProject).compile(createModuleCompileScope(module, false), new ListenerNotificator(callback), true);
  }

  @Override
  public void compile(@NotNull CompileScope scope, CompileStatusNotification callback) {
    new CompileDriver(myProject).compile(scope, new ListenerNotificator(callback), false);
  }

  @Override
  public void make(CompileStatusNotification callback) {
    new CompileDriver(myProject).make(createProjectCompileScope(myProject), new ListenerNotificator(callback));
  }

  @Override
  public void make(@NotNull Module module, CompileStatusNotification callback) {
    new CompileDriver(myProject).make(createModuleCompileScope(module, true), new ListenerNotificator(callback));
  }

  @Override
  public void make(@NotNull Project project, @NotNull Module[] modules, CompileStatusNotification callback) {
    new CompileDriver(myProject).make(createModuleGroupCompileScope(project, modules, true), new ListenerNotificator(callback));
  }

  @Override
  public void make(@NotNull CompileScope scope, CompileStatusNotification callback) {
    new CompileDriver(myProject).make(scope, new ListenerNotificator(callback));
  }

  @Override
  public void make(@NotNull CompileScope scope, CompilerFilter filter, @Nullable CompileStatusNotification callback) {
    final CompileDriver compileDriver = new CompileDriver(myProject);
    compileDriver.setCompilerFilter(filter);
    compileDriver.make(scope, new ListenerNotificator(callback));
  }

  @Override
  public boolean isUpToDate(@NotNull final CompileScope scope) {
    return new CompileDriver(myProject).isUpToDate(scope);
  }

  @Override
  public void rebuild(CompileStatusNotification callback) {
    new CompileDriver(myProject).rebuild(new ListenerNotificator(callback));
  }

  @Override
  public void executeTask(@NotNull CompileTask task, @NotNull CompileScope scope, String contentName, Runnable onTaskFinished) {
    final CompileDriver compileDriver = new CompileDriver(myProject);
    compileDriver.executeCompileTask(task, scope, contentName, onTaskFinished);
  }

  private final Map<CompilationStatusListener, MessageBusConnection> myListenerAdapters =
    new HashMap<CompilationStatusListener, MessageBusConnection>();

  @Override
  public void addCompilationStatusListener(@NotNull final CompilationStatusListener listener) {
    final MessageBusConnection connection = myProject.getMessageBus().connect();
    myListenerAdapters.put(listener, connection);
    connection.subscribe(CompilerTopics.COMPILATION_STATUS, listener);
  }

  @Override
  public void addCompilationStatusListener(@NotNull CompilationStatusListener listener, @NotNull Disposable parentDisposable) {
    final MessageBusConnection connection = myProject.getMessageBus().connect(parentDisposable);
    connection.subscribe(CompilerTopics.COMPILATION_STATUS, listener);
  }

  @Override
  public void removeCompilationStatusListener(@NotNull final CompilationStatusListener listener) {
    final MessageBusConnection connection = myListenerAdapters.remove(listener);
    if (connection != null) {
      connection.disconnect();
    }
  }

  @Override
  public boolean isExcludedFromCompilation(@NotNull VirtualFile file) {
    return false;
  }

  @Override
  @NotNull
  public CompileScope createFilesCompileScope(@NotNull final VirtualFile[] files) {
    CompileScope[] scopes = new CompileScope[files.length];
    for (int i = 0; i < files.length; i++) {
      scopes[i] = new OneProjectItemCompileScope(myProject, files[i]);
    }
    return new CompositeScope(scopes);
  }

  @Override
  @NotNull
  public CompileScope createModuleCompileScope(@NotNull final Module module, final boolean includeDependentModules) {
    return new ModuleCompileScope(module, includeDependentModules);
  }

  @Override
  @NotNull
  public CompileScope createModulesCompileScope(@NotNull final Module[] modules, final boolean includeDependentModules) {
    return new ModuleCompileScope(myProject, modules, includeDependentModules);
  }

  @Override
  @NotNull
  public CompileScope createModuleGroupCompileScope(@NotNull final Project project,
                                                    @NotNull final Module[] modules,
                                                    final boolean includeDependentModules) {
    return new ModuleCompileScope(project, modules, includeDependentModules);
  }

  @Override
  @NotNull
  public CompileScope createProjectCompileScope(@NotNull final Project project) {
    return new ProjectCompileScope(project);
  }

  @Override
  public boolean isValidationEnabled(Module moduleType) {
    return true;
  }

  @NotNull
  @Override
  public <T extends Compiler> CompilerSettings getSettings(T compiler) {
    return myCompilers.get(compiler);
  }

  @Nullable
  @Override
  public Element getState() {
    return new Element("test");
  }

  @Override
  public void loadState(Element state) {

  }

  public Semaphore getCompilationSemaphore() {
    return myCompilationSemaphore;
  }

  @Override
  public boolean isCompilationActive() {
    return myCompilationSemaphore.availablePermits() == 0;
  }
}
