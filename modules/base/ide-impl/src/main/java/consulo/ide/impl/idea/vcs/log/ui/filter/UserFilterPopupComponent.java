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

import consulo.dataContext.DataManager;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.popup.ListPopup;
import consulo.ide.impl.idea.openapi.util.Couple;
import consulo.ide.impl.idea.openapi.vcs.ui.FlatSpeedSearchPopup;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.vcs.log.VcsLogUserFilter;
import consulo.ide.impl.idea.vcs.log.data.MainVcsLogUiProperties;
import consulo.ide.impl.idea.vcs.log.data.VcsLogData;
import consulo.ide.impl.idea.vcs.log.impl.VcsLogUserFilterImpl;
import consulo.ide.impl.idea.vcs.log.util.VcsUserUtil;
import consulo.dataContext.DataContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Show a popup to select a user or enter the user name.
 */
class UserFilterPopupComponent extends MultipleValueFilterPopupComponent<VcsLogUserFilter> {
  @Nonnull
  private final VcsLogData myLogData;
  @Nonnull
  private final List<String> myAllUsers;

  UserFilterPopupComponent(@Nonnull MainVcsLogUiProperties uiProperties,
                           @Nonnull VcsLogData logData,
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
    return new UserLogSpeedSearchPopup(new DefaultActionGroup(actionGroup, speedsearchGroup),
                                       DataManager.getInstance().getDataContext(this));
  }

  @Nonnull
  private static List<String> collectUsers(@Nonnull VcsLogData logData) {
    List<String> users = ContainerUtil.map(logData.getAllUsers(), user -> {
      String shortPresentation = VcsUserUtil.getShortPresentation(user);
      Couple<String> firstAndLastName = VcsUserUtil.getFirstAndLastName(shortPresentation);
      if (firstAndLastName == null) return shortPresentation;
      return VcsUserUtil.capitalizeName(firstAndLastName.first) + " " + VcsUserUtil.capitalizeName(firstAndLastName.second);
    });
    TreeSet<String> sortedUniqueUsers = new TreeSet<>(users);
    return new ArrayList<>(sortedUniqueUsers);
  }

  private static class UserLogSpeedSearchPopup extends FlatSpeedSearchPopup {
    public UserLogSpeedSearchPopup(@Nonnull DefaultActionGroup actionGroup, @Nonnull DataContext dataContext) {
      super(null, actionGroup, dataContext, null, false);
    }

    @Override
    public boolean shouldBeShowing(@Nonnull AnAction action) {
      if (!super.shouldBeShowing(action)) return false;
      if (getSpeedSearch().isHoldingFilter()) {
        if (action instanceof MultipleValueFilterPopupComponent.PredefinedValueAction) {
          return action instanceof SpeedsearchAction ||
                 ((MultipleValueFilterPopupComponent.PredefinedValueAction)action).myValues.size() > 1;
        }
        return true;
      }
      else {
        return !isSpeedsearchAction(action);
      }
    }
  }

  private class SpeedsearchPredefinedValueAction extends PredefinedValueAction implements FlatSpeedSearchPopup.SpeedsearchAction {
    public SpeedsearchPredefinedValueAction(String user) {super(user);}
  }
}