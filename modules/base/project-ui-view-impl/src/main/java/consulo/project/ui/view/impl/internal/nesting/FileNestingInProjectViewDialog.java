// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.project.ui.view.impl.internal.nesting;

import consulo.project.Project;
import consulo.project.ui.view.localize.ProjectUIViewLocalize;
import consulo.ui.CheckBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.table.ListTableModel;
import consulo.ui.ex.awt.table.TableView;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

public final class FileNestingInProjectViewDialog extends DialogWrapper {
    private static final Comparator<ProjectViewFileNestingService.NestingRule> RULE_COMPARATOR =
        Comparator.comparing(o -> o.getParentFileSuffix() + " " + o.getChildFileSuffix());

    private final CheckBox myUseNestingRulesCheckBox;
    private final JPanel myRulesPanel;
    private final TableView<CombinedNestingRule> myTable;

    private final LocalizeAction myOkAction = new OkAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            myTable.stopEditing();
            super.actionPerformed(e);
        }
    };

    public FileNestingInProjectViewDialog(@Nonnull Project project) {
        super(project);
        setTitle(ProjectUIViewLocalize.fileNestingDialogTitle());

        myTable = createTable();
        myRulesPanel = createRulesPanel(myTable);

        myUseNestingRulesCheckBox = CheckBox.create(ProjectUIViewLocalize.fileNestingFeatureEnabledCheckbox());
        myUseNestingRulesCheckBox.addClickListener(event -> {
            UIUtil.setEnabled(myRulesPanel, myUseNestingRulesCheckBox.getValue(), true);
        });

        init();
    }

    @Override
    protected String getHelpId() {
        return "project.view.file.nesting.dialog";
    }

    @Override
    protected JComponent createCenterPanel() {
        final JPanel mainPanel = new JPanel(new BorderLayout(0, JBUIScale.scale(16)));
        mainPanel.setBorder(JBUI.Borders.emptyTop(8)); // Resulting indent will be 16 = 8 (default) + 8 (set here)
        mainPanel.add(TargetAWT.to(myUseNestingRulesCheckBox), BorderLayout.NORTH);
        mainPanel.add(myRulesPanel, BorderLayout.CENTER);
        return mainPanel;
    }

    private static JPanel createRulesPanel(@Nonnull TableView<CombinedNestingRule> table) {
        final ToolbarDecorator toolbarDecorator =
            ToolbarDecorator.createDecorator(table,
                    new ElementProducer<>() {
                        @Override
                        public boolean canCreateElement() {
                            return true;
                        }

                        @Override
                        public CombinedNestingRule createElement() {
                            return new CombinedNestingRule("", "");
                        }
                    })
                .disableUpDownActions();
        return new BorderLayoutPanel()
            .addToTop(new JLabel(ProjectUIViewLocalize.fileNestingTableTitle().get()))
            .addToCenter(toolbarDecorator.createPanel());
    }

    private static TableView<CombinedNestingRule> createTable() {
        String childColumn = ProjectUIViewLocalize.childFileSuffixColumnName().get();
        String parentColumn = ProjectUIViewLocalize.parentFileSuffixColumnName().get();
        final ListTableModel<CombinedNestingRule> model = new ListTableModel<>(
            new ColumnInfo<CombinedNestingRule, String>(parentColumn) {
                @Override
                public int getWidth(JTable table) {
                    return JBUIScale.scale(125);
                }

                @Override
                public boolean isCellEditable(CombinedNestingRule rule) {
                    return true;
                }

                @Override
                public String valueOf(CombinedNestingRule rule) {
                    return rule.parentSuffix;
                }

                @Override
                public void setValue(CombinedNestingRule rule, String value) {
                    rule.parentSuffix = value.trim();
                }
            },
            new ColumnInfo<CombinedNestingRule, String>(childColumn) {
                @Override
                public boolean isCellEditable(CombinedNestingRule rule) {
                    return true;
                }

                @Override
                public String valueOf(CombinedNestingRule rule) {
                    return rule.childSuffixes;
                }

                @Override
                public void setValue(CombinedNestingRule rule, String value) {
                    rule.childSuffixes = value;
                }
            }
        );

        final TableView<CombinedNestingRule> table = new TableView<>(model);
        table.setRowHeight(new JTextField().getPreferredSize().height + table.getRowMargin());
        return table;
    }

    @Nonnull
    @Override
    protected Action[] createActions() {
        DialogWrapperAction resetToDefaultAction = new DialogWrapperAction(ProjectUIViewLocalize.fileNestingResetToDefaultButton()) {
            @Override
            protected void doAction(ActionEvent e) {
                resetTable(ProjectViewFileNestingService.loadDefaultNestingRules());
            }
        };

        Action[] actions = super.createActions();
        return ArrayUtil.prepend(resetToDefaultAction, actions);
    }

    @Override
    protected @Nonnull LocalizeAction getOKAction() {
        return myOkAction;
    }

    @RequiredUIAccess
    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (!myUseNestingRulesCheckBox.getValue()) {
            return null;
        }

        List<CombinedNestingRule> items = myTable.getListTableModel().getItems();
        for (int i = 0; i < items.size(); i++) {
            final CombinedNestingRule rule = items.get(i);
            final int row = i + 1;
            if (rule.parentSuffix.isEmpty()) {
                return new ValidationInfo(ProjectUIViewLocalize.dialogMessageParentFileSuffixMustBeEmptySeeRow(row), null);
            }
            if (rule.childSuffixes.isEmpty()) {
                return new ValidationInfo(ProjectUIViewLocalize.dialogMessageChildFileSuffixMustBeEmptySeeRow(row), null);
            }

            for (String childSuffix : StringUtil.split(rule.childSuffixes, ";")) {
                if (rule.parentSuffix.equals(childSuffix.trim())) {
                    return new ValidationInfo(
                        ProjectUIViewLocalize.dialogMessageParentChildFileSuffixesMustBeEqualSeeRow(rule.parentSuffix, row), null);
                }
            }
        }

        return null;
    }

    public void reset(boolean useFileNestingRules) {
        myUseNestingRulesCheckBox.setValue(useFileNestingRules);
        UIUtil.setEnabled(myRulesPanel, myUseNestingRulesCheckBox.getValue(), true);

        resetTable(ProjectViewFileNestingService.getInstance().getRules());
    }

    private void resetTable(final @Nonnull List<? extends ProjectViewFileNestingService.NestingRule> rules) {
        final SortedMap<String, CombinedNestingRule> result = new TreeMap<>();
        for (ProjectViewFileNestingService.NestingRule rule : ContainerUtil.sorted(rules, RULE_COMPARATOR)) {
            final CombinedNestingRule r = result.get(rule.getParentFileSuffix());
            if (r == null) {
                result.put(rule.getParentFileSuffix(), new CombinedNestingRule(rule.getParentFileSuffix(), rule.getChildFileSuffix()));
            }
            else {
                //noinspection StringConcatenationInLoop
                r.childSuffixes += "; " + rule.getChildFileSuffix();
            }
        }
        myTable.getListTableModel().setItems(new ArrayList<>(result.values()));
    }

    public void apply(final @Nonnull Consumer<? super Boolean> useNestingRulesOptionConsumer) {
        useNestingRulesOptionConsumer.accept(myUseNestingRulesCheckBox.getValue());

        if (myUseNestingRulesCheckBox.getValue()) {
            final SortedSet<ProjectViewFileNestingService.NestingRule> result = new TreeSet<>(RULE_COMPARATOR);
            for (CombinedNestingRule rule : myTable.getListTableModel().getItems()) {
                for (String childSuffix : StringUtil.split(rule.childSuffixes, ";")) {
                    if (!StringUtil.isEmptyOrSpaces(childSuffix)) {
                        result.add(new ProjectViewFileNestingService.NestingRule(rule.parentSuffix, childSuffix.trim()));
                    }
                }
            }
            ProjectViewFileNestingService.getInstance().setRules(new ArrayList<>(result));
        }
    }

    private static final class CombinedNestingRule {
        @Nonnull
        String parentSuffix;
        @Nonnull
        String childSuffixes; // semicolon-separated, space symbols around each suffix are ignored

        private CombinedNestingRule(@Nonnull String parentSuffix, @Nonnull String childSuffixes) {
            this.parentSuffix = parentSuffix;
            this.childSuffixes = childSuffixes;
        }
    }
}
