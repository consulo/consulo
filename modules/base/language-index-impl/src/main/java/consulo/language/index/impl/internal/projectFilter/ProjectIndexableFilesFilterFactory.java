// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.projectFilter;

import consulo.project.Project;

abstract class ProjectIndexableFilesFilterFactory {
    abstract ProjectIndexableFilesFilter create(Project project, long currentVfsCreationTimestamp);
}
