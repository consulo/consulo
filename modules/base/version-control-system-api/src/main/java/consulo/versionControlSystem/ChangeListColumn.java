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
package consulo.versionControlSystem;

import consulo.application.util.DateFormatUtil;
import consulo.localize.LocalizeValue;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;

/**
 * @author yole
 * @since 2006-11-27
 */
public abstract class ChangeListColumn<T extends ChangeList> {
    public abstract LocalizeValue getTitle();

    public abstract Object getValue(T changeList);

    public @Nullable Comparator<T> getComparator() {
        return null;
    }

    // TODO: CompositeCommittedChangesProvider.getColumns() needs to be updated if new standard columns are added

    public static ChangeListColumn<CommittedChangeList> DATE = new ChangeListColumn<>() {
        @Override
        public LocalizeValue getTitle() {
            return VcsLocalize.columnNameRevisionListDate();
        }

        @Override
        public Object getValue(CommittedChangeList changeList) {
            return DateFormatUtil.formatPrettyDateTime(changeList.getCommitDate());
        }

        @Override
        public Comparator<CommittedChangeList> getComparator() {
            return (o1, o2) -> o1.getCommitDate().compareTo(o2.getCommitDate());
        }
    };

    public static ChangeListColumn<CommittedChangeList> NAME = new ChangeListColumn<>() {
        @Override
        public LocalizeValue getTitle() {
            return VcsLocalize.columnNameRevisionListCommitter();
        }

        @Override
        public Object getValue(CommittedChangeList changeList) {
            return changeList.getCommitterName();
        }

        @Override
        public Comparator<CommittedChangeList> getComparator() {
            return Comparator.comparing(o -> (String) getValue(o));
        }
    };

    public static ChangeListColumn<CommittedChangeList> NUMBER = new ChangeListNumberColumn(VcsLocalize.columnNameRevisionListNumber());

    public static ChangeListColumn<CommittedChangeList> DESCRIPTION = new ChangeListColumn<>() {
        @Override
        public LocalizeValue getTitle() {
            return VcsLocalize.columnNameRevisionListDescription();
        }

        @Override
        public Object getValue(CommittedChangeList changeList) {
            return changeList.getName();
        }

        @Override
        public Comparator<CommittedChangeList> getComparator() {
            return (o1, o2) -> o1.getName().compareTo(o2.getName());
        }
    };

    public static boolean isCustom(ChangeListColumn column) {
        return column != DATE && column != DESCRIPTION &&
            column != NAME && !(column instanceof ChangeListNumberColumn);
    }

    public static class ChangeListNumberColumn extends ChangeListColumn<CommittedChangeList> {
        private final LocalizeValue myTitle;

        public ChangeListNumberColumn(LocalizeValue title) {
            myTitle = title;
        }

        @Override
        public LocalizeValue getTitle() {
            return myTitle;
        }

        @Override
        public Object getValue(CommittedChangeList changeList) {
            return changeList.getNumber();
        }

        @Override
        public Comparator<CommittedChangeList> getComparator() {
            return (o1, o2) -> (int) (o1.getNumber() - o2.getNumber());
        }
    }
}
