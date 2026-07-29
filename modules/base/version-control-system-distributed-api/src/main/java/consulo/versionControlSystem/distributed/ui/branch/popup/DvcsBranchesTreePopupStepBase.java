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

import consulo.application.util.matcher.MinusculeMatcher;
import consulo.application.util.matcher.NameUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.popup.MnemonicNavigationFilter;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.ex.popup.SpeedSearchFilter;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.tree.TreePath;
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
    public TreePath getPreferredSelection() {
        return getTreeModel().getPreferredSelection();
    }

    @Nullable
    public TreePath createTreePathFor(Object value) {
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

    public void updateTreeModelIfNeeded(Tree tree, @Nullable String pattern) {
        if (shouldValidateNotNullTreeModel()) {
            if (tree.getModel() == null) {
                throw new IllegalStateException("Provided tree with null model");
            }
            return;
        }

        boolean filterActive = !(StringUtil.isEmptyOrSpaces(pattern) || "/".equals(pattern));
        setTreeModel(createTreeModel(filterActive));
        tree.setModel(getTreeModel());
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
            return presentableNode.getPresentableText();
        }
        return null;
    }

    private static String getRefText(DvcsRef value, boolean prefixGrouping) {
        String name = value.getName();
        if (prefixGrouping) {
            int idx = name.lastIndexOf('/');
            return idx >= 0 ? name.substring(idx + 1) : name;
        }
        return name;
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
