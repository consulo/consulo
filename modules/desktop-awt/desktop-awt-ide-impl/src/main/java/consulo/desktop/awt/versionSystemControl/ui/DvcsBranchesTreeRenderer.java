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
import consulo.ui.ex.awt.ColoredTextItemPresentation;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.SeparatorWithText;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreePopupStepBase;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreeSeparator;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;

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

    private final SeparatorWithText mySeparator = new SeparatorWithText();

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
        if (userObject instanceof DvcsBranchesTreeSeparator) {
            return mySeparator;
        }

        myTextRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        myArrowLabel.setVisible(myStep.hasSubstep(userObject));
        myArrowLabel.setIcon(TargetAWT.to(selected ? PlatformIconGroup.ideMenuarrowselected() : PlatformIconGroup.ideMenuarrow()));

        String secondaryText = myStep.getNodeSecondaryText(userObject);
        mySecondaryLabel.setText(secondaryText);
        mySecondaryLabel.setVisible(secondaryText != null);
        mySecondaryLabel.setForeground(selected ? UIUtil.getTreeSelectionForeground(true) : JBColor.GRAY);

        return myMainPanel;
    }

    private void renderNode(ColoredTreeCellRenderer renderer, Object value, boolean selected) {
        Object userObject = TreeUtil.getUserObject(value);

        renderer.setIcon(myStep.getNodeIcon(userObject, selected, myFavoriteToggleOnClickSupported));

        ColoredTextItemPresentation presentation = new ColoredTextItemPresentation(renderer);
        presentation.append(StringUtil.notNullize(myStep.getNodeText(userObject)), myStep.getNodeTextAttribute(userObject));
    }
}
