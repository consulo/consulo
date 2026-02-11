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

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import consulo.application.Application;
import consulo.application.localize.ApplicationLocalize;
import consulo.codeEditor.EditorHighlighter;
import consulo.colorScheme.EditorColorsScheme;
import consulo.configurable.ConfigurationException;
import consulo.configurable.internal.ConfigurableUIMigrationUtil;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.application.options.codeStyle.excludedFiles.ExcludedFilesList;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.localize.CodeStyleLocalize;
import consulo.language.codeStyle.ui.setting.CodeStyleAbstractPanel;
import consulo.language.codeStyle.ui.setting.CodeStyleSchemesModel;
import consulo.language.codeStyle.ui.setting.GeneralCodeStyleOptionsProvider;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.language.plain.PlainTextFileType;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.ProjectManager;
import consulo.ui.ComboBox;
import consulo.ui.Label;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.valueEditor.ValueValidationException;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.BalloonBuilder;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.style.StandardColors;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.Component;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static consulo.language.codeStyle.CodeStyleSettings.MAX_RIGHT_MARGIN;

public class GeneralCodeStylePanel extends CodeStyleAbstractPanel {
    @SuppressWarnings("UnusedDeclaration")
    private static final Logger LOG = Logger.getInstance(GeneralCodeStylePanel.class);

    enum LineBreak {
        SYSTEM_DEPENDENT(ApplicationLocalize.comboboxCrlfSystemDependent(), null),
        UNIX(ApplicationLocalize.comboboxCrlfUnix(), "\n"),
        WINDOWS(ApplicationLocalize.comboboxCrlfWindows(), "\r\n"),
        CLASSIC_MAC(ApplicationLocalize.comboboxCrlfMac(), "\r");

        @Nonnull
        private final LocalizeValue myName;
        @Nullable
        private final String myLineSeparator;

        LineBreak(@Nonnull LocalizeValue name, @Nullable String lineSeparator) {
            myName = name;
            myLineSeparator = lineSeparator;
        }

        @Nonnull
        public LocalizeValue getName() {
            return myName;
        }

        @Nullable
        public String getLineSeparator() {
            return myLineSeparator;
        }
    }

    private final List<GeneralCodeStyleOptionsProvider> myAdditionalOptions;

    private IntegerField myRightMarginField;

    private ComboBox<LineBreak> myLineSeparatorCombo;
    private JPanel myPanel;
    private CheckBox myCbWrapWhenTypingReachesRightMargin;
    private CheckBox myEnableFormatterTags;
    private TextBox myFormatterOnTagField;
    private TextBox myFormatterOffTagField;
    private CheckBox myAcceptRegularExpressionsCheckBox;
    private Label myFormatterOffLabel;
    private Label myFormatterOnLabel;
    private JPanel myMarkerOptionsPanel;
    private JPanel myAdditionalSettingsPanel;
    private CheckBox myAutodetectIndentsBox;
    private JPanel myIndentsDetectionPanel;
    private CommaSeparatedIntegersField myVisualGuides;
    private Label myVisualGuidesHint;
    private Label myLineSeparatorHint;
    private Label myVisualGuidesLabel;
    private ExcludedFilesList myExcludedFilesList;
    private JPanel myExcludedFilesPanel;
    private JPanel myToolbarPanel;
    private JBTabbedPane myTabbedPane;
    private static int ourSelectedTabIndex = -1;

