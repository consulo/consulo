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
import consulo.language.internal.custom.SyntaxTable;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ide.localize.IdeLocalize;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.util.ListUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * @author Yura Cangea, dsl
 */
public class CustomFileTypeEditor extends SettingsEditor<AbstractFileType> {
  private final JTextField myFileTypeName = new JTextField();
  private final JTextField myFileTypeDescr = new JTextField();
  private final JCheckBox myIgnoreCase = new JCheckBox(IdeLocalize.checkboxCustomfiletypeIgnoreCase().get());
  private final JCheckBox mySupportBraces = new JCheckBox(IdeLocalize.checkboxCustomfiletypeSupportPairedBraces().get());
  private final JCheckBox mySupportBrackets = new JCheckBox(IdeLocalize.checkboxCustomfiletypeSupportPairedBrackets().get());
  private final JCheckBox mySupportParens = new JCheckBox(IdeLocalize.checkboxCustomfiletypeSupportPairedParens().get());
  private final JCheckBox mySupportEscapes = new JCheckBox(IdeLocalize.checkboxCustomfiletypeSupportStringEscapes().get());

  private final JTextField myLineComment = new JTextField(5);
  private final JCheckBox myCommentAtLineStart =
    new JCheckBox(UIUtil.replaceMnemonicAmpersand("&Only at line start"));
  private final JTextField myBlockCommentStart = new JTextField(5);
  private final JTextField myBlockCommentEnd = new JTextField(5);
  private final JTextField myHexPrefix = new JTextField(5);

  private final JTextField myNumPostfixes = new JTextField(5);
  private final JBList[] myKeywordsLists = new JBList[]{new JBList(), new JBList(), new JBList(), new JBList()};
  private final DefaultListModel[] myKeywordModels = new DefaultListModel[]{
    new DefaultListModel(),
    new DefaultListModel(),
    new DefaultListModel(),
    new DefaultListModel()
  };

