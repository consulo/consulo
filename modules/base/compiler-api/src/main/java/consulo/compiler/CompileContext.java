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
import consulo.util.dataholder.UserDataHolder;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * An interface allowing access and modification of the data associated with the current compile session.
 */
public interface CompileContext extends UserDataHolder {
    /**
     * Allows to add a message to be shown in Compiler message view.
     * If correct url, line and column numbers are supplied, the navigation to the specified file is available from the view.
     *
     * @param category  the category of a message (information, error, warning).
     * @param message   the text of the message.
     * @param url       a url to the file to which the message applies, null if not available.
     * @param lineNum   a line number, -1 if not available.
     * @param columnNum a column number, -1 if not available.
     */
    void addMessage(CompilerMessageCategory category, String message, @Nullable String url, int lineNum, int columnNum);

    /**
     * Allows to add a message to be shown in Compiler message view, with a specified Navigatable
     * that is used to navigate to the error location.
     *
     * @param category    the category of a message (information, error, warning).
     * @param message     the text of the message.
     * @param url         a url to the file to which the message applies, null if not available.
     * @param lineNum     a line number, -1 if not available.
     * @param columnNum   a column number, -1 if not available.
     * @param navigatable the navigatable pointing to the error location.
     * @since 6.0
     */
    void addMessage(
        CompilerMessageCategory category, String message, @Nullable String url, int lineNum, int columnNum,
        Navigatable navigatable
    );

    /**
     * Returns all messages of the specified category added during the current compile session.
     *
     * @param category the category for which messages are requested.
     * @return all compiler messages of the specified category
     */
    CompilerMessage[] getMessages(CompilerMessageCategory category);

    /**
     * Returns the count of messages of the specified category added during the current compile session.
     *
     * @param category the category for which messages are requested.
     * @return the number of messages of the specified category
     */
    int getMessageCount(CompilerMessageCategory category);

    /**
     * Returns the progress indicator of the compilation process.
     *
     * @return the progress indicator instance.
     */
    @Nonnull
    ProgressIndicator getProgressIndicator();

    /**
     * Returns the current compile scope.
     *
     * @return current compile scope
     */
    CompileScope getCompileScope();

    /**
     * A compiler may call this method in order to request complete project rebuild.
     * This may be necessary, for example, when compiler caches are corrupted.
     */
    void requestRebuildNextTime(String message);

    /**
     * Returns the module to which the specified file belongs. This method is aware of the file->module mapping
     * for generated files.
     *
     * @param file the file to check.
     * @return the module to which the file belongs
     */
    Module getModuleByFile(VirtualFile file);

    /**
     * Returns the source roots for the specified module.
     *
     * @return module's source roots as well as source roots for generated sources that are attributed to the module
     */
    VirtualFile[] getSourceRoots(Module module);

    /**
     * Returns the list of all output directories.
     *
     * @return a list of all configured output directories from all modules (including output directories for tests)
     */
    VirtualFile[] getAllOutputDirectories();

    /**
     * Returns the output directory for the specified module.
     *
     * @param module the module to check.
     * @return the output directory for the module specified, null if corresponding VirtualFile is not valid or directory not specified
     */
    @jakarta.annotation.Nullable
    @Deprecated
    VirtualFile getModuleOutputDirectory(Module module);

    /**
     * Returns the test output directory for the specified module.
     *
     * @param module the module to check.
     * @return the tests output directory the module specified, null if corresponding VirtualFile is not valid. If in Paths settings
     * output directory for tests is not configured explicitly, but the output path is present, the output path will be returned.
     */
    @Nullable
    @Deprecated
    VirtualFile getModuleOutputDirectoryForTests(Module module);

    @Nullable
    VirtualFile getOutputForFile(Module module, VirtualFile virtualFile);

    @Nullable
    VirtualFile getOutputForFile(Module module, ContentFolderTypeProvider contentFolderType);

    /**
     * Checks if the compilation is incremental, i.e. triggered by one of "Make" actions.
     *
     * @return true if compilation is incremental.
     */
    boolean isMake();

    boolean isRebuild();

    Project getProject();
}
