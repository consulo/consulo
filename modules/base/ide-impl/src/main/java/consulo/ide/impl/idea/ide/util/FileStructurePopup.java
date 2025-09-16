// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.ReadAction;
import consulo.application.progress.ProgressManager;
import consulo.application.ui.event.UISettingsListener;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.application.util.matcher.NameUtil;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.util.TextRange;
import consulo.document.util.TextRangeUtil;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.history.IdeDocumentHistory;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.fileEditor.structureView.StructureView;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.fileEditor.structureView.event.ModelListener;
import consulo.fileEditor.structureView.tree.*;
import consulo.ide.impl.idea.ide.actions.ViewStructureAction;
import consulo.ide.impl.idea.ide.structureView.newStructureView.StructureViewComponent;
import consulo.ide.impl.idea.ide.structureView.newStructureView.TreeActionWrapper;
import consulo.ide.impl.idea.ide.structureView.newStructureView.TreeActionsOwner;
import consulo.ide.impl.idea.ide.structureView.newStructureView.TreeModelWrapper;
import consulo.ide.impl.idea.ide.util.treeView.smartTree.SmartTreeStructure;
import consulo.ide.impl.idea.ide.util.treeView.smartTree.TreeElementWrapper;
import consulo.ide.impl.idea.ide.util.treeView.smartTree.TreeStructureUtil;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.ui.popup.AbstractPopup;
import consulo.ide.impl.idea.ui.popup.PopupUpdateProcessor;
import consulo.ide.impl.idea.ui.treeStructure.filtered.FilteringTreeStructure;
import consulo.ide.impl.idea.util.Functions;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.ide.ui.popup.HintUpdateSupply;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.refactoring.ui.CopyPasteDelegator;
import consulo.language.editor.structureView.PsiTreeElementBase;
import consulo.language.editor.structureView.StructureViewCompositeModel;
import consulo.language.localize.LanguageLocalize;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.navigation.LocationPresentation;
import consulo.navigation.Navigatable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.PlaceProvider;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.dnd.DnDAwareTree;
import consulo.ui.ex.awt.speedSearch.ElementFilter;
import consulo.ui.ex.awt.speedSearch.SpeedSearchComparator;
import consulo.ui.ex.awt.speedSearch.SpeedSearchObjectWithWeight;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.transferable.TextTransferable;
import consulo.ui.ex.awt.tree.*;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.JBIterable;
import consulo.util.concurrent.AsyncPromise;
import consulo.util.concurrent.Promise;
import consulo.util.concurrent.Promises;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * @author Konstantin Bulenkov
 */
public class FileStructurePopup implements Disposable, TreeActionsOwner {
    private static final Logger LOG = Logger.getInstance(FileStructurePopup.class);
    private static final String NARROW_DOWN_PROPERTY_KEY = "FileStructurePopup.narrowDown";

    private final Project myProject;
    private final FileEditor myFileEditor;
    private final StructureViewModel myTreeModelWrapper;
    private final StructureViewModel myTreeModel;
    private final TreeStructureActionsOwner myTreeActionsOwner;

    private JBPopup myPopup;
    private String myTitle;

    private final Tree myTree;
    private final SmartTreeStructure myTreeStructure;
    private final FilteringTreeStructure myFilteringStructure;

    private final AsyncTreeModel myAsyncTreeModel;
    private final StructureTreeModel myStructureTreeModel;
    private final TreeSpeedSearch mySpeedSearch;

    private final Object myInitialElement;
    private final Map<Class, JBCheckBox> myCheckBoxes = new HashMap<>();
    private final List<JBCheckBox> myAutoClicked = new ArrayList<>();
    private String myTestSearchFilter;
    private final List<Pair<String, JBCheckBox>> myTriggeredCheckboxes = new ArrayList<>();
    private final TreeExpander myTreeExpander;
    private final CopyPasteDelegator myCopyPasteDelegator;

    private boolean myCanClose = true;
    private boolean myDisposed;

    /**
     * @noinspection unused
     */
    @Deprecated
    @RequiredReadAction
    public FileStructurePopup(
        @Nonnull Project project,
        @Nonnull FileEditor fileEditor,
        @Nonnull StructureView structureView,
        boolean applySortAndFilter
    ) {
        this(project, fileEditor, ViewStructureAction.createStructureViewModel(project, fileEditor, structureView));
        Disposer.register(this, structureView);
    }

