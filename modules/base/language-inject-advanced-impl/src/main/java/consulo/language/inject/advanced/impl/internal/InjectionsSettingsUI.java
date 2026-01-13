/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.language.inject.advanced.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.Configurable;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserFactory;
import consulo.fileChooser.FileSaverDescriptor;
import consulo.fileChooser.IdeaFileChooser;
import consulo.fileEditor.util.FileContentUtil;
import consulo.language.Language;
import consulo.language.editor.LangDataKeys;
import consulo.language.inject.advanced.*;
import consulo.language.inject.advanced.internal.ProjectInjectionConfiguration;
import consulo.language.plain.PlainTextFileType;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleColoredText;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.speedSearch.TableViewSpeedSearch;
import consulo.ui.ex.awt.table.ListTableModel;
import consulo.ui.ex.awt.table.TableView;
import consulo.ui.ex.awt.util.TableUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Sets;
import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWrapper;
import consulo.virtualFileSystem.archive.ArchiveFileType;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.jdom.Document;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Gregory.Shrago
 */
@ExtensionImpl
public class InjectionsSettingsUI implements SearchableConfigurable.Parent, Configurable.NoScroll, ProjectConfigurable {
    private static final Logger LOG = Logger.getInstance(InjectionsSettingsUI.class);

    private final Project myProject;
    private final CfgInfo[] myInfos;

    private JPanel myRoot;
    private InjectionsTable myInjectionsTable;
    private final Map<String, LanguageInjectionSupport> mySupports = new HashMap<>();
    private final Map<String, AnAction> myEditActions = new HashMap<>();
    private final List<AnAction> myAddActions = new ArrayList<>();
    private ActionToolbar myToolbar;
    private JLabel myCountLabel;

    private Configurable[] myConfigurables;
    private Configuration myConfiguration;

    @Inject
    public InjectionsSettingsUI(Project project) {
        this(project, Configuration.getProjectInstance(project));
    }

    protected InjectionsSettingsUI(Project project, Configuration configuration) {
        myProject = project;
        myConfiguration = configuration;

        CfgInfo currentInfo = new CfgInfo(configuration, "project");
        myInfos = configuration instanceof ProjectInjectionConfiguration
            ? new CfgInfo[]{new CfgInfo(((ProjectInjectionConfiguration) configuration).getParentConfiguration(), "global"), currentInfo}
            : new CfgInfo[]{currentInfo};
    }

