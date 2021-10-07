// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.browsers;

import com.intellij.ide.GeneralSettings;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.util.Comparing;
import com.intellij.util.Function;
import com.intellij.util.PathUtil;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.LocalPathCellEditor;
import com.intellij.util.ui.table.IconTableCellRenderer;
import com.intellij.util.ui.table.TableModelEditor;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposable;
import consulo.ide.actions.webSearch.WebSearchEngine;
import consulo.ide.actions.webSearch.WebSearchOptions;
import consulo.ide.ui.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.UUID;

import static com.intellij.util.ui.table.TableModelEditor.EditableColumnInfo;

final class BrowserSettingsPanel {
  private static final FileChooserDescriptor APP_FILE_CHOOSER_DESCRIPTOR = FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor();

  private static final EditableColumnInfo<ConfigurableWebBrowser, String> PATH_COLUMN_INFO = new EditableColumnInfo<ConfigurableWebBrowser, String>("Path") {
    @Override
    public String valueOf(ConfigurableWebBrowser item) {
      return PathUtil.toSystemDependentName(item.getPath());
    }

    @Override
    public void setValue(ConfigurableWebBrowser item, String value) {
      item.setPath(value);
    }

    @Nullable
    @Override
    public TableCellEditor getEditor(ConfigurableWebBrowser item) {
      return new LocalPathCellEditor().fileChooserDescriptor(APP_FILE_CHOOSER_DESCRIPTOR).normalizePath(true);
    }
  };

  private static final EditableColumnInfo<ConfigurableWebBrowser, Boolean> ACTIVE_COLUMN_INFO = new EditableColumnInfo<ConfigurableWebBrowser, Boolean>() {
    @Override
    public Class getColumnClass() {
      return Boolean.class;
    }

    @Override
    public Boolean valueOf(ConfigurableWebBrowser item) {
      return item.isActive();
    }

    @Override
    public void setValue(ConfigurableWebBrowser item, Boolean value) {
      item.setActive(value);
    }
  };

  private static final ColumnInfo[] COLUMNS = {ACTIVE_COLUMN_INFO, new EditableColumnInfo<ConfigurableWebBrowser, String>("Name") {
    @Override
    public String valueOf(ConfigurableWebBrowser item) {
      return item.getName();
    }

    @Override
    public void setValue(ConfigurableWebBrowser item, String value) {
      item.setName(value);
    }
  }, new ColumnInfo<ConfigurableWebBrowser, BrowserFamily>("Family") {
    @Override
    public Class getColumnClass() {
      return BrowserFamily.class;
    }

    @Override
    public BrowserFamily valueOf(ConfigurableWebBrowser item) {
      return item.getFamily();
    }

    @Override
    public void setValue(ConfigurableWebBrowser item, BrowserFamily value) {
      item.setFamily(value);
      item.setSpecificSettings(value.createBrowserSpecificSettings());
    }

    @Nonnull
    @Override
    public TableCellRenderer getRenderer(ConfigurableWebBrowser item) {
      return IconTableCellRenderer.ICONABLE;
    }

    @Override
    public boolean isCellEditable(ConfigurableWebBrowser item) {
      return !WebBrowserManager.getInstance().isPredefinedBrowser(item);
    }
  }, PATH_COLUMN_INFO};
  
  private final Provider<WebSearchOptions> myWebSearchOptionsProvider;

  private JPanel root;

  private FileChooserTextBoxBuilder.Controller myAlternativeBrowserPathBox;

  @SuppressWarnings("UnusedDeclaration")
  private JComponent browsersTable;

  private ComboBox<DefaultBrowserPolicy> myDefaultBrowserPolicyComboBox;
  private CheckBox myShowBrowserPopupCheckBox;
  private ComboBox<WebSearchEngine> myWebSearchEngineComboBox;

  private TableModelEditor<ConfigurableWebBrowser> browsersEditor;

  private String customPathValue;

