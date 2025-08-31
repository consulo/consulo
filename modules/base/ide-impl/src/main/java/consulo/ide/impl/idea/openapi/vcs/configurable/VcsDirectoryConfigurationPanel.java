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

package consulo.ide.impl.idea.openapi.vcs.configurable;

import consulo.application.util.registry.Registry;
import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.versionControlSystem.internal.DefaultVcsRootPolicy;
import consulo.versionControlSystem.impl.internal.NewMappings;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.table.ListTableModel;
import consulo.ui.ex.awt.table.TableView;
import consulo.util.io.FileUtil;
import consulo.util.io.UriUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.internal.VcsRootErrorsFinder;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;

import static consulo.ui.ex.awt.UIUtil.DEFAULT_HGAP;
import static consulo.ui.ex.awt.UIUtil.DEFAULT_VGAP;
import static consulo.versionControlSystem.VcsConfiguration.getInstance;
import static consulo.versionControlSystem.VcsConfiguration.ourMaximumFileForBaseRevisionSize;

/**
 * @author yole
 */
public class VcsDirectoryConfigurationPanel extends JPanel {
  private final Project myProject;
  private final String myProjectMessage;
  private final ProjectLevelVcsManager myVcsManager;
  private final TableView<MapInfo> myDirectoryMappingTable;
  private final ComboBox<VcsDescriptor> myVcsComboBox = new ComboBox<>();
  private final List<ModuleVcsListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  private final MyDirectoryRenderer myDirectoryRenderer;
  private final ColumnInfo<MapInfo, MapInfo> DIRECTORY;
  private final JCheckBox myBaseRevisionTexts;
  private ListTableModel<MapInfo> myModel;
  private final Map<String, VcsDescriptor> myAllVcss;
  private VcsContentAnnotationConfigurable myRecentlyChangedConfigurable;
  private final boolean myIsDisabled;
  private final VcsConfiguration myVcsConfiguration;
  private final
  @Nonnull
  Map<String, VcsRootChecker> myCheckers;
  private JCheckBox myShowChangedRecursively;
  private final VcsLimitHistoryConfigurable myLimitHistory;
  private final VcsUpdateInfoScopeFilterConfigurable myScopeFilterConfig;
  private VcsCommitMessageMarginConfigurable myCommitMessageMarginConfigurable;
  private JCheckBox myShowUnversionedFiles;
  private JCheckBox myCheckCommitMessageSpelling;

  private static class MapInfo {
    static final MapInfo SEPARATOR = new MapInfo(new VcsDirectoryMapping("SEPARATOR", "SEP"), Type.SEPARATOR);
    static final Comparator<MapInfo> COMPARATOR = (o1, o2) -> {
      if (o1.type.isRegistered() && o2.type.isRegistered() || o1.type == Type.UNREGISTERED && o2.type == Type.UNREGISTERED) {
        return NewMappings.MAPPINGS_COMPARATOR.compare(o1.mapping, o2.mapping);
      }
      return o1.type.ordinal() - o2.type.ordinal();
    };

    static MapInfo unregistered(@Nonnull VcsDirectoryMapping mapping) {
      return new MapInfo(mapping, Type.UNREGISTERED);
    }

    static MapInfo unregistered(@Nonnull String path, @Nonnull String vcs) {
      return new MapInfo(new VcsDirectoryMapping(path, vcs), Type.UNREGISTERED);
    }

    static MapInfo registered(@Nonnull VcsDirectoryMapping mapping, boolean valid) {
      return new MapInfo(mapping, valid ? Type.NORMAL : Type.INVALID);
    }

    enum Type {
      NORMAL,
      INVALID,
      SEPARATOR,
      UNREGISTERED;

      boolean isRegistered() {
        return this == NORMAL || this == INVALID;
      }
    }

    private final Type type;
    private final VcsDirectoryMapping mapping;

    private MapInfo(@Nonnull VcsDirectoryMapping mapping, @Nonnull Type type) {
      this.mapping = mapping;
      this.type = type;
    }
  }

