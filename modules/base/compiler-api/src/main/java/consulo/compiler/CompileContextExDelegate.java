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

import consulo.application.progress.ProgressIndicator;
import consulo.compiler.scope.CompileScope;
import consulo.content.ContentFolderTypeProvider;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.dataholder.Key;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

/**
 * @author Eugene Zhuravlev
 * @since 2007-12-04
 */
public class CompileContextExDelegate implements CompileContextEx {
    private final CompileContextEx myDelegate;

    public CompileContextExDelegate(CompileContextEx delegate) {
        myDelegate = delegate;
    }

    @Override
    public Project getProject() {
        return myDelegate.getProject();
    }

    @Override
    public CompositeDependencyCache getDependencyCache() {
        return myDelegate.getDependencyCache();
    }

    @Override
    public Path getSourceFileByOutputFile(Path outputFile) {
        return myDelegate.getSourceFileByOutputFile(outputFile);
    }

    @Override
    public void addMessage(CompilerMessage message) {
        myDelegate.addMessage(message);
    }

    @Override
    public Set<Path> getTestOutputDirectories() {
        return myDelegate.getTestOutputDirectories();
    }

    @Override
    public boolean isInTestSourceContent(Path fileOrDir) {
        return myDelegate.isInTestSourceContent(fileOrDir);
    }

    @Override
    public boolean isInSourceContent(Path fileOrDir) {
        return myDelegate.isInSourceContent(fileOrDir);
    }

    @Override
    public void addScope(CompileScope additionalScope) {
        myDelegate.addScope(additionalScope);
    }

    @Override
    public MessageBuilder newMessage(CompilerMessageCategory category, LocalizeValue message) {
        return myDelegate.newMessage(category, message);
    }

    @Override
    public int getMessageCount(CompilerMessageCategory category) {
        return myDelegate.getMessageCount(category);
    }

    @Override
    public ProgressIndicator getProgressIndicator() {
        return myDelegate.getProgressIndicator();
    }

    @Override
    public CompileScope getCompileScope() {
        return myDelegate.getCompileScope();
    }

    @Override
    public void requestRebuildNextTime(LocalizeValue message) {
        myDelegate.requestRebuildNextTime(message);
    }

    @Override
    public Module getModuleByFile(Path file) {
        return myDelegate.getModuleByFile(file);
    }

    @Override
    public Path[] getSourceRoots(Module module) {
        return myDelegate.getSourceRoots(module);
    }

    @Override
    public Path[] getAllOutputDirectories() {
        return myDelegate.getAllOutputDirectories();
    }

    @Override
    public Path getModuleOutputDirectory(Module module) {
        return myDelegate.getModuleOutputDirectory(module);
    }

    @Override
    public Path getModuleOutputDirectoryForTests(Module module) {
        return myDelegate.getModuleOutputDirectoryForTests(module);
    }

    @Override
    public Path getOutputForFile(Module module, Path file) {
        return myDelegate.getOutputForFile(module, file);
    }

    @Override
    public @Nullable Path getOutputForFile(Module module, ContentFolderTypeProvider contentFolderType) {
        return myDelegate.getOutputForFile(module, contentFolderType);
    }

    @Override
    public boolean isMake() {
        return myDelegate.isMake();
    }

    @Override
    public boolean isRebuild() {
        return myDelegate.isRebuild();
    }

    @Override
    public <T> T getUserData(Key<T> key) {
        return myDelegate.getUserData(key);
    }

    @Override
    public <T> void putUserData(Key<T> key, T value) {
        myDelegate.putUserData(key, value);
    }

    @Override
    public void recalculateOutputDirs() {
        myDelegate.recalculateOutputDirs();
    }

    @Override
    public void markGenerated(Collection<Path> files) {
        myDelegate.markGenerated(files);
    }

    @Override
    public boolean isGenerated(Path file) {
        return myDelegate.isGenerated(file);
    }

    @Override
    public long getStartCompilationStamp() {
        return myDelegate.getStartCompilationStamp();
    }

    @Override
    public void assignModule(Path root, Module module, boolean isTestSource, Compiler compiler) {
        myDelegate.assignModule(root, module, isTestSource, compiler);
    }
}
