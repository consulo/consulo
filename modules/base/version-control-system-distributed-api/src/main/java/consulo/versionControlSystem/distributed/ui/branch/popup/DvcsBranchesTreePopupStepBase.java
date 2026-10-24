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
    private final DvcsBranchesTreeNodePresentation myPresentation;

    protected DvcsBranchesTreePopupStepBase(Project project,
                                            @Nullable DvcsRepositoryModel selectedRepository,
                                            List<DvcsRepositoryModel> repositories) {
        myProject = project;
        mySelectedRepository = selectedRepository;
        myRepositories = repositories;
        myAffectedRepositories = selectedRepository != null ? List.of(selectedRepository) : repositories;
        myPresentation = new DvcsBranchesTreeNodePresentation(this::getTreeModel, selectedRepository, repositories);
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
        getTreeModel().setSearchPattern(pattern);
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
        if (node instanceof AnAction action) {
            return action.getTemplatePresentation().getText();
        }
        return myPresentation.getText(node);
    }

    public void renderNode(@Nullable Object node, TextItemPresentation presentation) {
        presentation.withIcon(getNodeIcon(node, false, false));
        presentation.append(StringUtil.notNullize(getNodeText(node)), getNodeTextAttribute(node));
        myPresentation.renderInOut(node, presentation);

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
        return myPresentation.getTextAttribute(node);
    }

    @Nullable
    public Image getNodeIcon(@Nullable Object node, boolean selected, boolean favoriteToggleOnClick) {
        if (node instanceof AnAction action) {
            return action.getTemplatePresentation().getIcon();
        }
        return myPresentation.getIcon(node, selected, favoriteToggleOnClick);
    }

    @Nullable
    public String getNodeSecondaryText(@Nullable Object node) {
        if (node instanceof AnAction action) {
            String shortcut = KeymapUtil.getFirstKeyboardShortcutText(action);
            return shortcut.isEmpty() ? null : shortcut;
        }
        return myPresentation.getSecondaryText(node);
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
