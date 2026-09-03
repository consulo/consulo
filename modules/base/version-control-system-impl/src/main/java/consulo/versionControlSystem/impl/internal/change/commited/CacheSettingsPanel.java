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
package consulo.versionControlSystem.impl.internal.change.commited;

import consulo.configurable.ConfigurationException;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;
import consulo.versionControlSystem.localize.VcsLocalize;

/**
 * @author yole
 */
public class CacheSettingsPanel {
    private CommittedChangesCache myCache;

    private IntBox myCountBox;
    private IntBox myDaysBox;
    private IntBox myRefreshBox;
    private CheckBox myRefreshCheckBox;

    private Component myCountRow;
    private Component myDaysRow;

    public void initPanel(Project project) {
        myCache = CommittedChangesCache.getInstance(project);
    }

    @RequiredUIAccess
    public Component createComponent() {
        VerticalLayout root = VerticalLayout.create();

        myCountBox = IntBox.create(500).withRange(1, 100000).withStep(10);
        myCountRow = LabeledBuilder.sided(VcsLocalize.changesChangelistsToCacheInitially(), myCountBox);
        root.add(myCountRow);

        myDaysBox = IntBox.create(90).withRange(1, 720).withStep(10);
        myDaysRow = LabeledBuilder.sided(VcsLocalize.changesDaysOfHistoryToCacheInitially(), myDaysBox);
        root.add(myDaysRow);

        myRefreshCheckBox = CheckBox.create(VcsLocalize.changesRefreshChangesEvery());
        myRefreshBox = IntBox.create(30).withRange(1, 60 * 24);
        myRefreshCheckBox.addValueListener(event -> myRefreshBox.setEnabled(event.getValue()));

        root.add(DockLayout.create()
            .left(myRefreshCheckBox)
            .right(HorizontalLayout.create(5).add(myRefreshBox).add(Label.create(VcsLocalize.changesMinutes()))));

        return root;
    }

    @RequiredUIAccess
    public void apply() throws ConfigurationException {
        CommittedChangesCache.State state = new CommittedChangesCache.State();
        state.setInitialCount(myCountBox.getValueOrError());
        state.setInitialDays(myDaysBox.getValueOrError());
        state.setRefreshInterval(myRefreshBox.getValueOrError());
        state.setRefreshEnabled(myRefreshCheckBox.getValueOrError());
        myCache.loadState(state);
    }

    @RequiredUIAccess
    public boolean isModified() {
        CommittedChangesCache.State state = myCache.getState();

        if (state.getInitialCount() != myCountBox.getValueOrError()) {
            return true;
        }
        if (state.getInitialDays() != myDaysBox.getValueOrError()) {
            return true;
        }
        if (state.getRefreshInterval() != myRefreshBox.getValueOrError()) {
            return true;
        }
        if (state.isRefreshEnabled() != myRefreshCheckBox.getValueOrError()) {
            return true;
        }

        return false;
    }

    @RequiredUIAccess
    public void reset() {
        CommittedChangesCache.State state = myCache.getState();

        myCountBox.setValue(state.getInitialCount());
        myDaysBox.setValue(state.getInitialDays());
        myRefreshBox.setValue(state.getRefreshInterval());

        boolean maxCountSupported = myCache.isMaxCountSupportedForProject();
        myCountRow.setVisible(maxCountSupported);
        myDaysRow.setVisible(!maxCountSupported);

        myRefreshCheckBox.setValue(state.isRefreshEnabled());
        myRefreshBox.setEnabled(state.isRefreshEnabled());
    }
}
