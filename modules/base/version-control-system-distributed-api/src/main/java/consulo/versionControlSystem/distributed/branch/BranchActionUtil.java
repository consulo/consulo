/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.distributed.branch;

import consulo.annotation.UsedInPlugin;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.distributed.internal.BranchHideableActionGroup;
import consulo.versionControlSystem.distributed.internal.BranchMoreAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Comparator;
import java.util.List;

@UsedInPlugin
public class BranchActionUtil {
    public static final Comparator<BranchActionGroup> FAVORITE_BRANCH_COMPARATOR =
        Comparator.comparing(branch -> branch.isFavorite() ? -1 : 0);

    public static int getNumOfFavorites(@Nonnull List<? extends BranchActionGroup> branchActions) {
        return ContainerUtil.count(branchActions, BranchActionGroup::isFavorite);
    }

    public static int getNumOfTopShownBranches(@Nonnull List<? extends BranchActionGroup> branchActions) {
        int numOfFavorites = getNumOfFavorites(branchActions);
        return branchActions.size() > DvcsBranchPopup.MyMoreIndex.MAX_BRANCH_NUM && numOfFavorites > 0 ? numOfFavorites : DvcsBranchPopup.MyMoreIndex.MAX_BRANCH_NUM;
    }

    public static void wrapWithMoreActionIfNeeded(
        @Nonnull Project project,
        @Nonnull ActionGroup.Builder parentGroup,
        @Nonnull List<? extends ActionGroup> actionList,
        int maxIndex,
        @Nullable String settingName
    ) {
        wrapWithMoreActionIfNeeded(project, parentGroup, actionList, maxIndex, settingName, false);
    }

    public static void wrapWithMoreActionIfNeeded(
        @Nonnull Project project,
        @Nonnull ActionGroup.Builder parentGroup,
        @Nonnull List<? extends ActionGroup> actionList,
        int maxIndex,
        @Nullable String settingName,
        boolean defaultExpandValue
    ) {
        if (actionList.size() > maxIndex) {
            boolean hasFavorites = actionList.stream()
                .anyMatch(action -> action instanceof BranchActionGroup && ((BranchActionGroup) action).isFavorite());
            BranchMoreAction moreAction =
                new BranchMoreAction(project, actionList.size() - maxIndex, settingName, defaultExpandValue, hasFavorites);
            for (int i = 0; i < actionList.size(); i++) {
                parentGroup.add(i < maxIndex ? actionList.get(i) : new BranchHideableActionGroup(actionList.get(i), moreAction));
            }
            parentGroup.add(moreAction);
        }
        else {
            parentGroup.addAll(actionList);
        }
    }

}
