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
package consulo.ide.impl.idea.application.options;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import consulo.localize.LocalizeValue;
import consulo.ui.Label;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;
import java.awt.*;

public class ShareSchemePanel {
    private JTextField mySharedSchemeName;
    private JTextArea mySharedSchemeDescription;
    private JPanel myPanel;

    public ShareSchemePanel() {
        init();
    }

    public String getName() {
        return mySharedSchemeName.getText();
    }

    public String getDescription() {
        return mySharedSchemeDescription.getText();
    }

    public JPanel getPanel() {
        return myPanel;
    }

    public void setName(String name) {
        mySharedSchemeName.setText(name);
    }

    private void init() {
        myPanel = new JPanel();
        myPanel.setLayout(new GridLayoutManager(2, 2, JBUI.emptyInsets(), -1, -1));
        myPanel.add(
            TargetAWT.to(Label.create(LocalizeValue.localizeTODO("Shared scheme name:"))),
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_EAST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        mySharedSchemeName = new JTextField();
        myPanel.add(
            mySharedSchemeName,
            new GridConstraints(
                0,
                1,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                new Dimension(150, -1),
                null,
                0,
                false
            )
        );
        JLabel label2 = new JLabel();
        label2.setHorizontalAlignment(2);
        label2.setText(LocalizeValue.localizeTODO("Description:").get());
        myPanel.add(
            label2,
            new GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_NORTHEAST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                1,
                null,
                null,
                null,
                0,
                false
            )
        );
        JBScrollPane jBScrollPane1 = new JBScrollPane();
        myPanel.add(
            jBScrollPane1,
            new GridConstraints(
                1,
                1,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                null,
                new Dimension(-1, 100),
                null,
                0,
                false
            )
        );
        mySharedSchemeDescription = new JTextArea();
        jBScrollPane1.setViewportView(mySharedSchemeDescription);
    }
}
