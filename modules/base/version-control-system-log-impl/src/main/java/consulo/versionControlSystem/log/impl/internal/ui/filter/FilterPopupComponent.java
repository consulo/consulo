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

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.versionControlSystem.log.VcsLogFilter;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogPopupComponent;
import org.jspecify.annotations.Nullable;

/**
 * Base class for components which allow to set up filter for the VCS Log, by displaying a popup with available choices.
 */
abstract class FilterPopupComponent<Filter extends VcsLogFilter> extends VcsLogPopupComponent {
    /**
     * Special value that indicates that no filtering is on.
     */
    protected static final String ALL = "All";
    
    protected final FilterModel<Filter> myFilterModel;

    FilterPopupComponent(String filterName, FilterModel<Filter> filterModel) {
        super(filterName);
        myFilterModel = filterModel;
    }

    @Override
    public String getCurrentText() {
        Filter filter = myFilterModel.getFilter();
        return filter == null ? ALL : getText(filter);
    }

    @Override
    public void installChangeListener(Runnable onChange) {
        myFilterModel.addSetFilterListener(onChange);
    }

    
    protected abstract String getText(Filter filter);

    protected abstract @Nullable String getToolTip(Filter filter);

    @Override
    public String getToolTipText() {
        Filter filter = myFilterModel.getFilter();
        return filter == null ? null : getToolTip(filter);
    }

    /**
     * Returns the special action that indicates that no filtering is selected in this component.
     */
    protected AnAction createAllAction() {
        return new AllAction();
    }

    private class AllAction extends DumbAwareAction {
        AllAction() {
            super(ALL);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            myFilterModel.setFilter(null);
        }
    }
}
