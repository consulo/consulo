/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.application.dumb.DumbAware;
import consulo.component.persist.PersistentStateComponentWithAsyncGet;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.ide.impl.ProjectViewSelectInTarget;
import consulo.ide.impl.idea.ide.projectView.actions.ProjectViewToolbarGroup;
import consulo.ide.impl.idea.ide.scopeView.ScopeViewPane;
import consulo.ide.localize.IdeLocalize;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiPackageSupportProviders;
import consulo.language.psi.PsiUtilCore;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.view.ProjectViewPane;
import consulo.project.ui.view.SelectInTarget;
import consulo.project.ui.view.internal.ProjectViewEx;
import consulo.project.ui.view.internal.ProjectViewSharedSettings;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.UIAccess;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowContentUiType;
import consulo.ui.image.Image;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncResult;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.dataholder.Key;
import consulo.util.lang.Couple;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.virtualFileSystem.VirtualFile;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.*;

public abstract class BaseProjectViewImpl implements ProjectViewEx, PersistentStateComponentWithAsyncGet<Element>, Disposable {
    private static final Logger LOG = Logger.getInstance(BaseProjectViewImpl.class);
    protected static final Key<String> ID_KEY = Key.create("pane-id");
    protected static final Key<String> SUB_ID_KEY = Key.create("pane-sub-id");

    private boolean isInitialized;
    private boolean myExtensionsLoaded = false;
    protected final Project myProject;
    private final ProjectViewSharedSettings myProjectViewSharedSettings;

    // + options
    private final Map<String, Boolean> myFlattenPackages = new HashMap<>();
    private static final boolean ourFlattenPackagesDefaults = false;
    private final Map<String, Boolean> myShowMembers = new HashMap<>();
    private static final boolean ourShowMembersDefaults = false;
    private final Map<String, Boolean> myManualOrder = new HashMap<>();
    private static final boolean ourManualOrderDefaults = false;
    private final Map<String, Boolean> mySortByType = new HashMap<>();
    private static final boolean ourSortByTypeDefaults = false;
    private final Map<String, Boolean> myShowModules = new HashMap<>();
    private final Map<String, Boolean> myShowLibraryContents = new HashMap<>();
    private final Map<String, Boolean> myHideEmptyPackages = new HashMap<>();
    private static final boolean ourHideEmptyPackagesDefaults = true;
    private final Map<String, Boolean> myAbbreviatePackageNames = new HashMap<>();
    private static final boolean ourAbbreviatePackagesDefaults = false;
    private final Map<String, Boolean> myAutoscrollToSource = new HashMap<>();
    private final Map<String, Boolean> myAutoscrollFromSource = new HashMap<>();

    private boolean myFoldersAlwaysOnTop = true;

    protected String myCurrentViewId;
    protected String myCurrentViewSubId;

    protected final Map<String, AbstractProjectViewPane> myId2Pane = new LinkedHashMap<>();
    protected final Collection<AbstractProjectViewPane> myUninitializedPanes = new HashSet<>();

    protected DefaultActionGroup myActionGroup;
    private String mySavedPaneId = ProjectViewPaneImpl.ID;
    private String mySavedPaneSubId;

    private static final String ELEMENT_NAVIGATOR = "navigator";
    private static final String ELEMENT_PANES = "panes";
    private static final String ELEMENT_PANE = "pane";
    private static final String ATTRIBUTE_CURRENT_VIEW = "currentView";
    private static final String ATTRIBUTE_CURRENT_SUBVIEW = "currentSubView";
    private static final String ELEMENT_FLATTEN_PACKAGES = "flattenPackages";
    private static final String ELEMENT_SHOW_MEMBERS = "showMembers";
    private static final String ELEMENT_SHOW_MODULES = "showModules";
    private static final String ELEMENT_SHOW_LIBRARY_CONTENTS = "showLibraryContents";
    private static final String ELEMENT_HIDE_EMPTY_PACKAGES = "hideEmptyPackages";
    private static final String ELEMENT_ABBREVIATE_PACKAGE_NAMES = "abbreviatePackageNames";
    private static final String ELEMENT_AUTOSCROLL_TO_SOURCE = "autoscrollToSource";
    private static final String ELEMENT_AUTOSCROLL_FROM_SOURCE = "autoscrollFromSource";
    private static final String ELEMENT_SORT_BY_TYPE = "sortByType";
    private static final String ELEMENT_FOLDERS_ALWAYS_ON_TOP = "foldersAlwaysOnTop";
    private static final String ELEMENT_MANUAL_ORDER = "manualOrder";

    private static final String ATTRIBUTE_ID = "id";
    protected static final Comparator<AbstractProjectViewPane> PANE_WEIGHT_COMPARATOR = (o1, o2) -> o1.getWeight() - o2.getWeight();
    private final Map<String, Element> myUninitializedPaneState = new HashMap<>();
    private final Map<String, SelectInTarget> mySelectInTargets = new LinkedHashMap<>();
    protected ContentManager myContentManager;