    @RequiredUIAccess
    public GeneralCodeStylePanel(CodeStyleSettings settings) {
        super(settings);

        createUIComponents();
        addPanelToWatch(myPanel);

        myRightMarginField.setDefaultValue(settings.getDefaultRightMargin());

        myEnableFormatterTags.addClickListener(e -> {
            boolean tagsEnabled = myEnableFormatterTags.getValue();
            setFormatterTagControlsEnabled(tagsEnabled);
        });

        myIndentsDetectionPanel.setBorder(IdeBorderFactory.createTitledBorder(
            ApplicationLocalize.settingsCodeStyleGeneralIndentsDetection().get()
        ));

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

        myVisualGuidesLabel.setText(LocalizeValue.join(ApplicationLocalize.settingsCodeStyleVisualGuides(), LocalizeValue.colon()));
        myVisualGuidesHint.setForegroundColor(StandardColors.GRAY);
//        myVisualGuidesHint.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
        myLineSeparatorHint.setForegroundColor(StandardColors.GRAY);
//        myLineSeparatorHint.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));

        myExcludedFilesList.initModel();
        myToolbarPanel.add(myExcludedFilesList.getDecorator().createPanel());
        myExcludedFilesPanel.setBorder(IdeBorderFactory.createTitledBorder(
            ApplicationLocalize.settingsCodeStyleGeneralExcludedFiles().get()
        ));
        if (ourSelectedTabIndex >= 0) {
            myTabbedPane.setSelectedIndex(ourSelectedTabIndex);
        }
        myTabbedPane.addChangeListener(e -> {
            //noinspection AssignmentToStaticFieldFromInstanceMethod
            ourSelectedTabIndex = myTabbedPane.getSelectedIndex();
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
        settings.WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN = myCbWrapWhenTypingReachesRightMargin.getValue();

        settings.FORMATTER_TAGS_ENABLED = myEnableFormatterTags.getValue();
        settings.FORMATTER_TAGS_ACCEPT_REGEXP = myAcceptRegularExpressionsCheckBox.getValue();

        settings.FORMATTER_OFF_TAG = getTagText(myFormatterOffTagField, settings.FORMATTER_OFF_TAG);
        settings.setFormatterOffPattern(compilePattern(settings, myFormatterOffTagField, settings.FORMATTER_OFF_TAG));

        settings.FORMATTER_ON_TAG = getTagText(myFormatterOnTagField, settings.FORMATTER_ON_TAG);
        settings.setFormatterOnPattern(compilePattern(settings, myFormatterOnTagField, settings.FORMATTER_ON_TAG));

        settings.AUTODETECT_INDENTS = myAutodetectIndentsBox.getValue();

        for (GeneralCodeStyleOptionsProvider option : myAdditionalOptions) {
            option.apply(settings);
        }
    }

    @RequiredUIAccess
    private void createUIComponents() {
        myRightMarginField =
            new IntegerField(CodeStyleLocalize.editboxRightMarginColumns().get(), 0, MAX_RIGHT_MARGIN);
        myVisualGuides = new CommaSeparatedIntegersField(
            ApplicationLocalize.settingsCodeStyleVisualGuides().get(),
            0,
            MAX_RIGHT_MARGIN,
            "Optional"
        );
        myExcludedFilesList = new ExcludedFilesList();

        myPanel = new JPanel();
        myPanel.setLayout(new BorderLayout(0, 0));
        myTabbedPane = new JBTabbedPane();
        myPanel.add(myTabbedPane, BorderLayout.CENTER);
        JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(8, 4, JBUI.emptyInsets(), -1, -1));
        myTabbedPane.addTab("General", panel1);
        Label label1 = Label.create(ApplicationLocalize.comboboxLineSeparatorForNewFiles());
        panel1.add(
            TargetAWT.to(label1),
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
        myLineSeparatorCombo = ComboBox.create(
            LineBreak.SYSTEM_DEPENDENT,
            LineBreak.UNIX,
            LineBreak.WINDOWS,
            LineBreak.CLASSIC_MAC
        );
        myLineSeparatorCombo.setTextRenderer(LineBreak::getName);
        panel1.add(
            TargetAWT.to(myLineSeparatorCombo),
            new GridConstraints(
                0,
                1,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                new Dimension(100, -1),
                null,
                0,
                false
            )
        );
        myLineSeparatorHint = Label.create(ApplicationLocalize.comboboxLineseparatorForNewFilesHint());
        panel1.add(
            TargetAWT.to(myLineSeparatorHint),
            new GridConstraints(
                1,
                1,
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
        JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, ApplicationLocalize.editboxRightMarginColumns());
        panel1.add(
            label2,
            new GridConstraints(
                2,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
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
            myRightMarginField,
            new GridConstraints(
                2,
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
        myCbWrapWhenTypingReachesRightMargin = CheckBox.create(CodeStyleLocalize.wrappingWrapOnTyping());
        panel1.add(
            TargetAWT.to(myCbWrapWhenTypingReachesRightMargin),
            new GridConstraints(
                2,
                3,
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
        panel1.add(
            myVisualGuides,
            new GridConstraints(
                3,
                1,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        myVisualGuidesLabel = Label.create(ApplicationLocalize.settingsCodeStyleVisualGuides());
        panel1.add(
            TargetAWT.to(myVisualGuidesLabel),
            new GridConstraints(
                3,
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
        myVisualGuidesHint = Label.create(ApplicationLocalize.settingsCodeStyleGeneralVisualGuidesHint());
        panel1.add(
            TargetAWT.to(myVisualGuidesHint),
            new GridConstraints(
                4,
                1,
                1,
                3,
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
        panel1.add(
            TargetAWT.to(Label.create(ApplicationLocalize.marginColumns())),
            new GridConstraints(
                3,
                2,
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
        panel1.add(
            TargetAWT.to(Label.create(ApplicationLocalize.marginColumns())),
            new GridConstraints(
                2,
                2,
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
        myIndentsDetectionPanel = new JPanel();
        myIndentsDetectionPanel.setLayout(new GridLayoutManager(1, 1, JBUI.emptyInsets(), -1, -1));
        panel1.add(
            myIndentsDetectionPanel,
            new GridConstraints(
                5,
                0,
                1,
                4,
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
        myIndentsDetectionPanel.setBorder(BorderFactory.createTitledBorder(
            ApplicationLocalize.settingsCodeStyleGeneralIndentsDetection().get()
        ));
        myAutodetectIndentsBox = CheckBox.create(ApplicationLocalize.settingsCodeStyleGeneralAutodetectIndents());
        myIndentsDetectionPanel.add(
            TargetAWT.to(myAutodetectIndentsBox),
            new GridConstraints(
                0,
                0,
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
        myAdditionalSettingsPanel = new JPanel();
        myAdditionalSettingsPanel.setLayout(new BorderLayout(0, 0));
        panel1.add(
            myAdditionalSettingsPanel,
            new GridConstraints(
                6,
                0,
                1,
                4,
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
            new Spacer(),
            new GridConstraints(
                7,
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
        JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(3, 2, JBUI.emptyInsets(), -1, -1));
        myTabbedPane.addTab(LocalizeValue.localizeTODO("Formatter Control").get(), panel2);
        myEnableFormatterTags = CheckBox.create(ApplicationLocalize.settingsCodeStyleGeneralEnableFormatterTags());
        panel2.add(
            TargetAWT.to(myEnableFormatterTags),
            new GridConstraints(
                0,
                0,
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
        myMarkerOptionsPanel = new JPanel();
        myMarkerOptionsPanel.setLayout(new GridLayoutManager(3, 2, JBUI.emptyInsets(), -1, -1));
        panel2.add(
            myMarkerOptionsPanel,
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
        myFormatterOffLabel = Label.create(ApplicationLocalize.settingsCodeStyleGeneralFormatterOffTag());
        myMarkerOptionsPanel.add(
            TargetAWT.to(myFormatterOffLabel),
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
                2,
                false
            )
        );
        myFormatterOffTagField = TextBox.create();
        myMarkerOptionsPanel.add(
            TargetAWT.to(myFormatterOffTagField),
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

        myFormatterOnLabel = Label.create(ApplicationLocalize.settingsCodeStyleGeneralFormatterOnTag());
        myMarkerOptionsPanel.add(
            TargetAWT.to(myFormatterOnLabel),
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
                2,
                false
            )
        );
        myFormatterOnTagField = TextBox.create();
        myMarkerOptionsPanel.add(
            TargetAWT.to(myFormatterOnTagField),
            new GridConstraints(
                1,
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
        myAcceptRegularExpressionsCheckBox = CheckBox.create(ApplicationLocalize.settingsCodeStyleGeneralFormatterMarkerRegexp());
        myMarkerOptionsPanel.add(
            TargetAWT.to(myAcceptRegularExpressionsCheckBox),
            new GridConstraints(
                2,
                0,
                1,
                2,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                2,
                false
            )
        );
        panel2.add(
            new Spacer(),
            new GridConstraints(
                1,
                1,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                1,
                null,
                null,
                null,
                0,
                false
            )
        );
        myExcludedFilesPanel = new JPanel();
        myExcludedFilesPanel.setLayout(new GridLayoutManager(1, 2, JBUI.emptyInsets(), -1, -1));
        panel2.add(
            myExcludedFilesPanel,
            new GridConstraints(
                2,
                0,
                1,
                2,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        myExcludedFilesPanel.setBorder(BorderFactory.createTitledBorder(LocalizeValue.localizeTODO("Do not format:").get()));
        myExcludedFilesPanel.add(
            myExcludedFilesList,
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        myToolbarPanel = new JPanel();
        myToolbarPanel.setLayout(new BorderLayout(0, 0));
        myExcludedFilesPanel.add(
            myToolbarPanel,
            new GridConstraints(
                0,
                1,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                1,
                null,
                null,
                null,
                0,
                false
            )
        );
        label1.setTarget(myLineSeparatorCombo);
        myFormatterOffLabel.setTarget(myFormatterOffTagField);
        myFormatterOnLabel.setTarget(myFormatterOnTagField);
    }

    private void $$$loadLabelText$$$(JLabel component, @Nonnull LocalizeValue text0) {
        StringBuilder result = new StringBuilder();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        String text = text0.get();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) {
                    break;
                }
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    @Nullable
    private static Pattern compilePattern(CodeStyleSettings settings, TextBox field, String patternText) {
        try {
            return Pattern.compile(patternText);
        }
        catch (PatternSyntaxException pse) {
            settings.FORMATTER_TAGS_ACCEPT_REGEXP = false;
            showError(TargetAWT.to(field), ApplicationLocalize.settingsCodeStyleGeneralFormatterMarkerInvalidRegexp());
            return null;
        }
    }

    private static String getTagText(TextBox field, String defaultValue) {
        String fieldText = field.getValue();
        if (StringUtil.isEmpty(field.getValue())) {
            return defaultValue;
        }
        return fieldText;
    }

    @Nullable
    private String getSelectedLineSeparator() {
        LineBreak selectedLineBreak = myLineSeparatorCombo.getValue();
        return selectedLineBreak == null ? null : selectedLineBreak.getLineSeparator();
    }

    @Override
    public boolean isModified(CodeStyleSettings settings) {
        if (!myVisualGuides.getValue().equals(settings.getDefaultSoftMargins())) {
            return true;
        }

        if (myExcludedFilesList.isModified(settings)) {
            return true;
        }

        if (!Objects.equals(getSelectedLineSeparator(), settings.LINE_SEPARATOR)) {
            return true;
        }

        if (settings.WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN ^ myCbWrapWhenTypingReachesRightMargin.getValue()) {
            return true;
        }

        if (getRightMargin() != settings.getDefaultRightMargin()) {
            return true;
        }

        if (myEnableFormatterTags.getValue()) {
            if (!settings.FORMATTER_TAGS_ENABLED
                || settings.FORMATTER_TAGS_ACCEPT_REGEXP != myAcceptRegularExpressionsCheckBox.getValue()
                || !StringUtil.equals(myFormatterOffTagField.getValue(), settings.FORMATTER_OFF_TAG)
                || !StringUtil.equals(myFormatterOnTagField.getValue(), settings.FORMATTER_ON_TAG)) {
                return true;
            }
        }
        else if (settings.FORMATTER_TAGS_ENABLED) {
            return true;
        }

        for (GeneralCodeStyleOptionsProvider option : myAdditionalOptions) {
            if (option.isModified(settings)) {
                return true;
            }
        }

        return settings.AUTODETECT_INDENTS != myAutodetectIndentsBox.getValue();
    }

    @Override
    public JComponent getPanel() {
        return myPanel;
    }

    @Override
    @RequiredUIAccess
    protected void resetImpl(CodeStyleSettings settings) {
        myVisualGuides.setValue(settings.getDefaultSoftMargins());

        myExcludedFilesList.reset(settings);

        String lineSeparator = settings.LINE_SEPARATOR;
        myLineSeparatorCombo.setValue(LineBreak.SYSTEM_DEPENDENT);
        for (LineBreak lineBreak : LineBreak.values()) {
            if (Objects.equals(lineSeparator, lineBreak.getLineSeparator())) {
                myLineSeparatorCombo.setValue(lineBreak);
                break;
            }
        }

        myRightMarginField.setValue(settings.getDefaultRightMargin());
        myCbWrapWhenTypingReachesRightMargin.setValue(settings.WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN);

        myAcceptRegularExpressionsCheckBox.setValue(settings.FORMATTER_TAGS_ACCEPT_REGEXP);
        myEnableFormatterTags.setValue(settings.FORMATTER_TAGS_ENABLED);

        myFormatterOnTagField.setValue(settings.FORMATTER_ON_TAG);
        myFormatterOffTagField.setValue(settings.FORMATTER_OFF_TAG);

        setFormatterTagControlsEnabled(settings.FORMATTER_TAGS_ENABLED);

        myAutodetectIndentsBox.setValue(settings.AUTODETECT_INDENTS);

        for (GeneralCodeStyleOptionsProvider option : myAdditionalOptions) {
            option.reset(settings);
        }
    }

    @RequiredUIAccess
    private void setFormatterTagControlsEnabled(boolean isEnabled) {
        myFormatterOffTagField.setEnabled(isEnabled);
        myFormatterOnTagField.setEnabled(isEnabled);
        myAcceptRegularExpressionsCheckBox.setEnabled(isEnabled);
        myFormatterOffLabel.setEnabled(isEnabled);
        myFormatterOnLabel.setEnabled(isEnabled);
        myMarkerOptionsPanel.setEnabled(isEnabled);
    }

    @Override
    protected EditorHighlighter createHighlighter(EditorColorsScheme scheme) {
        //noinspection NullableProblems
        return EditorHighlighterFactory.getInstance().createEditorHighlighter(getFileType(), scheme, null);
    }

    @Override
    public Language getDefaultLanguage() {
        return null;
    }

    private static void showError(Component field, @Nonnull LocalizeValue message) {
        BalloonBuilder balloonBuilder =
            JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(message.get(), NotificationType.ERROR, null);
        balloonBuilder.setFadeoutTime(1500);
        Balloon balloon = balloonBuilder.createBalloon();
        Rectangle rect = field.getBounds();
        Point p = new Point(0, rect.height);
        RelativePoint point = new RelativePoint(field, p);
        balloon.show(point, Balloon.Position.below);
        Disposer.register(ProjectManager.getInstance().getDefaultProject(), balloon);
    }

    @Override
    public void setModel(@Nonnull CodeStyleSchemesModel model) {
        super.setModel(model);
        myExcludedFilesList.setSchemesModel(model);
    }
}
