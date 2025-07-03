/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.compiler;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.compiler.localize.CompilerLocalize;
import consulo.compiler.scope.CompileScope;
import consulo.compiler.setting.ExcludedEntriesConfiguration;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A "root" class in compiler subsystem - allows one to register a custom compiler or a compilation task,
 * register/unregister a compilation listener and invoke various types of compilations (make, compile, rebuild)
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class CompilerManager {
    @Nonnull
    public static CompilerManager getInstance(@Nonnull Project project) {
        return project.getInstance(CompilerManager.class);
    }

    public static final boolean MAKE_ENABLED = true;
    public static final Key<Key> CONTENT_ID_KEY = Key.create("COMPILATION_CONTENT_ID_CUSTOM_KEY");
    public static final NotificationGroup NOTIFICATION_GROUP =
        NotificationGroup.logOnlyGroup("compiler", CompilerLocalize.notificationGroupCompilerDisplayName());

    public abstract boolean isCompilationActive();

    @Nonnull
    public abstract Collection<FileType> getRegisteredInputTypes(@Nonnull TranslatingCompiler compiler);

    @Nonnull
    public abstract Collection<FileType> getRegisteredOutputTypes(@Nonnull TranslatingCompiler compiler);

    @Nonnull
    @Deprecated
    @DeprecationInfo("Use Compiler extension point")
    public abstract Compiler[] getAllCompilers();

    /**
     * Returns all registered compilers of the specified class.
     *
     * @param compilerClass the class for which the compilers should be returned.
     * @return all registered compilers of the specified class.
     */
    @Nonnull
    public abstract <T extends Compiler> T[] getCompilers(@Nonnull Class<T> compilerClass);

    /**
     * Returns all registered compilers of the specified class that the filter accepts
     *
     * @param compilerClass the class for which the compilers should be returned.
     * @param filter        additional filter to restrict compiler instances
     * @return all registered compilers of the specified class.
     */
    @Nonnull
    public abstract <T extends Compiler> T[] getCompilers(@Nonnull Class<T> compilerClass, Predicate<Compiler> filter);

    /**
     * Checks if files of the specified type can be compiled by one of registered compilers.
     * If the compiler can process files of certain type, it should register this file type within
     * the CompilerManager as a compilable file type.
     *
     * @param type the type to check.
     * @return true if the file type is compilable, false otherwise.
     * @see Compiler#registerCompilableFileTypes(Consumer)
     */
    public abstract boolean isCompilableFileType(@Nonnull FileType type);

    /**
     * Returns the list of all tasks to be executed before compilation.
     *
     * @return all tasks to be executed before compilation.
     */
    @Nonnull
    public abstract List<? extends CompileTask> getBeforeTasks();

    /**
     * Returns the list of all tasks to be executed after compilation.
     *
     * @return all tasks to be executed after compilation.
     */
    @Nonnull
    public abstract List<? extends CompileTask> getAfterTasks();

    /**
     * Compile a set of files.
     *
     * @param files    a list of files to compile. If a VirtualFile is a directory, all containing files are processed.
     *                 Compiler excludes are not honored.
     * @param callback a notification callback, or null if no notifications needed.
     */
    public abstract void compile(@Nonnull VirtualFile[] files, @Nullable CompileStatusNotification callback);

    /**
     * Compile all sources (including test sources) from the module. Compiler excludes are not honored.
     *
     * @param module   a module which sources are to be compiled
     * @param callback a notification callback, or null if no notifications needed
     */
    public abstract void compile(@Nonnull Module module, @Nullable CompileStatusNotification callback);

    /**
     * Compile all files from the scope given.  Compiler excludes are not honored.
     *
     * @param scope    a scope to be compiled
     * @param callback a notification callback, or null if no notifications needed
     */
    public abstract void compile(@Nonnull CompileScope scope, @Nullable CompileStatusNotification callback);

    /**
     * Compile all modified files and all files that depend on them all over the project.
     * Files are compiled according to dependencies between the modules they belong to. Compiler excludes are honored.
     *
     * @param callback a notification callback, or null if no notifications needed
     */
    public abstract void make(@Nullable CompileStatusNotification callback);

    /**
     * Compile all modified files and all files that depend on them from the given module and all modules this module depends on recursively.
     * Files are compiled according to dependencies between the modules they belong to. Compiler excludes are honored.
     *
     * @param module   a module which sources are to be compiled.
     * @param callback a notification callback, or null if no notifications needed.
     */
    public abstract void make(@Nonnull Module module, @Nullable CompileStatusNotification callback);

    /**
     * Compile all modified files and all files that depend on them from the modules and all modules these modules depend on recursively.
     * Files are compiled according to dependencies between the modules they belong to. Compiler excludes are honored. All modules must belong to the same project.
     *
     * @param project  a project modules belong to
     * @param modules  modules to compile
     * @param callback a notification callback, or null if no notifications needed.
     */
    public abstract void make(@Nonnull Project project, @Nonnull Module[] modules, @Nullable CompileStatusNotification callback);

    /**
     * Compile all modified files and all files that depend on them from the scope given.
     * Files are compiled according to dependencies between the modules they belong to. Compiler excludes are honored. All modules must belong to the same project
     *
     * @param scope    a scope to be compiled
     * @param callback a notification callback, or null if no notifications needed
     */
    public abstract void make(@Nonnull CompileScope scope, @Nullable CompileStatusNotification callback);

    /**
     * Compile all modified files and all files that depend on them from the scope given.
     * Files are compiled according to dependencies between the modules they belong to. Compiler excludes are honored. All modules must belong to the same project
     *
     * @param scope    a scope to be compiled
     * @param filter   filter allowing choose what compilers should be executed
     * @param callback a notification callback, or null if no notifications needed
     */
    public abstract void make(@Nonnull CompileScope scope, Predicate<Compiler> filter, @Nullable CompileStatusNotification callback);

    /**
     * Checks if compile scope given is up-to-date
     *
     * @param scope a scope to check
     * @return true if make on the scope specified wouldn't do anything or false if something is to be compiled or deleted
     */
    public abstract boolean isUpToDate(@Nonnull CompileScope scope);

    /**
     * Rebuild the whole project from scratch. Compiler excludes are honored.
     *
     * @param callback a notification callback, or null if no notifications needed
     */
    @RequiredReadAction
    public abstract void rebuild(@Nullable CompileStatusNotification callback);

    /**
     * Execute a custom compile task.
     *
     * @param task           the task to execute.
     * @param scope          compile scope for which the task is executed.
     * @param contentName    the name of a tab in message view where the execution results will be displayed.
     * @param onTaskFinished a runnable to be executed when the task finishes, null if nothing should be executed.
     */
    public abstract void executeTask(
        @Nonnull CompileTask task, @Nonnull CompileScope scope, String contentName,
        @Nullable Runnable onTaskFinished
    );

    /**
     * Checks if the specified file is excluded from compilation.
     *
     * @param file the file to check.
     * @return true if the file is excluded from compilation, false otherwise
     */
    public abstract boolean isExcludedFromCompilation(@Nonnull VirtualFile file);

    public abstract ExcludedEntriesConfiguration getExcludedEntriesConfiguration();

    /*
     * Convetience methods for creating frequently-used compile scopes
     */
    @Nonnull
    public abstract CompileScope createFilesCompileScope(@Nonnull VirtualFile[] files);

    @Nonnull
    @RequiredReadAction
    public CompileScope createProjectCompileScope() {
        return createProjectCompileScope(true);
    }

    @Nonnull
    @RequiredReadAction
    public abstract CompileScope createProjectCompileScope(boolean includeTestScope);

    @Nonnull
    @RequiredReadAction
    public CompileScope createModuleCompileScope(@Nonnull Module module, boolean includeDependentModules) {
        return createModuleCompileScope(module, includeDependentModules, true);
    }

    @Nonnull
    @RequiredReadAction
    public abstract CompileScope createModuleCompileScope(
        @Nonnull Module module,
        boolean includeDependentModules,
        boolean includeTestScope
    );

    @Nonnull
    @RequiredReadAction
    public CompileScope createModulesCompileScope(@Nonnull Module[] modules, boolean includeDependentModules) {
        return createModulesCompileScope(modules, includeDependentModules, true);
    }

    @Nonnull
    @RequiredReadAction
    public abstract CompileScope createModulesCompileScope(
        @Nonnull Module[] modules,
        boolean includeDependentModules,
        boolean includeTestScope
    );

    @Nonnull
    @RequiredReadAction
    public CompileScope createModuleGroupCompileScope(
        @Nonnull Project project,
        @Nonnull Module[] modules,
        boolean includeDependentModules
    ) {
        return createModuleGroupCompileScope(project, modules, includeDependentModules, true);
    }

    @Nonnull
    @RequiredReadAction
    public abstract CompileScope createModuleGroupCompileScope(
        @Nonnull Project project,
        @Nonnull Module[] modules,
        boolean includeDependentModules,
        boolean includeTests
    );
}
