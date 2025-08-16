// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.ui.UISettings;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.ide.actions.GotoActionAction;
import consulo.ide.impl.idea.ide.actions.SetShortcutAction;
import consulo.ui.ex.action.BooleanOptionDescription;
import consulo.ide.impl.idea.ide.util.gotoByName.GotoActionItemProvider;
import consulo.ide.impl.idea.ide.util.gotoByName.GotoActionModel;
import consulo.ide.impl.idea.openapi.keymap.impl.ActionShortcutRestrictions;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.KeymapPanel;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.searchEverywhere.CheckBoxSearchEverywhereToggleAction;
import consulo.searchEverywhere.SearchEverywhereContributor;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static consulo.ide.impl.idea.openapi.keymap.KeymapUtil.getActiveKeymapShortcuts;
import static consulo.ide.impl.idea.openapi.keymap.KeymapUtil.getFirstKeyboardShortcutText;

public class ActionSearchEverywhereContributor implements SearchEverywhereContributor<GotoActionModel.MatchedValue> {
    private static final Logger LOG = Logger.getInstance(ActionSearchEverywhereContributor.class);

    private final Project myProject;
    private final Component myContextComponent;
    private final GotoActionModel myModel;
    private final GotoActionItemProvider myProvider;
    private boolean myDisabledActions;

    public ActionSearchEverywhereContributor(Project project, Component contextComponent, Editor editor) {
        myProject = project;
        myContextComponent = contextComponent;
        myModel = new GotoActionModel(project, contextComponent, editor);
        myProvider = new GotoActionItemProvider(myModel);
    }

    @Nonnull
    @Override
    public String getGroupName() {
        return "Actions";
    }

    @Nonnull
    @Override
    public String getAdvertisement() {
        ShortcutSet altEnterShortcutSet = getActiveKeymapShortcuts(IdeActions.ACTION_SHOW_INTENTION_ACTIONS);
        String altEnter = getFirstKeyboardShortcutText(altEnterShortcutSet);
        return "Press " + altEnter + " to assign a shortcut";
    }

    public LocalizeValue includeNonProjectItemsText() {
        return IdeLocalize.checkboxDisabledIncluded();
    }

    @Override
    public int getSortWeight() {
        return 400;
    }

    @Override
    public boolean isShownInSeparateTab() {
        return true;
    }

    @Override
    public void fetchElements(
        @Nonnull String pattern,
        @Nonnull ProgressIndicator progressIndicator,
        @Nonnull Predicate<? super GotoActionModel.MatchedValue> predicate
    ) {
        if (StringUtil.isEmptyOrSpaces(pattern)) {
            return;
        }

        myProvider.filterElements(
            pattern,
            element -> {
                if (progressIndicator.isCanceled()) {
                    return false;
                }

                if (!myDisabledActions
                    && element.value instanceof GotoActionModel.ActionWrapper actionWrapper
                    && !actionWrapper.isAvailable()) {
                    return true;
                }

                if (element == null) {
                    LOG.error("Null action has been returned from model");
                    return true;
                }

                return predicate.test(element);
            }
        );
    }

    @Nonnull
    @Override
    public List<AnAction> getActions(@Nonnull Runnable onChanged) {
        return Collections.singletonList(new CheckBoxSearchEverywhereToggleAction(includeNonProjectItemsText()) {
            @Override
            public boolean isEverywhere() {
                return myDisabledActions;
            }

            @Override
            public void setEverywhere(boolean state) {
                myDisabledActions = state;
                onChanged.run();
            }
        });
    }

    @Nonnull
    @Override
    public ListCellRenderer<? super GotoActionModel.MatchedValue> getElementsRenderer() {
        return new GotoActionModel.GotoActionListCellRenderer(myModel::getGroupName, true);
    }

    @Override
    public boolean showInFindResults() {
        return false;
    }

    @Nonnull
    @Override
    public String getSearchProviderId() {
        return ActionSearchEverywhereContributor.class.getSimpleName();
    }

    @Override
    public Object getDataForItem(@Nonnull GotoActionModel.MatchedValue element, @Nonnull Key dataId) {
        if (SetShortcutAction.SELECTED_ACTION == dataId) {
            return getAction(element);
        }

        if (SearchEverywhereDataKeys.ITEM_STRING_DESCRIPTION == dataId) {
            AnAction action = getAction(element);
            if (action != null) {
                String description = action.getTemplatePresentation().getDescription();
                if (UISettings.getInstance().getShowInplaceCommentsInternal()) {
                    String presentableId =
                        StringUtil.notNullize(ActionManager.getInstance().getId(action), "class: " + action.getClass().getName());
                    return String.format("[%s] %s", presentableId, StringUtil.notNullize(description));
                }
                return description;
            }
        }

        return null;
    }

    @Override
    @RequiredUIAccess
    public boolean processSelectedItem(@Nonnull GotoActionModel.MatchedValue item, int modifiers, @Nonnull String text) {
        if (modifiers == InputEvent.ALT_MASK) {
            showAssignShortcutDialog(item);
            return true;
        }

        Object selected = item.value;

        if (selected instanceof BooleanOptionDescription option) {
            option.setOptionState(!option.isOptionEnabled());
            return false;
        }

        GotoActionAction.openOptionOrPerformAction(selected, text, myProject, myContextComponent);
        boolean inplaceChange = selected instanceof GotoActionModel.ActionWrapper actionWrapper
            && actionWrapper.getAction() instanceof ToggleAction;
        return !inplaceChange;
    }

    @Nullable
    private static AnAction getAction(@Nonnull GotoActionModel.MatchedValue element) {
        Object value = element.value;
        if (value instanceof GotoActionModel.ActionWrapper actionWrapper) {
            value = actionWrapper.getAction();
        }
        return value instanceof AnAction action ? action : null;
    }

    private void showAssignShortcutDialog(@Nonnull GotoActionModel.MatchedValue value) {
        AnAction action = getAction(value);
        if (action == null) {
            return;
        }

        String id = ActionManager.getInstance().getId(action);

        Keymap activeKeymap = Optional.ofNullable(KeymapManager.getInstance()).map(KeymapManager::getActiveKeymap).orElse(null);
        if (activeKeymap == null) {
            return;
        }

        Application.get().invokeLater(() -> {
            Window window = myProject != null
                ? TargetAWT.to(WindowManager.getInstance().suggestParentWindow(myProject))
                : KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
            if (window == null) {
                return;
            }

            KeymapPanel.addKeyboardShortcut(id, ActionShortcutRestrictions.getInstance().getForActionId(id), activeKeymap, window);
        });
    }
}