  private static class MyDirectoryRenderer extends ColoredTableCellRenderer {
    private final Project myProject;

    public MyDirectoryRenderer(Project project) {
      myProject = project;
    }

    @Override
    protected void customizeCellRenderer(JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
      if (value instanceof MapInfo) {
        MapInfo info = (MapInfo)value;

        if (!selected && (info == MapInfo.SEPARATOR || info.type == MapInfo.Type.UNREGISTERED)) {
          setBackground(getUnregisteredRootBackground());
        }

        if (info == MapInfo.SEPARATOR) {
          append("Unregistered roots:", getAttributes(info));
          return;
        }

        if (info.mapping.isDefaultMapping()) {
          append(VcsDirectoryMapping.PROJECT_CONSTANT, getAttributes(info));
          return;
        }

        String directory = info.mapping.getDirectory();
        VirtualFile baseDir = myProject.getBaseDir();
        if (baseDir != null) {
          File directoryFile = new File(StringUtil.trimEnd(UriUtil.trimTrailingSlashes(directory), "\\") + "/");
          File ioBase = new File(baseDir.getPath());
          if (directoryFile.isAbsolute() && !FileUtil.isAncestor(ioBase, directoryFile, false)) {
            append(new File(directory).getPath(), getAttributes(info));
            return;
          }
          String relativePath = FileUtil.getRelativePath(ioBase, directoryFile);
          if (".".equals(relativePath) || relativePath == null) {
            append(ioBase.getPath(), getAttributes(info));
          }
          else {
            append(relativePath, getAttributes(info));
            append(" (" + ioBase + ")", SimpleTextAttributes.GRAYED_ATTRIBUTES);
          }
        }
      }
    }
  }

  @Nonnull
  private static Color getUnregisteredRootBackground() {
    return new JBColor(UIUtil.getLabelBackground(), new Color(0x45494A));
  }

