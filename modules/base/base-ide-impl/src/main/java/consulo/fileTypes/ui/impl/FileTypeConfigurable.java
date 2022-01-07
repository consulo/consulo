/*
 * Copyright 2013-2021 consulo.io
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
package consulo.fileTypes.ui.impl;

import com.intellij.CommonBundle;
import com.intellij.ide.highlighter.custom.SyntaxTable;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.fileTypes.*;
import com.intellij.openapi.fileTypes.impl.*;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.templateLanguages.TemplateDataLanguagePatterns;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.util.PairConvertor;
import com.intellij.util.ui.JBUI;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

/**
 * @author VISTALL
 * @since 04/12/2021
 */
public class FileTypeConfigurable implements SearchableConfigurable, Configurable.NoScroll, Configurable.NoMargin {
  public static class RecognizedFileTypes extends Wrapper {
    private final JList<FileType> myFileTypesList;
    private final MySpeedSearch mySpeedSearch;
    private FileTypeConfigurable myController;

    public RecognizedFileTypes() {
      myFileTypesList = new JBList<>(new DefaultListModel<>());
      myFileTypesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      myFileTypesList.setCellRenderer(new FileTypeRenderer(() -> {
        ArrayList<FileType> result = new ArrayList<>();
        for (int i = 0; i < myFileTypesList.getModel().getSize(); i++) {
          result.add(myFileTypesList.getModel().getElementAt(i));
        }
        return result;
      }));

      new DoubleClickListener() {
        @Override
        protected boolean onDoubleClick(MouseEvent e) {
          myController.editFileType();
          return true;
        }
      }.installOn(myFileTypesList);

      ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(myFileTypesList).setToolbarPosition(ActionToolbarPosition.TOP).setAddAction(button -> myController.addFileType())
              .setRemoveAction(button -> myController.removeFileType()).setEditAction(button -> myController.editFileType()).setEditActionUpdater(e -> {
                final FileType fileType = getSelectedFileType();
                return canBeModified(fileType);
              }).setRemoveActionUpdater(e -> {
                final FileType fileType = getSelectedFileType();
                return canBeModified(fileType);
              }).disableUpDownActions().setPanelBorder(JBUI.Borders.empty());

      setContent(toolbarDecorator.createPanel());

      mySpeedSearch = new MySpeedSearch(myFileTypesList);
    }

    private static class MySpeedSearch extends MultipleTraitsListSpeedSearch {
      private FileTypeConfigurable myController;
      private Object myCurrentType;
      private String myExtension;

      private MySpeedSearch(JList component) {
        super(component, new ArrayList<>());
        initConvertors();
      }

      @Override
      protected void selectElement(Object element, String selectedText) {
        super.selectElement(element, selectedText);
        if (myCurrentType != null && myCurrentType.equals(element) && myController != null) {
          myController.myPatterns.select(myExtension);
        }
      }

      private void initConvertors() {
        final PairConvertor<Object, String, Boolean> simpleConvertor = (element, s) -> {
          String value = element.toString();
          if (element instanceof FileType) {
            value = ((FileType)element).getDescription().get();
          }
          return getComparator().matchingFragments(s, value) != null;
        };
        final PairConvertor<Object, String, Boolean> byExtensionsConvertor = (element, s) -> {
          if (element instanceof FileType && myCurrentType != null) {
            return myCurrentType.equals(element);
          }
          return false;
        };
        myOrderedConvertors.add(simpleConvertor);
        myOrderedConvertors.add(byExtensionsConvertor);
      }

      @Override
      protected void onSearchFieldUpdated(String s) {
        if (myController == null || myController.myTempPatternsTable == null) return;
        int index = s.lastIndexOf('.');
        if (index < 0) {
          s = "." + s;
        }
        myCurrentType = myController.myTempPatternsTable.findAssociatedFileType(s);
        if (myCurrentType != null) {
          myExtension = s;
        }
        else {
          myExtension = null;
        }
      }
    }


    public void attachActions(final FileTypeConfigurable controller) {
      myController = controller;
      mySpeedSearch.myController = controller;
    }

