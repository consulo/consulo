// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.index.impl.internal;

import consulo.language.psi.stub.IdFilter;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Supplier;

abstract class ProjectIndexableFilesFilterHolder {
    abstract @Nullable IdFilter getProjectIndexableFiles(Project project);

    abstract void addFileId(int fileId, Supplier<Set<Project>> projects);

    abstract boolean addFileId(int fileId, Project project);

    abstract void entireProjectUpdateStarted(Project project);

    abstract void entireProjectUpdateFinished(Project project);

    abstract void removeFile(int fileId);
}
