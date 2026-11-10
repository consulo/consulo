/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.content.scope.SearchScope;
import consulo.fileEditor.FileEditor;
import consulo.find.FindManager;
import consulo.find.FindModel;
import consulo.find.FindResult;
import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesHandlerFactory;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * Looks find usages handlers up; the find UI and text search are not available headless.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
@ServiceImpl
@Singleton
public class HeadlessFindManager extends FindManager {
    private static final FindResult NOT_FOUND = new FindResult(0, 0) {
        @Override
        public boolean isStringFound() {
            return false;
        }
    };

    private final Project myProject;
    private final FindModel myFindInFileModel = new FindModel();
    private final FindModel myFindInProjectModel = new FindModel();
    private @Nullable FindModel myPreviousFindModel;
    private @Nullable FindModel myFindNextModel;
    private boolean myFindWasPerformed;
    private boolean mySelectNextOccurrenceWasPerformed;

    @Inject
    public HeadlessFindManager(Project project) {
        myProject = project;
    }

    @Override
    public FindModel createReplaceInFileModel() {
        FindModel model = new FindModel();
        model.copyFrom(myFindInFileModel);
        model.setReplaceState(true);
        return model;
    }

    @Override
    public @Nullable FindModel getPreviousFindModel() {
        return myPreviousFindModel;
    }

    @Override
    public void setPreviousFindModel(FindModel previousFindModel) {
        myPreviousFindModel = previousFindModel;
    }

    @Override
    public void showSettingsAndFindUsages(NavigationItem[] targets) {
    }

    @Override
    public void showFindDialog(FindModel model, Runnable okHandler) {
    }

    @Override
    public int showPromptDialog(FindModel model, LocalizeValue title) {
        return PromptResult.CANCEL;
    }

    @Override
    public FindModel getFindInFileModel() {
        return myFindInFileModel;
    }

    @Override
    public FindModel getFindInProjectModel() {
        return myFindInProjectModel;
    }

    @Override
    public FindResult findString(CharSequence text, int offset, FindModel model) {
        return NOT_FOUND;
    }

    @Override
    public FindResult findString(CharSequence text, int offset, FindModel model, @Nullable VirtualFile findContextFile) {
        return NOT_FOUND;
    }

    @Override
    public int showMalformedReplacementPrompt(FindModel model, LocalizeValue title, MalformedReplacementStringException exception) {
        return PromptResult.CANCEL;
    }

    @Override
    public String getStringToReplace(String foundString, FindModel model, int startOffset, CharSequence documentText) {
        return model.getStringToReplace();
    }

    @Override
    public boolean findWasPerformed() {
        return myFindWasPerformed;
    }

    @Override
    public void setFindWasPerformed() {
        myFindWasPerformed = true;
    }

    @Override
    public boolean selectNextOccurrenceWasPerformed() {
        return mySelectNextOccurrenceWasPerformed;
    }

    @Override
    public void setSelectNextOccurrenceWasPerformed() {
        mySelectNextOccurrenceWasPerformed = true;
    }

    @Override
    public void clearFindingNextUsageInFile() {
    }

    @Override
    public void setFindNextModel(FindModel model) {
        myFindNextModel = model;
    }

    @Override
    public FindModel getFindNextModel() {
        return myFindNextModel != null ? myFindNextModel : myFindInFileModel;
    }

    @Override
    public FindModel getFindNextModel(Editor editor) {
        return getFindNextModel();
    }

    @Override
    public boolean canFindUsages(PsiElement element) {
        return getFindUsagesHandler(element, false) != null;
    }

    @Override
    public void findUsages(PsiElement element) {
    }

    @Override
    public void findUsagesInScope(PsiElement element, SearchScope searchScope) {
    }

    @Override
    public void findUsages(PsiElement element, boolean showDialog) {
    }

    @Override
    public void findUsagesInEditor(PsiElement element, FileEditor editor) {
    }

    @Override
    public boolean findNextUsageInEditor(FileEditor editor) {
        return false;
    }

    @Override
    public boolean findPreviousUsageInEditor(FileEditor editor) {
        return false;
    }

    @Override
    public @Nullable FindUsagesHandler getFindUsagesHandler(PsiElement element, boolean forHighlightUsages) {
        for (FindUsagesHandlerFactory factory : myProject.getExtensionList(FindUsagesHandlerFactory.class)) {
            if (factory.canFindUsages(element)) {
                FindUsagesHandler handler = factory.createFindUsagesHandler(element, forHighlightUsages);
                if (handler == FindUsagesHandler.NULL_HANDLER) {
                    return null;
                }
                if (handler != null) {
                    return handler;
                }
            }
        }
        return null;
    }
}
