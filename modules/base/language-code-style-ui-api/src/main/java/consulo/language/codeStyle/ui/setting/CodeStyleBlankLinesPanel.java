/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.codeStyle.ui.setting;

import consulo.application.ApplicationBundle;
import consulo.configurable.ConfigurationException;
import consulo.ui.ex.awt.valueEditor.ValueValidationException;
import consulo.util.lang.Trinity;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.CustomCodeStyleSettings;
import consulo.language.codeStyle.setting.LanguageCodeStyleSettingsProvider;
import consulo.language.codeStyle.setting.CodeStyleSettingPresentation;
import consulo.ui.ex.awt.OptionGroup;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.IntegerField;
import consulo.util.collection.MultiMap;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;

public class CodeStyleBlankLinesPanel extends CustomizableLanguageCodeStylePanel {

  private static final Logger LOG = Logger.getInstance(CodeStyleBlankLinesPanel.class);

  private final List<IntOption> myOptions = new ArrayList<>();
  private final Set<String> myAllowedOptions = new HashSet<>();
  private boolean myAllOptionsAllowed = false;
  private boolean myIsFirstUpdate = true;
  private final Map<String, String> myRenamedFields = new HashMap<>();

  private final MultiMap<String, Trinity<Class<? extends CustomCodeStyleSettings>, String, String>> myCustomOptions = new MultiMap<>();

  private final JPanel myPanel = new JPanel(new GridBagLayout());

  public CodeStyleBlankLinesPanel(CodeStyleSettings settings) {
    super(settings);
    init();
  }

