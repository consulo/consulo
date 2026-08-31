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
import consulo.application.progress.ProgressIndicator;
import consulo.compiler.scope.CompileScope;
import consulo.content.ContentFolderTypeProvider;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.util.dataholder.UserDataHolder;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * An interface allowing access and modification of the data associated with the current compile session.
 */
public interface CompileContext extends UserDataHolder {
    interface MessageBuilder {
        MessageBuilder url(String url);

        default MessageBuilder optionalUrl(@Nullable String url) {
            return url == null ? this : url(url);
        }

        MessageBuilder position(int row, int column);

        MessageBuilder navigatable(Navigatable navigatable);

        default MessageBuilder optionalNavigatable(@Nullable Navigatable navigatable) {
            return navigatable == null ? this : navigatable(navigatable);
        }

        void add();
    }

    default MessageBuilder newInfo(LocalizeValue message) {
        return newMessage(CompilerMessageCategory.INFORMATION, message);
    }

    default MessageBuilder newWarning(LocalizeValue message) {
        return newMessage(CompilerMessageCategory.WARNING, message);
    }

    default MessageBuilder newError(LocalizeValue message) {
        return newMessage(CompilerMessageCategory.ERROR, message);
    }

    MessageBuilder newMessage(CompilerMessageCategory category, LocalizeValue message);

    /**
     * Returns the count of messages of the specified category added during the current compile session.
     *
     * @param category the category for which messages are requested.
     * @return the number of messages of the specified category
     */
    int getMessageCount(@Nullable CompilerMessageCategory category);

    /**
     * Returns the progress indicator of the compilation process.
     *
     * @return the progress indicator instance.
     */
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
    void requestRebuildNextTime(LocalizeValue message);

    /**
     * A compiler may call this method in order to request complete project rebuild.
     * This may be necessary, for example, when compiler caches are corrupted.
     */
    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    default void requestRebuildNextTime(String message) {
        requestRebuildNextTime(LocalizeValue.of(message));
    }

    /**
     * Returns the module to which the specified file belongs. This method is aware of the file->module mapping
     * for generated files.
     *
     * @param file the file to check.
     * @return the module to which the file belongs
     */
    Module getModuleByFile(Path file);

    /**
     * Returns the source roots for the specified module.
     *
     * @return module's source roots as well as source roots for generated sources that are attributed to the module
     */
    Path[] getSourceRoots(Module module);

    /**
     * Returns the list of all output directories.
     *
     * @return a list of all configured output directories from all modules (including output directories for tests)
     */
    Path[] getAllOutputDirectories();

    /**
     * Returns the output directory for the specified module.
     *
     * @param module the module to check.
     * @return the output directory for the module specified, null if directory is not specified
     */
    @Nullable
    Path getModuleOutputDirectory(Module module);

    /**
     * Returns the test output directory for the specified module.
     *
     * @param module the module to check.
     * @return the tests output directory the module specified, null if not specified. If in Paths settings
     * output directory for tests is not configured explicitly, but the output path is present, the output path will be returned.
     */
    @Nullable
    Path getModuleOutputDirectoryForTests(Module module);

    @Nullable
    Path getOutputForFile(Module module, Path file);

    @Nullable
    Path getOutputForFile(Module module, ContentFolderTypeProvider contentFolderType);

    /**
     * Checks if the compilation is incremental, i.e. triggered by one of "Make" actions.
     *
     * @return true if compilation is incremental.
     */
    boolean isMake();

    boolean isRebuild();

    Project getProject();
}
