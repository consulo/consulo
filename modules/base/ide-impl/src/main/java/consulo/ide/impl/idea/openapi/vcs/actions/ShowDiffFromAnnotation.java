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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.application.AllIcons;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.diff.DiffDialogHints;
import consulo.diff.internal.DiffUserDataKeysEx;
import consulo.diff.DiffNavigationContext;
import consulo.ide.impl.idea.openapi.vcs.annotate.UpToDateLineNumberListener;
import consulo.ide.impl.idea.openapi.vcs.changes.BackgroundFromStartOption;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.diff.ShowDiffAction;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.diff.ShowDiffContext;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesComparator;
import consulo.ide.impl.idea.util.containers.CacheOneStepIterator;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
class ShowDiffFromAnnotation extends DumbAwareAction implements UpToDateLineNumberListener {
  private final FileAnnotation myFileAnnotation;
  private final AbstractVcs myVcs;
  private final VirtualFile myFile;
  private int currentLine;
  private boolean myEnabled;

  ShowDiffFromAnnotation(final FileAnnotation fileAnnotation, final AbstractVcs vcs, final VirtualFile file) {
    super(
      ActionLocalize.actionDiffUpdatedfilesText(),
      ActionLocalize.actionDiffUpdatedfilesDescription(),
      AllIcons.Actions.Diff
    );
    myFileAnnotation = fileAnnotation;
    myVcs = vcs;
    myFile = file;
    currentLine = -1;
    myEnabled = ProjectLevelVcsManager.getInstance(vcs.getProject()).getVcsFor(myFile) != null;
  }

  @Override
  public void accept(Integer integer) {
    currentLine = integer;
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    final int number = currentLine;
    e.getPresentation().setVisible(myEnabled);
    e.getPresentation().setEnabled(myEnabled && number >= 0 && number < myFileAnnotation.getLineCount());
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final int actualNumber = currentLine;
    if (actualNumber < 0) return;

    final VcsRevisionNumber revisionNumber = myFileAnnotation.getLineRevisionNumber(actualNumber);
    if (revisionNumber != null) {
      final VcsException[] exc = new VcsException[1];
      final List<Change> changes = new LinkedList<Change>();
      final FilePath[] targetPath = new FilePath[1];
      ProgressManager.getInstance().run(new Task.Backgroundable(myVcs.getProject(),
                                                                "Loading revision " + revisionNumber.asString() + " contents", true,
                                                                BackgroundFromStartOption.getInstance()) {
        @Override
        public void run(@Nonnull ProgressIndicator indicator) {
          final CommittedChangesProvider provider = myVcs.getCommittedChangesProvider();
          try {
            final Pair<CommittedChangeList, FilePath> pair = provider.getOneList(myFile, revisionNumber);
            if (pair == null || pair.getFirst() == null) {
              VcsBalloonProblemNotifier.showOverChangesView(myVcs.getProject(), "Can not load data for show diff", NotificationType.ERROR);
              return;
            }
            targetPath[0] = pair.getSecond() == null ? VcsUtil.getFilePath(myFile) : pair.getSecond();
            final CommittedChangeList cl = pair.getFirst();
            changes.addAll(cl.getChanges());
            Collections.sort(changes, ChangesComparator.getInstance(true));
          }
          catch (VcsException e1) {
            exc[0] = e1;
          }
        }

        @Override
        public void onSuccess() {
          if (exc[0] != null) {
            VcsBalloonProblemNotifier
                    .showOverChangesView(myVcs.getProject(), "Can not show diff: " + exc[0].getMessage(), NotificationType.ERROR);
          }
          else if (!changes.isEmpty()) {
            int idx = findSelfInList(changes, targetPath[0]);
            final ShowDiffContext context = new ShowDiffContext(DiffDialogHints.FRAME);
            if (idx != -1) {
              context.putChangeContext(changes.get(idx), DiffUserDataKeysEx.NAVIGATION_CONTEXT, createDiffNavigationContext(actualNumber));
            }
            if (ChangeListManager.getInstance(myVcs.getProject()).isFreezedWithNotification(null)) return;
            ShowDiffAction.showDiffForChange(myVcs.getProject(), changes, idx, context);
          }
        }
      });
    }
  }

