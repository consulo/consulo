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
package consulo.language.codeStyle.ui.setting;

import consulo.application.Application;
import consulo.application.localize.ApplicationLocalize;
import consulo.codeEditor.EditorHighlighter;
import consulo.colorScheme.EditorColorsScheme;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposer;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.PredefinedCodeStyle;
import consulo.language.codeStyle.setting.CodeStyleSettingsProvider;
import consulo.language.codeStyle.setting.IndentOptionsEditor;
import consulo.language.codeStyle.setting.LanguageCodeStyleSettingsProvider;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.language.plain.PlainTextFileType;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.util.ProjectUIUtil;
import consulo.ui.ex.awt.JBMenuItem;
import consulo.ui.ex.awt.JBPopupMenu;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.TabbedPaneWrapper;
import consulo.ui.ex.awt.util.GraphicsUtil;
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
import java.util.*;

/**
 * @author Rustam Vishnyakov
 */

public abstract class TabbedLanguageCodeStylePanel extends CodeStyleAbstractPanel {
  private CodeStyleAbstractPanel myActiveTab;
  private List<CodeStyleAbstractPanel> myTabs;
  private JPanel myPanel;
  private TabbedPaneWrapper myTabbedPane;
  private final PredefinedCodeStyle[] myPredefinedCodeStyles;
  private JPopupMenu myCopyFromMenu;
  private
  @Nullable
  TabChangeListener myListener;

  protected TabbedLanguageCodeStylePanel(@Nullable Language language, CodeStyleSettings currentSettings, CodeStyleSettings settings) {
    super(language, currentSettings, settings);
    myPredefinedCodeStyles = getPredefinedStyles();
  }

  /**
   * Initializes all standard tabs: "Tabs and Indents", "Spaces", "Blank Lines" and "Wrapping and Braces" if relevant.
   * For "Tabs and Indents" LanguageCodeStyleSettingsProvider must instantiate its own indent options, for other standard tabs it
   * must return false in usesSharedPreview() method. You can override this method to add your own tabs by calling super.initTabs() and
   * then addTab() methods or selectively add needed tabs with your own implementation.
   *
   * @param settings Code style settings to be used with initialized panels.
   * @see LanguageCodeStyleSettingsProvider
   * @see #addIndentOptionsTab(CodeStyleSettings)
   * @see #addSpacesTab(CodeStyleSettings)
   * @see #addBlankLinesTab(CodeStyleSettings)
   * @see #addWrappingAndBracesTab(CodeStyleSettings)
   */
  protected void initTabs(CodeStyleSettings settings) {
    LanguageCodeStyleSettingsProvider provider = LanguageCodeStyleSettingsProvider.forLanguage(getDefaultLanguage());
    addIndentOptionsTab(settings);
    if (provider != null) {
      addSpacesTab(settings);
      addWrappingAndBracesTab(settings);
      addBlankLinesTab(settings);
    }
  }

  /**
   * Adds "Tabs and Indents" tab if the language has its own LanguageCodeStyleSettings provider and instantiates indent options in
   * getDefaultSettings() method.
   *
   * @param settings CodeStyleSettings to be used with "Tabs and Indents" panel.
   */
  protected void addIndentOptionsTab(CodeStyleSettings settings) {
    LanguageCodeStyleSettingsProvider provider = LanguageCodeStyleSettingsProvider.forLanguage(getDefaultLanguage());
    if (provider != null) {
      IndentOptionsEditor indentOptionsEditor = provider.getIndentOptionsEditor();
      if (indentOptionsEditor != null) {
        MyIndentOptionsWrapper indentOptionsWrapper = new MyIndentOptionsWrapper(settings, provider, indentOptionsEditor);
        addTab(indentOptionsWrapper);
      }
    }
  }

  protected void addSpacesTab(CodeStyleSettings settings) {
    addTab(new MySpacesPanel(settings));
  }

  protected void addBlankLinesTab(CodeStyleSettings settings) {
    addTab(new MyBlankLinesPanel(settings));
  }

