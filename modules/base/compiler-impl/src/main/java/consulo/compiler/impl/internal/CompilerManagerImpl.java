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
package consulo.compiler.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.compiler.*;
import consulo.compiler.event.CompilationStatusListener;
import consulo.compiler.impl.internal.scope.CompositeScope;
import consulo.compiler.impl.internal.scope.OneProjectItemCompileScope;
import consulo.compiler.scope.CompileModuleScopeFactory;
import consulo.compiler.scope.CompileScope;
import consulo.compiler.scope.FileIndexCompileScope;
import consulo.compiler.scope.ModuleCompileScope;
import consulo.compiler.setting.ExcludedEntriesConfiguration;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.function.Predicates;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.Predicate;

@Singleton
@ServiceImpl
public class CompilerManagerImpl extends CompilerManager {
    private static class ListenerNotifier implements CompileStatusNotification {
        private final @Nullable CompileStatusNotification myDelegate;
        private CompilationStatusListener myEventPublisher;

        private ListenerNotifier(Project project, @Nullable CompileStatusNotification delegate) {
            myDelegate = delegate;
            myEventPublisher = project.getMessageBus().syncPublisher(CompilationStatusListener.class);
        }

        @Override
        public void finished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
            myEventPublisher.compilationFinished(aborted, errors, warnings, compileContext);

