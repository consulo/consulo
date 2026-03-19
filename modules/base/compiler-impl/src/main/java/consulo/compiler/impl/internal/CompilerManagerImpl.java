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
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.io.FileUtil;
import consulo.util.lang.function.Predicates;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import java.io.File;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.function.Predicate;

@Singleton
@State(name = "CompilerManager", storages = @Storage("compiler.xml"))
@ServiceImpl
public class CompilerManagerImpl extends CompilerManager implements PersistentStateComponent<Element> {
    private class ListenerNotificator implements CompileStatusNotification {
        private final @Nullable CompileStatusNotification myDelegate;

        private ListenerNotificator(@Nullable CompileStatusNotification delegate) {
            myDelegate = delegate;
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

    private final ExcludedEntriesConfiguration myExcludedEntriesConfiguration = new ExcludedEntriesConfiguration();

    private CompilationStatusListener myEventPublisher;
    private Set<LocalFileSystem.WatchRequest> myWatchRoots;
    private final Semaphore myCompilationSemaphore = new Semaphore(1, true);

    @Inject
    public CompilerManagerImpl(Project project) {
        myProject = project;
        Disposer.register(project, myExcludedEntriesConfiguration);

        if (myProject.isDefault()) {
            return;
        }

        myEventPublisher = project.getMessageBus().syncPublisher(CompilationStatusListener.class);

        File projectGeneratedSrcRoot = CompilerPaths.getGeneratedDataDirectory(project);
        projectGeneratedSrcRoot.mkdirs();
        LocalFileSystem lfs = LocalFileSystem.getInstance();
        myWatchRoots = lfs.addRootsToWatch(Collections.singletonList(FileUtil.toCanonicalPath(projectGeneratedSrcRoot.getPath())), true);
        Disposer.register(
            project,
            () -> {
                lfs.removeWatchedRoots(myWatchRoots);
                if (myProject.getApplication().isUnitTestMode()) {
                    // force cleanup for created compiler system directory with generated sources
                    FileUtil.delete(CompilerPaths.getCompilerSystemDirectory(project));
                }
            }
        );
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
    public Compiler[] getAllCompilers() {
        return myProject.getExtensionPoint(Compiler.class).getExtensions();
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
    public void compile(VirtualFile[] files, CompileStatusNotification callback) {
        compile(createFilesCompileScope(files), callback);
    }

    @Override
    @RequiredUIAccess
    public void compile(Module module, CompileStatusNotification callback) {
        new CompileDriverImpl(myProject).compile(createModuleCompileScope(module, false), new ListenerNotificator(callback), true);
    }

    @Override
    @RequiredUIAccess
    public void compile(CompileScope scope, CompileStatusNotification callback) {
        new CompileDriverImpl(myProject).compile(scope, new ListenerNotificator(callback), false);
    }

    @Override
    @RequiredUIAccess
    public void make(CompileStatusNotification callback) {
        new CompileDriverImpl(myProject).make(createProjectCompileScope(), new ListenerNotificator(callback));
    }

    @Override
    @RequiredUIAccess
    public void make(Module module, CompileStatusNotification callback) {
        new CompileDriverImpl(myProject).make(createModuleCompileScope(module, true), new ListenerNotificator(callback));
    }

    @Override
    @RequiredUIAccess
    public void make(Project project, Module[] modules, CompileStatusNotification callback) {
        new CompileDriverImpl(myProject).make(createModuleGroupCompileScope(project, modules, true), new ListenerNotificator(callback));
    }

    @Override
    @RequiredUIAccess
    public void make(CompileScope scope, CompileStatusNotification callback) {
        new CompileDriverImpl(myProject).make(scope, new ListenerNotificator(callback));
    }

    @Override
    @RequiredUIAccess
    public void make(CompileScope scope, Predicate<Compiler> filter, @Nullable CompileStatusNotification callback) {
        CompileDriverImpl compileDriver = new CompileDriverImpl(myProject);
        compileDriver.setCompilerFilter(filter);
        compileDriver.make(scope, new ListenerNotificator(callback));
    }

    @Override
    @RequiredReadAction
    public boolean isUpToDate(CompileScope scope) {
        return new CompileDriverImpl(myProject).isUpToDate(scope);
    }

    @Override
    @RequiredUIAccess
    public void rebuild(CompileStatusNotification callback) {
        new CompileDriverImpl(myProject).rebuild(new ListenerNotificator(callback));
    }

    @Override
    @RequiredReadAction
    public void executeTask(CompileTask task, CompileScope scope, LocalizeValue contentName, Runnable onTaskFinished) {
        CompileDriverImpl compileDriver = new CompileDriverImpl(myProject);
        compileDriver.executeCompileTask(task, scope, contentName, onTaskFinished);
    }

    @Override
    public boolean isExcludedFromCompilation(VirtualFile file) {
        return myExcludedEntriesConfiguration.isExcluded(file);
    }

    @Override
    public ExcludedEntriesConfiguration getExcludedEntriesConfiguration() {
        return myExcludedEntriesConfiguration;
    }

    @Override
    
    public CompileScope createFilesCompileScope(VirtualFile[] files) {
        CompileScope[] scopes = new CompileScope[files.length];
        for (int i = 0; i < files.length; i++) {
            scopes[i] = new OneProjectItemCompileScope(myProject, files[i]);
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

    @Nullable
    @Override
    @RequiredReadAction
    public Element getState() {
        Element state = new Element("state");
        CompilerConfigurationImpl configuration = (CompilerConfigurationImpl) CompilerConfiguration.getInstance(myProject);
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

        CompilerConfigurationImpl configuration = (CompilerConfigurationImpl) CompilerConfiguration.getInstance(myProject);
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
