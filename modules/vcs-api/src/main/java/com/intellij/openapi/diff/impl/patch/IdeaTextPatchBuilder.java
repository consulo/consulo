/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.openapi.diff.impl.patch;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsOutgoingChangesProvider;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.util.BeforeAfter;
import com.intellij.util.containers.Convertor;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class IdeaTextPatchBuilder {
  private IdeaTextPatchBuilder() {
  }

  public static List<BeforeAfter<AirContentRevision>> revisionsConvertor(final Project project, final List<Change> changes) throws VcsException {
    final List<BeforeAfter<AirContentRevision>> result = new ArrayList<BeforeAfter<AirContentRevision>>(changes.size());

    final Convertor<Change, FilePath> beforePrefferingConvertor = new Convertor<Change, FilePath>() {
      @Override
      public FilePath convert(Change o) {
        final FilePath before = ChangesUtil.getBeforePath(o);
        return before == null ? ChangesUtil.getAfterPath(o) : before;
      }
    };
    final MultiMap<VcsRoot,Change> byRoots = new SortByVcsRoots<Change>(project, beforePrefferingConvertor).sort(changes);

    for (VcsRoot root : byRoots.keySet()) {
      final Collection<Change> rootChanges = byRoots.get(root);
      if (root.getVcs() == null || root.getVcs().getOutgoingChangesProvider() == null) {
        addConvertChanges(rootChanges, result);
        continue;
      }
      final VcsOutgoingChangesProvider<?> provider = root.getVcs().getOutgoingChangesProvider();
      final Collection<Change> basedOnLocal = provider.filterLocalChangesBasedOnLocalCommits(rootChanges, root.getPath());
      rootChanges.removeAll(basedOnLocal);
      addConvertChanges(rootChanges, result);

      for (Change change : basedOnLocal) {
        // dates are here instead of numbers
        result.add(new BeforeAfter<AirContentRevision>(convertRevision(change.getBeforeRevision(), provider),
                                                       convertRevision(change.getAfterRevision(), provider)));
      }
    }
    return result;
  }

  private static void addConvertChanges(final Collection<Change> changes, final List<BeforeAfter<AirContentRevision>> result) {
    for (Change change : changes) {
      result.add(new BeforeAfter<AirContentRevision>(convertRevisionToAir(change.getBeforeRevision()), convertRevisionToAir(change.getAfterRevision())));
    }
  }

  @NotNull
  public static List<FilePatch> buildPatch(final Project project, final Collection<Change> changes, final String basePath, final boolean reversePatch) throws VcsException {
    return buildPatch(project, changes, basePath, reversePatch, false);
  }

  @NotNull
  public static List<FilePatch> buildPatch(final Project project, final Collection<Change> changes, final String basePath,
                                           final boolean reversePatch, final boolean includeBaseText) throws VcsException {
    final Collection<BeforeAfter<AirContentRevision>> revisions;
    if (project != null) {
      revisions = revisionsConvertor(project, new ArrayList<Change>(changes));
    } else {
      revisions = new ArrayList<BeforeAfter<AirContentRevision>>(changes.size());
      for (Change change : changes) {
        revisions.add(new BeforeAfter<AirContentRevision>(convertRevisionToAir(change.getBeforeRevision()), convertRevisionToAir(change.getAfterRevision())));
      }
    }
    return TextPatchBuilder.buildPatch(revisions, basePath, reversePatch, SystemInfo.isFileSystemCaseSensitive, new Runnable() {
      @Override
      public void run() {
        ProgressManager.checkCanceled();
      }
    }, includeBaseText);
  }

  @Nullable
  private static AirContentRevision convertRevisionToAir(final ContentRevision cr) {
    return convertRevisionToAir(cr, null);
  }

  @Nullable
  private static AirContentRevision convertRevisionToAir(final ContentRevision cr, final Long ts) {
    if (cr == null) return null;
    final FilePath fp = cr.getFile();
    final StaticPathDescription description = new StaticPathDescription(fp.isDirectory(),
                                                                        ts == null ? fp.getIOFile().lastModified() : ts, fp.getPath());
    if (cr instanceof BinaryContentRevision) {
      return new AirContentRevision() {
        @Override
        public boolean isBinary() {
          return true;
        }
        @Override
        public String getContentAsString() {
          throw new IllegalStateException();
        }
        @Override
        public byte[] getContentAsBytes() throws VcsException {
          return ((BinaryContentRevision) cr).getBinaryContent();
        }
        @Override
        public String getRevisionNumber() {
          return ts != null ? null : cr.getRevisionNumber().asString();
        }
        @Override
        @NotNull
        public PathDescription getPath() {
          return description;
        }

        @Override
        public Charset getCharset() {
          return null;
        }
      };
    } else {
      return new AirContentRevision() {
        @Override
        public boolean isBinary() {
          return false;
        }
        @Override
        public String getContentAsString() throws VcsException {
          return cr.getContent();
        }
        @Override
        public byte[] getContentAsBytes() throws VcsException {
          throw new IllegalStateException();
        }
        @Override
        public String getRevisionNumber() {
          return ts != null ? null : cr.getRevisionNumber().asString();
        }
        @Override
        @NotNull
        public PathDescription getPath() {
          return description;
        }

        @Override
        public Charset getCharset() {
          return fp.getCharset();
        }
      };
    }
  }

  @Nullable
  private static AirContentRevision convertRevision(@Nullable final ContentRevision cr, final VcsOutgoingChangesProvider provider) {
    if (cr == null) return null;
    final Date date = provider.getRevisionDate(cr.getRevisionNumber(), cr.getFile());
    final Long ts = date == null ? null : date.getTime();
    return convertRevisionToAir(cr, ts);
  }
}
