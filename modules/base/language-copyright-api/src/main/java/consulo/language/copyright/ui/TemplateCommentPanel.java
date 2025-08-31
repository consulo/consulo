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

package consulo.language.copyright.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import consulo.application.WriteAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.document.Document;
import consulo.language.Commenter;
import consulo.language.copyright.UpdateCopyrightsProvider;
import consulo.language.copyright.config.CopyrightFileConfig;
import consulo.language.copyright.config.CopyrightFileConfigManager;
import consulo.language.copyright.config.CopyrightManager;
import consulo.language.copyright.config.CopyrightProfile;
import consulo.language.copyright.internal.CopyrightEditorUtil;
import consulo.language.copyright.internal.CopyrightGenerator;
import consulo.language.copyright.internal.TemplateCopyrightCommenter;
import consulo.language.copyright.util.EntityUtil;
import consulo.language.copyright.util.FileTypeUtil;
import consulo.language.file.LanguageFileType;
import consulo.language.plain.PlainTextFileType;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionListener;

public class TemplateCommentPanel implements SearchableConfigurable, Configurable.NoScroll {
  private final CopyrightManager myManager;
  private final TemplateCommentPanel parentPanel;
  private final EventListenerList listeners = new EventListenerList();
  private JRadioButton[] fileLocations = null;
  private JPanel mainPanel;
  private JRadioButton rbBefore;
  private JRadioButton rbAfter;
  private JPanel fileLocationPanel;
  private JRadioButton myUseDefaultSettingsRadioButton;
  private JRadioButton myUseCustomFormattingOptionsRadioButton;
  private JRadioButton myNoCopyright;
  private JRadioButton rbLineComment;
  private JCheckBox cbPrefixLines;
  private JRadioButton rbBlockComment;
  private JPanel myCommentTypePanel;
  private JCheckBox cbSeparatorBefore;
  private JTextField txtLengthBefore;
  private JTextField txtLengthAfter;
  private JCheckBox cbAddBlank;
  private JCheckBox cbSeparatorAfter;
  private JCheckBox cbBox;
  private JTextField txtFiller;
  private JPanel myBorderPanel;
  private JLabel lblLengthBefore;
  private JLabel lblLengthAfter;
  private JLabel mySeparatorCharLabel;
  private JPanel myAdditionalPanel;
  private JPanel myPreviewEditorPanel;
  private boolean myAllowBlock;
  private Commenter myCommenter;
  private boolean myAllowSeparator;
  private final FileType myFileType;

  private Document myDocument;
  private Editor myEditor;
  private Project myProject;

  public TemplateCommentPanel(@Nonnull FileType fileType, @Nullable TemplateCommentPanel parentPanel, @Nonnull Project project) {
    this(fileType.getName(), fileType, parentPanel, project);
    myAllowBlock = FileTypeUtil.hasBlockComment(fileType);
    myCommenter = FileTypeUtil.getCommenter(fileType);
    myAllowSeparator = UpdateCopyrightsProvider.forFileType(fileType).isAllowSeparator();
  }

