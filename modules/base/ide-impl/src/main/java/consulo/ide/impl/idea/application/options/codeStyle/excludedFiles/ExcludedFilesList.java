// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.application.options.codeStyle.excludedFiles;

import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.content.scope.PackageSet;
import consulo.ide.impl.idea.ide.util.scopeChooser.EditScopesDialog;
import consulo.language.codeStyle.CodeStyleScheme;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.fileSet.FileSetDescriptor;
import consulo.language.codeStyle.fileSet.NamedScopeDescriptor;
import consulo.language.codeStyle.ui.setting.CodeStyleSchemesModel;
import consulo.language.editor.packageDependency.DependencyValidationManager;
import consulo.language.editor.scope.NamedScopeManager;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.AnActionButton;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.ToolbarDecorator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExcludedFilesList extends JBList<FileSetDescriptor> {

  private final ToolbarDecorator myFileListDecorator;
  private DefaultListModel<FileSetDescriptor> myModel;
  @Nullable
  private CodeStyleSchemesModel mySchemesModel;

  public ExcludedFilesList() {
    super();
    myFileListDecorator = ToolbarDecorator.createDecorator(this)
      .setAddAction(button -> addDescriptor())
      .setRemoveAction(button -> removeDescriptor())
      .setEditAction(button -> editDescriptor()).disableUpDownActions();

    addListSelectionListener(e -> onSelectionChange());
  }

  public void initModel() {
    myModel = createDefaultListModel(new FileSetDescriptor[0]);
    setModel(myModel);
  }

  private void onSelectionChange() {
    int i = getSelectedIndex();
    AnActionButton removeButton = ToolbarDecorator.findRemoveButton(myFileListDecorator.getActionsPanel());
    removeButton.setEnabled(i >= 0);
  }

  public void reset(@Nonnull CodeStyleSettings settings) {
    myModel.clear();
    for (FileSetDescriptor descriptor : settings.getExcludedFiles().getDescriptors()) {
      myModel.addElement(descriptor);
    }
  }

  public void apply(@Nonnull CodeStyleSettings settings) {
    settings.getExcludedFiles().clear();
    for (int i = 0; i < myModel.getSize(); i++) {
      settings.getExcludedFiles().addDescriptor(myModel.get(i));
    }
  }

  public boolean isModified(@Nonnull CodeStyleSettings settings) {
    if (myModel.size() != settings.getExcludedFiles().getDescriptors().size()) return true;
    for (int i = 0; i < myModel.getSize(); i++) {
      if (!myModel.get(i).equals(settings.getExcludedFiles().getDescriptors().get(i))) {
        return true;
      }
    }
    return false;
  }

  public ToolbarDecorator getDecorator() {
    return myFileListDecorator;
  }

  @RequiredUIAccess
  private void addDescriptor() {
    assert mySchemesModel != null;
    List<NamedScope> availableScopes = getAvailableScopes();
    if (!availableScopes.isEmpty()) {
      ExcludedFilesScopeDialog dialog = new ExcludedFilesScopeDialog(mySchemesModel.getProject(), availableScopes);
      dialog.show();
      if (dialog.isOK()) {
        FileSetDescriptor descriptor = dialog.getDescriptor();
        if (descriptor != null) {
          int insertAt = getSelectedIndex() < 0 ? getItemsCount() : getSelectedIndex() + 1;
          int exiting = myModel.indexOf(descriptor);
          if (exiting < 0) {
            myModel.add(insertAt, descriptor);
            setSelectedValue(descriptor, true);
          }
          else {
            setSelectedValue(myModel.get(exiting), true);
          }
        }
      }
      else if (dialog.getExitCode() == ExcludedFilesScopeDialog.EDIT_SCOPES) {
        editScope(null);
      }
    }
    else {
      editScope(null);
    }
  }

  private List<NamedScope> getAvailableScopes() {
    Set<String> usedNames = getUsedScopeNames();
    List<NamedScope> namedScopes = new ArrayList<>();
    for (NamedScopesHolder holder : getScopeHolders()) {
      for (NamedScope scope : holder.getEditableScopes()) {
        if (!usedNames.contains(scope.getName())) {
          namedScopes.add(scope);
        }
      }
    }
    return namedScopes;
  }

  private Set<String> getUsedScopeNames() {
    Set<String> usedScopeNames = new HashSet<>();
    for (int i = 0; i < myModel.size(); i++) {
      FileSetDescriptor descriptor = myModel.get(i);
      if (descriptor instanceof NamedScopeDescriptor) {
        usedScopeNames.add(descriptor.getName());
      }
    }
    return usedScopeNames;
  }

  private void removeDescriptor() {
    int i = getSelectedIndex();
    if (i >= 0) {
      myModel.remove(i);
    }
  }

  @SuppressWarnings("unused")
  private void editDescriptor() {
    int i = getSelectedIndex();
    FileSetDescriptor selectedDescriptor = i >= 0 ? myModel.get(i) : null;
    if (selectedDescriptor instanceof NamedScopeDescriptor namedScopeDescriptor) {
      ensureScopeExists(namedScopeDescriptor);
      editScope(selectedDescriptor.getName());
    }
    else {
      editScope(null);
    }
  }

  public void setSchemesModel(@Nonnull CodeStyleSchemesModel schemesModel) {
    mySchemesModel = schemesModel;
  }

  public void editScope(@Nullable String selectedName) {
    assert mySchemesModel != null;
    EditScopesDialog scopesDialog = EditScopesDialog.showDialog(getScopeHolderProject(), selectedName);
    if (scopesDialog.isOK()) {
      NamedScope scope = scopesDialog.getSelectedScope();
      if (scope != null) {
        String newName = scope.getName();
        FileSetDescriptor newDesciptor = null;
        if (selectedName == null) {
          newDesciptor = findDescriptor(newName);
          if (newDesciptor == null) {
            newDesciptor = new NamedScopeDescriptor(scope);
            myModel.addElement(newDesciptor);
          }
        }
        else {
          FileSetDescriptor oldDescriptor = findDescriptor(selectedName);
          if (!selectedName.equals(newName)) {
            int index = myModel.indexOf(oldDescriptor);
            myModel.removeElement(oldDescriptor);
            newDesciptor = findDescriptor(newName);
            if (newDesciptor == null) {
              newDesciptor = new NamedScopeDescriptor(scope);
              myModel.add(index, newDesciptor);
            }
          }
          else if (oldDescriptor != null) {
            PackageSet fileSet = scope.getValue();
            oldDescriptor.setPattern(fileSet != null ? fileSet.getText() : null);
          }
        }
        if (newDesciptor != null) {
          setSelectedValue(newDesciptor, true);
        }
      }
    }
  }

  private void ensureScopeExists(@Nonnull NamedScopeDescriptor descriptor) {
    List<NamedScopesHolder> holders = getScopeHolders();
    for (NamedScopesHolder holder : holders) {
      if (holder.getScope(descriptor.getName()) != null) return;
    }
    NamedScopesHolder projectScopeHolder = DependencyValidationManager.getInstance(getScopeHolderProject());
    NamedScope newScope = projectScopeHolder.createScope(descriptor.getName(), descriptor.getFileSet());
    projectScopeHolder.addScope(newScope);
  }

  private Project getScopeHolderProject() {
    assert mySchemesModel != null;
    CodeStyleScheme scheme = mySchemesModel.getSelectedScheme();
    return mySchemesModel.isProjectScheme(scheme) ? mySchemesModel.getProject() : ProjectManager.getInstance().getDefaultProject();
  }

  @Nullable
  private FileSetDescriptor findDescriptor(@Nonnull String name) {
    for (int i = 0; i < myModel.size(); i++) {
      if (name.equals(myModel.get(i).getName())) return myModel.get(i);
    }
    return null;
  }

  private List<NamedScopesHolder> getScopeHolders() {
    List<NamedScopesHolder> holders = new ArrayList<>();
    Project project = getScopeHolderProject();
    holders.add(DependencyValidationManager.getInstance(project));
    holders.add(NamedScopeManager.getInstance(project));
    return holders;
  }
}
