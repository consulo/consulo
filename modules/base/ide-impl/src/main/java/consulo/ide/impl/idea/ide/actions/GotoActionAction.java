// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.dumb.DumbAware;
import consulo.application.impl.internal.progress.ProgressWindowListener;
import consulo.application.ui.UISettings;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.ide.actions.searcheverywhere.ActionSearchEverywhereContributor;
import consulo.ide.impl.idea.ide.ui.search.BooleanOptionDescription;
import consulo.ide.impl.idea.ide.ui.search.OptionDescription;
import consulo.ide.impl.idea.ide.util.gotoByName.ChooseByNamePopup;
import consulo.ide.impl.idea.ide.util.gotoByName.GotoActionItemProvider;
import consulo.ide.impl.idea.ide.util.gotoByName.GotoActionModel;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionImplUtil;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.keymap.impl.ActionShortcutRestrictions;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.KeymapPanel;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.JdkConstants;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import java.util.Set;

import static consulo.ide.impl.idea.openapi.keymap.KeymapUtil.getActiveKeymapShortcuts;

public class GotoActionAction extends GotoActionBase implements DumbAware {
    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        if (e.hasData(Project.KEY)) {
            showInSearchEverywherePopup(ActionSearchEverywhereContributor.class.getSimpleName(), e, false, true);
        }
        else {
            super.actionPerformed(e);
        }
    }

    @Override
    public void gotoActionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Component component = e.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
        Editor editor = e.getData(Editor.KEY);

        FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.action");
        GotoActionModel model = new GotoActionModel(project, component, editor);
        GotoActionCallback<Object> callback = new GotoActionCallback<>() {
            @Override
            public void elementChosen(@Nonnull ChooseByNamePopup popup, @Nonnull Object element) {
                if (project != null) {
                    // if the chosen action displays another popup, don't populate it automatically with the text from this popup
                    project.putUserData(ChooseByNamePopup.CHOOSE_BY_NAME_POPUP_IN_PROJECT_KEY, null);
                }
                String enteredText = popup.getTrimmedText();
                int modifiers = popup.isClosedByShiftEnter() ? InputEvent.SHIFT_MASK : 0;
                openOptionOrPerformAction(((GotoActionModel.MatchedValue)element).value, enteredText, project, component, modifiers);
            }
        };

        Pair<String, Integer> start = getInitialText(false, e);
        showNavigationPopup(callback, null, createPopup(project, model, start.first, start.second, component, e), false);
    }

    @Nonnull
    private static ChooseByNamePopup createPopup(
        @Nullable Project project,
        @Nonnull GotoActionModel model,
        String initialText,
        int initialIndex,
        Component component,
        AnActionEvent event
    ) {
        ChooseByNamePopup oldPopup = project == null ? null : project.getUserData(ChooseByNamePopup.CHOOSE_BY_NAME_POPUP_IN_PROJECT_KEY);
        if (oldPopup != null) {
            oldPopup.close(false);
        }
        Disposable disposable = Disposable.newDisposable();
        ShortcutSet altEnterShortcutSet = getActiveKeymapShortcuts(IdeActions.ACTION_SHOW_INTENTION_ACTIONS);
        KeymapManager km = KeymapManager.getInstance();
        Keymap activeKeymap = km != null ? km.getActiveKeymap() : null;
        ChooseByNamePopup popup =
            new ChooseByNamePopup(project, model, new GotoActionItemProvider(model), oldPopup, initialText, false, initialIndex) {
                @Override
                protected void initUI(Callback callback, ModalityState modalityState, boolean allowMultipleSelection) {
                    super.initUI(callback, modalityState, allowMultipleSelection);
                    myList.addListSelectionListener(new ListSelectionListener() {
                        @Override
                        public void valueChanged(ListSelectionEvent e) {
                            Object value = myList.getSelectedValue();
                            String text = getText(value);
                            if (text != null && myDropdownPopup != null) {
                                myDropdownPopup.setAdText(text, SwingConstants.LEFT);
                            }
                        }

                        @Nullable
                        private String getText(@Nullable Object o) {
                            if (o instanceof GotoActionModel.MatchedValue mv) {
                                if (UISettings.getInstance().getShowInplaceCommentsInternal()
                                    && mv.value instanceof GotoActionModel.ActionWrapper actionWrapper) {
                                    AnAction action = actionWrapper.getAction();
                                    String actionId = ActionManager.getInstance().getId(action);
                                    return StringUtil.notNullize(actionId, "class: " + action.getClass().getName());
                                }

                                if (mv.value instanceof BooleanOptionDescription
                                    || mv.value instanceof GotoActionModel.ActionWrapper actionWrapper
                                    && actionWrapper.getAction() instanceof ToggleAction) {
                                    return "Press " + KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke(
                                        KeyEvent.VK_ENTER,
                                        0
                                    )) + " to toggle option";
                                }

                                if (altEnterShortcutSet.getShortcuts().length > 0
                                    && mv.value instanceof GotoActionModel.ActionWrapper aw
                                    && activeKeymap != null) {
                                    if (aw.isAvailable()) {
                                        String actionId = ActionManager.getInstance().getId(aw.getAction());
                                        boolean actionWithoutShortcuts = activeKeymap.getShortcuts(actionId).length == 0;
                                        if (actionWithoutShortcuts && new Random().nextInt(2) > 0) {
                                            String altEnter = KeymapUtil.getFirstKeyboardShortcutText(altEnterShortcutSet);
                                            return "Press " + altEnter + " to assign a shortcut for the selected action";
                                        }
                                    }
                                }
                            }
                            return getAdText();
                        }
                    });
                }

                @Nullable
                private String getValueDescription(@Nullable Object value) {
                    if (value instanceof GotoActionModel.MatchedValue mv
                        && mv.value instanceof GotoActionModel.ActionWrapper actionWrapper) {
                        AnAction action = actionWrapper.getAction();
                        return action.getTemplatePresentation().getDescription();
                    }
                    return null;
                }

                @Nonnull
                @Override
                protected Set<Object> filter(@Nonnull Set<Object> elements) {
                    return super.filter(model.sortItems(elements));
                }

                @Override
                protected boolean closeForbidden(boolean ok) {
                    if (!ok) {
                        return false;
                    }
                    Object element = getChosenElement();
                    return element instanceof GotoActionModel.MatchedValue mv
                        && processOptionInplace(mv.value, this, component, event)
                        || super.closeForbidden(true);
                }

                @Override
                public void setDisposed(boolean disposedFlag) {
                    super.setDisposed(disposedFlag);
                    Disposer.dispose(disposable);

                    for (ListSelectionListener listener : myList.getListSelectionListeners()) {
                        myList.removeListSelectionListener(listener);
                    }
                    UIUtil.dispose(myList);
                }
            };

        ApplicationManager.getApplication().getMessageBus().connect(disposable).subscribe(
            ProgressWindowListener.class,
            pw -> Disposer.register(
                pw,
                (Disposable)() -> {
                    if (!popup.checkDisposed()) {
                        popup.repaintList();
                    }
                }
            )
        );

        if (project != null) {
            project.putUserData(ChooseByNamePopup.CHOOSE_BY_NAME_POPUP_IN_PROJECT_KEY, popup);
        }

        popup.addMouseClickListener(new MouseAdapter() {
            @Override
            public void mouseClicked(@Nonnull MouseEvent me) {
                Object element = popup.getSelectionByPoint(me.getPoint());
                if (element instanceof GotoActionModel.MatchedValue mv && processOptionInplace(mv.value, popup, component, event)) {
                    me.consume();
                }
            }
        });

        DumbAwareAction.create(e -> {
            Object o = popup.getChosenElement();
            if (o instanceof GotoActionModel.MatchedValue mv && activeKeymap != null
                && mv.value instanceof GotoActionModel.ActionWrapper aw && aw.isAvailable()) {
                String id = ActionManager.getInstance().getId(aw.getAction());
                KeymapPanel.addKeyboardShortcut(
                    id,
                    ActionShortcutRestrictions.getInstance().getForActionId(id),
                    activeKeymap,
                    component
                );
            }
        }).registerCustomShortcutSet(altEnterShortcutSet, popup.getTextField(), disposable);

        return popup;
    }

    private static boolean processOptionInplace(Object value, ChooseByNamePopup popup, Component component, AnActionEvent e) {
        if (value instanceof BooleanOptionDescription option) {
            option.setOptionState(!option.isOptionEnabled());
            repaint(popup);
            return true;
        }
        else if (value instanceof GotoActionModel.ActionWrapper aw
            && aw.getAction() instanceof ToggleAction toggleAction) {
            performAction(toggleAction, component, e, 0, () -> repaint(popup));
            return true;
        }
        return false;
    }

    private static void repaint(@Nullable ChooseByNamePopup popup) {
        if (popup != null) {
            popup.repaintListImmediate();
        }
    }

    @RequiredUIAccess
    public static void openOptionOrPerformAction(
        @Nonnull Object element,
        String enteredText,
        @Nullable Project project,
        Component component
    ) {
        openOptionOrPerformAction(element, enteredText, project, component, 0);
    }

    @RequiredUIAccess
    private static void openOptionOrPerformAction(
        Object element,
        String enteredText,
        @Nullable Project project,
        Component component,
        @JdkConstants.InputEventMask int modifiers
    ) {
        if (element instanceof OptionDescription optionDescription) {
            String configurableId = optionDescription.getConfigurableId();
            if (optionDescription.hasExternalEditor()) {
                optionDescription.invokeInternalEditor();
            }
            else {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, configurableId, enteredText);
            }
        }
        else {
            ApplicationManager.getApplication().invokeLater(
                () -> ProjectIdeFocusManager.getInstance(project)
                    .doWhenFocusSettlesDown(() -> performAction(element, component, null, modifiers, null))
            );
        }
    }

    public static void performAction(@Nonnull Object element, @Nullable Component component, @Nullable AnActionEvent e) {
        performAction(element, component, e, 0, null);
    }

    private static void performAction(
        Object element,
        @Nullable Component component,
        @Nullable AnActionEvent e,
        @JdkConstants.InputEventMask int modifiers,
        @Nullable Runnable callback
    ) {
        // element could be AnAction (SearchEverywhere)
        if (component == null) {
            return;
        }
        AnAction action = element instanceof AnAction anAction ? anAction : ((GotoActionModel.ActionWrapper)element).getAction();
        Application.get().invokeLater(() -> {
            DataManager instance = DataManager.getInstance();
            DataContext context = instance != null ? instance.getDataContext(component) : DataContext.EMPTY_CONTEXT;
            InputEvent inputEvent = e != null ? e.getInputEvent() : null;
            AnActionEvent event = AnActionEvent.createFromAnAction(action, inputEvent, ActionPlaces.ACTION_SEARCH, context);
            if (inputEvent == null && modifiers != 0) {
                event = new AnActionEvent(
                    null,
                    event.getDataContext(),
                    event.getPlace(),
                    event.getPresentation(),
                    event.getActionManager(),
                    modifiers
                );
            }

            if (ActionImplUtil.lastUpdateAndCheckDumb(action, event, false)) {
                if (action instanceof ActionGroup actionGroup && !actionGroup.canBePerformed(context)) {
                    ListPopup popup = JBPopupFactory.getInstance()
                        .createActionGroupPopup(event.getPresentation().getText(), actionGroup, context, false, callback, -1);
                    Window window = SwingUtilities.getWindowAncestor(component);
                    if (window != null) {
                        popup.showInCenterOf(window);
                    }
                    else {
                        popup.showInFocusCenter();
                    }
                }
                else {
                    ActionManagerEx manager = ActionManagerEx.getInstanceEx();
                    manager.fireBeforeActionPerformed(action, context, event);
                    ActionImplUtil.performActionDumbAware(action, event);
                    if (callback != null) {
                        callback.run();
                    }
                    manager.fireAfterActionPerformed(action, context, event);
                }
            }
        });
    }

    @Override
    protected boolean requiresProject() {
        return false;
    }
}