  protected void addWrappingAndBracesTab(CodeStyleSettings settings) {
    addTab(new MyWrappingAndBracesPanel(settings));
  }

  protected void ensureTabs() {
    if (myTabs == null) {
      myPanel = new JPanel();
      myPanel.setLayout(new BorderLayout());
      myTabbedPane = new TabbedPaneWrapper(this);
      myTabbedPane.addChangeListener(new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
          if (myListener != null) {
            String title = myTabbedPane.getSelectedTitle();
            if (title != null) {
              myListener.tabChanged(TabbedLanguageCodeStylePanel.this, title);
            }
          }
        }
      });
      myTabs = new ArrayList<>();
      myPanel.add(myTabbedPane.getComponent());
      initTabs(getSettings());
    }
    assert !myTabs.isEmpty();
  }

  public void showSetFrom(Component component) {
    initCopyFromMenu();
    myCopyFromMenu.show(component, 0, component.getHeight());
  }

  private void initCopyFromMenu() {
    if (myCopyFromMenu == null) {
      myCopyFromMenu = new JBPopupMenu();
      setupCopyFromMenu(myCopyFromMenu);
    }
  }

  /**
   * Adds a tab with the given CodeStyleAbstractPanel. Tab title is taken from getTabTitle() method.
   *
   * @param tab The panel to use in a tab.
   */
  protected final void addTab(CodeStyleAbstractPanel tab) {
    myTabs.add(tab);
    tab.setShouldUpdatePreview(true);
    addPanelToWatch(tab.getPanel());
    myTabbedPane.addTab(tab.getTabTitle().get(), tab.getPanel());
    if (myActiveTab == null) {
      myActiveTab = tab;
    }
  }

  private void addTab(Configurable configurable) {
    ConfigurableWrapper wrapper = new ConfigurableWrapper(configurable, getSettings());
    addTab(wrapper);
  }

  /**
   * Creates and adds a tab from CodeStyleSettingsProvider. The provider may return false in hasSettingsPage() method in order not to be
   * shown at top level of code style settings.
   *
   * @param provider The provider used to create a settings page.
   */
  protected final void createTab(CodeStyleSettingsProvider provider) {
    if (provider.hasSettingsPage()) return;
    Configurable configurable = provider.createSettingsPage(getCurrentSettings(), getSettings());
    addTab(configurable);
  }

  @Override
  public final void setModel(@Nonnull CodeStyleSchemesModel model) {
    super.setModel(model);
    ensureTabs();
    for (CodeStyleAbstractPanel tab : myTabs) {
      tab.setModel(model);
    }
  }

  @Override
  protected int getRightMargin() {
    ensureTabs();
    return myActiveTab.getRightMargin();
  }

  @Override
  protected EditorHighlighter createHighlighter(EditorColorsScheme scheme) {
    ensureTabs();
    return myActiveTab.createHighlighter(scheme);
  }

  @Nonnull
  @Override
  protected FileType getFileType() {
    ensureTabs();
    return myActiveTab.getFileType();
  }

  @Override
  protected String getPreviewText() {
    ensureTabs();
    return myActiveTab.getPreviewText();
  }

  @Override
  protected void updatePreview(boolean useDefaultSample) {
    ensureTabs();
    for (CodeStyleAbstractPanel tab : myTabs) {
      tab.updatePreview(useDefaultSample);
    }
  }

  @Override
  public void onSomethingChanged() {
    ensureTabs();
    for (CodeStyleAbstractPanel tab : myTabs) {
      tab.setShouldUpdatePreview(true);
      tab.onSomethingChanged();
    }
  }

  @Override
  public void apply(CodeStyleSettings settings) throws ConfigurationException {
    ensureTabs();
    for (CodeStyleAbstractPanel tab : myTabs) {
      tab.apply(settings);
    }
  }

  @Override
  public void dispose() {
    for (CodeStyleAbstractPanel tab : myTabs) {
      Disposer.dispose(tab);
    }
    super.dispose();
  }

  @Override
  public boolean isModified(CodeStyleSettings settings) {
    ensureTabs();
    for (CodeStyleAbstractPanel tab : myTabs) {
      if (tab.isModified(settings)) {
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
  protected void resetImpl(CodeStyleSettings settings) {
    ensureTabs();
    for (CodeStyleAbstractPanel tab : myTabs) {
      tab.resetImpl(settings);
    }
  }


  @Override
  public void setupCopyFromMenu(JPopupMenu copyMenu) {
    super.setupCopyFromMenu(copyMenu);
    if (myPredefinedCodeStyles.length > 0) {
      JMenu langs = new JMenu("Language") {
        @Override
        public void paint(Graphics g) {
          GraphicsUtil.setupAntialiasing(g);
          super.paint(g);
        }
      }; //TODO<rv>: Move to resource bundle
      copyMenu.add(langs);
      fillLanguages(langs);
      JMenu predefined = new JMenu("Predefined Style") {
        @Override
        public void paint(Graphics g) {
          GraphicsUtil.setupAntialiasing(g);
          super.paint(g);
        }
      }; //TODO<rv>: Move to resource bundle
      copyMenu.add(predefined);
      fillPredefined(predefined);
    }
    else {
      fillLanguages(copyMenu);
    }
  }


  private void fillLanguages(JComponent parentMenu) {
    Language[] languages = LanguageCodeStyleSettingsProvider.getLanguagesWithCodeStyleSettings();
    @SuppressWarnings("UnnecessaryFullyQualifiedName") java.util.List<JMenuItem> langItems = new ArrayList<>();
    for (Language lang : languages) {
      if (!lang.equals(getDefaultLanguage())) {
        LocalizeValue langName = LanguageCodeStyleSettingsProvider.getLanguageName(lang);
        JMenuItem langItem = new JBMenuItem(langName.get());
        langItem.addActionListener(e -> applyLanguageSettings(lang));
        langItems.add(langItem);
      }
    }
    Collections.sort(langItems, (item1, item2) -> item1.getText().compareToIgnoreCase(item2.getText()));
    for (JMenuItem langItem : langItems) {
      parentMenu.add(langItem);
    }
  }

  private void fillPredefined(JMenuItem parentMenu) {
    for (final PredefinedCodeStyle predefinedCodeStyle : myPredefinedCodeStyles) {
      JMenuItem predefinedItem = new JBMenuItem(predefinedCodeStyle.getName());
      parentMenu.add(predefinedItem);
      predefinedItem.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          applyPredefinedStyle(predefinedCodeStyle.getName());
        }
      });
    }
  }

  private PredefinedCodeStyle[] getPredefinedStyles() {
    Language language = getDefaultLanguage();
    List<PredefinedCodeStyle> result = new ArrayList<>();

    for (PredefinedCodeStyle codeStyle : Application.get().getExtensionList(PredefinedCodeStyle.class)) {
      if (codeStyle.getLanguage().equals(language)) {
        result.add(codeStyle);
      }
    }
    return result.toArray(PredefinedCodeStyle.EMPTY_ARRAY);
  }


  private void applyLanguageSettings(Language lang) {
    Project currProject = ProjectUIUtil.guessCurrentProject(getPanel());
    CodeStyleSettings rootSettings = CodeStyle.getSettings(currProject);
    CodeStyleSettings targetSettings = getSettings();

    applyLanguageSettings(lang, rootSettings, targetSettings);
    reset(targetSettings);
    onSomethingChanged();
  }

  protected void applyLanguageSettings(Language lang, CodeStyleSettings rootSettings, CodeStyleSettings targetSettings) {
    CommonCodeStyleSettings sourceCommonSettings = rootSettings.getCommonSettings(lang);
    CommonCodeStyleSettings targetCommonSettings = targetSettings.getCommonSettings(getDefaultLanguage());
    targetCommonSettings.copyFrom(sourceCommonSettings);
  }

  private void applyPredefinedStyle(String styleName) {
    for (PredefinedCodeStyle style : myPredefinedCodeStyles) {
      if (style.getName().equals(styleName)) {
        applyPredefinedSettings(style);
      }
    }
  }

