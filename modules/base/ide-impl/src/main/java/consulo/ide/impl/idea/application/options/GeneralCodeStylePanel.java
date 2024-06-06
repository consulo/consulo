/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.application.Application;
import consulo.application.ApplicationBundle;
import consulo.codeEditor.EditorHighlighter;
import consulo.colorScheme.EditorColorsScheme;
import consulo.configurable.ConfigurationException;
import consulo.configurable.internal.ConfigurableUIMigrationUtil;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.application.options.codeStyle.excludedFiles.ExcludedFilesList;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.ui.setting.CodeStyleAbstractPanel;
import consulo.language.codeStyle.ui.setting.CodeStyleSchemesModel;
import consulo.language.codeStyle.ui.setting.GeneralCodeStyleOptionsProvider;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.language.plain.PlainTextFileType;
import consulo.logging.Logger;
import consulo.project.ProjectManager;
import consulo.ui.NotificationType;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.valueEditor.ValueValidationException;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.BalloonBuilder;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static consulo.language.codeStyle.CodeStyleSettings.MAX_RIGHT_MARGIN;

public class GeneralCodeStylePanel extends CodeStyleAbstractPanel {
  @SuppressWarnings("UnusedDeclaration")
  private static final Logger LOG = Logger.getInstance(GeneralCodeStylePanel.class);

  private static final String SYSTEM_DEPENDANT_STRING = ApplicationBundle.message("combobox.crlf.system.dependent");
  private static final String UNIX_STRING = ApplicationBundle.message("combobox.crlf.unix");
  private static final String WINDOWS_STRING = ApplicationBundle.message("combobox.crlf.windows");
  private static final String MACINTOSH_STRING = ApplicationBundle.message("combobox.crlf.mac");
  private final List<GeneralCodeStyleOptionsProvider> myAdditionalOptions;

  private IntegerField myRightMarginField;

  private ComboBox<String> myLineSeparatorCombo;
  private JPanel myPanel;
  private JBCheckBox myCbWrapWhenTypingReachesRightMargin;
  private JCheckBox myEnableFormatterTags;
  private JTextField myFormatterOnTagField;
  private JTextField myFormatterOffTagField;
  private JCheckBox myAcceptRegularExpressionsCheckBox;
  private JBLabel myFormatterOffLabel;
  private JBLabel myFormatterOnLabel;
  private JPanel myMarkerOptionsPanel;
  private JPanel myAdditionalSettingsPanel;
  private JCheckBox myAutodetectIndentsBox;
  private JPanel myIndentsDetectionPanel;
  private CommaSeparatedIntegersField myVisualGuides;
  private JBLabel myVisualGuidesHint;
  private JBLabel myLineSeparatorHint;
  private JBLabel myVisualGuidesLabel;
  private ExcludedFilesList myExcludedFilesList;
  private JPanel myExcludedFilesPanel;
  private JPanel myToolbarPanel;
  private JBTabbedPane myTabbedPane;
  private static int ourSelectedTabIndex = -1;

