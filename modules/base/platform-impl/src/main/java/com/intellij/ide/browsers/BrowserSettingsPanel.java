// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.browsers;

import com.intellij.ide.GeneralSettings;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.Function;
import com.intellij.util.PathUtil;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.LocalPathCellEditor;
import com.intellij.util.ui.table.IconTableCellRenderer;
import com.intellij.util.ui.table.TableModelEditor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ItemEvent;
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

  private JPanel root;

  private TextFieldWithBrowseButton alternativeBrowserPathField;

  @SuppressWarnings("UnusedDeclaration")
  private JComponent browsersTable;

  private ComboBox<DefaultBrowserPolicy> defaultBrowserPolicyComboBox;
  private JBCheckBox showBrowserHover;

  private TableModelEditor<ConfigurableWebBrowser> browsersEditor;

  private String customPathValue;

  BrowserSettingsPanel() {
    root = new JPanel(new BorderLayout());

    showBrowserHover = new JBCheckBox("Show browser popup in the editor");

    alternativeBrowserPathField = new TextFieldWithBrowseButton();
    alternativeBrowserPathField.addBrowseFolderListener(IdeBundle.message("title.select.path.to.browser"), null, null, APP_FILE_CHOOSER_DESCRIPTOR);

    JPanel bottomPanel = new JPanel(new VerticalFlowLayout());
    root.add(bottomPanel, BorderLayout.SOUTH);
    
    ArrayList<DefaultBrowserPolicy> defaultBrowserPolicies = new ArrayList<>();
    if (BrowserLauncherAppless.canUseSystemDefaultBrowserPolicy()) {
      defaultBrowserPolicies.add(DefaultBrowserPolicy.SYSTEM);
    }
    defaultBrowserPolicies.add(DefaultBrowserPolicy.FIRST);
    defaultBrowserPolicies.add(DefaultBrowserPolicy.ALTERNATIVE);

    defaultBrowserPolicyComboBox = new ComboBox<>();
    defaultBrowserPolicyComboBox.setModel(new CollectionComboBoxModel<>(defaultBrowserPolicies));
    defaultBrowserPolicyComboBox.addItemListener(e -> {
      boolean customPathEnabled = e.getItem() == DefaultBrowserPolicy.ALTERNATIVE;
      if (e.getStateChange() == ItemEvent.DESELECTED) {
        if (customPathEnabled) {
          customPathValue = alternativeBrowserPathField.getText();
        }
      }
      else if (e.getStateChange() == ItemEvent.SELECTED) {
        alternativeBrowserPathField.setEnabled(customPathEnabled);
        updateCustomPathTextFieldValue((DefaultBrowserPolicy)e.getItem());
      }
    });

    defaultBrowserPolicyComboBox.setRenderer(SimpleListCellRenderer.create("", value -> {
      String text = value == DefaultBrowserPolicy.SYSTEM ? "System default" : value == DefaultBrowserPolicy.FIRST ? "First listed" : value == DefaultBrowserPolicy.ALTERNATIVE ? "Custom path" : null;
      if (text == null) throw new IllegalStateException(String.valueOf(value));
      return text;
    }));

    TableModelEditor.DialogItemEditor<ConfigurableWebBrowser> itemEditor = new TableModelEditor.DialogItemEditor<ConfigurableWebBrowser>() {
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

    JPanel defaultBrowserPanel = new JPanel(new BorderLayout());
    bottomPanel.add(defaultBrowserPanel);

    defaultBrowserPanel.add(LabeledComponent.create(defaultBrowserPolicyComboBox, "Default Browser"), BorderLayout.WEST);
    defaultBrowserPanel.add(alternativeBrowserPathField, BorderLayout.CENTER);

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

  private void updateCustomPathTextFieldValue(@Nonnull DefaultBrowserPolicy browser) {
    if (browser == DefaultBrowserPolicy.ALTERNATIVE) {
      alternativeBrowserPathField.setText(customPathValue);
    }
    else if (browser == DefaultBrowserPolicy.FIRST) {
      setCustomPathToFirstListed();
    }
    else {
      alternativeBrowserPathField.setText("");
    }
  }

  private void setCustomPathToFirstListed() {
    ListTableModel<ConfigurableWebBrowser> model = browsersEditor.getModel();
    for (int i = 0, n = model.getRowCount(); i < n; i++) {
      ConfigurableWebBrowser browser = model.getRowValue(i);
      if (browser.isActive() && browser.getPath() != null) {
        alternativeBrowserPathField.setText(browser.getPath());
        return;
      }
    }

    alternativeBrowserPathField.setText("");
  }

  @Nonnull
  public JPanel getComponent() {
    return root;
  }

  public boolean isModified() {
    WebBrowserManager browserManager = WebBrowserManager.getInstance();
    GeneralSettings generalSettings = GeneralSettings.getInstance();

    DefaultBrowserPolicy defaultBrowserPolicy = getDefaultBrowser();
    if (getDefaultBrowserPolicy(browserManager) != defaultBrowserPolicy || browserManager.isShowBrowserHover() != showBrowserHover.isSelected()) {
      return true;
    }

    if (defaultBrowserPolicy == DefaultBrowserPolicy.ALTERNATIVE && !Comparing.strEqual(generalSettings.getBrowserPath(), alternativeBrowserPathField.getText())) {
      return true;
    }

    return browsersEditor.isModified();
  }

  public void apply() {
    GeneralSettings settings = GeneralSettings.getInstance();

    settings.setUseDefaultBrowser(getDefaultBrowser() == DefaultBrowserPolicy.SYSTEM);

    if (alternativeBrowserPathField.isEnabled()) {
      settings.setBrowserPath(alternativeBrowserPathField.getText());
    }

    WebBrowserManager browserManager = WebBrowserManager.getInstance();
    browserManager.setShowBrowserHover(showBrowserHover.isSelected());
    browserManager.defaultBrowserPolicy = getDefaultBrowser();
    browserManager.setList(browsersEditor.apply());
  }

  private DefaultBrowserPolicy getDefaultBrowser() {
    return (DefaultBrowserPolicy)defaultBrowserPolicyComboBox.getSelectedItem();
  }

  public void reset() {
    final WebBrowserManager browserManager = WebBrowserManager.getInstance();
    DefaultBrowserPolicy effectiveDefaultBrowserPolicy = getDefaultBrowserPolicy(browserManager);
    defaultBrowserPolicyComboBox.setSelectedItem(effectiveDefaultBrowserPolicy);

    GeneralSettings settings = GeneralSettings.getInstance();
    showBrowserHover.setSelected(browserManager.isShowBrowserHover());
    browsersEditor.reset(browserManager.getList());

    customPathValue = settings.getBrowserPath();
    alternativeBrowserPathField.setEnabled(effectiveDefaultBrowserPolicy == DefaultBrowserPolicy.ALTERNATIVE);
    updateCustomPathTextFieldValue(effectiveDefaultBrowserPolicy);
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