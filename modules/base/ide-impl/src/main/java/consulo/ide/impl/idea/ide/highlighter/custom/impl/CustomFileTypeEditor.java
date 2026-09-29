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
package consulo.ide.impl.idea.ide.highlighter.custom.impl;

import consulo.configurable.ConfigurationException;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.ide.impl.idea.openapi.fileTypes.impl.AbstractFileType;
import consulo.ide.localize.IdeLocalize;
import consulo.language.internal.custom.SyntaxTable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.InputBoxBuilder;
import consulo.ui.InputProblem;
import consulo.ui.Label;
import consulo.ui.ListBox;
import consulo.ui.StaticPosition;
import consulo.ui.Tab;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.TableLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class CustomFileTypeEditor extends SettingsEditor<AbstractFileType> {
    private final TextBox myFileTypeName = TextBox.create();
    private final TextBox myFileTypeDescr = TextBox.create();
    private final CheckBox myIgnoreCase = CheckBox.create(IdeLocalize.checkboxCustomfiletypeIgnoreCase());
    private final CheckBox mySupportBraces = CheckBox.create(IdeLocalize.checkboxCustomfiletypeSupportPairedBraces());
    private final CheckBox mySupportBrackets = CheckBox.create(IdeLocalize.checkboxCustomfiletypeSupportPairedBrackets());
    private final CheckBox mySupportParens = CheckBox.create(IdeLocalize.checkboxCustomfiletypeSupportPairedParens());
    private final CheckBox mySupportEscapes = CheckBox.create(IdeLocalize.checkboxCustomfiletypeSupportStringEscapes());

    private final TextBox myLineComment = TextBox.create();
    private final CheckBox myCommentAtLineStart = CheckBox.create(IdeLocalize.checkboxCustomfiletypeCommentOnlyAtLineStart());
    private final TextBox myBlockCommentStart = TextBox.create();
    private final TextBox myBlockCommentEnd = TextBox.create();
    private final TextBox myHexPrefix = TextBox.create();

    private final TextBox myNumPostfixes = TextBox.create();
    private final List<MutableFlatDataModel<String>> myKeywordModels = List.of(
        FlatDataModel.of(List.of()),
        FlatDataModel.of(List.of()),
        FlatDataModel.of(List.of()),
        FlatDataModel.of(List.of())
    );

    @RequiredUIAccess
    public CustomFileTypeEditor() {
        myLineComment.addValueListener(event -> updateCommentAtLineStart());
        myCommentAtLineStart.setEnabled(false);
    }

    @RequiredUIAccess
    private void updateCommentAtLineStart() {
        boolean enabled = StringUtil.isNotEmpty(myLineComment.getValue());
        myCommentAtLineStart.setEnabled(enabled);
        if (!enabled) {
            myCommentAtLineStart.setValue(false);
        }
    }

    @RequiredUIAccess
    @Override
    public void resetEditorFrom(AbstractFileType fileType) {
        myFileTypeName.setValue(fileType.getId());
        myFileTypeDescr.setValue(fileType.getDescription().get());

        SyntaxTable table = fileType.getSyntaxTable();

        if (table != null) {
            myLineComment.setValue(StringUtil.notNullize(table.getLineComment()));
            myBlockCommentEnd.setValue(StringUtil.notNullize(table.getEndComment()));
            myBlockCommentStart.setValue(StringUtil.notNullize(table.getStartComment()));
            myHexPrefix.setValue(StringUtil.notNullize(table.getHexPrefix()));
            myNumPostfixes.setValue(StringUtil.notNullize(table.getNumPostfixChars()));
            myIgnoreCase.setValue(table.isIgnoreCase());
            myCommentAtLineStart.setValue(table.lineCommentOnlyAtStart);

            mySupportBraces.setValue(table.isHasBraces());
            mySupportBrackets.setValue(table.isHasBrackets());
            mySupportParens.setValue(table.isHasParens());
            mySupportEscapes.setValue(table.isHasStringEscapes());

            myKeywordModels.get(0).replaceAll(new ArrayList<>(table.getKeywords1()));
            myKeywordModels.get(1).replaceAll(new ArrayList<>(table.getKeywords2()));
            myKeywordModels.get(2).replaceAll(new ArrayList<>(table.getKeywords3()));
            myKeywordModels.get(3).replaceAll(new ArrayList<>(table.getKeywords4()));
        }
        updateCommentAtLineStart();
    }

    @RequiredUIAccess
    @Override
    public void applyEditorTo(AbstractFileType type) throws ConfigurationException {
        String name = StringUtil.notNullize(myFileTypeName.getValue());
        if (name.trim().isEmpty()) {
            throw new ConfigurationException(IdeLocalize.errorNameCannotBeEmpty(), CommonLocalize.titleError());
        }
        else if (StringUtil.notNullize(myFileTypeDescr.getValue()).trim().isEmpty()) {
            myFileTypeDescr.setValue(name);
        }
        type.setName(name);
        type.setDescription(myFileTypeDescr.getValue());
        type.setSyntaxTable(getSyntaxTable());
    }

    @RequiredUIAccess
    @Override
    protected Component createUIComponent() {
        FormBuilder info = FormBuilder.create();
        info.addLabeled(IdeLocalize.editboxCustomfiletypeName(), myFileTypeName);
        info.addLabeled(IdeLocalize.editboxCustomfiletypeDescription(), myFileTypeDescr);

        TableLayout commentsAndNumbersPanel = TableLayout.create(StaticPosition.TOP);
        commentsAndNumbersPanel.add(Label.create(IdeLocalize.editboxCustomfiletypeLineComment()), TableLayout.cell(0, 0));
        commentsAndNumbersPanel.add(myLineComment, TableLayout.cell(0, 1).fill());
        commentsAndNumbersPanel.add(myCommentAtLineStart, TableLayout.cell(0, 2));

        commentsAndNumbersPanel.add(Label.create(IdeLocalize.editboxCustomfiletypeBlockCommentStart()), TableLayout.cell(1, 0));
        commentsAndNumbersPanel.add(myBlockCommentStart, TableLayout.cell(1, 1).fill());
        commentsAndNumbersPanel.add(Label.create(IdeLocalize.editboxCustomfiletypeBlockCommentEnd()), TableLayout.cell(1, 2));
        commentsAndNumbersPanel.add(myBlockCommentEnd, TableLayout.cell(1, 3).fill());

        commentsAndNumbersPanel.add(Label.create(IdeLocalize.editboxCustomfiletypeHexPrefix()), TableLayout.cell(2, 0));
        commentsAndNumbersPanel.add(myHexPrefix, TableLayout.cell(2, 1).fill());
        commentsAndNumbersPanel.add(Label.create(IdeLocalize.editboxCustomfiletypeNumberPostfixes()), TableLayout.cell(2, 2));
        commentsAndNumbersPanel.add(myNumPostfixes, TableLayout.cell(2, 3).fill());

        HorizontalLayout pairsPanel = HorizontalLayout.create();
        pairsPanel.add(mySupportBraces);
        pairsPanel.add(mySupportBrackets);
        pairsPanel.add(mySupportParens);
        pairsPanel.add(mySupportEscapes);

        DockLayout highlighterTop = DockLayout.create();
        highlighterTop.top(commentsAndNumbersPanel);
        highlighterTop.bottom(pairsPanel);

        TabbedLayout keywordsTabs = TabbedLayout.create();
        for (int i = 0; i < myKeywordModels.size(); i++) {
            LocalizeValue title = LocalizeValue.of(String.valueOf(i + 1));

            Tab tab = keywordsTabs.createTab();
            tab.setRenderer((it, presentation) -> presentation.append(title));

            keywordsTabs.addTab(tab, createKeywordsPanel(myKeywordModels.get(i)));
        }

        DockLayout highlighterPanel = DockLayout.create();
        highlighterPanel.top(highlighterTop);
        highlighterPanel.center(LabeledLayout.create(IdeLocalize.listboxCustomfiletypeKeywords(), keywordsTabs));
        highlighterPanel.bottom(myIgnoreCase);

        DockLayout panel = DockLayout.create();
        panel.top(info.build());
        panel.center(LabeledLayout.create(IdeLocalize.groupCustomfiletypeSyntaxHighlighting(), highlighterPanel));
        return panel;
    }

    @RequiredUIAccess
    private Component createKeywordsPanel(MutableFlatDataModel<String> model) {
        ListBox<String> list = ListBox.create(model);
        list.addDoubleClickListener(event -> {
            String value = list.getValue();
            if (value != null) {
                editKeyword(list, model, value);
            }
        });

        return ToolbarDecoratorBuilderFactory.getInstance()
            .create(list)
            .addOrReplaceAction(new AddAction<>() {
                @Override
                @RequiredUIAccess
                protected void doAdd(AnActionEvent e) {
                    showKeywordBox(list, IdeLocalize.titleAddNewKeyword(), "", keyword -> {
                        if (model.indexOf(keyword) < 0) {
                            model.add(keyword);
                        }
                    });
                }
            })
            .addOrReplaceAction(new EditAction<>() {
                @Override
                @RequiredUIAccess
                protected void doEdit(String value, AnActionEvent e) {
                    editKeyword(list, model, value);
                }
            })
            .disableAction(UpMoveAction.class)
            .disableAction(DownMoveAction.class)
            .build();
    }

    @RequiredUIAccess
    private void editKeyword(ListBox<String> list, MutableFlatDataModel<String> model, String value) {
        showKeywordBox(list, IdeLocalize.titleEditKeyword(), value, keyword -> {
            int index = model.indexOf(value);
            if (index >= 0) {
                model.remove(value);
                model.add(keyword, index);
                list.setValue(keyword);
            }
        });
    }

    @RequiredUIAccess
    private static void showKeywordBox(
        Component parent,
        LocalizeValue title,
        String initialValue,
        @RequiredUIAccess Consumer<String> onKeyword
    ) {
        UIAccess uiAccess = UIAccess.current();
        InputBoxBuilder.text()
            .title(title)
            .text(IdeLocalize.editboxKeyword())
            .value(initialValue)
            .validator(value -> {
                String keyword = StringUtil.notNullize(value).trim();
                if (keyword.isEmpty()) {
                    return InputProblem.error(IdeLocalize.errorKeywordCannotBeEmpty());
                }
                if (keyword.indexOf(' ') >= 0) {
                    return InputProblem.error(IdeLocalize.errorKeywordMayNotContainSpaces());
                }
                return null;
            })
            .showAsync(parent)
            .whenComplete((value, error) -> {
                if (error != null || value == null) {
                    return;
                }
                uiAccess.give(() -> onKeyword.accept(value.trim()));
            });
    }

    public SyntaxTable getSyntaxTable() {
        SyntaxTable syntaxTable = new SyntaxTable();
        syntaxTable.setLineComment(myLineComment.getValue());
        syntaxTable.setStartComment(myBlockCommentStart.getValue());
        syntaxTable.setEndComment(myBlockCommentEnd.getValue());
        syntaxTable.setHexPrefix(myHexPrefix.getValue());
        syntaxTable.setNumPostfixChars(myNumPostfixes.getValue());
        syntaxTable.lineCommentOnlyAtStart = Boolean.TRUE.equals(myCommentAtLineStart.getValue());

        boolean ignoreCase = Boolean.TRUE.equals(myIgnoreCase.getValue());
        syntaxTable.setIgnoreCase(ignoreCase);

        syntaxTable.setHasBraces(Boolean.TRUE.equals(mySupportBraces.getValue()));
        syntaxTable.setHasBrackets(Boolean.TRUE.equals(mySupportBrackets.getValue()));
        syntaxTable.setHasParens(Boolean.TRUE.equals(mySupportParens.getValue()));
        syntaxTable.setHasStringEscapes(Boolean.TRUE.equals(mySupportEscapes.getValue()));

        for (String keyword : getKeywords(0, ignoreCase)) {
            syntaxTable.addKeyword1(keyword);
        }
        for (String keyword : getKeywords(1, ignoreCase)) {
            syntaxTable.addKeyword2(keyword);
        }
        for (String keyword : getKeywords(2, ignoreCase)) {
            syntaxTable.addKeyword3(keyword);
        }
        for (String keyword : getKeywords(3, ignoreCase)) {
            syntaxTable.addKeyword4(keyword);
        }
        return syntaxTable;
    }

    private List<String> getKeywords(int index, boolean ignoreCase) {
        MutableFlatDataModel<String> model = myKeywordModels.get(index);
        List<String> keywords = new ArrayList<>(model.getSize());
        for (int i = 0; i < model.getSize(); i++) {
            String keyword = model.get(i);
            keywords.add(ignoreCase ? keyword.toLowerCase(Locale.ROOT) : keyword);
        }
        return keywords;
    }
}
