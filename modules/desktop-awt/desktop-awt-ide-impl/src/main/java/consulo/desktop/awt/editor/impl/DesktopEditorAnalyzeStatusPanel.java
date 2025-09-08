/*
 * Copyright 2013-2020 consulo.io
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
package consulo.desktop.awt.editor.impl;

import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.PowerSaveMode;
import consulo.application.dumb.DumbAware;
import consulo.application.impl.internal.performance.ActivityTracker;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.codeEditor.EditorBundle;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorColorsScheme;
import consulo.component.messagebus.MessageBusConnection;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.language.editor.DesktopEditorFloatPanel;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionImplUtil;
import consulo.ui.ex.awt.event.AncestorListenerAdapter;
import consulo.ide.impl.idea.ui.components.labels.DropDownLink;
import consulo.ide.impl.idea.ui.popup.util.PopupState;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.language.editor.impl.internal.markup.*;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.action.CustomComponentAction;
import consulo.ui.ex.awt.util.ComponentUtil;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.popup.ComponentPopupBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kava.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2020-06-19
 */
public class DesktopEditorAnalyzeStatusPanel implements Disposable {
    private static class StatusButton extends JButton {
        private static final int LEFT_RIGHT_INDENT = 5;
        private static final int INTER_GROUP_OFFSET = 6;

        private final MouseListener mouseListener;
        private final PropertyChangeListener presentationPropertyListener;
        private final Presentation presentation;
        private final EditorColorsScheme colorsScheme;
        private boolean translucent;

        private final JPanel myContainerPanel;

        private StatusButton(@Nonnull AnAction action, @Nonnull Presentation presentation, @Nonnull String place, @Nonnull EditorColorsScheme colorsScheme, @Nonnull BooleanSupplier hasNavButtons) {
            myContainerPanel = new JPanel(new GridBagLayout());
            myContainerPanel.setOpaque(false);

            putClientProperty("JButton.buttonType", "borderless");

            add(myContainerPanel);

            this.presentation = presentation;
            this.colorsScheme = colorsScheme;

            presentationPropertyListener = l -> {
                String propName = l.getPropertyName();
                if (propName.equals(EXPANDED_STATUS.toString()) && l.getNewValue() != null) {
                    //noinspection unchecked
                    List<StatusItem> newStatus = (List<StatusItem>) l.getNewValue();
                    updateContents(newStatus);
                    translucent = false;
                    revalidate();
                    repaint();
                }
                else if (propName.equals(TRANSLUCENT_STATE.toString())) {
                    translucent = l.getNewValue() == Boolean.TRUE;
                    repaint();
                }
            };

            mouseListener = new MouseAdapter() {
                @Override
                @RequiredUIAccess
                public void mouseClicked(MouseEvent me) {
                    DataContext context = getDataContext();
                    AnActionEvent event = AnActionEvent.createFromInputEvent(
                        me,
                        place,
                        presentation,
                        context,
                        false,
                        true,
                        DesktopAWTInputDetails.convert(StatusButton.this, me)
                    );
                    if (!ActionImplUtil.lastUpdateAndCheckDumb(action, event, false)) {
                        return;
                    }

                    if (presentation.isEnabled()) {
                        ActionManagerEx manager = ActionManagerEx.getInstanceEx();
                        manager.fireBeforeActionPerformed(action, context, event);

                        action.actionPerformed(event);

                        manager.queueActionPerformedEvent(action, context, event);
                        //ActionsCollector.getInstance().record(event.getProject(), action, event, null);

                        ActionToolbar toolbar = getActionToolbar();
                        if (toolbar != null) {
                            toolbar.updateActionsAsync();
                        }
                    }
                }
            };
            updateUI();
        }

        @Override
        public void addNotify() {
            super.addNotify();
            presentation.addPropertyChangeListener(presentationPropertyListener);
            addMouseListener(mouseListener);
        }

        @Override
        public void removeNotify() {
            presentation.removePropertyChangeListener(presentationPropertyListener);
            removeMouseListener(mouseListener);
        }

