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
package consulo.ide.impl.idea.openapi.vcs.annotate;

import consulo.codeEditor.EditorGutterComponentEx;
import consulo.ide.impl.idea.openapi.vcs.actions.CompareWithSelectedRevisionAction;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Arrays;
import java.util.List;

public class HighlightAnnotationsActions {
  private final HightlightAction myBefore;
  private final HightlightAction myAfter;
  private final RemoveHighlightingAction myRemove;
  private final EditorGutterComponentEx myGutter;

  public HighlightAnnotationsActions(Project project, VirtualFile virtualFile, FileAnnotation fileAnnotation, EditorGutterComponentEx gutter) {
    myGutter = gutter;
    myBefore = new HightlightAction(true, project, virtualFile, fileAnnotation, myGutter, null);
    List<VcsFileRevision> fileRevisionList = fileAnnotation.getRevisions();
    VcsFileRevision afterSelected = ((fileRevisionList != null) && (fileRevisionList.size() > 1)) ? fileRevisionList.get(0) : null;
    myAfter = new HightlightAction(false, project, virtualFile, fileAnnotation, myGutter, afterSelected);
    myRemove = new RemoveHighlightingAction();
  }

  public List<AnAction> getList() {
    return Arrays.asList(myBefore, myAfter, myRemove);
  }

  public boolean isLineBold(int lineNumber) {
    if (turnedOn()) {
      if (myBefore.isTurnedOn() && (!myBefore.isBold(lineNumber))) {
        return false;
      }
      if (myAfter.isTurnedOn() && (!myAfter.isBold(lineNumber))) {
        return false;
      }
      return true;
    }
    return false;
  }

  private boolean turnedOn() {
    return myBefore.isTurnedOn() || myAfter.isTurnedOn();
  }

  private class RemoveHighlightingAction extends AnAction {
    @Override
    public void update(AnActionEvent e) {
      e.getPresentation().setText("Remove highlighting");
      e.getPresentation().setEnabled(turnedOn());
    }

    public void actionPerformed(AnActionEvent e) {
      myBefore.clear();
      myAfter.clear();
      myGutter.revalidateMarkup();
    }
  }

  private static class HightlightAction extends AnAction {
    private final Project myProject;
    private final VirtualFile myVirtualFile;
    private final FileAnnotation myFileAnnotation;
    private final EditorGutterComponentEx myGutter;
    private final boolean myBefore;
    private VcsFileRevision mySelectedRevision;
    private Boolean myShowComments;

    private HightlightAction(boolean before,
                             Project project,
                             VirtualFile virtualFile,
                             FileAnnotation fileAnnotation,
                             EditorGutterComponentEx gutter,
                             @jakarta.annotation.Nullable VcsFileRevision selectedRevision) {
      myBefore = before;
      myProject = project;
      myVirtualFile = virtualFile;
      myFileAnnotation = fileAnnotation;
      myGutter = gutter;
      myShowComments = null;
      mySelectedRevision = selectedRevision;
    }

    @Override
    public void update(AnActionEvent e) {
      super.update(e);
      String text;
      String description;
      if (myBefore) {
        text = (mySelectedRevision == null)
               ? VcsBundle.message("highlight.annotation.before.not.selected.text")
               : VcsBundle.message("highlight.annotation.before.selected.text", mySelectedRevision.getRevisionNumber().asString());
        description = VcsBundle.message("highlight.annotation.before.description");
      }
      else {
        text = (mySelectedRevision == null)
               ? VcsBundle.message("highlight.annotation.after.not.selected.text")
               : VcsBundle.message("highlight.annotation.after.selected.text", mySelectedRevision.getRevisionNumber().asString());
        description = VcsBundle.message("highlight.annotation.after.description");
      }
      e.getPresentation().setText(text);
      e.getPresentation().setDescription(description);
      e.getPresentation().setEnabled(myFileAnnotation.revisionsNotEmpty());
    }

    public void actionPerformed(AnActionEvent e) {
      List<VcsFileRevision> fileRevisionList = myFileAnnotation.getRevisions();
      if (fileRevisionList != null) {
        if (myShowComments == null) {
          initShowComments(fileRevisionList);
        }
        CompareWithSelectedRevisionAction.showListPopup(fileRevisionList, myProject, vcsFileRevision -> {
          mySelectedRevision = vcsFileRevision;
          myGutter.revalidateMarkup();
        }, myShowComments.booleanValue());
      }
    }

    private void initShowComments(List<VcsFileRevision> revisions) {
      for (VcsFileRevision revision : revisions) {
        if (revision.getCommitMessage() != null) {
          myShowComments = true;
          return;
        }
      }
      myShowComments = false;
    }

    public boolean isTurnedOn() {
      return mySelectedRevision != null;
    }

    public void clear() {
      mySelectedRevision = null;
    }

    public boolean isBold(int line) {
      if (mySelectedRevision != null) {
        VcsRevisionNumber number = myFileAnnotation.originalRevision(line);
        if (number != null) {
          int compareResult = number.compareTo(mySelectedRevision.getRevisionNumber());
          return (myBefore && compareResult <= 0) || ((!myBefore) && (compareResult >= 0));
        }
      }
      return false;
    }
  }
}
