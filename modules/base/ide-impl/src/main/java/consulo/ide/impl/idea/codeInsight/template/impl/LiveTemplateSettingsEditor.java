/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.template.impl;

import consulo.application.Application;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.impl.internal.template.TemplateEditorUtil;
import consulo.language.editor.impl.internal.template.TemplateImpl;
import consulo.language.editor.impl.internal.template.TemplateImplUtil;
import consulo.language.editor.impl.internal.template.TemplateSettingsImpl;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.template.Template;
import consulo.language.editor.template.TemplateOptionalProcessor;
import consulo.language.editor.template.Variable;
import consulo.language.editor.template.context.EverywhereContextType;
import consulo.language.editor.template.context.TemplateContext;
import consulo.language.editor.template.context.TemplateContextType;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.ComboBox;
import consulo.ui.Label;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.tree.CheckboxTree;
import consulo.ui.ex.awt.tree.CheckedTreeNode;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupAdapter;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.ex.update.Activatable;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

public class LiveTemplateSettingsEditor extends JPanel {
    private static enum ExpandByKey {
        DEFAULT(TemplateSettingsImpl.DEFAULT_CHAR, LocalizeValue.empty()) {
            @Override
            public LocalizeValue getTitle(String defaultShortcut) {
                return CodeInsightLocalize.dialogEditTemplateShortcutDefault(defaultShortcut);
            }
        },
        SPACE(TemplateSettingsImpl.TAB_CHAR, CodeInsightLocalize.templateShortcutSpace()),
        TAB(TemplateSettingsImpl.ENTER_CHAR, CodeInsightLocalize.templateShortcutTab()),
        ENTER(TemplateSettingsImpl.SPACE_CHAR, CodeInsightLocalize.templateShortcutEnter());

        private final char myShortcutChar;
        private final LocalizeValue myTitle;

        ExpandByKey(char shortcutChar, LocalizeValue title) {
            myShortcutChar = shortcutChar;
            myTitle = title;
        }

        public char getShortcutChar() {
            return myShortcutChar;
        }

        public LocalizeValue getTitle(String defaultShortcut) {
            return myTitle;
        }

        @Nonnull
        public static ExpandByKey valueOfShortcutChar(char shortcutChar) {
            for (ExpandByKey value : values()) {
                if (value.getShortcutChar() == shortcutChar) {
                    return value;
                }
            }
            return SPACE;
        }
    }

    private final TemplateImpl myTemplate;

    private final JTextField myKeyField;
    private final JTextField myDescription;
    private final Editor myTemplateEditor;

    private ComboBox<ExpandByKey> myExpandByCombo;
    private final String myDefaultShortcut;
    private CheckBox myCbReformat;

    private Button myEditVariablesButton;

    private final Map<TemplateOptionalProcessor, Boolean> myOptions;
    private final Map<TemplateContextType, Boolean> myContext;
    private JBPopup myContextPopup;
    private Dimension myLastSize;

    @RequiredUIAccess
    public LiveTemplateSettingsEditor(
        TemplateImpl template,
        final String defaultShortcut,
        Map<TemplateOptionalProcessor, Boolean> options,
        Map<TemplateContextType, Boolean> context,
        final Runnable nodeChanged,
        boolean allowNoContext
    ) {
        super(new BorderLayout());
        myOptions = options;
        myContext = context;

        myTemplate = template;
        myDefaultShortcut = defaultShortcut;

        myKeyField = new JTextField();
        myDescription = new JTextField();
        myTemplateEditor = TemplateEditorUtil.createEditor(false, myTemplate.getString(), context);
        myTemplate.setId(null);

        createComponents(allowNoContext);

        myKeyField.getDocument().addDocumentListener(new consulo.ui.ex.awt.event.DocumentAdapter() {
            @Override
            protected void textChanged(javax.swing.event.DocumentEvent e) {
                myTemplate.setKey(myKeyField.getText().trim());
                nodeChanged.run();
            }
        });
        myDescription.getDocument().addDocumentListener(new consulo.ui.ex.awt.event.DocumentAdapter() {
            @Override
            protected void textChanged(javax.swing.event.DocumentEvent e) {
                myTemplate.setDescription(myDescription.getText().trim());
                nodeChanged.run();
            }
        });

        new UiNotifyConnector(
            this,
            new Activatable.Adapter() {
                @Override
                public void hideNotify() {
                    disposeContextPopup();
                }
            }
        );
    }

    public TemplateImpl getTemplate() {
        return myTemplate;
    }