  @RequiredUIAccess
  BrowserSettingsPanel(Provider<WebSearchOptions> webSearchOptionsProvider, Disposable uiDisposable) {
    myWebSearchOptionsProvider = webSearchOptionsProvider;
    root = new JPanel(new BorderLayout());

    myShowBrowserPopupCheckBox = CheckBox.create(LocalizeValue.localizeTODO("Show browser popup in the editor"));

    myAlternativeBrowserPathBox =
            FileChooserTextBoxBuilder.create(null).fileChooserDescriptor(APP_FILE_CHOOSER_DESCRIPTOR).dialogTitle(IdeBundle.message("title.select.path.to.browser")).uiDisposable(uiDisposable).build();

    VerticalLayout bottomPanel = VerticalLayout.create();
    root.add(TargetAWT.to(bottomPanel), BorderLayout.SOUTH);

    ArrayList<DefaultBrowserPolicy> defaultBrowserPolicies = new ArrayList<>();
    if (BrowserLauncherAppless.canUseSystemDefaultBrowserPolicy()) {
      defaultBrowserPolicies.add(DefaultBrowserPolicy.SYSTEM);
    }
    defaultBrowserPolicies.add(DefaultBrowserPolicy.FIRST);
    defaultBrowserPolicies.add(DefaultBrowserPolicy.ALTERNATIVE);

    myDefaultBrowserPolicyComboBox = ComboBox.create(defaultBrowserPolicies);
    myDefaultBrowserPolicyComboBox.selectFirst();
    myDefaultBrowserPolicyComboBox.addValueListener(event -> {
      DefaultBrowserPolicy value = event.getValue();
      boolean customPathEnabled = value == DefaultBrowserPolicy.ALTERNATIVE;

      if(myAlternativeBrowserPathBox.getComponent().isEnabled()) {
        customPathValue = myAlternativeBrowserPathBox.getValue();
      }

      myAlternativeBrowserPathBox.getComponent().setEnabled(customPathEnabled);
      updateCustomPathTextFieldValue(value);
    });
    myDefaultBrowserPolicyComboBox.setTextRender(defaultBrowserPolicy -> {
      switch (defaultBrowserPolicy) {
        case SYSTEM:
          return LocalizeValue.localizeTODO("System default");
        case FIRST:
          return LocalizeValue.localizeTODO("First listed");
        case ALTERNATIVE:
          return LocalizeValue.localizeTODO("Custom path");
        default:
          throw new IllegalArgumentException(defaultBrowserPolicies.toString());
      }
    });

    TableModelEditor.DialogItemEditor<ConfigurableWebBrowser> itemEditor = new TableModelEditor.DialogItemEditor<>() {
      @Nonnull
      @Override
      public Class<ConfigurableWebBrowser> getItemClass() {
        return ConfigurableWebBrowser.class;
      }

      @Override
      public ConfigurableWebBrowser clone(@Nonnull ConfigurableWebBrowser item, boolean forInPlaceEditing) {
        return new ConfigurableWebBrowser(forInPlaceEditing ? item.getId() : UUID.randomUUID(), item.getFamily(), item.getName(), item.getPath(), item.isActive(),
                                          forInPlaceEditing ? item.getSpecificSettings() : cloneSettings(item));
      }

      @Override
      public void edit(@Nonnull ConfigurableWebBrowser browser, @Nonnull Function<ConfigurableWebBrowser, ConfigurableWebBrowser> mutator, boolean isAdd) {
        BrowserSpecificSettings settings = cloneSettings(browser);
        if (settings == null) {
          return;
        }

        ShowSettingsUtil.getInstance().editConfigurable(browsersTable, settings.createConfigurable()).doWhenDone(() -> {
          mutator.fun(browser).setSpecificSettings(settings);
        });
      }

      @Nullable
      private BrowserSpecificSettings cloneSettings(@Nonnull ConfigurableWebBrowser browser) {
        BrowserSpecificSettings settings = browser.getSpecificSettings();
        if (settings == null) {
          return null;
        }

        BrowserSpecificSettings newSettings = browser.getFamily().createBrowserSpecificSettings();
        assert newSettings != null;
        TableModelEditor.cloneUsingXmlSerialization(settings, newSettings);
        return newSettings;
      }

      @Override
      public void applyEdited(@Nonnull ConfigurableWebBrowser oldItem, @Nonnull ConfigurableWebBrowser newItem) {
        oldItem.setSpecificSettings(newItem.getSpecificSettings());
      }

      @Override
      public boolean isEditable(@Nonnull ConfigurableWebBrowser browser) {
        return browser.getSpecificSettings() != null;
      }

      @Override
      public boolean isRemovable(@Nonnull ConfigurableWebBrowser item) {
        return !WebBrowserManager.getInstance().isPredefinedBrowser(item);
      }
    };

    DockLayout defaultBrowserPanel = DockLayout.create();
    bottomPanel.add(defaultBrowserPanel);

    defaultBrowserPanel.left(LabeledBuilder.simple(LocalizeValue.localizeTODO("Default Browser:"), myDefaultBrowserPolicyComboBox));
    defaultBrowserPanel.center(myAlternativeBrowserPathBox.getComponent());

    ComboBox.Builder<WebSearchEngine> webSearchEngineBuilder = ComboBox.<WebSearchEngine>builder().fillByEnum(WebSearchEngine.class, WebSearchEngine::getPresentableName);
    bottomPanel.add(LabeledBuilder.sided(LocalizeValue.localizeTODO("Web Search Engine:"), myWebSearchEngineComboBox = webSearchEngineBuilder.build()));

    bottomPanel.add(myShowBrowserPopupCheckBox);

    browsersEditor = new TableModelEditor<>(COLUMNS, itemEditor, "No web browsers configured").modelListener(new TableModelEditor.DataChangedListener<ConfigurableWebBrowser>() {
      @Override
      public void tableChanged(@Nonnull TableModelEvent event) {
        update();
      }

      @Override
      public void dataChanged(@Nonnull ColumnInfo<ConfigurableWebBrowser, ?> columnInfo, int rowIndex) {
        if (columnInfo == PATH_COLUMN_INFO || columnInfo == ACTIVE_COLUMN_INFO) {
          update();
        }
      }

      private void update() {
        if (getDefaultBrowser() == DefaultBrowserPolicy.FIRST) {
          setCustomPathToFirstListed();
        }
      }
    });
    browsersTable = browsersEditor.createComponent();

    root.add(browsersTable, BorderLayout.CENTER);
  }

