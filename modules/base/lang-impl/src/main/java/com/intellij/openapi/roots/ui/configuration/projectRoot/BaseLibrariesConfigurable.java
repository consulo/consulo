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
package com.intellij.openapi.roots.ui.configuration.projectRoot;

import com.intellij.CommonBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.impl.libraries.LibraryImpl;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablePresentation;
import com.intellij.openapi.roots.ui.configuration.LibraryTableModifiableModelProvider;
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.CreateNewLibraryAction;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.LibraryEditorListener;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.LibraryProjectStructureElement;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import com.intellij.openapi.ui.MasterDetailsStateService;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.NonEmptyInputValidator;
import com.intellij.openapi.util.Comparing;
import com.intellij.util.ui.tree.TreeUtil;
import consulo.ide.settings.impl.ProjectStructureSettingsUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.roots.ui.configuration.LibrariesConfigurator;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.preferences.MasterDetailsConfigurable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.*;

public abstract class BaseLibrariesConfigurable extends BaseStructureConfigurable {
  @Nonnull
  protected final Project myProject;
  @Nonnull
  protected String myLevel;

  protected BaseLibrariesConfigurable(final @Nonnull Project project) {
    myProject = project;
  }

  @RequiredUIAccess
  @Override
  public void initialize() {
    getLibrariesConfigurator().addLibraryEditorListener(new LibraryEditorListener() {
      @Override
      public void libraryCreated(@Nonnull Library library) {
        createLibraryNode(library);
      }

      @Override
      public void libraryRenamed(@Nonnull Library library, String oldName, String newName) {

      }
    });
  }

  @Nullable
  @Override
  protected MasterDetailsStateService getStateService() {
    return MasterDetailsStateService.getInstance(myProject);
  }

  public abstract LibraryTablePresentation getLibraryTablePresentation();

  @Override
  protected void processRemovedItems() {
  }

  @Override
  protected boolean wasObjectStored(final Object editableObject) {
    return false;
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    boolean isModified = false;
    for (final LibraryTable.ModifiableModel provider : getLibrariesConfigurator().getModels()) {
      isModified |= provider.isChanged();
    }
    return isModified || super.isModified();
  }

  @Override
  public void checkCanApply() throws ConfigurationException {
    super.checkCanApply();
    for (LibraryConfigurable configurable : getLibraryConfigurables()) {
      if (configurable.getDisplayName().isEmpty()) {
        ((LibraryProjectStructureElement)configurable.getProjectStructureElement()).navigate(myProject);
        throw new ConfigurationException("Library name is not specified");
      }
    }
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    super.reset();
    myTree.setRootVisible(false);
  }

  @Override
  protected void loadTree() {
    createLibrariesNode(getLibrariesConfigurator().createModifiableModelProvider(myLevel));
  }

  @Nonnull
  @Override
  protected Collection<? extends ProjectStructureElement> getProjectStructureElements() {
    final List<ProjectStructureElement> result = new ArrayList<>();
    for (LibraryConfigurable libraryConfigurable : getLibraryConfigurables()) {
      result.add(new LibraryProjectStructureElement(libraryConfigurable.getEditableObject()));
    }
    return result;
  }

  private List<LibraryConfigurable> getLibraryConfigurables() {
    //todo[nik] improve
    List<LibraryConfigurable> libraryConfigurables = new ArrayList<>();
    for (int i = 0; i < myRoot.getChildCount(); i++) {
      final TreeNode node = myRoot.getChildAt(i);
      if (node instanceof MyNode) {
        final Configurable configurable = ((MyNode)node).getConfigurable();
        if (configurable instanceof LibraryConfigurable) {
          libraryConfigurables.add((LibraryConfigurable)configurable);
        }
      }
    }
    return libraryConfigurables;
  }

