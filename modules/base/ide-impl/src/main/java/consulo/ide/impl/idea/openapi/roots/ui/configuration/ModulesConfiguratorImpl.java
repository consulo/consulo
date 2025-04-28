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
package consulo.ide.impl.idea.openapi.roots.ui.configuration;

import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.compiler.CompilerConfiguration;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.util.ModuleCompilerUtil;
import consulo.component.util.graph.GraphGenerator;
import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.actions.ModuleDeleteProvider;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.newProject.ui.NewProjectDialog;
import consulo.ide.impl.newProject.ui.NewProjectPanel;
import consulo.ide.moduleImport.ModuleImportContext;
import consulo.ide.moduleImport.ModuleImportProcessor;
import consulo.ide.moduleImport.ModuleImportProvider;
import consulo.ide.newModule.NewOrImportModuleUtil;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.ModulesAlphaComparator;
import consulo.module.content.ModifiableModelCommitter;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.ModuleRootModel;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.concurrent.AsyncPromise;
import consulo.util.concurrent.AsyncResult;
import consulo.util.concurrent.Promise;
import consulo.util.concurrent.Promises;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Supplier;

/**
 * @author Eugene Zhuravlev
 * Date: Dec 15, 2003
 */
public class ModulesConfiguratorImpl implements ModulesConfigurator, ModuleEditor.ChangeListener, Disposable {
    private static final Logger LOG = Logger.getInstance(ModulesConfiguratorImpl.class);

    private final Project myProject;
    private final List<ModuleEditor> myModuleEditors = new ArrayList<>();
    private final Comparator<ModuleEditor> myModuleEditorComparator =
        (editor1, editor2) -> ModulesAlphaComparator.INSTANCE.compare(editor1.getModule(), editor2.getModule());
    private boolean myModified = false;
    private ModifiableModuleModel myModuleModel;
    private boolean myModuleModelCommitted = false;

    private final List<ModuleEditor.ChangeListener> myAllModulesChangeListeners = new ArrayList<>();

    private final Supplier<LibrariesConfigurator> myLibrariesConfiguratorSupplier;

    private String myCompilerOutputUrl;

    public ModulesConfiguratorImpl(Project project, Supplier<LibrariesConfigurator> librariesConfiguratorSupplier) {
        myProject = project;
        myLibrariesConfiguratorSupplier = librariesConfiguratorSupplier;
    }

    @Override
    @RequiredUIAccess
    public void dispose() {
        for (final ModuleEditor moduleEditor : myModuleEditors) {
            Disposer.dispose(moduleEditor);
        }
        myModuleEditors.clear();

        WriteAction.run(() -> {
            if (myModuleModel != null) {
                myModuleModel.dispose();
            }
        });
    }

    @Override
    @Nonnull
    public Module[] getModules() {
        return myModuleModel.getModules();
    }

    @Override
    @Nullable
    public Module getModule(String name) {
        final Module moduleByName = myModuleModel.findModuleByName(name);
        if (moduleByName != null) {
            return moduleByName;
        }
        return myModuleModel.getModuleToBeRenamed(name); //if module was renamed
    }

    @Nullable
    public ModuleEditor getModuleEditor(Module module) {
        for (final ModuleEditor moduleEditor : myModuleEditors) {
            if (module.equals(moduleEditor.getModule())) {
                return moduleEditor;
            }
        }
        return null;
    }

    @Override
    public ModuleRootModel getRootModel(@Nonnull Module module) {
        return getOrCreateModuleEditor(module).getRootModel();
    }

    public ModuleEditor getOrCreateModuleEditor(Module module) {
        LOG.assertTrue(getModule(module.getName()) != null, "Module has been deleted");
        ModuleEditor editor = getModuleEditor(module);
        if (editor == null) {
            editor = doCreateModuleEditor(module);
        }
        return editor;
    }

    private ModuleEditor doCreateModuleEditor(final Module module) {
        final ModuleEditor moduleEditor = new TabbedModuleEditor(myProject, this, myLibrariesConfiguratorSupplier.get(), module);

        myModuleEditors.add(moduleEditor);

        moduleEditor.addChangeListener(this);
        Disposer.register(moduleEditor, () -> moduleEditor.removeChangeListener(ModulesConfiguratorImpl.this));
        return moduleEditor;
    }

