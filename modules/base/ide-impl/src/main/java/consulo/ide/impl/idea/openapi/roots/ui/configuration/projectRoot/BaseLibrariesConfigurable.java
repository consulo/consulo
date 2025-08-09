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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot;

import consulo.application.ApplicationManager;
import consulo.application.content.impl.internal.library.LibraryImpl;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.MasterDetailsConfigurable;
import consulo.content.internal.LibraryEx;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.content.library.LibraryTablePresentation;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor.CreateNewLibraryAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.LibraryProjectStructureElement;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import consulo.ui.ex.NonEmptyInputValidator;
import consulo.ide.setting.ProjectStructureSettingsUtil;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.LibraryTableModifiableModelProvider;
import consulo.ide.setting.module.event.LibraryEditorListener;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.MasterDetailsStateService;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Provider;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.*;

public abstract class BaseLibrariesConfigurable extends BaseStructureConfigurable {
    @Nonnull
    protected final Project myProject;
    @Nonnull
    protected String myLevel;

    protected BaseLibrariesConfigurable(@Nonnull Project project, Provider<MasterDetailsStateService> masterDetailsStateService) {
        super(masterDetailsStateService);
        myProject = project;
    }

    @RequiredUIAccess
    @Override
    public void initialize() {
        super.initialize();

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

    public abstract LibraryTablePresentation getLibraryTablePresentation();

    @Override
    protected void processRemovedItems() {
    }

    @Override
    protected boolean wasObjectStored(Object editableObject) {
        return false;
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        boolean isModified = false;
        for (LibraryTable.ModifiableModel provider : getLibrariesConfigurator().getModels()) {
            isModified |= provider.isChanged();
        }
        return isModified || super.isModified();
    }

    @Override
    public void checkCanApply() throws ConfigurationException {
        super.checkCanApply();
        for (LibraryConfigurable configurable : getLibraryConfigurables()) {
            if (configurable.getDisplayName().get().isEmpty()) {
                ((LibraryProjectStructureElement) configurable.getProjectStructureElement()).navigate(myProject);
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
        List<ProjectStructureElement> result = new ArrayList<>();
        for (LibraryConfigurable libraryConfigurable : getLibraryConfigurables()) {
            result.add(new LibraryProjectStructureElement(libraryConfigurable.getEditableObject()));
        }
        return result;
    }

    private List<LibraryConfigurable> getLibraryConfigurables() {
        //todo[nik] improve
        List<LibraryConfigurable> libraryConfigurables = new ArrayList<>();
        for (int i = 0; i < myRoot.getChildCount(); i++) {
            TreeNode node = myRoot.getChildAt(i);
            if (node instanceof MyNode) {
                Configurable configurable = ((MyNode) node).getConfigurable();
                if (configurable instanceof LibraryConfigurable) {
                    libraryConfigurables.add((LibraryConfigurable) configurable);
                }
            }
        }
        return libraryConfigurables;
    }

    private void createLibrariesNode(LibraryTableModifiableModelProvider modelProvider) {
        Library[] libraries = modelProvider.getModifiableModel().getLibraries();
        for (Library library : libraries) {
            myRoot.add(new MyNode(new LibraryConfigurable(myProject, modelProvider, library, getLibrariesConfigurator(), TREE_UPDATER)));
        }
        TreeUtil.sort(myRoot, (o1, o2) -> {
            MyNode node1 = (MyNode) o1;
            MyNode node2 = (MyNode) o2;
            return node1.getDisplayName().compareIgnoreCase(node2.getDisplayName());
        });
        ((DefaultTreeModel) myTree.getModel()).reload(myRoot);
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

        LibraryTable table = library.getTable();
        if (table != null) {
            String level = table.getTableLevel();
            LibraryConfigurable configurable = new LibraryConfigurable(
                myProject,
                librariesConfigurator.createModifiableModelProvider(level),
                library,
                librariesConfigurator,
                TREE_UPDATER
            );
            MyNode node = new MyNode(configurable);
            addNode(node, myRoot);
            //final ProjectStructureDaemonAnalyzer daemonAnalyzer = myContext.getDaemonAnalyzer();
            //daemonAnalyzer.queueUpdate(new LibraryProjectStructureElement(library));
            //daemonAnalyzer.queueUpdateForAllElementsWithErrors();
        }
    }

    @Override
    @Nonnull
    protected List<? extends AnAction> createCopyActions(boolean fromPopup) {
        ArrayList<AnAction> actions = new ArrayList<>();
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
            @Nonnull
            @Override
            public AnAction[] getChildren(@Nullable AnActionEvent e) {
                return CreateNewLibraryAction.createActionOrGroup(getAddText(), BaseLibrariesConfigurable.this, myProject);
            }
        };
    }

    @Nonnull
    protected LibrariesConfigurator getLibrariesConfigurator() {
        ProjectStructureSettingsUtil util = (ProjectStructureSettingsUtil) ShowSettingsUtil.getInstance();
        return Objects.requireNonNull(util.getLibrariesModel(myProject), "Called libraries configurable without librariesConfigurator");
    }

    protected abstract String getAddText();

    public abstract LibraryTableModifiableModelProvider getModelProvider();

    @RequiredUIAccess
    @Override
    protected void updateSelection(@Nullable MasterDetailsConfigurable configurable) {
        boolean selectionChanged = !Comparing.equal(myCurrentConfigurable, configurable);
        if (myCurrentConfigurable != null && selectionChanged) {
            ((LibraryConfigurable) myCurrentConfigurable).onUnselected();
        }
        super.updateSelection(configurable);
        if (myCurrentConfigurable != null && selectionChanged) {
            ((LibraryConfigurable) myCurrentConfigurable).onSelected();
        }
    }

    @Override
    public void onStructureUnselected() {
        if (myCurrentConfigurable != null) {
            ((LibraryConfigurable) myCurrentConfigurable).onUnselected();
        }
    }

    @Override
    public void onStructureSelected() {
        if (myCurrentConfigurable != null) {
            ((LibraryConfigurable) myCurrentConfigurable).onSelected();
        }
    }

    public void removeLibrary(@Nonnull LibraryProjectStructureElement element) {
        getModelProvider().getModifiableModel().removeLibrary(element.getLibrary());
        // todo myContext.getDaemonAnalyzer().removeElement(element);
        MyNode node = findNodeByObject(myRoot, element.getLibrary());
        if (node != null) {
            removePaths(TreeUtil.getPathFromRoot(node));
        }
    }

    @Override
    protected boolean removeLibrary(Library library) {
        LibraryTable table = library.getTable();
        if (table != null) {
            LibraryProjectStructureElement libraryElement = new LibraryProjectStructureElement(library);
            /*
            final Collection<ProjectStructureElementUsage> usages = new ArrayList<>(myContext.getDaemonAnalyzer().getUsages(libraryElement));
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
            else */{
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
            super(CommonLocalize.buttonCopy(), CommonLocalize.buttonCopy(), PlatformIconGroup.actionsCopy());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            Object o = getSelectedObject();
            if (o instanceof LibraryEx) {
                LibraryEx selected = (LibraryEx) o;
                String newName = Messages.showInputDialog(
                    "Enter library name:",
                    "Copy Library",
                    null,
                    selected.getName() + "2",
                    new NonEmptyInputValidator()
                );
                if (newName == null) {
                    return;
                }

                BaseLibrariesConfigurable configurable = BaseLibrariesConfigurable.this;
                LibraryEx library = (LibraryEx) getLibrariesConfigurator().getLibrary(selected.getName(), myLevel);
                LOG.assertTrue(library != null);

                LibrariesModifiableModel libsModel = (LibrariesModifiableModel) configurable.getModelProvider().getModifiableModel();
                Library lib = libsModel.createLibrary(newName, library.getKind());
                LibraryEx.ModifiableModelEx model = libsModel.getLibraryEditor(lib).getModel();
                LibraryEditingUtil.copyLibrary(library, Collections.<String, String>emptyMap(), model);
            }
        }

        @Override
        public void update(AnActionEvent e) {
            if (myTree.getSelectionPaths() == null || myTree.getSelectionPaths().length != 1) {
                e.getPresentation().setEnabled(false);
            }
            else {
                e.getPresentation().setEnabled(getSelectedObject() instanceof LibraryImpl);
            }
        }
    }
}
