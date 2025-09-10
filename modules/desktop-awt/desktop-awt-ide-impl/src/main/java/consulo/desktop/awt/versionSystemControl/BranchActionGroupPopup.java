// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.versionSystemControl;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.openapi.actionSystem.impl.SimpleDataContext;
import consulo.ide.impl.idea.openapi.vcs.ui.PopupListElementRendererWithIcon;
import consulo.ide.impl.idea.ui.popup.WizardPopup;
import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.ProjectWindowStateService;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.Size2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.popup.ListPopupModel;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.ex.popup.event.JBPopupAdapter;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.distributed.DvcsBundle;
import consulo.versionControlSystem.distributed.branch.BranchActionGroup;
import consulo.versionControlSystem.distributed.branch.PopupElementWithAdditionalInfo;
import consulo.versionControlSystem.distributed.icon.DistributedVersionControlIconGroup;
import consulo.versionControlSystem.distributed.internal.BranchHideableActionGroup;
import consulo.versionControlSystem.distributed.internal.BranchListPopup;
import consulo.versionControlSystem.distributed.internal.BranchMoreAction;
import consulo.versionControlSystem.distributed.internal.BranchMoreHideableActionGroup;
import consulo.versionControlSystem.internal.FlatSpeedSearchPopupFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static consulo.ui.ex.awt.UIUtil.DEFAULT_HGAP;
import static consulo.ui.ex.awt.UIUtil.DEFAULT_VGAP;

public class BranchActionGroupPopup extends FlatSpeedSearchPopup implements BranchListPopup {
    static final String BRANCH_POPUP = "BranchWidget";
    private Project myProject;
    private MyPopupListElementRenderer myListElementRenderer;
    private boolean myShown;
    private boolean myUserSizeChanged;
    private boolean myInternalSizeChanged;
    private int myMeanRowHeight;
    @Nullable
    private final String myKey;
    @Nonnull
    private Dimension myPrevSize = JBUI.emptySize();

    private final List<AnAction> mySettingsActions = new ArrayList<>();
    private final List<AnAction> myToolbarActions = new ArrayList<>();

    public BranchActionGroupPopup(
        @Nonnull String title,
        @Nonnull Project project,
        @Nonnull Predicate<AnAction> preselectActionCondition,
        @Nonnull ActionGroup actions,
        @Nullable String dimensionKey
    ) {
        super(
            title,
            createBranchSpeedSearchActionGroup(actions),
            SimpleDataContext.builder()
                .add(Project.KEY, project)
                .add(UIExAWTDataKey.CONTEXT_COMPONENT, ProjectIdeFocusManager.getInstance(project).getFocusOwner())
                .build(),
            preselectActionCondition,
            true
        );
        myProject = project;
        DataManager.registerDataProvider(getList(), dataId -> BranchListPopup.POPUP_MODEL == dataId ? getListModel() : null);
        myKey = dimensionKey;
        if (myKey != null) {
            Size2D storedSize = ProjectWindowStateService.getInstance(myProject).getSizeFor(myProject, myKey);
            if (storedSize != null) {
                //set forced size before component is shown
                setSize(new Dimension(storedSize.width(), storedSize.height()));
                myUserSizeChanged = true;
            }
            createTitlePanelToolbar(myKey);
        }
        setSpeedSearchAlwaysShown();
        myMeanRowHeight = getList().getCellBounds(0, 0).height + UIUtil.getListCellVPadding() * 2;
    }

    private void createTitlePanelToolbar(@Nonnull String dimensionKey) {
        ActionGroup actionGroup = new ActionGroup() {
            @Override
            @Nonnull
            public AnAction[] getChildren(@Nullable AnActionEvent e) {
                return myToolbarActions.toArray(AnAction.EMPTY_ARRAY);
            }
        };
        AnAction restoreSizeButton = new AnAction(
            DvcsBundle.message("action.BranchActionGroupPopup.Anonymous.text.restore.size"),
            null,
            PlatformIconGroup.generalCollapsecomponent()
        ) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                ProjectWindowStateService.getInstance(myProject).putSizeFor(myProject, dimensionKey, null);
                myInternalSizeChanged = true;
                pack(true, true);
            }

