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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.localize.LocalizeValue;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.status.FileStatus;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public enum ChangesTreeGroupKind {
    UNVERSIONED(VcsLocalize.changesNodeTitleUnversionedFiles(), FileStatus.UNKNOWN),
    MODIFIED_WITHOUT_EDITING(VcsLocalize.changesNodeTitleModifiedWithoutEditing(), FileStatus.HIJACKED),
    LOCALLY_DELETED(VcsLocalize.changesNodeTitleLocallyDeletedFiles(), FileStatus.DELETED_FROM_FS),
    IGNORED(VcsLocalize.changesNodeTitleIgnoredFiles(), FileStatus.IGNORED);

    private final LocalizeValue myTitle;
    private final FileStatus myFileStatus;

    ChangesTreeGroupKind(LocalizeValue title, FileStatus fileStatus) {
        myTitle = title;
        myFileStatus = fileStatus;
    }

    public LocalizeValue getTitle() {
        return myTitle;
    }

    public FileStatus getFileStatus() {
        return myFileStatus;
    }
}
