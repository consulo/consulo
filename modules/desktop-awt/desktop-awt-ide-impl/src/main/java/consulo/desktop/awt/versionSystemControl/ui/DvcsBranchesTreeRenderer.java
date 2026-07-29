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
package consulo.desktop.awt.versionSystemControl.ui;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.SeparatorWithText;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.image.Image;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreeModel;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreePopupStepBase;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsRef;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsRepositoryModel;
import org.jspecify.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;

/**
 * @author VISTALL
 */
public class DvcsBranchesTreeRenderer implements TreeCellRenderer {
    private final DvcsBranchesTreePopupStepBase myStep;
    private final boolean myFavoriteToggleOnClickSupported;

    private final ColoredTreeCellRenderer myTextRenderer = new ColoredTreeCellRenderer() {
        @Override
        public void customizeCellRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            renderNode(this, value, selected);
        }
    };

    private final JLabel mySecondaryLabel = new JLabel();
    private final JLabel myArrowLabel = new JLabel();

    private final JPanel myCenterPanel = new JPanel(new BorderLayout());
    private final JPanel myMainPanel = new JPanel(new BorderLayout());

    public DvcsBranchesTreeRenderer(DvcsBranchesTreePopupStepBase step, boolean favoriteToggleOnClickSupported) {
        myStep = step;
        myFavoriteToggleOnClickSupported = favoriteToggleOnClickSupported;

        mySecondaryLabel.setOpaque(false);
        mySecondaryLabel.setBorder(JBUI.Borders.empty(0, 10));

        myArrowLabel.setOpaque(false);
        myArrowLabel.setBorder(JBUI.Borders.emptyLeft(4));

        myCenterPanel.setOpaque(false);
        myCenterPanel.add(myTextRenderer, BorderLayout.CENTER);
        myCenterPanel.add(mySecondaryLabel, BorderLayout.EAST);

        myMainPanel.setOpaque(false);
        myMainPanel.add(myCenterPanel, BorderLayout.CENTER);
        myMainPanel.add(myArrowLabel, BorderLayout.EAST);
        myMainPanel.setBorder(JBUI.Borders.emptyRight(6));
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Object userObject = TreeUtil.getUserObject(value);
        // render separator text in accessible mode
        if (userObject instanceof SeparatorWithText separator) {
            return separator;
        }

        myTextRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        myArrowLabel.setVisible(myStep.hasSubstep(userObject));
        myArrowLabel.setIcon(TargetAWT.to(selected ? PlatformIconGroup.ideMenuarrowselected() : PlatformIconGroup.ideMenuarrow()));

        String secondaryText = getSecondaryText(userObject);
        mySecondaryLabel.setText(secondaryText);
        mySecondaryLabel.setVisible(secondaryText != null);
        mySecondaryLabel.setForeground(selected ? UIUtil.getTreeSelectionForeground(true) : JBColor.GRAY);

        return myMainPanel;
    }

    @Nullable
    private String getSecondaryText(@Nullable Object userObject) {
        if (userObject instanceof AnAction action) {
            String shortcut = KeymapUtil.getFirstKeyboardShortcutText(action);
            return shortcut.isEmpty() ? null : shortcut;
        }
        if (userObject instanceof DvcsBranchesTreeModel.RefUnderRepository refUnderRepository) {
            return getCommonTrackedBranchName(refUnderRepository.ref(), List.of(refUnderRepository.repository()));
        }
        if (userObject instanceof DvcsRef ref) {
            return getCommonTrackedBranchName(ref, myStep.getAffectedRepositories());
        }
        return null;
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

    private void renderNode(ColoredTreeCellRenderer renderer, Object value, boolean selected) {
        Object userObject = TreeUtil.getUserObject(value);

        Image icon = getIcon(userObject, selected);
        renderer.setIcon(icon);

        String text = myStep.getNodeText(userObject);
        if (text == null) {
            text = "";
        }

        if (isDisabledActionItem(userObject)) {
            renderer.append(text, SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
        else if (isCurrentRef(userObject)) {
            renderer.append(text, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        }
        else {
            renderer.append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
    }

    @Nullable
    private Image getIcon(@Nullable Object treeNode, boolean isSelected) {
        if (treeNode instanceof DvcsBranchesTreeModel.BranchesPrefixGroup) {
            return DvcsBranchesTreeIconProvider.forGroup();
        }
        if (treeNode instanceof DvcsBranchesTreeModel.RefUnderRepository refUnderRepository) {
            return getBranchIcon(refUnderRepository.ref(), List.of(refUnderRepository.repository()), isSelected);
        }
        if (treeNode instanceof DvcsRef ref) {
            return getBranchIcon(ref, myStep.getAffectedRepositories(), isSelected);
        }
        if (treeNode instanceof DvcsBranchesTreeModel.RepositoryNode repositoryNode) {
            return repositoryNode.repository().getIcon();
        }
        if (treeNode instanceof AnAction action) {
            return action.getTemplatePresentation().getIcon();
        }
        return null;
    }

    private Image getBranchIcon(DvcsRef reference, List<DvcsRepositoryModel> repositories, boolean selected) {
        boolean isCurrent = allCurrent(reference, repositories);
        boolean isFavorite = allFavorite(reference, repositories);
        return DvcsBranchesTreeIconProvider.forRef(reference, isCurrent, isFavorite, selected, myFavoriteToggleOnClickSupported);
    }

    private boolean isCurrentRef(@Nullable Object userObject) {
        DvcsRef ref = null;
        List<DvcsRepositoryModel> repositories = myStep.getAffectedRepositories();
        if (userObject instanceof DvcsBranchesTreeModel.RefUnderRepository refUnderRepository) {
            ref = refUnderRepository.ref();
            repositories = List.of(refUnderRepository.repository());
        }
        else if (userObject instanceof DvcsRef r) {
            ref = r;
        }
        return ref != null && allCurrent(ref, repositories);
    }

    private static boolean allCurrent(DvcsRef ref, List<DvcsRepositoryModel> repositories) {
        if (repositories.isEmpty()) {
            return false;
        }
        for (DvcsRepositoryModel repo : repositories) {
            if (!repo.isCurrentRef(ref)) {
                return false;
            }
        }
        return true;
    }

    private static boolean allFavorite(DvcsRef ref, List<DvcsRepositoryModel> repositories) {
        if (repositories.isEmpty()) {
            return false;
        }
        for (DvcsRepositoryModel repo : repositories) {
            if (!repo.isFavorite(ref)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDisabledActionItem(@Nullable Object userObject) {
        return userObject instanceof AnAction action && !action.getTemplatePresentation().isEnabled();
    }
}