    @RequiredUIAccess
    public void reset() {
        myCompilerOutputUrl = CompilerConfiguration.getInstance(myProject).getCompilerOutputUrl();

        myModuleModel = ModuleManager.getInstance(myProject).getModifiableModel();

        if (!myModuleEditors.isEmpty()) {
            LOG.error("module editors was not disposed");
            myModuleEditors.clear();
        }
        final Module[] modules = myModuleModel.getModules();
        if (modules.length > 0) {
            for (Module module : modules) {
                getOrCreateModuleEditor(module);
            }
            Collections.sort(myModuleEditors, myModuleEditorComparator);
        }
        myModified = false;
    }

    @Override
    public void moduleStateChanged(final ModifiableRootModel moduleRootModel) {
        for (ModuleEditor.ChangeListener listener : myAllModulesChangeListeners) {
            listener.moduleStateChanged(moduleRootModel);
        }

        // todo context.getDaemonAnalyzer().queueUpdate(new ModuleProjectStructureElement(context, moduleRootModel.getModule()));
    }

    public void addAllModuleChangeListener(ModuleEditor.ChangeListener listener) {
        myAllModulesChangeListeners.add(listener);
    }

    public GraphGenerator<ModuleRootModel> createGraphGenerator() {
        final Map<Module, ModuleRootModel> models = new HashMap<>();
        for (ModuleEditor moduleEditor : myModuleEditors) {
            models.put(moduleEditor.getModule(), moduleEditor.getRootModel());
        }
        return ModuleCompilerUtil.createGraphGenerator(models);
    }

    @RequiredUIAccess
    public void apply() throws ConfigurationException {
        CompilerConfiguration.getInstance(myProject).setCompilerOutputUrl(myCompilerOutputUrl);

        // validate content and source roots
        final Map<VirtualFile, String> contentRootToModuleNameMap = new HashMap<>();
        final Map<VirtualFile, VirtualFile> srcRootsToContentRootMap = new HashMap<>();
        for (final ModuleEditor moduleEditor : myModuleEditors) {
            final ModifiableRootModel rootModel = moduleEditor.getModifiableRootModel();
            final ContentEntry[] contents = rootModel.getContentEntries();
            for (ContentEntry contentEntry : contents) {
                final VirtualFile contentRoot = contentEntry.getFile();
                if (contentRoot == null) {
                    continue;
                }
                final String moduleName = moduleEditor.getName();
                final String previousName = contentRootToModuleNameMap.put(contentRoot, moduleName);
                if (previousName != null && !previousName.equals(moduleName)) {
                    throw new ConfigurationException(ProjectLocalize.modulePathsValidationDuplicateContentError(
                        contentRoot.getPresentableUrl(),
                        previousName,
                        moduleName
                    ));
                }

                final VirtualFile[] sourceAndTestFiles = contentEntry.getFolderFiles(LanguageContentFolderScopes.all(false));
                for (VirtualFile srcRoot : sourceAndTestFiles) {
                    final VirtualFile anotherContentRoot = srcRootsToContentRootMap.put(srcRoot, contentRoot);
                    if (anotherContentRoot != null) {
                        final String problematicModule;
                        final String correctModule;
                        if (VfsUtilCore.isAncestor(anotherContentRoot, contentRoot, true)) {
                            problematicModule = contentRootToModuleNameMap.get(anotherContentRoot);
                            correctModule = contentRootToModuleNameMap.get(contentRoot);
                        }
                        else {
                            problematicModule = contentRootToModuleNameMap.get(contentRoot);
                            correctModule = contentRootToModuleNameMap.get(anotherContentRoot);
                        }
                        throw new ConfigurationException(ProjectLocalize.modulePathsValidationDuplicateSourceRootError(
                            problematicModule,
                            srcRoot.getPresentableUrl(),
                            correctModule
                        ));
                    }
                }
            }
        }
        // additional validation: directories marked as src roots must belong to the same module as their corresponding content root
        for (Map.Entry<VirtualFile, VirtualFile> entry : srcRootsToContentRootMap.entrySet()) {
            final VirtualFile srcRoot = entry.getKey();
            final VirtualFile correspondingContent = entry.getValue();
            final String expectedModuleName = contentRootToModuleNameMap.get(correspondingContent);

            for (VirtualFile candidateContent = srcRoot; candidateContent != null && !candidateContent.equals(correspondingContent);
                 candidateContent = candidateContent.getParent()) {
                final String moduleName = contentRootToModuleNameMap.get(candidateContent);
                if (moduleName != null && !moduleName.equals(expectedModuleName)) {
                    throw new ConfigurationException(ProjectLocalize.modulePathsValidationSourceRootBelongsToAnotherModuleError(
                        srcRoot.getPresentableUrl(),
                        expectedModuleName,
                        moduleName
                    ));
                }
            }
        }

        final List<ModifiableRootModel> models = new ArrayList<>(myModuleEditors.size());
        for (ModuleEditor moduleEditor : myModuleEditors) {
            moduleEditor.canApply();
        }

        for (final ModuleEditor moduleEditor : myModuleEditors) {
            final ModifiableRootModel model = moduleEditor.apply();
            if (model != null) {
                models.add(model);
            }
        }

        Application.get().runWriteAction(() -> {
            try {
                final ModifiableRootModel[] rootModels = models.toArray(new ModifiableRootModel[models.size()]);
                ModifiableModelCommitter.getInstance(myProject).multiCommit(rootModels, myModuleModel);
                myModuleModelCommitted = true;
            }
            finally {

                myModuleModel = ModuleManager.getInstance(myProject).getModifiableModel();
                myModuleModelCommitted = false;
            }
        });

        myModified = false;
    }

