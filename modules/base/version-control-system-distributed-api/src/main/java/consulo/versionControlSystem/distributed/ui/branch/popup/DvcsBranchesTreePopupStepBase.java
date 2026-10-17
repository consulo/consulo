// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.versionControlSystem.distributed.ui.branch.popup;

import consulo.annotation.UsedInPlugin;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.application.util.matcher.NameUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.ex.popup.MnemonicNavigationFilter;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.ex.popup.SpeedSearchFilter;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.internal.VersionControlSystemInternal;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 */
public abstract class DvcsBranchesTreePopupStepBase implements PopupStep<Object> {
    protected final Project myProject;
    @Nullable
    protected final DvcsRepositoryModel mySelectedRepository;
    protected final List<DvcsRepositoryModel> myRepositories;
    protected final List<DvcsRepositoryModel> myAffectedRepositories;

    protected DvcsBranchesTreePopupStepBase(Project project,
                                            @Nullable DvcsRepositoryModel selectedRepository,
                                            List<DvcsRepositoryModel> repositories) {
        myProject = project;
        mySelectedRepository = selectedRepository;
        myRepositories = repositories;
        myAffectedRepositories = selectedRepository != null ? List.of(selectedRepository) : repositories;
    }

    public Project getProject() {
        return myProject;
    }

    @Nullable
    public DvcsRepositoryModel getSelectedRepository() {
        return mySelectedRepository;
    }

    public List<DvcsRepositoryModel> getRepositories() {
        return myRepositories;
    }

    public List<DvcsRepositoryModel> getAffectedRepositories() {
        return myAffectedRepositories;
    }

    public abstract DvcsBranchesTreeModel getTreeModel();

    /**
     * Counterpart of {@code GitBranchesPopupBase.getSearchFiledEmptyText}: the placeholder shown in
     * the always-visible search field.
     */
    public abstract LocalizeValue getSearchFieldEmptyText();

    protected abstract DvcsBranchesTreeModel createTreeModel(boolean filterActive);

    protected abstract void setTreeModel(DvcsBranchesTreeModel treeModel);

    @Nullable
    public List<Object> getPreferredSelection() {
        return getTreeModel().getPreferredSelection();
    }

    @Nullable
    public List<Object> createTreePathFor(Object value) {
        return DvcsBranchesTreeModelUtil.createTreePathFor(getTreeModel(), value);
    }

    public void setSearchPattern(@Nullable String pattern) {
        if (pattern == null || pattern.equals("/")) {
            getTreeModel().applyFilterAndRebuild(null);
            return;
        }

        String trimmedPattern = pattern.trim(); // otherwise Character.isSpaceChar would affect filtering
        MinusculeMatcher matcher = NameUtil.buildMatcher("*" + trimmedPattern).build();
        getTreeModel().applyFilterAndRebuild(matcher);
    }

    public boolean updateTreeModelIfNeeded(@Nullable String pattern) {
        if (shouldValidateNotNullTreeModel()) {
            return false;
        }

        boolean filterActive = !(StringUtil.isEmptyOrSpaces(pattern) || "/".equals(pattern));
        setTreeModel(createTreeModel(filterActive));
        return true;
    }

    protected boolean shouldValidateNotNullTreeModel() {
        return myAffectedRepositories.size() == 1;
    }

    @Override
    public boolean hasSubstep(@Nullable Object selectedValue) {
        if (selectedValue == null) {
            return false;
        }
        if (selectedValue instanceof AnAction action) {
            return action.getTemplatePresentation().isEnabled() && action instanceof ActionGroup;
        }
        return getTreeModel().isSelectable(selectedValue);
    }

    public boolean isSelectable(@Nullable Object node) {
        return getTreeModel().isSelectable(node);
    }

    @Nullable
    @Override
    public String getTitle() {
        return null;
    }

    @Nullable
    @Override
    public PopupStep onChosen(Object selectedValue, boolean finalChoice) {
        return FINAL_CHOICE;
    }

    @Override
    public void canceled() {
    }

    @Override
    public boolean isMnemonicsNavigationEnabled() {
        return false;
    }

    @Nullable
    @Override
    public MnemonicNavigationFilter<Object> getMnemonicNavigationFilter() {
        return null;
    }

    @Override
    public boolean isSpeedSearchEnabled() {
        return true;
    }

    @Override
    public SpeedSearchFilter<Object> getSpeedSearchFilter() {
        return node -> {
            if (node instanceof DvcsRef ref) {
                return ref.getName();
            }
            String text = getNodeText(node);
            return text != null ? text : "";
        };
    }

    @Nullable
    public String getNodeText(@Nullable Object node) {
        if (node == null) {
            return null;
        }
        if (node instanceof DvcsRefType type) {
            if (mySelectedRepository != null) {
                return type.getInRepoText(mySelectedRepository.getShortName()).get();
            }
            else if (myRepositories.size() > 1) {
                return type.getCommonText().get();
            }
            else {
                return type.getText().get();
            }
        }
        if (node instanceof DvcsBranchesTreeModel.BranchesPrefixGroup group) {
            List<String> prefix = group.prefix();
            return prefix.get(prefix.size() - 1);
        }
        if (node instanceof DvcsBranchesTreeModel.RefTypeUnderRepository typeUnderRepository) {
            return typeUnderRepository.type().getText().get();
        }
        if (node instanceof DvcsBranchesTreeModel.RefUnderRepository refUnderRepository) {
            return getRefText(refUnderRepository.ref(), getTreeModel().isDirectoryGrouping());
        }
        if (node instanceof DvcsRef ref) {
            return getRefText(ref, getTreeModel().isDirectoryGrouping());
        }
        if (node instanceof AnAction action) {
            return action.getTemplatePresentation().getText();
        }
        if (node instanceof DvcsBranchesTreeModel.PresentableNode presentableNode) {
            return presentableNode.getPresentableText().getNullIfEmpty();
        }
        return null;
    }