    public void dispose() {
        final Project project = myTemplateEditor.getProject();
        if (project != null) {
            final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(myTemplateEditor.getDocument());
            if (psiFile != null) {
                DaemonCodeAnalyzer.getInstance(project).setHighlightingEnabled(psiFile, true);
            }
        }
        EditorFactory.getInstance().releaseEditor(myTemplateEditor);
    }

    @RequiredUIAccess
    private void createComponents(boolean allowNoContexts) {
        JPanel panel = new JPanel(new GridBagLayout());

        GridBag gb = new GridBag().setDefaultInsets(4, 4, 4, 4).setDefaultWeightY(1).setDefaultFill(GridBagConstraints.BOTH);

        JPanel editorPanel = new JPanel(new BorderLayout(4, 4));
        editorPanel.setPreferredSize(new Dimension(250, 100));
        editorPanel.setMinimumSize(editorPanel.getPreferredSize());
        editorPanel.add(myTemplateEditor.getComponent(), BorderLayout.CENTER);
        Label templateTextLabel = Label.create(CodeInsightLocalize.dialogEditTemplateTemplateTextTitle());
        templateTextLabel.setTarget(TargetAWT.wrap(myTemplateEditor.getContentComponent()));
        editorPanel.add(TargetAWT.to(templateTextLabel), BorderLayout.NORTH);
        panel.add(editorPanel, gb.nextLine().next().weighty(1).weightx(1).coverColumn(2));

        myEditVariablesButton = Button.create(CodeInsightLocalize.dialogEditTemplateButtonEditVariables());
        panel.add(TargetAWT.to(myEditVariablesButton), gb.next().weighty(0));

        panel.add(createTemplateOptionsPanel(), gb.nextLine().next().next().coverColumn(2).weighty(1));

        panel.add(createShortContextPanel(allowNoContexts), gb.nextLine().next().weighty(0).fillCellNone().anchor(GridBagConstraints.WEST));

        myTemplateEditor.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            @RequiredUIAccess
            public void documentChanged(DocumentEvent e) {
                validateEditVariablesButton();

                myTemplate.setString(myTemplateEditor.getDocument().getText());
                applyVariables(updateVariablesByTemplateText());
            }
        });

        myEditVariablesButton.addClickListener(e -> editVariables());

        add(createNorthPanel(), BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
    }

    private void applyVariables(final List<Variable> variables) {
        myTemplate.removeAllParsed();
        for (Variable variable : variables) {
            myTemplate.addVariable(
                variable.getName(),
                variable.getExpressionString(),
                variable.getDefaultValueString(),
                variable.isAlwaysStopAt()
            );
        }
        myTemplate.parseSegments();
    }

    @Nonnull
    private JComponent createNorthPanel() {
        JPanel panel = new JPanel(new GridBagLayout());

        GridBag gb = new GridBag().setDefaultInsets(4, 4, 4, 4).setDefaultWeightY(1).setDefaultFill(GridBagConstraints.BOTH);

        Label keyPrompt = Label.create(CodeInsightLocalize.dialogEditTemplateLabelAbbreviation());
        keyPrompt.setTarget(TargetAWT.wrap(myKeyField));
        panel.add(TargetAWT.to(keyPrompt), gb.nextLine().next());

        panel.add(myKeyField, gb.next().weightx(1));

        Label descriptionPrompt = Label.create(CodeInsightLocalize.dialogEditTemplateLabelDescription());
        descriptionPrompt.setTarget(TargetAWT.wrap(myDescription));
        panel.add(TargetAWT.to(descriptionPrompt), gb.next());

        panel.add(myDescription, gb.next().weightx(3));
        return panel;
    }

    @RequiredUIAccess
    private JPanel createTemplateOptionsPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(IdeBorderFactory.createTitledBorder(CodeInsightLocalize.dialogEditTemplateOptionsTitle().get(), true));
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbConstraints = new GridBagConstraints();
        gbConstraints.fill = GridBagConstraints.BOTH;

        gbConstraints.weighty = 0;
        gbConstraints.weightx = 0;
        gbConstraints.gridy = 0;
        Label expandWithLabel = Label.create(CodeInsightLocalize.dialogEditTemplateLabelExpandWith());
        panel.add(TargetAWT.to(expandWithLabel), gbConstraints);

        gbConstraints.gridx = 1;
        gbConstraints.insets = JBUI.insetsLeft(4);
        myExpandByCombo = ComboBox.create(ExpandByKey.values());
        myExpandByCombo.setTextRender(key -> key.getTitle(myDefaultShortcut));
        //noinspection ConstantConditions
        myExpandByCombo.addValueListener(e -> myTemplate.setShortcutChar(myExpandByCombo.getValue().getShortcutChar()));
        expandWithLabel.setTarget(myExpandByCombo);

        panel.add(TargetAWT.to(myExpandByCombo), gbConstraints);
        gbConstraints.weightx = 1;
        gbConstraints.gridx = 2;
        panel.add(new JPanel(), gbConstraints);

        gbConstraints.gridx = 0;
        gbConstraints.gridy++;
        gbConstraints.gridwidth = 3;
        myCbReformat = CheckBox.create(CodeInsightLocalize.dialogEditTemplateCheckboxReformatAccordingToStyle());
        panel.add(TargetAWT.to(myCbReformat), gbConstraints);

        for (final TemplateOptionalProcessor processor : myOptions.keySet()) {
            if (!processor.isVisible(myTemplate)) {
                continue;
            }
            gbConstraints.gridy++;
            final JCheckBox cb = new JCheckBox(processor.getOptionName());
            panel.add(cb, gbConstraints);
            cb.setSelected(myOptions.get(processor));
            cb.addActionListener(e -> myOptions.put(processor, cb.isSelected()));
        }

        gbConstraints.weighty = 1;
        gbConstraints.gridy++;
        panel.add(new JPanel(), gbConstraints);

        return panel;
    }

    private List<TemplateContextType> getApplicableContexts() {
        ArrayList<TemplateContextType> result = new ArrayList<>();
        for (TemplateContextType type : myContext.keySet()) {
            if (myContext.get(type)) {
                result.add(type);
            }
        }
        return result;
    }

    private JPanel createShortContextPanel(final boolean allowNoContexts) {
        JPanel panel = new JPanel(new BorderLayout());

        final JLabel ctxLabel = new JLabel();
        final JLabel change = new JLabel();
        change.setForeground(JBColor.BLUE);
        change.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panel.add(ctxLabel, BorderLayout.CENTER);
        panel.add(change, BorderLayout.EAST);

        final Runnable updateLabel = () -> {
            StringBuilder sb = new StringBuilder();
            String oldPrefix = "";
            for (TemplateContextType type : getApplicableContexts()) {
                final TemplateContextType base = type.getBaseContextType();
                String ownName = type.getPresentableName().map(Presentation.NO_MNEMONIC).get();
                String prefix = "";
                if (base != null && !(base instanceof EverywhereContextType)) {
                    prefix = base.getPresentableName().map(Presentation.NO_MNEMONIC).get() + ": ";
                    ownName = StringUtil.decapitalize(ownName);
                }
                if (type instanceof EverywhereContextType) {
                    ownName = CodeInsightLocalize.dialogEditTemplateContextOther().get();
                }
                if (sb.length() > 0) {
                    sb.append(oldPrefix.equals(prefix) ? ", " : "; ");
                }
                if (!oldPrefix.equals(prefix)) {
                    sb.append(prefix);
                    oldPrefix = prefix;
                }
                sb.append(ownName);
            }
            final boolean hasContexts = !sb.isEmpty();
            ctxLabel.setText(
                hasContexts
                    ? CodeInsightLocalize.dialogEditTemplateApplicableInContexts(sb).get()
                    : allowNoContexts
                    ? CodeInsightLocalize.dialogEditTemplateNoApplicableContexts().get()
                    : CodeInsightLocalize.dialogEditTemplateNoApplicableContextsYet().get()
            );
            ctxLabel.setForeground(hasContexts ? allowNoContexts ? JBColor.GRAY : JBColor.RED : UIUtil.getLabelForeground());
            change.setText(hasContexts ? CodeInsightLocalize.linkDefineContext().get() : CodeInsightLocalize.linkChangeContext().get());
        };

        new ClickListener() {
            @Override
            public boolean onClick(@Nonnull MouseEvent e, int clickCount) {
                if (disposeContextPopup()) {
                    return false;
                }

                final JPanel content = createPopupContextPanel(updateLabel);
                Dimension prefSize = content.getPreferredSize();
                if (myLastSize != null && (myLastSize.width > prefSize.width || myLastSize.height > prefSize.height)) {
                    content.setPreferredSize(new Dimension(
                        Math.max(prefSize.width, myLastSize.width),
                        Math.max(prefSize.height, myLastSize.height)
                    ));
                }
                myContextPopup = JBPopupFactory.getInstance().createComponentPopupBuilder(content, null).setResizable(true).createPopup();
                myContextPopup.show(new RelativePoint(change, new Point(change.getWidth(), -content.getPreferredSize().height - 10)));
                myContextPopup.addListener(new JBPopupAdapter() {
                    @Override
                    public void onClosed(LightweightWindowEvent event) {
                        myLastSize = content.getSize();
                    }
                });
                return true;
            }
        }.installOn(change);

        updateLabel.run();

        return panel;
    }

    private boolean disposeContextPopup() {
        if (myContextPopup != null && myContextPopup.isVisible()) {
            myContextPopup.cancel();
            myContextPopup = null;
            return true;
        }
        return false;
    }

    private JPanel createPopupContextPanel(final Runnable onChange) {
        JPanel panel = new JPanel(new BorderLayout());

        MultiMap<TemplateContextType, TemplateContextType> hierarchy = MultiMap.createLinked();
        for (TemplateContextType type : myContext.keySet()) {
            hierarchy.putValue(type.getBaseContextType(), type);
        }

        final CheckedTreeNode root = new CheckedTreeNode(Pair.create(null, "Hi"));
        final CheckboxTree checkboxTree = new CheckboxTree(
            new CheckboxTree.CheckboxTreeCellRenderer() {
                @Override
                public void customizeRenderer(
                    JTree tree,
                    Object value,
                    boolean selected,
                    boolean expanded,
                    boolean leaf,
                    int row,
                    boolean hasFocus
                ) {
                    final Object o = ((DefaultMutableTreeNode)value).getUserObject();
                    if (o instanceof Pair pair) {
                        getTextRenderer().append((String)pair.second);
                    }
                }
            },
            root
        ) {
            @Override
            @RequiredUIAccess
            protected void onNodeStateChanged(CheckedTreeNode node) {
                final TemplateContextType type = (TemplateContextType)((Pair)node.getUserObject()).first;
                if (type != null) {
                    myContext.put(type, node.isChecked());
                }
                myExpandByCombo.setEnabled(isExpandableFromEditor());
                updateHighlighter();
                onChange.run();
            }
        };

        for (TemplateContextType type : hierarchy.get(null)) {
            addContextNode(hierarchy, root, type);
        }

        ((DefaultTreeModel)checkboxTree.getModel()).nodeStructureChanged(root);

        TreeUtil.traverse(
            root,
            _node -> {
                final CheckedTreeNode node = (CheckedTreeNode)_node;
                if (node.isChecked()) {
                    checkboxTree.expandPath(new TreePath(node.getPath()).getParentPath());
                }
                return true;
            }
        );

        panel.add(ScrollPaneFactory.createScrollPane(checkboxTree));
        final Dimension size = checkboxTree.getPreferredSize();
        panel.setPreferredSize(new Dimension(size.width + 30, Math.min(size.height + 10, 500)));

        return panel;
    }

    private void addContextNode(
        MultiMap<TemplateContextType, TemplateContextType> hierarchy,
        CheckedTreeNode parent,
        TemplateContextType type
    ) {
        final Collection<TemplateContextType> children = hierarchy.get(type);
        final String name = type.getPresentableName().map(Presentation.NO_MNEMONIC).get();
        final CheckedTreeNode node = new CheckedTreeNode(Pair.create(children.isEmpty() ? type : null, name));
        parent.add(node);

        if (children.isEmpty()) {
            node.setChecked(myContext.get(type));
        }
        else {
            for (TemplateContextType child : children) {
                addContextNode(hierarchy, node, child);
            }
            final CheckedTreeNode other = new CheckedTreeNode(Pair.create(type, "Other"));
            other.setChecked(myContext.get(type));
            node.add(other);
        }
    }

    private boolean isExpandableFromEditor() {
        boolean hasNonExpandable = false;
        for (TemplateContextType type : getApplicableContexts()) {
            if (type.isExpandableFromEditor()) {
                return true;
            }
            hasNonExpandable = true;
        }

        return !hasNonExpandable;
    }

    private void updateHighlighter() {
        List<TemplateContextType> applicableContexts = getApplicableContexts();
        if (!applicableContexts.isEmpty()) {
            TemplateContext contextByType = new TemplateContext();
            contextByType.setEnabled(applicableContexts.get(0), true);
            TemplateEditorUtil.setHighlighter(myTemplateEditor, contextByType);
            return;
        }
        ((EditorEx)myTemplateEditor).repaint(0, myTemplateEditor.getDocument().getTextLength());
    }

    @RequiredUIAccess
    private void validateEditVariablesButton() {
        myEditVariablesButton.setEnabled(!parseVariables(myTemplateEditor.getDocument().getCharsSequence()).isEmpty());
    }

    @RequiredUIAccess
    void resetUi() {
        myKeyField.setText(myTemplate.getKey());
        myDescription.setText(myTemplate.getDescription());

        myExpandByCombo.setValue(ExpandByKey.valueOfShortcutChar(myTemplate.getShortcutChar()));

        CommandProcessor.getInstance().newCommand(() -> {
                final Document document = myTemplateEditor.getDocument();
                document.replaceString(0, document.getTextLength(), myTemplate.getString());
            })
            .executeInWriteAction();

        myCbReformat.setValue(myTemplate.isToReformat());
        myCbReformat.addValueListener(e -> myTemplate.setToReformat(myCbReformat.getValue()));

        myExpandByCombo.setEnabled(isExpandableFromEditor());

        updateHighlighter();
        validateEditVariablesButton();
    }

    @RequiredUIAccess
    private void editVariables() {
        ArrayList<Variable> newVariables = updateVariablesByTemplateText();

        EditVariableDialog editVariableDialog =
            new EditVariableDialog(myTemplateEditor, TargetAWT.to(myEditVariablesButton), newVariables, getApplicableContexts());
        editVariableDialog.show();
        if (editVariableDialog.isOK()) {
            applyVariables(newVariables);
        }
    }

    private ArrayList<Variable> updateVariablesByTemplateText() {
        List<Variable> oldVariables = getCurrentVariables();

        Set<String> oldVariableNames = ContainerUtil.map2Set(oldVariables, Variable::getName);

        ArrayList<Variable> parsedVariables = parseVariables(myTemplateEditor.getDocument().getCharsSequence());

        Map<String, String> newVariableNames = new HashMap<>();
        for (Object parsedVariable : parsedVariables) {
            Variable newVariable = (Variable)parsedVariable;
            String name = newVariable.getName();
            newVariableNames.put(name, name);
        }

        int oldVariableNumber = 0;
        for (int i = 0; i < parsedVariables.size(); i++) {
            Variable variable = parsedVariables.get(i);
            if (oldVariableNames.contains(variable.getName())) {
                Variable oldVariable = null;
                for (; oldVariableNumber < oldVariables.size(); oldVariableNumber++) {
                    oldVariable = oldVariables.get(oldVariableNumber);
                    if (newVariableNames.get(oldVariable.getName()) != null) {
                        break;
                    }
                    oldVariable = null;
                }
                oldVariableNumber++;
                if (oldVariable != null) {
                    parsedVariables.set(i, oldVariable);
                }
            }
        }

        return parsedVariables;
    }

    private List<Variable> getCurrentVariables() {
        List<Variable> myVariables = new ArrayList<>();

        for (int i = 0; i < myTemplate.getVariableCount(); i++) {
            myVariables.add(new Variable(
                myTemplate.getVariableNameAt(i),
                myTemplate.getExpressionStringAt(i),
                myTemplate.getDefaultValueStringAt(i),
                myTemplate.isAlwaysStopAt(i)
            ));
        }
        return myVariables;
    }

    public JTextField getKeyField() {
        return myKeyField;
    }

    public void focusKey() {
        myKeyField.selectAll();
        //TODO[peter,kirillk] without these invokeLaters this requestFocus conflicts with
        // consulo.ide.impl.idea.openapi.ui.impl.DialogWrapperPeerImpl.MyDialog.MyWindowListener.windowOpened()
        IdeFocusManager.findInstanceByComponent(myKeyField).requestFocus(myKeyField, true);
        final ModalityState modalityState = Application.get().getModalityStateForComponent(myKeyField);
        Application.get().invokeLater(
            () -> Application.get().invokeLater(
                () -> Application.get().invokeLater(
                    () -> IdeFocusManager.findInstanceByComponent(myKeyField).requestFocus(myKeyField, true),
                    modalityState
                ),
                modalityState
            ),
            modalityState
        );
    }

    private static ArrayList<Variable> parseVariables(CharSequence text) {
        ArrayList<Variable> variables = new ArrayList<>();
        TemplateImplUtil.parseVariables(text, variables, Template.INTERNAL_VARS_SET);
        return variables;
    }
}