    public void setModified(final boolean modified) {
        myModified = modified;
    }

    @Override
    public ModifiableModuleModel getModuleModel() {
        return myModuleModel;
    }

    @Nonnull
    @Override
    public String getRealName(final Module module) {
        final ModifiableModuleModel moduleModel = getModuleModel();
        String newName = moduleModel.getNewName(module);
        return newName != null ? newName : module.getName();
    }

    @Override
    public boolean isModuleModelCommitted() {
        return myModuleModelCommitted;
    }

    @Nullable
    @Override
    public ModifiableRootModel getModuleEditorModelProxy(Module module) {
        ModuleEditor moduleEditor = getModuleEditor(module);
        if (moduleEditor != null) {
            return moduleEditor.getModifiableRootModelProxy();
        }
        return null;
    }

    @RequiredUIAccess
    public boolean deleteModule(final Module module) {
        ModuleEditor moduleEditor = getModuleEditor(module);
        return moduleEditor == null || doRemoveModule(moduleEditor);
    }

    @Nonnull
    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    public Promise<List<Module>> addModule(boolean anImport) {
        if (myProject.isDefault()) {
            return Promises.rejectedPromise();
        }

        if (anImport) {
            AsyncPromise<List<Module>> asyncPromise = new AsyncPromise<>();
            AsyncResult listAsyncResult = ModuleImportProcessor.showFileChooser(myProject, null);

            listAsyncResult.doWhenDone(o -> {
                Pair<ModuleImportContext, ModuleImportProvider> pair = (Pair<ModuleImportContext, ModuleImportProvider>)o;
                ModuleImportProvider<ModuleImportContext> importProvider = pair.getSecond();
                ModuleImportContext importContext = pair.getFirst();
                assert importProvider != null;
                assert importContext != null;

                List<Module> modules = new ArrayList<>();

                importProvider.process(importContext, myProject, myModuleModel, modules::add);

                for (Module module : modules) {
                    getOrCreateModuleEditor(module);
                }

                asyncPromise.setResult(modules);
            });

            listAsyncResult.doWhenRejected(() -> asyncPromise.setError("rejected"));
        }
        else {
            FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false) {
                @RequiredUIAccess
                @Override
                public boolean isFileSelectable(VirtualFile file) {
                    if (!super.isFileSelectable(file)) {
                        return false;
                    }
                    for (Module module : myModuleModel.getModules()) {
                        VirtualFile moduleDir = module.getModuleDir();
                        if (moduleDir != null && moduleDir.equals(file)) {
                            return false;
                        }
                    }
                    return true;
                }
            };
            fileChooserDescriptor.withTitleValue(ProjectLocalize.chooseModuleHome());

            AsyncPromise<List<Module>> promise = new AsyncPromise<>();

            AsyncResult<VirtualFile> fileChooserAsync = FileChooser.chooseFile(fileChooserDescriptor, myProject, null);
            fileChooserAsync.doWhenDone(moduleDir -> {
                final NewProjectDialog dialog = new NewProjectDialog(myProject, moduleDir);

                AsyncResult<Void> dialogAsync = dialog.showAsync();
                dialogAsync.doWhenDone(() -> {
                    NewProjectPanel panel = dialog.getProjectPanel();

                    Module newModule =
                        NewOrImportModuleUtil.doCreate(panel.getProcessor(), panel.getWizardContext(), myModuleModel, moduleDir, false);

                    getOrCreateModuleEditor(newModule);

                    Collections.sort(myModuleEditors, myModuleEditorComparator);

                    processModuleCountChanged();

                    promise.setResult(Collections.singletonList(newModule));
                });
                dialogAsync.doWhenRejected(() -> promise.setError("dialog canceled"));
            });
            fileChooserAsync.doWhenRejected(() -> promise.setError("rejected from chooser"));

            return promise;
        }
        return Promises.rejectedPromise();
    }

    @RequiredUIAccess
    private boolean doRemoveModule(@Nonnull ModuleEditor selectedEditor) {
        LocalizeValue question;
        if (myModuleEditors.size() == 1) {
            question = ProjectLocalize.moduleRemoveLastConfirmation();
        }
        else {
            question = ProjectLocalize.moduleRemoveConfirmation(selectedEditor.getModule().getName());
        }
        int result = Messages.showYesNoDialog(
            myProject,
            question.get(),
            ProjectLocalize.moduleRemoveConfirmationTitle().get(),
            UIUtil.getQuestionIcon()
        );
        if (result != Messages.YES) {
            return false;
        }
        // do remove
        myModuleEditors.remove(selectedEditor);

        // destroyProcess removed module
        final Module moduleToRemove = selectedEditor.getModule();
        // remove all dependencies on the module that is about to be removed
        List<ModifiableRootModel> modifiableRootModels = new ArrayList<>();
        for (final ModuleEditor moduleEditor : myModuleEditors) {
            final ModifiableRootModel modifiableRootModel = moduleEditor.getModifiableRootModelProxy();
            modifiableRootModels.add(modifiableRootModel);
        }

        // destroyProcess editor
        ModuleDeleteProvider.removeModule(moduleToRemove, null, modifiableRootModels, myModuleModel);
        processModuleCountChanged();
        Disposer.dispose(selectedEditor);

        return true;
    }


    private void processModuleCountChanged() {
        for (ModuleEditor moduleEditor : myModuleEditors) {
            moduleEditor.fireModuleStateChanged();
        }
    }

    @Nonnull
    @Override
    public String getCompilerOutputUrl() {
        return myCompilerOutputUrl;
    }

    @Override
    public void setCompilerOutputUrl(String compilerOutputUrl) {
        myCompilerOutputUrl = compilerOutputUrl;
    }

    @Override
    public void processModuleCompilerOutputChanged(String baseUrl) {
        setCompilerOutputUrl(baseUrl);

        for (ModuleEditor moduleEditor : myModuleEditors) {
            moduleEditor.updateCompilerOutputPathChanged(baseUrl, moduleEditor.getName());
        }
    }

    public boolean isModified() {
        String compilerOutputUrl = CompilerConfiguration.getInstance(myProject).getCompilerOutputUrl();
        if (!Objects.equals(myCompilerOutputUrl, compilerOutputUrl)) {
            return true;
        }

        if (myModuleModel.isChanged()) {
            return true;
        }
        for (ModuleEditor moduleEditor : myModuleEditors) {
            if (moduleEditor.isModified()) {
                return true;
            }
        }
        return myModified;
    }

    @RequiredUIAccess
    public static void showArtifactSettings(@Nonnull Project project, @Nullable final Artifact artifact) {
        ShowSettingsUtil.getInstance().showProjectStructureDialog(project, config -> config.select(artifact, true));
    }

    public void moduleRenamed(Module module, final String oldName, final String name) {

        for (ModuleEditor moduleEditor : myModuleEditors) {
            if (module == moduleEditor.getModule() && Comparing.strEqual(moduleEditor.getName(), oldName)) {
                moduleEditor.setModuleName(name);
                moduleEditor.updateCompilerOutputPathChanged(getCompilerOutputUrl(), name);
                // todo context.getDaemonAnalyzer().queueUpdate(new ModuleProjectStructureElement(this, module));
                return;
            }
        }
    }
}