//========================================================================================================================================

  protected class MySpacesPanel extends CodeStyleSpacesPanel {

    public MySpacesPanel(CodeStyleSettings settings) {
      super(settings);
    }

    @Override
    protected boolean shouldHideOptions() {
      return true;
    }

    @Override
    public Language getDefaultLanguage() {
      return TabbedLanguageCodeStylePanel.this.getDefaultLanguage();
    }
  }

  protected class MyBlankLinesPanel extends CodeStyleBlankLinesPanel {

    public MyBlankLinesPanel(CodeStyleSettings settings) {
      super(settings);
    }

    @Override
    public Language getDefaultLanguage() {
      return TabbedLanguageCodeStylePanel.this.getDefaultLanguage();
    }
  }

  protected class MyWrappingAndBracesPanel extends WrappingAndBracesPanel {

    public MyWrappingAndBracesPanel(CodeStyleSettings settings) {
      super(settings);
    }

    @Override
    public Language getDefaultLanguage() {
      return TabbedLanguageCodeStylePanel.this.getDefaultLanguage();
    }
  }


  //========================================================================================================================================

  private class ConfigurableWrapper extends CodeStyleAbstractPanel {

    private final Configurable myConfigurable;
    private JComponent myComponent;

    public ConfigurableWrapper(@Nonnull Configurable configurable, CodeStyleSettings settings) {
      super(settings);
      myConfigurable = configurable;

      Disposer.register(this, () -> myConfigurable.disposeUIResources());
    }

    @Override
    protected int getRightMargin() {
      return 0;
    }

    @Nullable
    @Override
    protected EditorHighlighter createHighlighter(EditorColorsScheme scheme) {
      return null;
    }

    @SuppressWarnings("ConstantConditions")
    @Nonnull
    @Override
    protected FileType getFileType() {
      Language language = getDefaultLanguage();
      return language != null ? language.getAssociatedFileType() : PlainTextFileType.INSTANCE;
    }

    @Override
    public Language getDefaultLanguage() {
      return TabbedLanguageCodeStylePanel.this.getDefaultLanguage();
    }

    @Override
    protected LocalizeValue getTabTitle() {
      return myConfigurable.getDisplayName();
    }

    @Override
    protected String getPreviewText() {
      return null;
    }

    @Override
    public void apply(CodeStyleSettings settings) throws ConfigurationException {
      myConfigurable.apply();
    }

    @Override
    public boolean isModified(CodeStyleSettings settings) {
      return myConfigurable.isModified();
    }

    @Nullable
    @Override
    public JComponent getPanel() {
      if (myComponent == null) {
        myComponent = myConfigurable.createComponent();
      }
      return myComponent;
    }

    @Override
    protected void resetImpl(CodeStyleSettings settings) {
      if (myConfigurable instanceof CodeStyleAbstractConfigurable) {
        // when a predefined style is chosen and the configurable is wrapped in a tab,
        // we apply it to CLONED code style settings and then pass them to this method to reset,
        // usual reset() won't work in such case
        ((CodeStyleAbstractConfigurable)myConfigurable).reset(settings);
      }
      else {
        // todo: support   for other configurables
        myConfigurable.reset();
      }
    }
  }

  @Override
  public Set<String> processListOptions() {
    Set<String> result = new HashSet<>();
    for (CodeStyleAbstractPanel tab : myTabs) {
      result.addAll(tab.processListOptions());
    }
    return result;
  }

  //========================================================================================================================================

  protected class MyIndentOptionsWrapper extends CodeStyleAbstractPanel {

    private final IndentOptionsEditor myEditor;
    private final LanguageCodeStyleSettingsProvider myProvider;
    private final JPanel myTopPanel = new JPanel(new BorderLayout());
    private final JPanel myLeftPanel = new JPanel(new BorderLayout());
    private final JPanel myRightPanel;

    protected MyIndentOptionsWrapper(CodeStyleSettings settings, LanguageCodeStyleSettingsProvider provider, IndentOptionsEditor editor) {
      super(settings);
      myProvider = provider;
      myTopPanel.add(myLeftPanel, BorderLayout.WEST);
      myRightPanel = new JPanel();
      installPreviewPanel(myRightPanel);
      myEditor = editor;
      if (myEditor != null) {
        JPanel panel = myEditor.createPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane scroll = ScrollPaneFactory.createScrollPane(panel, true);
        scroll.setPreferredSize(new Dimension(panel.getPreferredSize().width + scroll.getVerticalScrollBar().getPreferredSize().width + 5, -1));
        myLeftPanel.add(scroll, BorderLayout.CENTER);
      }
      myTopPanel.add(myRightPanel, BorderLayout.CENTER);
    }

    @Override
    protected int getRightMargin() {
      return myProvider.getRightMargin(LanguageCodeStyleSettingsProvider.SettingsType.INDENT_SETTINGS);
    }

    @Override
    protected EditorHighlighter createHighlighter(EditorColorsScheme scheme) {
      //noinspection NullableProblems
      return EditorHighlighterFactory.getInstance().createEditorHighlighter(getFileType(), scheme, null);
    }

    @SuppressWarnings("ConstantConditions")
    @Nonnull
    @Override
    protected FileType getFileType() {
      Language language = TabbedLanguageCodeStylePanel.this.getDefaultLanguage();
      return language != null ? language.getAssociatedFileType() : PlainTextFileType.INSTANCE;
    }

    @Override
    protected String getPreviewText() {
      return myProvider != null ? myProvider.getCodeSample(LanguageCodeStyleSettingsProvider.SettingsType.INDENT_SETTINGS) : "Loading...";
    }

    @Override
    protected String getFileExt() {
      if (myProvider != null) {
        String ext = myProvider.getFileExt();
        if (ext != null) return ext;
      }
      return super.getFileExt();
    }

    @Override
    public void apply(CodeStyleSettings settings) {
      CommonCodeStyleSettings.IndentOptions indentOptions = getIndentOptions(settings);
      if (indentOptions == null) return;
      myEditor.apply(settings, indentOptions);
    }

    @Override
    public boolean isModified(CodeStyleSettings settings) {
      CommonCodeStyleSettings.IndentOptions indentOptions = getIndentOptions(settings);
      if (indentOptions == null) return false;
      return myEditor.isModified(settings, indentOptions);
    }

    @Override
    public JComponent getPanel() {
      return myTopPanel;
    }

    @Override
    protected void resetImpl(CodeStyleSettings settings) {
      CommonCodeStyleSettings.IndentOptions indentOptions = getIndentOptions(settings);
      if (indentOptions == null) {
        myEditor.setEnabled(false);
        indentOptions = settings.getIndentOptions(myProvider.getLanguage().getAssociatedFileType());
      }
      myEditor.reset(settings, indentOptions);
    }

    @Nullable
    private CommonCodeStyleSettings.IndentOptions getIndentOptions(CodeStyleSettings settings) {
      return settings.getCommonSettings(getDefaultLanguage()).getIndentOptions();
    }

    @Override
    public Language getDefaultLanguage() {
      return TabbedLanguageCodeStylePanel.this.getDefaultLanguage();
    }

    @Nonnull
    @Override
    protected LocalizeValue getTabTitle() {
      return ApplicationLocalize.titleTabsAndIndents();
    }

    @Override
    public void onSomethingChanged() {
      super.onSomethingChanged();
      myEditor.setEnabled(true);
    }

  }

  public interface TabChangeListener {
    void tabChanged(@Nonnull TabbedLanguageCodeStylePanel source, @Nonnull String tabTitle);
  }

  public void setListener(@Nullable TabChangeListener listener) {
    myListener = listener;
  }

  public void changeTab(@Nonnull String tabTitle) {
    myTabbedPane.setSelectedTitle(tabTitle);
  }
}
