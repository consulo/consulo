// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.webBrowser.impl.internal;

import consulo.application.Application;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.FileChooserTextBoxBuilder;
import consulo.ui.ex.awt.ColumnInfo;
import consulo.ui.ex.awt.table.IconTableCellRenderer;
import consulo.ui.ex.awt.table.ListTableModel;
import consulo.ui.ex.awt.table.LocalPathCellEditor;
import consulo.ui.ex.awt.table.TableModelEditor;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;
import consulo.util.io.PathUtil;
import consulo.util.lang.Comparing;
import consulo.webBrowser.*;
import consulo.webBrowser.localize.WebBrowserLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Provider;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Function;

import static consulo.ui.ex.awt.table.TableModelEditor.EditableColumnInfo;

final class BrowserSettingsPanel {
    private static final FileChooserDescriptor APP_FILE_CHOOSER_DESCRIPTOR =
        FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor();

    private static final EditableColumnInfo<ConfigurableWebBrowser, String> PATH_COLUMN_INFO = new EditableColumnInfo<>("Path") {
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

    private static final EditableColumnInfo<ConfigurableWebBrowser, Boolean> ACTIVE_COLUMN_INFO = new EditableColumnInfo<>() {
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

    private static final ColumnInfo[] COLUMNS = {
        ACTIVE_COLUMN_INFO,
        new EditableColumnInfo<ConfigurableWebBrowser, String>("Name") {
            @Override
            public String valueOf(ConfigurableWebBrowser item) {
                return item.getName();
            }

            @Override
            public void setValue(ConfigurableWebBrowser item, String value) {
                item.setName(value);
            }
        },
        new ColumnInfo<ConfigurableWebBrowser, BrowserFamily>("Family") {
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
                return !WebBrowserManagerImpl.getInstance().isPredefinedBrowser(item);
            }
        },
        PATH_COLUMN_INFO
    };

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

        myAlternativeBrowserPathBox = FileChooserTextBoxBuilder.create(null)
            .fileChooserDescriptor(APP_FILE_CHOOSER_DESCRIPTOR)
            .dialogTitle(WebBrowserLocalize.titleSelectPathToBrowser())
            .uiDisposable(uiDisposable)
            .build();

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

            if (myAlternativeBrowserPathBox.getComponent().isEnabled()) {
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
                return new ConfigurableWebBrowser(
                    forInPlaceEditing ? item.getId() : UUID.randomUUID(),
                    item.getFamily(),
                    item.getName(),
                    item.getPath(),
                    item.isActive(),
                    forInPlaceEditing ? item.getSpecificSettings() : cloneSettings(item)
                );
            }

            @Override
            @RequiredUIAccess
            public void edit(
                @Nonnull ConfigurableWebBrowser browser,
                @Nonnull Function<ConfigurableWebBrowser, ConfigurableWebBrowser> mutator,
                boolean isAdd
            ) {
                BrowserSpecificSettings settings = cloneSettings(browser);
                if (settings == null) {
                    return;
                }

                ShowConfigurableService service = Application.get().getInstance(ShowConfigurableService.class);
                service.editConfigurable(browsersTable, settings.createConfigurable())
                    .doWhenDone(() -> mutator.apply(browser).setSpecificSettings(settings));
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
                return !WebBrowserManagerImpl.getInstance().isPredefinedBrowser(item);
            }
        };

        DockLayout defaultBrowserPanel = DockLayout.create();
        bottomPanel.add(defaultBrowserPanel);

        defaultBrowserPanel.left(LabeledBuilder.simple(LocalizeValue.localizeTODO("Default Browser:"), myDefaultBrowserPolicyComboBox));
        defaultBrowserPanel.center(myAlternativeBrowserPathBox.getComponent());

        ComboBox.Builder<WebSearchEngine> webSearchEngineBuilder =
            ComboBox.<WebSearchEngine>builder().fillByEnum(WebSearchEngine.class, WebSearchEngine::getPresentableName);
        bottomPanel.add(LabeledBuilder.sided(
            LocalizeValue.localizeTODO("Web Search Engine:"),
            myWebSearchEngineComboBox = webSearchEngineBuilder.build()
        ));

        bottomPanel.add(myShowBrowserPopupCheckBox);

        browsersEditor = new TableModelEditor<>(COLUMNS, itemEditor, "No web browsers configured", ConfigurableWebBrowser::new)
            .modelListener(new TableModelEditor.DataChangedListener<>() {
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

        DefaultBrowserPolicy defaultBrowserPolicy = getDefaultBrowser();
        if (getDefaultBrowserPolicy(browserManager) != defaultBrowserPolicy
            || browserManager.isShowBrowserHover() != myShowBrowserPopupCheckBox.getValueOrError()) {
            return true;
        }

        if (defaultBrowserPolicy == DefaultBrowserPolicy.ALTERNATIVE
            && !Comparing.strEqual(browserManager.getAlternativeBrowserPath(), myAlternativeBrowserPathBox.getValue())) {
            return true;
        }

        WebSearchOptions webSearchOptions = myWebSearchOptionsProvider.get();

        return webSearchOptions.getEngine() != myWebSearchEngineComboBox.getValue() || browsersEditor.isModified();
    }

    @RequiredUIAccess
    public void apply() {
        WebBrowserManagerImpl browserManager = WebBrowserManagerImpl.getInstance();

        browserManager.setUseDefaultBrowser(getDefaultBrowser() == DefaultBrowserPolicy.SYSTEM);

        if (myAlternativeBrowserPathBox.getComponent().isEnabled()) {
            browserManager.setBrowserPath(myAlternativeBrowserPathBox.getValue());
        }

        browserManager.setShowBrowserHover(myShowBrowserPopupCheckBox.getValueOrError());
        browserManager.setDefaultBrowserPolicy(getDefaultBrowser());
        browserManager.setList(browsersEditor.apply());

        WebSearchOptions webSearchOptions = myWebSearchOptionsProvider.get();
        webSearchOptions.setEngine(myWebSearchEngineComboBox.getValueOrError());
    }

    private DefaultBrowserPolicy getDefaultBrowser() {
        return myDefaultBrowserPolicyComboBox.getValueOrError();
    }

    @RequiredUIAccess
    public void reset() {
        final WebBrowserManagerImpl browserManager = WebBrowserManagerImpl.getInstance();
        DefaultBrowserPolicy effectiveDefaultBrowserPolicy = getDefaultBrowserPolicy(browserManager);
        myDefaultBrowserPolicyComboBox.setValue(effectiveDefaultBrowserPolicy);

        myShowBrowserPopupCheckBox.setValue(browserManager.isShowBrowserHover());
        browsersEditor.reset(browserManager.getList());

        customPathValue = browserManager.getAlternativeBrowserPath();
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
        if (browser instanceof ConfigurableWebBrowser configurableWebBrowser) {
            browsersEditor.selectItem(configurableWebBrowser);
        }
    }
}