  @Override
  protected void init() {
    super.init();

    JPanel optionsPanel = new JPanel(new GridBagLayout());

    Map<CodeStyleSettingPresentation.SettingsGroup, List<CodeStyleSettingPresentation>> settings = CodeStyleSettingPresentation.getStandardSettings(getSettingsType());

    OptionGroup keepBlankLinesOptionsGroup = createOptionsGroup(BLANK_LINES_KEEP, settings.get(new CodeStyleSettingPresentation.SettingsGroup(BLANK_LINES_KEEP)));
    OptionGroup blankLinesOptionsGroup = createOptionsGroup(BLANK_LINES, settings.get(new CodeStyleSettingPresentation.SettingsGroup(BLANK_LINES)));
    if (keepBlankLinesOptionsGroup != null) {
      keepBlankLinesOptionsGroup.setAnchor(keepBlankLinesOptionsGroup.findAnchor());
      optionsPanel.add(keepBlankLinesOptionsGroup.createPanel(), new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, JBUI.emptyInsets(), 0, 0));
    }
    if (blankLinesOptionsGroup != null) {
      blankLinesOptionsGroup.setAnchor(blankLinesOptionsGroup.findAnchor());
      optionsPanel.add(blankLinesOptionsGroup.createPanel(), new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, JBUI.emptyInsets(), 0, 0));
    }
    UIUtil.mergeComponentsWithAnchor(keepBlankLinesOptionsGroup, blankLinesOptionsGroup);

    optionsPanel.add(new JPanel(), new GridBagConstraints(0, 2, 1, 1, 0, 1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0));

    optionsPanel.setBorder(JBUI.Borders.empty(0, 10));
    JScrollPane scroll = ScrollPaneFactory.createScrollPane(optionsPanel, true);
    scroll.setMinimumSize(new Dimension(optionsPanel.getPreferredSize().width + scroll.getVerticalScrollBar().getPreferredSize().width + 5, -1));
    scroll.setPreferredSize(scroll.getMinimumSize());

    myPanel.add(scroll, new GridBagConstraints(0, 0, 1, 1, 0, 1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0));

    final JPanel previewPanel = createPreviewPanel();
    myPanel.add(previewPanel, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0));

    installPreviewPanel(previewPanel);
    addPanelToWatch(myPanel);

    myIsFirstUpdate = false;
  }

  @Override
  public LanguageCodeStyleSettingsProvider.SettingsType getSettingsType() {
    return LanguageCodeStyleSettingsProvider.SettingsType.BLANK_LINES_SETTINGS;
  }

  @Nullable
  private OptionGroup createOptionsGroup(@Nonnull String groupName, @Nonnull List<CodeStyleSettingPresentation> settings) {
    OptionGroup optionGroup = new OptionGroup(groupName);

    for (CodeStyleSettingPresentation setting : settings) {
      createOption(optionGroup, setting.getUiName(), setting.getFieldName());
    }
    initCustomOptions(optionGroup, groupName);

    if (optionGroup.getComponents().length == 0) return null;

    return optionGroup;
  }

  private void initCustomOptions(OptionGroup optionGroup, String groupName) {
    for (Trinity<Class<? extends CustomCodeStyleSettings>, String, String> each : myCustomOptions.get(groupName)) {
      doCreateOption(optionGroup, each.third, new IntOption(each.third, each.first, each.second), each.second);
    }
  }

  private void createOption(OptionGroup optionGroup, String title, String fieldName) {
    if (myAllOptionsAllowed || myAllowedOptions.contains(fieldName)) {
      doCreateOption(optionGroup, title, new IntOption(title, fieldName), fieldName);
    }
  }

  private void doCreateOption(OptionGroup optionGroup, String title, IntOption option, String fieldName) {
    String renamed = myRenamedFields.get(fieldName);
    if (renamed != null) title = renamed;

    JBLabel l = new JBLabel(title);
    optionGroup.add(l, option.myIntField);
    myOptions.add(option);
  }

  @Override
  protected void resetImpl(final CodeStyleSettings settings) {
    for (IntOption option : myOptions) {
      option.setValue(option.getFieldValue(settings));
    }
  }

  @Override
  public void apply(CodeStyleSettings settings) throws ConfigurationException {
    for (IntOption option : myOptions) {
      try {
        option.myIntField.validateContent();
      }
      catch (ValueValidationException e) {
        throw new ConfigurationException(e.getMessage());
      }
    }
    for (IntOption option : myOptions) {
      option.setFieldValue(settings, option.getValue());
    }
  }

  @Override
  public boolean isModified(CodeStyleSettings settings) {
    for (IntOption option : myOptions) {
      if (option.getFieldValue(settings) != option.getValue()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public JComponent getPanel() {
    return myPanel;
  }

  @Override
  public void showAllStandardOptions() {
    myAllOptionsAllowed = true;
    for (IntOption option : myOptions) {
      option.myIntField.setEnabled(true);
    }
  }

  @Override
  public void showStandardOptions(String... optionNames) {
    if (myIsFirstUpdate) {
      Collections.addAll(myAllowedOptions, optionNames);
    }
    for (IntOption option : myOptions) {
      option.myIntField.setEnabled(false);
      for (String optionName : optionNames) {
        if (option.myTarget.getName().equals(optionName)) {
          option.myIntField.setEnabled(true);
          break;
        }
      }
    }
  }

  @Override
  public void showCustomOption(Class<? extends CustomCodeStyleSettings> settingsClass, String fieldName, String title, String groupName, Object... options) {
    showCustomOption(settingsClass, fieldName, title, groupName, null, null, options);
  }

  @Override
  public void showCustomOption(Class<? extends CustomCodeStyleSettings> settingsClass,
                               String fieldName,
                               String title,
                               String groupName,
                               @Nullable OptionAnchor anchor,
                               @Nullable String anchorFieldName,
                               Object... options) {
    if (myIsFirstUpdate) {
      myCustomOptions.putValue(groupName, Trinity.create(settingsClass, fieldName, title));
    }

    for (IntOption option : myOptions) {
      if (option.myTarget.getName().equals(fieldName)) {
        option.myIntField.setEnabled(true);
      }
    }
  }

  @Override
  public void renameStandardOption(String fieldName, String newTitle) {
    if (myIsFirstUpdate) {
      myRenamedFields.put(fieldName, newTitle);
    }
    for (IntOption option : myOptions) {
      option.myIntField.invalidate();
    }
  }

  private class IntOption {
    private final IntegerField myIntField;
    private final Field myTarget;
    private Class<? extends CustomCodeStyleSettings> myTargetClass;
    private int myCurrValue = Integer.MAX_VALUE;

    private IntOption(@Nonnull String title, String fieldName) {
      this(title, CommonCodeStyleSettings.class, fieldName, false);
    }

    private IntOption(@Nonnull String title, Class<? extends CustomCodeStyleSettings> targetClass, String fieldName) {
      this(title, targetClass, fieldName, false);
      myTargetClass = targetClass;
    }

    // dummy is used to distinguish constructors
    private IntOption(@Nonnull String title, Class<?> fieldClass, String fieldName, boolean dummy) {
      try {
        myTarget = fieldClass.getField(fieldName);
      }
      catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
      myIntField = new IntegerField(null, 0, 10);
      myIntField.setColumns(6);
      myIntField.setName(title);
      myIntField.setMinimumSize(new Dimension(30, myIntField.getMinimumSize().height));
    }

    private int getFieldValue(CodeStyleSettings settings) {
      try {
        if (myTargetClass != null) {
          return myTarget.getInt(settings.getCustomSettings(myTargetClass));
        }
        CommonCodeStyleSettings commonSettings = settings.getCommonSettings(getDefaultLanguage());
        return myTarget.getInt(commonSettings);
      }
      catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    public void setFieldValue(CodeStyleSettings settings, int value) {
      try {
        if (myTargetClass != null) {
          myTarget.setInt(settings.getCustomSettings(myTargetClass), value);
        }
        else {
          CommonCodeStyleSettings commonSettings = settings.getCommonSettings(getDefaultLanguage());
          myTarget.setInt(commonSettings, value);
        }
      }
      catch (IllegalAccessException e) {
        LOG.error(e);
      }
    }

    private int getValue() {
      try {
        myCurrValue = Integer.parseInt(myIntField.getText());
        if (myCurrValue < 0) {
          myCurrValue = 0;
        }
        if (myCurrValue > 10) {
          myCurrValue = 10;
        }
      }
      catch (NumberFormatException e) {
        //bad number entered
        myCurrValue = 0;
      }
      return myCurrValue;
    }

    public void setValue(int fieldValue) {
      if (fieldValue != myCurrValue) {
        myCurrValue = fieldValue;
        myIntField.setText(String.valueOf(fieldValue));
      }
    }
  }

  @Override
  protected String getTabTitle() {
    return ApplicationBundle.message("title.blank.lines");
  }
}
