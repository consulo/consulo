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

import consulo.localize.LocalizeValue;
import consulo.ui.InputBoxBuilder;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.log.VcsLogFilter;
import consulo.versionControlSystem.log.impl.internal.data.MainVcsLogUiProperties;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

abstract class MultipleValueFilterPopup<Filter extends VcsLogFilter> extends FilterPopup<Filter> {
    private static final int MAX_FILTER_VALUE_LENGTH = 30;

    protected final MainVcsLogUiProperties myUiProperties;

    MultipleValueFilterPopup(LocalizeValue filterName, MainVcsLogUiProperties uiProperties, FilterModel<Filter> filterModel) {
        super(filterName, filterModel);
        myUiProperties = uiProperties;
    }

    protected abstract List<List<String>> getRecentValuesFromSettings();

    protected abstract void rememberValuesInSettings(Collection<String> values);

    protected abstract List<String> getAllValues();

    protected ActionGroup createRecentItemsActionGroup() {
        DefaultActionGroup group = new DefaultActionGroup();
        List<List<String>> recentlyFiltered = getRecentValuesFromSettings();
        if (!recentlyFiltered.isEmpty()) {
            group.add(AnSeparator.create(VersionControlSystemLogLocalize.filterRecent()));
            for (List<String> recentGroup : recentlyFiltered) {
                if (!recentGroup.isEmpty()) {
                    group.add(new PredefinedValueAction(recentGroup));
                }
            }
            group.addSeparator();
        }
        return group;
    }

    static String displayableText(Collection<String> values) {
        if (values.size() == 1) {
            return values.iterator().next();
        }
        return StringUtil.shortenTextWithEllipsis(StringUtil.join(values, "|"), MAX_FILTER_VALUE_LENGTH, 0, true);
    }

    static String tooltip(Collection<String> values) {
        return StringUtil.join(values, ", ");
    }

    protected AnAction createSelectMultipleValuesAction() {
        return new SelectMultipleValuesAction();
    }

    /**
     * Return true if the filter supports "negative" values, i.e. values like "-value" which means "match anything but 'value'".
     */
    protected boolean supportsNegativeValues() {
        return false;
    }

    static List<String> parseValues(String text) {
        List<String> values = new ArrayList<>();
        for (String value : StringUtil.tokenize(text, "|\n")) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    protected class PredefinedValueAction extends DumbAwareAction {
        protected final List<String> myValues;

        public PredefinedValueAction(String value) {
            this(Collections.singletonList(value));
        }

        public PredefinedValueAction(List<String> values) {
            super(LocalizeValue.of(displayableText(values)), LocalizeValue.of(tooltip(values)));
            myValues = values;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            myFilterModel.setFilter(myFilterModel.createFilter(myValues));
            rememberValuesInSettings(myValues);
        }
    }

    private class SelectMultipleValuesAction extends DumbAwareAction {
        SelectMultipleValuesAction() {
            super(VersionControlSystemLogLocalize.filterSelect());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            Filter filter = myFilterModel.getFilter();
            List<String> values = filter == null ? Collections.emptyList() : myFilterModel.getFilterValues(filter);
            UIAccess uiAccess = UIAccess.current();
            InputBoxBuilder.text()
                .title(VersionControlSystemLogLocalize.filterSelectValuesTitle(getName()))
                .text(supportsNegativeValues()
                    ? VersionControlSystemLogLocalize.filterSelectValuesNegativeText()
                    : VersionControlSystemLogLocalize.filterSelectValuesText())
                .value(StringUtil.join(values, "|"))
                .showAsync()
                .whenCompleteAsync((text, error) -> {
                    if (text == null) {
                        return;
                    }
                    List<String> selectedValues = parseValues(text);
                    if (selectedValues.isEmpty()) {
                        myFilterModel.setFilter(null);
                    }
                    else {
                        myFilterModel.setFilter(myFilterModel.createFilter(selectedValues));
                        rememberValuesInSettings(selectedValues);
                    }
                }, uiAccess);
        }
    }
}
