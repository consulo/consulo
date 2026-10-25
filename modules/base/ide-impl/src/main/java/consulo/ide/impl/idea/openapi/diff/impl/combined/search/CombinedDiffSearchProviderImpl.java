// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.openapi.diff.impl.combined.search;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.diff.impl.internal.combined.CombinedDiffMainUI;
import consulo.diff.impl.internal.combined.CombinedDiffViewer;
import consulo.diff.impl.internal.combined.search.CombinedDiffSearchContext;
import consulo.diff.impl.internal.combined.search.CombinedDiffSearchController;
import consulo.diff.impl.internal.combined.search.CombinedDiffSearchProvider;
import consulo.diff.localize.DiffLocalize;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@ServiceImpl
@Singleton
public class CombinedDiffSearchProviderImpl implements CombinedDiffSearchProvider {
    private final Project myProject;

    @Inject
    public CombinedDiffSearchProviderImpl(Project project) {
        myProject = project;
    }

    @RequiredUIAccess
    @Override
    public void installSearch(CombinedDiffViewer viewer) {
        CombinedDiffMainUI mainUI = viewer.getMainUI();
        Editor currentEditor = CombinedDiffViewer.getCurrentEditor(viewer.getCurrentDiffViewer());
        if (currentEditor == null) {
            return;
        }

        CombinedDiffSearchControllerImpl searchController =
            new CombinedDiffSearchControllerImpl(myProject, currentEditor, viewer, mainUI::closeSearch, mainUI.getComponent());

        mainUI.setSearchController(searchController);

        searchController.mySession.getComponent().requestFocusInTheSearchFieldAndSelectContent(myProject);
    }

    private static final class CombinedDiffSearchControllerImpl implements CombinedDiffSearchController, CombinedEditorSearchSessionListener {
        private final CombinedDiffViewer myViewer;
        private final CombinedEditorSearchSession mySession;

        @RequiredUIAccess
        private CombinedDiffSearchControllerImpl(
            Project project,
            Editor currentEditor,
            CombinedDiffViewer viewer,
            Runnable closeAction,
            Component parentComponent
        ) {
            myViewer = viewer;
            mySession = new CombinedEditorSearchSession(project, currentEditor, closeAction, parentComponent, viewer);
            mySession.addListener(this);
            mySession.update(viewer.createSearchContext().getHolders(), CombinedDiffSearchContext.EditorHolder::getEditors, currentEditor);
        }

        @Override
        public Component getSearchComponent() {
            return mySession.getComponent().getUIComponent();
        }

        @RequiredUIAccess
        @Override
        public void update(CombinedDiffSearchContext context) {
            mySession.update(context.getHolders(), CombinedDiffSearchContext.EditorHolder::getEditors);
        }

        @RequiredUIAccess
        @Override
        public void onSearch(boolean forward, Editor editor) {
            myViewer.scrollToEditor(editor, false);
        }

        @RequiredUIAccess
        @Override
        public void statusTextChanged(int matches, int files) {
            int totalFilesCount = myViewer.getDiffBlocksCount();
            String filesCount = DiffLocalize.diffFilesEditorsSearchFilesCount(files).get();
            String status = totalFilesCount != files
                ? DiffLocalize.diffFilesEditorsSearchPartlyMatches(matches, filesCount).get()
                : DiffLocalize.diffFilesEditorsSearchMatches(matches, filesCount).get();
            mySession.setStatusText(status);
        }
    }
}
