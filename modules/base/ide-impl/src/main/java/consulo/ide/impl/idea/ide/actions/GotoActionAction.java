// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionImpl;
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
import consulo.ui.ex.action.BooleanOptionDescription;
import consulo.ui.ex.action.OptionDescription;
import consulo.ide.impl.idea.ide.util.gotoByName.ChooseByNamePopup;
import consulo.ide.impl.idea.ide.util.gotoByName.GotoActionItemProvider;
import consulo.ide.impl.idea.ide.util.gotoByName.GotoActionModel;
import consulo.ui.ex.awt.AWTConstants;
import consulo.ui.ex.impl.internal.action.ActionImplUtil;
import consulo.ui.ex.impl.internal.action.ActionRunnerAsync;
import consulo.ui.UIAccess;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.keymap.impl.ActionShortcutRestrictions;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.KeymapPanel;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.platform.base.localize.ActionLocalize;
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
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import java.util.Set;

import static consulo.ide.impl.idea.openapi.keymap.KeymapUtil.getActiveKeymapShortcuts;

@ActionImpl(id = "GotoAction")
public class GotoActionAction extends SearchEverywhereBaseAction implements DumbAware {
    public GotoActionAction() {
        super(ActionLocalize.actionGotoactionText(), ActionLocalize.actionGotoactionDescription());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        showInSearchEverywherePopup(ActionSearchEverywhereContributor.class.getSimpleName(), e, false, true);
    }

    @RequiredUIAccess
    public static void openOptionOrPerformAction(
        Object element,
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
        @AWTConstants.InputEventMask int modifiers
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

    public static void performAction(Object element, @Nullable Component component, @Nullable AnActionEvent e) {
        performAction(element, component, e, 0, null);
    }

    private static void performAction(
        Object element,
        @Nullable Component component,
        @Nullable AnActionEvent e,
        @AWTConstants.InputEventMask int modifiers,
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

            AnActionEvent finalEvent = event;
            UIAccess uiAccess = Application.get().getLastUIAccess();
            ActionRunnerAsync.lastUpdateAndCheckDumbAsync(action, finalEvent, false).whenCompleteAsync((enabled, throwable) -> {
                if (!Boolean.TRUE.equals(enabled)) {
                    return;
                }
                if (action instanceof ActionGroup actionGroup && !finalEvent.getPresentation().isPerformGroup()) {
                    ListPopup popup = JBPopupFactory.getInstance()
                        .createActionGroupPopup(finalEvent.getPresentation().getTextValue().get(), actionGroup, context, false, callback, -1);
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
                    manager.fireBeforeActionPerformed(action, context, finalEvent);
                    manager.performActionDumbAware(action, finalEvent);
                    if (callback != null) {
                        callback.run();
                    }
                    manager.fireAfterActionPerformed(action, context, finalEvent);
                }
            }, uiAccess);
        });
    }

    @Override
    protected boolean requiresProject() {
        return false;
    }
}