        private DataContext getDataContext() {
            ActionToolbar actionToolbar = getActionToolbar();
            return actionToolbar != null ? actionToolbar.getToolbarDataContext() : DataManager.getInstance().getDataContext(this);
        }

        private ActionToolbar getActionToolbar() {
            return ComponentUtil.getParentOfType(ActionToolbar.class, this);
        }

        private void updateContents(@Nonnull List<StatusItem> status) {
            myContainerPanel.removeAll();

            setEnabled(!status.isEmpty());
            setVisible(!status.isEmpty());

            GridBag gc = new GridBag().nextLine();
            if (status.size() == 1 && StringUtil.isEmpty(status.get(0).getText())) {
                myContainerPanel.add(createStyledLabel(null, status.get(0).getIcon(), SwingConstants.CENTER), gc.next().weightx(1).fillCellHorizontally());
            }
            else if (status.size() > 0) {
                int leftRightOffset = JBUIScale.scale(LEFT_RIGHT_INDENT);
                myContainerPanel.add(Box.createHorizontalStrut(leftRightOffset), gc.next());

                int counter = 0;
                for (StatusItem item : status) {
                    myContainerPanel.add(createStyledLabel(item.getText(), item.getIcon(), SwingConstants.LEFT), gc.next().insetLeft(counter++ > 0 ? INTER_GROUP_OFFSET : 0));
                }

                myContainerPanel.add(Box.createHorizontalStrut(leftRightOffset), gc.next());
            }
        }

        private JLabel createStyledLabel(@Nullable String text, @Nullable Image icon, int alignment) {
            JLabel label = new JLabel(text, TargetAWT.to(icon), alignment) {
                @Override
                protected void paintComponent(Graphics graphics) {
                    Graphics2D g2 = (Graphics2D) graphics.create();
                    try {
                        float alpha = translucent ? 0.5f : 1.0f;
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                        super.paintComponent(g2);
                    }
                    finally {
                        g2.dispose();
                    }
                }
            };

            label.setForeground(new JBColor(() -> ObjectUtil.notNull(TargetAWT.to(colorsScheme.getColor(ICON_TEXT_COLOR)), TargetAWT.to(ICON_TEXT_COLOR.getDefaultColorValue()))));
            label.setIconTextGap(JBUIScale.scale(1));

            return label;
        }
    }

    private class StatusAction extends DumbAwareAction implements CustomComponentAction {
        @Override
        @Nonnull
        public JComponent createCustomComponent(@Nonnull Presentation presentation, @Nonnull String place) {
            return new StatusButton(this, presentation, place, myEditor.getColorsScheme(), () -> showNavigation);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            myPopupManager.showPopup(e.getInputEvent());
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            Presentation presentation = e.getPresentation();

            if (analyzerStatus != null) {
                List<StatusItem> newStatus = analyzerStatus.getExpandedStatus();
                Image newIcon = analyzerStatus.getIcon();

                if (!hasAnalyzed || analyzerStatus.getAnalyzingType() != AnalyzingType.EMPTY) {
                    if (newStatus.isEmpty()) {
                        newStatus = Collections.singletonList(new StatusItem("", newIcon));
                        presentation.putClientProperty(EXPANDED_STATUS, newStatus);
                    }

                    if (!Objects.equals(presentation.getClientProperty(EXPANDED_STATUS), newStatus)) {
                        presentation.putClientProperty(EXPANDED_STATUS, newStatus);
                    }

                    presentation.putClientProperty(TRANSLUCENT_STATE, analyzerStatus.getAnalyzingType() != AnalyzingType.COMPLETE);
                }
                else {
                    presentation.putClientProperty(TRANSLUCENT_STATE, true);
                }
            }
            else {
                presentation.putClientProperty(EXPANDED_STATUS, Collections.emptyList());
            }
        }
    }

    private class InspectionPopupManager {
        private final JPanel myContent = new JPanel(new GridBagLayout());
        private final ComponentPopupBuilder myPopupBuilder;
        private final Map<String, JProgressBar> myProgressBarMap = new HashMap<>();
        private final AncestorListener myAncestorListener;
        private final JBPopupListener myPopupListener;
        private final PopupState myPopupState = new PopupState();

        private JBPopup myPopup;