  @Nonnull
  private static SimpleTextAttributes getAttributes(@Nonnull MapInfo info) {
    if (info == MapInfo.SEPARATOR) {
      return new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD | SimpleTextAttributes.STYLE_SMALLER, null);
    }
    else if (info.type == MapInfo.Type.INVALID) {
      return new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.RED);
    }
    else if (info.type == MapInfo.Type.UNREGISTERED) {
      return new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, JBColor.GRAY);
    }
    else {
      return SimpleTextAttributes.REGULAR_ATTRIBUTES;
    }
  }

  private final ColumnInfo<MapInfo, String> VCS_SETTING = new ColumnInfo<>(VcsBundle.message("column.name.configure.vcses.vcs")) {
    @Override
    public String valueOf(MapInfo object) {
      return object.mapping.getVcs();
    }

    @Override
    public boolean isCellEditable(MapInfo info) {
      return info != MapInfo.SEPARATOR && info.type != MapInfo.Type.UNREGISTERED;
    }

    @Override
    public void setValue(MapInfo o, String aValue) {
      Collection<AbstractVcs> activeVcses = getActiveVcses();
      o.mapping.setVcs(aValue);
      checkNotifyListeners(activeVcses);
    }

    @Override
    public TableCellRenderer getRenderer(final MapInfo info) {
      return new ColoredTableCellRenderer() {
        @Override
        protected void customizeCellRenderer(JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
          if (info == MapInfo.SEPARATOR) {
            if (!selected) {
              setBackground(getUnregisteredRootBackground());
            }
            return;
          }

          if (info.type == MapInfo.Type.UNREGISTERED && !selected) {
            setBackground(getUnregisteredRootBackground());
          }

          String vcsName = info.mapping.getVcs();
          LocalizeValue text;
          if (vcsName.length() == 0) {
            text = VcsLocalize.noneVcsPresentation();
          }
          else {
            VcsDescriptor vcs = myAllVcss.get(vcsName);
            if (vcs != null) {
              text = vcs.getDisplayName();
            }
            else {
              text = VcsLocalize.unknownVcsPresentation(vcsName);
            }
          }
          append(text.get(), getAttributes(info));
        }
      };
    }

    @Override
    public TableCellEditor getEditor(MapInfo o) {
      return new AbstractTableCellEditor() {
        @Override
        public Object getCellEditorValue() {
          VcsDescriptor selectedVcs = (VcsDescriptor)myVcsComboBox.getSelectedItem();
          return ((selectedVcs == null) || selectedVcs.isNone()) ? "" : selectedVcs.getId();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
          String vcsName = (String)value;
          myVcsComboBox.setSelectedItem(myAllVcss.get(vcsName));
          return myVcsComboBox;
        }
      };
    }

    @Nullable
    @Override
    public String getMaxStringValue() {
      String maxString = null;
      for (String name : myAllVcss.keySet()) {
        if (maxString == null || maxString.length() < name.length()) {
          maxString = name;
        }
      }
      return maxString;
    }

    @Override
    public int getAdditionalWidth() {
      return DEFAULT_HGAP;
    }
  };

  public VcsDirectoryConfigurationPanel(Project project, Disposable uiDisposable) {
    myProject = project;
    myVcsConfiguration = getInstance(myProject);
    myProjectMessage = XmlStringUtil
            .wrapInHtml(StringUtil.escapeXml(VcsDirectoryMapping.PROJECT_CONSTANT) + " - " + DefaultVcsRootPolicy.getInstance(myProject).getProjectConfigurationMessage(myProject).replace('\n', ' '));
    myIsDisabled = myProject.isDefault();
    myVcsManager = ProjectLevelVcsManager.getInstance(project);
    VcsDescriptor[] vcsDescriptors = myVcsManager.getAllVcss();
    myAllVcss = new HashMap<>();
    for (VcsDescriptor vcsDescriptor : vcsDescriptors) {
      myAllVcss.put(vcsDescriptor.getId(), vcsDescriptor);
    }

    myDirectoryMappingTable = new TableView<>();
    myDirectoryMappingTable.setIntercellSpacing(JBUI.emptySize());

    myBaseRevisionTexts = new JCheckBox("Store on shelf base revision texts for files under DVCS");
    myLimitHistory = new VcsLimitHistoryConfigurable(myProject);
    myScopeFilterConfig = new VcsUpdateInfoScopeFilterConfigurable(myProject, myVcsConfiguration);

    myCheckers = new HashMap<>();
    updateRootCheckers();

    setLayout(new BorderLayout());
    add(createMainComponent(uiDisposable));

    myDirectoryRenderer = new MyDirectoryRenderer(myProject);
    DIRECTORY = new ColumnInfo<>(VcsBundle.message("column.info.configure.vcses.directory")) {
      @Override
      public MapInfo valueOf(MapInfo mapping) {
        return mapping;
      }

      @Override
      public TableCellRenderer getRenderer(MapInfo vcsDirectoryMapping) {
        return myDirectoryRenderer;
      }
    };
    initializeModel();

    myVcsComboBox.setModel(buildVcsWrappersModel(myProject));
    myVcsComboBox.addItemListener(e -> {
      if (myDirectoryMappingTable.isEditing()) {
        myDirectoryMappingTable.stopEditing();
      }
    });

    myDirectoryMappingTable.setRowHeight(myVcsComboBox.getPreferredSize().height);
    if (myIsDisabled) {
      myDirectoryMappingTable.setEnabled(false);
    }
  }

  private void updateRootCheckers() {
    myCheckers.clear();
    List<VcsRootChecker> checkers = VcsRootChecker.EXTENSION_POINT_NAME.getExtensionList();
    for (VcsRootChecker checker : checkers) {
      VcsKey key = checker.getSupportedVcs();
      AbstractVcs vcs = myVcsManager.findVcsByName(key.getName());
      if (vcs == null) {
        continue;
      }
      myCheckers.put(key.getName(), checker);
    }
  }

  private void initializeModel() {
    List<MapInfo> mappings = new ArrayList<>();
    for (VcsDirectoryMapping mapping : ProjectLevelVcsManager.getInstance(myProject).getDirectoryMappings()) {
      mappings.add(MapInfo.registered(new VcsDirectoryMapping(mapping.getDirectory(), mapping.getVcs(), mapping.getRootSettings()), isMappingValid(mapping)));
    }

    Collection<VcsRootError> errors = findUnregisteredRoots();
    if (!errors.isEmpty()) {
      mappings.add(MapInfo.SEPARATOR);
      for (VcsRootError error : errors) {
        mappings.add(MapInfo.unregistered(error.getMapping()));
      }
    }

    myModel = new ListTableModel<>(new ColumnInfo[]{DIRECTORY, VCS_SETTING}, mappings, 0);
    myDirectoryMappingTable.setModelAndUpdateColumns(myModel);

    myRecentlyChangedConfigurable.reset();
    myLimitHistory.reset();
    myScopeFilterConfig.reset();
    myBaseRevisionTexts.setSelected(myVcsConfiguration.INCLUDE_TEXT_INTO_SHELF);
    myShowChangedRecursively.setSelected(myVcsConfiguration.SHOW_DIRTY_RECURSIVELY);
    myCommitMessageMarginConfigurable.reset();
    myShowUnversionedFiles.setSelected(myVcsConfiguration.SHOW_UNVERSIONED_FILES_WHILE_COMMIT);
    myCheckCommitMessageSpelling.setSelected(myVcsConfiguration.CHECK_COMMIT_MESSAGE_SPELLING);
  }

  @Nonnull
  private Collection<VcsRootError> findUnregisteredRoots() {
    return ContainerUtil.filter(VcsRootErrorsFinder.getInstance(myProject).find(),
                                error -> error.getType() == VcsRootError.Type.UNREGISTERED_ROOT);
  }

  private boolean isMappingValid(@Nonnull VcsDirectoryMapping mapping) {
    String vcs = mapping.getVcs();
    VcsRootChecker checker = myCheckers.get(vcs);
    return checker == null || (mapping.isDefaultMapping() ? checker.isRoot(myProject.getBasePath()) : checker.isRoot(mapping.getDirectory()));
  }

  public static DefaultComboBoxModel buildVcsWrappersModel(Project project) {
    VcsDescriptor[] vcsDescriptors = ProjectLevelVcsManager.getInstance(project).getAllVcss();
    VcsDescriptor[] result = new VcsDescriptor[vcsDescriptors.length + 1];
    result[0] = VcsDescriptor.createFictive();
    System.arraycopy(vcsDescriptors, 0, result, 1, vcsDescriptors.length);
    return new DefaultComboBoxModel(result);
  }

  private void addMapping() {
    VcsMappingConfigurationDialog dlg = new VcsMappingConfigurationDialog(myProject, VcsBundle.message("directory.mapping.add.title"));
    // due to wonderful UI designer bug
    dlg.initProjectMessage();
    if (dlg.showAndGet()) {
      addMapping(dlg.getMapping());
    }
  }

  private void addMapping(VcsDirectoryMapping mapping) {
    List<MapInfo> items = new ArrayList<>(myModel.getItems());
    items.add(MapInfo.registered(new VcsDirectoryMapping(mapping.getDirectory(), mapping.getVcs(), mapping.getRootSettings()), isMappingValid(mapping)));
    Collections.sort(items, MapInfo.COMPARATOR);
    myModel.setItems(items);
    checkNotifyListeners(getActiveVcses());
  }


  private void addSelectedUnregisteredMappings(List<MapInfo> infos) {
    List<MapInfo> items = new ArrayList<>(myModel.getItems());
    for (MapInfo info : infos) {
      items.remove(info);
      items.add(MapInfo.registered(info.mapping, isMappingValid(info.mapping)));
    }
    sortAndAddSeparatorIfNeeded(items);
    myModel.setItems(items);
    checkNotifyListeners(getActiveVcses());
  }

  @Contract(pure = false)
  private static void sortAndAddSeparatorIfNeeded(@Nonnull List<MapInfo> items) {
    boolean hasUnregistered = false;
    boolean hasSeparator = false;
    for (MapInfo item : items) {
      if (item.type == MapInfo.Type.UNREGISTERED) {
        hasUnregistered = true;
      }
      else if (item.type == MapInfo.Type.SEPARATOR) {
        hasSeparator = true;
      }
    }
    if (!hasUnregistered && hasSeparator) {
      items.remove(MapInfo.SEPARATOR);
    }
    else if (hasUnregistered && !hasSeparator) {
      items.add(MapInfo.SEPARATOR);
    }
    Collections.sort(items, MapInfo.COMPARATOR);
  }

  private void editMapping() {
    VcsMappingConfigurationDialog dlg = new VcsMappingConfigurationDialog(myProject, VcsBundle.message("directory.mapping.remove.title"));
    int row = myDirectoryMappingTable.getSelectedRow();
    VcsDirectoryMapping mapping = myDirectoryMappingTable.getRow(row).mapping;
    dlg.setMapping(mapping);
    if (dlg.showAndGet()) {
      setMapping(row, dlg.getMapping());
    }
  }

  private void setMapping(int row, @Nonnull VcsDirectoryMapping mapping) {
    List<MapInfo> items = new ArrayList<>(myModel.getItems());
    items.set(row, MapInfo.registered(mapping, isMappingValid(mapping)));
    Collections.sort(items, MapInfo.COMPARATOR);
    myModel.setItems(items);
    checkNotifyListeners(getActiveVcses());
  }

  private void removeMapping() {
    Collection<AbstractVcs> activeVcses = getActiveVcses();
    ArrayList<MapInfo> mappings = new ArrayList<>(myModel.getItems());
    int index = myDirectoryMappingTable.getSelectionModel().getMinSelectionIndex();
    Collection<MapInfo> selection = myDirectoryMappingTable.getSelection();
    mappings.removeAll(selection);

    Collection<MapInfo> removedValidRoots = ContainerUtil.mapNotNull(selection, info -> info.type == MapInfo.Type.NORMAL && myCheckers.get(info.mapping.getVcs()) != null ? MapInfo
            .unregistered(info.mapping.getDirectory(), info.mapping.getVcs()) : null);
    mappings.addAll(removedValidRoots);
    sortAndAddSeparatorIfNeeded(mappings);

    myModel.setItems(mappings);
    if (mappings.size() > 0) {
      if (index >= mappings.size()) {
        index = mappings.size() - 1;
      }
      myDirectoryMappingTable.getSelectionModel().setSelectionInterval(index, index);
    }
    checkNotifyListeners(activeVcses);
  }

  @RequiredUIAccess
  protected JComponent createMainComponent(Disposable uiDisposable) {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBag gb = new GridBag().setDefaultInsets(JBUI.insets(0, 0, DEFAULT_VGAP, DEFAULT_HGAP)).setDefaultWeightX(1).setDefaultFill(GridBagConstraints.HORIZONTAL);

    panel.add(createMappingsTable(), gb.nextLine().next().fillCell().weighty(1.0));
    panel.add(createProjectMappingDescription(), gb.nextLine().next());
    panel.add(myLimitHistory.createComponent(uiDisposable), gb.nextLine().next());
    panel.add(createShowRecursivelyDirtyOption(), gb.nextLine().next());
    panel.add(createStoreBaseRevisionOption(), gb.nextLine().next());
    panel.add(createShowChangedOption(uiDisposable), gb.nextLine().next());
    panel.add(myScopeFilterConfig.createComponent(uiDisposable), gb.nextLine().next());
    panel.add(createUseCommitMessageRightMargin(uiDisposable), gb.nextLine().next().fillCellHorizontally());
    createShowUnversionedFilesOption();
    if (Registry.is("vcs.unversioned.files.in.commit")) {
      panel.add(myShowUnversionedFiles, gb.nextLine().next());
    }
    panel.add(createCheckCommitMessageSpelling(), gb.nextLine().next());
    return panel;
  }

  private JComponent createMappingsTable() {
    JPanel panelForTable = ToolbarDecorator.createDecorator(myDirectoryMappingTable, null).setAddAction(button -> {
      if (onlyRegisteredRootsInSelection()) {
        addMapping();
      }
      else {
        addSelectedUnregisteredMappings(getSelectedUnregisteredRoots());
      }
      updateRootCheckers();
    }).setEditAction(button -> {
      editMapping();
      updateRootCheckers();
    }).setRemoveAction(button -> {
      removeMapping();
      updateRootCheckers();
    }).setAddActionUpdater(e -> !myIsDisabled && rootsOfOneKindInSelection()).setEditActionUpdater(e -> !myIsDisabled && onlyRegisteredRootsInSelection())
            .setRemoveActionUpdater(e -> !myIsDisabled && onlyRegisteredRootsInSelection()).disableUpDownActions().createPanel();
    panelForTable.setPreferredSize(new Dimension(-1, 200));
    return panelForTable;
  }

  @Nonnull
  private List<MapInfo> getSelectedUnregisteredRoots() {
    return ContainerUtil.filter(myDirectoryMappingTable.getSelection(), info -> info.type == MapInfo.Type.UNREGISTERED);
  }

  private boolean rootsOfOneKindInSelection() {
    Collection<MapInfo> selection = myDirectoryMappingTable.getSelection();
    if (selection.isEmpty()) {
      return true;
    }
    if (selection.size() == 1 && selection.iterator().next().type == MapInfo.Type.SEPARATOR) {
      return false;
    }
    List<MapInfo> selectedRegisteredRoots = getSelectedRegisteredRoots();
    return selectedRegisteredRoots.size() == selection.size() || selectedRegisteredRoots.size() == 0;
  }

  @Nonnull
  private List<MapInfo> getSelectedRegisteredRoots() {
    Collection<MapInfo> selection = myDirectoryMappingTable.getSelection();
    return ContainerUtil.filter(selection, info -> info.type == MapInfo.Type.NORMAL || info.type == MapInfo.Type.INVALID);
  }

  private boolean onlyRegisteredRootsInSelection() {
    return getSelectedRegisteredRoots().size() == myDirectoryMappingTable.getSelection().size();
  }

  private JComponent createProjectMappingDescription() {
    JBLabel label = new JBLabel(myProjectMessage);
    label.setComponentStyle(UIUtil.ComponentStyle.SMALL);
    label.setFontColor(UIUtil.FontColor.BRIGHTER);
    label.setBorder(JBUI.Borders.empty(2, 5, 2, 0));
    return label;
  }

  private JComponent createStoreBaseRevisionOption() {
    JBLabel noteLabel = new JBLabel("File texts bigger than " + ourMaximumFileForBaseRevisionSize / 1000 + "K are not stored");
    noteLabel.setComponentStyle(UIUtil.ComponentStyle.SMALL);
    noteLabel.setFontColor(UIUtil.FontColor.BRIGHTER);
    noteLabel.setBorder(JBUI.Borders.empty(2, 25, 5, 0));

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(myBaseRevisionTexts, BorderLayout.NORTH);
    panel.add(noteLabel, BorderLayout.SOUTH);
    return panel;
  }

  @RequiredUIAccess
  private JComponent createShowChangedOption(Disposable uiDisposable) {
    myRecentlyChangedConfigurable = new VcsContentAnnotationConfigurable(myProject);
    JComponent component = myRecentlyChangedConfigurable.createComponent(uiDisposable);
    assert component != null;
    return component;
  }

  @RequiredUIAccess
  private JComponent createUseCommitMessageRightMargin(Disposable uiDisposable) {
    myCommitMessageMarginConfigurable = new VcsCommitMessageMarginConfigurable(myProject, myVcsConfiguration);
    return myCommitMessageMarginConfigurable.createComponent(uiDisposable);
  }

  private JComponent createShowRecursivelyDirtyOption() {
    myShowChangedRecursively = new JCheckBox("Show directories with changed descendants", myVcsConfiguration.SHOW_DIRTY_RECURSIVELY);
    return myShowChangedRecursively;
  }

  @Nonnull
  private JComponent createShowUnversionedFilesOption() {
    myShowUnversionedFiles = new JCheckBox("Show unversioned files in Commit dialog", myVcsConfiguration.SHOW_UNVERSIONED_FILES_WHILE_COMMIT);
    return myShowUnversionedFiles;
  }

  @Nonnull
  private JComponent createCheckCommitMessageSpelling() {
    myCheckCommitMessageSpelling = new JBCheckBox("Check commit message spelling", myVcsConfiguration.CHECK_COMMIT_MESSAGE_SPELLING);
    return myCheckCommitMessageSpelling;
  }

  public void reset() {
    initializeModel();
  }

  public void apply() throws ConfigurationException {
    myVcsManager.setDirectoryMappings(getModelMappings());
    myRecentlyChangedConfigurable.apply();
    myLimitHistory.apply();
    myScopeFilterConfig.apply();
    myVcsConfiguration.INCLUDE_TEXT_INTO_SHELF = myBaseRevisionTexts.isSelected();
    myVcsConfiguration.SHOW_DIRTY_RECURSIVELY = myShowChangedRecursively.isSelected();
    myCommitMessageMarginConfigurable.apply();
    myVcsConfiguration.SHOW_UNVERSIONED_FILES_WHILE_COMMIT = myShowUnversionedFiles.isSelected();
    myVcsConfiguration.CHECK_COMMIT_MESSAGE_SPELLING = myCheckCommitMessageSpelling.isSelected();
    initializeModel();
  }

  public boolean isModified() {
    if (myRecentlyChangedConfigurable.isModified()) return true;
    if (myLimitHistory.isModified()) return true;
    if (myScopeFilterConfig.isModified()) return true;
    if (myVcsConfiguration.INCLUDE_TEXT_INTO_SHELF != myBaseRevisionTexts.isSelected()) return true;
    if (myVcsConfiguration.SHOW_DIRTY_RECURSIVELY != myShowChangedRecursively.isSelected()) {
      return true;
    }
    if (myCommitMessageMarginConfigurable.isModified()) {
      return true;
    }
    if (myVcsConfiguration.SHOW_UNVERSIONED_FILES_WHILE_COMMIT != myShowUnversionedFiles.isSelected()) {
      return true;
    }
    if (myVcsConfiguration.CHECK_COMMIT_MESSAGE_SPELLING != myCheckCommitMessageSpelling.isSelected()) {
      return true;
    }
    return !getModelMappings().equals(myVcsManager.getDirectoryMappings());
  }

  @Nonnull
  private List<VcsDirectoryMapping> getModelMappings() {
    return ContainerUtil.mapNotNull(myModel.getItems(), info -> info == MapInfo.SEPARATOR || info.type == MapInfo.Type.UNREGISTERED ? null : info.mapping);
  }

  public void addVcsListener(ModuleVcsListener moduleVcsListener) {
    myListeners.add(moduleVcsListener);
  }

  public void removeVcsListener(ModuleVcsListener moduleVcsListener) {
    myListeners.remove(moduleVcsListener);
  }

  private void checkNotifyListeners(Collection<AbstractVcs> oldVcses) {
    Collection<AbstractVcs> vcses = getActiveVcses();
    if (!vcses.equals(oldVcses)) {
      for (ModuleVcsListener listener : myListeners) {
        listener.activeVcsSetChanged(vcses);
      }
    }
  }

  public Collection<AbstractVcs> getActiveVcses() {
    Set<AbstractVcs> vcses = new HashSet<>();
    for (VcsDirectoryMapping mapping : getModelMappings()) {
      if (mapping.getVcs().length() > 0) {
        vcses.add(myVcsManager.findVcsByName(mapping.getVcs()));
      }
    }
    return vcses;
  }

  @RequiredUIAccess
  public void disposeUIResources() {
    myScopeFilterConfig.disposeUIResources();
  }
}
