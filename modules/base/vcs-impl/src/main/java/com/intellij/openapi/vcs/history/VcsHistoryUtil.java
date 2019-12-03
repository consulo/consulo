/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.history;

import com.intellij.diff.DiffContentFactoryEx;
import com.intellij.diff.DiffManager;
import com.intellij.diff.DiffRequestFactoryImpl;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.diff.util.DiffUserDataKeysEx;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.openapi.vfs.encoding.EncodingProjectManager;
import com.intellij.util.WaitForProgressToShow;
import com.intellij.util.ui.JBUI;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;
import java.io.IOException;

public class VcsHistoryUtil {
  @Deprecated
  public static Key<Pair<FilePath, VcsRevisionNumber>> REVISION_INFO_KEY = DiffUserDataKeysEx.REVISION_INFO;

  private static final Logger LOG = Logger.getInstance(VcsHistoryUtil.class);

  private VcsHistoryUtil() {
  }

  public static int compare(VcsFileRevision first, VcsFileRevision second) {
    if (first instanceof CurrentRevision && second instanceof CurrentRevision) {
      return compareNumbers(first, second);
    }
    if (second instanceof CurrentRevision) return -1 * compare(second, first);

    if (first instanceof CurrentRevision) {
      int result = compareNumbers(first, second);
      if (result == 0) {
        return 1;
      }
      else {
        return result;
      }
    }
    else {
      return compareNumbers(first, second);
    }
  }

  public static int compareNumbers(VcsFileRevision first, VcsFileRevision second) {
    return first.getRevisionNumber().compareTo(second.getRevisionNumber());
  }

  /**
   * Invokes {@link DiffManager#getDiffTool()} to show difference between the given revisions of the given file.
   * @param project   project under vcs control.
   * @param path  file which revisions are compared.
   * @param revision1 first revision - 'before', to the left.
   * @param revision2 second revision - 'after', to the right.
   * @throws VcsException
   * @throws IOException
   */
  public static void showDiff(@Nonnull final Project project, @Nonnull FilePath path,
                              @Nonnull VcsFileRevision revision1, @Nonnull VcsFileRevision revision2,
                              @Nonnull String title1, @Nonnull String title2) throws VcsException, IOException {
    final byte[] content1 = loadRevisionContent(revision1);
    final byte[] content2 = loadRevisionContent(revision2);

    FilePath path1 = getRevisionPath(revision1);
    FilePath path2 = getRevisionPath(revision2);

    String title;
    if (path1 != null && path2 != null) {
      title = DiffRequestFactoryImpl.getTitle(path1, path2, " -> ");
    }
    else {
      title = DiffRequestFactoryImpl.getContentTitle(path);
    }

    DiffContent diffContent1 = createContent(project, content1, revision1, path);
    DiffContent diffContent2 = createContent(project, content2, revision2, path);

    final DiffRequest request = new SimpleDiffRequest(title, diffContent1, diffContent2, title1, title2);

    diffContent1.putUserData(DiffUserDataKeysEx.REVISION_INFO, getRevisionInfo(revision1));
    diffContent2.putUserData(DiffUserDataKeysEx.REVISION_INFO, getRevisionInfo(revision2));

    WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
      public void run() {
        DiffManager.getInstance().showDiff(project, request);
      }
    }, null, project);
  }

  @javax.annotation.Nullable
  private static Pair<FilePath, VcsRevisionNumber> getRevisionInfo(@Nonnull VcsFileRevision revision) {
    if (revision instanceof VcsFileRevisionEx) {
      return Pair.create(((VcsFileRevisionEx)revision).getPath(), revision.getRevisionNumber());
    }
    return null;
  }

  @javax.annotation.Nullable
  private static FilePath getRevisionPath(@Nonnull VcsFileRevision revision) {
    if (revision instanceof VcsFileRevisionEx) {
      return ((VcsFileRevisionEx)revision).getPath();
    }
    return null;
  }

  @Nonnull
  public static byte[] loadRevisionContent(@Nonnull VcsFileRevision revision) throws VcsException, IOException {
    byte[] content = revision.getContent();
    if (content == null) {
      revision.loadContent();
      content = revision.getContent();
    }
    if (content == null) throw new VcsException("Failed to load content for revision " + revision.getRevisionNumber().asString());
    return content;
  }

  public static String loadRevisionContentGuessEncoding(@Nonnull final VcsFileRevision revision, @Nullable final VirtualFile file,
                                                        @javax.annotation.Nullable final Project project) throws VcsException, IOException {
    final byte[] bytes = loadRevisionContent(revision);
    if (file != null) {
      return new String(bytes, file.getCharset());
    }
    EncodingManager e = project != null ? EncodingProjectManager.getInstance(project) : null;
    if (e == null) {
      e = EncodingManager.getInstance();
    }

    return CharsetToolkit.bytesToString(bytes, e.getDefaultCharset());
  }

  @Nonnull
  private static DiffContent createContent(@Nonnull Project project, @Nonnull byte[] content, @Nonnull VcsFileRevision revision,
                                           @Nonnull FilePath filePath) throws IOException {
    DiffContentFactoryEx contentFactory = DiffContentFactoryEx.getInstanceEx();
    if (isCurrent(revision)) {
      VirtualFile file = filePath.getVirtualFile();
      if (file != null) return contentFactory.create(project, file);
    }
    if (isEmpty(revision)) {
      return contentFactory.createEmpty();
    }
    return contentFactory.createFromBytes(project, content, filePath);
  }

  private static boolean isCurrent(VcsFileRevision revision) {
    return revision instanceof CurrentRevision;
  }

  private static boolean isEmpty(VcsFileRevision revision) {
    return revision == null || VcsFileRevision.NULL.equals(revision);
  }

  /**
   * Shows difference between two revisions of a file in a diff tool.
   * The content of revisions is queried in a background thread.
   *
   * @see #showDiff(Project, FilePath, VcsFileRevision, VcsFileRevision, String, String)
   */
  public static void showDifferencesInBackground(@Nonnull final Project project,
                                                 @Nonnull final FilePath filePath,
                                                 @Nonnull final VcsFileRevision older,
                                                 @Nonnull final VcsFileRevision newer) {
    new Task.Backgroundable(project, "Comparing Revisions...") {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        try {
          showDiff(project, filePath, older, newer, makeTitle(older), makeTitle(newer));
        }
        catch (final VcsException e) {
          LOG.info(e);
          WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
            public void run() {
              Messages.showErrorDialog(VcsBundle.message("message.text.cannot.show.differences", e.getLocalizedMessage()),
                                       VcsBundle.message("message.title.show.differences"));
            }
          }, null, project);
        }
        catch (IOException e) {
          LOG.info(e);
        }
      }

      @Nonnull
      private String makeTitle(@Nonnull VcsFileRevision revision) {
        return revision.getRevisionNumber().asString() +
               (revision instanceof CurrentRevision ? " (" + VcsBundle.message("diff.title.local") + ")" : "");
      }
    }.queue();
  }

  @Nonnull
  public static Font getCommitDetailsFont() {
    return JBUI.scale(EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN));
  }
}
