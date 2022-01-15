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

package org.intellij.plugins.intelliLang;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.ui.SplitterProportionsDataImpl;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SplitterProportionsData;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.*;
import com.intellij.ui.table.TableView;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Convertor;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import consulo.awt.TargetAWT;
import consulo.fileTypes.ArchiveFileType;
import consulo.logging.Logger;
import consulo.psi.injection.AbstractLanguageInjectionSupport;
import consulo.psi.injection.LanguageInjectionSupport;
import consulo.psi.injection.impl.ProjectInjectionConfiguration;
import consulo.ui.image.Image;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Sets;
import jakarta.inject.Inject;
import org.intellij.plugins.intelliLang.inject.InjectedLanguage;
import org.intellij.plugins.intelliLang.inject.InjectorUtils;
import org.intellij.plugins.intelliLang.inject.config.BaseInjection;
import org.intellij.plugins.intelliLang.inject.config.InjectionPlace;
import org.jdom.Document;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;
import java.util.*;

/**
 * @author Gregory.Shrago
 */
public class InjectionsSettingsUI implements SearchableConfigurable.Parent, Configurable.NoScroll {
  private static final Logger LOG = Logger.getInstance(InjectionsSettingsUI.class);

  private final Project myProject;
  private final CfgInfo[] myInfos;

  private final JPanel myRoot;
  private final InjectionsTable myInjectionsTable;
  private final Map<String, LanguageInjectionSupport> mySupports = new HashMap<String, LanguageInjectionSupport>();
  private final Map<String, AnAction> myEditActions = new HashMap<String, AnAction>();
  private final List<AnAction> myAddActions = new ArrayList<AnAction>();
  private final ActionToolbar myToolbar;
  private final JLabel myCountLabel;

  private Configurable[] myConfigurables;
  private Configuration myConfiguration;

  @Inject
  public InjectionsSettingsUI(final Project project) {
    this(project, Configuration.getProjectInstance(project));
  }