  public GeneralCodeStylePanel(CodeStyleSettings settings) {
    super(settings);

    myLineSeparatorCombo.setRenderer(new ColoredListCellRenderer<String>() {
      @Override
      protected void customizeCellRenderer(@Nonnull JList list, String value, int index, boolean selected, boolean hasFocus) {
        append(consulo.util.lang.StringUtil.notNullize(value));
      }
    });
    //noinspection unchecked
    myLineSeparatorCombo.addItem(SYSTEM_DEPENDANT_STRING);
    //noinspection unchecked
    myLineSeparatorCombo.addItem(UNIX_STRING);
    //noinspection unchecked
    myLineSeparatorCombo.addItem(WINDOWS_STRING);
    //noinspection unchecked
    myLineSeparatorCombo.addItem(MACINTOSH_STRING);
    addPanelToWatch(myPanel);

    myRightMarginField.setDefaultValue(settings.getDefaultRightMargin());

    myEnableFormatterTags.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        boolean tagsEnabled = myEnableFormatterTags.isSelected();
        setFormatterTagControlsEnabled(tagsEnabled);
      }
    });

    myIndentsDetectionPanel.setBorder(IdeBorderFactory.createTitledBorder(ApplicationBundle.message("settings.code.style.general.indents.detection")));

    myPanel.setBorder(JBUI.Borders.empty(0, 10));

    myAdditionalSettingsPanel.setLayout(new VerticalFlowLayout(true, true));
    myAdditionalSettingsPanel.removeAll();
    myAdditionalOptions = Application.get().getExtensionList(GeneralCodeStyleOptionsProvider.class);
    for (GeneralCodeStyleOptionsProvider provider : myAdditionalOptions) {
      JComponent generalSettingsComponent = ConfigurableUIMigrationUtil.createComponent(provider, this);
      if (generalSettingsComponent != null) {
        myAdditionalSettingsPanel.add(generalSettingsComponent);
      }
    }

    myVisualGuidesLabel.setText(ApplicationBundle.message("settings.code.style.visual.guides") + ":");
    myVisualGuidesHint.setForeground(JBColor.GRAY);
    myVisualGuidesHint.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
    myLineSeparatorHint.setForeground(JBColor.GRAY);
    myLineSeparatorHint.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));

    myExcludedFilesList.initModel();
    myToolbarPanel.add(myExcludedFilesList.getDecorator().createPanel());
    myExcludedFilesPanel.setBorder(IdeBorderFactory.createTitledBorder(ApplicationBundle.message("settings.code.style.general.excluded.files")));
    if (ourSelectedTabIndex >= 0) {
      myTabbedPane.setSelectedIndex(ourSelectedTabIndex);
    }
    myTabbedPane.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        //noinspection AssignmentToStaticFieldFromInstanceMethod
        ourSelectedTabIndex = myTabbedPane.getSelectedIndex();
      }
    });
  }


  @Override
  protected int getRightMargin() {
    return myRightMarginField.getValue();
  }

  @Override
  @Nonnull
  protected FileType getFileType() {
    return PlainTextFileType.INSTANCE;
  }

  @Override
  protected String getPreviewText() {
    return null;
  }


  @Override
  public void apply(CodeStyleSettings settings) throws ConfigurationException {
    try {
      myVisualGuides.validateContent();
      myRightMarginField.validateContent();
    }
    catch (ValueValidationException e) {
      throw new ConfigurationException(e.getMessage());
    }
    settings.setDefaultSoftMargins(myVisualGuides.getValue());
    myExcludedFilesList.apply(settings);

    settings.LINE_SEPARATOR = getSelectedLineSeparator();

    settings.setDefaultRightMargin(getRightMargin());
    settings.WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN = myCbWrapWhenTypingReachesRightMargin.isSelected();

    settings.FORMATTER_TAGS_ENABLED = myEnableFormatterTags.isSelected();
    settings.FORMATTER_TAGS_ACCEPT_REGEXP = myAcceptRegularExpressionsCheckBox.isSelected();

    settings.FORMATTER_OFF_TAG = getTagText(myFormatterOffTagField, settings.FORMATTER_OFF_TAG);
    settings.setFormatterOffPattern(compilePattern(settings, myFormatterOffTagField, settings.FORMATTER_OFF_TAG));

    settings.FORMATTER_ON_TAG = getTagText(myFormatterOnTagField, settings.FORMATTER_ON_TAG);
    settings.setFormatterOnPattern(compilePattern(settings, myFormatterOnTagField, settings.FORMATTER_ON_TAG));

    settings.AUTODETECT_INDENTS = myAutodetectIndentsBox.isSelected();

    for (GeneralCodeStyleOptionsProvider option : myAdditionalOptions) {
      option.apply(settings);
    }
  }

  private void createUIComponents() {
    myRightMarginField = new IntegerField(ApplicationBundle.message("editbox.right.margin.columns"), 0, MAX_RIGHT_MARGIN);
    myVisualGuides = new CommaSeparatedIntegersField(ApplicationBundle.message("settings.code.style.visual.guides"), 0, MAX_RIGHT_MARGIN, "Optional");
    myExcludedFilesList = new ExcludedFilesList();
  }

  @Nullable
  private static Pattern compilePattern(CodeStyleSettings settings, JTextField field, String patternText) {
    try {
      return Pattern.compile(patternText);
    }
    catch (PatternSyntaxException pse) {
      settings.FORMATTER_TAGS_ACCEPT_REGEXP = false;
      showError(field, ApplicationBundle.message("settings.code.style.general.formatter.marker.invalid.regexp"));
      return null;
    }
  }

  private static String getTagText(JTextField field, String defaultValue) {
    String fieldText = field.getText();
    if (StringUtil.isEmpty(field.getText())) {
      return defaultValue;
    }
    return fieldText;
  }

  @Nullable
  private String getSelectedLineSeparator() {
    if (UNIX_STRING.equals(myLineSeparatorCombo.getSelectedItem())) {
      return "\n";
    }
    else if (MACINTOSH_STRING.equals(myLineSeparatorCombo.getSelectedItem())) {
      return "\r";
    }
    else if (WINDOWS_STRING.equals(myLineSeparatorCombo.getSelectedItem())) {
      return "\r\n";
    }
    return null;
  }


  @Override
  public boolean isModified(CodeStyleSettings settings) {
    if (!myVisualGuides.getValue().equals(settings.getDefaultSoftMargins())) return true;

    if (myExcludedFilesList.isModified(settings)) return true;

    if (!Comparing.equal(getSelectedLineSeparator(), settings.LINE_SEPARATOR)) {
      return true;
    }

    if (settings.WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN ^ myCbWrapWhenTypingReachesRightMargin.isSelected()) {
      return true;
    }

    if (getRightMargin() != settings.getDefaultRightMargin()) return true;

    if (myEnableFormatterTags.isSelected()) {
      if (!settings.FORMATTER_TAGS_ENABLED ||
          settings.FORMATTER_TAGS_ACCEPT_REGEXP != myAcceptRegularExpressionsCheckBox.isSelected() ||
          !StringUtil.equals(myFormatterOffTagField.getText(), settings.FORMATTER_OFF_TAG) ||
          !StringUtil.equals(myFormatterOnTagField.getText(), settings.FORMATTER_ON_TAG)) return true;
    }
    else {
      if (settings.FORMATTER_TAGS_ENABLED) return true;
    }

    for (GeneralCodeStyleOptionsProvider option : myAdditionalOptions) {
      if (option.isModified(settings)) return true;
    }

    if (settings.AUTODETECT_INDENTS != myAutodetectIndentsBox.isSelected()) return true;

    return false;
  }

  @Override
  public JComponent getPanel() {
    return myPanel;
  }

  @Override
  protected void resetImpl(final CodeStyleSettings settings) {
    myVisualGuides.setValue(settings.getDefaultSoftMargins());

    myExcludedFilesList.reset(settings);

    String lineSeparator = settings.LINE_SEPARATOR;
    if ("\n".equals(lineSeparator)) {
      myLineSeparatorCombo.setSelectedItem(UNIX_STRING);
    }
    else if ("\r\n".equals(lineSeparator)) {
      myLineSeparatorCombo.setSelectedItem(WINDOWS_STRING);
    }
    else if ("\r".equals(lineSeparator)) {
      myLineSeparatorCombo.setSelectedItem(MACINTOSH_STRING);
    }
    else {
      myLineSeparatorCombo.setSelectedItem(SYSTEM_DEPENDANT_STRING);
    }

    myRightMarginField.setValue(settings.getDefaultRightMargin());
    myCbWrapWhenTypingReachesRightMargin.setSelected(settings.WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN);

    myAcceptRegularExpressionsCheckBox.setSelected(settings.FORMATTER_TAGS_ACCEPT_REGEXP);
    myEnableFormatterTags.setSelected(settings.FORMATTER_TAGS_ENABLED);

    myFormatterOnTagField.setText(settings.FORMATTER_ON_TAG);
    myFormatterOffTagField.setText(settings.FORMATTER_OFF_TAG);

    setFormatterTagControlsEnabled(settings.FORMATTER_TAGS_ENABLED);

    myAutodetectIndentsBox.setSelected(settings.AUTODETECT_INDENTS);

    for (GeneralCodeStyleOptionsProvider option : myAdditionalOptions) {
      option.reset(settings);
    }
  }

  private void setFormatterTagControlsEnabled(boolean isEnabled) {
    myFormatterOffTagField.setEnabled(isEnabled);
    myFormatterOnTagField.setEnabled(isEnabled);
    myAcceptRegularExpressionsCheckBox.setEnabled(isEnabled);
    myFormatterOffLabel.setEnabled(isEnabled);
    myFormatterOnLabel.setEnabled(isEnabled);
    myMarkerOptionsPanel.setEnabled(isEnabled);
  }

  @Override
  protected EditorHighlighter createHighlighter(final EditorColorsScheme scheme) {
    //noinspection NullableProblems
    return EditorHighlighterFactory.getInstance().createEditorHighlighter(getFileType(), scheme, null);
  }


  @Override
  public Language getDefaultLanguage() {
    return null;
  }

  private static void showError(final JTextField field, final String message) {
    BalloonBuilder balloonBuilder = JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(message, NotificationType.ERROR, null);
    balloonBuilder.setFadeoutTime(1500);
    final Balloon balloon = balloonBuilder.createBalloon();
    final Rectangle rect = field.getBounds();
    final Point p = new Point(0, rect.height);
    final RelativePoint point = new RelativePoint(field, p);
    balloon.show(point, Balloon.Position.below);
    Disposer.register(ProjectManager.getInstance().getDefaultProject(), balloon);
  }

  @Override
  public void setModel(@Nonnull CodeStyleSchemesModel model) {
    super.setModel(model);
    myExcludedFilesList.setSchemesModel(model);
  }
}
