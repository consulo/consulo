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
import consulo.module.Module;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
    public VirtualFile getSourceFileByOutputFile(VirtualFile outputFile) {
        return myDelegate.getSourceFileByOutputFile(outputFile);
    }

    @Override
    public void addMessage(CompilerMessage message) {
        myDelegate.addMessage(message);
    }

    @Override
    @Nonnull
    public Set<VirtualFile> getTestOutputDirectories() {
        return myDelegate.getTestOutputDirectories();
    }

    @Override
    public boolean isInTestSourceContent(@Nonnull VirtualFile fileOrDir) {
        return myDelegate.isInTestSourceContent(fileOrDir);
    }

    @Override
    public boolean isInSourceContent(@Nonnull VirtualFile fileOrDir) {
        return myDelegate.isInSourceContent(fileOrDir);
    }

    @Override
    public void addScope(CompileScope additionalScope) {
        myDelegate.addScope(additionalScope);
    }

    @Override
    public void addMessage(
        CompilerMessageCategory category,
        String message,
        @Nullable String url,
        int lineNum,
        int columnNum
    ) {
        myDelegate.addMessage(category, message, url, lineNum, columnNum);
    }

    @Override
    public void addMessage(
        CompilerMessageCategory category,
        String message,
        @Nullable String url,
        int lineNum,
        int columnNum,
        Navigatable navigatable
    ) {
        myDelegate.addMessage(category, message, url, lineNum, columnNum, navigatable);
    }

    @Override
    public CompilerMessage[] getMessages(CompilerMessageCategory category) {
        return myDelegate.getMessages(category);
    }

    @Override
    public int getMessageCount(CompilerMessageCategory category) {
        return myDelegate.getMessageCount(category);
    }

    @Nonnull
    @Override
    public ProgressIndicator getProgressIndicator() {
        return myDelegate.getProgressIndicator();
    }

    @Override
    public CompileScope getCompileScope() {
        return myDelegate.getCompileScope();
    }

    @Override
    public void requestRebuildNextTime(String message) {
        myDelegate.requestRebuildNextTime(message);
    }

    @Override
    public Module getModuleByFile(VirtualFile file) {
        return myDelegate.getModuleByFile(file);
    }

    @Override
    public VirtualFile[] getSourceRoots(Module module) {
        return myDelegate.getSourceRoots(module);
    }

    @Override
    public VirtualFile[] getAllOutputDirectories() {
        return myDelegate.getAllOutputDirectories();
    }

    @Override
    public VirtualFile getModuleOutputDirectory(Module module) {
        return myDelegate.getModuleOutputDirectory(module);
    }

    @Override
    public VirtualFile getModuleOutputDirectoryForTests(Module module) {
        return myDelegate.getModuleOutputDirectoryForTests(module);
    }

    @Override
    public VirtualFile getOutputForFile(Module module, VirtualFile virtualFile) {
        return myDelegate.getOutputForFile(module, virtualFile);
    }

    @Nullable
    @Override
    public VirtualFile getOutputForFile(Module module, ContentFolderTypeProvider contentFolderType) {
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
    public <T> T getUserData(@Nonnull Key<T> key) {
        return myDelegate.getUserData(key);
    }

    @Override
    public <T> void putUserData(@Nonnull Key<T> key, T value) {
        myDelegate.putUserData(key, value);
    }

    @Override
    public void recalculateOutputDirs() {
        myDelegate.recalculateOutputDirs();
    }

    @Override
    public void markGenerated(Collection<VirtualFile> files) {
        myDelegate.markGenerated(files);
    }

    @Override
    public boolean isGenerated(VirtualFile file) {
        return myDelegate.isGenerated(file);
    }

    @Override
    public long getStartCompilationStamp() {
        return myDelegate.getStartCompilationStamp();
    }

    @Override
    public void assignModule(@Nonnull VirtualFile root, @Nonnull Module module, boolean isTestSource, Compiler compiler) {
        myDelegate.assignModule(root, module, isTestSource, compiler);
    }
}
