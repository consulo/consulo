// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.ui.UISettings;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.actions.ApplyIntentionAction;
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
import consulo.searchEverywhere.SearchEverywhereItemPresentation;
import consulo.ui.TextAttribute;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.style.ComponentColors;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static consulo.ide.impl.idea.openapi.keymap.KeymapUtil.getFirstKeyboardShortcutText;
import static consulo.ide.impl.idea.openapi.keymap.KeymapUtil.getPreferredShortcutText;
import static consulo.ui.ex.keymap.util.KeymapUtil.getActiveKeymapShortcuts;

public class ActionSearchEverywhereContributor implements SearchEverywhereContributor<GotoActionModel.MatchedValue> {
    private static final Logger LOG = Logger.getInstance(ActionSearchEverywhereContributor.class);

    private final Project myProject;
    private final Component myContextComponent;
    private final @Nullable DataContext myDataContext;
    private final GotoActionModel myModel;
    private final GotoActionItemProvider myProvider;
    private boolean myDisabledActions;

    public ActionSearchEverywhereContributor(Project project, Component contextComponent, Editor editor) {
        this(project, contextComponent, editor, null);
    }

    public ActionSearchEverywhereContributor(
        Project project,
        Component contextComponent,
        Editor editor,
        @Nullable DataContext dataContext
    ) {
        myProject = project;
        myContextComponent = contextComponent;
        myDataContext = dataContext;
        myModel = new GotoActionModel(project, contextComponent, editor);
        myModel.setDataContext(dataContext);
        myProvider = new GotoActionItemProvider(myModel);
    }

   
    @Override
    public String getGroupName() {
        return "Actions";
    }

   
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
        String pattern,
        ProgressIndicator progressIndicator,
        Predicate<? super GotoActionModel.MatchedValue> predicate
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

   
    @Override
    public List<AnAction> getActions(Runnable onChanged) {
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

   
    @Override
    public ListCellRenderer<? super GotoActionModel.MatchedValue> getElementsRenderer() {
        return new GotoActionModel.GotoActionListCellRenderer(myModel::getGroupName, true);
    }

    @Override
    public SearchEverywhereItemPresentation getItemPresentation(GotoActionModel.MatchedValue item, @Nullable MinusculeMatcher matcher) {
        boolean showIcon = UISettings.getInstance().getShowIconsInMenus();

        Object value = item.value;
        if (value instanceof GotoActionModel.ActionWrapper actionWrapper) {
            AnAction action = actionWrapper.getAction();
            boolean toggle = action instanceof ToggleAction;
            String groupName = action instanceof ApplyIntentionAction ? null : actionWrapper.getGroupName();
            Presentation presentation = actionWrapper.getPresentation();
            boolean disabled = !presentation.isEnabledAndVisible();

            SearchEverywhereItemPresentation.Builder builder = SearchEverywhereItemPresentation.builder();
            if (showIcon) {
                Image icon = presentation.getIcon();
                if (icon == null) {
                    builder.icon(Image.empty(Image.DEFAULT_ICON_SIZE));
                }
                else {
                    builder.icon(disabled ? ImageEffects.grayed(icon) : icon);
                }
            }

            String name = GotoActionModel.GotoActionListCellRenderer.getName(presentation.getText(), groupName, toggle);
            builder.appendMatched(name, matcher, disabled ? TextAttribute.GRAYED : TextAttribute.REGULAR);

            Shortcut[] shortcuts = getActiveKeymapShortcuts(ActionManager.getInstance().getId(action)).getShortcuts();
            String shortcutText = getPreferredShortcutText(shortcuts);
            if (StringUtil.isNotEmpty(shortcutText)) {
                builder.append(" " + shortcutText, new TextAttribute(TextAttribute.STYLE_BOLD, ComponentColors.DISABLED_TEXT));
            }

            if (toggle) {
                builder.switchState(Toggleable.isSelected(presentation));
            }
            else if (groupName != null) {
                builder.suffix(LocalizeValue.of(groupName), null);
            }

            return builder.tooltip(presentation.getDescription()).build();
        }

        if (value instanceof OptionDescription optionDescription) {
            String hit = GotoActionModel.GotoActionListCellRenderer.calcHit(optionDescription);

            SearchEverywhereItemPresentation.Builder builder = SearchEverywhereItemPresentation.builder();
            if (showIcon) {
                builder.icon(Image.empty(Image.DEFAULT_ICON_SIZE));
            }
            builder.appendMatched(hit, matcher, TextAttribute.REGULAR);

            if (optionDescription instanceof BooleanOptionDescription booleanOptionDescription) {
                builder.switchState(booleanOptionDescription.isOptionEnabled());
            }
            else {
                String settingsName = myModel.getGroupName(optionDescription);
                if (StringUtil.isNotEmpty(settingsName)) {
                    builder.suffix(LocalizeValue.of(settingsName), null);
                }
            }
            return builder.build();
        }

        return SearchEverywhereItemPresentation.of(String.valueOf(value));
    }

    @Override
    public boolean showInFindResults() {
        return false;
    }

   
    @Override
    public String getSearchProviderId() {
        return ActionSearchEverywhereContributor.class.getSimpleName();
    }

    @Override
    public Object getDataForItem(GotoActionModel.MatchedValue element, Key dataId) {
        if (SetShortcutAction.SELECTED_ACTION == dataId) {
            return getAction(element);
        }

        if (SearchEverywhereDataKeys.ITEM_STRING_DESCRIPTION == dataId) {
            AnAction action = getAction(element);
            if (action != null) {
                LocalizeValue description = action.getTemplatePresentation().getDescription();
                if (UISettings.getInstance().getShowInplaceCommentsInternal()) {
                    String presentableId =
                        StringUtil.notNullize(ActionManager.getInstance().getId(action), "class: " + action.getClass().getName());
                    return String.format("[%s] %s", presentableId, description);
                }
                return description;
            }
        }

        return null;
    }

    @Override
    @RequiredUIAccess
    public boolean processSelectedItem(GotoActionModel.MatchedValue item, int modifiers, String text) {
        if (modifiers == InputEvent.ALT_MASK && myContextComponent != null) {
            showAssignShortcutDialog(item);
            return true;
        }

        Object selected = item.value;

        if (selected instanceof BooleanOptionDescription option) {
            option.setOptionState(!option.isOptionEnabled());
            return false;
        }

        DataContext dataContext = myDataContext;
        if (myContextComponent == null && dataContext != null) {
            GotoActionAction.openOptionOrPerformAction(selected, text, myProject, dataContext);
        }
        else {
            GotoActionAction.openOptionOrPerformAction(selected, text, myProject, myContextComponent);
        }
        boolean inplaceChange = selected instanceof GotoActionModel.ActionWrapper actionWrapper
            && actionWrapper.getAction() instanceof ToggleAction;
        return !inplaceChange;
    }

    private static AnAction getAction(GotoActionModel.@Nullable MatchedValue element) {
        Object value = element.value;
        if (value instanceof GotoActionModel.ActionWrapper actionWrapper) {
            value = actionWrapper.getAction();
        }
        return value instanceof AnAction action ? action : null;
    }

    private void showAssignShortcutDialog(GotoActionModel.MatchedValue value) {
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
