/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInspection.actions;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import consulo.content.scope.SearchScope;
import consulo.find.localize.FindLocalize;
import consulo.ide.impl.idea.find.impl.FindInProjectUtil;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.ui.CheckBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.TitledSeparator;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.function.Predicate;

/**
 * @author Dmitry Avdeev
 * @since 2011-10-11
 */
class FileFilterPanel {
    private CheckBox myUseFileMask;
    private JComboBox myFileMask;
    private JPanel myPanel;

    void init() {
        FindInProjectUtil.initFileFilter(myFileMask, myUseFileMask);
    }

    @Nullable SearchScope getSearchScope() {
        if (!myUseFileMask.getValue()) {
            return null;
        }
        String text = (String)myFileMask.getSelectedItem();
        if (text == null) {
            return null;
        }

        Predicate<CharSequence> patternCondition = FindInProjectUtil.createFileMaskCondition(text);
        return new GlobalSearchScope() {
            @Override
            public boolean contains(VirtualFile file) {
                return patternCondition.test(file.getNameSequence());
            }

            @Override
            public int compare(VirtualFile file1, VirtualFile file2) {
                return 0;
            }

            @Override
            public boolean isSearchInModuleContent(Module aModule) {
                return true;
            }

            @Override
            public boolean isSearchInLibraries() {
                return true;
            }
        };
    }

    JPanel getPanel() {
        return myPanel;
    }

    @RequiredUIAccess
    FileFilterPanel() {
        myPanel = new JPanel();
        myPanel.setLayout(new GridLayoutManager(2, 2, JBUI.emptyInsets(), -1, -1));
        myUseFileMask = CheckBox.create(FindLocalize.findFilterFileMaskCheckbox());
        myPanel.add(
            TargetAWT.to(myUseFileMask),
            new GridConstraints(
                1,
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
                1,
                false
            )
        );
        myFileMask = new JComboBox();
        myPanel.add(
            myFileMask,
            new GridConstraints(1,
                1,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        TitledSeparator titledSeparator1 = new TitledSeparator(FindLocalize.findFilterFileNameGroup().get());
        myPanel.add(
            titledSeparator1,
            new GridConstraints(0,
                0,
                1,
                2,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
    }

    public JComponent $$$getRootComponent$$$() {
        return myPanel;
    }
}