    public void renderNode(@Nullable Object node, TextItemPresentation presentation) {
        presentation.withIcon(getNodeIcon(node, false, false));
        presentation.append(StringUtil.notNullize(getNodeText(node)), getNodeTextAttribute(node));

        String secondaryText = getNodeSecondaryText(node);
        Image arrow = hasSubstep(node) ? PlatformIconGroup.ideMenuarrow() : null;
        if (secondaryText != null || arrow != null) {
            presentation.withSuffix(LocalizeValue.ofNullable(secondaryText), arrow);
        }
    }

    public TextAttribute getNodeTextAttribute(@Nullable Object node) {
        if (node instanceof AnAction action && !action.getTemplatePresentation().isEnabled()) {
            return TextAttribute.GRAYED;
        }
        if (isCurrentRef(node)) {
            return TextAttribute.REGULAR_BOLD;
        }
        return TextAttribute.REGULAR;
    }

    @Nullable
    public Image getNodeIcon(@Nullable Object node, boolean selected, boolean favoriteToggleOnClick) {
        if (node instanceof DvcsBranchesTreeModel.BranchesPrefixGroup) {
            return DvcsBranchesTreeIconProvider.forGroup();
        }
        if (node instanceof DvcsBranchesTreeModel.RefUnderRepository refUnderRepository) {
            return getBranchIcon(refUnderRepository.ref(), List.of(refUnderRepository.repository()), selected, favoriteToggleOnClick);
        }
        if (node instanceof DvcsRef ref) {
            return getBranchIcon(ref, myAffectedRepositories, selected, favoriteToggleOnClick);
        }
        if (node instanceof DvcsBranchesTreeModel.RepositoryNode repositoryNode) {
            return repositoryNode.repository().getIcon();
        }
        if (node instanceof AnAction action) {
            return action.getTemplatePresentation().getIcon();
        }
        return null;
    }

    @Nullable
    public String getNodeSecondaryText(@Nullable Object node) {
        if (node instanceof AnAction action) {
            String shortcut = KeymapUtil.getFirstKeyboardShortcutText(action);
            return shortcut.isEmpty() ? null : shortcut;
        }
        if (node instanceof DvcsBranchesTreeModel.RefUnderRepository refUnderRepository) {
            return getCommonTrackedBranchName(refUnderRepository.ref(), List.of(refUnderRepository.repository()));
        }
        if (node instanceof DvcsRef ref) {
            return getCommonTrackedBranchName(ref, myAffectedRepositories);
        }
        return null;
    }

    private boolean isCurrentRef(@Nullable Object node) {
        if (node instanceof DvcsBranchesTreeModel.RefUnderRepository refUnderRepository) {
            return allCurrent(refUnderRepository.ref(), List.of(refUnderRepository.repository()));
        }
        if (node instanceof DvcsRef ref) {
            return allCurrent(ref, myAffectedRepositories);
        }
        return false;
    }

    private static Image getBranchIcon(
        DvcsRef ref,
        List<DvcsRepositoryModel> repositories,
        boolean selected,
        boolean favoriteToggleOnClick
    ) {
        boolean current = allCurrent(ref, repositories);
        boolean favorite = allFavorite(ref, repositories);
        return DvcsBranchesTreeIconProvider.forRef(ref, current, favorite, selected, favoriteToggleOnClick);
    }

    @Nullable
    private static String getCommonTrackedBranchName(DvcsRef ref, List<DvcsRepositoryModel> repositories) {
        String commonTrackedBranch = null;
        for (DvcsRepositoryModel repository : repositories) {
            String trackedBranch = repository.getTrackedBranchName(ref);
            if (trackedBranch == null) {
                return null;
            }
            if (commonTrackedBranch == null) {
                commonTrackedBranch = trackedBranch;
            }
            else if (!commonTrackedBranch.equals(trackedBranch)) {
                return null;
            }
        }
        return commonTrackedBranch;
    }

    private static boolean allCurrent(DvcsRef ref, List<DvcsRepositoryModel> repositories) {
        if (repositories.isEmpty()) {
            return false;
        }
        for (DvcsRepositoryModel repository : repositories) {
            if (!repository.isCurrentRef(ref)) {
                return false;
            }
        }
        return true;
    }

    private static boolean allFavorite(DvcsRef ref, List<DvcsRepositoryModel> repositories) {
        if (repositories.isEmpty()) {
            return false;
        }
        for (DvcsRepositoryModel repository : repositories) {
            if (!repository.isFavorite(ref)) {
                return false;
            }
        }
        return true;
    }

    private static String getRefText(DvcsRef value, boolean prefixGrouping) {
        String name = value.getName();
        if (prefixGrouping) {
            int idx = name.lastIndexOf('/');
            return idx >= 0 ? name.substring(idx + 1) : name;
        }
        return name;
    }

    @RequiredUIAccess
    @UsedInPlugin
    protected void invokeAction(AnAction action) {
        VersionControlSystemInternal.getInstance().invokeAction(myProject, action);
    }

    @Override
    public boolean isAutoSelectionEnabled() {
        return false;
    }

    @Nullable
    @Override
    public Runnable getFinalRunnable() {
        return null;
    }
}
