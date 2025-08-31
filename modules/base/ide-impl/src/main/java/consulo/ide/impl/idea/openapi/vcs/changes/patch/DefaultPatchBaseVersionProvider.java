/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.changes.patch;

import consulo.versionControlSystem.history.VcsFileRevisionDvcsSpecific;
import consulo.virtualFileSystem.internal.LoadTextUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.ref.Ref;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.diff.DiffProvider;
import consulo.versionControlSystem.history.*;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yole
 * @since 2006-11-21
 */
public class DefaultPatchBaseVersionProvider {
  private static final Logger LOG = Logger.getInstance(DefaultPatchBaseVersionProvider.class);
  private final static Pattern ourTsPattern = Pattern.compile("\\(date ([0-9]+)\\)");

  private final Project myProject;
  private final VirtualFile myFile;
  private final String myVersionId;
  private final Pattern myRevisionPattern;

  private final AbstractVcs myVcs;

  public DefaultPatchBaseVersionProvider(Project project, VirtualFile file, String versionId) {
    myProject = project;
    myFile = file;
    myVersionId = versionId;
    myVcs = ProjectLevelVcsManager.getInstance(myProject).getVcsFor(myFile);
    if (myVcs != null) {
      String vcsPattern = myVcs.getRevisionPattern();
      if (vcsPattern != null) {
        myRevisionPattern = Pattern.compile("\\(revision (" + vcsPattern + ")\\)");
        return;
      }
    }
    myRevisionPattern = null;
  }

  @SuppressWarnings("ReturnValueIgnored")
  public void getBaseVersionContent(
    FilePath filePath,
    Predicate<CharSequence> processor,
    List<String> warnings
  ) throws VcsException {
    if (myVcs == null) {
      return;
    }
    VcsHistoryProvider historyProvider = myVcs.getVcsHistoryProvider();
    if (historyProvider == null) return;

    VcsRevisionNumber revision = null;
    if (myRevisionPattern != null) {
      Matcher matcher = myRevisionPattern.matcher(myVersionId);
      if (matcher.find()) {
        revision = myVcs.parseRevisionNumber(matcher.group(1), filePath);
        VcsRevisionNumber finalRevision = revision;
        Boolean[] loadedExactRevision = new Boolean[1];

        if (historyProvider instanceof VcsBaseRevisionAdviser vcsBaseRevisionAdviser) {
          boolean success = VcsUtil.runVcsProcessWithProgress(
            () -> loadedExactRevision[0] =
              vcsBaseRevisionAdviser.getBaseVersionContent(filePath, processor, finalRevision.asString(), warnings),
            VcsLocalize.progressText2LoadingRevision(revision.asString()).get(),
            true,
            myProject
          );
          // was cancelled
          if (!success) return;
        }
        else {
          // use diff provider
          DiffProvider diffProvider = myVcs.getDiffProvider();
          if (diffProvider != null && filePath.getVirtualFile() != null) {
            ContentRevision fileContent = diffProvider.createFileContent(finalRevision, filePath.getVirtualFile());

            boolean success = VcsUtil.runVcsProcessWithProgress(
              () -> loadedExactRevision[0] = !processor.test(fileContent.getContent()),
              VcsLocalize.progressText2LoadingRevision(revision.asString()).get(),
              true,
              myProject
            );
            // was cancelled
            if (!success) return;
          }
        }
        if (Boolean.TRUE.equals(loadedExactRevision[0])) return;
      }
    }

    Date versionDate = null;
    if (revision == null) {
      try {
        Matcher tsMatcher = ourTsPattern.matcher(myVersionId);
        if (tsMatcher.find()) {
          Long fromTsPattern = getFromTsPattern();
          if (fromTsPattern == null) return;
          versionDate = new Date(fromTsPattern);
        }
        else {
          versionDate = new Date(myVersionId);
        }
      }
      catch (IllegalArgumentException ex) {
        return;
      }
    }
    try {
      Ref<VcsHistorySession> ref = new Ref<>();
      boolean result = VcsUtil.runVcsProcessWithProgress(
        () -> ref.set(historyProvider.createSessionFor(filePath)),
        VcsLocalize.loadingFileHistoryProgress().get(),
        true,
        myProject
      );
      //if not found or cancelled
      if (ref.isNull() || !result) return;
      VcsHistorySession session = ref.get();
      List<VcsFileRevision> list = session.getRevisionList();
      if (list == null) return;
      for (VcsFileRevision fileRevision : list) {
        boolean found;
        if (revision != null) {
          found = fileRevision.getRevisionNumber().compareTo(revision) <= 0;
        }
        else {
          Date date = fileRevision instanceof VcsFileRevisionDvcsSpecific vcsFileRevisionDvcsSpecific
            ? vcsFileRevisionDvcsSpecific.getDateForRevisionsOrdering() : fileRevision.getRevisionDate();
          found = (date != null) && (date.before(versionDate) || date.equals(versionDate));
        }

        if (found) {
          fileRevision.loadContent();
          processor.test(LoadTextUtil.getTextByBinaryPresentation(fileRevision.getContent(), myFile, false, false));
          // TODO: try to download more than one version
          break;
        }
      }
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  public boolean canProvideContent() {
    if (myVcs == null) {
      return false;
    }
    if ((myRevisionPattern != null) && myRevisionPattern.matcher(myVersionId).matches()) {
      return true;
    }
    if (ourTsPattern.matcher(myVersionId).matches()) return true;
    try {
      Date.parse(myVersionId);
    }
    catch (IllegalArgumentException ex) {
      return false;
    }
    return true;
  }

  public boolean hasVcs() {
    return myVcs != null;
  }

  private Long getFromTsPattern() {
    String trimmed = myVersionId.trim();
    String startPattern = "(date";
    int start = trimmed.indexOf(startPattern);
    if (start >= 0) {
      String number = trimmed.substring(startPattern.length() + start);
      number = number.endsWith(")") ? number.substring(0, number.length() - 1) : number;
      try {
        return Long.parseLong(number.trim());
      }
      catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
  }
}
