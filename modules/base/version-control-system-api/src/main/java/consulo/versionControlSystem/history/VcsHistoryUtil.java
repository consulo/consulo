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
package consulo.versionControlSystem.history;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorFontType;
import consulo.diff.DiffManager;
import consulo.diff.content.DiffContent;
import consulo.diff.internal.DiffContentFactoryEx;
import consulo.diff.internal.DiffRequestFactoryEx;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.SimpleDiffRequest;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.util.WaitForProgressToShow;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.Messages;
import consulo.util.io.CharsetToolkit;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.diff.VcsDiffDataKeys;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.encoding.EncodingManager;
import consulo.virtualFileSystem.encoding.EncodingProjectManager;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.io.IOException;

public class VcsHistoryUtil {

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
  public static void showDiff(Project project, FilePath path,
                              VcsFileRevision revision1, VcsFileRevision revision2,
                              String title1, String title2) throws VcsException, IOException {
    byte[] content1 = loadRevisionContent(revision1);
    byte[] content2 = loadRevisionContent(revision2);

    FilePath path1 = getRevisionPath(revision1);
    FilePath path2 = getRevisionPath(revision2);

    String title;
    if (path1 != null && path2 != null) {
      title = DiffRequestFactoryEx.getInstanceEx().getTitle(path1, path2, " -> ");
    }
    else {
      title = DiffRequestFactoryEx.getInstanceEx().getContentTitle(path);
    }

    DiffContent diffContent1 = createContent(project, content1, revision1, path);
    DiffContent diffContent2 = createContent(project, content2, revision2, path);

    DiffRequest request = new SimpleDiffRequest(title, diffContent1, diffContent2, title1, title2);

    diffContent1.putUserData(VcsDiffDataKeys.REVISION_INFO, getRevisionInfo(revision1));
    diffContent2.putUserData(VcsDiffDataKeys.REVISION_INFO, getRevisionInfo(revision2));

    WaitForProgressToShow.runOrInvokeLaterAboveProgress(() -> DiffManager.getInstance().showDiff(project, request), null, project);
  }

  @Nullable
  private static Pair<FilePath, VcsRevisionNumber> getRevisionInfo(VcsFileRevision revision) {
    if (revision instanceof VcsFileRevisionEx) {
      return Pair.create(((VcsFileRevisionEx)revision).getPath(), revision.getRevisionNumber());
    }
    return null;
  }

  @Nullable
  private static FilePath getRevisionPath(VcsFileRevision revision) {
    if (revision instanceof VcsFileRevisionEx) {
      return ((VcsFileRevisionEx)revision).getPath();
    }
    return null;
  }

  
  public static byte[] loadRevisionContent(VcsFileRevision revision) throws VcsException, IOException {
    byte[] content = revision.getContent();
    if (content == null) {
      revision.loadContent();
      content = revision.getContent();
    }
    if (content == null) throw new VcsException("Failed to load content for revision " + revision.getRevisionNumber().asString());
    return content;
  }

  public static String loadRevisionContentGuessEncoding(VcsFileRevision revision, @Nullable VirtualFile file,
                                                        @Nullable Project project) throws VcsException, IOException {
    byte[] bytes = loadRevisionContent(revision);
    if (file != null) {
      return new String(bytes, file.getCharset());
    }
    EncodingManager e = project != null ? EncodingProjectManager.getInstance(project) : null;
    if (e == null) {
      e = EncodingManager.getInstance();
    }

    return CharsetToolkit.bytesToString(bytes, e.getDefaultCharset());
  }

  
  private static DiffContent createContent(Project project, byte[] content, VcsFileRevision revision,
                                           FilePath filePath) throws IOException {
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
  public static void showDifferencesInBackground(final Project project,
                                                 final FilePath filePath,
                                                 final VcsFileRevision older,
                                                 final VcsFileRevision newer) {
    new Task.Backgroundable(project, "Comparing Revisions...") {
      @Override
      public void run(ProgressIndicator indicator) {
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

      
      private String makeTitle(VcsFileRevision revision) {
        return revision.getRevisionNumber().asString() +
               (revision instanceof CurrentRevision ? " (" + VcsBundle.message("diff.title.local") + ")" : "");
      }
    }.queue();
  }

  
  public static Font getCommitDetailsFont() {
    return JBUI.scale(EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN));
  }
}
