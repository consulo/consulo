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
import consulo.dataContext.DataManager;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.log.VcsLogBranchFilter;
import consulo.versionControlSystem.log.VcsLogDataPack;
import consulo.versionControlSystem.log.VcsRef;
import consulo.versionControlSystem.log.impl.internal.data.MainVcsLogUiProperties;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogUiImpl;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BranchFilterPopupComponent extends MultipleValueFilterPopupComponent<VcsLogBranchFilter> {
    @Nonnull
    private final VcsLogUiImpl myUi;
    private VcsLogClassicFilterUi.BranchFilterModel myBranchFilterModel;

    public BranchFilterPopupComponent(
        @Nonnull VcsLogUiImpl ui,
        @Nonnull MainVcsLogUiProperties uiProperties,
        @Nonnull VcsLogClassicFilterUi.BranchFilterModel filterModel
    ) {
        super("Branch", uiProperties, filterModel);
        myUi = ui;
        myBranchFilterModel = filterModel;
    }

    @Nonnull
    @Override
    protected String getText(@Nonnull VcsLogBranchFilter filter) {
        return MultipleValueFilterPopupComponent.displayableText(myFilterModel.getFilterValues(filter));
    }

    @Nullable
    @Override
    protected String getToolTip(@Nonnull VcsLogBranchFilter filter) {
        return MultipleValueFilterPopupComponent.tooltip(myFilterModel.getFilterValues(filter));
    }

    @Override
    protected boolean supportsNegativeValues() {
        return true;
    }

    @Nonnull
    @Override
    protected ListPopup createPopupMenu() {
        ActionGroup actionGroup = createActionGroup();
        DataContext context = DataManager.getInstance().getDataContext(this);
        return BranchLogSpeedSearchPopup.createSpeedSearchPopup(actionGroup, context);
    }

    @Override
    protected ActionGroup createActionGroup() {
        ActionGroup.Builder actionGroup = ActionGroup.newImmutableBuilder();

        actionGroup.add(createAllAction());
        actionGroup.add(createSelectMultipleValuesAction());

        actionGroup.add(
            new MyBranchPopupBuilder(
                myFilterModel.getDataPack(),
                myBranchFilterModel.getVisibleRoots(),
                getRecentValuesFromSettings()
            ).build());
        return actionGroup.build();
    }

    @Nonnull
    @Override
    protected List<List<String>> getRecentValuesFromSettings() {
        return myUiProperties.getRecentlyFilteredBranchGroups();
    }

    @Override
    protected void rememberValuesInSettings(@Nonnull Collection<String> values) {
        myUiProperties.addRecentlyFilteredBranchGroup(new ArrayList<>(values));
    }

    @Nonnull
    @Override
    protected List<String> getAllValues() {
        return ContainerUtil.map(myFilterModel.getDataPack().getRefs().getBranches(), VcsRef::getName);
    }

    private class MyBranchPopupBuilder extends BranchPopupBuilder {
        protected MyBranchPopupBuilder(
            @Nonnull VcsLogDataPack dataPack,
            @Nullable Collection<VirtualFile> visibleRoots,
            @Nullable List<List<String>> recentItems
        ) {
            super(dataPack, visibleRoots, recentItems);
        }

        @Nonnull
        @Override
        public AnAction createAction(@Nonnull String name) {
            return new PredefinedValueAction(name) {
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    myFilterModel.setFilter(myFilterModel.createFilter(myValues)); // does not add to recent
                }
            };
        }

        @Override
        protected void createRecentAction(@Nonnull DefaultActionGroup actionGroup, @Nonnull List<String> recentItem) {
            actionGroup.add(new PredefinedValueAction(recentItem));
        }

        @Nonnull
        @Override
        protected AnAction createCollapsedAction(String actionName) {
            return new PredefinedValueAction(actionName); // adds to recent
        }
    }
}