    protected BaseProjectViewImpl(Project project, ProjectViewSharedSettings projectViewSharedSettings) {
        myProject = project;
        myProjectViewSharedSettings = projectViewSharedSettings;

        project.getMessageBus().connect(this).subscribe(ModuleRootListener.class, new ModuleRootListener() {
            @Override
            @RequiredUIAccess
            public void rootsChanged(ModuleRootEvent event) {
                refresh();
            }
        });
    }

    protected abstract Content createPaneContent(String title);

    @RequiredUIAccess
    protected abstract void showPane(AbstractProjectViewPane newPane);

    @RequiredUIAccess
    protected void installToolWindow(ToolWindow toolWindow) {
    }

    @RequiredUIAccess
    protected void onPaneRemoved(AbstractProjectViewPane pane) {
    }

    @RequiredUIAccess
    protected ActionCallback updatePane(AbstractProjectViewPane pane) {
        return pane.updateFromRoot(false);
    }

    protected boolean isPaneCreated(AbstractProjectViewPane pane) {
        return pane.getTree() != null;
    }

    protected void queuePaneUpdate(AbstractProjectViewPane pane) {
        pane.queueUpdate();
    }

    @RequiredUIAccess
    protected void installComparator(AbstractProjectViewPane pane) {
        pane.installComparator();
    }

    @RequiredUIAccess
    protected Runnable saveSelection(@Nullable AbstractProjectViewPane pane) {
        return () -> {
        };
    }

    protected void addAutoScrollActions(DefaultActionGroup actionGroup) {
    }

    protected void readNavigatorState(Element navigatorElement) {
    }

    protected void writeNavigatorState(Element navigatorElement) {
    }

    protected void readPaneExternal(AbstractProjectViewPane pane, Element paneElement) throws InvalidDataException {
        pane.readExternal(paneElement);
    }

    protected void writePaneExternal(AbstractProjectViewPane pane, Element paneElement) throws WriteExternalException {
        pane.writeExternal(paneElement);
    }

    @Override
    @RequiredUIAccess
    public synchronized void addProjectPane(ProjectViewPane pane) {
        myUninitializedPanes.add((AbstractProjectViewPane) pane);
        SelectInTarget selectInTarget = pane.createSelectInTarget();
        if (selectInTarget != null) {
            mySelectInTargets.put(pane.getId(), selectInTarget);
        }
        if (isInitialized) {
            doAddUninitializedPanes();
        }
    }

    @Override
    @RequiredUIAccess
    public synchronized void removeProjectPane(ProjectViewPane pane) {
        UIAccess.assertIsUIThread();
        myUninitializedPanes.remove(pane);
        //assume we are completely initialized here
        String idToRemove = pane.getId();

        if (!myId2Pane.containsKey(idToRemove)) {
            return;
        }
        for (int i = getContentManager().getContentCount() - 1; i >= 0; i--) {
            Content content = getContentManager().getContent(i);
            String id = content != null ? content.getUserData(ID_KEY) : null;
            if (id != null && id.equals(idToRemove)) {
                getContentManager().removeContent(content, true);
            }
        }
        AbstractProjectViewPane removed = myId2Pane.remove(idToRemove);
        mySelectInTargets.remove(idToRemove);
        if (removed != null) {
            onPaneRemoved(removed);
        }
        viewSelectionChanged();
    }

    @RequiredUIAccess
    private synchronized void doAddUninitializedPanes() {
        for (AbstractProjectViewPane pane : myUninitializedPanes) {
            doAddPane(pane);
        }
        Content[] contents = getContentManager().getContents();
        for (int i = 1; i < contents.length; i++) {
            Content content = contents[i];
            Content prev = contents[i - 1];
            if (!StringUtil.equals(content.getUserData(ID_KEY), prev.getUserData(ID_KEY))
                && prev.getUserData(SUB_ID_KEY) != null
                && content.getSeparator() == null) {
                content.setSeparator("");
            }
        }

        String selectID = null;
        String selectSubID = null;

        // try to find saved selected view...
        for (Content content : contents) {
            String id = content.getUserData(ID_KEY);
            String subId = content.getUserData(SUB_ID_KEY);
            if (id != null && id.equals(mySavedPaneId) && StringUtil.equals(subId, mySavedPaneSubId)) {
                selectID = id;
                selectSubID = subId;
                mySavedPaneId = null;
                mySavedPaneSubId = null;
                break;
            }
        }

        // saved view not found (plugin disabled, ID changed etc.) - select first available view...
        if (selectID == null && contents.length > 0 && myCurrentViewId == null) {
            Content content = contents[0];
            selectID = content.getUserData(ID_KEY);
            selectSubID = content.getUserData(SUB_ID_KEY);
        }

        if (selectID != null) {
            changeView(selectID, selectSubID);
        }

        myUninitializedPanes.clear();
    }

