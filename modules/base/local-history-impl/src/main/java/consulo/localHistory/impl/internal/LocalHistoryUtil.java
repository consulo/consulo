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
package consulo.localHistory.impl.internal;

import consulo.localHistory.impl.internal.revision.ChangeRevision;
import consulo.localHistory.impl.internal.revision.Revision;
import consulo.localHistory.impl.internal.ui.model.HistoryDialogModel;
import consulo.localHistory.impl.internal.ui.model.RevisionItem;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.util.List;

public class LocalHistoryUtil {

  static int findRevisionIndexToRevert(@Nonnull HistoryDialogModel dirHistoryModel, @Nonnull LabelImpl label) {
    List<RevisionItem> revs = dirHistoryModel.getRevisions();
    for (int i = 0; i < revs.size(); i++) {
      final RevisionItem rev = revs.get(i);
      if (isLabelRevision(rev, label)) return i;
      //when lvcs model is not constructed yet or is empty then PutLabelChange is created but without label, so we need to scan revisions themselves
      if (isChangeWithId(rev.revision, label.getLabelChangeId())) return i;
    }
    return -1;
  }

  static boolean isLabelRevision(@Nonnull RevisionItem rev, @Nonnull LabelImpl label) {
    final long targetChangeId = label.getLabelChangeId();
    return ContainerUtil.exists(rev.labels, revision -> isChangeWithId(revision, targetChangeId));
  }

  private static boolean isChangeWithId(@Nonnull Revision revision, long targetChangeId) {
    return revision instanceof ChangeRevision && ((ChangeRevision)revision).containsChangeWithId(targetChangeId);
  }
}
