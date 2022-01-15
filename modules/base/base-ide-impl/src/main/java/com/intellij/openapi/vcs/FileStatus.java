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
package com.intellij.openapi.vcs;

import com.intellij.openapi.editor.colors.EditorColorKey;
import com.intellij.ui.Gray;
import consulo.editor.ui.api.localize.EditorUIApiLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.color.ColorValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

@SuppressWarnings("UseJBColor")
public interface FileStatus {
  /**
   * @deprecated Use FileStatus.getColor() instead
   */
  @Deprecated
  Color COLOR_NOT_CHANGED = null; // deliberately null, do not use hardcoded Color.BLACK
  @Deprecated
  Color COLOR_NOT_CHANGED_RECURSIVE = new Color(138, 164, 200);
  @Deprecated
  Color COLOR_NOT_CHANGED_IMMEDIATE = new Color(50, 100, 180);
  @Deprecated
  Color COLOR_MERGE = new Color(117, 3, 220);
  @Deprecated
  Color COLOR_MODIFIED = new Color(0, 50, 160);
  @Deprecated
  Color COLOR_MISSING = Gray._97;
  @Deprecated
  Color COLOR_ADDED = new Color(10, 119, 0);
  @Deprecated
  Color COLOR_OUT_OF_DATE = Color.yellow.darker().darker();
  @Deprecated
  Color COLOR_HIJACKED = Color.ORANGE.darker();
  @Deprecated
  Color COLOR_SWITCHED = new Color(8, 151, 143);
  @Deprecated
  Color COLOR_UNKNOWN = new Color(153, 51, 0);

  FileStatus NOT_CHANGED = FileStatusFactory.getInstance().createFileStatus("NOT_CHANGED", EditorUIApiLocalize.fileStatusNameUpToDate());
  FileStatus NOT_CHANGED_IMMEDIATE = FileStatusFactory.getInstance().createFileStatus("NOT_CHANGED_IMMEDIATE", EditorUIApiLocalize.fileStatusNameUpToDateImmediateChildren());
  FileStatus NOT_CHANGED_RECURSIVE = FileStatusFactory.getInstance().createFileStatus("NOT_CHANGED_RECURSIVE", EditorUIApiLocalize.fileStatusNameDeletedFromFileSystem());
  FileStatus DELETED = FileStatusFactory.getInstance().createFileStatus("DELETED", EditorUIApiLocalize.fileStatusNameDeleted());
  FileStatus MODIFIED = FileStatusFactory.getInstance().createFileStatus("MODIFIED", EditorUIApiLocalize.fileStatusNameModified());
  FileStatus ADDED = FileStatusFactory.getInstance().createFileStatus("ADDED", EditorUIApiLocalize.fileStatusNameAdded());
  FileStatus MERGE = FileStatusFactory.getInstance().createFileStatus("MERGED", EditorUIApiLocalize.fileStatusNameMerged());
  FileStatus UNKNOWN = FileStatusFactory.getInstance().createFileStatus("UNKNOWN", EditorUIApiLocalize.fileStatusNameUnknown());
  FileStatus IGNORED = FileStatusFactory.getInstance().createFileStatus("IDEA_FILESTATUS_IGNORED", EditorUIApiLocalize.fileStatusNameIgnored());
  FileStatus HIJACKED = FileStatusFactory.getInstance().createFileStatus("HIJACKED", EditorUIApiLocalize.fileStatusNameHijacked());
  FileStatus MERGED_WITH_CONFLICTS = FileStatusFactory.getInstance().createFileStatus("IDEA_FILESTATUS_MERGED_WITH_CONFLICTS", EditorUIApiLocalize.fileStatusNameMergedWithConflicts());
  FileStatus MERGED_WITH_BOTH_CONFLICTS = FileStatusFactory.getInstance().createFileStatus("IDEA_FILESTATUS_MERGED_WITH_BOTH_CONFLICTS", EditorUIApiLocalize.fileStatusNameMergedWithBothConflicts());
  FileStatus MERGED_WITH_PROPERTY_CONFLICTS =
          FileStatusFactory.getInstance().createFileStatus("IDEA_FILESTATUS_MERGED_WITH_PROPERTY_CONFLICTS", EditorUIApiLocalize.fileStatusNameMergedWithConflicts());
  FileStatus DELETED_FROM_FS = FileStatusFactory.getInstance().createFileStatus("IDEA_FILESTATUS_DELETED_FROM_FILE_SYSTEM", EditorUIApiLocalize.fileStatusNameDeletedFromFileSystem());
  FileStatus SWITCHED = FileStatusFactory.getInstance().createFileStatus("SWITCHED", EditorUIApiLocalize.fileStatusNameSwitched());
  FileStatus OBSOLETE = FileStatusFactory.getInstance().createFileStatus("OBSOLETE", EditorUIApiLocalize.fileStatusNameObsolete());
  FileStatus SUPPRESSED = FileStatusFactory.getInstance().createFileStatus("SUPPRESSED", EditorUIApiLocalize.fileStatusNameSuppressed());

  @Nonnull
  LocalizeValue getText();

  @Nullable
  ColorValue getColor();

  @Nonnull
  EditorColorKey getColorKey();

  @Nonnull
  String getId();
}