    @RequiredUIAccess
    private void doAddPane(AbstractProjectViewPane newPane) {
        UIAccess.assertIsUIThread();
        int index;
        ContentManager manager = getContentManager();
        for (index = 0; index < manager.getContentCount(); index++) {
            Content content = manager.getContent(index);
            String id = content.getUserData(ID_KEY);
            AbstractProjectViewPane pane = myId2Pane.get(id);

            int comp = PANE_WEIGHT_COMPARATOR.compare(pane, newPane);
            LOG.assertTrue(
                comp != 0,
                "Project view pane " + newPane +
                    " has the same weight as " + pane +
                    ". Please make sure that you overload getWeight() and return a distinct weight value."
            );
            if (comp > 0) {
                break;
            }
        }
        String id = newPane.getId();
        myId2Pane.put(id, newPane);
        String[] subIds = newPane.getSubIds();
        subIds = subIds.length == 0 ? new String[]{null} : subIds;
        boolean first = true;
        for (String subId : subIds) {
            LocalizeValue title = subId != null ? newPane.getPresentableSubIdName(subId) : newPane.getTitle();
            Content content = createPaneContent(title.get());
            content.setTabName(title.get());
            content.putUserData(ID_KEY, id);
            content.putUserData(SUB_ID_KEY, subId);
            content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
            if (first && subId != null) {
                content.setSeparator(newPane.getTitle().get());
            }
            manager.addContent(content, index++);
            first = false;
        }
    }

    @Override
    @RequiredUIAccess
    public void setupToolWindow(ToolWindow toolWindow, boolean loadPaneExtensions) {
        UIAccess.assertIsUIThread();
        myActionGroup = new DefaultActionGroup();

        if (myContentManager != null && myContentManager != toolWindow.getContentManager()) {
            if (myCurrentViewId != null) {
                mySavedPaneId = myCurrentViewId;
                mySavedPaneSubId = myCurrentViewSubId;
            }
            myUninitializedPanes.addAll(myId2Pane.values());
            myId2Pane.clear();
            myCurrentViewId = null;
            myCurrentViewSubId = null;
        }

        myContentManager = toolWindow.getContentManager();

        toolWindow.setDefaultContentUiType(ToolWindowContentUiType.COMBO);
        toolWindow.setAdditionalGearActions(myActionGroup);
        toolWindow.putUserData(ToolWindow.HIDE_ID_LABEL, Boolean.TRUE);

        installToolWindow(toolWindow);

        if (loadPaneExtensions) {
            ensurePanesLoaded();
        }
        isInitialized = true;
        doAddUninitializedPanes();

        getContentManager().addContentManagerListener(new ContentManagerListener() {
            @Override
            @RequiredUIAccess
            public void selectionChanged(ContentManagerEvent event) {
                if (event.getOperation() == ContentManagerEvent.ContentOperation.add) {
                    viewSelectionChanged();
                }
            }
        });
        viewSelectionChanged();
    }

    @RequiredUIAccess
    protected void ensurePanesLoaded() {
        if (myExtensionsLoaded) {
            return;
        }
        myExtensionsLoaded = true;
        List<AbstractProjectViewPane> extensions = new ArrayList<>(AbstractProjectViewPane.EP_NAME.getExtensionList(myProject));
        extensions.sort(PANE_WEIGHT_COMPARATOR);
        for (AbstractProjectViewPane pane : extensions) {
            if (myUninitializedPaneState.containsKey(pane.getId())) {
                try {
                    readPaneExternal(pane, myUninitializedPaneState.get(pane.getId()));
                }
                catch (InvalidDataException e) {
                    // ignore
                }
                myUninitializedPaneState.remove(pane.getId());
            }
            if (pane.isInitiallyVisible() && !myId2Pane.containsKey(pane.getId())) {
                addProjectPane(pane);
            }
        }
    }

    @RequiredUIAccess
    protected boolean viewSelectionChanged() {
        Content content = getContentManager().getSelectedContent();
        if (content == null) {
            return false;
        }
        String id = content.getUserData(ID_KEY);
        String subId = content.getUserData(SUB_ID_KEY);
        if (content.equals(Couple.of(myCurrentViewId, myCurrentViewSubId))) {
            return false;
        }
        AbstractProjectViewPane newPane = getProjectViewPaneById(id);
        if (newPane == null) {
            return false;
        }
        newPane.setSubId(subId);
        showPane(newPane);
        ProjectViewSelectInTarget target = getProjectViewSelectInTarget(newPane);
        if (target != null) {
            target.setSubId(subId);
        }
        if (isAutoscrollFromSource(id)) {
            scrollFromSource();
        }
        return true;
    }