    public FileStructurePopup(@Nonnull Project project, @Nonnull FileEditor fileEditor, @Nonnull StructureViewModel treeModel) {
        myProject = project;
        myFileEditor = fileEditor;
        myTreeModel = treeModel;

        //Stop code analyzer to speedup EDT
        DaemonCodeAnalyzer.getInstance(myProject).disableUpdateByTimer(this);

        myTreeActionsOwner = new TreeStructureActionsOwner(myTreeModel);
        myTreeActionsOwner.setActionIncluded(Sorter.ALPHA_SORTER, true);
        myTreeModelWrapper = new TreeModelWrapper(myTreeModel, myTreeActionsOwner);
        Disposer.register(this, myTreeModelWrapper);

        myTreeStructure = new SmartTreeStructure(project, myTreeModelWrapper) {
            @Override
            public void rebuildTree() {
                if (!Application.get().isUnitTestMode() && myPopup.isDisposed()) {
                    return;
                }
                ProgressManager.getInstance().computePrioritized(() -> {
                    super.rebuildTree();
                    myFilteringStructure.rebuild();
                    return null;
                });
            }

            @Override
            public boolean isToBuildChildrenInBackground(@Nonnull Object element) {
                return getRootElement() == element;
            }

            @Nonnull
            @Override
            protected TreeElementWrapper createTree() {
                return StructureViewComponent.createWrapper(myProject, myModel.getRoot(), myModel);
            }

            @Override
            public String toString() {
                return "structure view tree structure(model=" + myTreeModelWrapper + ")";
            }
        };

        FileStructurePopupFilter filter = new FileStructurePopupFilter();
        myFilteringStructure = new FilteringTreeStructure(filter, myTreeStructure, false);

        myStructureTreeModel = new StructureTreeModel<>(myFilteringStructure, this);
        myAsyncTreeModel = new AsyncTreeModel(myStructureTreeModel, this);
        myAsyncTreeModel.setRootImmediately(myStructureTreeModel.getRootImmediately());
        myTree = new MyTree(myAsyncTreeModel);
        StructureViewComponent.registerAutoExpandListener(myTree, myTreeModel);

        ModelListener modelListener = () -> rebuild(false);
        myTreeModel.addModelListener(modelListener);
        Disposer.register(this, () -> myTreeModel.removeModelListener(modelListener));
        myTree.setCellRenderer(new NodeRenderer());
        myProject.getMessageBus().connect(this).subscribe(UISettingsListener.class, o -> rebuild(false));

        myTree.setTransferHandler(new TransferHandler() {
            @Override
            public boolean importData(@Nonnull TransferSupport support) {
                String s = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);
                if (s != null && !mySpeedSearch.isPopupActive()) {
                    mySpeedSearch.showPopup(s);
                    return true;
                }
                return false;
            }

            @Nullable
            @Override
            protected Transferable createTransferable(JComponent component) {
                JBIterable<Pair<FilteringTreeStructure.FilteringNode, PsiElement>> pairs = JBIterable.of(myTree.getSelectionPaths())
                    .filterMap(TreeUtil::getLastUserObject)
                    .filter(FilteringTreeStructure.FilteringNode.class)
                    .filterMap(o -> o.getDelegate() instanceof PsiElement element ? Pair.create(o, element) : null)
                    .collect();
                if (pairs.isEmpty()) {
                    return null;
                }
                Set<PsiElement> psiSelection = pairs.map(Functions.pairSecond()).toSet();

                String text = StringUtil.join(
                    pairs,
                    pair -> {
                        PsiElement psi = pair.second;
                        String defaultPresentation = pair.first.getPresentation().getPresentableText();
                        if (psi == null) {
                            return defaultPresentation;
                        }
                        for (PsiElement p = psi.getParent(); p != null; p = p.getParent()) {
                            if (psiSelection.contains(p)) {
                                return null;
                            }
                        }
                        return ObjectUtil.chooseNotNull(psi.getText(), defaultPresentation);
                    },
                    "\n"
                );

                String htmlText = "<body>\n" + text + "\n</body>";
                return new TextTransferable(XmlStringUtil.wrapInHtml(htmlText), text);
            }

            @Override
            public int getSourceActions(JComponent component) {
                return COPY;
            }
        });

        mySpeedSearch = new MyTreeSpeedSearch();
        mySpeedSearch.setComparator(new SpeedSearchComparator(false, true) {
            @Nonnull
            @Override
            protected MinusculeMatcher createMatcher(@Nonnull String pattern) {
                return NameUtil.buildMatcher(pattern).withSeparators(" ()").build();
            }
        });

        myTreeExpander = new DefaultTreeExpander(myTree);
        myCopyPasteDelegator = new CopyPasteDelegator(myProject, myTree);