        private InspectionPopupManager() {
            myContent.setOpaque(true);
            myContent.setBackground(UIUtil.getToolTipBackground());

            myPopupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(myContent, null).
                setCancelOnClickOutside(true).
                setCancelCallback(() -> analyzerStatus == null || analyzerStatus.getController().canClosePopup());

            myAncestorListener = new AncestorListenerAdapter() {
                @Override
                public void ancestorMoved(AncestorEvent event) {
                    hidePopup();
                }
            };

            myPopupListener = new JBPopupListener() {
                @Override
                public void onClosed(@Nonnull LightweightWindowEvent event) {
                    if (analyzerStatus != null) {
                        analyzerStatus.getController().onClosePopup();
                    }
                    myEditor.getComponent().removeAncestorListener(myAncestorListener);
                }
            };
        }

        private void showPopup(@Nonnull InputEvent event) {
            showPopup(event, (size) -> {
                JComponent owner = (JComponent) event.getComponent();
                return new RelativePoint(owner, new Point(owner.getWidth() - owner.getInsets().right + JBUIScale.scale(DELTA_X) - size.width, owner.getHeight() + JBUIScale.scale(DELTA_Y)));
            });
        }

        private void showPopup(@Nonnull InputEvent event, @Nonnull Function<Dimension, RelativePoint> pointFunction) {
            hidePopup();
            if (myPopupState.isRecentlyHidden()) {
                return; // do not show new popup
            }

            updateContentPanel(analyzerStatus.getController());

            myPopup = myPopupBuilder.createPopup();
            myPopup.addListener(myPopupListener);
            myPopup.addListener(myPopupState);
            myEditor.getComponent().addAncestorListener(myAncestorListener);

            Dimension size = myContent.getPreferredSize();
            size.width = Math.max(size.width, JBUIScale.scale(296));

            myPopup.setSize(size);

            myPopup.show(pointFunction.apply(size));
        }

        private void hidePopup() {
            if (myPopup != null && !myPopup.isDisposed()) {
                myPopup.cancel();
            }
            myPopup = null;
        }

