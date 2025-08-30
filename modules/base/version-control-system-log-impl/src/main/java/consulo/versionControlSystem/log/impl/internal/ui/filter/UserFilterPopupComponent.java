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
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Couple;
import consulo.versionControlSystem.internal.FlatSpeedSearchPopupFactory;
import consulo.versionControlSystem.log.VcsLogUserFilter;
import consulo.versionControlSystem.log.impl.internal.VcsLogUserFilterImpl;
import consulo.versionControlSystem.log.impl.internal.data.MainVcsLogUiProperties;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogDataImpl;
import consulo.versionControlSystem.log.util.VcsUserUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Show a popup to select a user or enter the user name.
 */
class UserFilterPopupComponent extends MultipleValueFilterPopupComponent<VcsLogUserFilter> {
    @Nonnull
    private final VcsLogDataImpl myLogData;
    @Nonnull
    private final List<String> myAllUsers;

    UserFilterPopupComponent(@Nonnull MainVcsLogUiProperties uiProperties,
                             @Nonnull VcsLogDataImpl logData,
                             @Nonnull FilterModel<VcsLogUserFilter> filterModel) {
        super("User", uiProperties, filterModel);
        myLogData = logData;
        myAllUsers = collectUsers(logData);
    }

    @Nonnull
    @Override
    protected String getText(@Nonnull VcsLogUserFilter filter) {
        return displayableText(myFilterModel.getFilterValues(filter));
    }

    @Nullable
    @Override
    protected String getToolTip(@Nonnull VcsLogUserFilter filter) {
        return tooltip(myFilterModel.getFilterValues(filter));
    }

    @Override
    protected ActionGroup createActionGroup() {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(createAllAction());
        group.add(createSelectMultipleValuesAction());
        if (!myLogData.getCurrentUser().isEmpty()) {
            group.add(new PredefinedValueAction(VcsLogUserFilterImpl.ME));
        }
        group.addAll(createRecentItemsActionGroup());
        return group;
    }

    @Nonnull
    protected ActionGroup createSpeedSearchActionGroup() {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new SpeedsearchPredefinedValueAction(VcsLogUserFilterImpl.ME));
        group.add(AnSeparator.getInstance());
        for (String user : myAllUsers) {
            group.add(new SpeedsearchPredefinedValueAction(user));
        }
        return group;
    }

    @Nonnull
    @Override
    protected List<List<String>> getRecentValuesFromSettings() {
        return myUiProperties.getRecentlyFilteredUserGroups();
    }

    @Override
    protected void rememberValuesInSettings(@Nonnull Collection<String> values) {
        myUiProperties.addRecentlyFilteredUserGroup(new ArrayList<>(values));
    }

    @Nonnull
    @Override
    protected List<String> getAllValues() {
        return myAllUsers;
    }

    @Nonnull
    @Override
    protected ListPopup createPopupMenu() {
        ActionGroup actionGroup = createActionGroup();
        ActionGroup speedsearchGroup = createSpeedSearchActionGroup();
        DataContext dataContext = DataManager.getInstance().getDataContext(this);
        ActionGroup popupGroup = ActionGroup.of(actionGroup, speedsearchGroup);

        return FlatSpeedSearchPopupFactory.getInstance()
            .createFlatSpeedSearchPopup(null, popupGroup, dataContext, null, false, (action, holdingFilter) -> {
                if (holdingFilter) {
                    if (action instanceof MultipleValueFilterPopupComponent.PredefinedValueAction) {
                        return action instanceof FlatSpeedSearchPopupFactory.SpeedsearchAction ||
                            ((MultipleValueFilterPopupComponent.PredefinedValueAction) action).myValues.size() > 1;
                    }
                    return true;
                }
                else {
                    return !FlatSpeedSearchPopupFactory.isSpeedsearchAction(action);
                }
            });
    }

    @Nonnull
    private static List<String> collectUsers(@Nonnull VcsLogDataImpl logData) {
        List<String> users = ContainerUtil.map(logData.getAllUsers(), user -> {
            String shortPresentation = VcsUserUtil.getShortPresentation(user);
            Couple<String> firstAndLastName = VcsUserUtil.getFirstAndLastName(shortPresentation);
            if (firstAndLastName == null) {
                return shortPresentation;
            }
            return VcsUserUtil.capitalizeName(firstAndLastName.first) + " " + VcsUserUtil.capitalizeName(firstAndLastName.second);
        });
        TreeSet<String> sortedUniqueUsers = new TreeSet<>(users);
        return new ArrayList<>(sortedUniqueUsers);
    }

    private class SpeedsearchPredefinedValueAction extends PredefinedValueAction implements FlatSpeedSearchPopupFactory.SpeedsearchAction {
        public SpeedsearchPredefinedValueAction(String user) {
            super(user);
        }
    }
}