        myInitialElement = myTreeModel.getCurrentEditorElement();
        TreeUtil.installActions(myTree);
    }

    public void show() {
        JComponent panel = createCenterPanel();
        MnemonicHelper.init(panel);
        myTree.addTreeSelectionListener(__ -> {
            if (myPopup.isVisible()) {
                PopupUpdateProcessor updateProcessor = myPopup.getUserData(PopupUpdateProcessor.class);
                if (updateProcessor != null) {
                    AbstractTreeNode node = getSelectedNode();
                    updateProcessor.updatePopup(node);
                }
            }
        });

        myPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, myTree)
            .setTitle(myTitle)
            .setResizable(true)
            .setModalContext(false)
            .setFocusable(true)
            .setRequestFocus(true)
            .setMovable(true)
            .setBelongsToGlobalPopupStack(true)
            //.setCancelOnClickOutside(false) //for debug and snapshots
            .setCancelOnOtherWindowOpen(true)
            .setCancelKeyEnabled(false)
            .setDimensionServiceKey(null, getDimensionServiceKey(), true)
            .setCancelCallback(() -> myCanClose)
            .setNormalWindowLevel(true)
            .createPopup();

        Disposer.register(myPopup, this);

        myTree.getEmptyText().setText("Loading...");
        myPopup.showCenteredInCurrentWindow(myProject);

        ((AbstractPopup) myPopup).setShowHints(true);

        ProjectIdeFocusManager.getInstance(myProject).requestFocus(myTree, true);

        rebuildAndSelect(false, myInitialElement).onProcessed(path -> UIUtil.invokeLaterIfNeeded(() -> {
            TreeUtil.ensureSelection(myTree);
            installUpdater();
        }));
    }

    private void installUpdater() {
        if (Application.get().isUnitTestMode() || myPopup.isDisposed()) {
            return;
        }
        Alarm alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, myPopup);
        alarm.addRequest(new Runnable() {
            String filter = "";

            @Override
            public void run() {
                alarm.cancelAllRequests();
                String prefix = mySpeedSearch.getEnteredPrefix();
                myTree.getEmptyText().setText(StringUtil.isEmpty(prefix) ? "Structure is empty" : "'" + prefix + "' not found");
                if (prefix == null) {
                    prefix = "";
                }

                if (!filter.equals(prefix)) {
                    boolean isBackspace = prefix.length() < filter.length();
                    filter = prefix;
                    rebuild(true).onProcessed(ignore -> UIUtil.invokeLaterIfNeeded(() -> {
                        if (isDisposed()) {
                            return;
                        }
                        TreeUtil.promiseExpandAll(myTree);
                        if (isBackspace && handleBackspace(filter)) {
                            return;
                        }
                        if (myFilteringStructure.getRootElement().getChildren().length == 0) {
                            for (JBCheckBox box : myCheckBoxes.values()) {
                                if (!box.isSelected()) {
                                    myAutoClicked.add(box);
                                    myTriggeredCheckboxes.add(0, Pair.create(filter, box));
                                    box.doClick();
                                    filter = "";
                                    break;
                                }
                            }
                        }
                    }));
                }
                if (!alarm.isDisposed()) {
                    alarm.addRequest(this, 300);
                }
            }
        }, 300);
    }

    private boolean handleBackspace(String filter) {
        boolean clicked = false;
        Iterator<Pair<String, JBCheckBox>> iterator = myTriggeredCheckboxes.iterator();
        while (iterator.hasNext()) {
            Pair<String, JBCheckBox> next = iterator.next();
            if (next.getFirst().length() < filter.length()) {
                break;
            }

            iterator.remove();
            next.getSecond().doClick();
            clicked = true;
        }
        return clicked;
    }

    @Nonnull
    public Promise<TreePath> select(Object element) {
        int[] stage = {1, 0}; // 1 - first pass, 2 - optimization applied, 3 - retry w/o optimization
        TreePath[] deepestPath = {null};
        TreeVisitor visitor = path -> {
            Object last = path.getLastPathComponent();
            Object userObject = StructureViewComponent.unwrapNavigatable(last);
            Object value = StructureViewComponent.unwrapValue(last);
            if (Comparing.equal(value, element)
                || userObject instanceof AbstractTreeNode abstractTreeNode && abstractTreeNode.canRepresent(element)) {
                return TreeVisitor.Action.INTERRUPT;
            }
            if (value instanceof PsiElement valueElement && element instanceof PsiElement elementElement) {
                if (PsiTreeUtil.isAncestor(valueElement, elementElement, true)) {
                    int count = path.getPathCount();
                    if (stage[1] == 0 || stage[1] < count) {
                        stage[1] = count;
                        deepestPath[0] = path;
                    }
                }
                else if (stage[0] != 3) {
                    stage[0] = 2;
                    return TreeVisitor.Action.SKIP_CHILDREN;
                }
            }
            return TreeVisitor.Action.CONTINUE;
        };
        Function<TreePath, Promise<TreePath>> action = path -> {
            myTree.expandPath(path);
            TreeUtil.selectPath(myTree, path);
            TreeUtil.ensureSelection(myTree);
            return Promises.resolvedPromise(path);
        };
        Function<TreePath, Promise<TreePath>> fallback = new Function<>() {
            @Override
            @RequiredReadAction
            public Promise<TreePath> apply(TreePath path) {
                if (path == null && stage[0] == 2) {
                    // Some structure views merge unrelated psi elements into a structure node (MarkdownStructureViewModel).
                    // So turn off the isAncestor() optimization and retry once.
                    stage[0] = 3;
                    return myAsyncTreeModel.accept(visitor).thenAsync(this);
                }
                else {
                    TreePath adjusted = path == null ? deepestPath[0] : path;
                    if (path == null && adjusted != null && element instanceof PsiElement psiElement) {
                        Object minChild = findClosestPsiElement(psiElement, adjusted, myAsyncTreeModel);
                        if (minChild != null) {
                            adjusted = adjusted.pathByAddingChild(minChild);
                        }
                    }
                    return adjusted == null ? Promises.rejectedPromise() : action.apply(adjusted);
                }
            }
        };

        return myAsyncTreeModel.accept(visitor).thenAsync(fallback);
    }

    @TestOnly
    @RequiredUIAccess
    public AsyncPromise<Void> rebuildAndUpdate() {
        AsyncPromise<Void> result = new AsyncPromise<>();
        @RequiredUIAccess
        TreeVisitor visitor = path -> {
            AbstractTreeNode node = TreeUtil.getLastUserObject(AbstractTreeNode.class, path);
            if (node != null) {
                node.update();
            }
            return TreeVisitor.Action.CONTINUE;
        };
        rebuild(false).onProcessed(ignore1 -> myAsyncTreeModel.accept(visitor).onProcessed(ignore2 -> result.setResult(null)));
        return result;
    }

    public boolean isDisposed() {
        return myDisposed;
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }

    private static boolean isShouldNarrowDown() {
        return ApplicationPropertiesComponent.getInstance().getBoolean(NARROW_DOWN_PROPERTY_KEY, true);
    }

    protected static String getDimensionServiceKey() {
        return "StructurePopup";
    }

    @Nullable
    public PsiElement getCurrentElement(@Nullable PsiFile psiFile) {
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();

        if (myTreeModelWrapper.getCurrentEditorElement() instanceof PsiElement elementAtCursor) {
            return elementAtCursor;
        }

        if (psiFile != null && myFileEditor instanceof TextEditor textEditor) {
            return psiFile.getViewProvider().findElementAt(textEditor.getEditor().getCaretModel().getOffset());
        }

        return null;
    }

    public JComponent createCenterPanel() {
        List<FileStructureFilter> fileStructureFilters = new ArrayList<>();
        List<FileStructureNodeProvider> fileStructureNodeProviders = new ArrayList<>();
        if (myTreeActionsOwner != null) {
            for (Filter filter : myTreeModel.getFilters()) {
                if (filter instanceof FileStructureFilter fsFilter) {
                    myTreeActionsOwner.setActionIncluded(fsFilter, true);
                    fileStructureFilters.add(fsFilter);
                }
            }

            if (myTreeModel instanceof ProvidingTreeModel providingTreeModel) {
                for (NodeProvider provider : providingTreeModel.getNodeProviders()) {
                    if (provider instanceof FileStructureNodeProvider fileStructureNodeProvider) {
                        fileStructureNodeProviders.add(fileStructureNodeProvider);
                    }
                }
            }
        }

        int checkBoxCount = fileStructureNodeProviders.size() + fileStructureFilters.size();
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(JBUI.size(540, 500));
        JPanel chkPanel = new JPanel(new GridLayout(
            0,
            checkBoxCount > 0 && checkBoxCount % 4 == 0 ? checkBoxCount / 2 : 3,
            JBUI.scale(UIUtil.DEFAULT_HGAP),
            0
        ));
        chkPanel.setOpaque(false);

        Shortcut[] F4 = ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_SOURCE).getShortcutSet().getShortcuts();
        Shortcut[] ENTER = CustomShortcutSet.fromString("ENTER").getShortcuts();
        CustomShortcutSet shortcutSet = new CustomShortcutSet(ArrayUtil.mergeArrays(F4, ENTER));
        new DumbAwareAction() {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                boolean succeeded = navigateSelectedElement();
                if (succeeded) {
                    unregisterCustomShortcutSet(panel);
                }
            }
        }.registerCustomShortcutSet(shortcutSet, panel);

        DumbAwareAction.create(e -> {
            if (mySpeedSearch != null && mySpeedSearch.isPopupActive()) {
                mySpeedSearch.hidePopup();
            }
            else {
                myPopup.cancel();
            }
        }).registerCustomShortcutSet(CustomShortcutSet.fromString("ESCAPE"), myTree);
        new ClickListener() {
            @Override
            @RequiredUIAccess
            public boolean onClick(@Nonnull MouseEvent e, int clickCount) {
                TreePath path = myTree.getClosestPathForLocation(e.getX(), e.getY());
                Rectangle bounds = path == null ? null : myTree.getPathBounds(path);
                if (bounds == null || bounds.x > e.getX() || bounds.y > e.getY() || bounds.y + bounds.height < e.getY()) {
                    return false;
                }
                navigateSelectedElement();
                return true;
            }
        }.installOn(myTree);

        for (FileStructureFilter filter : fileStructureFilters) {
            addCheckbox(chkPanel, filter);
        }

        for (FileStructureNodeProvider provider : fileStructureNodeProviders) {
            addCheckbox(chkPanel, provider);
        }
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(chkPanel, BorderLayout.WEST);

        topPanel.add(TargetAWT.to(createSettingsButton()), BorderLayout.EAST);

        topPanel.setBackground(JBCurrentTheme.Popup.toolbarPanelColor());
        Dimension prefSize = topPanel.getPreferredSize();
        prefSize.height = JBCurrentTheme.Popup.toolbarHeight();
        topPanel.setPreferredSize(prefSize);
        topPanel.setBorder(JBUI.Borders.emptyLeft(UIUtil.DEFAULT_HGAP));

        panel.add(topPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTree);
        scrollPane.setBorder(IdeBorderFactory.createBorder(JBCurrentTheme.Popup.toolbarBorderColor(), SideBorder.TOP | SideBorder.BOTTOM));
        panel.add(scrollPane, BorderLayout.CENTER);
        DataManager.registerDataProvider(panel, dataId -> {
            if (Project.KEY == dataId) {
                return myProject;
            }
            if (FileEditor.KEY == dataId) {
                return myFileEditor;
            }
            if (OpenFileDescriptorImpl.NAVIGATE_IN_EDITOR == dataId && myFileEditor instanceof TextEditor textEditor) {
                return textEditor.getEditor();
            }
            if (PsiElement.KEY == dataId) {
                return getSelectedElements().filter(PsiElement.class).first();
            }
            if (PsiElement.KEY_OF_ARRAY == dataId) {
                return PsiUtilCore.toPsiElementArray(getSelectedElements().filter(PsiElement.class).toList());
            }
            if (Navigatable.KEY == dataId) {
                return getSelectedElements().filter(Navigatable.class).first();
            }
            if (Navigatable.KEY_OF_ARRAY == dataId) {
                List<Navigatable> result = getSelectedElements().filter(Navigatable.class).toList();
                return result.isEmpty() ? null : result.toArray(new Navigatable[0]);
            }
            if (LangDataKeys.POSITION_ADJUSTER_POPUP == dataId) {
                return myPopup;
            }
            if (CopyProvider.KEY == dataId) {
                return myCopyPasteDelegator.getCopyProvider();
            }
            if (PlatformDataKeys.TREE_EXPANDER == dataId) {
                return myTreeExpander;
            }
            return null;
        });

        panel.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                myPopup.cancel();
            }
        });

        return panel;
    }

    @Nonnull
    private JBIterable<Object> getSelectedElements() {
        return JBIterable.of(myTree.getSelectionPaths()).filterMap(o -> StructureViewComponent.unwrapValue(o.getLastPathComponent()));
    }

    @Nonnull
    @RequiredUIAccess
    private Button createSettingsButton() {
        Button settingsButton = Button.create(LocalizeValue.of());
        settingsButton.setIcon(PlatformIconGroup.generalGearplain());
        settingsButton.addStyle(ButtonStyle.TOOLBAR);
        settingsButton.addClickListener(event -> {
            List<AnAction> sorters = createSorters();
            ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
            if (!sorters.isEmpty()) {
                group.addAll(sorters);
                group.addSeparator();
            }

            group.add(new ToggleAction(IdeLocalize.checkboxNarrowDownOnTyping()) {
                @Override
                public boolean isSelected(@Nonnull AnActionEvent e) {
                    return isShouldNarrowDown();
                }

                @Override
                @RequiredUIAccess
                public void setSelected(@Nonnull AnActionEvent e, boolean state) {
                    ApplicationPropertiesComponent.getInstance().setValue(NARROW_DOWN_PROPERTY_KEY, Boolean.toString(state));
                    if (mySpeedSearch.isPopupActive() && !StringUtil.isEmpty(mySpeedSearch.getEnteredPrefix())) {
                        rebuild(true);
                    }
                }
            });

            DataManager dataManager = DataManager.getInstance();
            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                null,
                group.build(),
                dataManager.getDataContext(settingsButton),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
            );
            popup.addListener(new JBPopupListener() {
                @Override
                public void onClosed(@Nonnull LightweightWindowEvent event) {
                    myCanClose = true;
                }
            });
            myCanClose = false;

            popup.showUnderneathOf(TargetAWT.to(settingsButton));
        });
        return settingsButton;
    }

    private List<AnAction> createSorters() {
        List<AnAction> actions = new ArrayList<>();
        for (Sorter sorter : myTreeModel.getSorters()) {
            if (sorter.isVisible()) {
                actions.add(new MyTreeActionWrapper(sorter));
            }
        }
        return actions;
    }

    @Nullable
    @RequiredReadAction
    private static Object findClosestPsiElement(@Nonnull PsiElement element, @Nonnull TreePath adjusted, @Nonnull TreeModel treeModel) {
        TextRange range = element.getTextRange();
        if (range.isEmpty()) {
            return null;
        }
        Object parent = adjusted.getLastPathComponent();
        int minDistance = 0;
        Object minChild = null;
        for (int i = 0, count = treeModel.getChildCount(parent); i < count; i++) {
            Object child = treeModel.getChild(parent, i);
            Object value = StructureViewComponent.unwrapValue(child);
            if (value instanceof StubBasedPsiElement stubBasedPsiElement && stubBasedPsiElement.getStub() != null) {
                continue;
            }
            TextRange r = value instanceof PsiElement psiElement ? psiElement.getTextRange() : null;
            if (r == null) {
                continue;
            }
            int distance = TextRangeUtil.getDistance(range, r);
            if (minChild == null || distance < minDistance) {
                minDistance = distance;
                minChild = child;
            }
        }
        return minChild;
    }

    private class MyTreeActionWrapper extends TreeActionWrapper {
        private final TreeAction myAction;

        MyTreeActionWrapper(TreeAction action) {
            super(action, myTreeActionsOwner);
            myAction = action;
            myTreeActionsOwner.setActionIncluded(action, getDefaultValue(action));
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            super.update(e);
            e.getPresentation().setIcon(null);
        }

        @Override
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            boolean actionState = TreeModelWrapper.shouldRevert(myAction) != state;
            myTreeActionsOwner.setActionIncluded(myAction, actionState);
            saveState(myAction, state);
            rebuild(false).onProcessed(ignore -> {
                if (mySpeedSearch.isPopupActive()) {
                    mySpeedSearch.refreshSelection();
                }
            });
        }
    }

    @Nullable
    private AbstractTreeNode getSelectedNode() {
        TreePath path = myTree.getSelectionPath();
        Object o = StructureViewComponent.unwrapNavigatable(path == null ? null : path.getLastPathComponent());
        return o instanceof AbstractTreeNode treeNode ? treeNode : null;
    }

    @RequiredUIAccess
    private boolean navigateSelectedElement() {
        AbstractTreeNode selectedNode = getSelectedNode();
        if (Application.get().isInternal()) {
            String enteredPrefix = mySpeedSearch.getEnteredPrefix();
            String itemText = getSpeedSearchText(selectedNode);
            if (StringUtil.isNotEmpty(enteredPrefix) && StringUtil.isNotEmpty(itemText)) {
                LOG.info("Chosen in file structure popup by prefix '" + enteredPrefix + "': '" + itemText + "'");
            }
        }

        return CommandProcessor.getInstance().<Boolean>newCommand()
            .project(myProject)
            .name(LanguageLocalize.commandNameNavigate())
            .compute(() -> {
                IdeDocumentHistory.getInstance(myProject).includeCurrentCommandAsNavigation();

                if (selectedNode == null) {
                    return false;
                }
                else if (selectedNode.canNavigateToSource()) {
                    selectedNode.navigate(true);
                    myPopup.cancel();
                    return true;
                }
                else {
                    return false;
                }
            });
    }

    private void addCheckbox(JPanel panel, TreeAction action) {
        String text = action instanceof FileStructureFilter fileStructureFilter
            ? fileStructureFilter.getCheckBoxText()
            : action instanceof FileStructureNodeProvider fileStructureNodeProvider
            ? fileStructureNodeProvider.getCheckBoxText()
            : null;

        if (text == null) {
            return;
        }

        Shortcut[] shortcuts = extractShortcutFor(action);


        JBCheckBox checkBox = new JBCheckBox();
        checkBox.setOpaque(false);
        UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, checkBox);

        boolean selected = getDefaultValue(action);
        checkBox.setSelected(selected);
        boolean isRevertedStructureFilter = action instanceof FileStructureFilter fileStructureFilter && fileStructureFilter.isReverted();
        myTreeActionsOwner.setActionIncluded(action, isRevertedStructureFilter != selected);
        checkBox.addActionListener(__ -> {
            boolean state = checkBox.isSelected();
            if (!myAutoClicked.contains(checkBox)) {
                saveState(action, state);
            }
            myTreeActionsOwner.setActionIncluded(action, isRevertedStructureFilter != state);
            rebuild(false).onProcessed(ignore -> {
                if (mySpeedSearch.isPopupActive()) {
                    mySpeedSearch.refreshSelection();
                }
            });
        });
        checkBox.setFocusable(false);

        if (shortcuts.length > 0) {
            text += " (" + KeymapUtil.getShortcutText(shortcuts[0]) + ")";
            DumbAwareAction.create(e -> checkBox.doClick()).registerCustomShortcutSet(new CustomShortcutSet(shortcuts), myTree);
        }
        checkBox.setText(StringUtil.capitalize(StringUtil.trimStart(text.trim(), "Show ")));
        panel.add(checkBox);

        myCheckBoxes.put(action.getClass(), checkBox);
    }

    @Nonnull
    private Promise<Void> rebuild(boolean refilterOnly) {
        Object selection =
            JBIterable.of(myTree.getSelectionPaths()).filterMap(o -> StructureViewComponent.unwrapValue(o.getLastPathComponent())).first();
        return rebuildAndSelect(refilterOnly, selection).then(o -> null);
    }

    @Nonnull
    private Promise<TreePath> rebuildAndSelect(boolean refilterOnly, Object selection) {
        AsyncPromise<TreePath> result = new AsyncPromise<>();
        myStructureTreeModel.getInvoker().runOrInvokeLater(() -> {
            if (refilterOnly) {
                myFilteringStructure.refilter();
                myStructureTreeModel.invalidate().onSuccess(
                    res -> (selection == null ? myAsyncTreeModel.accept(o -> TreeVisitor.Action.CONTINUE) : select(selection))
                        .onError(ignore2 -> result.setError("rejected"))
                        .onSuccess(p -> UIUtil.invokeLaterIfNeeded(() -> {
                            TreeUtil.expand(getTree(), myTreeModel instanceof StructureViewCompositeModel ? 3 : 2);
                            TreeUtil.ensureSelection(myTree);
                            mySpeedSearch.refreshSelection();
                            result.setResult(p);
                        }))
                );
            }
            else {
                myTreeStructure.rebuildTree();
                myStructureTreeModel.invalidate().onSuccess(res -> rebuildAndSelect(true, selection).processed(result));
            }
        });
        return result;
    }

    @Nonnull
    static Shortcut[] extractShortcutFor(@Nonnull TreeAction action) {
        if (action instanceof ActionShortcutProvider actionShortcutProvider) {
            String actionId = actionShortcutProvider.getActionIdForShortcut();
            return KeymapUtil.getActiveKeymapShortcuts(actionId).getShortcuts();
        }
        return action instanceof FileStructureFilter fileStructureFilter
            ? fileStructureFilter.getShortcut()
            : ((FileStructureNodeProvider) action).getShortcut();
    }

    private static boolean getDefaultValue(TreeAction action) {
        String propertyName = action.getSerializePropertyName();
        return ApplicationPropertiesComponent.getInstance()
            .getBoolean(TreeStructureUtil.getPropertyName(propertyName), Sorter.ALPHA_SORTER.equals(action));
    }

    private static void saveState(TreeAction action, boolean state) {
        String propertyName = action.getSerializePropertyName();
        ApplicationPropertiesComponent.getInstance().setValue(TreeStructureUtil.getPropertyName(propertyName), state);
    }

    public void setTitle(String title) {
        myTitle = title;
    }

    @Nonnull
    public Tree getTree() {
        return myTree;
    }

    @TestOnly
    public TreeSpeedSearch getSpeedSearch() {
        return mySpeedSearch;
    }

    @TestOnly
    public void setSearchFilterForTests(String filter) {
        myTestSearchFilter = filter;
    }

    public void setTreeActionState(Class<? extends TreeAction> action, boolean state) {
        JBCheckBox checkBox = myCheckBoxes.get(action);
        if (checkBox != null) {
            checkBox.setSelected(state);
            for (ActionListener listener : checkBox.getActionListeners()) {
                listener.actionPerformed(new ActionEvent(this, 1, ""));
            }
        }
    }

    @Nullable
    public static String getSpeedSearchText(Object object) {
        String text = String.valueOf(object);
        Object value = StructureViewComponent.unwrapWrapper(object);
        if (text != null) {
            if (value instanceof PsiTreeElementBase psiTreeElementBase && psiTreeElementBase.isSearchInLocationString()) {
                String locationString = psiTreeElementBase.getLocationString();
                if (!StringUtil.isEmpty(locationString)) {
                    String locationPrefix = null;
                    String locationSuffix = null;
                    if (value instanceof LocationPresentation locationPresentation) {
                        locationPrefix = locationPresentation.getLocationPrefix();
                        locationSuffix = locationPresentation.getLocationSuffix();
                    }

                    return text +
                        StringUtil.notNullize(locationPrefix, LocationPresentation.DEFAULT_LOCATION_PREFIX) +
                        locationString +
                        StringUtil.notNullize(locationSuffix, LocationPresentation.DEFAULT_LOCATION_SUFFIX);
                }
            }
            return text;
        }
        // NB!: this point is achievable if the following method returns null
        // see consulo.ide.impl.idea.ide.util.treeView.NodeDescriptor.toString
        if (value instanceof TreeElement treeElement) {
            return ReadAction.compute(() -> treeElement.getPresentation().getPresentableText());
        }

        return null;
    }

    @Override
    public void setActionActive(String name, boolean state) {

    }

    @Override
    public boolean isActionActive(String name) {
        return false;
    }

    private class FileStructurePopupFilter implements ElementFilter {
        private String myLastFilter;
        private final Set<Object> myVisibleParents = new HashSet<>();
        private final boolean isUnitTest = Application.get().isUnitTestMode();

        @Override
        public boolean shouldBeShowing(Object value) {
            if (!isShouldNarrowDown()) {
                return true;
            }

            String filter = getSearchPrefix();
            if (!StringUtil.equals(myLastFilter, filter)) {
                myVisibleParents.clear();
                myLastFilter = filter;
            }
            if (filter != null) {
                if (myVisibleParents.contains(value)) {
                    return true;
                }

                String text = getSpeedSearchText(value);
                if (text == null) {
                    return false;
                }

                if (matches(filter, text)) {
                    Object o = value;
                    while (o instanceof FilteringTreeStructure.FilteringNode filteringNode && (o = filteringNode.getParent()) != null) {
                        myVisibleParents.add(filteringNode.getParent());
                    }
                    return true;
                }
                else {
                    return false;
                }
            }
            return true;
        }

        private boolean matches(@Nonnull String filter, @Nonnull String text) {
            return (isUnitTest || mySpeedSearch.isPopupActive())
                && StringUtil.isNotEmpty(filter)
                && mySpeedSearch.getComparator().matchingFragments(filter, text) != null;
        }
    }

    @Nullable
    private String getSearchPrefix() {
        if (Application.get().isUnitTestMode()) {
            return myTestSearchFilter;
        }

        return mySpeedSearch != null && !StringUtil.isEmpty(mySpeedSearch.getEnteredPrefix()) ? mySpeedSearch.getEnteredPrefix() : null;
    }

    private class MyTreeSpeedSearch extends TreeSpeedSearch {

        MyTreeSpeedSearch() {
            super(myTree, path -> getSpeedSearchText(TreeUtil.getLastUserObject(path)), true);
        }

        @Override
        protected Point getComponentLocationOnScreen() {
            return myPopup.getContent().getLocationOnScreen();
        }

        @Override
        protected Rectangle getComponentVisibleRect() {
            return myPopup.getContent().getVisibleRect();
        }

        @Override
        public Object findElement(String s) {
            List<SpeedSearchObjectWithWeight> elements = SpeedSearchObjectWithWeight.findElement(s, this);
            SpeedSearchObjectWithWeight best = ContainerUtil.getFirstItem(elements);
            if (best == null) {
                return null;
            }
            if (myInitialElement instanceof PsiElement initial) {
                // find children of the initial element
                SpeedSearchObjectWithWeight bestForParent = find(initial, elements, FileStructurePopup::isParent);
                if (bestForParent != null) {
                    return bestForParent.node;
                }
                // find siblings of the initial element
                PsiElement parent = initial.getParent();
                if (parent != null) {
                    SpeedSearchObjectWithWeight bestSibling = find(parent, elements, FileStructurePopup::isParent);
                    if (bestSibling != null) {
                        return bestSibling.node;
                    }
                }
                // find grand children of the initial element
                SpeedSearchObjectWithWeight bestForAncestor = find(initial, elements, FileStructurePopup::isAncestor);
                if (bestForAncestor != null) {
                    return bestForAncestor.node;
                }
            }
            return best.node;
        }
    }

    @Nullable
    private static SpeedSearchObjectWithWeight find(
        @Nonnull PsiElement element,
        @Nonnull List<SpeedSearchObjectWithWeight> objects,
        @Nonnull BiPredicate<PsiElement, TreePath> predicate
    ) {
        return ContainerUtil.find(objects, object -> predicate.test(element, ObjectUtil.tryCast(object.node, TreePath.class)));
    }

    private static boolean isElement(@Nonnull PsiElement element, @Nullable TreePath path) {
        return element.equals(StructureViewComponent.unwrapValue(TreeUtil.getLastUserObject(
            FilteringTreeStructure.FilteringNode.class,
            path
        )));
    }

    private static boolean isParent(@Nonnull PsiElement parent, @Nullable TreePath path) {
        return path != null && isElement(parent, path.getParentPath());
    }

    private static boolean isAncestor(@Nonnull PsiElement ancestor, @Nullable TreePath path) {
        while (path != null) {
            if (isElement(ancestor, path)) {
                return true;
            }
            path = path.getParentPath();
        }
        return false;
    }

    static class MyTree extends DnDAwareTree implements PlaceProvider<String> {
        MyTree(TreeModel treeModel) {
            super(treeModel);
            setRootVisible(false);
            setShowsRootHandles(true);

            HintUpdateSupply.installHintUpdateSupply(
                this,
                o -> StructureViewComponent.unwrapValue(o) instanceof PsiElement element ? element : null
            );
        }

        @Override
        public String getPlace() {
            return ActionPlaces.STRUCTURE_VIEW_POPUP;
        }
    }
}
