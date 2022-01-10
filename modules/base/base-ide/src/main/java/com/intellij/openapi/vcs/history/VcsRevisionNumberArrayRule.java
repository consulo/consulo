/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.ide.impl.dataRules.GetDataRule;
import com.intellij.openapi.actionSystem.DataProvider;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.committed.CommittedChangeListByDateComparator;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList;
import com.intellij.openapi.vcs.versionBrowser.VcsRevisionNumberAware;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author Konstantin Kolosovsky.
 */
public class VcsRevisionNumberArrayRule implements GetDataRule<VcsRevisionNumber[]> {

  @Nonnull
  @Override
  public Key<VcsRevisionNumber[]> getKey() {
    return VcsDataKeys.VCS_REVISION_NUMBERS;
  }

  @javax.annotation.Nullable
  @Override
  public VcsRevisionNumber[] getData(@Nonnull DataProvider dataProvider) {
    List<VcsRevisionNumber> revisionNumbers = getRevisionNumbers(dataProvider);

    return !ContainerUtil.isEmpty(revisionNumbers) ? ArrayUtil.toObjectArray(revisionNumbers, VcsRevisionNumber.class) : null;
  }

  @Nullable
  public List<VcsRevisionNumber> getRevisionNumbers(@Nonnull DataProvider dataProvider) {
    VcsRevisionNumber revisionNumber = dataProvider.getDataUnchecked(VcsDataKeys.VCS_REVISION_NUMBER);
    if (revisionNumber != null) {
      return Collections.singletonList(revisionNumber);
    }

    ChangeList[] changeLists = dataProvider.getDataUnchecked(VcsDataKeys.CHANGE_LISTS);
    if (changeLists != null && changeLists.length > 0) {
      List<CommittedChangeList> committedChangeLists = ContainerUtil.findAll(changeLists, CommittedChangeList.class);

      if (!committedChangeLists.isEmpty()) {
        ContainerUtil.sort(committedChangeLists, CommittedChangeListByDateComparator.DESCENDING);

        return ContainerUtil.mapNotNull(committedChangeLists, CommittedChangeListToRevisionNumberFunction.INSTANCE);
      }
    }

    VcsFileRevision[] fileRevisions = dataProvider.getDataUnchecked(VcsDataKeys.VCS_FILE_REVISIONS);
    if (fileRevisions != null && fileRevisions.length > 0) {
      return ContainerUtil.mapNotNull(fileRevisions, FileRevisionToRevisionNumberFunction.INSTANCE);
    }

    return null;
  }

  private static class CommittedChangeListToRevisionNumberFunction implements Function<CommittedChangeList, VcsRevisionNumber> {

    private static final CommittedChangeListToRevisionNumberFunction INSTANCE = new CommittedChangeListToRevisionNumberFunction();

    /**
     * TODO: Currently we do not return just "new VcsRevisionNumber.Long(changeList.getNumber())" for change lists which are not
     * TODO: explicitly VcsRevisionNumberAware as a lot of unnecessary objects will be created because VCS_REVISION_NUMBERS value is
     * TODO: obtained in CopyRevisionNumberAction.update().
     * <p/>
     * TODO: Decide if this is reasonable.
     */
    @Override
    public VcsRevisionNumber fun(CommittedChangeList changeList) {
      return changeList instanceof VcsRevisionNumberAware ? ((VcsRevisionNumberAware)changeList).getRevisionNumber() : null;
    }
  }

  private static class FileRevisionToRevisionNumberFunction implements Function<VcsFileRevision, VcsRevisionNumber> {

    private static final FileRevisionToRevisionNumberFunction INSTANCE = new FileRevisionToRevisionNumberFunction();

    @Override
    public VcsRevisionNumber fun(VcsFileRevision fileRevision) {
      return fileRevision.getRevisionNumber();
    }
  }
}
