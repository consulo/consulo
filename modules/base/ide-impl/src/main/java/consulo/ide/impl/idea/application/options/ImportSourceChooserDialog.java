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
package consulo.ide.impl.idea.application.options;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import consulo.application.ApplicationBundle;
import consulo.component.persist.scheme.SchemeImporter;
import consulo.component.util.pointer.Named;
import consulo.language.codeStyle.localize.CodeStyleLocalize;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ImportSourceChooserDialog<S extends Named> extends DialogWrapper {
    private JPanel myContentPane;
    private JBList<String> mySourceList;

    private String mySelectedSourceName;
    private final ListModel<String> myListModel;

    private final static String SHARED_IMPORT_SOURCE = ApplicationBundle.message("import.scheme.shared");

    public ImportSourceChooserDialog(JComponent parent, Class<S> schemeClass) {
        super(parent, true);
        createUIComponents();
        setTitle(CodeStyleLocalize.titleImportSchemeFrom());
        myListModel = new SourceListModel(SchemeImporter.getExtensions(schemeClass));
        initSourceList();
        init();
    }

    private void initSourceList() {
        mySourceList.setModel(myListModel);
        mySourceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mySourceList.addListSelectionListener(e -> {
            int index = mySourceList.getSelectedIndex();
            if (index >= 0) {
                setSelectedSourceName(myListModel.getElementAt(index));
            }
        });
        mySourceList.setSelectedIndex(0);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return myContentPane;
    }

    private void setSelectedSourceName(String name) {
        mySelectedSourceName = name;
    }

    @Nullable
    public String getSelectedSourceName() {
        return mySelectedSourceName;
    }

    public boolean isImportFromSharedSelected() {
        return SHARED_IMPORT_SOURCE.equals(mySelectedSourceName);
    }

    private void createUIComponents() {
        myContentPane = new JPanel();
        myContentPane.setLayout(new GridLayoutManager(1, 3, JBUI.insets(10), -1, -1));
        JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, JBUI.emptyInsets(), -1, -1));
        myContentPane.add(
            panel1,
            new GridConstraints(
                0,
                0,
                1,
                3,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                new Dimension(200, 200),
                null,
                0,
                false
            )
        );
        JBScrollPane jBScrollPane1 = new JBScrollPane();
        panel1.add(
            jBScrollPane1,
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
        mySourceList = new JBList<>();
        jBScrollPane1.setViewportView(mySourceList);
    }

    private class SourceListModel extends DefaultListModel<String> {
        private final List<String> mySourceNames = new ArrayList<>();

        public SourceListModel(Collection<SchemeImporter<S>> extensions) {
            for (SchemeImporter extension : extensions) {
                mySourceNames.add(extension.getName());
            }
        }

        @Override
        public int getSize() {
            return mySourceNames.size();
        }

        @Override
        public String getElementAt(int index) {
            return mySourceNames.get(index);
        }
    }
}
