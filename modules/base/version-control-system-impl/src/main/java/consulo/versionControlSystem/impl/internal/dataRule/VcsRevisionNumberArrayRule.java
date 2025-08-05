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
package consulo.versionControlSystem.impl.internal.dataRule;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataProvider;
import consulo.dataContext.GetDataRule;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.versionControlSystem.versionBrowser.CommittedChangeListByDateComparator;
import consulo.versionControlSystem.versionBrowser.VcsRevisionNumberAware;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * @author Konstantin Kolosovsky.
 */
@ExtensionImpl
public class VcsRevisionNumberArrayRule implements GetDataRule<VcsRevisionNumber[]> {

  @Nonnull
  @Override
  public Key<VcsRevisionNumber[]> getKey() {
    return VcsDataKeys.VCS_REVISION_NUMBERS;
  }

  @Nullable
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
    public VcsRevisionNumber apply(CommittedChangeList changeList) {
      return changeList instanceof VcsRevisionNumberAware ? ((VcsRevisionNumberAware)changeList).getRevisionNumber() : null;
    }
  }

  private static class FileRevisionToRevisionNumberFunction implements Function<VcsFileRevision, VcsRevisionNumber> {

    private static final FileRevisionToRevisionNumberFunction INSTANCE = new FileRevisionToRevisionNumberFunction();

    @Override
    public VcsRevisionNumber apply(VcsFileRevision fileRevision) {
      return fileRevision.getRevisionNumber();
    }
  }
}
