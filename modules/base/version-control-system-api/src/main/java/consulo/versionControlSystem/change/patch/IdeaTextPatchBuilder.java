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
package consulo.versionControlSystem.change.patch;

import consulo.application.progress.ProgressManager;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.util.collection.MultiMap;
import consulo.util.lang.BeforeAfter;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.VcsOutgoingChangesProvider;
import consulo.versionControlSystem.change.BinaryContentRevision;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.internal.SortByVcsRoots;
import consulo.versionControlSystem.root.VcsRoot;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

public class IdeaTextPatchBuilder {
  private IdeaTextPatchBuilder() {
  }

  public static List<BeforeAfter<AirContentRevision>> revisionsConvertor(Project project, List<Change> changes) throws VcsException {
    List<BeforeAfter<AirContentRevision>> result = new ArrayList<>(changes.size());

    Function<Change, FilePath> beforePrefferingConvertor = o -> {
      FilePath before = ChangesUtil.getBeforePath(o);
      return before == null ? ChangesUtil.getAfterPath(o) : before;
    };
    MultiMap<VcsRoot,Change> byRoots = new SortByVcsRoots<>(project, beforePrefferingConvertor).sort(changes);

    for (VcsRoot root : byRoots.keySet()) {
      Collection<Change> rootChanges = byRoots.get(root);
      if (root.getVcs() == null || root.getVcs().getOutgoingChangesProvider() == null) {
        addConvertChanges(rootChanges, result);
        continue;
      }
      VcsOutgoingChangesProvider<?> provider = root.getVcs().getOutgoingChangesProvider();
      Collection<Change> basedOnLocal = provider.filterLocalChangesBasedOnLocalCommits(rootChanges, root.getPath());
      rootChanges.removeAll(basedOnLocal);
      addConvertChanges(rootChanges, result);

      for (Change change : basedOnLocal) {
        // dates are here instead of numbers
        result.add(new BeforeAfter<>(
          convertRevision(change.getBeforeRevision(), provider),
          convertRevision(change.getAfterRevision(), provider))
        );
      }
    }
    return result;
  }

  private static void addConvertChanges(Collection<Change> changes, List<BeforeAfter<AirContentRevision>> result) {
    for (Change change : changes) {
      result.add(new BeforeAfter<>(convertRevisionToAir(change.getBeforeRevision()), convertRevisionToAir(change.getAfterRevision())));
    }
  }

  @Nonnull
  public static List<FilePatch> buildPatch(Project project, Collection<Change> changes, String basePath, boolean reversePatch) throws VcsException {
    return buildPatch(project, changes, basePath, reversePatch, false);
  }

  @Nonnull
  public static List<FilePatch> buildPatch(Project project, Collection<Change> changes, String basePath,
                                           boolean reversePatch, boolean includeBaseText) throws VcsException {
    Collection<BeforeAfter<AirContentRevision>> revisions;
    if (project != null) {
      revisions = revisionsConvertor(project, new ArrayList<>(changes));
    } else {
      revisions = new ArrayList<>(changes.size());
      for (Change change : changes) {
        revisions.add(new BeforeAfter<>(convertRevisionToAir(change.getBeforeRevision()), convertRevisionToAir(change.getAfterRevision())));
      }
    }
    return TextPatchBuilder.buildPatch(
      revisions,
      basePath,
      reversePatch,
      Platform.current().fs().isCaseSensitive(),
      ProgressManager::checkCanceled,
      includeBaseText
    );
  }

  @Nullable
  private static AirContentRevision convertRevisionToAir(ContentRevision cr) {
    return convertRevisionToAir(cr, null);
  }

  @Nullable
  private static AirContentRevision convertRevisionToAir(final ContentRevision cr, final Long ts) {
    if (cr == null) return null;
    final FilePath fp = cr.getFile();
    final StaticPathDescription description = new StaticPathDescription(fp.isDirectory(),
                                                                        ts == null ? fp.getIOFile().lastModified() : ts, fp.getPath());
    if (cr instanceof BinaryContentRevision bcr) {
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
          return bcr.getBinaryContent();
        }
        @Override
        public String getRevisionNumber() {
          return ts != null ? null : cr.getRevisionNumber().asString();
        }
        @Override
        @Nonnull
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
        @Nonnull
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
  private static AirContentRevision convertRevision(@Nullable ContentRevision cr, VcsOutgoingChangesProvider provider) {
    if (cr == null) return null;
    Date date = provider.getRevisionDate(cr.getRevisionNumber(), cr.getFile());
    Long ts = date == null ? null : date.getTime();
    return convertRevisionToAir(cr, ts);
  }
}