  private void createLibrariesNode(final LibraryTableModifiableModelProvider modelProvider) {
    final Library[] libraries = modelProvider.getModifiableModel().getLibraries();
    for (Library library : libraries) {
      myRoot.add(new MyNode(new LibraryConfigurable(myProject, modelProvider, library, getLibrariesConfigurator(), TREE_UPDATER)));
    }
    TreeUtil.sort(myRoot, (o1, o2) -> {
      MyNode node1 = (MyNode)o1;
      MyNode node2 = (MyNode)o2;
      return node1.getDisplayName().compareToIgnoreCase(node2.getDisplayName());
    });
    ((DefaultTreeModel)myTree.getModel()).reload(myRoot);
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    super.apply();
    ApplicationManager.getApplication().runWriteAction(() -> {
      getLibrariesConfigurator().commit();
    });
  }

  public String getLevel() {
    return myLevel;
  }

  public void createLibraryNode(Library library) {
    LibrariesConfigurator librariesConfigurator = getLibrariesConfigurator();

    final LibraryTable table = library.getTable();
    if (table != null) {
      final String level = table.getTableLevel();
      final LibraryConfigurable configurable = new LibraryConfigurable(myProject, librariesConfigurator.createModifiableModelProvider(level), library, librariesConfigurator, TREE_UPDATER);
      final MyNode node = new MyNode(configurable);
      addNode(node, myRoot);
      //final ProjectStructureDaemonAnalyzer daemonAnalyzer = myContext.getDaemonAnalyzer();
      //daemonAnalyzer.queueUpdate(new LibraryProjectStructureElement(library));
      //daemonAnalyzer.queueUpdateForAllElementsWithErrors();
    }
  }

  @Override
  @Nonnull
  protected List<? extends AnAction> createCopyActions(boolean fromPopup) {
    final ArrayList<AnAction> actions = new ArrayList<>();
    actions.add(new CopyLibraryAction());
    if (fromPopup) {
      // actions.add(new ChangeLibraryLevelAction(myProject, myTree, this, targetGroup));
      actions.add(new AddLibraryToModuleDependenciesAction(myProject, this));
    }
    return actions;
  }

  @Override
  protected AbstractAddGroup createAddAction() {
    return new AbstractAddGroup(getAddText()) {
      @Override
      @Nonnull
      public AnAction[] getChildren(@Nullable final AnActionEvent e) {
        return CreateNewLibraryAction.createActionOrGroup(getAddText(), BaseLibrariesConfigurable.this, myProject);
      }
    };
  }

  @Nonnull
  protected LibrariesConfigurator getLibrariesConfigurator() {
    ProjectStructureSettingsUtil util = (ProjectStructureSettingsUtil)ShowSettingsUtil.getInstance();
    return Objects.requireNonNull(util.getLibrariesModel(myProject), "Called libraries configurable without librariesConfigurator");
  }

  protected abstract String getAddText();

  public abstract LibraryTableModifiableModelProvider getModelProvider();

  @Override
  protected void updateSelection(@Nullable MasterDetailsConfigurable configurable) {
    boolean selectionChanged = !Comparing.equal(myCurrentConfigurable, configurable);
    if (myCurrentConfigurable != null && selectionChanged) {
      ((LibraryConfigurable)myCurrentConfigurable).onUnselected();
    }
    super.updateSelection(configurable);
    if (myCurrentConfigurable != null && selectionChanged) {
      ((LibraryConfigurable)myCurrentConfigurable).onSelected();
    }
  }

  @Override
  public void onStructureUnselected() {
    if (myCurrentConfigurable != null) {
      ((LibraryConfigurable)myCurrentConfigurable).onUnselected();
    }
  }

  @Override
  public void onStructureSelected() {
    if (myCurrentConfigurable != null) {
      ((LibraryConfigurable)myCurrentConfigurable).onSelected();
    }
  }

  public void removeLibrary(@Nonnull LibraryProjectStructureElement element) {
    getModelProvider().getModifiableModel().removeLibrary(element.getLibrary());
    // todo myContext.getDaemonAnalyzer().removeElement(element);
    final MyNode node = findNodeByObject(myRoot, element.getLibrary());
    if (node != null) {
      removePaths(TreeUtil.getPathFromRoot(node));
    }
  }