  private static int findSelfInList(List<Change> changes, final FilePath filePath) {
    int idx = -1;
    for (int i = 0; i < changes.size(); i++) {
      final Change change = changes.get(i);
      if ((change.getAfterRevision() != null) && (change.getAfterRevision().getFile().equals(filePath))) {
        idx = i;
        break;
      }
    }
    if (idx >= 0) return idx;
    idx = 0;
    // try to use name only
    final String name = filePath.getName();
    for (int i = 0; i < changes.size(); i++) {
      final Change change = changes.get(i);
      if ((change.getAfterRevision() != null) && (change.getAfterRevision().getFile().getName().equals(name))) {
        idx = i;
        break;
      }
    }

    return idx;
  }

  // for current line number
  private DiffNavigationContext createDiffNavigationContext(final int actualLine) {
    final ContentsLines contentsLines = new ContentsLines(myFileAnnotation.getAnnotatedContent());

    final Pair<Integer, String> pair = correctActualLineIfTextEmpty(contentsLines, actualLine);
    return new DiffNavigationContext(new Iterable<String>() {
      @Override
      public Iterator<String> iterator() {
        return new CacheOneStepIterator<String>(new ContextLineIterator(contentsLines, myFileAnnotation, pair.getFirst()));
      }
    }, pair.getSecond());
  }

  private final static int ourVicinity = 5;

  private Pair<Integer, String> correctActualLineIfTextEmpty(final ContentsLines contentsLines, final int actualLine) {
    final VcsRevisionNumber revision = myFileAnnotation.getLineRevisionNumber(actualLine);

    for (int i = actualLine; (i < (actualLine + ourVicinity)) && (!contentsLines.isLineEndsFinished()); i++) {
      if (!revision.equals(myFileAnnotation.getLineRevisionNumber(i))) continue;
      final String lineContents = contentsLines.getLineContents(i);
      if (!StringUtil.isEmptyOrSpaces(lineContents)) {
        return new Pair<Integer, String>(i, lineContents);
      }
    }
    int bound = Math.max(actualLine - ourVicinity, 0);
    for (int i = actualLine - 1; (i >= bound); --i) {
      if (!revision.equals(myFileAnnotation.getLineRevisionNumber(i))) continue;
      final String lineContents = contentsLines.getLineContents(i);
      if (!StringUtil.isEmptyOrSpaces(lineContents)) {
        return new Pair<Integer, String>(i, lineContents);
      }
    }
    return new Pair<Integer, String>(actualLine, contentsLines.getLineContents(actualLine));
  }

  /**
   * Slightly break the contract: can return null from next() while had claimed hasNext()
   */
  private static class ContextLineIterator implements Iterator<String> {
    private final ContentsLines myContentsLines;

    private final VcsRevisionNumber myRevisionNumber;
    private final FileAnnotation myAnnotation;
    private final int myStopAtLine;
    // we assume file has at least one line ;)
    private int myCurrentLine;  // to start looking for next line with revision from

    private ContextLineIterator(final ContentsLines contentLines, final FileAnnotation annotation, final int stopAtLine) {
      myAnnotation = annotation;
      myRevisionNumber = myAnnotation.originalRevision(stopAtLine);
      myStopAtLine = stopAtLine;
      myContentsLines = contentLines;
    }

    @Override
    public boolean hasNext() {
      return lineNumberInBounds();
    }

    private boolean lineNumberInBounds() {
      final int knownLinesNumber = myContentsLines.getKnownLinesNumber();
      return ((knownLinesNumber == -1) || (myCurrentLine < knownLinesNumber)) && (myCurrentLine < myStopAtLine);
    }

    @Override
    public String next() {
      int nextLine;
      while (lineNumberInBounds()) {
        final VcsRevisionNumber vcsRevisionNumber = myAnnotation.originalRevision(myCurrentLine);
        if (myRevisionNumber.equals(vcsRevisionNumber)) {
          nextLine = myCurrentLine;
          final String text = myContentsLines.getLineContents(nextLine);
          if (!StringUtil.isEmptyOrSpaces(text)) {
            ++myCurrentLine;
            return text;
          }
        }
        ++myCurrentLine;
      }
      return null;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
