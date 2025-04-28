/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ide.favoritesTreeView.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesManagerImpl;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.VirtualFile;

import java.util.ArrayList;

/**
 * @author anna
 * @since 2005-04-05
 */
public class AddAllOpenFilesToFavorites extends AnAction {
    private final String myFavoritesName;

    public AddAllOpenFilesToFavorites(String chosenList) {
        getTemplatePresentation().setText(chosenList, false);
        myFavoritesName = chosenList;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        FavoritesManagerImpl favoritesManager = FavoritesManagerImpl.getInstance(project);

        ArrayList<PsiFile> filesToAdd = getFilesToAdd(project);
        for (PsiFile file : filesToAdd) {
            favoritesManager.addRoots(myFavoritesName, null, file);
        }
    }

    @RequiredReadAction
    static ArrayList<PsiFile> getFilesToAdd(Project project) {
        ArrayList<PsiFile> result = new ArrayList<>();
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        PsiManager psiManager = PsiManager.getInstance(project);
        VirtualFile[] openFiles = editorManager.getOpenFiles();
        for (VirtualFile openFile : openFiles) {
            if (!openFile.isValid()) {
                continue;
            }
            PsiFile psiFile = psiManager.findFile(openFile);
            if (psiFile != null) {
                result.add(psiFile);
            }
        }
        return result;
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        e.getPresentation().setEnabled(!getFilesToAdd(project).isEmpty());
    }
}
