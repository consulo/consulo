/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.ui.filter;

import consulo.application.util.DateFormatUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.localize.LocalizeValue;
import consulo.versionControlSystem.log.VcsLogDateFilter;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogDateFilterImpl;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import org.jspecify.annotations.Nullable;

import java.util.Calendar;
import java.util.Date;

class DateFilterPopup extends FilterPopup<VcsLogDateFilter> {
    DateFilterPopup(FilterModel<VcsLogDateFilter> filterModel) {
        super(VersionControlSystemLogLocalize.filterDate(), filterModel);
    }

    @Override
    protected String getText(VcsLogDateFilter filter) {
        Date after = filter.getAfter();
        Date before = filter.getBefore();
        if (after != null && before != null) {
            return DateFormatUtil.formatDate(after) + "-" + DateFormatUtil.formatDate(before);
        }
        else if (after != null) {
            return VersionControlSystemLogLocalize.filterDateSince(DateFormatUtil.formatDate(after)).get();
        }
        else if (before != null) {
            return VersionControlSystemLogLocalize.filterDateUntil(DateFormatUtil.formatDate(before)).get();
        }
        else {
            return VersionControlSystemLogLocalize.filterAll().get();
        }
    }

    @Override
    protected @Nullable String getToolTip(VcsLogDateFilter filter) {
        return null;
    }

    @Override
    public ActionGroup createActionGroup() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_YEAR, -1);
        Date oneDayBefore = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, -6);
        Date oneWeekBefore = cal.getTime();

        return new DefaultActionGroup(
            createAllAction(),
            new DateAction(oneDayBefore, VersionControlSystemLogLocalize.filterDateLast24Hours()),
            new DateAction(oneWeekBefore, VersionControlSystemLogLocalize.filterDateLast7Days()),
            new SelectAction()
        );
    }

    private class DateAction extends DumbAwareAction {
        private final Date mySince;

        DateAction(Date since, LocalizeValue text) {
            super(text);
            mySince = since;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            myFilterModel.setFilter(new VcsLogDateFilterImpl(mySince, null));
        }
    }

    private class SelectAction extends DumbAwareAction {
        SelectAction() {
            super(VersionControlSystemLogLocalize.filterSelect());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            DateFilterDialogDescriptor.showAsync(myFilterModel.getFilter()).whenComplete((filter, error) -> {
                if (filter != null) {
                    myFilterModel.setFilter(filter.getAfter() == null && filter.getBefore() == null ? null : filter);
                }
            });
        }
    }
}