    @RequiredUIAccess
    protected void createToolbarActions() {
        if (myActionGroup == null) {
            return;
        }
        myActionGroup.removeAll();
        myActionGroup.addAction(new PaneOptionAction(
            myFlattenPackages,
            IdeLocalize.actionFlattenPackages(),
            IdeLocalize.actionFlattenPackages(),
            PlatformIconGroup.objectbrowserFlattenpackages(),
            ourFlattenPackagesDefaults
        ) {
            @Override
            @RequiredUIAccess
            public void setSelected(AnActionEvent event, boolean flag) {
                AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
                Runnable restoreSelection = saveSelection(viewPane);

                setFlattenPackages(flag, viewPane.getId());

                super.setSelected(event, flag);

                restoreSelection.run();
            }

            @Override
            public boolean isSelected(AnActionEvent event) {
                return getGlobalOptions().isFlattenPackages();
            }

            @Override
            public void update(AnActionEvent e) {
                super.update(e);
                Project project = e.getRequiredData(Project.KEY);
                if (!PsiPackageSupportProviders.isPackageSupported(project)) {
                    e.getPresentation().setVisible(false);
                }
            }
        }).setAsSecondary(true);

        class FlattenPackagesDependableAction extends PaneOptionAction {
            FlattenPackagesDependableAction(
                Map<String, Boolean> optionsMap,
                LocalizeValue text,
                LocalizeValue description,
                Image icon,
                boolean optionDefaultValue
            ) {
                super(optionsMap, text, description, icon, optionDefaultValue);
            }

            @Override
            @RequiredUIAccess
            public void setSelected(AnActionEvent event, boolean flag) {
                getGlobalOptions().setFlattenPackages(flag);

                super.setSelected(event, flag);
            }

            @Override
            public void update(AnActionEvent e) {
                super.update(e);
                Project project = e.getRequiredData(Project.KEY);
                Presentation presentation = e.getPresentation();
                presentation.setVisible(PsiPackageSupportProviders.isPackageSupported(project) && isFlattenPackages(myCurrentViewId));
            }
        }
        myActionGroup.addAction(new HideEmptyMiddlePackagesAction()).setAsSecondary(true);
        myActionGroup.addAction(new FlattenPackagesDependableAction(
            myAbbreviatePackageNames,
            IdeLocalize.actionAbbreviateQualifiedPackageNames(),
            IdeLocalize.actionAbbreviateQualifiedPackageNames(),
            PlatformIconGroup.objectbrowserAbbreviatepackagenames(),
            ourAbbreviatePackagesDefaults
        ) {
            @Override
            public boolean isSelected(AnActionEvent event) {
                return isFlattenPackages(myCurrentViewId) && isAbbreviatePackageNames(myCurrentViewId);
            }

            @Override
            @RequiredUIAccess
            public void setSelected(AnActionEvent event, boolean flag) {
                setAbbreviatePackageNames(flag, myCurrentViewId);

                setPaneOption(myOptionsMap, flag, myCurrentViewId, true);
            }

            @RequiredUIAccess
            @Override
            public void update(AnActionEvent e) {
                super.update(e);
                if (ScopeViewPane.ID.equals(myCurrentViewId)) {
                    e.getPresentation().setEnabled(false);
                }
            }
        }).setAsSecondary(true);
        if (isShowMembersOptionSupported()) {
            myActionGroup.addAction(new PaneOptionAction(
                myShowMembers,
                IdeLocalize.actionShowMembers(),
                IdeLocalize.actionShowHideMembers(),
                PlatformIconGroup.objectbrowserShowmembers(),
                ourShowMembersDefaults
            ) {
                @Override
                public boolean isSelected(AnActionEvent event) {
                    return getGlobalOptions().isShowMembers();
                }

                @Override
                @RequiredUIAccess
                public void setSelected(AnActionEvent event, boolean flag) {
                    getGlobalOptions().setShowMembers(flag);

                    super.setSelected(event, flag);
                }
            }).setAsSecondary(true);
        }
        addAutoScrollActions(myActionGroup);
        myActionGroup.addAction(new ManualOrderAction()).setAsSecondary(true);
        myActionGroup.addAction(new SortByTypeAction()).setAsSecondary(true);
        myActionGroup.addAction(new FoldersAlwaysOnTopAction()).setAsSecondary(true);

        getCurrentProjectViewPane().addToolbarActionsImpl(myActionGroup);

        List<AnAction> titleActions = new ArrayList<>();
        createTitleActions(titleActions);
        if (!titleActions.isEmpty()) {
            ToolWindow window = ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.PROJECT_VIEW);
            if (window != null) {
                window.setTitleActions(titleActions.toArray(AnAction[]::new));
            }
        }
    }

    protected void createTitleActions(List<? super AnAction> titleActions) {
        ProjectViewToolbarGroup action = ActionManager.getInstance().getAction(ProjectViewToolbarGroup.class);
        titleActions.add(action);
    }

    protected boolean isShowMembersOptionSupported() {
        return true;
    }

    @Override
    @RequiredUIAccess
    public AbstractProjectViewPane getProjectViewPaneById(String id) {
        if (!myProject.getApplication().isUnitTestMode()) {   // most tests don't need all panes to be loaded
            ensurePanesLoaded();
        }

        AbstractProjectViewPane pane = myId2Pane.get(id);
        if (pane != null) {
            return pane;
        }
        for (AbstractProjectViewPane viewPane : myUninitializedPanes) {
            if (viewPane.getId().equals(id)) {
                return viewPane;
            }
        }
        return null;
    }

    @Override
    @RequiredUIAccess
    public AbstractProjectViewPane getCurrentProjectViewPane() {
        return getProjectViewPaneById(myCurrentViewId);
    }

    @Override
    @RequiredUIAccess
    public void refresh() {
        AbstractProjectViewPane currentProjectViewPane = getCurrentProjectViewPane();
        if (currentProjectViewPane != null) {
            // may be null for e.g. default project
            updatePane(currentProjectViewPane);
        }
    }

    @Override
    public void dispose() {
    }

    @Override
    public String getCurrentViewId() {
        return myCurrentViewId;
    }

    protected SelectInTarget getCurrentSelectInTarget() {
        return getSelectInTarget(getCurrentViewId());
    }

    private SelectInTarget getSelectInTarget(String id) {
        return mySelectInTargets.get(id);
    }

    protected ProjectViewSelectInTarget getProjectViewSelectInTarget(AbstractProjectViewPane pane) {
        SelectInTarget target = getSelectInTarget(pane.getId());
        return target instanceof ProjectViewSelectInTarget projectViewSelectInTarget ? projectViewSelectInTarget : null;
    }

    public ContentManager getContentManager() {
        if (myContentManager == null) {
            ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.PROJECT_VIEW).getContentManager();
        }
        return myContentManager;
    }

    protected class PaneOptionAction extends ToggleAction implements DumbAware {
        protected final Map<String, Boolean> myOptionsMap;
        private final boolean myOptionDefaultValue;

        protected PaneOptionAction(
            Map<String, Boolean> optionsMap,
            LocalizeValue text,
            LocalizeValue description,
            Image icon,
            boolean optionDefaultValue
        ) {
            super(text, description, icon);
            myOptionsMap = optionsMap;
            myOptionDefaultValue = optionDefaultValue;
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return getPaneOptionValue(myOptionsMap, myCurrentViewId, myOptionDefaultValue);
        }

        @Override
        @RequiredUIAccess
        public void setSelected(AnActionEvent event, boolean flag) {
            setPaneOption(myOptionsMap, flag, myCurrentViewId, true);
        }
    }

    @Override
    @RequiredUIAccess
    public void changeView(String viewId) {
        changeView(viewId, null);
    }

    @Override
    @RequiredUIAccess
    public void changeView(String viewId, @Nullable String subId) {
        changeViewCB(viewId, subId);
    }

    @Override
    @RequiredUIAccess
    public AsyncResult<Void> changeViewCB(String viewId, String subId) {
        AbstractProjectViewPane pane = getProjectViewPaneById(viewId);
        LOG.assertTrue(pane != null, "Project view pane not found: " + viewId + "; subId:" + subId + "; project: " + myProject);
        if (!viewId.equals(getCurrentViewId()) || subId != null && !subId.equals(pane.getSubId())) {
            for (Content content : getContentManager().getContents()) {
                if (viewId.equals(content.getUserData(ID_KEY)) && StringUtil.equals(subId, content.getUserData(SUB_ID_KEY))) {
                    return getContentManager().setSelectedContentCB(content);
                }
            }
        }
        return AsyncResult.rejected();
    }

    @Override
    @RequiredUIAccess
    public void selectPsiElement(PsiElement element, boolean requestFocus) {
        if (element == null) {
            return;
        }
        VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);
        select(element, virtualFile, requestFocus);
    }

    private static void readOption(Element node, Map<String, Boolean> options) {
        if (node == null) {
            return;
        }
        for (Attribute attribute : node.getAttributes()) {
            options.put(attribute.getName(), Boolean.TRUE.toString().equals(attribute.getValue()) ? Boolean.TRUE : Boolean.FALSE);
        }
    }

    private static void writeOption(
        Element parentNode,
        Map<String, Boolean> optionsForPanes,
        String optionName
    ) {
        Element e = new Element(optionName);
        for (Map.Entry<String, Boolean> entry : optionsForPanes.entrySet()) {
            String key = entry.getKey();
            if (key != null) { //SCR48267
                e.setAttribute(key, Boolean.toString(entry.getValue()));
            }
        }

        parentNode.addContent(e);
    }

    @Override
    public void loadState(Element parentNode) {
        Element navigatorElement = parentNode.getChild(ELEMENT_NAVIGATOR);
        if (navigatorElement != null) {
            mySavedPaneId = navigatorElement.getAttributeValue(ATTRIBUTE_CURRENT_VIEW);
            mySavedPaneSubId = navigatorElement.getAttributeValue(ATTRIBUTE_CURRENT_SUBVIEW);
            if (mySavedPaneId == null) {
                mySavedPaneId = ProjectViewPaneImpl.ID;
                mySavedPaneSubId = null;
            }
            readOption(navigatorElement.getChild(ELEMENT_FLATTEN_PACKAGES), myFlattenPackages);
            readOption(navigatorElement.getChild(ELEMENT_SHOW_MEMBERS), myShowMembers);
            readOption(navigatorElement.getChild(ELEMENT_SHOW_MODULES), myShowModules);
            readOption(navigatorElement.getChild(ELEMENT_SHOW_LIBRARY_CONTENTS), myShowLibraryContents);
            readOption(navigatorElement.getChild(ELEMENT_HIDE_EMPTY_PACKAGES), myHideEmptyPackages);
            readOption(navigatorElement.getChild(ELEMENT_ABBREVIATE_PACKAGE_NAMES), myAbbreviatePackageNames);
            readOption(navigatorElement.getChild(ELEMENT_AUTOSCROLL_TO_SOURCE), myAutoscrollToSource);
            readOption(navigatorElement.getChild(ELEMENT_AUTOSCROLL_FROM_SOURCE), myAutoscrollFromSource);
            readOption(navigatorElement.getChild(ELEMENT_SORT_BY_TYPE), mySortByType);
            readOption(navigatorElement.getChild(ELEMENT_MANUAL_ORDER), myManualOrder);

            Element foldersElement = navigatorElement.getChild(ELEMENT_FOLDERS_ALWAYS_ON_TOP);
            if (foldersElement != null) {
                myFoldersAlwaysOnTop = Boolean.valueOf(foldersElement.getAttributeValue("value"));
            }

            readNavigatorState(navigatorElement);
        }
        Element panesElement = parentNode.getChild(ELEMENT_PANES);
        if (panesElement != null) {
            readPaneState(panesElement);
        }
    }

    private void readPaneState(Element panesElement) {
        List<Element> paneElements = panesElement.getChildren(ELEMENT_PANE);

        for (Element paneElement : paneElements) {
            String paneId = paneElement.getAttributeValue(ATTRIBUTE_ID);
            AbstractProjectViewPane pane = myId2Pane.get(paneId);
            if (pane != null) {
                try {
                    readPaneExternal(pane, paneElement);
                }
                catch (InvalidDataException e) {
                    // ignore
                }
            }
            else {
                myUninitializedPaneState.put(paneId, paneElement);
            }
        }
    }

    @Override
    public Coroutine<?, Element> getStateAsync() {
        return UIAction.<Void, Element>apply((input, continuation) -> writeState()).toCoroutine();
    }

    @RequiredUIAccess
    protected Element writeState() {
        Element parentNode = new Element("projectView");
        Element navigatorElement = new Element(ELEMENT_NAVIGATOR);
        AbstractProjectViewPane currentPane = getCurrentProjectViewPane();
        if (currentPane != null) {
            navigatorElement.setAttribute(ATTRIBUTE_CURRENT_VIEW, currentPane.getId());
            String subId = currentPane.getSubId();
            if (subId != null) {
                navigatorElement.setAttribute(ATTRIBUTE_CURRENT_SUBVIEW, subId);
            }
        }
        writeOption(navigatorElement, myFlattenPackages, ELEMENT_FLATTEN_PACKAGES);
        writeOption(navigatorElement, myShowMembers, ELEMENT_SHOW_MEMBERS);
        writeOption(navigatorElement, myShowModules, ELEMENT_SHOW_MODULES);
        writeOption(navigatorElement, myShowLibraryContents, ELEMENT_SHOW_LIBRARY_CONTENTS);
        writeOption(navigatorElement, myHideEmptyPackages, ELEMENT_HIDE_EMPTY_PACKAGES);
        writeOption(navigatorElement, myAbbreviatePackageNames, ELEMENT_ABBREVIATE_PACKAGE_NAMES);
        writeOption(navigatorElement, myAutoscrollToSource, ELEMENT_AUTOSCROLL_TO_SOURCE);
        writeOption(navigatorElement, myAutoscrollFromSource, ELEMENT_AUTOSCROLL_FROM_SOURCE);
        writeOption(navigatorElement, mySortByType, ELEMENT_SORT_BY_TYPE);
        writeOption(navigatorElement, myManualOrder, ELEMENT_MANUAL_ORDER);

        Element foldersElement = new Element(ELEMENT_FOLDERS_ALWAYS_ON_TOP);
        foldersElement.setAttribute("value", Boolean.toString(myFoldersAlwaysOnTop));
        navigatorElement.addContent(foldersElement);

        writeNavigatorState(navigatorElement);
        parentNode.addContent(navigatorElement);

        Element panesElement = new Element(ELEMENT_PANES);
        writePaneState(panesElement);
        parentNode.addContent(panesElement);
        return parentNode;
    }

    private void writePaneState(Element panesElement) {
        for (AbstractProjectViewPane pane : myId2Pane.values()) {
            Element paneElement = new Element(ELEMENT_PANE);
            paneElement.setAttribute(ATTRIBUTE_ID, pane.getId());
            try {
                writePaneExternal(pane, paneElement);
            }
            catch (WriteExternalException e) {
                continue;
            }
            panesElement.addContent(paneElement);
        }
        for (Element element : myUninitializedPaneState.values()) {
            panesElement.addContent(element.clone());
        }
    }

    public ProjectViewSharedSettings getGlobalOptions() {
        return myProjectViewSharedSettings;
    }

    @Override
    public boolean isAutoscrollToSource(String paneId) {
        return getGlobalOptions().isAutoscrollToSource();
    }

    public void setAutoscrollToSource(boolean autoscrollMode, String paneId) {
        getGlobalOptions().setAutoscrollToSource(autoscrollMode);

        myAutoscrollToSource.put(paneId, autoscrollMode);
    }

    @Override
    public boolean isAutoscrollFromSource(String paneId) {
        return getGlobalOptions().isAutoscrollFromSource();
    }

    @RequiredUIAccess
    public void setAutoscrollFromSource(boolean autoscrollMode, String paneId) {
        getGlobalOptions().setAutoscrollFromSource(autoscrollMode);

        setPaneOption(myAutoscrollFromSource, autoscrollMode, paneId, false);
    }

    @Override
    public boolean isFlattenPackages(String paneId) {
        return getGlobalOptions().isFlattenPackages();
    }

    @RequiredUIAccess
    public void setFlattenPackages(boolean flattenPackages, String paneId) {
        getGlobalOptions().setFlattenPackages(flattenPackages);

        for (String pane : myFlattenPackages.keySet()) {
            setPaneOption(myFlattenPackages, flattenPackages, pane, true);
        }

        setPaneOption(myFlattenPackages, flattenPackages, paneId, true);
    }

    @Override
    public boolean isFoldersAlwaysOnTop() {
        return getGlobalOptions().isFoldersAlwaysOnTop();
    }

    @RequiredUIAccess
    public void setFoldersAlwaysOnTop(boolean foldersAlwaysOnTop) {
        getGlobalOptions().setFoldersAlwaysOnTop(foldersAlwaysOnTop);

        if (myFoldersAlwaysOnTop != foldersAlwaysOnTop) {
            myFoldersAlwaysOnTop = foldersAlwaysOnTop;
            for (AbstractProjectViewPane pane : myId2Pane.values()) {
                if (isPaneCreated(pane)) {
                    updatePane(pane);
                }
            }
        }
    }

    @Override
    public boolean isShowMembers(String paneId) {
        return getGlobalOptions().isShowMembers();
    }

    @RequiredUIAccess
    public void setShowMembers(boolean showMembers, String paneId) {
        setPaneOption(myShowMembers, showMembers, paneId, true);
    }

    @Override
    public boolean isHideEmptyMiddlePackages(String paneId) {
        return getGlobalOptions().isHideEmptyPackages();
    }

    @Override
    public boolean isAbbreviatePackageNames(String paneId) {
        return getGlobalOptions().isAbbreviatePackages();
    }

    @Override
    public boolean isShowLibraryContents(String paneId) {
        return getGlobalOptions().isShowLibraryContents();
    }

    @Override
    @RequiredUIAccess
    public void setShowLibraryContents(boolean showLibraryContents, String paneId) {
        getGlobalOptions().setShowLibraryContents(showLibraryContents);

        setPaneOption(myShowLibraryContents, showLibraryContents, paneId, true);
    }

    @RequiredUIAccess
    public ActionCallback setShowLibraryContentsCB(boolean showLibraryContents, String paneId) {
        return setPaneOption(myShowLibraryContents, showLibraryContents, paneId, true);
    }

    @Override
    public boolean isShowModules(String paneId) {
        return getGlobalOptions().getShowModules();
    }

    @Override
    @RequiredUIAccess
    public void setShowModules(boolean showModules, String paneId) {
        getGlobalOptions().setShowModules(showModules);

        setPaneOption(myShowModules, showModules, paneId, true);
    }

    @Override
    @RequiredUIAccess
    public void setHideEmptyPackages(boolean hideEmptyPackages, String paneId) {
        getGlobalOptions().setHideEmptyPackages(hideEmptyPackages);

        for (String pane : myHideEmptyPackages.keySet()) {
            setPaneOption(myHideEmptyPackages, hideEmptyPackages, pane, true);
        }

        setPaneOption(myHideEmptyPackages, hideEmptyPackages, paneId, true);
    }

    @Override
    @RequiredUIAccess
    public void setAbbreviatePackageNames(boolean abbreviatePackageNames, String paneId) {
        getGlobalOptions().setAbbreviatePackages(abbreviatePackageNames);

        setPaneOption(myAbbreviatePackageNames, abbreviatePackageNames, paneId, true);
    }

    @RequiredUIAccess
    protected ActionCallback setPaneOption(Map<String, Boolean> optionsMap, boolean value, String paneId, boolean updatePane) {
        if (paneId != null) {
            optionsMap.put(paneId, value);
            if (updatePane) {
                AbstractProjectViewPane pane = getProjectViewPaneById(paneId);
                if (pane != null) {
                    return updatePane(pane);
                }
            }
        }
        return ActionCallback.DONE;
    }

    private static boolean getPaneOptionValue(Map<String, Boolean> optionsMap, String paneId, boolean defaultValue) {
        Boolean value = optionsMap.get(paneId);
        return value == null ? defaultValue : value;
    }

    private class HideEmptyMiddlePackagesAction extends PaneOptionAction {
        private HideEmptyMiddlePackagesAction() {
            super(myHideEmptyPackages, LocalizeValue.empty(), LocalizeValue.empty(), null, ourHideEmptyPackagesDefaults);
        }

        @Override
        @RequiredUIAccess
        public void setSelected(AnActionEvent event, boolean flag) {
            AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
            Runnable restoreSelection = saveSelection(viewPane);

            getGlobalOptions().setHideEmptyPackages(flag);

            super.setSelected(event, flag);

            restoreSelection.run();
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return getGlobalOptions().isHideEmptyPackages();
        }

        @Override
        public void update(AnActionEvent e) {
            super.update(e);
            Presentation presentation = e.getPresentation();
            Project project = e.getRequiredData(Project.KEY);
            if (!PsiPackageSupportProviders.isPackageSupported(project)) {
                presentation.setVisible(false);
                return;
            }
            if (isHideEmptyMiddlePackages(myCurrentViewId)) {
                presentation.setText(IdeLocalize.actionHideEmptyMiddlePackages());
                presentation.setDescription(IdeLocalize.actionShowHideEmptyMiddlePackages());
            }
            else {
                presentation.setText(IdeLocalize.actionCompactEmptyMiddlePackages());
                presentation.setDescription(IdeLocalize.actionShowCompactEmptyMiddlePackages());
            }
        }
    }

    @Override
    public boolean isManualOrder(String paneId) {
        return getPaneOptionValue(myManualOrder, paneId, ourManualOrderDefaults);
    }

    @Override
    @RequiredUIAccess
    public void setManualOrder(String paneId, boolean enabled) {
        setPaneOption(myManualOrder, enabled, paneId, false);
        AbstractProjectViewPane pane = getProjectViewPaneById(paneId);
        installComparator(pane);
    }

    @Override
    public boolean isSortByType(String paneId) {
        return getPaneOptionValue(mySortByType, paneId, ourSortByTypeDefaults);
    }

    @Override
    @RequiredUIAccess
    public void setSortByType(String paneId, boolean sortByType) {
        setPaneOption(mySortByType, sortByType, paneId, false);
        AbstractProjectViewPane pane = getProjectViewPaneById(paneId);
        installComparator(pane);
    }

    private class ManualOrderAction extends ToggleAction implements DumbAware {
        private ManualOrderAction() {
            super(
                IdeLocalize.actionManualOrder(),
                IdeLocalize.actionManualOrder(),
                PlatformIconGroup.objectbrowserSorted()
            );
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return isManualOrder(getCurrentViewId());
        }

        @Override
        @RequiredUIAccess
        public void setSelected(AnActionEvent event, boolean flag) {
            setManualOrder(getCurrentViewId(), flag);
        }

        @RequiredUIAccess
        @Override
        public void update(AnActionEvent e) {
            super.update(e);
            Presentation presentation = e.getPresentation();
            AbstractProjectViewPane pane = getCurrentProjectViewPane();
            presentation.setEnabledAndVisible(pane != null && pane.supportsManualOrder());
        }
    }

    private class SortByTypeAction extends ToggleAction implements DumbAware {
        private SortByTypeAction() {
            super(
                IdeLocalize.actionSortByType(),
                IdeLocalize.actionSortByType(),
                PlatformIconGroup.objectbrowserSortbytype()
            );
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return isSortByType(getCurrentViewId());
        }

        @Override
        @RequiredUIAccess
        public void setSelected(AnActionEvent event, boolean flag) {
            setSortByType(getCurrentViewId(), flag);
        }

        @RequiredUIAccess
        @Override
        public void update(AnActionEvent e) {
            super.update(e);
            Presentation presentation = e.getPresentation();
            AbstractProjectViewPane pane = getCurrentProjectViewPane();
            presentation.setVisible(pane != null && pane.supportsSortByType());
        }
    }

    private class FoldersAlwaysOnTopAction extends ToggleAction implements DumbAware {
        private FoldersAlwaysOnTopAction() {
            super(LocalizeValue.localizeTODO("Folders Always on Top"));
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return isFoldersAlwaysOnTop();
        }

        @Override
        @RequiredUIAccess
        public void setSelected(AnActionEvent event, boolean flag) {
            setFoldersAlwaysOnTop(flag);
        }

        @RequiredUIAccess
        @Override
        public void update(AnActionEvent e) {
            super.update(e);
            Presentation presentation = e.getPresentation();
            AbstractProjectViewPane pane = getCurrentProjectViewPane();
            presentation.setEnabledAndVisible(pane != null && pane.supportsFoldersAlwaysOnTop());
        }
    }

    @Override
    public Collection<String> getPaneIds() {
        return Collections.unmodifiableCollection(myId2Pane.keySet());
    }

    @Override
    public void queueUpdateAll() {
        for (AbstractProjectViewPane pane : myId2Pane.values()) {
            queuePaneUpdate(pane);
        }
    }

    @Override
    @RequiredUIAccess
    public Collection<SelectInTarget> getSelectInTargets() {
        ensurePanesLoaded();
        return mySelectInTargets.values();
    }
}
