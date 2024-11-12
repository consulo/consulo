// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.module.content;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;

/**
 * Represents a non-linear operation which is executed before indexing process
 * is started {@link PushedFilePropertiesUpdater#pushAllPropertiesNow()}.
 * <br />
 * During this process any pusher is allowed to set some properties to any of files being indexed.
 * <br />
 * Most frequently property represents some kind of "language level"
 * which is in most cases required to determine the algorithm of stub and other indexes building.
 * <br />
 * After property was pushed it can be retrieved any time using {@link FilePropertyPusher#getFileDataKey()}.
 * <br/>
 * The computation of the value (simplified) for a given {@link VirtualFile} works as follows:
 * <ul>
 * <li> the {@link #getImmediateValue(Project, VirtualFile)} is executed,
 * a non-null result is returned</li>
 * <li> the {@link #getImmediateValue(Module)} is executed for the module of a file,
 * a non-null result is retuned</li>
 * <li> iterate all parent file that is in content root of the file
 * and try to return first non-null
 * result of the {@link #getImmediateValue(Project, VirtualFile)} call
 * </li>
 * <li> try calling the {@link #getImmediateValue(Project, VirtualFile)} with {@code null} as {@link VirtualFile}
 * and return a non-null value</li>
 * <li> fallback to {@link #getDefaultValue()} and return it</li>
 * </ul>
 * Once the value is computed, it calls the {@link #persistAttribute(Project, VirtualFile, Object)}
 * to test if the value is changed, if so, the implementation is responsible to notify back
 * <br/>
 * This API is intended to be used to prepare state for indexing, please do not use it for other purposes
 * <br/><br/>
 * <b>Note:</b> Don't use plugin-specific classes as pushed properties, consider using <code>String</code> instead.
 * Otherwise, the plugin becomes not dynamic because its classes will be hard-referenced as <code>VirtualFile</code>'s user data.
 * For example, instead of pushing <code>SomeLanguageDialect</code> instances, push <code>someLanguageDialect.getName()</code> and
 * restore <code>SomeLanguageDialect</code> by name where needed.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface FilePropertyPusher<T> {
    ExtensionPointName<FilePropertyPusher> EP_NAME = ExtensionPointName.create(FilePropertyPusher.class);

    default void initExtra(@Nonnull Project project) {
    }

    default void afterRootsChanged(@Nonnull Project project) {
    }

    /**
     * After property was pushed it can be retrieved any time using {@see FilePropertyPusher#getFileDataKey()}
     * from {@link VirtualFile#getUserData(Key)}.
     */
    @Nonnull
    Key<T> getFileDataKey();

    boolean pushDirectoriesOnly();

    @Nonnull
    T getDefaultValue();

    @Nullable
    T getImmediateValue(@Nonnull Module module);

    @Nullable
    T getImmediateValue(@Nonnull Project project, @Nullable VirtualFile file);

    boolean acceptsFile(@Nonnull VirtualFile file, @Nonnull Project project);

    boolean acceptsDirectory(@Nonnull VirtualFile file, @Nonnull Project project);

    /**
     * This method is called to persist the computed Pusher value (of type T).
     * The implementation is supposed to call {@link PushedFilePropertiesUpdater#filePropertiesChanged}
     * if a change is detected to issue the {@param fileOrDir} re-index
     */
    void persistAttribute(@Nonnull Project project, @Nonnull VirtualFile fileOrDir, @Nonnull T value) throws IOException;
}
