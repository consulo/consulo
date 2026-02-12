package consulo.ide.impl.idea.application.options;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import consulo.application.localize.ApplicationLocalize;
import consulo.language.codeStyle.localize.CodeStyleLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Label;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImportSchemeChooserDialog extends DialogWrapper {
    private JPanel contentPane;
    private JBList<String> mySchemeList;
    private TextBox myTargetNameField;
    private CheckBox myUseCurrentScheme;
    private String mySelectedName;
    private final static LocalizeValue UNNAMED_SCHEME_ITEM = LocalizeValue.join(
        LocalizeValue.of("<"),
        CodeStyleLocalize.codeStyleSchemeImportUnnamed(),
        LocalizeValue.of(">")
    );
    private final List<String> myNames = new ArrayList<>();

    @RequiredUIAccess
    public ImportSchemeChooserDialog(
        @Nonnull Component parent,
        String[] schemeNames,
        @Nullable String currScheme
    ) {
        super(parent, false);
        createUIComponents();
        if (schemeNames.length > 0) {
            myNames.addAll(Arrays.asList(schemeNames));
        }
        else {
            myNames.add(UNNAMED_SCHEME_ITEM.get());
        }
        mySchemeList.setModel(new DefaultListModel<>() {
            @Override
            public int getSize() {
                return myNames.size();
            }

            @Override
            public String getElementAt(int index) {
                return myNames.get(index);
            }
        });
        mySchemeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mySchemeList.addListSelectionListener(e -> {
            int index = mySchemeList.getSelectedIndex();
            if (index >= 0) {
                mySelectedName = myNames.get(index);
                if (!myUseCurrentScheme.getValue() && !UNNAMED_SCHEME_ITEM.get().equals(mySelectedName)) {
                    myTargetNameField.setValue(mySelectedName);
                }
            }
        });
        myUseCurrentScheme.setEnabled(currScheme != null);
        myUseCurrentScheme.addClickListener(e -> {
            if (myUseCurrentScheme.getValue()) {
                myTargetNameField.setEnabled(false);
                if (currScheme != null) {
                    myTargetNameField.setValue(currScheme);
                }
            }
            else {
                myTargetNameField.setEnabled(true);
                if (mySelectedName != null) {
                    myTargetNameField.setValue(mySelectedName);
                }
            }
        });
        mySchemeList.getSelectionModel().setSelectionInterval(0, 0);
        init();
        setTitle(CodeStyleLocalize.titleImportSchemeChooser());
    }

    public String getSelectedName() {
        return UNNAMED_SCHEME_ITEM.get().equals(mySelectedName) ? null : mySelectedName;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    public boolean isUseCurrentScheme() {
        return myUseCurrentScheme.getValue();
    }

    @Nullable
    public String getTargetName() {
        String name = myTargetNameField.getValue();
        return name != null && !name.trim().isEmpty() ? name : null;
    }

    @RequiredUIAccess
    private void createUIComponents() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(4, 1, JBUI.insets(10), -1, -1));
        contentPane.putClientProperty("BorderFactoryClass", "");
        JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 2, JBUI.emptyInsets(), -1, -1));
        panel1.putClientProperty("BorderFactoryClass", "");
        contentPane.add(
            panel1,
            new GridConstraints(
                2,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                new Dimension(200, -1),
                null,
                null,
                0,
                false
            )
        );
        panel1.add(
            TargetAWT.to(Label.create(ApplicationLocalize.importSchemeChooserDestination())),
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
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
        myTargetNameField = TextBox.create();
        panel1.add(
            TargetAWT.to(myTargetNameField),
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
        myUseCurrentScheme = CheckBox.create(LocalizeValue.localizeTODO("Current scheme"));
        panel1.add(
            TargetAWT.to(myUseCurrentScheme),
            new GridConstraints(
                1,
                1,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        JBScrollPane jBScrollPane1 = new JBScrollPane();
        contentPane.add(
            jBScrollPane1,
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
        mySchemeList = new JBList<>();
        jBScrollPane1.setViewportView(mySchemeList);
        contentPane.add(
            new Spacer(),
            new GridConstraints(
                3,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_VERTICAL,
                1,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        contentPane.add(
            TargetAWT.to(Label.create(ApplicationLocalize.importSchemeChooserSource())),
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
    }
}
