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
package consulo.ide.impl.presentationAssistant;

import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.action.util.MacKeymapUtil;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.BitUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.JdkConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

/**
 * @author Nikolay Chashnikov (kotlin)
 * @author VISTALL
 * @since 2017-08-21
 */
class ShortcutPresenter implements Disposable {
    public static class ActionData {
        private final String myActionId;
        private final Project myProject;
        @Nonnull
        private final LocalizeValue myActionText;
        private final InputEvent myEvent;

        public ActionData(String actionId, Project project, @Nonnull LocalizeValue actionText, @Nullable InputEvent event) {
            myActionId = actionId;
            myProject = project;
            myActionText = actionText;
            myEvent = event;
        }
    }

    private static final Set<String> MOVING_ACTIONS = Set.of(
        IdeActions.ACTION_EDITOR_MOVE_CARET_LEFT,
        IdeActions.ACTION_EDITOR_MOVE_CARET_RIGHT,
        IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN,
        IdeActions.ACTION_EDITOR_MOVE_CARET_UP,
        IdeActions.ACTION_EDITOR_MOVE_LINE_START,
        IdeActions.ACTION_EDITOR_MOVE_LINE_END,
        IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_UP,
        IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_DOWN,
        IdeActions.ACTION_EDITOR_PREVIOUS_WORD,
        IdeActions.ACTION_EDITOR_NEXT_WORD,
        "EditorScrollUp",
        "EditorScrollDown",
        "EditorTextStart",
        "EditorTextEnd",
        "EditorDownWithSelection",
        "EditorUpWithSelection",
        IdeActions.ACTION_EDITOR_MOVE_CARET_RIGHT_WITH_SELECTION,
        IdeActions.ACTION_EDITOR_MOVE_CARET_LEFT_WITH_SELECTION,
        IdeActions.ACTION_EDITOR_MOVE_LINE_START_WITH_SELECTION,
        IdeActions.ACTION_EDITOR_MOVE_LINE_END_WITH_SELECTION,
        "EditorPageDownWithSelection",
        "EditorPageUpWithSelection"
    );

    private static final Set<String> TYPING_ACTIONS = Set.of(
        IdeActions.ACTION_EDITOR_BACKSPACE,
        IdeActions.ACTION_EDITOR_ENTER,
        IdeActions.ACTION_EDITOR_NEXT_TEMPLATE_VARIABLE
    );

    private static final Set<String> PARENT_GROUP_IDS = Set.of(
        "CodeCompletionGroup",
        "FoldingGroup",
        "GoToMenu",
        "IntroduceActionsGroup"
    );

    private ActionInfoPanel infoPanel;

    private Map<String, LocalizeValue> parentNames = new HashMap<>();

    public ShortcutPresenter() {
        enable();
    }