        @RequiredUIAccess
        private void updateContentPanel(@Nonnull UIController controller) {
            List<PassWrapper> passes = analyzerStatus.getPasses();
            Set<String> presentableNames = ContainerUtil.map2Set(passes, PassWrapper::getPresentableName);

            if (!presentableNames.isEmpty() && myProgressBarMap.keySet().equals(presentableNames)) {
                for (PassWrapper pass : passes) {
                    myProgressBarMap.get(pass.getPresentableName()).setValue(pass.toPercent());
                }
                return;
            }
            myContent.removeAll();

            GridBag gc = new GridBag().nextLine().next().
                anchor(GridBagConstraints.LINE_START).
                weightx(1).
                fillCellHorizontally().
                insets(10, 10, 10, 0);

            boolean hasTitle = StringUtil.isNotEmpty(analyzerStatus.getTitle());

            if (hasTitle) {
                myContent.add(new JLabel(XmlStringUtil.wrapInHtml(analyzerStatus.getTitle())), gc);
            }
            else if (StringUtil.isNotEmpty(analyzerStatus.getDetails())) {
                myContent.add(new JLabel(XmlStringUtil.wrapInHtml(analyzerStatus.getDetails())), gc);
            }
            else if (analyzerStatus.getExpandedStatus().size() > 0 && analyzerStatus.getAnalyzingType() != AnalyzingType.EMPTY) {
                myContent.add(createDetailsPanel(), gc);
            }

            List<AnAction> actions = controller.getActions();
            if (!actions.isEmpty()) {
                ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("AnalyzeStatusPanel", new WrapperGroup(actions), true);
                toolbar.setTargetComponent(myContent);
                toolbar.setMiniMode(true);
                toolbar.updateActionsAsync();

                JComponent component = toolbar.getComponent();

                myContent.add(component, gc.next().anchor(GridBagConstraints.LINE_END).weightx(0).insets(10, 6, 10, 6));
            }

            myProgressBarMap.clear();
            JPanel myProgressPanel = new NonOpaquePanel(new GridBagLayout());
            GridBag progressGC = new GridBag();
            for (PassWrapper pass : passes) {
                myProgressPanel.add(new JLabel(pass.getPresentableName() + ": "), progressGC.nextLine().next().anchor(GridBagConstraints.LINE_START).weightx(0).insets(0, 10, 0, 6));

                JProgressBar pb = new JProgressBar(0, 100);
                pb.setValue(pass.toPercent());
                myProgressPanel.add(pb, progressGC.next().anchor(GridBagConstraints.LINE_START).weightx(1).fillCellHorizontally().insets(0, 0, 0, 6));
                myProgressBarMap.put(pass.getPresentableName(), pb);
            }

            myContent.add(myProgressPanel, gc.nextLine().next().anchor(GridBagConstraints.LINE_START).fillCellHorizontally().coverLine().weightx(1));

            if (hasTitle) {
                int topIndent = !myProgressBarMap.isEmpty() ? 10 : 0;
                gc.nextLine().next().anchor(GridBagConstraints.LINE_START).fillCellHorizontally().coverLine().weightx(1).insets(topIndent, 10, 10, 6);

                if (StringUtil.isNotEmpty(analyzerStatus.getDetails())) {
                    myContent.add(new JLabel(XmlStringUtil.wrapInHtml(analyzerStatus.getDetails())), gc);
                }
                else if (analyzerStatus.getExpandedStatus().size() > 0 && analyzerStatus.getAnalyzingType() != AnalyzingType.EMPTY) {
                    myContent.add(createDetailsPanel(), gc);
                }
            }

            //if (Experiments.getInstance().isFeatureEnabled("problems.view.enabled")) {
            //  JLabel openProblemsViewLabel = new TrackableLinkLabel(EditorBundle.message("iw.open.problems.view"), () -> {
            //    hidePopup();
            //    controller.openProblemsView();
            //  });
            //  myContent.add(openProblemsViewLabel, gc.nextLine().next().anchor(GridBagConstraints.LINE_START).fillCellHorizontally().coverLine().weightx(1).insets(10, 10, 10, 0));
            //}

            myContent.add(createLowerPanel(controller), gc.nextLine().next().anchor(GridBagConstraints.LINE_START).fillCellHorizontally().coverLine().weightx(1));
        }

        private void updateVisiblePopup() {
            if (myPopup != null && myPopup.isVisible()) {
                updateContentPanel(analyzerStatus.getController());

                Dimension size = myContent.getPreferredSize();
                size.width = Math.max(size.width, JBUIScale.scale(296));
                myPopup.setSize(size);
            }
        }

        @Nonnull
        private JComponent createDetailsPanel() {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < analyzerStatus.getExpandedStatus().size(); i++) {
                boolean last = i == analyzerStatus.getExpandedStatus().size() - 1;
                StatusItem item = analyzerStatus.getExpandedStatus().get(i);

                text.append(item.getText()).append(" ").append(item.getType());
                if (!last) {
                    text.append(", ");
                }
                else if (analyzerStatus.getAnalyzingType() != AnalyzingType.COMPLETE) {
                    text.append(" ").append(EditorBundle.message("iw.found.so.far.suffix"));
                }
            }

            return new JLabel(text.toString());
        }

        private JPanel createLowerPanel(@Nonnull UIController controller) {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBag gc = new GridBag().nextLine();

            if (PowerSaveMode.isEnabled()) {
                panel.add(new TrackableLinkLabel(EditorBundle.message("iw.disable.powersave"), () -> {
                    PowerSaveMode.setEnabled(false);
                    hidePopup();
                }), gc.next().anchor(GridBagConstraints.LINE_START));
            }
            else {
                List<LanguageHighlightLevel> levels = controller.getHighlightLevels();

                if (levels.size() == 1) {
                    JLabel highlightLabel = new JLabel(EditorBundle.message("iw.highlight.label") + " ");
                    highlightLabel.setForeground(JBCurrentTheme.Link.linkColor());

                    panel.add(highlightLabel, gc.next().anchor(GridBagConstraints.LINE_START));
                    panel.add(createDropDownLink(levels.get(0), controller), gc.next());
                }
                else if (levels.size() > 1) {
                    for (LanguageHighlightLevel level : levels) {
                        JLabel highlightLabel = new JLabel(level.getLangID() + ": ");
                        highlightLabel.setForeground(JBCurrentTheme.Link.linkColor());

                        panel.add(highlightLabel, gc.next().anchor(GridBagConstraints.LINE_START).gridx > 0 ? gc.insetLeft(8) : gc);
                        panel.add(createDropDownLink(level, controller), gc.next());
                    }
                }
            }
            Component component = Box.createHorizontalGlue();
            panel.add(component, gc.next().fillCellHorizontally().weightx(1.0));

            controller.fillHectorPanels(panel, gc);

            panel.setOpaque(true);
            panel.setBorder(JBUI.Borders.empty(4, 10));
            return panel;
        }