  @RequiredUIAccess
  private void updateCustomPathTextFieldValue(@Nonnull DefaultBrowserPolicy browser) {
    if (browser == DefaultBrowserPolicy.ALTERNATIVE) {
      myAlternativeBrowserPathBox.setValue(customPathValue);
    }
    else if (browser == DefaultBrowserPolicy.FIRST) {
      setCustomPathToFirstListed();
    }
    else {
      myAlternativeBrowserPathBox.setValue("");
    }
  }

  @RequiredUIAccess
  private void setCustomPathToFirstListed() {
    ListTableModel<ConfigurableWebBrowser> model = browsersEditor.getModel();
    for (int i = 0, n = model.getRowCount(); i < n; i++) {
      ConfigurableWebBrowser browser = model.getRowValue(i);
      if (browser.isActive() && browser.getPath() != null) {
        myAlternativeBrowserPathBox.setValue(browser.getPath());
        return;
      }
    }

    myAlternativeBrowserPathBox.setValue("");
  }

  @Nonnull
  public JPanel getComponent() {
    return root;
  }

  public boolean isModified() {
    WebBrowserManager browserManager = WebBrowserManager.getInstance();
    GeneralSettings generalSettings = GeneralSettings.getInstance();

    DefaultBrowserPolicy defaultBrowserPolicy = getDefaultBrowser();
    if (getDefaultBrowserPolicy(browserManager) != defaultBrowserPolicy || browserManager.isShowBrowserHover() != myShowBrowserPopupCheckBox.getValueOrError()) {
      return true;
    }

    if (defaultBrowserPolicy == DefaultBrowserPolicy.ALTERNATIVE && !Comparing.strEqual(generalSettings.getBrowserPath(), myAlternativeBrowserPathBox.getValue())) {
      return true;
    }

    WebSearchOptions webSearchOptions = myWebSearchOptionsProvider.get();

    if(webSearchOptions.getEngine() != myWebSearchEngineComboBox.getValue()) {
      return true;
    }

    return browsersEditor.isModified();
  }

  @RequiredUIAccess
  public void apply() {
    GeneralSettings settings = GeneralSettings.getInstance();

    settings.setUseDefaultBrowser(getDefaultBrowser() == DefaultBrowserPolicy.SYSTEM);

    if (myAlternativeBrowserPathBox.getComponent().isEnabled()) {
      settings.setBrowserPath(myAlternativeBrowserPathBox.getValue());
    }

    WebBrowserManager browserManager = WebBrowserManager.getInstance();
    browserManager.setShowBrowserHover(myShowBrowserPopupCheckBox.getValueOrError());
    browserManager.defaultBrowserPolicy = getDefaultBrowser();
    browserManager.setList(browsersEditor.apply());

    WebSearchOptions webSearchOptions = myWebSearchOptionsProvider.get();
    webSearchOptions.setEngine(myWebSearchEngineComboBox.getValueOrError());
  }

  private DefaultBrowserPolicy getDefaultBrowser() {
    return myDefaultBrowserPolicyComboBox.getValueOrError();
  }

  @RequiredUIAccess
  public void reset() {
    final WebBrowserManager browserManager = WebBrowserManager.getInstance();
    DefaultBrowserPolicy effectiveDefaultBrowserPolicy = getDefaultBrowserPolicy(browserManager);
    myDefaultBrowserPolicyComboBox.setValue(effectiveDefaultBrowserPolicy);

    GeneralSettings settings = GeneralSettings.getInstance();
    myShowBrowserPopupCheckBox.setValue(browserManager.isShowBrowserHover());
    browsersEditor.reset(browserManager.getList());

    customPathValue = settings.getBrowserPath();
    myAlternativeBrowserPathBox.getComponent().setEnabled(effectiveDefaultBrowserPolicy == DefaultBrowserPolicy.ALTERNATIVE);
    updateCustomPathTextFieldValue(effectiveDefaultBrowserPolicy);

    WebSearchOptions webSearchOptions = myWebSearchOptionsProvider.get();
    myWebSearchEngineComboBox.setValue(webSearchOptions.getEngine());
  }

  private static DefaultBrowserPolicy getDefaultBrowserPolicy(WebBrowserManager manager) {
    DefaultBrowserPolicy policy = manager.getDefaultBrowserPolicy();
    if (policy != DefaultBrowserPolicy.SYSTEM || BrowserLauncherAppless.canUseSystemDefaultBrowserPolicy()) {
      return policy;
    }
    // if system default browser policy cannot be used
    return DefaultBrowserPolicy.ALTERNATIVE;
  }

  public void selectBrowser(@Nonnull WebBrowser browser) {
    if (browser instanceof ConfigurableWebBrowser) {
      browsersEditor.selectItem((ConfigurableWebBrowser)browser);
    }
  }
}