    public FileType getSelectedFileType() {
      return myFileTypesList.getSelectedValue();
    }

    public JComponent getComponent() {
      return this;
    }

    public void setFileTypes(FileType[] types) {
      DefaultListModel<FileType> listModel = (DefaultListModel)myFileTypesList.getModel();
      listModel.clear();
      for (FileType type : types) {
        if (type != UnknownFileType.INSTANCE) {
          listModel.addElement(type);
        }
      }
      ScrollingUtil.ensureSelectionExists(myFileTypesList);
    }

    public int getSelectedIndex() {
      return myFileTypesList.getSelectedIndex();
    }

    public void setSelectionIndex(int selectedIndex) {
      myFileTypesList.setSelectedIndex(selectedIndex);
    }

    public void selectFileType(FileType fileType) {
      myFileTypesList.setSelectedValue(fileType, true);
      IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myFileTypesList);
    }
  }

  private static class ExtensionRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      setText(" " + getText());
      return this;
    }
  }

  public static class PatternsPanel extends JPanel {
    private final JBList myPatternsList;
    private FileTypeConfigurable myController;

    public PatternsPanel() {
      super(new BorderLayout());
      myPatternsList = new JBList<>(new DefaultListModel());
      myPatternsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      myPatternsList.setCellRenderer(new ExtensionRenderer());
      myPatternsList.getEmptyText().setText(FileTypesBundle.message("filetype.settings.no.patterns"));

      add(ToolbarDecorator.createDecorator(myPatternsList).setToolbarPosition(ActionToolbarPosition.TOP).
              setAddAction(button -> myController.addPattern()).setEditAction(button -> myController.editPattern()).setRemoveAction(button -> myController.removePattern()).disableUpDownActions()
                  .setPanelBorder(JBUI.Borders.empty()).createPanel(), BorderLayout.CENTER);

      setBorder(IdeBorderFactory.createTitledBorder(FileTypesBundle.message("filetype.registered.patterns.group"), false));
    }

    public void attachActions(final FileTypeConfigurable controller) {
      myController = controller;
    }

    public JComponent getComponent() {
      return this;
    }

    public void clearList() {
      getListModel().clear();
      myPatternsList.clearSelection();
    }

    private DefaultListModel getListModel() {
      return (DefaultListModel)myPatternsList.getModel();
    }

    public void addPattern(String pattern) {
      getListModel().addElement(pattern);
    }

    public void ensureSelectionExists() {
      ScrollingUtil.ensureSelectionExists(myPatternsList);
    }

    public void addPatternAndSelect(String pattern) {
      addPattern(pattern);
      ScrollingUtil.selectItem(myPatternsList, getListModel().getSize() - 1);
    }

    public void select(final String pattern) {
      for (int i = 0; i < myPatternsList.getItemsCount(); i++) {
        final Object at = myPatternsList.getModel().getElementAt(i);
        if (at instanceof String) {
          final FileNameMatcher matcher = FileTypeManager.parseFromString((String)at);
          if (matcher.acceptsCharSequence(pattern)) {
            ScrollingUtil.selectItem(myPatternsList, i);
            return;
          }
        }
      }
    }

    public boolean isListEmpty() {
      return getListModel().isEmpty();
    }

    public String removeSelected() {
      Object selectedValue = myPatternsList.getSelectedValue();
      if (selectedValue == null) return null;
      ListUtil.removeSelectedItems(myPatternsList);
      return (String)selectedValue;
    }

    public String getDefaultExtension() {
      return (String)getListModel().getElementAt(0);
    }

    public String getSelectedItem() {
      return (String)myPatternsList.getSelectedValue();
    }
  }

  private static class TypeEditor<T extends UserFileType<T>> extends DialogWrapper {
    private final T myFileType;
    private final SettingsEditor<T> myEditor;

    public TypeEditor(Component parent, T fileType, final String title) {
      super(parent, false);
      myFileType = fileType;
      myEditor = fileType.getEditor();
      setTitle(title);
      init();
      Disposer.register(myDisposable, myEditor);
    }

    @Override
    protected void init() {
      super.init();
      myEditor.resetFrom(myFileType);
    }

    @Override
    protected JComponent createCenterPanel() {
      return myEditor.getComponent();
    }

    @Override
    protected void doOKAction() {
      try {
        myEditor.applyTo(myFileType);
      }
      catch (ConfigurationException e) {
        Messages.showErrorDialog(getContentPane(), e.getMessage(), e.getTitle());
        return;
      }
      super.doOKAction();
    }

    @Override
    protected String getHelpId() {
      return "reference.dialogs.newfiletype";
    }
  }

  private RecognizedFileTypes myRecognizedFileType;

  private PatternsPanel myPatterns;

  private final Map<UserFileType, UserFileType> myOriginalToEditedMap = new HashMap<>();

  private FileTypeAssocTable<FileType> myTempPatternsTable;

  private FileTypeAssocTable<Language> myTempTemplateDataLanguages;

  private Set<FileType> myTempFileTypes;

  private final Map<FileNameMatcher, FileType> myReassigned = new HashMap<>();

  private CollectionListModel<String> myIgnoredFilesModel = new CollectionListModel<>();

  private final FileTypeManagerImpl myManager;

  @Inject
  public FileTypeConfigurable(FileTypeManager fileTypeManager) {
    myManager = (FileTypeManagerImpl)fileTypeManager;
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public JComponent createComponent(@Nonnull Disposable parentDisposable) {
    TabbedPaneWrapper tabbedPaneWrapper = new TabbedPaneWrapper(parentDisposable);

    myRecognizedFileType = new RecognizedFileTypes();
    myRecognizedFileType.attachActions(this);
    myRecognizedFileType.myFileTypesList.addListSelectionListener(e -> updateExtensionList());

    myPatterns = new PatternsPanel();
    myPatterns.attachActions(this);

    OnePixelSplitter splitter = new OnePixelSplitter(false);
    splitter.setFirstComponent(myRecognizedFileType);
    splitter.setSecondComponent(myPatterns);

    JBList<String> ignoredFilesList = new JBList<>(myIgnoredFilesModel);
    ToolbarDecorator decorator = ToolbarDecorator.createDecorator(ignoredFilesList).setPanelBorder(JBUI.Borders.empty()).setToolbarPosition(ActionToolbarPosition.TOP).setAddAction(button -> {
      String newPattern = Messages.showInputDialog(ignoredFilesList, "Enter Ignore Pattern ('*' and '?' allowed):", "Ignore Pattern", null);
      if (newPattern != null) {
        myIgnoredFilesModel.add(newPattern);
      }
    }).setRemoveAction(button -> {
      String selectedValue = ignoredFilesList.getSelectedValue();
      if (selectedValue != null) {
        myIgnoredFilesModel.remove(selectedValue);
      }
    }).disableUpDownActions();

    tabbedPaneWrapper.addTab("Recognized File Types", splitter);
    tabbedPaneWrapper.addTab("Ignored Files and Folders", decorator.createPanel());

    return tabbedPaneWrapper.getComponent();
  }

  @RequiredUIAccess
  private void editFileType() {
    FileType fileType = myRecognizedFileType.getSelectedFileType();
    if (!canBeModified(fileType)) return;
    UserFileType ftToEdit = myOriginalToEditedMap.get(fileType);
    if (ftToEdit == null) ftToEdit = ((UserFileType)fileType).clone();
    TypeEditor editor = new TypeEditor(myRecognizedFileType.myFileTypesList, ftToEdit, FileTypesBundle.message("filetype.edit.existing.title"));
    editor.show();
    if (editor.isOK()) {
      myOriginalToEditedMap.put((UserFileType)fileType, ftToEdit);
    }
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    myTempPatternsTable = myManager.getExtensionMap().copy();
    myTempTemplateDataLanguages = TemplateDataLanguagePatterns.getInstance().getAssocTable();

    myTempFileTypes = new HashSet<>(getModifiableFileTypes());
    myOriginalToEditedMap.clear();

    updateFileTypeList();
    updateExtensionList();

    myIgnoredFilesModel.replaceAll(new ArrayList<>(myManager.getIgnoredFiles()));
  }

  private void removePattern() {
    FileType type = myRecognizedFileType.getSelectedFileType();
    if (type == null) return;
    String extension = myPatterns.removeSelected();
    if (extension == null) return;
    FileNameMatcher matcher = FileTypeManager.parseFromString(extension);

    myTempPatternsTable.removeAssociation(matcher, type);
    IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myPatterns.myPatternsList);
  }

  private void removeFileType() {
    FileType fileType = myRecognizedFileType.getSelectedFileType();
    if (fileType == null) return;
    myTempFileTypes.remove(fileType);
    myOriginalToEditedMap.remove(fileType);

    myTempPatternsTable.removeAllAssociations(fileType);

    updateFileTypeList();
    updateExtensionList();
  }

  private void updateFileTypeList() {
    FileType[] types = myTempFileTypes.toArray(new FileType[myTempFileTypes.size()]);
    Arrays.sort(types, (o1, o2) -> {
      FileType fileType1 = o1;
      FileType fileType2 = o2;
      return fileType1.getDescription().get().compareToIgnoreCase(fileType2.getDescription().get());
    });
    myRecognizedFileType.setFileTypes(types);
  }

  private static List<FileType> getModifiableFileTypes() {
    FileType[] registeredFileTypes = FileTypeManager.getInstance().getRegisteredFileTypes();
    ArrayList<FileType> result = new ArrayList<>();
    for (FileType fileType : registeredFileTypes) {
      if (!fileType.isReadOnly()) result.add(fileType);
    }
    return result;
  }

  private void addFileType() {
    //TODO: support adding binary file types...
    AbstractFileType type = new AbstractFileType(new SyntaxTable());
    TypeEditor<AbstractFileType> editor = new TypeEditor<>(myRecognizedFileType.myFileTypesList, type, FileTypesBundle.message("filetype.edit.new.title"));
    editor.show();
    if (editor.isOK()) {
      myTempFileTypes.add(type);
      updateFileTypeList();
      updateExtensionList();
      myRecognizedFileType.selectFileType(type);
    }
  }

  private void updateExtensionList() {
    FileType type = myRecognizedFileType.getSelectedFileType();
    if (type == null) return;
    List<String> extensions = new ArrayList<>();

    final List<FileNameMatcher> assocs = myTempPatternsTable.getAssociations(type);
    for (FileNameMatcher assoc : assocs) {
      extensions.add(assoc.getPresentableString());
    }

    myPatterns.clearList();
    Collections.sort(extensions);
    for (String extension : extensions) {
      myPatterns.addPattern(extension);
    }
    myPatterns.ensureSelectionExists();
  }

  private void addPattern() {
    editPattern(null);
  }

  private void editPattern() {
    final String item = myPatterns.getSelectedItem();
    if (item == null) return;

    editPattern(item);
  }

  private void editPattern(@Nullable final String item) {
    final FileType type = myRecognizedFileType.getSelectedFileType();
    if (type == null) return;

    final String title = item == null ? FileTypesBundle.message("filetype.edit.add.pattern.title") : FileTypesBundle.message("filetype.edit.edit.pattern.title");

    final Language oldLanguage = item == null ? null : myTempTemplateDataLanguages.findAssociatedFileType(item);
    final FileTypePatternDialog dialog = new FileTypePatternDialog(item, type, oldLanguage);
    final DialogBuilder builder = new DialogBuilder(myPatterns);
    builder.setPreferredFocusComponent(dialog.getPatternField());
    builder.setCenterPanel(dialog.getMainPanel());
    builder.setTitle(title);
    builder.showModal(true);
    if (builder.getDialogWrapper().isOK()) {
      final String pattern = dialog.getPatternField().getText();
      if (StringUtil.isEmpty(pattern)) return;

      final FileNameMatcher matcher = FileTypeManager.parseFromString(pattern);
      FileType registeredFileType = findExistingFileType(matcher);
      if (registeredFileType != null && registeredFileType != type) {
        if (registeredFileType.isReadOnly()) {
          Messages.showMessageDialog(myPatterns.myPatternsList, FileTypesBundle.message("filetype.edit.add.pattern.exists.error", registeredFileType.getDescription()), title, Messages.getErrorIcon());
          return;
        }
        else {
          if (Messages.OK ==
              Messages.showOkCancelDialog(myPatterns.myPatternsList, FileTypesBundle.message("filetype.edit.add.pattern.exists.message", registeredFileType.getDescription()),
                                          FileTypesBundle.message("filetype.edit.add.pattern.exists.title"), FileTypesBundle.message("filetype.edit.add.pattern.reassign.button"),
                                          CommonBundle.getCancelButtonText(), Messages.getQuestionIcon())) {
            myTempPatternsTable.removeAssociation(matcher, registeredFileType);
            if (oldLanguage != null) {
              myTempTemplateDataLanguages.removeAssociation(matcher, oldLanguage);
            }
            myReassigned.put(matcher, registeredFileType);
          }
          else {
            return;
          }
        }
      }

      if (item != null) {
        final FileNameMatcher oldMatcher = FileTypeManager.parseFromString(item);
        myTempPatternsTable.removeAssociation(oldMatcher, type);
        if (oldLanguage != null) {
          myTempTemplateDataLanguages.removeAssociation(oldMatcher, oldLanguage);
        }
      }
      myTempPatternsTable.addAssociation(matcher, type);
      Language templateDataLanguage = dialog.getTemplateDataLanguage();
      if (templateDataLanguage != null) {
        myTempTemplateDataLanguages.addAssociation(matcher, templateDataLanguage);
      }

      updateExtensionList();
      final int index = myPatterns.getListModel().indexOf(matcher.getPresentableString());
      if (index >= 0) {
        ScrollingUtil.selectItem(myPatterns.myPatternsList, index);
      }
      IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myPatterns.myPatternsList);
    }
  }

  private static boolean canBeModified(FileType fileType) {
    return fileType instanceof AbstractFileType;
  }

  @Nullable
  public FileType findExistingFileType(FileNameMatcher matcher) {
    FileType fileTypeByExtension = myTempPatternsTable.findAssociatedFileType(matcher);

    if (fileTypeByExtension != null && fileTypeByExtension != UnknownFileType.INSTANCE) {
      return fileTypeByExtension;
    }
    FileType registeredFileType = FileTypeManager.getInstance().getFileTypeByExtension(matcher.getPresentableString());
    if (registeredFileType != UnknownFileType.INSTANCE && registeredFileType.isReadOnly()) {
      return registeredFileType;
    }
    return null;
  }

  @Nonnull
  @Override
  public String getId() {
    return "preferences.fileType";
  }

  @Override
  public String getDisplayName() {
    return FileTypesBundle.message("filetype.settings.title");
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    Set<String> ignoredSet = new LinkedHashSet<>(myIgnoredFilesModel.getItems());
    if (!ignoredSet.equals(myManager.getIgnoredFiles())) {
      return true;
    }

    Set<FileType> types = new HashSet<>(getModifiableFileTypes());
    return !myTempPatternsTable.equals(myManager.getExtensionMap()) ||
           !myTempFileTypes.equals(types) ||
           !myOriginalToEditedMap.isEmpty() ||
           !myTempTemplateDataLanguages.equals(TemplateDataLanguagePatterns.getInstance().getAssocTable());
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    Set<UserFileType> modifiedUserTypes = myOriginalToEditedMap.keySet();
    for (UserFileType oldType : modifiedUserTypes) {
      UserFileType newType = myOriginalToEditedMap.get(oldType);
      oldType.copyFrom(newType);
    }
    myOriginalToEditedMap.clear();

    Set<String> ignoredSet = new LinkedHashSet<>(myIgnoredFilesModel.getItems());
    Application.get().runWriteAction(() -> {
      if (!ignoredSet.equals(myManager.getIgnoredFiles())) {
        myManager.setIgnoredFiles(ignoredSet);
      }
      myManager.setPatternsTable(myTempFileTypes, myTempPatternsTable);

      TemplateDataLanguagePatterns.getInstance().setAssocTable(myTempTemplateDataLanguages);
    });
  }
}