        @Nonnull
        private DropDownLink<InspectionsLevel> createDropDownLink(@Nonnull LanguageHighlightLevel level, @Nonnull UIController controller) {
            return new DropDownLink<>(level.getLevel(), controller.getAvailableLevels(), inspectionsLevel -> {
                controller.setHighLightLevel(level.copy(level.getLangID(), inspectionsLevel));
                myContent.revalidate();

                Dimension size = myContent.getPreferredSize();
                size.width = Math.max(size.width, JBUIScale.scale(296));
                myPopup.setSize(size);

                // Update statistics
                //FeatureUsageData data = new FeatureUsageData().
                //        addProject(myEditor.getProject()).
                //        addLanguage(level.getLangID()).
                //        addData("level", inspectionsLevel.toString());
                //
                //FUCounterUsageLogger.getInstance().logEvent("inspection.widget", "highlight.level.changed", data);
            }, true);
        }
    }

    private class WrapperGroup extends DumbAwareActionGroup implements  HintManagerImpl.ActionToIgnore {
        private final ActionGroup[] myActions;

        public WrapperGroup(@Nonnull List<? extends AnAction> actions) {
            myActions = new ActionGroup[]{new MenuAction(actions)};
        }

        @Nonnull
        @Override
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
            return myActions;
        }
    }

    private class MenuAction extends DefaultActionGroup implements DumbAware, HintManagerImpl.ActionToIgnore {
        private MenuAction(@Nonnull List<? extends AnAction> actions) {
            setPopup(true);
            addAll(actions);
            add(new ToggleAction(CodeEditorLocalize.iwCompactView()) {
                @Override
                public boolean isSelected(@Nonnull AnActionEvent e) {
                    return !showToolbar;
                }

                @Override
                public void setSelected(@Nonnull AnActionEvent e, boolean state) {
                    showToolbar = !state;
                    EditorSettingsExternalizable.getInstance().setShowInspectionWidget(showToolbar);
                    updateTrafficLightVisibility();
                    //ActionsCollector.getInstance().record(e.getProject(), this, e, null);
                }

                @Override
                public void update(@Nonnull AnActionEvent e) {
                    super.update(e);
                    e.getPresentation().setEnabled(analyzerStatus == null || analyzerStatus.getController().enableToolbar());
                }

                @Override
                public boolean isDumbAware() {
                    return true;
                }
            });
        }

        @Override
        public boolean showBelowArrow() {
            return false;
        }

        @Nullable
        @Override
        protected Image getTemplateIcon() {
            return PlatformIconGroup.actionsMorevertical();
        }
    }

    private static class TrackableLinkLabel extends LinkLabel<Object> {
        private InputEvent myEvent;

        private TrackableLinkLabel(@Nonnull String text, @Nonnull Runnable action) {
            super(text, (Image) null);
            setListener((aSource, aLinkData) -> {
                action.run();
                //  ActionsCollector.getInstance().record(null, myEvent, getClass());
            }, null);
        }

        @Override
        public void doClick(InputEvent e) {
            myEvent = e;
            super.doClick(e);
        }
    }

    private static final Key<List<StatusItem>> EXPANDED_STATUS = Key.create("EXPANDED_STATUS");
    private static final Key<Boolean> TRANSLUCENT_STATE = Key.create("TRANSLUCENT_STATE");
    private static final int DELTA_X = 6;
    private static final int DELTA_Y = 6;

    private static final EditorColorKey ICON_TEXT_COLOR = EditorColorKey.createColorKey("ActionButtonImpl.iconTextForeground", TargetAWT.from(UIUtil.getContextHelpForeground()));

    private final DesktopEditorMarkupModelImpl myModel;

    private final DesktopEditorImpl myEditor;

    private final ActionToolbar statusToolbar;
    private boolean showToolbar;
    private boolean trafficLightVisible;
    private final ComponentListener toolbarComponentListener;
    private Rectangle cachedToolbarBounds = new Rectangle();
    private AnalyzerStatus analyzerStatus;
    private boolean hasAnalyzed;
    private boolean isAnalyzing;
    private boolean showNavigation;
    private InspectionPopupManager myPopupManager = new InspectionPopupManager();
    private final MergingUpdateQueue myStatusUpdates;

    private DesktopEditorErrorPanel myErrorPanel;

    public DesktopEditorAnalyzeStatusPanel(DesktopEditorMarkupModelImpl model) {
        myModel = model;
        myEditor = (DesktopEditorImpl) model.getEditor();
        myStatusUpdates = new MergingUpdateQueue(getClass().getName(), 50, true, MergingUpdateQueue.ANY_COMPONENT, this);

        AnAction nextErrorAction = createAction("GotoNextError", AllIcons.Actions.FindAndShowNextMatchesSmall);
        AnAction prevErrorAction = createAction("GotoPreviousError", AllIcons.Actions.FindAndShowPrevMatchesSmall);
        DefaultActionGroup navigateGroup = new DefaultActionGroup(nextErrorAction, prevErrorAction) {
            @Override
            public void update(@Nonnull AnActionEvent e) {
                e.getPresentation().setEnabledAndVisible(showNavigation);
            }
        };

        AnAction statusAction = new StatusAction();
        ActionGroup actions = new DefaultActionGroup(statusAction, navigateGroup);
        statusToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.EDITOR_INSPECTIONS_TOOLBAR, actions, true);

        MessageBusConnection connection = Application.get().getMessageBus().connect(this);
        connection.subscribe(AnActionListener.class, new AnActionListener() {
            @Override
            public void beforeActionPerformed(@Nonnull AnAction action, @Nonnull DataContext dataContext, @Nonnull AnActionEvent event) {
                if (action instanceof HintManagerImpl.ActionToIgnore) {
                    return;
                }
                myPopupManager.hidePopup();
            }
        });

        statusToolbar.setMiniMode(true);
        toolbarComponentListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                Component toolbar = event.getComponent();
                if (toolbar.getWidth() > 0 && toolbar.getHeight() > 0) {
                    updateTrafficLightVisibility();
                }
            }
        };

    }

    public void updateUI() {
        JComponent toolbarComponent = statusToolbar.getComponent();
        toolbarComponent.addComponentListener(toolbarComponentListener);

        DesktopEditorFloatPanel statusPanel = new DesktopEditorFloatPanel() {
            @Override
            public Color getBackground() {
                return TargetAWT.to(myEditor.getBackgroundColor());
            }
        };
        statusPanel.setVisible(!myEditor.isOneLineMode());
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
        statusPanel.add(toolbarComponent);

        statusToolbar.setTargetComponent(statusPanel);

        myEditor.setStatusComponent(statusPanel);
    }

    public void setErrorPanel(@Nullable DesktopEditorErrorPanel errorPanel) {
        myErrorPanel = errorPanel;
    }

    private AnAction createAction(@Nonnull String id, @Nonnull Image icon) {
        AnAction delegate = ActionManager.getInstance().getAction(id);
        AnAction result = new DumbAwareAction(delegate.getTemplatePresentation().getTextValue(), LocalizeValue.of(), icon) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                IdeFocusManager focusManager = ProjectIdeFocusManager.getInstance(myEditor.getProject());

                AnActionEvent delegateEvent = AnActionEvent.createFromAnAction(delegate, e.getInputEvent(), ActionPlaces.EDITOR_INSPECTIONS_TOOLBAR, myEditor.getDataContext());

                if (focusManager.getFocusOwner() != myEditor.getContentComponent()) {
                    focusManager.requestFocus(myEditor.getContentComponent(), true).doWhenDone(() -> {
                        delegate.actionPerformed(delegateEvent);
                    });
                }
                else {
                    delegate.actionPerformed(delegateEvent);
                }
            }
        };

        result.copyShortcutFrom(delegate);
        return result;
    }

    private void updateTrafficLightVisibility() {
        myStatusUpdates.queue(Update.create("visibility", this::doUpdateTrafficLightVisibility));
    }

    private void doUpdateTrafficLightVisibility() {
        if (trafficLightVisible) {
            if (showToolbar && myEditor.myView != null) {
                VisualPosition pos = myEditor.getCaretModel().getPrimaryCaret().getVisualPosition();
                Point point = myEditor.visualPositionToXY(pos);
                point = SwingUtilities.convertPoint(myEditor.getContentComponent(), point, myEditor.getScrollPane());

                JComponent stComponent = statusToolbar.getComponent();
                if (stComponent.isVisible()) {
                    Rectangle bounds = SwingUtilities.convertRectangle(stComponent, stComponent.getBounds(), myEditor.getScrollPane());

                    if (!bounds.isEmpty() && bounds.contains(point)) {
                        cachedToolbarBounds = bounds;
                        stComponent.setVisible(false);
                        setSmallIconVisible(true);
                    }
                }
                else if (!cachedToolbarBounds.contains(point)) {
                    stComponent.setVisible(true);
                    setSmallIconVisible(false);
                }
            }
            else {
                statusToolbar.getComponent().setVisible(false);
                setSmallIconVisible(true);
            }
        }
        else {
            statusToolbar.getComponent().setVisible(false);
            setSmallIconVisible(false);
        }
    }

    private void setSmallIconVisible(boolean visible) {
        if (myErrorPanel != null) {
            myErrorPanel.setSmallIconVisible(visible);
        }
    }

    public void repaintTrafficLightIcon() {
        ErrorStripeRenderer errorStripeRenderer = myModel.getErrorStripeRenderer();

        if (errorStripeRenderer == null) {
            return;
        }

        myStatusUpdates.queue(Update.create("icon", () -> {
            if (errorStripeRenderer != null) {
                AnalyzerStatus newStatus = errorStripeRenderer.getStatus(myEditor);
                if (!AnalyzerStatus.equals(newStatus, analyzerStatus)) {
                    changeStatus(newStatus);
                }

                if (myErrorPanel != null) {
                    myErrorPanel.repaint();
                }
            }
        }));
    }

    public void setTrafficLightIconVisible(boolean value) {
        if (value != trafficLightVisible) {
            trafficLightVisible = value;
            updateTrafficLightVisibility();
        }
    }

    private void changeStatus(AnalyzerStatus newStatus) {
        boolean resetAnalyzingStatus = analyzerStatus != null && analyzerStatus.isTextStatus() && analyzerStatus.getAnalyzingType() == AnalyzingType.COMPLETE;
        analyzerStatus = newStatus;
        //smallIconLabel.setIcon(analyzerStatus.getIcon());

        if (showToolbar != analyzerStatus.getController().enableToolbar()) {
            showToolbar = EditorSettingsExternalizable.getInstance().isShowInspectionWidget() && analyzerStatus.getController().enableToolbar();
            updateTrafficLightVisibility();
        }

        boolean analyzing = analyzerStatus.getAnalyzingType() != AnalyzingType.COMPLETE;
        hasAnalyzed = !resetAnalyzingStatus && (hasAnalyzed || (isAnalyzing && !analyzing));
        isAnalyzing = analyzing;

        if (analyzerStatus.getAnalyzingType() != AnalyzingType.EMPTY) {
            showNavigation = analyzerStatus.isShowNavigation();
        }

        myPopupManager.updateVisiblePopup();
        ActivityTracker.getInstance().inc();
    }

    public void showStatusPopup(InputEvent e, @Nonnull Function<Dimension, RelativePoint> function) {
        myPopupManager.showPopup(e, function);
    }

    @Override
    public void dispose() {
    }
}