  public CustomFileTypeEditor() {
    myLineComment.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        boolean enabled = StringUtil.isNotEmpty(myLineComment.getText());
        myCommentAtLineStart.setEnabled(enabled);
        if (!enabled) {
          myCommentAtLineStart.setSelected(false);
        }
      }
    });
    myCommentAtLineStart.setEnabled(false);
  }

  public void resetEditorFrom(AbstractFileType fileType) {
    myFileTypeName.setText(fileType.getId());
    myFileTypeDescr.setText(fileType.getDescription().get());

    SyntaxTable table = fileType.getSyntaxTable();

    if (table != null) {
      myLineComment.setText(table.getLineComment());
      myBlockCommentEnd.setText(table.getEndComment());
      myBlockCommentStart.setText(table.getStartComment());
      myHexPrefix.setText(table.getHexPrefix());
      myNumPostfixes.setText(table.getNumPostfixChars());
      myIgnoreCase.setSelected(table.isIgnoreCase());
      myCommentAtLineStart.setSelected(table.lineCommentOnlyAtStart);

      mySupportBraces.setSelected(table.isHasBraces());
      mySupportBrackets.setSelected(table.isHasBrackets());
      mySupportParens.setSelected(table.isHasParens());
      mySupportEscapes.setSelected(table.isHasStringEscapes());

      for (String s : table.getKeywords1()) {
        myKeywordModels[0].addElement(s);
      }
      for (String s : table.getKeywords2()) {
        myKeywordModels[1].addElement(s);
      }
      for (String s : table.getKeywords3()) {
        myKeywordModels[2].addElement(s);
      }
      for (String s : table.getKeywords4()) {
        myKeywordModels[3].addElement(s);
      }
    }
  }

  public void applyEditorTo(AbstractFileType type) throws ConfigurationException {
    if (myFileTypeName.getText().trim().length() == 0) {
      throw new ConfigurationException(IdeLocalize.errorNameCannotBeEmpty().get(), CommonLocalize.titleError().get());
    }
    else if (myFileTypeDescr.getText().trim().length() == 0) {
      myFileTypeDescr.setText(myFileTypeName.getText());
    }
    type.setName(myFileTypeName.getText());
    type.setDescription(myFileTypeDescr.getText());
    type.setSyntaxTable(getSyntaxTable());
  }

  @Nonnull
  public JComponent createEditor() {
    JComponent panel = createCenterPanel();
    for (int i = 0; i < myKeywordsLists.length; i++) {
      myKeywordsLists[i].setModel(myKeywordModels[i]);
    }
    return panel;
  }

  public void disposeEditor() {
  }


  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new BorderLayout());

    JPanel fileTypePanel = new JPanel(new BorderLayout());
    JPanel info = FormBuilder.createFormBuilder()
      .addLabeledComponent(IdeLocalize.editboxCustomfiletypeName().get(), myFileTypeName)
      .addLabeledComponent(IdeLocalize.editboxCustomfiletypeDescription().get(), myFileTypeDescr).getPanel();
    info.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
    fileTypePanel.add(info, BorderLayout.NORTH);

    JPanel highlighterPanel = new JPanel();
    highlighterPanel.setBorder(IdeBorderFactory.createTitledBorder(
      IdeLocalize.groupCustomfiletypeSyntaxHighlighting().get(),
      false
    ));
    highlighterPanel.setLayout(new BorderLayout());
    JPanel commentsAndNumbersPanel = new JPanel();
    commentsAndNumbersPanel.setLayout(new GridBagLayout());

    JPanel _panel1 = new JPanel(new BorderLayout());
    GridBag gb = new GridBag()
      .setDefaultFill(GridBagConstraints.HORIZONTAL)
      .setDefaultAnchor(GridBagConstraints.WEST)
      .setDefaultInsets(1, 5, 1, 5);

    commentsAndNumbersPanel.add(
      new JLabel(IdeLocalize.editboxCustomfiletypeLineComment().get()),
      gb.nextLine().next()
    );
    commentsAndNumbersPanel.add(myLineComment, gb.next());
    commentsAndNumbersPanel.add(myCommentAtLineStart, gb.next().coverLine(2));

    commentsAndNumbersPanel.add(
      new JLabel(IdeLocalize.editboxCustomfiletypeBlockCommentStart().get()),
      gb.nextLine().next()
    );
    commentsAndNumbersPanel.add(myBlockCommentStart, gb.next());
    commentsAndNumbersPanel.add(new JLabel(IdeLocalize.editboxCustomfiletypeBlockCommentEnd().get()), gb.next());
    commentsAndNumbersPanel.add(myBlockCommentEnd, gb.next());

    commentsAndNumbersPanel.add(new JLabel(IdeLocalize.editboxCustomfiletypeHexPrefix().get()), gb.nextLine().next());
    commentsAndNumbersPanel.add(myHexPrefix, gb.next());
    commentsAndNumbersPanel.add(new JLabel(IdeLocalize.editboxCustomfiletypeNumberPostfixes().get()), gb.next());
    commentsAndNumbersPanel.add(myNumPostfixes, gb.next());

    commentsAndNumbersPanel.add(mySupportBraces, gb.nextLine().next().coverLine(2));
    commentsAndNumbersPanel.add(mySupportBrackets, gb.next().next().coverLine(2));
    commentsAndNumbersPanel.add(mySupportParens, gb.nextLine().next().coverLine(2));
    commentsAndNumbersPanel.add(mySupportEscapes, gb.next().next().coverLine(2));

    _panel1.add(commentsAndNumbersPanel, BorderLayout.WEST);


    highlighterPanel.add(_panel1, BorderLayout.NORTH);

    TabbedPaneWrapper tabbedPaneWrapper = new TabbedPaneWrapper(this);
    tabbedPaneWrapper.getComponent().setBorder(
      IdeBorderFactory.createTitledBorder(IdeLocalize.listboxCustomfiletypeKeywords().get(), false)
    );
    tabbedPaneWrapper.addTab(" 1 ", createKeywordsPanel(0));
    tabbedPaneWrapper.addTab(" 2 ", createKeywordsPanel(1));
    tabbedPaneWrapper.addTab(" 3 ", createKeywordsPanel(2));
    tabbedPaneWrapper.addTab(" 4 ", createKeywordsPanel(3));

    highlighterPanel.add(tabbedPaneWrapper.getComponent(), BorderLayout.CENTER);
    highlighterPanel.add(myIgnoreCase, BorderLayout.SOUTH);

    fileTypePanel.add(highlighterPanel, BorderLayout.CENTER);

    panel.add(fileTypePanel);

    for (int i = 0; i < myKeywordsLists.length; i++) {
      final int idx = i;
      new DoubleClickListener() {
        @Override
        protected boolean onDoubleClick(MouseEvent e) {
          edit(idx);
          return true;
        }
      }.installOn(myKeywordsLists[i]);
    }


    return panel;
  }

  private JPanel createKeywordsPanel(final int index) {
    JPanel panel = ToolbarDecorator.createDecorator(myKeywordsLists[index])
      .setAddAction(button -> {
        ModifyKeywordDialog dialog = new ModifyKeywordDialog(myKeywordsLists[index], "");
        dialog.show();
        if (dialog.isOK()) {
          String keywordName = dialog.getKeywordName();
          if (!myKeywordModels[index].contains(keywordName)) myKeywordModels[index].addElement(keywordName);
        }
      })
      .setRemoveAction(button -> ListUtil.removeSelectedItems(myKeywordsLists[index]))
      .disableUpDownActions()
      .createPanel();
    panel.setBorder(null);
    return panel;
  }

  private void edit(int index) {
    if (myKeywordsLists[index].getSelectedIndex() == -1) return;
    ModifyKeywordDialog dialog =
      new ModifyKeywordDialog(myKeywordsLists[index], (String)myKeywordsLists[index].getSelectedValue());
    dialog.show();
    if (dialog.isOK()) {
      myKeywordModels[index].setElementAt(dialog.getKeywordName(), myKeywordsLists[index].getSelectedIndex());
    }
  }

  public SyntaxTable getSyntaxTable() {
    SyntaxTable syntaxTable = new SyntaxTable();
    syntaxTable.setLineComment(myLineComment.getText());
    syntaxTable.setStartComment(myBlockCommentStart.getText());
    syntaxTable.setEndComment(myBlockCommentEnd.getText());
    syntaxTable.setHexPrefix(myHexPrefix.getText());
    syntaxTable.setNumPostfixChars(myNumPostfixes.getText());
    syntaxTable.lineCommentOnlyAtStart = myCommentAtLineStart.isSelected();

    boolean ignoreCase = myIgnoreCase.isSelected();
    syntaxTable.setIgnoreCase(ignoreCase);

    syntaxTable.setHasBraces(mySupportBraces.isSelected());
    syntaxTable.setHasBrackets(mySupportBrackets.isSelected());
    syntaxTable.setHasParens(mySupportParens.isSelected());
    syntaxTable.setHasStringEscapes(mySupportEscapes.isSelected());

    for (int i = 0; i < myKeywordModels[0].size(); i++) {
      if (ignoreCase) {
        syntaxTable.addKeyword1(((String)myKeywordModels[0].getElementAt(i)).toLowerCase());
      }
      else {
        syntaxTable.addKeyword1((String)myKeywordModels[0].getElementAt(i));
      }
    }
    for (int i = 0; i < myKeywordModels[1].size(); i++) {
      if (ignoreCase) {
        syntaxTable.addKeyword2(((String)myKeywordModels[1].getElementAt(i)).toLowerCase());
      }
      else {
        syntaxTable.addKeyword2((String)myKeywordModels[1].getElementAt(i));
      }
    }
    for (int i = 0; i < myKeywordModels[2].size(); i++) {
      if (ignoreCase) {
        syntaxTable.addKeyword3(((String)myKeywordModels[2].getElementAt(i)).toLowerCase());
      }
      else {
        syntaxTable.addKeyword3((String)myKeywordModels[2].getElementAt(i));
      }
    }
    for (int i = 0; i < myKeywordModels[3].size(); i++) {
      if (ignoreCase) {
        syntaxTable.addKeyword4(((String)myKeywordModels[3].getElementAt(i)).toLowerCase());
      }
      else {
        syntaxTable.addKeyword4((String)myKeywordModels[3].getElementAt(i));
      }
    }
    return syntaxTable;
  }
}