  public TemplateCommentPanel(@Nonnull String optionName, @Nullable FileType fileType, @Nullable TemplateCommentPanel parentPanel, @Nonnull Project project) {
    this.parentPanel = parentPanel;
    myProject = project;
    myManager = CopyrightManager.getInstance(project);
    myFileType = fileType;

    // no comboboxes for template
    if (optionName.equals(CopyrightFileConfigManager.LANG_TEMPLATE)) {
      myUseDefaultSettingsRadioButton.setVisible(false);
      myUseCustomFormattingOptionsRadioButton.setVisible(false);
      myNoCopyright.setVisible(false);
      myCommenter = TemplateCopyrightCommenter.INSTANCE;

      myAllowBlock = true;
      myAllowSeparator = true;
    }

    if (parentPanel != null) {
      parentPanel.addOptionChangeListener(this::updateOverride);
    }

    ButtonGroup group = new ButtonGroup();
    group.add(rbBefore);
    group.add(rbAfter);

    fileLocationPanel.setBorder(JBUI.Borders.empty());

    addAdditionalComponents(myAdditionalPanel);

    addOptionChangeListener(() -> showPreview(getOptions()));

    myUseDefaultSettingsRadioButton.setSelected(true);

    ActionListener listener = e -> updateOverride();

    myUseDefaultSettingsRadioButton.addActionListener(listener);
    myUseCustomFormattingOptionsRadioButton.addActionListener(listener);
    myNoCopyright.addActionListener(listener);
    txtLengthBefore.setText("80");
    txtLengthAfter.setText("80");

    rbBlockComment.addActionListener(actionEvent -> {
      cbPrefixLines.setEnabled(rbBlockComment.isSelected());
      fireChangeEvent();
    });

    rbLineComment.addActionListener(actionEvent -> {
      cbPrefixLines.setEnabled(rbBlockComment.isSelected());
      fireChangeEvent();
    });

    cbPrefixLines.addActionListener(actionEvent -> fireChangeEvent());

    cbSeparatorBefore.addActionListener(actionEvent -> {
      lblLengthBefore.setEnabled(cbSeparatorBefore.isSelected());
      txtLengthBefore.setEnabled(cbSeparatorBefore.isSelected());
      fireChangeEvent();
      updateBox();
    });

    cbSeparatorAfter.addActionListener(actionEvent -> {
      lblLengthAfter.setEnabled(cbSeparatorAfter.isSelected());
      txtLengthAfter.setEnabled(cbSeparatorAfter.isSelected());
      fireChangeEvent();
      updateBox();
    });

    DocumentAdapter documentAdapter = new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        fireChangeEvent();
        updateBox();
      }
    };
    txtLengthBefore.getDocument().addDocumentListener(documentAdapter);
    txtLengthAfter.getDocument().addDocumentListener(documentAdapter);
    txtFiller.getDocument().addDocumentListener(documentAdapter);

    cbBox.addActionListener(actionEvent -> fireChangeEvent());
  }

  private void updateBox() {
    boolean enable = true;
    if (!cbSeparatorBefore.isSelected()) {
      enable = false;
    }
    else if (!cbSeparatorAfter.isSelected()) {
      enable = false;
    }
    else {
      if (!txtLengthBefore.getText().equals(txtLengthAfter.getText())) {
        enable = false;
      }
    }

    boolean either = cbSeparatorBefore.isSelected() || cbSeparatorAfter.isSelected();

    cbBox.setEnabled(enable);

    txtFiller.setEnabled(either);
  }

  public void addAdditionalComponents(@Nonnull JPanel additionalPanel) {

  }

  public void addLocationInFile(@Nonnull String[] locations) {
    fileLocationPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Location in File"));
    fileLocations = new JRadioButton[locations.length];
    ButtonGroup group = new ButtonGroup();
    for (int i = 0; i < fileLocations.length; i++) {
      fileLocations[i] = new JRadioButton(locations[i]);
      group.add(fileLocations[i]);

      fileLocationPanel.add(fileLocations[i],
                            new GridConstraints(i, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                GridConstraints.SIZEPOLICY_FIXED, null, null, null));
    }
  }

  public CopyrightFileConfig getOptions() {
    // If this is a fully custom comment we should really ensure there are no blank lines in the comments outside
    // of a block comment. If there are any blank lines the replacement logic will fall apart.
    CopyrightFileConfig res = myFileType == null ? new CopyrightFileConfig() : UpdateCopyrightsProvider.forFileType(myFileType).createDefaultOptions();
    res.setBlock(rbBlockComment.isSelected());
    res.setPrefixLines(!myAllowBlock || cbPrefixLines.isSelected());
    res.setSeparateAfter(cbSeparatorAfter.isSelected());
    res.setSeparateBefore(cbSeparatorBefore.isSelected());
    try {
      Integer val = Integer.parseInt(txtLengthBefore.getText());
      res.setLenBefore(val);
      val = Integer.parseInt(txtLengthAfter.getText());
      res.setLenAfter(val);
    }
    catch (NumberFormatException e) {
      //leave blank
    }
    res.setBox(cbBox.isSelected());

    String filler = txtFiller.getText();
    if (filler.length() > 0) {
      res.setFiller(filler);
    }
    else {
      res.setFiller(CopyrightFileConfig.DEFAULT_FILLER);
    }

    res.setFileTypeOverride(getOverrideChoice());
    res.setRelativeBefore(rbBefore.isSelected());
    res.setAddBlankAfter(cbAddBlank.isSelected());
    if (fileLocations != null) {
      for (int i = 0; i < fileLocations.length; i++) {
        if (fileLocations[i].isSelected()) {
          res.setFileLocation(i + 1);
        }
      }
    }

    return res;
  }

  private int getOverrideChoice() {
    return myUseDefaultSettingsRadioButton.isSelected() ? CopyrightFileConfig.USE_TEMPLATE : myNoCopyright.isSelected() ? CopyrightFileConfig.NO_COPYRIGHT : CopyrightFileConfig.USE_TEXT;
  }

  private void updateOverride() {
    int choice = getOverrideChoice();
    CopyrightFileConfig parentOpts = parentPanel != null ? parentPanel.getOptions() : null;
    switch (choice) {
      case CopyrightFileConfig.NO_COPYRIGHT:
        enableFormattingOptions(false);
        showPreview(getOptions());
        rbBefore.setEnabled(false);
        rbAfter.setEnabled(false);
        cbAddBlank.setEnabled(false);
        if (fileLocations != null) {
          for (JRadioButton fileLocation : fileLocations) {
            fileLocation.setEnabled(false);
          }
        }
        break;
      case CopyrightFileConfig.USE_TEMPLATE:
        boolean isTemplate = parentPanel == null;
        enableFormattingOptions(isTemplate);
        showPreview(parentOpts != null ? parentOpts : getOptions());
        rbBefore.setEnabled(isTemplate);
        rbAfter.setEnabled(isTemplate);
        cbAddBlank.setEnabled(isTemplate);
        if (fileLocations != null) {
          for (JRadioButton fileLocation : fileLocations) {
            fileLocation.setEnabled(true);
          }
        }
        break;
      case CopyrightFileConfig.USE_TEXT:
        enableFormattingOptions(true);
        showPreview(getOptions());
        rbBefore.setEnabled(true);
        rbAfter.setEnabled(true);
        cbAddBlank.setEnabled(true);
        if (fileLocations != null) {
          for (JRadioButton fileLocation : fileLocations) {
            fileLocation.setEnabled(true);
          }
        }
        break;
    }
  }

  private void enableFormattingOptions(boolean enable) {
    if (enable) {
      rbBlockComment.setEnabled(true);
      rbLineComment.setEnabled(true);
      cbPrefixLines.setEnabled(myAllowBlock);
      cbSeparatorBefore.setEnabled(true);
      cbSeparatorAfter.setEnabled(true);
      lblLengthBefore.setEnabled(cbSeparatorBefore.isSelected());
      txtLengthBefore.setEnabled(cbSeparatorBefore.isSelected());
      lblLengthAfter.setEnabled(cbSeparatorAfter.isSelected());
      txtLengthAfter.setEnabled(cbSeparatorAfter.isSelected());
      mySeparatorCharLabel.setEnabled(true);
      updateBox();
    }
    else {
      UIUtil.setEnabled(myCommentTypePanel, enable, true);
      UIUtil.setEnabled(myBorderPanel, enable, true);
    }
  }

  private void showPreview(CopyrightFileConfig options) {
    String defaultCopyrightText;

    if (myNoCopyright.isSelected()) {
      defaultCopyrightText = "";
    }
    else {
      String evaluate;
      try {
        evaluate = CopyrightGenerator.getInstance(myProject).generate(null, null, EntityUtil.decode(CopyrightProfile.DEFAULT_COPYRIGHT_NOTICE));
      }
      catch (Exception e) {
        evaluate = "";
      }
      defaultCopyrightText = FileTypeUtil.buildComment(myCommenter, myAllowSeparator, evaluate, options);
    }

    initEditor();
    WriteAction.run(() -> myDocument.setText(defaultCopyrightText));
  }

  @Nonnull
  @Override
  public LocalizeValue getDisplayName() {
    if (myFileType instanceof LanguageFileType) {
      return ((LanguageFileType)myFileType).getLanguage().getDisplayName();
    }
    return myFileType.getDisplayName();
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent() {
    initEditor();
    return mainPanel;
  }

  private void initEditor() {
    if (myEditor == null) {
      myPreviewEditorPanel.removeAll();
      EditorFactory editorFactory = EditorFactory.getInstance();
      myDocument = editorFactory.createDocument("");

      myEditor = editorFactory.createEditor(myDocument, myProject, ObjectUtil.notNull(myFileType, PlainTextFileType.INSTANCE), true);
      CopyrightEditorUtil.setupEditor(myEditor);

      myPreviewEditorPanel.add(myEditor.getComponent(), BorderLayout.CENTER);
    }
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    if (myEditor != null) {
      EditorFactory.getInstance().releaseEditor(myEditor);
      myEditor = null;
    }
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    if (myFileType == null) {
      return !myManager.getCopyrightFileConfigManager().getTemplateOptions().equals(getOptions());
    }
    else {
      return !myManager.getCopyrightFileConfigManager().getOptions(myFileType).equals(getOptions());
    }
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    CopyrightFileConfigManager copyrightFileConfigManager = myManager.getCopyrightFileConfigManager();
    if (myFileType == null) {
      copyrightFileConfigManager.setTemplateOptions(getOptions());
    }
    else {
      copyrightFileConfigManager.setOptions(myFileType, getOptions());
    }
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    CopyrightFileConfig options = myFileType == null ? myManager.getCopyrightFileConfigManager().getTemplateOptions() : myManager.getCopyrightFileConfigManager().getOptions(myFileType);
    boolean isBlock = options.isBlock();
    if (isBlock) {
      rbBlockComment.setSelected(true);
    }
    else {
      rbLineComment.setSelected(true);
    }

    cbPrefixLines.setSelected(!myAllowBlock || options.isPrefixLines());
    cbSeparatorAfter.setSelected(options.isSeparateAfter());
    cbSeparatorBefore.setSelected(options.isSeparateBefore());
    txtLengthBefore.setText(String.valueOf(options.getLenBefore()));
    txtLengthAfter.setText(String.valueOf(options.getLenAfter()));
    txtFiller.setText(options.getFiller() == CopyrightFileConfig.DEFAULT_FILLER ? "" : options.getFiller());
    cbBox.setSelected(options.isBox());

    int fileTypeOverride = options.getFileTypeOverride();
    myUseDefaultSettingsRadioButton.setSelected(fileTypeOverride == CopyrightFileConfig.USE_TEMPLATE);
    myUseCustomFormattingOptionsRadioButton.setSelected(fileTypeOverride == CopyrightFileConfig.USE_TEXT);
    myNoCopyright.setSelected(fileTypeOverride == CopyrightFileConfig.NO_COPYRIGHT);
    if (options.isRelativeBefore()) {
      rbBefore.setSelected(true);
    }
    else {
      rbAfter.setSelected(true);
    }
    cbAddBlank.setSelected(options.isAddBlankAfter());

    if (fileLocations != null) {
      int choice = options.getFileLocation() - 1;
      choice = Math.max(0, Math.min(choice, fileLocations.length - 1));
      fileLocations[choice].setSelected(true);
    }

    updateOverride();
  }

  public void addOptionChangeListener(TemplateOptionsPanelListener listener) {
    listeners.add(TemplateOptionsPanelListener.class, listener);
  }

  private void fireChangeEvent() {
    Object[] fires = listeners.getListenerList();
    for (int i = fires.length - 2; i >= 0; i -= 2) {
      if (fires[i] == TemplateOptionsPanelListener.class) {
        ((TemplateOptionsPanelListener)fires[i + 1]).optionChanged();
      }
    }
  }

  @Override
  public String getHelpTopic() {
    return "copyright.filetypes";
  }

  @Override
  @Nonnull
  public String getId() {
    return getHelpTopic() + "." + myFileType.getId();
  }
}
