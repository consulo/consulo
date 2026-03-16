/*
 * Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.dataContext.DataContext;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.awt.Messages;
import consulo.undoRedo.CommandProcessor;

final class DetachLibraryDeleteProvider implements DeleteProvider {

    private final Project myProject;
    private final LibraryOrderEntry myOrderEntry;

    DetachLibraryDeleteProvider(Project project, LibraryOrderEntry orderEntry) {
        myProject = project;
        myOrderEntry = orderEntry;
    }

    @Override
    public boolean canDeleteElement(DataContext dataContext) {
        return true;
    }

    @Override
    public void deleteElement(DataContext dataContext) {
        Module module = myOrderEntry.getOwnerModule();
        String libraryName = myOrderEntry.getPresentableName();
        String moduleName = module.getName();
        int ret = Messages.showOkCancelDialog(
            myProject,
            "Detach library '" + libraryName + "' from module '" + moduleName + "'?",
            "Detach Library",
            Messages.getQuestionIcon()
        );
        if (ret != Messages.OK) {
            return;
        }
        CommandProcessor.getInstance().newCommand()
            .project(module.getProject())
            .name(ProjectLocalize.moduleRemoveCommand())
            .inWriteAction()
            .run(() -> {
                ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
                OrderEntry[] orderEntries = rootManager.getOrderEntries();
                ModifiableRootModel model = rootManager.getModifiableModel();
                OrderEntry[] modifiableEntries = model.getOrderEntries();
                for (int i = 0; i < orderEntries.length; i++) {
                    OrderEntry entry = orderEntries[i];
                    if (entry instanceof LibraryOrderEntry libraryEntry
                        && libraryEntry.getLibrary() == myOrderEntry.getLibrary()) {
                        model.removeOrderEntry(modifiableEntries[i]);
                    }
                }
                model.commit();
            });
    }
}