    private DefaultActionGroup createActions() {
        Consumer<BaseInjection> consumer = injection -> addInjection(injection);
        Supplier<BaseInjection> producer = () -> {
            InjInfo info = getSelectedInjection();
            return info == null ? null : info.injection;
        };
        for (LanguageInjectionSupport support : InjectorUtils.getActiveInjectionSupports()) {
            ContainerUtil.addAll(myAddActions, support.createAddActions(myProject, consumer));
            AnAction action = support.createEditAction(myProject, producer);
            myEditActions.put(support.getId(), action == null ? AbstractLanguageInjectionSupport.createDefaultEditAction(myProject, producer) : action);
            mySupports.put(support.getId(), support);
        }
        Collections.sort(myAddActions, new Comparator<>() {
            @Override
            public int compare(AnAction o1, AnAction o2) {
                return Comparing.compare(o1.getTemplatePresentation().getText(), o2.getTemplatePresentation().getText());
            }
        });

        DefaultActionGroup group = new DefaultActionGroup();
        AnAction addAction = new AnAction(CommonLocalize.buttonAdd(), CommonLocalize.buttonAdd(), PlatformIconGroup.generalAdd()) {
            @Override
            public void update(@Nonnull AnActionEvent e) {
                e.getPresentation().setEnabled(!myAddActions.isEmpty());
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                performAdd(e);
            }
        };
        AnAction removeAction = new AnAction(
            CommonLocalize.buttonRemove(),
            CommonLocalize.buttonRemove(),
            PlatformIconGroup.generalRemove()
        ) {
            @Override
            public void update(@Nonnull AnActionEvent e) {
                boolean enabled = false;
                for (InjInfo info : getSelectedInjections()) {
                    if (!info.bundled) {
                        enabled = true;
                        break;
                    }
                }
                e.getPresentation().setEnabled(enabled);
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                performRemove();
            }
        };

        AnAction editAction = new AnAction(
            CommonLocalize.buttonEdit(),
            CommonLocalize.buttonEdit(),
            PlatformIconGroup.actionsEdit()
        ) {
            @Override
            public void update(@Nonnull AnActionEvent e) {
                AnAction action = getEditAction();
                e.getPresentation().setEnabled(action != null);
                if (action != null) {
                    action.update(e);
                }
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                performEditAction(e);
            }
        };
        AnAction copyAction = new AnAction(
            LocalizeValue.localizeTODO("Duplicate"),
            LocalizeValue.localizeTODO("Duplicate"),
            PlatformIconGroup.actionsCopy()
        ) {
            @Override
            public void update(@Nonnull AnActionEvent e) {
                AnAction action = getEditAction();
                e.getPresentation().setEnabled(action != null);
                if (action != null) {
                    action.update(e);
                }
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                InjInfo injection = getSelectedInjection();
                if (injection != null) {
                    addInjection(injection.injection.copy());
                    //performEditAction(e);
                }
            }
        };
        group.add(addAction);
        group.add(removeAction);
        group.add(copyAction);
        group.add(editAction);

        addAction.registerCustomShortcutSet(CommonShortcuts.INSERT, myInjectionsTable);
        removeAction.registerCustomShortcutSet(CommonShortcuts.getDelete(), myInjectionsTable);
        editAction.registerCustomShortcutSet(CommonShortcuts.ENTER, myInjectionsTable);

        group.addSeparator();
        group.add(new AnAction("Enable Selected Injections", "Enable Selected Injections", PlatformIconGroup.actionsSelectall()) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                performSelectedInjectionsEnabled(true);
            }
        });
        group.add(new AnAction("Disable Selected Injections", "Disable Selected Injections", PlatformIconGroup.actionsUnselectall()) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                performSelectedInjectionsEnabled(false);
            }
        });

        new AnAction("Toggle") {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                performToggleAction();
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0)), myInjectionsTable);

        if (myInfos.length > 1) {
            group.addSeparator();
            AnAction shareAction = new AnAction(
                LocalizeValue.localizeTODO("Make Global"),
                LocalizeValue.empty(),
                PlatformIconGroup.actionsImport()
            ) {
                @Override
                @RequiredUIAccess
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    List<InjInfo> injections = getSelectedInjections();
                    CfgInfo cfg = getTargetCfgInfo(injections);
                    if (cfg == null) {
                        return;
                    }
                    for (InjInfo info : injections) {
                        if (info.cfgInfo == cfg) {
                            continue;
                        }
                        if (info.bundled) {
                            continue;
                        }
                        info.cfgInfo.injectionInfos.remove(info);
                        cfg.addInjection(info.injection);
                    }
                    int[] selectedRows = myInjectionsTable.getSelectedRows();
                    myInjectionsTable.getListTableModel().setItems(getInjInfoList(myInfos));
                    TableUtil.selectRows(myInjectionsTable, selectedRows);
                }

                @Override
                public void update(@Nonnull AnActionEvent e) {
                    CfgInfo cfg = getTargetCfgInfo(getSelectedInjections());
                    e.getPresentation().setEnabled(cfg != null);
                    e.getPresentation().setText(cfg == getDefaultCfgInfo() ? "Make Global" : "Move to Project");
                    super.update(e);
                }

                @Nullable
                private CfgInfo getTargetCfgInfo(List<InjInfo> injections) {
                    CfgInfo cfg = null;
                    for (InjInfo info : injections) {
                        if (info.bundled) {
                            continue;
                        }
                        if (cfg == null) {
                            cfg = info.cfgInfo;
                        }
                        else if (cfg != info.cfgInfo) {
                            return info.cfgInfo;
                        }
                    }
                    if (cfg == null) {
                        return cfg;
                    }
                    for (CfgInfo info : myInfos) {
                        if (info != cfg) {
                            return info;
                        }
                    }
                    throw new AssertionError();
                }
            };
            shareAction.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.SHIFT_DOWN_MASK)), myInjectionsTable);
            group.add(shareAction);
        }
        group.addSeparator();
        group.add(new AnAction(
            LocalizeValue.localizeTODO("Import"),
            LocalizeValue.localizeTODO("Import"),
            PlatformIconGroup.actionsInstall()
        ) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                doImportAction(e.getDataContext());
                updateCountLabel();
            }
        });
        group.add(new AnAction(
            LocalizeValue.localizeTODO("Export"),
            LocalizeValue.localizeTODO("Export"),
            PlatformIconGroup.actionsExport()
        ) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                List<BaseInjection> injections = getInjectionList(getSelectedInjections());
                VirtualFileWrapper wrapper =
                    FileChooserFactory.getInstance().createSaveFileDialog(new FileSaverDescriptor("Export Selected " + "Injections to File...", "", "xml"), myProject).save(null, null);
                if (wrapper == null) {
                    return;
                }
                Configuration configuration = new Configuration();
                configuration.setInjections(injections);
                Document document = new Document(configuration.getState());
                try {
                    JDOMUtil.writeDocument(document, wrapper.getFile(), "\n");
                }
                catch (IOException ex) {
                    String msg = ex.getLocalizedMessage();
                    Messages.showErrorDialog(myProject, msg != null && msg.length() > 0 ? msg : ex.toString(), "Export Failed");
                }
            }

            @Override
            public void update(@Nonnull AnActionEvent e) {
                e.getPresentation().setEnabled(!getSelectedInjections().isEmpty());
            }
        });

        return group;
    }


    private void performEditAction(AnActionEvent e) {
        AnAction action = getEditAction();
        if (action != null) {
            int row = myInjectionsTable.getSelectedRow();
            action.actionPerformed(e);
            myInjectionsTable.getListTableModel().fireTableDataChanged();
            myInjectionsTable.getSelectionModel().setSelectionInterval(row, row);
            updateCountLabel();
        }
    }

    private void updateCountLabel() {
        int placesCount = 0;
        int enablePlacesCount = 0;
        List<InjInfo> items = myInjectionsTable.getListTableModel().getItems();
        if (!items.isEmpty()) {
            for (InjInfo injection : items) {
                for (InjectionPlace place : injection.injection.getInjectionPlaces()) {
                    placesCount++;
                    if (place.isEnabled()) {
                        enablePlacesCount++;
                    }
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append(items.size()).append(" injection").append(items.size() > 1 ? "s" : "").append(" (").append(enablePlacesCount).append(" of ").append(placesCount).append(" place")
                .append(placesCount > 1 ? "s" : "").append(" enabled) ");
            myCountLabel.setText(sb.toString());
        }
        else {
            myCountLabel.setText("no injections configured ");
        }
    }

    @Nullable
    private AnAction getEditAction() {
        InjInfo info = getSelectedInjection();
        String supportId = info == null ? null : info.injection.getSupportId();
        return supportId == null ? null : myEditActions.get(supportId);
    }

    private void addInjection(BaseInjection injection) {
        InjInfo info = getDefaultCfgInfo().addInjection(injection);
        myInjectionsTable.getListTableModel().setItems(getInjInfoList(myInfos));
        int index = myInjectionsTable.convertRowIndexToView(myInjectionsTable.getListTableModel().getItems().indexOf(info));
        myInjectionsTable.getSelectionModel().setSelectionInterval(index, index);
        TableUtil.scrollSelectionToVisible(myInjectionsTable);
    }

    private CfgInfo getDefaultCfgInfo() {
        return myInfos[0];
    }

    @Override
    public boolean hasOwnContent() {
        return true;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public Configurable[] getConfigurables() {
        if (myConfigurables == null) {
            List<Configurable> configurables = new ArrayList<>();
            for (LanguageInjectionSupport support : InjectorUtils.getActiveInjectionSupports()) {
                ContainerUtil.addAll(configurables, support.createSettings(myProject, myConfiguration));
            }
            Collections.sort(configurables, Configurable.IGNORE_CASE_DISPLAY_NAME_COMPARATOR);
            myConfigurables = configurables.toArray(new Configurable[configurables.size()]);
        }

        return myConfigurables;
    }

    @Nonnull
    @Override
    public String getId() {
        return "IntelliLang.Configuration";
    }

    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    private static void sortInjections(List<BaseInjection> injections) {
        Collections.sort(injections, new Comparator<>() {
            @Override
            public int compare(BaseInjection o1, BaseInjection o2) {
                int support = Comparing.compare(o1.getSupportId(), o2.getSupportId());
                if (support != 0) {
                    return support;
                }
                int lang = Comparing.compare(o1.getInjectedLanguageId(), o2.getInjectedLanguageId());
                if (lang != 0) {
                    return lang;
                }
                return Comparing.compare(o1.getDisplayName(), o2.getDisplayName());
            }
        });
    }

    @RequiredUIAccess
    @Override
    public JComponent createComponent(Disposable uiDisposable) {
        myRoot = new JPanel(new BorderLayout());

        myInjectionsTable = new InjectionsTable(getInjInfoList(myInfos));
        myInjectionsTable.getEmptyText().setText("No injections configured");
        JPanel tablePanel = new JPanel(new BorderLayout());

        tablePanel.add(ScrollPaneFactory.createScrollPane(myInjectionsTable), BorderLayout.CENTER);

        DefaultActionGroup group = createActions();

        myToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);
        myToolbar.setTargetComponent(myInjectionsTable);
        myRoot.add(myToolbar.getComponent(), BorderLayout.NORTH);
        myRoot.add(tablePanel, BorderLayout.CENTER);
        myCountLabel = new JLabel();
        myCountLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        myCountLabel.setForeground(SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES.getFgColor());
        myRoot.add(myCountLabel, BorderLayout.SOUTH);
        updateCountLabel();
        return myRoot;
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        for (CfgInfo info : myInfos) {
            info.reset();
        }
        myInjectionsTable.getListTableModel().setItems(getInjInfoList(myInfos));
        updateCountLabel();
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        myRoot = null;
        myInjectionsTable = null;
        myToolbar = null;
    }

    @RequiredUIAccess
    @Override
    public void apply() {
        for (CfgInfo info : myInfos) {
            info.apply();
        }
        reset();
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        for (CfgInfo info : myInfos) {
            if (info.isModified()) {
                return true;
            }
        }
        return false;
    }

    private void performSelectedInjectionsEnabled(boolean enabled) {
        for (InjInfo info : getSelectedInjections()) {
            info.injection.setPlaceEnabled(null, enabled);
        }
        myInjectionsTable.updateUI();
        updateCountLabel();
    }

    private void performToggleAction() {
        List<InjInfo> selectedInjections = getSelectedInjections();
        boolean enabledExists = false;
        boolean disabledExists = false;
        for (InjInfo info : selectedInjections) {
            if (info.injection.isEnabled()) {
                enabledExists = true;
            }
            else {
                disabledExists = true;
            }
            if (enabledExists && disabledExists) {
                break;
            }
        }
        boolean allEnabled = !enabledExists && disabledExists;
        performSelectedInjectionsEnabled(allEnabled);
    }

    private void performRemove() {
        int selectedRow = myInjectionsTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        List<InjInfo> selected = getSelectedInjections();
        for (InjInfo info : selected) {
            if (info.bundled) {
                continue;
            }
            info.cfgInfo.injectionInfos.remove(info);
        }
        myInjectionsTable.getListTableModel().setItems(getInjInfoList(myInfos));
        int index = Math.min(myInjectionsTable.getListTableModel().getRowCount() - 1, selectedRow);
        myInjectionsTable.getSelectionModel().setSelectionInterval(index, index);
        TableUtil.scrollSelectionToVisible(myInjectionsTable);
        updateCountLabel();
    }

    private List<InjInfo> getSelectedInjections() {
        ArrayList<InjInfo> toRemove = new ArrayList<>();
        for (int row : myInjectionsTable.getSelectedRows()) {
            toRemove.add(myInjectionsTable.getItems().get(myInjectionsTable.convertRowIndexToModel(row)));
        }
        return toRemove;
    }

    @Nullable
    private InjInfo getSelectedInjection() {
        int row = myInjectionsTable.getSelectedRow();
        return row < 0 ? null : myInjectionsTable.getItems().get(myInjectionsTable.convertRowIndexToModel(row));
    }

    private void performAdd(AnActionEvent e) {
        DefaultActionGroup group = new DefaultActionGroup();
        for (AnAction action : myAddActions) {
            group.add(action);
        }

        JBPopupFactory.getInstance().createActionGroupPopup(null, group, e.getDataContext(), JBPopupFactory.ActionSelectionAid.NUMBERING, true, new Runnable() {
            @Override
            public void run() {
                updateCountLabel();
            }
        }, -1).showUnderneathOf(myToolbar.getComponent());
    }

    @Override
    @Nonnull
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Language Injections");
    }

    @Nullable
    @Override
    public String getParentId() {
        return StandardConfigurableIds.EDITOR_GROUP;
    }

    private class InjectionsTable extends TableView<InjInfo> {
        private InjectionsTable(List<InjInfo> injections) {
            super(new ListTableModel<>(createInjectionColumnInfos(), injections, 1));
            setAutoResizeMode(AUTO_RESIZE_LAST_COLUMN);
            getColumnModel().getColumn(2).setCellRenderer(createLanguageCellRenderer());
            getColumnModel().getColumn(1).setCellRenderer(createDisplayNameCellRenderer());
            getColumnModel().getColumn(0).setResizable(false);
            setShowGrid(false);
            setShowVerticalLines(false);
            setGridColor(getForeground());
            getColumnModel().getColumn(0).setMaxWidth(new JCheckBox().getPreferredSize().width);

            new DoubleClickListener() {
                @Override
                protected boolean onDoubleClick(MouseEvent e) {
                    int row = rowAtPoint(e.getPoint());
                    if (row < 0) {
                        return false;
                    }
                    if (columnAtPoint(e.getPoint()) <= 0) {
                        return false;
                    }
                    myInjectionsTable.getSelectionModel().setSelectionInterval(row, row);
                    performEditAction(new AnActionEvent(e, DataManager.getInstance().getDataContext(InjectionsTable.this), ActionPlaces.UNKNOWN, new Presentation(""), ActionManager.getInstance(), 0));
                    return true;
                }
            }.installOn(this);

            String[] maxName = new String[]{""};
            ContainerUtil.process(injections, injection -> {
                String languageId = injection.injection.getInjectedLanguageId();
                Language language = InjectedLanguage.findLanguageById(languageId);
                String displayName = language == null ? languageId : language.getDisplayName().get();
                if (maxName[0].length() < displayName.length()) {
                    maxName[0] = displayName;
                }
                return true;
            });
            ContainerUtil.process(InjectedLanguage.getAvailableLanguages(), language -> {
                String displayName = language.getDisplayName().get();
                if (maxName[0].length() < displayName.length()) {
                    maxName[0] = displayName;
                }
                return true;
            });
            Image icon = PlainTextFileType.INSTANCE.getIcon();
            int preferred = (int) (new JLabel(maxName[0], TargetAWT.to(icon), SwingConstants.LEFT).getPreferredSize().width * 1.1);
            getColumnModel().getColumn(2).setMinWidth(preferred);
            getColumnModel().getColumn(2).setPreferredWidth(preferred);
            getColumnModel().getColumn(2).setMaxWidth(preferred);

            TableViewSpeedSearch.register(this, element -> {
                BaseInjection injection = element.injection;
                return injection.getSupportId() + " " + injection.getInjectedLanguageId() + " " + injection.getDisplayName();
            });
        }
    }

    private ColumnInfo[] createInjectionColumnInfos() {
        final TableCellRenderer booleanCellRenderer = createBooleanCellRenderer();
        final TableCellRenderer displayNameCellRenderer = createDisplayNameCellRenderer();
        final TableCellRenderer languageCellRenderer = createLanguageCellRenderer();
        final Comparator<InjInfo> languageComparator = new Comparator<>() {
            @Override
            public int compare(InjInfo o1, InjInfo o2) {
                return Comparing.compare(o1.injection.getInjectedLanguageId(), o2.injection.getInjectedLanguageId());
            }
        };
        final Comparator<InjInfo> displayNameComparator = new Comparator<>() {
            @Override
            public int compare(InjInfo o1, InjInfo o2) {
                int support = Comparing.compare(o1.injection.getSupportId(), o2.injection.getSupportId());
                if (support != 0) {
                    return support;
                }
                return Comparing.compare(o1.injection.getDisplayName(), o2.injection.getDisplayName());
            }
        };
        ColumnInfo[] columnInfos = {new ColumnInfo<InjInfo, Boolean>(" ") {
            @Override
            public Class getColumnClass() {
                return Boolean.class;
            }

            @Override
            public Boolean valueOf(InjInfo o) {
                return o.injection.isEnabled();
            }

            @Override
            public boolean isCellEditable(InjInfo injection) {
                return true;
            }

            @Override
            public void setValue(InjInfo injection, Boolean value) {
                injection.injection.setPlaceEnabled(null, value);
            }

            @Override
            public TableCellRenderer getRenderer(InjInfo injection) {
                return booleanCellRenderer;
            }
        }, new ColumnInfo<InjInfo, InjInfo>("Display Name") {
            @Override
            public InjInfo valueOf(InjInfo info) {
                return info;
            }

            @Override
            public Comparator<InjInfo> getComparator() {
                return displayNameComparator;
            }

            @Override
            public TableCellRenderer getRenderer(InjInfo injection) {
                return displayNameCellRenderer;
            }
        }, new ColumnInfo<InjInfo, InjInfo>("Language") {
            @Override
            public InjInfo valueOf(InjInfo info) {
                return info;
            }

            @Override
            public Comparator<InjInfo> getComparator() {
                return languageComparator;
            }

            @Override
            public TableCellRenderer getRenderer(InjInfo info) {
                return languageCellRenderer;
            }
        }};
        if (myInfos.length > 1) {
            final TableCellRenderer typeRenderer = createTypeRenderer();
            return ArrayUtil.append(columnInfos, new ColumnInfo<InjInfo, String>("Type") {
                @Override
                public String valueOf(InjInfo info) {
                    return info.bundled ? "bundled" : info.cfgInfo.title;
                }

                @Override
                public TableCellRenderer getRenderer(InjInfo injInfo) {
                    return typeRenderer;
                }

                @Override
                public int getWidth(JTable table) {
                    return table.getFontMetrics(table.getFont()).stringWidth(StringUtil.repeatSymbol('m', 6));
                }

                @Override
                public Comparator<InjInfo> getComparator() {
                    return new Comparator<>() {
                        @Override
                        public int compare(InjInfo o1, InjInfo o2) {
                            return Comparing.compare(valueOf(o1), valueOf(o2));
                        }
                    };
                }
            });
        }
        return columnInfos;
    }

    private static BooleanTableCellRenderer createBooleanCellRenderer() {
        return new BooleanTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return setLabelColors(super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column), table, isSelected, row);
            }
        };
    }

    private static TableCellRenderer createLanguageCellRenderer() {
        return new TableCellRenderer() {
            final JLabel myLabel = new JLabel();

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                InjInfo injection = (InjInfo) value;
                // fix for a marvellous Swing peculiarity: AccessibleJTable likes to pass null here
                if (injection == null) {
                    return myLabel;
                }
                String languageId = injection.injection.getInjectedLanguageId();
                Language language = InjectedLanguage.findLanguageById(languageId);
                FileType fileType = language == null ? null : language.getAssociatedFileType();
                myLabel.setIcon(fileType == null ? null : TargetAWT.to(fileType.getIcon()));
                myLabel.setText(language == null ? languageId : language.getDisplayName().get());
                setLabelColors(myLabel, table, isSelected, row);
                return myLabel;
            }
        };
    }

    private TableCellRenderer createDisplayNameCellRenderer() {
        return new TableCellRenderer() {
            final SimpleColoredComponent myLabel = new SimpleColoredComponent();
            final SimpleColoredText myText = new SimpleColoredText();

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                myLabel.clear();
                InjInfo info = (InjInfo) value;
                // fix for a marvellous Swing peculiarity: AccessibleJTable likes to pass null here
                if (info == null) {
                    return myLabel;
                }
                SimpleTextAttributes grayAttrs = isSelected ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.GRAY_ATTRIBUTES;
                String supportId = info.injection.getSupportId();
                myText.append(supportId + ": ", grayAttrs);
                mySupports.get(supportId).setupPresentation(info.injection, myText, isSelected);
                myText.appendToComponent(myLabel);
                myText.clear();
                setLabelColors(myLabel, table, isSelected, row);
                return myLabel;
            }
        };
    }

    private static TableCellRenderer createTypeRenderer() {
        return new TableCellRenderer() {
            final SimpleColoredComponent myLabel = new SimpleColoredComponent();

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                myLabel.clear();
                String info = (String) value;
                if (info == null) {
                    return myLabel;
                }
                SimpleTextAttributes grayAttrs = isSelected ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.GRAY_ATTRIBUTES;
                myLabel.append(info, grayAttrs);
                setLabelColors(myLabel, table, isSelected, row);
                return myLabel;
            }
        };
    }

    private static Component setLabelColors(Component label, JTable table, boolean isSelected, int row) {
        if (label instanceof JComponent) {
            ((JComponent) label).setOpaque(true);
        }
        label.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
        label.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        return label;
    }

    private void doImportAction(DataContext dataContext) {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, true, false, true, false) {
            @Override
            public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
                return super.isFileVisible(file, showHiddenFiles) && (file.isDirectory() || "xml".equals(file.getExtension()) || file.getFileType() instanceof ArchiveFileType);
            }

            @RequiredUIAccess
            @Override
            public boolean isFileSelectable(VirtualFile file) {
                return "xml".equalsIgnoreCase(file.getExtension());
            }
        };
        descriptor.setDescription("Please select the configuration file (usually named IntelliLang.xml) to import.");
        descriptor.setTitle("Import Configuration");

        descriptor.putUserData(LangDataKeys.MODULE_CONTEXT, dataContext.getData(Module.KEY));

        SplitterProportionsData splitterData = new SplitterProportionsDataImpl();
        splitterData.externalizeFromDimensionService("IntelliLang.ImportSettingsKey.SplitterProportions");

        VirtualFile file = IdeaFileChooser.chooseFile(descriptor, myProject, null);
        if (file == null) {
            return;
        }
        try {
            Configuration cfg = Configuration.load(file.getInputStream());
            if (cfg == null) {
                Messages.showWarningDialog(myProject, "The selected file does not contain any importable configuration.", "Nothing to Import");
                return;
            }
            CfgInfo info = getDefaultCfgInfo();
            Map<String, Set<InjInfo>> currentMap = ContainerUtil.classify(info.injectionInfos.iterator(), new Function<InjInfo, String>() {
                @Override
                public String apply(InjInfo o) {
                    return o.injection.getSupportId();
                }
            });
            List<BaseInjection> originalInjections = new ArrayList<>();
            List<BaseInjection> newInjections = new ArrayList<>();
            //// remove duplicates
            //for (String supportId : InjectorUtils.getActiveInjectionSupportIds()) {
            //  final Set<BaseInjection> currentInjections = currentMap.get(supportId);
            //  if (currentInjections == null) continue;
            //  for (BaseInjection injection : currentInjections) {
            //    Configuration.importInjections(newInjections, Collections.singleton(injection), originalInjections, newInjections);
            //  }
            //}
            //myInjections.clear();
            //myInjections.addAll(newInjections);

            for (String supportId : InjectorUtils.getActiveInjectionSupportIds()) {
                ArrayList<InjInfo> list = new ArrayList<>(ObjectUtil.notNull(currentMap.get(supportId), Collections.<InjInfo>emptyList()));
                List<BaseInjection> currentInjections = getInjectionList(list);
                List<BaseInjection> importingInjections = cfg.getInjections(supportId);
                if (currentInjections == null) {
                    newInjections.addAll(importingInjections);
                }
                else {
                    Configuration.importInjections(currentInjections, importingInjections, originalInjections, newInjections);
                }
            }
            info.replace(originalInjections, newInjections);
            myInjectionsTable.getListTableModel().setItems(getInjInfoList(myInfos));
            int n = newInjections.size();
            if (n > 1) {
                Messages.showInfoMessage(myProject, n + " entries have been successfully imported", "Import Successful");
            }
            else if (n == 1) {
                Messages.showInfoMessage(myProject, "One entry has been successfully imported", "Import Successful");
            }
            else {
                Messages.showInfoMessage(myProject, "No new entries have been imported", "Import");
            }
        }
        catch (Exception ex) {
            LOG.error(ex);

            String msg = ex.getLocalizedMessage();
            Messages.showErrorDialog(myProject, msg != null && msg.length() > 0 ? msg : ex.toString(), "Import Failed");
        }
    }

    private static class CfgInfo {
        final Configuration cfg;
        final ArrayList<BaseInjection> originalInjections;
        final List<InjInfo> injectionInfos = new ArrayList<>();
        final Set<BaseInjection> bundledInjections = Sets.newHashSet(new SameParamsAndPlacesStrategy());
        final String title;

        public CfgInfo(Configuration cfg, String title) {
            this.cfg = cfg;
            this.title = title;
            bundledInjections.addAll(cfg.getDefaultInjections());
            Collection<String> supportIds = InjectorUtils.getActiveInjectionSupportIds();

            originalInjections = new ArrayList<>(ContainerUtil.concat(supportIds, s -> ContainerUtil
                .findAll(CfgInfo.this.cfg instanceof ProjectInjectionConfiguration ? ((ProjectInjectionConfiguration) CfgInfo.this.cfg).getOwnInjections(s) : CfgInfo.this.cfg.getInjections(s),
                    injection -> InjectedLanguage.findLanguageById(injection.getInjectedLanguageId()) != null)));
            sortInjections(originalInjections);
            reset();
        }

        public void apply() {
            List<BaseInjection> injectionList = getInjectionList(injectionInfos);
            cfg.replaceInjections(injectionList, originalInjections, true);
            originalInjections.clear();
            originalInjections.addAll(injectionList);
            sortInjections(originalInjections);
            FileContentUtil.reparseOpenedFiles();
        }

        public void reset() {
            injectionInfos.clear();
            for (BaseInjection injection : originalInjections) {
                injectionInfos.add(new InjInfo(injection.copy(), this));
            }
        }

        public InjInfo addInjection(BaseInjection injection) {
            InjInfo info = new InjInfo(injection, this);
            injectionInfos.add(info);
            return info;
        }

        public boolean isModified() {
            List<BaseInjection> copy = new ArrayList<>(getInjectionList(injectionInfos));
            sortInjections(copy);
            return !originalInjections.equals(copy);
        }

        public void replace(List<BaseInjection> originalInjections, List<BaseInjection> newInjections) {
            for (Iterator<InjInfo> it = injectionInfos.iterator(); it.hasNext(); ) {
                InjInfo info = it.next();
                if (originalInjections.contains(info.injection)) {
                    it.remove();
                }
            }
            for (BaseInjection newInjection : newInjections) {
                injectionInfos.add(new InjInfo(newInjection, this));
            }
        }

    }

    private static class SameParamsAndPlacesStrategy implements HashingStrategy<BaseInjection> {
        @Override
        public int hashCode(BaseInjection object) {
            return object.hashCode();
        }

        @Override
        public boolean equals(BaseInjection o1, BaseInjection o2) {
            return o1.sameLanguageParameters(o2) && Arrays.equals(o1.getInjectionPlaces(), o2.getInjectionPlaces());
        }
    }

    private static class InjInfo {
        final BaseInjection injection;
        final CfgInfo cfgInfo;
        final boolean bundled;

        private InjInfo(BaseInjection injection, CfgInfo cfgInfo) {
            this.injection = injection;
            this.cfgInfo = cfgInfo;
            bundled = cfgInfo.bundledInjections.contains(injection);
        }
    }

    private static List<InjInfo> getInjInfoList(CfgInfo[] infos) {
        return ContainerUtil.concat(infos, cfgInfo -> cfgInfo.injectionInfos);
    }

    private static List<BaseInjection> getInjectionList(final List<InjInfo> list) {
        return new AbstractList<>() {
            @Override
            public BaseInjection get(int index) {
                return list.get(index).injection;
            }

            @Override
            public int size() {
                return list.size();
            }
        };
    }
}
