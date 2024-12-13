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
package consulo.ide.impl.idea.vcs.log.ui.filter;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.DialogBuilder;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.versionControlSystem.versionBrowser.ui.awt.DateFilterComponent;
import consulo.application.util.DateFormatUtil;
import consulo.versionControlSystem.log.VcsLogDateFilter;
import consulo.ide.impl.idea.vcs.log.data.VcsLogDateFilterImpl;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Calendar;
import java.util.Date;

class DateFilterPopupComponent extends FilterPopupComponent<VcsLogDateFilter> {

  public DateFilterPopupComponent(FilterModel<VcsLogDateFilter> filterModel) {
    super("Date", filterModel);
  }

  @Nonnull
  @Override
  protected String getText(@Nonnull VcsLogDateFilter filter) {
    Date after = filter.getAfter();
    Date before = filter.getBefore();
    if (after != null && before != null) {
      return DateFormatUtil.formatDate(after) + "-" + DateFormatUtil.formatDate(before);
    }
    else if (after != null) {
      return "Since " + DateFormatUtil.formatDate(after);
    }
    else if (before != null) {
      return "Until " + DateFormatUtil.formatDate(before);
    }
    else {
      return ALL;
    }
  }

  @Nullable
  @Override
  protected String getToolTip(@Nonnull VcsLogDateFilter filter) {
    return null;
  }

  @Override
  protected ActionGroup createActionGroup() {
    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.add(Calendar.DAY_OF_YEAR, -1);
    Date oneDayBefore = cal.getTime();
    cal.add(Calendar.DAY_OF_YEAR, -6);
    Date oneWeekBefore = cal.getTime();

    return new DefaultActionGroup(createAllAction(),
                                  new DateAction(oneDayBefore, "Last 24 hours"),
                                  new DateAction(oneWeekBefore, "Last 7 days"),
                                  new SelectAction());
  }

  private class DateAction extends DumbAwareAction {

    @Nonnull
    private final Date mySince;

    DateAction(@Nonnull Date since, @Nonnull String text) {
      super(text);
      mySince = since;
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      myFilterModel.setFilter(new VcsLogDateFilterImpl(mySince, null));
    }
  }

  private class SelectAction extends DumbAwareAction {

    SelectAction() {
      super("Select...");
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      final DateFilterComponent dateComponent = new DateFilterComponent(false, DateFormatUtil.getDateFormat().toPattern());
      VcsLogDateFilter currentFilter = myFilterModel.getFilter();
      if (currentFilter != null) {
        if (currentFilter.getBefore() != null) {
          dateComponent.setBefore(currentFilter.getBefore().getTime());
        }
        if (currentFilter.getAfter() != null) {
          dateComponent.setAfter(currentFilter.getAfter().getTime());
        }
      }

      DialogBuilder db = new DialogBuilder(DateFilterPopupComponent.this);
      db.addOkAction();
      db.setCenterPanel(dateComponent.getPanel());
      db.setPreferredFocusComponent(dateComponent.getPanel());
      db.setTitle("Select Period");
      if (DialogWrapper.OK_EXIT_CODE == db.show()) {
        long after = dateComponent.getAfter();
        long before = dateComponent.getBefore();
        VcsLogDateFilter filter = new VcsLogDateFilterImpl(after > 0 ? new Date(after) : null, before > 0 ? new Date(before) : null);
        myFilterModel.setFilter(filter);
      }
    }
  }
}
