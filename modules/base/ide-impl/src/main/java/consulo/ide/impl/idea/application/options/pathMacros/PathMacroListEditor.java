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
package consulo.ide.impl.idea.application.options.pathMacros;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import consulo.application.Application;
import consulo.application.macro.PathMacros;
import consulo.configurable.ConfigurationException;
import consulo.localize.LocalizeValue;
import consulo.ui.Label;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.ToolbarDecorator;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.style.StandardColors;
import consulo.util.lang.StringUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author dsl
 */
public class PathMacroListEditor {
    JPanel myPanel;
    private TextBox myIgnoredVariables;
    private JPanel myPathVariablesPanel;
    private PathMacroTable myPathMacroTable;

    @RequiredUIAccess
    public PathMacroListEditor() {
        this(null);
    }

    @RequiredUIAccess
    public PathMacroListEditor(Collection<String> undefinedMacroNames) {
        createUIComponents();
        myPathMacroTable = undefinedMacroNames != null ? new PathMacroTable(undefinedMacroNames) : new PathMacroTable();
        myPathVariablesPanel.add(
            ToolbarDecorator.createDecorator(myPathMacroTable)
                .setAddAction(button -> myPathMacroTable.addMacro())
                .setRemoveAction(button -> myPathMacroTable.removeSelectedMacros())
                .setEditAction(button -> myPathMacroTable.editMacro())
                .disableUpDownActions()
                .createPanel(),
            BorderLayout.CENTER
        );

        fillIgnoredVariables();
    }

    @RequiredUIAccess
    private void fillIgnoredVariables() {
        Collection<String> ignored = PathMacros.getInstance().getIgnoredMacroNames();
        myIgnoredVariables.setValue(StringUtil.join(ignored, ";"));
    }

    private boolean isIgnoredModified() {
        Collection<String> ignored = PathMacros.getInstance().getIgnoredMacroNames();
        return !parseIgnoredVariables().equals(ignored);
    }

    private Collection<String> parseIgnoredVariables() {
        String s = myIgnoredVariables.getValue();
        List<String> ignored = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(s, ";");
        while (st.hasMoreElements()) {
            ignored.add(st.nextToken().trim());
        }

        return ignored;
    }

    @RequiredUIAccess
    public void commit() throws ConfigurationException {
        Application.get().runWriteAction(() -> {
            myPathMacroTable.commit();

            Collection<String> ignored = parseIgnoredVariables();
            PathMacros instance = PathMacros.getInstance();
            instance.setIgnoredMacroNames(ignored);
        });
    }

    public JComponent getPanel() {
        return myPanel;
    }

    @RequiredUIAccess
    public void reset() {
        myPathMacroTable.reset();
        fillIgnoredVariables();
    }

    public boolean isModified() {
        return myPathMacroTable.isModified() || isIgnoredModified();
    }

    private void createUIComponents() {
        myPanel = new JPanel();
        myPanel.setLayout(new GridLayoutManager(2, 1, JBUI.emptyInsets(), -1, -1));
        myPathVariablesPanel = new JPanel();
        myPathVariablesPanel.setLayout(new BorderLayout(0, 0));
        myPanel.add(
            myPathVariablesPanel,
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 2, JBUI.emptyInsets(), -1, 0));
        myPanel.add(
            panel1,
            new GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        panel1.add(
            TargetAWT.to(Label.create(LocalizeValue.localizeTODO("Ignored Variables:"))),
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
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
        myIgnoredVariables = TextBox.create();
        panel1.add(
            TargetAWT.to(myIgnoredVariables),
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
        Label label = Label.create(LocalizeValue.localizeTODO("use ; to separate ignored variables"));
        label.setForegroundColor(StandardColors.GRAY);
//        label.setComponentStyle(UIUtil.ComponentStyle.SMALL);

        panel1.add(
            TargetAWT.to(label),
            new GridConstraints(
                1,
                1,
                1,
                1,
                GridConstraints.ANCHOR_NORTHWEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                1,
                false
            )
        );
    }
}