            if (myDelegate != null) {
                myDelegate.finished(aborted, errors, warnings, compileContext);
            }
        }
    }

    private final Project myProject;


    private final Semaphore myCompilationSemaphore = new Semaphore(1, true);

    private Provider<CompilerConfiguration> myCompilerConfigurationProvider;

    @Inject
    public CompilerManagerImpl(Project project, Provider<CompilerConfiguration> compilerConfigurationProvider) {
        myProject = project;
        myCompilerConfigurationProvider = compilerConfigurationProvider;
    }

    @Override
    public Collection<FileType> getRegisteredInputTypes(TranslatingCompiler compiler) {
        return CompilerExtensionCache.get(myProject).getRegisteredInputTypes(compiler);
    }

    @Override
    public Collection<FileType> getRegisteredOutputTypes(TranslatingCompiler compiler) {
        return CompilerExtensionCache.get(myProject).getRegisteredOutputTypes(compiler);
    }

    @Override
    public <T extends Compiler> T[] getCompilers(Class<T> compilerClass) {
        return getCompilers(compilerClass, Predicates.<Compiler>alwaysTrue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Compiler> T[] getCompilers(Class<T> compilerClass, Predicate<Compiler> filter) {
        return CompilerExtensionCache.get(myProject).getCompilers(compilerClass, filter);
    }

    @Override
    public boolean isCompilableFileType(FileType type) {
        return CompilerExtensionCache.get(myProject).isCompilableFileType(type);
    }

    @Override

    public List<? extends CompileTask> getBeforeTasks() {
        return myProject.getApplication().getExtensionList(BeforeCompileTask.class);
    }

    @Override

    public List<? extends CompileTask> getAfterTasks() {
        return myProject.getApplication().getExtensionList(AfterCompilerTask.class);
    }

    @Override
    @RequiredUIAccess
    public void compile(Collection<Path> files, CompileStatusNotification callback) {
        compile(createFilesCompileScope(files), callback);
    }

    @Override
    @RequiredUIAccess
    public void compile(Module module, CompileStatusNotification callback) {
        new CompileDriverImpl(myProject).compile(createModuleCompileScope(module, false), new ListenerNotifier(myProject, callback), true);
    }

    @Override
    @RequiredUIAccess
    public void compile(CompileScope scope, CompileStatusNotification callback) {
        new CompileDriverImpl(myProject).compile(scope, new ListenerNotifier(myProject, callback), false);
    }

    @Override
    @RequiredUIAccess
    public void make(CompileStatusNotification callback) {
        new CompileDriverImpl(myProject).make(createProjectCompileScope(), new ListenerNotifier(myProject, callback));
    }

    @Override
    @RequiredUIAccess
    public void make(Module module, CompileStatusNotification callback) {
        new CompileDriverImpl(myProject).make(createModuleCompileScope(module, true), new ListenerNotifier(myProject, callback));
    }

    @Override
    @RequiredUIAccess
    public void make(Project project, Module[] modules, CompileStatusNotification callback) {
        new CompileDriverImpl(myProject).make(createModuleGroupCompileScope(project, modules, true), new ListenerNotifier(myProject, callback));
    }

    @Override
    @RequiredUIAccess
    public void make(CompileScope scope, CompileStatusNotification callback) {
        new CompileDriverImpl(myProject).make(scope, new ListenerNotifier(myProject, callback));
    }

    @Override
    @RequiredUIAccess
    public void make(CompileScope scope, Predicate<Compiler> filter, @Nullable CompileStatusNotification callback) {
        CompileDriverImpl compileDriver = new CompileDriverImpl(myProject);
        compileDriver.setCompilerFilter(filter);
        compileDriver.make(scope, new ListenerNotifier(myProject, callback));
    }

    @Override
    @RequiredReadAction
    public boolean isUpToDate(CompileScope scope) {
        return new CompileDriverImpl(myProject).isUpToDate(scope);
    }

    @Override
    @RequiredUIAccess
    public void rebuild(CompileStatusNotification callback) {
        new CompileDriverImpl(myProject).rebuild(new ListenerNotifier(myProject, callback));
    }

    @Override
    @RequiredReadAction
    public void executeTask(CompileTask task, CompileScope scope, LocalizeValue contentName, Runnable onTaskFinished) {
        CompileDriverImpl compileDriver = new CompileDriverImpl(myProject);
        compileDriver.executeCompileTask(task, scope, contentName, onTaskFinished);
    }

    @Override
    public boolean isExcludedFromCompilation(Path file) {
        return myCompilerConfigurationProvider.get().isExcludedFromCompilation(file);
    }

    @Override
    public ExcludedEntriesConfiguration getExcludedEntriesConfiguration() {
        return myCompilerConfigurationProvider.get().getExcludedEntriesConfiguration();
    }

    @Override
    public CompileScope createFilesCompileScope(Collection<Path> files) {
        List<CompileScope> scopes = new ArrayList<>(files.size());
        LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
        for (Path file : files) {
            VirtualFile virtualFile = localFileSystem.findFileByNioFile(file);
            if (virtualFile != null) {
                scopes.add(new OneProjectItemCompileScope(myProject, virtualFile));
            }
        }
        return new CompositeScope(scopes);
    }


    @Override
    @RequiredReadAction
    public CompileScope createProjectCompileScope(boolean includeTestScope) {
        Module[] modules = ModuleManager.getInstance(myProject).getModules();
        return createModulesCompileScope(modules, false, includeTestScope);
    }

    @Override

    @RequiredReadAction
    public CompileScope createModuleCompileScope(Module module, boolean includeDependentModules, boolean includeTestScope) {
        FileIndexCompileScope scope = myProject.getApplication()
            .getExtensionPoint(CompileModuleScopeFactory.class)
            .computeSafeIfAny(compileModuleScopeFactory -> compileModuleScopeFactory.createScope(
                module,
                includeDependentModules,
                includeTestScope
            ));
        if (scope != null) {
            return scope;
        }
        return new ModuleCompileScope(module, includeDependentModules, includeTestScope);
    }


    @Override
    @RequiredReadAction
    public CompileScope createModulesCompileScope(
        Module[] modules,
        boolean includeDependentModules,
        boolean includeTestScope
    ) {
        List<CompileScope> list = new ArrayList<>(modules.length);
        for (Module module : modules) {
            list.add(createModuleCompileScope(module, includeDependentModules, includeTestScope));
        }
        return new CompositeScope(list);
    }


    @Override
    @RequiredReadAction
    public CompileScope createModuleGroupCompileScope(
        Project project,
        Module[] modules,
        boolean includeDependentModules,
        boolean includeTestScope
    ) {
        List<CompileScope> list = new ArrayList<>(modules.length);
        for (Module module : modules) {
            list.add(createModuleCompileScope(module, includeDependentModules, includeTestScope));
        }
        return new CompositeScope(list);
    }

    public Semaphore getCompilationSemaphore() {
        return myCompilationSemaphore;
    }

    @Override
    public boolean isCompilationActive() {
        return myCompilationSemaphore.availablePermits() == 0;
    }
}
