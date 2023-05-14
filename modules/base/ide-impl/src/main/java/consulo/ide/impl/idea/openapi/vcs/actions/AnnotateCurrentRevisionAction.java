package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.application.AllIcons;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import java.util.HashMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class AnnotateCurrentRevisionAction extends AnnotateRevisionAction {
  @Nullable private final List<VcsFileRevision> myRevisions;

  public AnnotateCurrentRevisionAction(@Nonnull FileAnnotation annotation, @Nonnull AbstractVcs vcs) {
    super("Annotate Revision", "Annotate selected revision in new tab", AllIcons.Actions.Annotate,
          annotation, vcs);
    List<VcsFileRevision> revisions = annotation.getRevisions();
    if (revisions == null) {
      myRevisions = null;
      return;
    }

    Map<VcsRevisionNumber, VcsFileRevision> map = new HashMap<VcsRevisionNumber, VcsFileRevision>();
    for (VcsFileRevision revision : revisions) {
      map.put(revision.getRevisionNumber(), revision);
    }

    myRevisions = new ArrayList<VcsFileRevision>(annotation.getLineCount());
    for (int i = 0; i < annotation.getLineCount(); i++) {
      myRevisions.add(map.get(annotation.getLineRevisionNumber(i)));
    }
  }

  @Override
  @Nullable
  public List<VcsFileRevision> getRevisions() {
    return myRevisions;
  }
}
