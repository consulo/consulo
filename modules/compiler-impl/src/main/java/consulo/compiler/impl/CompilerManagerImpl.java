/*
 * Copyright 2013-2016 consulo.io
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
package consulo.compiler.impl;

import com.intellij.compiler.ModuleCompilerUtil;
import com.intellij.compiler.impl.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.compiler.Compiler;
import com.intellij.openapi.compiler.options.ExcludedEntriesConfiguration;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Chunk;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.graph.Graph;
import com.intellij.util.graph.GraphGenerator;
import com.intellij.util.graph.InboundSemiGraph;
import com.intellij.util.messages.MessageBus;
import consulo.annotations.RequiredReadAction;
import consulo.compiler.CompilerConfiguration;
import consulo.compiler.CompilerConfigurationImpl;
import gnu.trove.THashSet;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.Semaphore;

@State(name = "CompilerManager", storages = @Storage(value = "compiler.xml"))
public class CompilerManagerImpl extends CompilerManager implements PersistentStateComponent<Element> {
  private class ListenerNotificator implements CompileStatusNotification {
    @Nullable
    private final CompileStatusNotification myDelegate;

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

  private final Project myProject;

  private final ExcludedEntriesConfiguration myExcludedEntriesConfiguration = new ExcludedEntriesConfiguration();
  private final List<TranslatingCompiler> myTranslatingCompilers = new ArrayList<>();
  private final List<Compiler> myCompilers = new ArrayList<>();
  private final Map<TranslatingCompiler, Collection<FileType>> myTranslatingCompilerInputFileTypes = new HashMap<>();
  private final Map<TranslatingCompiler, Collection<FileType>> myTranslatingCompilerOutputFileTypes = new HashMap<>();

  private CompilationStatusListener myEventPublisher;
  private Set<LocalFileSystem.WatchRequest> myWatchRoots;
  private final Semaphore myCompilationSemaphore = new Semaphore(1, true);

  private final Set<FileType> myCompilableFileTypes = new THashSet<>();
  private Compiler[] myAllCompilers;

  public CompilerManagerImpl(final Project project, final MessageBus messageBus) {
    myProject = project;

    if (myProject.isDefault()) {
      return;
    }

    myEventPublisher = messageBus.syncPublisher(CompilerTopics.COMPILATION_STATUS);

    List<TranslatingCompiler> translatingCompilers = new ArrayList<>();
    for (Compiler compiler : Compiler.EP_NAME.getExtensions(project)) {
      compiler.registerCompilableFileTypes(myCompilableFileTypes::add);

      if (compiler instanceof TranslatingCompiler) {
        TranslatingCompiler translatingCompiler = (TranslatingCompiler)compiler;

        translatingCompilers.add(translatingCompiler);

        myTranslatingCompilerInputFileTypes.put(translatingCompiler, Arrays.asList(translatingCompiler.getInputFileTypes()));
        myTranslatingCompilerOutputFileTypes.put(translatingCompiler, Arrays.asList(translatingCompiler.getOutputFileTypes()));
      }
      else {
        myCompilers.add(compiler);
      }
    }

    final List<Chunk<TranslatingCompiler>> chunks = ModuleCompilerUtil.getSortedChunks(createCompilerGraph(translatingCompilers));

    for (Chunk<TranslatingCompiler> chunk : chunks) {
      myTranslatingCompilers.addAll(chunk.getNodes());
    }

    final File projectGeneratedSrcRoot = CompilerPaths.getGeneratedDataDirectory(project);
    projectGeneratedSrcRoot.mkdirs();
    final LocalFileSystem lfs = LocalFileSystem.getInstance();
    myWatchRoots = lfs.addRootsToWatch(Collections.singletonList(FileUtil.toCanonicalPath(projectGeneratedSrcRoot.getPath())), true);
    Disposer.register(project, () -> {
      lfs.removeWatchedRoots(myWatchRoots);
      if (ApplicationManager.getApplication().isUnitTestMode()) {    // force cleanup for created compiler system directory with generated sources
        FileUtil.delete(CompilerPaths.getCompilerSystemDirectory(project));
      }
    });
  }

  @NotNull
  @Override
  public Collection<FileType> getRegisteredInputTypes(@NotNull TranslatingCompiler compiler) {
    final Collection<FileType> fileTypes = myTranslatingCompilerInputFileTypes.get(compiler);
    return fileTypes == null ? Collections.<FileType>emptyList() : fileTypes;
  }

  @NotNull
  @Override
  public Collection<FileType> getRegisteredOutputTypes(@NotNull TranslatingCompiler compiler) {
    final Collection<FileType> fileTypes = myTranslatingCompilerOutputFileTypes.get(compiler);
    return fileTypes == null ? Collections.<FileType>emptyList() : fileTypes;
  }

  @NotNull
  @Override
  public Compiler[] getAllCompilers() {
    if (myAllCompilers == null) {
      List<Compiler> list = new ArrayList<>(myCompilers.size() + myTranslatingCompilers.size());
      list.addAll(myCompilers);
      list.addAll(myTranslatingCompilers);
      myAllCompilers = list.toArray(new Compiler[list.size()]);
    }
    return myAllCompilers;
  }

  @Override
  @NotNull
  public <T extends Compiler> T[] getCompilers(@NotNull Class<T> compilerClass) {
    return getCompilers(compilerClass, Conditions.<Compiler>alwaysTrue());
  }

  @Override
  @NotNull
  @SuppressWarnings("unchecked")
  public <T extends Compiler> T[] getCompilers(@NotNull Class<T> compilerClass, Condition<Compiler> filter) {
    final List<T> compilers = new ArrayList<>(myCompilers.size());
    for (final Compiler item : myCompilers) {
      if (compilerClass.isAssignableFrom(item.getClass()) && filter.value(item)) {
        compilers.add((T)item);
      }
    }
    for (final Compiler item : myTranslatingCompilers) {
      if (compilerClass.isAssignableFrom(item.getClass()) && filter.value(item)) {
        compilers.add((T)item);
      }
    }
    final T[] array = (T[])Array.newInstance(compilerClass, compilers.size());
    return compilers.toArray(array);
  }

  private Graph<TranslatingCompiler> createCompilerGraph(final List<TranslatingCompiler> compilers) {
    return GraphGenerator.generate(new InboundSemiGraph<TranslatingCompiler>() {
      @Override
      public Collection<TranslatingCompiler> getNodes() {
        return compilers;
      }

      @Override
      public Iterator<TranslatingCompiler> getIn(TranslatingCompiler compiler) {
        final Collection<FileType> compilerInput = myTranslatingCompilerInputFileTypes.get(compiler);
        if (compilerInput == null || compilerInput.isEmpty()) {
          return Collections.<TranslatingCompiler>emptySet().iterator();
        }

        final Set<TranslatingCompiler> inCompilers = new HashSet<>();

        for (Map.Entry<TranslatingCompiler, Collection<FileType>> entry : myTranslatingCompilerOutputFileTypes.entrySet()) {
          final Collection<FileType> outputs = entry.getValue();
          TranslatingCompiler comp = entry.getKey();
          if (outputs != null && ContainerUtil.intersects(compilerInput, outputs)) {
            inCompilers.add(comp);
          }
        }
        return inCompilers.iterator();
      }
    });
  }

  @Override
  public boolean isCompilableFileType(@NotNull FileType type) {
    return myCompilableFileTypes.contains(type);
  }

  @Override
  @NotNull
  public CompileTask[] getBeforeTasks() {
    return CompileTask.BEFORE_EP_NAME.getExtensions();
  }

  @Override
  @NotNull
  public CompileTask[] getAfterTasks() {
    return CompileTask.AFTER_EP_NAME.getExtensions();
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
    new CompileDriver(myProject).make(createProjectCompileScope(), new ListenerNotificator(callback));
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
  public void make(@NotNull CompileScope scope, Condition<Compiler> filter, @Nullable CompileStatusNotification callback) {
    final CompileDriver compileDriver = new CompileDriver(myProject);
    compileDriver.setCompilerFilter(filter);
    compileDriver.make(scope, new ListenerNotificator(callback));
  }

  @Override
  public boolean isUpToDate(@NotNull final CompileScope scope) {
    return new CompileDriver(myProject).isUpToDate(scope);
  }

  @Override
  @RequiredReadAction
  public void rebuild(CompileStatusNotification callback) {
    new CompileDriver(myProject).rebuild(new ListenerNotificator(callback));
  }

  @Override
  public void executeTask(@NotNull CompileTask task, @NotNull CompileScope scope, String contentName, Runnable onTaskFinished) {
    final CompileDriver compileDriver = new CompileDriver(myProject);
    compileDriver.executeCompileTask(task, scope, contentName, onTaskFinished);
  }

  @Override
  public void addCompilationStatusListener(@NotNull final CompilationStatusListener listener) {
    myProject.getMessageBus().connect().subscribe(CompilerTopics.COMPILATION_STATUS, listener);
  }

  @Override
  public void addCompilationStatusListener(@NotNull CompilationStatusListener listener, @NotNull Disposable parentDisposable) {
    myProject.getMessageBus().connect(parentDisposable).subscribe(CompilerTopics.COMPILATION_STATUS, listener);
  }

  @Override
  public void removeCompilationStatusListener(@NotNull final CompilationStatusListener listener) {
  }

  @Override
  public boolean isExcludedFromCompilation(@NotNull VirtualFile file) {
    return myExcludedEntriesConfiguration.isExcluded(file);
  }

  @Override
  public ExcludedEntriesConfiguration getExcludedEntriesConfiguration() {
    return myExcludedEntriesConfiguration;
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

  @NotNull
  @Override
  @RequiredReadAction
  public CompileScope createProjectCompileScope() {
    Module[] modules = ModuleManager.getInstance(myProject).getModules();
    return createModulesCompileScope(modules, false);
  }

  @Override
  @NotNull
  public CompileScope createModuleCompileScope(@NotNull final Module module, final boolean includeDependentModules) {
    for (CompileModuleScopeFactory compileModuleScopeFactory : CompileModuleScopeFactory.EP_NAME.getExtensions()) {
      FileIndexCompileScope scope = compileModuleScopeFactory.createScope(module, includeDependentModules);
      if (scope != null) {
        return scope;
      }
    }
    return new ModuleCompileScope(module, includeDependentModules);
  }

  @Override
  @NotNull
  public CompileScope createModulesCompileScope(@NotNull final Module[] modules, final boolean includeDependentModules) {
    List<CompileScope> list = new ArrayList<>(modules.length);
    for (Module module : modules) {
      list.add(createModuleCompileScope(module, includeDependentModules));
    }
    return new CompositeScope(list);
  }

  @Override
  @NotNull
  public CompileScope createModuleGroupCompileScope(@NotNull final Project project, @NotNull final Module[] modules, final boolean includeDependentModules) {
    List<CompileScope> list = new ArrayList<>(modules.length);
    for (Module module : modules) {
      list.add(createModuleCompileScope(module, includeDependentModules));
    }
    return new CompositeScope(list);
  }

  @Override
  public boolean isValidationEnabled(Module moduleType) {
    return true;
  }

  @Nullable
  @Override
  public Element getState() {
    final Element state = new Element("state");
    CompilerConfigurationImpl configuration = (CompilerConfigurationImpl)CompilerConfiguration.getInstance(myProject);
    configuration.getState(state);

    if (!myExcludedEntriesConfiguration.isEmpty()) {
      Element element = new Element("exclude-from-compilation");
      myExcludedEntriesConfiguration.writeExternal(element);
      state.addContent(element);
    }
    return state;
  }

  @Override
  public void loadState(Element state) {
    if (!myProject.isInitialized()) {
      throw new IllegalArgumentException("Project is not initialized yet. Please do not call CompilerManager inside #initCompoment()");
    }

    Element exclude = state.getChild("exclude-from-compilation");
    if (exclude != null) {
      myExcludedEntriesConfiguration.readExternal(exclude);
    }

    CompilerConfigurationImpl configuration = (CompilerConfigurationImpl)CompilerConfiguration.getInstance(myProject);
    configuration.loadState(state);
  }

  public Semaphore getCompilationSemaphore() {
    return myCompilationSemaphore;
  }

  @Override
  public boolean isCompilationActive() {
    return myCompilationSemaphore.availablePermits() == 0;
  }
}
