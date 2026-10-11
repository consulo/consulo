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

import consulo.application.Application;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.MasterDetailsConfigurable;
import consulo.configurable.UnifiedMasterDetailsComponent;
import consulo.content.internal.LibraryEx;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.content.library.LibraryTablePresentation;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryPresentationManager;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor.CreateNewLibraryAction;
import consulo.ide.setting.ProjectStructureSettingsUtil;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.LibraryTableModifiableModelProvider;
import consulo.ide.setting.module.event.LibraryEditorListener;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.InputBoxBuilder;
import consulo.ui.InputProblem;
import consulo.ui.TextItemPresentation;
import consulo.ui.Tree;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public abstract class BaseLibrariesConfigurable extends UnifiedMasterDetailsComponent
    implements Configurable.NoMargin, Configurable.NoScroll {
    private static final Logger LOG = Logger.getInstance(BaseLibrariesConfigurable.class);

    protected final Project myProject;
    protected final String myLevel;

    private final Map<Library, Image> myIcons = new ConcurrentHashMap<>();

    private @Nullable Disposable myListenerDisposable;

    protected BaseLibrariesConfigurable(Project project, String level) {
        myProject = project;
        myLevel = level;
    }

    public abstract LibraryTablePresentation getLibraryTablePresentation();

    public abstract LibraryTableModifiableModelProvider getModelProvider();

    protected abstract LocalizeValue getAddText();

    public String getLevel() {
        return myLevel;
    }

    @Override
    protected LocalizeValue getEmptySelectionText() {
        return ProjectLocalize.librariesEmptyText();
    }

    @Override
    protected boolean isMultipleSelectAllowed() {
        return true;
    }

    @Override
    protected @Nullable Comparator<Object> getValueComparator() {
        return Comparator.comparing(this::getValueText, String.CASE_INSENSITIVE_ORDER);
    }

    @Override
    protected String getValueText(Object value) {
        if (value instanceof Library library) {
            return getLibraryName(library);
        }
        return String.valueOf(value);
    }

    @Override
    protected void renderValue(Object value, TextItemPresentation presentation) {
        if (value instanceof Library library) {
            presentation.withIcon(myIcons.getOrDefault(library, PlatformIconGroup.nodesPplib()));
            presentation.append(getLibraryName(library));
        }
    }

    @RequiredUIAccess
    @Override
    protected void loadTree() {
        disposeListener();

        LibrariesConfigurator librariesConfigurator = getLibrariesConfigurator();

        Disposable listenerDisposable = Disposable.newDisposable();
        librariesConfigurator.addLibraryEditorListener(new LibraryEditorListener() {
            @RequiredUIAccess
            @Override
            public void libraryCreated(Library library) {
                addLibrary(library);
            }

            @RequiredUIAccess
            @Override
            public void libraryRenamed(Library library, String oldName, String newName) {
                if (isAttached(library)) {
                    refreshValue(library);
                }
            }
        }, listenerDisposable);
        myListenerDisposable = listenerDisposable;

        List<Library> libraries = List.of(librariesConfigurator.createModifiableModelProvider(myLevel).getModifiableModel().getLibraries());
        for (Library library : libraries) {
            addValue(null, library);
        }
        updateIcons(libraries);
    }

    @RequiredUIAccess
    @Override
    protected @Nullable MasterDetailsConfigurable<?> createConfigurable(Object value) {
        if (!(value instanceof Library library)) {
            return null;
        }

        LibraryTable table = library.getTable();
        String level = table != null ? table.getTableLevel() : myLevel;
        return new LibraryConfigurable(
            myProject,
            getLibrariesConfigurator().createModifiableModelProvider(level),
            library,
            this::onLibraryRenamed,
            this::onLibraryRootsChanged
        );
    }

    @Override
    protected List<AnAction> createActions() {
        return List.of(new AddLibraryAction(), new RemoveLibraryAction(), new CopyLibraryAction());
    }

    @Override
    protected List<AnAction> createPopupActions() {
        return List.of(
            new AddLibraryAction(),
            new RemoveLibraryAction(),
            new CopyLibraryAction(),
            new AddLibraryToModuleDependenciesAction(myProject, this)
        );
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        for (LibraryTable.ModifiableModel model : getLibrariesConfigurator().getModels()) {
            if (model.isChanged()) {
                return true;
            }
        }
        return super.isModified();
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        checkLibraryNames();

        super.apply();

        myProject.getApplication().runWriteAction(() -> getLibrariesConfigurator().commit());
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        super.disposeUIResources();

        disposeListener();
        myIcons.clear();
    }

    @RequiredUIAccess
    @Override
    protected void updateSelection(@Nullable MasterDetailsConfigurable<?> configurable) {
        MasterDetailsConfigurable<?> previous = getSelectedConfigurable();
        boolean selectionChanged = previous != configurable;
        if (previous instanceof LibraryConfigurable libraryConfigurable && selectionChanged) {
            libraryConfigurable.onUnselected();
        }

        super.updateSelection(configurable);

        if (configurable instanceof LibraryConfigurable libraryConfigurable && selectionChanged) {
            libraryConfigurable.onSelected();
        }
    }

    @RequiredUIAccess
    public CompletableFuture<?> selectLibrary(Library library) {
        return select(findLibrary(library));
    }

    @RequiredUIAccess
    public void removeLibrary(Library library) {
        Library target = findLibrary(library);
        if (target == null) {
            return;
        }

        getModelProvider().getModifiableModel().removeLibrary(target);
        removeValue(target);
        refreshTree(getSelectedValue());
    }

    protected LibrariesConfigurator getLibrariesConfigurator() {
        ProjectStructureSettingsUtil util = ShowSettingsUtil.getInstance();
        return Objects.requireNonNull(util.getLibrariesModel(myProject), "Called libraries configurable without librariesConfigurator");
    }

    private @Nullable Library findLibrary(Library library) {
        Object value = findValue(candidate -> candidate == library);
        if (value == null) {
            Library model = getLibrariesConfigurator().getLibraryModel(library);
            value = findValue(candidate -> candidate == model);
        }
        if (value == null) {
            String name = library.getName();
            value = findValue(candidate -> candidate instanceof Library candidateLibrary && Objects.equals(candidateLibrary.getName(), name));
        }
        return value instanceof Library found ? found : null;
    }

    private String getLibraryName(Library library) {
        LibrariesModifiableModel model = (LibrariesModifiableModel) getModelProvider().getModifiableModel();
        String name = model.hasLibraryEditor(library) ? model.getLibraryEditor(library).getName() : library.getName();
        return name == null ? "" : name;
    }

    @RequiredUIAccess
    private void checkLibraryNames() throws ConfigurationException {
        Set<String> names = new HashSet<>();
        for (Object value : getChildren(null)) {
            if (!(value instanceof Library library)) {
                continue;
            }

            String name = getLibraryName(library);
            if (name.isBlank()) {
                select(library);
                throw new ConfigurationException(ProjectLocalize.libraryNameNotSpecifiedError(), ProjectLocalize.libraryNameNotSpecifiedTitle());
            }
            if (!names.add(name)) {
                select(library);
                throw new ConfigurationException(ProjectLocalize.libraryNameAlreadyExistsError(name), ProjectLocalize.libraryNameAlreadyExistsTitle());
            }
        }
    }

    @RequiredUIAccess
    private void addLibrary(Library library) {
        LibraryTable table = library.getTable();
        if (table == null || !myLevel.equals(table.getTableLevel()) || isAttached(library)) {
            return;
        }

        addValue(null, library);
        updateIcons(List.of(library));
        refreshTree(library);
    }

    @RequiredUIAccess
    private void onLibraryRenamed(Library library) {
        if (isAttached(library)) {
            refreshValue(library);
        }
    }

    @RequiredUIAccess
    private void onLibraryRootsChanged(Library library) {
        if (isAttached(library)) {
            updateIcons(List.of(library));
        }
    }

    @RequiredUIAccess
    private void updateIcons(Collection<Library> libraries) {
        if (libraries.isEmpty()) {
            return;
        }

        LibrariesConfigurator librariesConfigurator = getLibrariesConfigurator();
        List<Library> targets = List.copyOf(libraries);
        UIAccess uiAccess = UIAccess.current();
        Application application = myProject.getApplication();
        CompletableFuture.supplyAsync(() -> {
            Map<Library, Image> icons = new HashMap<>();
            for (Library library : targets) {
                Supplier<Image> icon = () -> LibraryPresentationManager.getInstance().getNamedLibraryIcon(library, librariesConfigurator);
                icons.put(library, application.runReadAction(icon));
            }
            return icons;
        }, AppExecutorUtil.getAppExecutorService()).whenCompleteAsync((icons, error) -> {
            if (error != null) {
                LOG.error(error);
                return;
            }

            for (Map.Entry<Library, Image> entry : icons.entrySet()) {
                Library library = entry.getKey();
                if (isAttached(library)) {
                    myIcons.put(library, entry.getValue());
                    refreshValue(library);
                }
            }
        }, uiAccess);
    }

    private void disposeListener() {
        Disposable listenerDisposable = myListenerDisposable;
        if (listenerDisposable != null) {
            listenerDisposable.disposeWithTree();
            myListenerDisposable = null;
        }
    }

    private class AddLibraryAction extends DumbAwareAction {
        private AddLibraryAction() {
            super(getAddText(), LocalizeValue.empty(), PlatformIconGroup.generalAdd());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
            for (AnAction action : CreateNewLibraryAction.createActionOrGroup(getAddText(), BaseLibrariesConfigurable.this, myProject)) {
                group.add(action);
            }

            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                getAddText().get(),
                group.build(),
                e.getDataContext(),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
            );
            popup.showUnderneathOf(e);
        }
    }

    private class RemoveLibraryAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private RemoveLibraryAction() {
            super(CommonLocalize.buttonDelete(), CommonLocalize.buttonDelete(), PlatformIconGroup.generalRemove());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            List<Object> selected = getSelectedValues();
            if (!canRemove(selected)) {
                return;
            }

            LibraryTable.ModifiableModel model = getModelProvider().getModifiableModel();
            for (Object value : selected) {
                model.removeLibrary((Library) value);
            }
            removeSelected();
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(canRemove(getSelectedValues()));
        }

        private boolean canRemove(List<Object> selected) {
            if (selected.isEmpty()) {
                return false;
            }

            for (Object value : selected) {
                if (!(value instanceof Library)) {
                    return false;
                }
            }
            return true;
        }
    }

    private class CopyLibraryAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private CopyLibraryAction() {
            super(CommonLocalize.buttonCopy(), CommonLocalize.buttonCopy(), PlatformIconGroup.actionsCopy());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            if (getSelectedValues().size() != 1 || !(getSelectedValue() instanceof LibraryEx selected)) {
                return;
            }

            LibrariesModifiableModel librariesModel = (LibrariesModifiableModel) getModelProvider().getModifiableModel();
            String selectedName = getLibraryName(selected);

            Tree<Object> parent = getTree();
            UIAccess uiAccess = UIAccess.current();
            InputBoxBuilder.text()
                .title(ProjectLocalize.libraryCopyTitle())
                .text(ProjectLocalize.libraryCopyPrompt())
                .value(LibraryEditingUtil.suggestNewLibraryName(librariesModel, selectedName))
                .validator(name -> {
                    if (name.isBlank()) {
                        return InputProblem.error(ProjectLocalize.libraryNameNotSpecifiedError());
                    }
                    if (librariesModel.getLibraryByName(name) != null) {
                        return InputProblem.error(ProjectLocalize.libraryNameAlreadyExistsError(name));
                    }
                    return null;
                })
                .showAsync(parent)
                .whenCompleteAsync((newName, error) -> {
                    if (error != null || newName == null) {
                        return;
                    }
                    LibraryEx library = (LibraryEx) getLibrariesConfigurator().getLibrary(selectedName, myLevel);
                    LibraryEx source = library != null ? library : selected;

                    Library copy = librariesModel.createLibrary(newName, source.getKind());
                    LibraryEx.ModifiableModelEx model = librariesModel.getLibraryEditor(copy).getModel();
                    LibraryEditingUtil.copyLibrary(source, Map.of(), model);
                    updateIcons(List.of(copy));
                 }, uiAccess);
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(getSelectedValues().size() == 1 && getSelectedValue() instanceof LibraryEx);
        }
    }
}