  protected InjectionsSettingsUI(final Project project, final Configuration configuration) {
    myProject = project;
    myConfiguration = configuration;

    final CfgInfo currentInfo = new CfgInfo(configuration, "project");
    myInfos = configuration instanceof ProjectInjectionConfiguration
              ? new CfgInfo[]{new CfgInfo(((ProjectInjectionConfiguration)configuration).getParentConfiguration(), "global"), currentInfo}
              : new CfgInfo[]{currentInfo};

    myRoot = new JPanel(new BorderLayout());

    myInjectionsTable = new InjectionsTable(getInjInfoList(myInfos));
    myInjectionsTable.getEmptyText().setText("No injections configured");
    final JPanel tablePanel = new JPanel(new BorderLayout());

    tablePanel.add(ScrollPaneFactory.createScrollPane(myInjectionsTable), BorderLayout.CENTER);

    final DefaultActionGroup group = createActions();

    myToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);
    myToolbar.setTargetComponent(myInjectionsTable);
    myRoot.add(myToolbar.getComponent(), BorderLayout.NORTH);
    myRoot.add(tablePanel, BorderLayout.CENTER);
    myCountLabel = new JLabel();
    myCountLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    myCountLabel.setForeground(SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES.getFgColor());
    myRoot.add(myCountLabel, BorderLayout.SOUTH);
    updateCountLabel();
  }

  private DefaultActionGroup createActions() {
    final Consumer<BaseInjection> consumer = new Consumer<BaseInjection>() {
      @Override
      public void consume(final BaseInjection injection) {
        addInjection(injection);
      }
    };
    final Factory<BaseInjection> producer = new NullableFactory<BaseInjection>() {
      @Override
      public BaseInjection create() {
        final InjInfo info = getSelectedInjection();
        return info == null ? null : info.injection;
      }
    };
    for (LanguageInjectionSupport support : InjectorUtils.getActiveInjectionSupports()) {
      ContainerUtil.addAll(myAddActions, support.createAddActions(myProject, consumer));
      final AnAction action = support.createEditAction(myProject, producer);
      myEditActions.put(support.getId(), action == null ? AbstractLanguageInjectionSupport.createDefaultEditAction(myProject, producer) : action);
      mySupports.put(support.getId(), support);
    }
    Collections.sort(myAddActions, new Comparator<AnAction>() {
      @Override
      public int compare(final AnAction o1, final AnAction o2) {
        return Comparing.compare(o1.getTemplatePresentation().getText(), o2.getTemplatePresentation().getText());
      }
    });

    final DefaultActionGroup group = new DefaultActionGroup();
    final AnAction addAction = new AnAction("Add", "Add", IconUtil.getAddIcon()) {
      @Override
      public void update(final AnActionEvent e) {
        e.getPresentation().setEnabled(!myAddActions.isEmpty());
      }

      @Override
      public void actionPerformed(final AnActionEvent e) {
        performAdd(e);
      }
    };
    final AnAction removeAction = new AnAction("Remove", "Remove", IconUtil.getRemoveIcon()) {
      @Override
      public void update(final AnActionEvent e) {
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
      public void actionPerformed(final AnActionEvent e) {
        performRemove();
      }
    };

    final AnAction editAction = new AnAction("Edit", "Edit", AllIcons.Actions.Properties) {
      @Override
      public void update(final AnActionEvent e) {
        final AnAction action = getEditAction();
        e.getPresentation().setEnabled(action != null);
        if (action != null) {
          action.update(e);
        }
      }

      @Override
      public void actionPerformed(final AnActionEvent e) {
        performEditAction(e);
      }
    };
    final AnAction copyAction = new AnAction("Duplicate", "Duplicate", AllIcons.Actions.Copy) {
      @Override
      public void update(final AnActionEvent e) {
        final AnAction action = getEditAction();
        e.getPresentation().setEnabled(action != null);
        if (action != null) {
          action.update(e);
        }
      }

      @Override
      public void actionPerformed(final AnActionEvent e) {
        final InjInfo injection = getSelectedInjection();
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
    group.add(new AnAction("Enable Selected Injections", "Enable Selected Injections", AllIcons.Actions.Selectall) {
      @Override
      public void actionPerformed(final AnActionEvent e) {
        performSelectedInjectionsEnabled(true);
      }
    });
    group.add(new AnAction("Disable Selected Injections", "Disable Selected Injections", AllIcons.Actions.Unselectall) {
      @Override
      public void actionPerformed(final AnActionEvent e) {
        performSelectedInjectionsEnabled(false);
      }
    });

    new AnAction("Toggle") {
      @Override
      public void actionPerformed(final AnActionEvent e) {
        performToggleAction();
      }
    }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0)), myInjectionsTable);

    if (myInfos.length > 1) {
      group.addSeparator();
      final AnAction shareAction = new AnAction("Make Global", null, AllIcons.ToolbarDecorator.Import) {
        @Override
        public void actionPerformed(final AnActionEvent e) {
          final List<InjInfo> injections = getSelectedInjections();
          final CfgInfo cfg = getTargetCfgInfo(injections);
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
          final int[] selectedRows = myInjectionsTable.getSelectedRows();
          myInjectionsTable.getListTableModel().setItems(getInjInfoList(myInfos));
          TableUtil.selectRows(myInjectionsTable, selectedRows);
        }

        @Override
        public void update(final AnActionEvent e) {
          final CfgInfo cfg = getTargetCfgInfo(getSelectedInjections());
          e.getPresentation().setEnabled(cfg != null);
          e.getPresentation().setText(cfg == getDefaultCfgInfo() ? "Make Global" : "Move to Project");
          super.update(e);
        }

        @Nullable
        private CfgInfo getTargetCfgInfo(final List<InjInfo> injections) {
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
    group.add(new AnAction("Import", "Import", AllIcons.Actions.Install) {
      @Override
      public void actionPerformed(final AnActionEvent e) {
        doImportAction(e.getDataContext());
        updateCountLabel();
      }
    });
    group.add(new AnAction("Export", "Export", AllIcons.Actions.Export) {
      @Override
      public void actionPerformed(final AnActionEvent e) {
        final List<BaseInjection> injections = getInjectionList(getSelectedInjections());
        final VirtualFileWrapper wrapper =
                FileChooserFactory.getInstance().createSaveFileDialog(new FileSaverDescriptor("Export Selected " + "Injections to File...", "", "xml"), myProject).save(null, null);
        if (wrapper == null) {
          return;
        }
        final Configuration configuration = new Configuration();
        configuration.setInjections(injections);
        final Document document = new Document(configuration.getState());
        try {
          JDOMUtil.writeDocument(document, wrapper.getFile(), "\n");
        }
        catch (IOException ex) {
          final String msg = ex.getLocalizedMessage();
          Messages.showErrorDialog(myProject, msg != null && msg.length() > 0 ? msg : ex.toString(), "Export Failed");
        }
      }

      @Override
      public void update(final AnActionEvent e) {
        e.getPresentation().setEnabled(!getSelectedInjections().isEmpty());
      }
    });

    return group;
  }


  private void performEditAction(AnActionEvent e) {
    final AnAction action = getEditAction();
    if (action != null) {
      final int row = myInjectionsTable.getSelectedRow();
      action.actionPerformed(e);
      myInjectionsTable.getListTableModel().fireTableDataChanged();
      myInjectionsTable.getSelectionModel().setSelectionInterval(row, row);
      updateCountLabel();
    }
  }

  private void updateCountLabel() {
    int placesCount = 0;
    int enablePlacesCount = 0;
    final List<InjInfo> items = myInjectionsTable.getListTableModel().getItems();
    if (!items.isEmpty()) {
      for (InjInfo injection : items) {
        for (InjectionPlace place : injection.injection.getInjectionPlaces()) {
          placesCount++;
          if (place.isEnabled()) {
            enablePlacesCount++;
          }
        }
      }
      final StringBuilder sb = new StringBuilder();
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
    final InjInfo info = getSelectedInjection();
    final String supportId = info == null ? null : info.injection.getSupportId();
    return supportId == null ? null : myEditActions.get(supportId);
  }

  private void addInjection(final BaseInjection injection) {
    final InjInfo info = getDefaultCfgInfo().addInjection(injection);
    myInjectionsTable.getListTableModel().setItems(getInjInfoList(myInfos));
    final int index = myInjectionsTable.convertRowIndexToView(myInjectionsTable.getListTableModel().getItems().indexOf(info));
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
      final ArrayList<Configurable> configurables = new ArrayList<Configurable>();
      for (LanguageInjectionSupport support : InjectorUtils.getActiveInjectionSupports()) {
        ContainerUtil.addAll(configurables, support.createSettings(myProject, myConfiguration));
      }
      Collections.sort(configurables, new Comparator<Configurable>() {
        @Override
        public int compare(final Configurable o1, final Configurable o2) {
          return Comparing.compare(o1.getDisplayName(), o2.getDisplayName());
        }
      });
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

  private static void sortInjections(final List<BaseInjection> injections) {
    Collections.sort(injections, new Comparator<BaseInjection>() {
      @Override
      public int compare(final BaseInjection o1, final BaseInjection o2) {
        final int support = Comparing.compare(o1.getSupportId(), o2.getSupportId());
        if (support != 0) {
          return support;
        }
        final int lang = Comparing.compare(o1.getInjectedLanguageId(), o2.getInjectedLanguageId());
        if (lang != 0) {
          return lang;
        }
        return Comparing.compare(o1.getDisplayName(), o2.getDisplayName());
      }
    });
  }

  @Override
  public JComponent createComponent() {
    return myRoot;
  }

  @Override
  public void reset() {
    for (CfgInfo info : myInfos) {
      info.reset();
    }
    myInjectionsTable.getListTableModel().setItems(getInjInfoList(myInfos));
    updateCountLabel();
  }

  @Override
  public void disposeUIResources() {
  }

  @Override
  public void apply() {
    for (CfgInfo info : myInfos) {
      info.apply();
    }
    reset();
  }

  @Override
  public boolean isModified() {
    for (CfgInfo info : myInfos) {
      if (info.isModified()) {
        return true;
      }
    }
    return false;
  }

  private void performSelectedInjectionsEnabled(final boolean enabled) {
    for (InjInfo info : getSelectedInjections()) {
      info.injection.setPlaceEnabled(null, enabled);
    }
    myInjectionsTable.updateUI();
    updateCountLabel();
  }

  private void performToggleAction() {
    final List<InjInfo> selectedInjections = getSelectedInjections();
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
    final int selectedRow = myInjectionsTable.getSelectedRow();
    if (selectedRow < 0) {
      return;
    }
    final List<InjInfo> selected = getSelectedInjections();
    for (InjInfo info : selected) {
      if (info.bundled) {
        continue;
      }
      info.cfgInfo.injectionInfos.remove(info);
    }
    myInjectionsTable.getListTableModel().setItems(getInjInfoList(myInfos));
    final int index = Math.min(myInjectionsTable.getListTableModel().getRowCount() - 1, selectedRow);
    myInjectionsTable.getSelectionModel().setSelectionInterval(index, index);
    TableUtil.scrollSelectionToVisible(myInjectionsTable);
    updateCountLabel();
  }

  private List<InjInfo> getSelectedInjections() {
    final ArrayList<InjInfo> toRemove = new ArrayList<InjInfo>();
    for (int row : myInjectionsTable.getSelectedRows()) {
      toRemove.add(myInjectionsTable.getItems().get(myInjectionsTable.convertRowIndexToModel(row)));
    }
    return toRemove;
  }

  @Nullable
  private InjInfo getSelectedInjection() {
    final int row = myInjectionsTable.getSelectedRow();
    return row < 0 ? null : myInjectionsTable.getItems().get(myInjectionsTable.convertRowIndexToModel(row));
  }

  private void performAdd(final AnActionEvent e) {
    final DefaultActionGroup group = new DefaultActionGroup();
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
  @Nls
  public String getDisplayName() {
    return "Language Injections";
  }

  private class InjectionsTable extends TableView<InjInfo> {
    private InjectionsTable(final List<InjInfo> injections) {
      super(new ListTableModel<InjInfo>(createInjectionColumnInfos(), injections, 1));
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
          final int row = rowAtPoint(e.getPoint());
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

      final String[] maxName = new String[]{""};
      ContainerUtil.process(injections, new Processor<InjInfo>() {
        @Override
        public boolean process(final InjInfo injection) {
          String languageId = injection.injection.getInjectedLanguageId();
          Language language = InjectedLanguage.findLanguageById(languageId);
          String displayName = language == null ? languageId : language.getDisplayName();
          if (maxName[0].length() < displayName.length()) {
            maxName[0] = displayName;
          }
          return true;
        }
      });
      ContainerUtil.process(InjectedLanguage.getAvailableLanguages(), new Processor<Language>() {
        @Override
        public boolean process(final Language language) {
          String displayName = language.getDisplayName();
          if (maxName[0].length() < displayName.length()) {
            maxName[0] = displayName;
          }
          return true;
        }
      });
      Image icon = PlainTextFileType.INSTANCE.getIcon();
      int preferred = (int)(new JLabel(maxName[0], TargetAWT.to(icon), SwingConstants.LEFT).getPreferredSize().width * 1.1);
      getColumnModel().getColumn(2).setMinWidth(preferred);
      getColumnModel().getColumn(2).setPreferredWidth(preferred);
      getColumnModel().getColumn(2).setMaxWidth(preferred);
      new TableViewSpeedSearch<InjInfo>(this) {
        @Override
        protected String getItemText(@Nonnull InjInfo element) {
          final BaseInjection injection = element.injection;
          return injection.getSupportId() + " " + injection.getInjectedLanguageId() + " " + injection.getDisplayName();
        }
      };
    }

  }

  private ColumnInfo[] createInjectionColumnInfos() {
    final TableCellRenderer booleanCellRenderer = createBooleanCellRenderer();
    final TableCellRenderer displayNameCellRenderer = createDisplayNameCellRenderer();
    final TableCellRenderer languageCellRenderer = createLanguageCellRenderer();
    final Comparator<InjInfo> languageComparator = new Comparator<InjInfo>() {
      @Override
      public int compare(final InjInfo o1, final InjInfo o2) {
        return Comparing.compare(o1.injection.getInjectedLanguageId(), o2.injection.getInjectedLanguageId());
      }
    };
    final Comparator<InjInfo> displayNameComparator = new Comparator<InjInfo>() {
      @Override
      public int compare(final InjInfo o1, final InjInfo o2) {
        final int support = Comparing.compare(o1.injection.getSupportId(), o2.injection.getSupportId());
        if (support != 0) {
          return support;
        }
        return Comparing.compare(o1.injection.getDisplayName(), o2.injection.getDisplayName());
      }
    };
    final ColumnInfo[] columnInfos = {new ColumnInfo<InjInfo, Boolean>(" ") {
      @Override
      public Class getColumnClass() {
        return Boolean.class;
      }

      @Override
      public Boolean valueOf(final InjInfo o) {
        return o.injection.isEnabled();
      }

      @Override
      public boolean isCellEditable(final InjInfo injection) {
        return true;
      }

      @Override
      public void setValue(final InjInfo injection, final Boolean value) {
        injection.injection.setPlaceEnabled(null, value.booleanValue());
      }

      @Override
      public TableCellRenderer getRenderer(final InjInfo injection) {
        return booleanCellRenderer;
      }
    }, new ColumnInfo<InjInfo, InjInfo>("Display Name") {
      @Override
      public InjInfo valueOf(final InjInfo info) {
        return info;
      }

      @Override
      public Comparator<InjInfo> getComparator() {
        return displayNameComparator;
      }

      @Override
      public TableCellRenderer getRenderer(final InjInfo injection) {
        return displayNameCellRenderer;
      }
    }, new ColumnInfo<InjInfo, InjInfo>("Language") {
      @Override
      public InjInfo valueOf(final InjInfo info) {
        return info;
      }

      @Override
      public Comparator<InjInfo> getComparator() {
        return languageComparator;
      }

      @Override
      public TableCellRenderer getRenderer(final InjInfo info) {
        return languageCellRenderer;
      }
    }};
    if (myInfos.length > 1) {
      final TableCellRenderer typeRenderer = createTypeRenderer();
      return ArrayUtil.append(columnInfos, new ColumnInfo<InjInfo, String>("Type") {
        @Override
        public String valueOf(final InjInfo info) {
          return info.bundled ? "bundled" : info.cfgInfo.title;
        }

        @Override
        public TableCellRenderer getRenderer(final InjInfo injInfo) {
          return typeRenderer;
        }

        @Override
        public int getWidth(final JTable table) {
          return table.getFontMetrics(table.getFont()).stringWidth(StringUtil.repeatSymbol('m', 6));
        }

        @Override
        public Comparator<InjInfo> getComparator() {
          return new Comparator<InjInfo>() {
            @Override
            public int compare(final InjInfo o1, final InjInfo o2) {
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
      public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        return setLabelColors(super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column), table, isSelected, row);
      }
    };
  }

  private static TableCellRenderer createLanguageCellRenderer() {
    return new TableCellRenderer() {
      final JLabel myLabel = new JLabel();

      @Override
      public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        final InjInfo injection = (InjInfo)value;
        // fix for a marvellous Swing peculiarity: AccessibleJTable likes to pass null here
        if (injection == null) {
          return myLabel;
        }
        final String languageId = injection.injection.getInjectedLanguageId();
        final Language language = InjectedLanguage.findLanguageById(languageId);
        final FileType fileType = language == null ? null : language.getAssociatedFileType();
        myLabel.setIcon(fileType == null ? null : TargetAWT.to(fileType.getIcon()));
        myLabel.setText(language == null ? languageId : language.getDisplayName());
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
      public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        myLabel.clear();
        final InjInfo info = (InjInfo)value;
        // fix for a marvellous Swing peculiarity: AccessibleJTable likes to pass null here
        if (info == null) {
          return myLabel;
        }
        final SimpleTextAttributes grayAttrs = isSelected ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.GRAY_ATTRIBUTES;
        final String supportId = info.injection.getSupportId();
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
      public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        myLabel.clear();
        final String info = (String)value;
        if (info == null) {
          return myLabel;
        }
        final SimpleTextAttributes grayAttrs = isSelected ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.GRAY_ATTRIBUTES;
        myLabel.append(info, grayAttrs);
        setLabelColors(myLabel, table, isSelected, row);
        return myLabel;
      }
    };
  }

  private static Component setLabelColors(final Component label, final JTable table, final boolean isSelected, final int row) {
    if (label instanceof JComponent) {
      ((JComponent)label).setOpaque(true);
    }
    label.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
    label.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
    return label;
  }

  private void doImportAction(final DataContext dataContext) {
    final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, true, false, true, false) {
      @Override
      public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
        return super.isFileVisible(file, showHiddenFiles) && (file.isDirectory() || "xml".equals(file.getExtension()) || file.getFileType() instanceof ArchiveFileType);
      }

      @Override
      public boolean isFileSelectable(VirtualFile file) {
        return "xml".equalsIgnoreCase(file.getExtension());
      }
    };
    descriptor.setDescription("Please select the configuration file (usually named IntelliLang.xml) to import.");
    descriptor.setTitle("Import Configuration");

    descriptor.putUserData(LangDataKeys.MODULE_CONTEXT, dataContext.getData(LangDataKeys.MODULE));

    final SplitterProportionsData splitterData = new SplitterProportionsDataImpl();
    splitterData.externalizeFromDimensionService("IntelliLang.ImportSettingsKey.SplitterProportions");

    final VirtualFile file = FileChooser.chooseFile(descriptor, myProject, null);
    if (file == null) {
      return;
    }
    try {
      final Configuration cfg = Configuration.load(file.getInputStream());
      if (cfg == null) {
        Messages.showWarningDialog(myProject, "The selected file does not contain any importable configuration.", "Nothing to Import");
        return;
      }
      final CfgInfo info = getDefaultCfgInfo();
      final Map<String, Set<InjInfo>> currentMap = ContainerUtil.classify(info.injectionInfos.iterator(), new Convertor<InjInfo, String>() {
        @Override
        public String convert(final InjInfo o) {
          return o.injection.getSupportId();
        }
      });
      final List<BaseInjection> originalInjections = new ArrayList<BaseInjection>();
      final List<BaseInjection> newInjections = new ArrayList<BaseInjection>();
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
        ArrayList<InjInfo> list = new ArrayList<InjInfo>(ObjectUtils.notNull(currentMap.get(supportId), Collections.<InjInfo>emptyList()));
        final List<BaseInjection> currentInjections = getInjectionList(list);
        final List<BaseInjection> importingInjections = cfg.getInjections(supportId);
        if (currentInjections == null) {
          newInjections.addAll(importingInjections);
        }
        else {
          Configuration.importInjections(currentInjections, importingInjections, originalInjections, newInjections);
        }
      }
      info.replace(originalInjections, newInjections);
      myInjectionsTable.getListTableModel().setItems(getInjInfoList(myInfos));
      final int n = newInjections.size();
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

      final String msg = ex.getLocalizedMessage();
      Messages.showErrorDialog(myProject, msg != null && msg.length() > 0 ? msg : ex.toString(), "Import Failed");
    }
  }

  private static class CfgInfo {
    final Configuration cfg;
    final List<BaseInjection> originalInjections;
    final List<InjInfo> injectionInfos = new ArrayList<InjInfo>();
    final Set<BaseInjection> bundledInjections = Sets.newHashSet(new SameParamsAndPlacesStrategy());
    final String title;

    public CfgInfo(Configuration cfg, final String title) {
      this.cfg = cfg;
      this.title = title;
      bundledInjections.addAll(cfg.getDefaultInjections());
      originalInjections = ContainerUtil.concat(InjectorUtils.getActiveInjectionSupportIds(), new Function<String, Collection<? extends BaseInjection>>() {
        @Override
        public Collection<? extends BaseInjection> fun(final String s) {
          return ContainerUtil
                  .findAll(CfgInfo.this.cfg instanceof ProjectInjectionConfiguration ? ((ProjectInjectionConfiguration)CfgInfo.this.cfg).getOwnInjections(s) : CfgInfo.this.cfg.getInjections(s),
                           new Condition<BaseInjection>() {
                             @Override
                             public boolean value(final BaseInjection injection) {
                               return InjectedLanguage.findLanguageById(injection.getInjectedLanguageId()) != null;
                             }
                           });
        }
      });
      sortInjections(originalInjections);
      reset();
    }

    public void apply() {
      final List<BaseInjection> injectionList = getInjectionList(injectionInfos);
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

    public InjInfo addInjection(final BaseInjection injection) {
      final InjInfo info = new InjInfo(injection, this);
      injectionInfos.add(info);
      return info;
    }

    public boolean isModified() {
      final List<BaseInjection> copy = new ArrayList<BaseInjection>(getInjectionList(injectionInfos));
      sortInjections(copy);
      return !originalInjections.equals(copy);
    }

    public void replace(final List<BaseInjection> originalInjections, final List<BaseInjection> newInjections) {
      for (Iterator<InjInfo> it = injectionInfos.iterator(); it.hasNext(); ) {
        final InjInfo info = it.next();
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
    public int hashCode(final BaseInjection object) {
      return object.hashCode();
    }

    @Override
    public boolean equals(final BaseInjection o1, final BaseInjection o2) {
      return o1.sameLanguageParameters(o2) && Arrays.equals(o1.getInjectionPlaces(), o2.getInjectionPlaces());
    }
  }

  private static class InjInfo {
    final BaseInjection injection;
    final CfgInfo cfgInfo;
    final boolean bundled;

    private InjInfo(final BaseInjection injection, final CfgInfo cfgInfo) {
      this.injection = injection;
      this.cfgInfo = cfgInfo;
      bundled = cfgInfo.bundledInjections.contains(injection);
    }
  }

  private static List<InjInfo> getInjInfoList(final CfgInfo[] infos) {
    return ContainerUtil.concat(infos, cfgInfo -> cfgInfo.injectionInfos);
  }

  private static List<BaseInjection> getInjectionList(final List<InjInfo> list) {
    return new AbstractList<BaseInjection>() {
      @Override
      public BaseInjection get(final int index) {
        return list.get(index).injection;
      }

      @Override
      public int size() {
        return list.size();
      }
    };
  }
}