            @Override
            public void update(@Nonnull AnActionEvent e) {
                e.getPresentation().setEnabled(myUserSizeChanged);
            }
        };
        ActionGroup settingsGroup = new ActionGroup(DvcsBundle.message("action.BranchActionGroupPopup.settings.text"), true) {
            @Override
            @Nonnull
            public AnAction[] getChildren(@Nullable AnActionEvent e) {
                return mySettingsActions.toArray(AnAction.EMPTY_ARRAY);
            }

            @Override
            public boolean hideIfNoVisibleChildren() {
                return true;
            }
        };
        settingsGroup.getTemplatePresentation().setIcon(PlatformIconGroup.generalGearplain());

        myToolbarActions.add(restoreSizeButton);
        myToolbarActions.add(settingsGroup);

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(BRANCH_POPUP, actionGroup, true);
        toolbar.setMiniMode(true);
        toolbar.setTargetComponent(null);
        toolbar.setReservePlaceAutoPopupIcon(false);

        getTitle().setRightComponent(toolbar.getComponent());
    }

    //for child popups only
    private BranchActionGroupPopup(@Nullable WizardPopup aParent, @Nonnull ListPopupStep aStep, @Nullable Object parentValue) {
        super(aParent, aStep, DataContext.EMPTY_CONTEXT, parentValue);
        // don't store children popup userSize;
        myKey = null;
        DataManager.registerDataProvider(getList(), dataId -> BranchListPopup.POPUP_MODEL == dataId ? getListModel() : null);
    }

    private void trackDimensions(@Nullable String dimensionKey) {
        Window popupWindow = getPopupWindow();
        if (popupWindow == null) {
            return;
        }
        ComponentListener windowListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (myShown) {
                    processOnSizeChanged();
                }
            }
        };
        popupWindow.addComponentListener(windowListener);
        addListener(new JBPopupAdapter() {
            @Override
            public void onClosed(@Nonnull LightweightWindowEvent event) {
                popupWindow.removeComponentListener(windowListener);
                if (dimensionKey != null && myUserSizeChanged) {
                    ProjectWindowStateService.getInstance(myProject)
                        .putSizeFor(myProject, dimensionKey, new Size2D(myPrevSize.width, myPrevSize.height));
                }
            }
        });
    }

    private void processOnSizeChanged() {
        Dimension newSize = Objects.requireNonNull(getSize());
        int preferredHeight = getComponent().getPreferredSize().height;
        int realHeight = getComponent().getHeight();
        boolean shouldExpand = preferredHeight + myMeanRowHeight < realHeight;
        boolean sizeWasIncreased = myPrevSize.height < newSize.height;
        if (!myInternalSizeChanged && sizeWasIncreased && shouldExpand) {
            List<BranchMoreAction> mores = getMoreActions();
            for (BranchMoreAction more : mores) {
                if (!getList().getScrollableTracksViewportHeight()) {
                    break;
                }
                if (!more.isExpanded()) {
                    more.setExpanded(true);
                    getListModel().refilter();
                }
            }
        }
        myPrevSize = newSize;
        //ugly properties to distinguish user size changed from pack method call after Restore Size action performed
        myUserSizeChanged = !myInternalSizeChanged;
        myInternalSizeChanged = false;
    }

    @Nonnull
    private List<BranchMoreAction> getMoreActions() {
        List<BranchMoreAction> result = new ArrayList<>();
        ListPopupModel model = getListModel();
        for (int i = 0; i < model.getSize(); i++) {
            BranchMoreAction moreAction = getSpecificAction(model.getElementAt(i), BranchMoreAction.class);
            if (moreAction != null) {
                result.add(moreAction);
            }
        }
        return result;
    }

    public void addToolbarAction(@Nonnull AnAction action, boolean underSettingsPopup) {
        if (underSettingsPopup) {
            mySettingsActions.add(action);
        }
        else {
            myToolbarActions.add(0, action);
        }
    }

    @Nonnull
    private static ActionGroup createBranchSpeedSearchActionGroup(@Nonnull ActionGroup actions) {
        ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
        group.add(actions);
        group.addAll(createSpeedSearchActions(actions, true));
        return group.build();
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void afterShow() {
        super.afterShow();
        myShown = true;
        Dimension size = getSize();
        if (size != null) {
            myPrevSize = size;
        }
        trackDimensions(myKey);
    }

    private static List<AnAction> createSpeedSearchActions(@Nonnull ActionGroup parentActionGroup, boolean isFirstLevel) {
        if (parentActionGroup instanceof BranchHideableActionGroup) {
            parentActionGroup = ((BranchHideableActionGroup) parentActionGroup).getDelegate();
        }

        if (parentActionGroup instanceof BranchActionGroup) {
            return Collections.emptyList();
        }

        // add per repository branches into the model as Speed Search elements and show them only if regular items were not found by mask;
        List<AnAction> speedSearchActions = new ArrayList<>();
        if (!isFirstLevel) {
            speedSearchActions.add(AnSeparator.create(parentActionGroup.getTemplatePresentation().getTextValue()));
        }
        for (AnAction child : parentActionGroup.getChildren(null)) {
            if (child instanceof ActionGroup) {
                ActionGroup childGroup = (ActionGroup) child;
                if (childGroup instanceof BranchHideableActionGroup) {
                    childGroup = ((BranchHideableActionGroup) childGroup).getDelegate();
                }

                if (isFirstLevel) {
                    speedSearchActions.addAll(createSpeedSearchActions(childGroup, false));
                }
                else if (childGroup instanceof BranchActionGroup) {
                    speedSearchActions.add(FlatSpeedSearchPopupFactory.createSpeedSearchActionGroupWrapper(childGroup));
                }
            }
        }
        return speedSearchActions;
    }

    @Override
    public void handleSelect(boolean handleFinalChoices) {
        super.handleSelect(handleFinalChoices, null);
        if (getSpecificAction(getList().getSelectedValue(), BranchMoreAction.class) != null) {
            getListModel().refilter();
        }
    }

    @Override
    public void handleSelect(boolean handleFinalChoices, InputEvent e) {
        BranchActionGroup branchActionGroup = getSelectedBranchGroup();
        if (branchActionGroup != null && e instanceof MouseEvent && myListElementRenderer.isIconAt(((MouseEvent) e).getPoint())) {
            branchActionGroup.toggle();
            getList().repaint();
        }
        else {
            super.handleSelect(handleFinalChoices, e);
        }
    }

    @Override
    protected void handleToggleAction() {
        BranchActionGroup branchActionGroup = getSelectedBranchGroup();
        if (branchActionGroup != null) {
            branchActionGroup.toggle();
            getList().repaint();
        }
        else {
            super.handleToggleAction();
        }
    }

    @Nullable
    private BranchActionGroup getSelectedBranchGroup() {
        return getSpecificAction(getList().getSelectedValue(), BranchActionGroup.class);
    }

    @Override
    protected void onSpeedSearchPatternChanged() {
        String newFilter = mySpeedSearch.getFilter();
        if (newFilter.endsWith(" ")) {
            mySpeedSearch.updatePattern(newFilter.trim());
            return;
        }
        getList().setSelectedIndex(0);
        super.onSpeedSearchPatternChanged();
        ScrollingUtil.ensureSelectionExists(getList());
    }

    @Override
    protected boolean shouldBeShowing(@Nonnull AnAction action) {
        if (!super.shouldBeShowing(action)) {
            return false;
        }
        if (getSpeedSearch().isHoldingFilter()) {
            return !(action instanceof BranchMoreAction);
        }
        return !(action instanceof BranchMoreHideableActionGroup moreHideableActionGroup) || moreHideableActionGroup.shouldBeShown();
    }

    @Override
    protected WizardPopup createPopup(WizardPopup parent, PopupStep step, Object parentValue) {
        return createListPopupStep(parent, step, parentValue);
    }

    private WizardPopup createListPopupStep(WizardPopup parent, PopupStep step, Object parentValue) {
        if (step instanceof ListPopupStep) {
            return new BranchActionGroupPopup(parent, (ListPopupStep) step, parentValue);
        }
        return super.createPopup(parent, step, parentValue);
    }

    @Override
    protected MyPopupListElementRenderer getListElementRenderer() {
        if (myListElementRenderer == null) {
            myListElementRenderer = new MyPopupListElementRenderer(this);
        }
        return myListElementRenderer;
    }

    private static class MyPopupListElementRenderer extends PopupListElementRendererWithIcon {
        private ErrorLabel myInfoLabel;

        MyPopupListElementRenderer(ListPopupImpl aPopup) {
            super(aPopup);
        }

        @Override
        protected SeparatorWithText createSeparator() {
            return new MyTextSeparator();
        }

        @Override
        protected void customizeComponent(JList list, Object value, boolean isSelected) {
            BranchMoreAction more = getSpecificAction(value, BranchMoreAction.class);
            if (more != null) {
                myTextLabel.setForeground(JBColor.gray);
            }
            super.customizeComponent(list, value, isSelected);
            BranchActionGroup branchActionGroup = getSpecificAction(value, BranchActionGroup.class);
            if (branchActionGroup != null) {
                myTextLabel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
                myTextLabel.setIcon(TargetAWT.to(chooseUpdateIndicatorIcon(branchActionGroup)));
            }
            PopupElementWithAdditionalInfo additionalInfoAction = getSpecificAction(value, PopupElementWithAdditionalInfo.class);
            updateInfoComponent(myInfoLabel, additionalInfoAction != null ? additionalInfoAction.getInfoText() : null, isSelected);
        }

        private static Image chooseUpdateIndicatorIcon(@Nonnull BranchActionGroup branchActionGroup) {
            if (branchActionGroup.hasIncomingCommits()) {
                return branchActionGroup.hasOutgoingCommits() ? DistributedVersionControlIconGroup.incomingoutgoing() : DistributedVersionControlIconGroup.incoming();
            }
            return branchActionGroup.hasOutgoingCommits() ? DistributedVersionControlIconGroup.outgoing() : null;
        }

        private void updateInfoComponent(@Nonnull ErrorLabel infoLabel, @Nullable String infoText, boolean isSelected) {
            if (infoText != null) {
                infoLabel.setVisible(true);
                infoLabel.setText(infoText);

                if (isSelected) {
                    setSelected(infoLabel);
                }
                else {
                    infoLabel.setBackground(getBackground());
                    infoLabel.setForeground(JBColor.GRAY);    // different foreground than for other elements
                }
            }
            else {
                infoLabel.setVisible(false);
            }
        }

        @Override
        protected JComponent createItemComponent() {
            myTextLabel = new ErrorLabel() {
                @Override
                public void setText(String text) {
                    super.setText(text);
                }
            };
            myTextLabel.setOpaque(false);
            myTextLabel.setBorder(JBUI.Borders.empty(1));

            myInfoLabel = new ErrorLabel();
            myInfoLabel.setOpaque(false);
            myInfoLabel.setBorder(JBUI.Borders.empty(1, DEFAULT_HGAP, 1, 1));
            myInfoLabel.setFont(FontUtil.minusOne(myInfoLabel.getFont()));

            JPanel compoundPanel = new JPanel(new BorderLayout());
            compoundPanel.setOpaque(false);
            myIconLabel = new IconComponent();
            myInfoLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            myIconLabel.setOpaque(false);

            JPanel compoundTextPanel = new NonOpaquePanel(new BorderLayout());
            JPanel textPanel = new NonOpaquePanel(new BorderLayout());
            compoundPanel.add(myIconLabel, BorderLayout.WEST);
            textPanel.add(myTextLabel, BorderLayout.WEST);
            textPanel.add(myInfoLabel, BorderLayout.CENTER);
            compoundTextPanel.add(textPanel, BorderLayout.CENTER);
            compoundPanel.add(compoundTextPanel, BorderLayout.CENTER);
            return layoutComponent(compoundPanel);
        }
    }

    private static class MyTextSeparator extends SeparatorWithText {

        MyTextSeparator() {
            super();
            setTextForeground(UIUtil.getListForeground());
            setCaptionCentered(false);
            UIUtil.addInsets(this, DEFAULT_VGAP, UIUtil.getListCellHPadding(), 0, 0);
        }

        @Override
        protected void paintLine(Graphics g, int x, int y, int width) {
            if (StringUtil.isEmptyOrSpaces(getCaption())) {
                super.paintLine(g, x, y, width);
            }
        }
    }
}
