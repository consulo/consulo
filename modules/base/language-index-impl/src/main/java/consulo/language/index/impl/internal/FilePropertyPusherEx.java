// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.language.index.impl.internal.roots.kind.IndexableSetOrigin;
import consulo.module.content.FilePropertyPusher;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

/**
 * As of 25.2 {@link FilePropertyPusher} allows pushing properties outside module content only
 * through {@link FilePropertyPusher#initExtra(Project)} and {@link FilePropertyPusher#afterRootsChanged(Project)}.
 * Such API enforced pushing values to all related files in a project, ignoring information about the scope of the current scanning.
 * It's inefficient and may result in a performance issue (see IJPL-2963).
 * <p>
 * {@link FilePropertyPusherEx} allows selecting applicable {@link IndexableSetOrigin}s of current scanning, and push new values only there,
 * making pusher's work incremental instead of a push to all related files
 * on every {@link consulo.module.content.layer.event.ModuleRootEvent} (which includes every scanning) and on opening a project.
 * <p>
 * Unlike {@link FilePropertyPusher} with multiple methods to provide the value to push,
 * {@link FilePropertyPusherEx} determines the value to push
 * based on a single @{link {@link #getImmediateValueEx(IndexableSetOrigin)}} method.
 */
public interface FilePropertyPusherEx<T> extends FilePropertyPusher<T> {

    boolean acceptsOrigin(Project project, IndexableSetOrigin origin);

    @Nullable T getImmediateValueEx(IndexableSetOrigin origin);
}
