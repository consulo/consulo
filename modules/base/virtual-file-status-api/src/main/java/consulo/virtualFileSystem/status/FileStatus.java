/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.virtualFileSystem.status;

import consulo.colorScheme.EditorColorKey;
import consulo.localize.LocalizeValue;
import consulo.ui.color.ColorValue;
import consulo.virtualFileSystem.status.localize.FileStatusLocalize;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@SuppressWarnings("UseJBColor")
public interface FileStatus {
  FileStatus NOT_CHANGED = FileStatusFactory.getInstance().createFileStatus("NOT_CHANGED", FileStatusLocalize.fileStatusNameUpToDate());
  FileStatus NOT_CHANGED_IMMEDIATE = FileStatusFactory.getInstance().createFileStatus("NOT_CHANGED_IMMEDIATE", FileStatusLocalize.fileStatusNameUpToDateImmediateChildren());
  FileStatus NOT_CHANGED_RECURSIVE = FileStatusFactory.getInstance().createFileStatus("NOT_CHANGED_RECURSIVE", FileStatusLocalize.fileStatusNameDeletedFromFileSystem());
  FileStatus DELETED = FileStatusFactory.getInstance().createFileStatus("DELETED", FileStatusLocalize.fileStatusNameDeleted());
  FileStatus MODIFIED = FileStatusFactory.getInstance().createFileStatus("MODIFIED", FileStatusLocalize.fileStatusNameModified());
  FileStatus ADDED = FileStatusFactory.getInstance().createFileStatus("ADDED", FileStatusLocalize.fileStatusNameAdded());
  FileStatus MERGE = FileStatusFactory.getInstance().createFileStatus("MERGED", FileStatusLocalize.fileStatusNameMerged());
  FileStatus UNKNOWN = FileStatusFactory.getInstance().createFileStatus("UNKNOWN", FileStatusLocalize.fileStatusNameUnknown());
  FileStatus IGNORED = FileStatusFactory.getInstance().createFileStatus("IDEA_FILESTATUS_IGNORED", FileStatusLocalize.fileStatusNameIgnored());
  FileStatus HIJACKED = FileStatusFactory.getInstance().createFileStatus("HIJACKED", FileStatusLocalize.fileStatusNameHijacked());
  FileStatus MERGED_WITH_CONFLICTS = FileStatusFactory.getInstance().createFileStatus("IDEA_FILESTATUS_MERGED_WITH_CONFLICTS", FileStatusLocalize.fileStatusNameMergedWithConflicts());
  FileStatus MERGED_WITH_BOTH_CONFLICTS = FileStatusFactory.getInstance().createFileStatus("IDEA_FILESTATUS_MERGED_WITH_BOTH_CONFLICTS", FileStatusLocalize.fileStatusNameMergedWithBothConflicts());
  FileStatus MERGED_WITH_PROPERTY_CONFLICTS =
          FileStatusFactory.getInstance().createFileStatus("IDEA_FILESTATUS_MERGED_WITH_PROPERTY_CONFLICTS", FileStatusLocalize.fileStatusNameMergedWithConflicts());
  FileStatus DELETED_FROM_FS = FileStatusFactory.getInstance().createFileStatus("IDEA_FILESTATUS_DELETED_FROM_FILE_SYSTEM", FileStatusLocalize.fileStatusNameDeletedFromFileSystem());
  FileStatus SWITCHED = FileStatusFactory.getInstance().createFileStatus("SWITCHED", FileStatusLocalize.fileStatusNameSwitched());
  FileStatus OBSOLETE = FileStatusFactory.getInstance().createFileStatus("OBSOLETE", FileStatusLocalize.fileStatusNameObsolete());
  FileStatus SUPPRESSED = FileStatusFactory.getInstance().createFileStatus("SUPPRESSED", FileStatusLocalize.fileStatusNameSuppressed());

  @Nonnull
  LocalizeValue getText();

  @Nullable
  ColorValue getColor();

  @Nonnull
  EditorColorKey getColorKey();

  @Nonnull
  String getId();
}