    public void enable() {
        ActionManager actionManager = ActionManager.getInstance();
        for (String groupId : PARENT_GROUP_IDS) {
            if (actionManager.getAction(groupId) instanceof ActionGroup actionGroup) {
                fillParentNames(actionGroup, actionGroup.getTemplatePresentation().getTextValue());
            }
        }

        actionManager.addAnActionListener(
            new AnActionListener() {
                private ActionData currentAction;

                @Override
                public void beforeActionPerformed(AnAction anAction, DataContext dataContext, AnActionEvent event) {
                    currentAction = null;
                    String id = ActionManager.getInstance().getId(anAction);
                    if (id == null) {
                        return;
                    }
                    if (!MOVING_ACTIONS.contains(id) && !TYPING_ACTIONS.contains(id) && event != null) {
                        Project project = event.getData(Project.KEY);
                        LocalizeValue text = event.getPresentation().getTextValue();
                        currentAction = new ActionData(id, project, text, event.getInputEvent());
                    }
                }

                @Override
                public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
                    ActionData actionData = currentAction;
                    String actionId = ActionManager.getInstance().getId(action);
                    if (actionData != null && actionData.myActionId.equals(actionId)) {
                        showActionInfo(actionData);
                    }
                }

            },
            this
        );
    }

    public void disable() {
        if (infoPanel != null) {
            infoPanel.close();
            infoPanel = null;
        }
        Disposer.dispose(this);
    }

    public void addText(List<Pair<String, Font>> list, String text) {
        list.add(Pair.create(text, null));
    }

    public void showActionInfo(ActionData actionData) {
        String actionId = actionData.myActionId;
        LocalizeValue parentGroupName = parentNames.get(actionId);
        InputEvent event = actionData.myEvent;
        String actionText = (parentGroupName != null ? parentGroupName + " " + MacKeymapUtil.RIGHT + " " : "") +
            actionData.myActionText.map(text -> StringUtil.trimEnd(text, "…"));

        List<Pair<String, Font>> fragments = new ArrayList<>();
        if (actionText.length() > 0) {
            addText(fragments, "<b>" + actionText + "</b>");
        }

        Keymaps.KeymapDescription mainKeymap = PresentationAssistant.getInstance().getConfiguration().mainKeymap;
        List<Pair<String, Font>> shortcutTextFragments = shortcutTextFragments(mainKeymap, actionId, actionText, event);
        if (!shortcutTextFragments.isEmpty()) {
            if (!fragments.isEmpty()) {
                addText(fragments, " via&nbsp;");
            }
            fragments.addAll(shortcutTextFragments);
        }

        Keymaps.KeymapDescription alternativeKeymap = PresentationAssistant.getInstance().getConfiguration().alternativeKeymap;
        if (alternativeKeymap != null) {
            String mainShortcut = shortcutText(mainKeymap.getKeymap().getShortcuts(actionId), mainKeymap.getKind(), event);
            List<Pair<String, Font>> altShortcutTextFragments =
                shortcutTextFragments(alternativeKeymap, actionId, mainShortcut, tryToRemapMouseEvent(event));
            if (!altShortcutTextFragments.isEmpty()) {
                addText(fragments, "&nbsp;(");
                fragments.addAll(altShortcutTextFragments);
                addText(fragments, ")");
            }
        }

        Project realProject = actionData.myProject == null
            ? ArrayUtil.getFirstElement(ProjectManager.getInstance().getOpenProjects())
            : actionData.myProject;
        if (realProject != null && !realProject.isDisposed() && realProject.isOpen()) {
            if (infoPanel == null || !infoPanel.canBeReused()) {
                infoPanel = new ActionInfoPanel(realProject, fragments);
            }
            else {
                infoPanel.updateText(realProject, fragments);
            }
        }
    }

    private static InputEvent tryToRemapMouseEvent(InputEvent e) {
        if (!(e instanceof MouseEvent event)) {
            return e;
        }

        int modifiers = event.getModifiers();
        // just a hack for search correct mouse event for alternative keymap
        // Meta + Click => Ctrl + Click
        if (BitUtil.isSet(modifiers, MouseEvent.META_MASK)) {
            modifiers = BitUtil.clear(modifiers, MouseEvent.META_MASK);
            modifiers = BitUtil.set(modifiers, MouseEvent.CTRL_MASK, true);
        }
        return new MouseEvent(
            event.getComponent(),
            event.getID(),
            event.getWhen(),
            modifiers,
            event.getX(),
            event.getY(),
            event.getClickCount(),
            event.isPopupTrigger(),
            event.getButton()
        );
    }

    private List<Pair<String, Font>> shortcutTextFragments(
        Keymaps.KeymapDescription keymap,
        String actionId,
        String shownShortcut,
        InputEvent type
    ) {
        List<Pair<String, Font>> fragments = new ArrayList<>();
        String shortcutText = shortcutText(keymap.getKeymap().getShortcuts(actionId), keymap.getKind(), type);
        if (StringUtil.isEmpty(shortcutText) || Comparing.equal(shortcutText, shownShortcut)) {
            return fragments;
        }
        if (keymap.getKind() == Keymaps.KeymapKind.WIN || Platform.current().os().isMac()) {
            addText(fragments, shortcutText);
        }
        else if (MacKeyStrokePresentation.macKeyStrokesFont != null && MacKeyStrokePresentation.macKeyStrokesFont.canDisplayUpTo(
            shortcutText) == -1) {
            fragments.add(Pair.create(shortcutText, MacKeyStrokePresentation.macKeyStrokesFont));
        }
        else {
            String altShortcutAsWin = shortcutText(keymap.getKeymap().getShortcuts(actionId), Keymaps.KeymapKind.WIN, type);
            if (!altShortcutAsWin.isEmpty() & !Comparing.equal(shortcutText, altShortcutAsWin)) {
                addText(fragments, altShortcutAsWin);
            }
        }

        String keymapText = keymap.getDisplayText();
        if (!StringUtil.isEmpty(keymapText)) {
            addText(fragments, "&nbsp;" + keymapText);
        }
        return fragments;
    }

    private String shortcutText(Shortcut[] shortcuts, Keymaps.KeymapKind keymapKind, InputEvent inputEvent) {
        if (shortcuts == null || shortcuts.length == 0) {
            return "";
        }

        if (inputEvent instanceof MouseEvent mouseEvent) {
            int button = mouseEvent.getButton();
            int clickCount = mouseEvent.getClickCount();
            int modifiers = inputEvent.getModifiersEx();

            for (Shortcut shortcut : shortcuts) {
                if (shortcut instanceof MouseShortcut mouseShortcut
                    && button == mouseShortcut.getButton()
                    && clickCount == mouseShortcut.getClickCount()
                    && modifiers == mouseShortcut.getModifiers()) {
                    return shortcutText(mouseShortcut, keymapKind);
                }
            }
        }

        for (Shortcut shortcut : shortcuts) {
            if (shortcut instanceof KeyboardShortcut keyboardShortcut) {
                return shortcutText(keyboardShortcut, keymapKind);
            }
        }

        return "";
    }

    @Nonnull
    private String shortcutText(KeyboardShortcut shortcut, Keymaps.KeymapKind kind) {
        List<KeyStroke> list = Arrays.asList(shortcut.getFirstKeyStroke(), shortcut.getSecondKeyStroke());
        return StringUtil.join(
            ContainerUtil.mapNotNull(
                list,
                keyStroke -> keyStroke == null ? null : shortcutText(keyStroke, kind)
            ),
            ", "
        );
    }

    @Nonnull
    private String shortcutText(KeyStroke keyStroke, Keymaps.KeymapKind keymapKind) {
        switch (keymapKind) {
            case MAC:
                return StringUtil.notNullize(MacKeymapUtil.getKeyStrokeText(keyStroke));
            case WIN:
                int modifiers = keyStroke.getModifiers();
                List<String> list = Arrays.asList(
                    modifiers > 0 ? WinKeyStrokePresentation.getWinModifiersText(modifiers) : null,
                    WinKeyStrokePresentation.getWinKeyText(keyStroke.getKeyCode())
                );

                return String.join("+", list.stream().filter(s -> !StringUtil.isEmpty(s)).toArray(String[]::new)).trim();
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Nonnull
    private String shortcutText(MouseShortcut shortcut, Keymaps.KeymapKind keymapKind) {
        String mouseShortcutText = MouseShortcutPresentation.getMouseShortcutText(shortcut);
        if (mouseShortcutText == null) {
            return "";
        }

        int modifiers = mapNewModifiers(shortcut.getModifiers());
        List<String> texts = new ArrayList<>();
        if (modifiers > 0) {
            String winModifiersText = keymapKind == Keymaps.KeymapKind.MAC
                ? MacKeymapUtil.getModifiersText(modifiers)
                : WinKeyStrokePresentation.getWinModifiersText(modifiers);
            if (!StringUtil.isEmpty(winModifiersText)) {
                texts.add(winModifiersText);
            }
        }
        texts.add(mouseShortcutText);
        return String.join("+", texts);
    }

    /**
     * copy&paste from KeymapUtil
     */
    @JdkConstants.InputEventMask
    private static int mapNewModifiers(@JdkConstants.InputEventMask int modifiers) {
        if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
            modifiers |= InputEvent.SHIFT_MASK;
        }
        if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) {
            modifiers |= InputEvent.ALT_MASK;
        }
        if ((modifiers & InputEvent.ALT_GRAPH_DOWN_MASK) != 0) {
            modifiers |= InputEvent.ALT_GRAPH_MASK;
        }
        if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
            modifiers |= InputEvent.CTRL_MASK;
        }
        if ((modifiers & InputEvent.META_DOWN_MASK) != 0) {
            modifiers |= InputEvent.META_MASK;
        }

        return modifiers;
    }

    private void fillParentNames(ActionGroup group, @Nonnull LocalizeValue parentName) {
        ActionManager actionManager = ActionManager.getInstance();
        for (AnAction item : group.getChildren(null)) {
            if (item instanceof ActionGroup actionGroup) {
                if (!actionGroup.isPopup()) {
                    fillParentNames(actionGroup, parentName);
                }
            }
            else {
                String id = actionManager.getId(item);
                if (id != null) {
                    parentNames.put(id, parentName);
                }
            }
        }
    }

    @Override
    public void dispose() {
    }
}
