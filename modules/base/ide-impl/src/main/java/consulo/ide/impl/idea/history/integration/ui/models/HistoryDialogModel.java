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

package consulo.ide.impl.idea.history.integration.ui.models;

import consulo.ide.impl.idea.history.core.LocalHistoryFacade;
import consulo.ide.impl.idea.history.core.RevisionsCollector;
import consulo.ide.impl.idea.history.core.revisions.Difference;
import consulo.ide.impl.idea.history.core.revisions.Revision;
import consulo.ide.impl.idea.history.core.tree.Entry;
import consulo.ide.impl.idea.history.core.tree.RootEntry;
import consulo.ide.impl.idea.history.integration.IdeaGateway;
import consulo.ide.impl.idea.history.integration.patches.PatchCreator;
import consulo.ide.impl.idea.history.integration.revertion.Reverter;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import consulo.application.util.function.Computable;
import consulo.util.lang.Pair;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.Change;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class HistoryDialogModel {
  protected final Project myProject;
  protected LocalHistoryFacade myVcs;
  protected VirtualFile myFile;
  protected IdeaGateway myGateway;
  private String myFilter;
  private List<RevisionItem> myRevisionsCache;
  private Revision myCurrentRevisionCache;
  private int myRightRevisionIndex;
  private int myLeftRevisionIndex;
  private Entry[] myLeftEntryCache;
  private Entry[] myRightEntryCache;

  public HistoryDialogModel(Project p, IdeaGateway gw, LocalHistoryFacade vcs, VirtualFile f) {
    myProject = p;
    myVcs = vcs;
    myFile = f;
    myGateway = gw;
  }

  public String getTitle() {
    return FileUtil.toSystemDependentName(myFile.getPath());
  }


  public List<RevisionItem> getRevisions() {
    if (myRevisionsCache == null) {
      Pair<Revision, List<RevisionItem>> revs = calcRevisionsCache();
      myCurrentRevisionCache = revs.first;
      myRevisionsCache = revs.second;
    }
    return myRevisionsCache;
  }

  public Revision getCurrentRevision() {
    getRevisions();
    return myCurrentRevisionCache;
  }

  protected Pair<Revision, List<RevisionItem>> calcRevisionsCache() {
    return ApplicationManager.getApplication().runReadAction(new Computable<Pair<Revision, List<RevisionItem>>>() {
      public Pair<Revision, List<RevisionItem>> compute() {
        myGateway.registerUnsavedDocuments(myVcs);
        String path = myFile.getPath();
        RootEntry root = myGateway.createTransientRootEntry();
        RevisionsCollector collector = new RevisionsCollector(myVcs, root, path, myProject.getLocationHash(), myFilter);

        List<Revision> all = collector.getResult();
        return Pair.create(all.get(0), groupRevisions(all.subList(1, all.size())));
      }
    });
  }

  private List<RevisionItem> groupRevisions(List<Revision> revs) {
    LinkedList<RevisionItem> result = new LinkedList<>();

    for (Revision each : ContainerUtil.iterateBackward(revs)) {
      if (each.isLabel() && !result.isEmpty()) {
        result.getFirst().labels.addFirst(each);
      } else {
        result.addFirst(new RevisionItem(each));
      }
    }

    return result;
  }

  public void setFilter(@Nullable String filter) {
    myFilter = StringUtil.isEmptyOrSpaces(filter) ? null : filter;
    clearRevisions();
  }

  public void clearRevisions() {
    myRevisionsCache = null;
    resetEntriesCache();
  }

  private void resetEntriesCache() {
    myLeftEntryCache = null;
    myRightEntryCache = null;
  }

  public Revision getLeftRevision() {
    if (getRevisions().isEmpty()) return getCurrentRevision();
    return getRevisions().get(myLeftRevisionIndex).revision;
  }

  public Revision getRightRevision() {
    if (isCurrentRevisionSelected() || getRevisions().isEmpty()) {
      return getCurrentRevision();
    }
    return getRevisions().get(myRightRevisionIndex).revision;
  }

  protected Entry getLeftEntry() {
    if (myLeftEntryCache == null) {
      // array is used because entry itself can be null
      myLeftEntryCache = new Entry[]{getLeftRevision().findEntry()};
    }
    return myLeftEntryCache[0];
  }

  protected Entry getRightEntry() {
    if (myRightEntryCache == null) {
      // array is used because entry itself can be null
      myRightEntryCache = new Entry[]{getRightRevision().findEntry()};
    }
    return myRightEntryCache[0];
  }

  public void selectRevisions(int first, int second) {
    if (first == second) {
      myRightRevisionIndex = -1;
      myLeftRevisionIndex = first == -1 ? 0 : first;
    }
    else {
      myRightRevisionIndex = first;
      myLeftRevisionIndex = second;
    }
    resetEntriesCache();
  }

  public void resetSelection() {
    selectRevisions(0, 0);
  }

  public boolean isCurrentRevisionSelected() {
    return myRightRevisionIndex == -1;
  }

  public List<Change> getChanges() {
    List<Difference> dd = getDifferences();

    List<Change> result = new ArrayList<>();
    for (Difference d : dd) {
      result.add(createChange(d));
    }

    return result;
  }

  protected List<Difference> getDifferences() {
    return getLeftRevision().getDifferencesWith(getRightRevision());
  }

  protected Change createChange(Difference d) {
    return new Change(d.getLeftContentRevision(myGateway), d.getRightContentRevision(myGateway));
  }

  public void createPatch(String path, String basePath, boolean isReverse, @Nonnull Charset charset) throws VcsException, IOException {
    PatchCreator.create(myProject, basePath, getChanges(), path, isReverse, null, charset);
  }

  public abstract Reverter createReverter();

  public boolean isRevertEnabled() {
    return isCorrectSelectionForRevertAndPatch();
  }

  public boolean isCreatePatchEnabled() {
    return isCorrectSelectionForRevertAndPatch();
  }

  private boolean isCorrectSelectionForRevertAndPatch() {
    return myLeftRevisionIndex != -1;
  }

  public boolean canPerformCreatePatch() {
    return !getLeftEntry().hasUnavailableContent() && !getRightEntry().hasUnavailableContent();
  }
}