  @Override
  protected boolean removeLibrary(final Library library) {
    final LibraryTable table = library.getTable();
    if (table != null) {
      final LibraryProjectStructureElement libraryElement = new LibraryProjectStructureElement(library);
      /**final Collection<ProjectStructureElementUsage> usages = new ArrayList<>(myContext.getDaemonAnalyzer().getUsages(libraryElement));
      if (usages.size() > 0) {
        final MultiMap<String, ProjectStructureElementUsage> containerType2Usage = new MultiMap<>();
        for (final ProjectStructureElementUsage usage : usages) {
          containerType2Usage.putValue(usage.getContainingElement().getTypeName(), usage);
        }

        List<String> types = new ArrayList<>(containerType2Usage.keySet());
        Collections.sort(types);

        final StringBuilder sb = new StringBuilder("Library '");
        Library libraryModel = myContext.getLibraryModel(library);
        sb.append(libraryModel != null ? libraryModel.getName() : library.getName()).append("' is used in ");
        for (int i = 0; i < types.size(); i++) {
          if (i > 0 && i == types.size() - 1) {
            sb.append(" and in ");
          }
          else if (i > 0) {
            sb.append(", in ");
          }
          String type = types.get(i);
          Collection<ProjectStructureElementUsage> usagesOfType = containerType2Usage.get(type);
          if (usagesOfType.size() > 1) {
            sb.append(usagesOfType.size()).append(" ").append(StringUtil.decapitalize(StringUtil.pluralize(type)));
          }
          else {
            sb.append(StringUtil.decapitalize(usagesOfType.iterator().next().getContainingElement().getPresentableName()));
          }
        }

        sb.append(".\n\nAre you sure you want to delete this library?");

        if (DialogWrapper.OK_EXIT_CODE == Messages.showOkCancelDialog(myProject, sb.toString(), "Delete Library", Messages.getQuestionIcon())) {

          for (final ProjectStructureElementUsage usage : usages) {
            usage.removeSourceElement();
          }

          getModelProvider().getModifiableModel().removeLibrary(library);
          myContext.getDaemonAnalyzer().removeElement(libraryElement);
          return true;
        }
      }
      else */ {
        getModelProvider().getModifiableModel().removeLibrary(library);
        // todo myContext.getDaemonAnalyzer().removeElement(libraryElement);
        return true;
      }
    }

    return false;
  }

  @Override
  @Nullable
  protected String getEmptySelectionString() {
    return "Select a library to view or edit its details here";
  }

  private class CopyLibraryAction extends AnAction {
    private CopyLibraryAction() {
      super(CommonBundle.message("button.copy"), CommonBundle.message("button.copy"), PlatformIconGroup.actionsCopy());
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(final AnActionEvent e) {
      final Object o = getSelectedObject();
      if (o instanceof LibraryEx) {
        final LibraryEx selected = (LibraryEx)o;
        final String newName = Messages.showInputDialog("Enter library name:", "Copy Library", null, selected.getName() + "2", new NonEmptyInputValidator());
        if (newName == null) return;

        BaseLibrariesConfigurable configurable = BaseLibrariesConfigurable.this;
        final LibraryEx library = (LibraryEx)getLibrariesConfigurator().getLibrary(selected.getName(), myLevel);
        LOG.assertTrue(library != null);

        final LibrariesModifiableModel libsModel = (LibrariesModifiableModel)configurable.getModelProvider().getModifiableModel();
        final Library lib = libsModel.createLibrary(newName, library.getKind());
        final LibraryEx.ModifiableModelEx model = libsModel.getLibraryEditor(lib).getModel();
        LibraryEditingUtil.copyLibrary(library, Collections.<String, String>emptyMap(), model);
      }
    }

    @Override
    public void update(final AnActionEvent e) {
      if (myTree.getSelectionPaths() == null || myTree.getSelectionPaths().length != 1) {
        e.getPresentation().setEnabled(false);
      }
      else {
        e.getPresentation().setEnabled(getSelectedObject() instanceof LibraryImpl);
      }
    }
  }
}
