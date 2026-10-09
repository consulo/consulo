/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.impl.projectView;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.HelpManager;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.impl.idea.ide.projectView.HelpID;
import consulo.ide.impl.idea.ide.projectView.impl.AbstractProjectViewPSIPane;
import consulo.ide.impl.idea.ide.projectView.impl.AbstractProjectViewPane;
import consulo.ide.impl.idea.ide.projectView.impl.BaseProjectViewImpl;
import consulo.ide.localize.IdeLocalize;
import consulo.ide.util.DirectoryChooserUtil;
import consulo.language.editor.refactoring.ui.CopyPasteDelegator;
import consulo.language.editor.util.EditorHelper;
import consulo.language.editor.util.IdeView;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.project.ui.view.internal.ProjectViewSharedSettings;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.CutProvider;
import consulo.ui.ex.PasteProvider;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.tree.ApplicationTreeExecutorFactory;
import consulo.ui.layout.WrappedLayout;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncResult;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * @author VISTALL
 * @since 2017-10-23
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
@State(name = "ProjectView", storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
public class UnifiedProjectViewImpl extends BaseProjectViewImpl {
    private final ApplicationTreeExecutorFactory myTreeExecutorFactory;

    private final Map<String, UnifiedProjectViewPaneView> myId2View = new HashMap<>();
    private final Map<String, Element> myUnappliedPaneState = new HashMap<>();

    private final List<Attribute> myLoadedNavigatorAttributes = new ArrayList<>();

    private @Nullable WrappedLayout myViewContentPanel;

    private @Nullable CopyPasteDelegator myCopyPasteDelegator;

    @Inject
    public UnifiedProjectViewImpl(
        Project project,
        ProjectViewSharedSettings projectViewSharedSettings,
        ApplicationTreeExecutorFactory treeExecutorFactory
    ) {
        super(project, projectViewSharedSettings);
        myTreeExecutorFactory = treeExecutorFactory;
    }

    @RequiredUIAccess
    private void uiDataSnapshot(DataSink sink) {
        sink.set(Project.KEY, myProject);
        sink.set(HelpManager.HELP_ID, HelpID.PROJECT_VIEWS);

        if (myCopyPasteDelegator != null) {
            sink.set(CutProvider.KEY, myCopyPasteDelegator.getCutProvider());
            sink.set(CopyProvider.KEY, myCopyPasteDelegator.getCopyProvider());
            sink.set(PasteProvider.KEY, myCopyPasteDelegator.getPasteProvider());
        }

        UnifiedProjectViewPaneView view = getCurrentView();
        if (view != null) {
            view.uiDataSnapshot(sink);
        }

        AbstractProjectViewPane currentProjectViewPane = getCurrentProjectViewPane();
        if (currentProjectViewPane != null) {
            currentProjectViewPane.uiDataSnapshot(sink);
        }
    }

    private @Nullable UnifiedProjectViewPaneView getCurrentView() {
        return myCurrentViewId == null ? null : myId2View.get(myCurrentViewId);
    }

    @RequiredUIAccess
    private UnifiedProjectViewPaneView getOrCreateView(AbstractProjectViewPane pane) {
        UnifiedProjectViewPaneView view = myId2View.get(pane.getId());
        if (view != null) {
            return view;
        }

        view = pane instanceof AbstractProjectViewPSIPane psiPane
            ? new UnifiedProjectViewTreePaneView(myProject, psiPane, myTreeExecutorFactory, MyIdeView::new)
            : new UnifiedProjectViewStubPaneView(pane);
        Disposer.register(this, view);

        Element state = myUnappliedPaneState.remove(pane.getId());
        if (state != null) {
            view.readExternal(state);
        }

        myId2View.put(pane.getId(), view);
        return view;
    }

    @Override
    @RequiredUIAccess
    protected void installToolWindow(ToolWindow toolWindow) {
        for (Map.Entry<String, UnifiedProjectViewPaneView> entry : myId2View.entrySet()) {
            UnifiedProjectViewPaneView view = entry.getValue();
            Element paneElement = new Element("pane");
            view.writeExternal(paneElement);
            myUnappliedPaneState.put(entry.getKey(), paneElement);
            Disposer.dispose(view);
        }
        myId2View.clear();

        WrappedLayout viewContentPanel = WrappedLayout.create();
        viewContentPanel.putUserData(UiDataProvider.KEY, this::uiDataSnapshot);
        myViewContentPanel = viewContentPanel;

        if (TargetAWT.to(viewContentPanel) instanceof JComponent popupTarget) {
            myCopyPasteDelegator = new CopyPasteDelegator(myProject, popupTarget) {
                @Override
                @RequiredUIAccess
                protected PsiElement[] getSelectedElements() {
                    UnifiedProjectViewPaneView view = getCurrentView();
                    return view == null ? PsiElement.EMPTY_ARRAY : view.getSelectedPsiElements();
                }
            };

            PopupHandler.installPopupHandlerFromCustomActions(
                popupTarget,
                IdeActions.GROUP_PROJECT_VIEW_POPUP,
                ActionPlaces.PROJECT_VIEW_POPUP
            );
        }
    }

    @Override
    protected Content createPaneContent(String title) {
        return getContentManager().getFactory().createUIContent(myViewContentPanel, title, false);
    }

    @Override
    @RequiredUIAccess
    protected void showPane(AbstractProjectViewPane newPane) {
        UnifiedProjectViewPaneView currentView = getCurrentView();
        if (currentView != null) {
            currentView.onHide();
        }

        UnifiedProjectViewPaneView view = getOrCreateView(newPane);
        if (myViewContentPanel != null) {
            myViewContentPanel.set(view.getComponent());
        }
        myCurrentViewId = newPane.getId();
        myCurrentViewSubId = newPane.getSubId();
        createToolbarActions();

        view.onShow();
    }

    @Override
    @RequiredUIAccess
    protected void onPaneRemoved(AbstractProjectViewPane pane) {
        UnifiedProjectViewPaneView view = myId2View.remove(pane.getId());
        if (view != null) {
            Disposer.dispose(view);
        }
    }

    @Override
    @RequiredUIAccess
    protected ActionCallback updatePane(AbstractProjectViewPane pane) {
        UnifiedProjectViewPaneView view = myId2View.get(pane.getId());
        if (view == null) {
            return ActionCallback.DONE;
        }

        ActionCallback callback = new ActionCallback();
        view.rebuild().whenComplete((result, error) -> callback.setDone());
        return callback;
    }

    @Override
    protected boolean isPaneCreated(AbstractProjectViewPane pane) {
        return myId2View.containsKey(pane.getId());
    }

    @Override
    protected void queuePaneUpdate(AbstractProjectViewPane pane) {
        UnifiedProjectViewPaneView view = myId2View.get(pane.getId());
        if (view != null) {
            view.queueUpdate();
        }
    }

    @Override
    @RequiredUIAccess
    protected void installComparator(AbstractProjectViewPane pane) {
        updatePane(pane);
    }

    @Override
    protected void readNavigatorState(Element navigatorElement) {
        myLoadedNavigatorAttributes.clear();
        for (Attribute attribute : navigatorElement.getAttributes()) {
            myLoadedNavigatorAttributes.add(attribute.clone());
        }
    }

    @Override
    protected void writeNavigatorState(Element navigatorElement) {
        for (Attribute attribute : myLoadedNavigatorAttributes) {
            if (navigatorElement.getAttribute(attribute.getName()) == null) {
                navigatorElement.setAttribute(attribute.clone());
            }
        }
    }

    @Override
    protected void readPaneExternal(AbstractProjectViewPane pane, Element paneElement) throws InvalidDataException {
        super.readPaneExternal(pane, paneElement);

        UnifiedProjectViewPaneView view = myId2View.get(pane.getId());
        if (view != null) {
            view.readExternal(paneElement);
        }
        else {
            myUnappliedPaneState.put(pane.getId(), paneElement.clone());
        }
    }

    @Override
    protected void writePaneExternal(AbstractProjectViewPane pane, Element paneElement) throws WriteExternalException {
        super.writePaneExternal(pane, paneElement);

        UnifiedProjectViewPaneView view = myId2View.get(pane.getId());
        if (view != null) {
            view.writeExternal(paneElement);
        }
    }

    @Override
    @RequiredUIAccess
    public void reRestoreExpandedPaths() {
        UnifiedProjectViewPaneView view = getCurrentView();
        if (view != null) {
            view.reRestoreExpandedPaths();
        }
    }

    @Override
    @RequiredUIAccess
    public void select(Object element, VirtualFile file, boolean requestFocus) {
        selectCB(element, file, requestFocus);
    }

    @Override
    @RequiredUIAccess
    public AsyncResult<Void> selectCB(Object element, VirtualFile file, boolean requestFocus) {
        UnifiedProjectViewPaneView view = getCurrentView();
        if (view == null) {
            return AsyncResult.resolved();
        }

        AsyncResult<Void> result = new AsyncResult<>();
        view.select(element, file, requestFocus).whenComplete((node, error) -> result.setDone());
        return result;
    }

    @Override
    @RequiredUIAccess
    public void scrollFromSource() {
        VirtualFile[] selectedFiles = FileEditorManager.getInstance(myProject).getSelectedFiles();
        if (selectedFiles.length == 0) {
            return;
        }

        select(null, selectedFiles[0], false);
    }

    @Override
    @RequiredUIAccess
    public @Nullable PsiElement getParentOfCurrentSelection() {
        UnifiedProjectViewPaneView view = getCurrentView();
        return view == null ? null : view.getParentOfCurrentSelection();
    }

    @Override
    @RequiredUIAccess
    public void changeView() {
        List<AbstractProjectViewPane> views = new ArrayList<>(myId2Pane.values());
        views.remove(getCurrentProjectViewPane());
        views.sort(PANE_WEIGHT_COMPARATOR);
        if (views.isEmpty() || myViewContentPanel == null) {
            return;
        }

        JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<>(IdeLocalize.titlePopupViews().get(), views) {
            @Override
            public String getTextFor(AbstractProjectViewPane value) {
                return value.getTitle().get();
            }

            @Override
            @RequiredUIAccess
            public @Nullable PopupStep onChosen(AbstractProjectViewPane selectedValue, boolean finalChoice) {
                return doFinalStep(() -> changeView(selectedValue.getId()));
            }
        }).showBy(myViewContentPanel, null);
    }

    private final class MyIdeView implements IdeView {
        private final PsiDirectory[] myDirectories;

        private MyIdeView(PsiDirectory[] directories) {
            myDirectories = directories;
        }

        @Override
        @RequiredUIAccess
        public void selectElement(PsiElement element) {
            selectPsiElement(element, false);

            if (element != null && !(element instanceof PsiDirectory)) {
                EditorHelper.openInEditor(element, false);
            }
        }

        @Override
        @RequiredUIAccess
        public PsiDirectory[] getDirectories() {
            return myDirectories;
        }

        @Override
        @RequiredUIAccess
        public PsiDirectory getOrChooseDirectory() {
            return DirectoryChooserUtil.getOrChooseDirectory(this);
        }
    }
}
