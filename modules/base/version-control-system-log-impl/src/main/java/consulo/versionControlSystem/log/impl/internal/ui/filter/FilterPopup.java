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

import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.versionControlSystem.log.VcsLogFilter;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import org.jspecify.annotations.Nullable;

/**
 * Lets the user set up a filter of the VCS Log by picking one of the choices of a popup. Holds no widget: the toolbar
 * of a log shows it as it can.
 */
public abstract class FilterPopup<Filter extends VcsLogFilter> {
    private final LocalizeValue myName;
    protected final FilterModel<Filter> myFilterModel;

    FilterPopup(LocalizeValue name, FilterModel<Filter> filterModel) {
        myName = name;
        myFilterModel = filterModel;
    }

    public LocalizeValue getName() {
        return myName;
    }

    public String getCurrentText() {
        Filter filter = myFilterModel.getFilter();
        return filter == null ? VersionControlSystemLogLocalize.filterAll().get() : getText(filter);
    }

    public @Nullable String getToolTip() {
        Filter filter = myFilterModel.getFilter();
        return filter == null ? null : getToolTip(filter);
    }

    public void addChangeListener(Runnable onChange) {
        myFilterModel.addSetFilterListener(onChange);
    }

    /**
     * Create popup actions available under this filter.
     */
    public abstract ActionGroup createActionGroup();

    public ListPopup createPopup(DataContext context) {
        return JBPopupFactory.getInstance()
            .createActionGroupPopup(null, createActionGroup(), context, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
    }

    protected abstract String getText(Filter filter);

    protected abstract @Nullable String getToolTip(Filter filter);

    /**
     * Returns the special action that indicates that no filtering is selected in this component.
     */
    protected AnAction createAllAction() {
        return new AllAction();
    }

    private class AllAction extends DumbAwareAction {
        AllAction() {
            super(VersionControlSystemLogLocalize.filterAll());
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            myFilterModel.setFilter(null);
        }
    }
}
