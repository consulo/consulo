/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.application.dumb.DumbAware;
import consulo.application.ui.UIFontManager;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.daemon.impl.tooltips.TooltipActionProvider;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.codeInsight.hint.LineTooltipRenderer;
import consulo.ide.impl.idea.openapi.actionSystem.PopupAction;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.ui.BalloonImpl;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.language.editor.impl.internal.hint.TooltipAction;
import consulo.language.editor.impl.internal.hint.TooltipGroup;
import consulo.language.editor.localize.DaemonLocalize;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.Html;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * from kotlin
 */
public class DaemonTooltipWithActionRenderer extends DaemonTooltipRenderer {
    private class ShowDocAction extends ToggleAction implements HintManagerImpl.ActionToIgnore, DumbAware, PopupAction {
        private final TooltipReloader myTooltipReloader;
        private final boolean myEnabled;

        private ShowDocAction(TooltipReloader tooltipReloader, boolean enabled) {
            super("Show Inspection Description");
            myTooltipReloader = tooltipReloader;
            myEnabled = enabled;

            setShortcutSet(KeymapUtil.getActiveKeymapShortcuts(IdeActions.ACTION_SHOW_ERROR_DESCRIPTION));
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return myCurrentWidth > 0;
        }

        @Override
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            myTooltipReloader.reload(state);
        }

        @RequiredUIAccess
        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setEnabled(myEnabled);
            super.update(e);
        }
    }

    private class ShowActionsAction extends ToggleAction implements HintManagerImpl.ActionToIgnore {
        private final TooltipReloader myTooltipReloader;
        private final boolean myEnabled;

        private ShowActionsAction(TooltipReloader tooltipReloader, boolean enabled) {
            super("Show Quick Fixes");
            myTooltipReloader = tooltipReloader;
            myEnabled = enabled;
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return TooltipActionProvider.isShowActions();
        }

        @Override
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            TooltipActionProvider.setShowActions(state);
            myTooltipReloader.reload(myCurrentWidth > 0);
        }

        @RequiredUIAccess
        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setEnabled(myEnabled);
            super.update(e);
        }
    }

    private static class WrapperActionGroup extends ActionGroup implements HintManagerImpl.ActionToIgnore, DumbAware {
        private final AnAction[] myActions;

        private WrapperActionGroup(List<? extends AnAction> actions) {
            myActions = new AnAction[]{new SettingsActionGroup(actions)};
        }

        @Nonnull
        @Override
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
            return myActions;
        }
    }

    private static class SettingsActionGroup extends DefaultActionGroup implements HintManagerImpl.ActionToIgnore, DumbAware {
        private SettingsActionGroup(@Nonnull List<? extends AnAction> actions) {
            super(actions);

            setPopup(true);
        }

        @Nullable
        @Override
        protected Image getTemplateIcon() {
            return PlatformIconGroup.actionsMorevertical();
        }

        @Override
        public boolean showBelowArrow() {
            return false;
        }
    }

    private static final CustomShortcutSet runActionCustomShortcutSet = new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));

    private final TooltipAction tooltipAction;

    public DaemonTooltipWithActionRenderer(String text, TooltipAction tooltipAction, int width, Object[] comparableObjects) {
        super(text, width, comparableObjects);
        this.tooltipAction = tooltipAction;
    }

    @Nonnull
    @Override
    protected String dressDescription(@Nonnull Editor editor, @Nonnull String tooltipText, boolean expand) {
        if (!LineTooltipRenderer.isActiveHtml(myText) || expand) {
            return super.dressDescription(editor, tooltipText, expand);
        }

        List<String> problems = getProblems(tooltipText);
        StringBuilder text = new StringBuilder();

        StringUtil.join(problems, (param) -> {
            String ref = getLinkRef(param);

            if (ref != null) {
                return getHtmlForProblemWithLink(param);
            }
            else {
                return UIUtil.getHtmlBody(new Html(param).setKeepFont(true));
            }
        }, UIUtil.BORDER_LINE, text);

        return text.toString();
    }

    @Nonnull
    @Override
    protected String getHtmlForProblemWithLink(@Nonnull String problem) {
        //remove "more... (keymap)" info

        Html html = new Html(problem).setKeepFont(true);
        String extendMessage = DaemonLocalize.inspectionExtendedDescription().get();
        String textToProcess = UIUtil.getHtmlBody(html);
        int indexOfMore = textToProcess.indexOf(extendMessage);
        if (indexOfMore < 0) {
            return textToProcess;
        }
        int keymapStartIndex = textToProcess.indexOf("(", indexOfMore);

        if (keymapStartIndex > 0) {
            int keymapEndIndex = textToProcess.indexOf(")", keymapStartIndex);

            if (keymapEndIndex > 0) {
                textToProcess = textToProcess.substring(0, keymapStartIndex) + textToProcess.substring(keymapEndIndex + 1, textToProcess.length());
            }
        }

        return textToProcess.replace(extendMessage, "");
    }

    @Override
    public LightweightHintImpl createHint(@Nonnull Editor editor,
                                          @Nonnull Point p,
                                          boolean alignToRight,
                                          @Nonnull TooltipGroup group,
                                          @Nonnull HintHint hintHint,
                                          boolean newLayout,
                                          boolean highlightActions,
                                          boolean limitWidthToScreen,
                                          @Nullable TooltipReloader tooltipReloader) {
        return super.createHint(editor, p, alignToRight, group, hintHint, newLayout, highlightActions || !(TooltipActionProvider.isShowActions() && tooltipAction != null && hintHint.isAwtTooltip()),
            limitWidthToScreen, tooltipReloader);
    }

    @Override
    protected boolean isContentAction(String dressedText) {
        return super.isContentAction(dressedText) || tooltipAction != null;
    }

    @Nonnull
    @Override
    public LineTooltipRenderer createRenderer(@Nullable String text, int width) {
        return new DaemonTooltipWithActionRenderer(text, tooltipAction, width, getEqualityObjects());
    }

    @Override
    protected void fillPanel(@Nonnull Editor editor,
                             @Nonnull JPanel grid,
                             @Nonnull LightweightHintImpl hint,
                             @Nonnull HintHint hintHint,
                             @Nonnull List<? super AnAction> actions,
                             @Nonnull TooltipReloader tooltipReloader,
                             boolean newLayout,
                             boolean highlightActions) {
        super.fillPanel(editor, grid, hint, hintHint, actions, tooltipReloader, newLayout, highlightActions);

        boolean hasMore = LineTooltipRenderer.isActiveHtml(myText);

        if (tooltipAction == null && !hasMore) {
            return;
        }

        JComponent settingsComponent = createSettingsComponent(hintHint, tooltipReloader, hasMore);

        GridBagConstraints settingsConstraints =
            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, JBUI.insets(newLayout ? 7 : 4, 7, newLayout ? 0 : 4, newLayout ? 2 : 4), 0, 0);

        grid.add(settingsComponent, settingsConstraints);

        if (TooltipActionProvider.isShowActions()) {
            addActionsRow(hintHint, hint, editor, actions, grid, newLayout, highlightActions);
        }
    }

    private void addActionsRow(HintHint hintHint, LightweightHintImpl hint, Editor editor, List<? super AnAction> actions, JComponent grid, boolean newLayout, boolean highlightActions) {
        if (tooltipAction == null || !hintHint.isAwtTooltip()) {
            return;
        }


        JPanel buttons = new JPanel(new GridBagLayout());
        JPanel wrapper = createActionPanelWithBackground(highlightActions);
        wrapper.add(buttons, BorderLayout.WEST);

        buttons.setBorder(JBUI.Borders.empty());
        buttons.setOpaque(false);

        Consumer<InputEvent> runFixAction = event -> {
            hint.hide();
            tooltipAction.execute(editor, event);
        };

        String shortcutRunActionText = KeymapUtil.getShortcutsText(runActionCustomShortcutSet.getShortcuts());
        String shortcutShowAllActionsText = getKeymap(IdeActions.ACTION_SHOW_INTENTION_ACTIONS);

        GridBag gridBag = new GridBag().fillCellHorizontally().anchor(GridBagConstraints.WEST);

        int topInset = 5;
        int bottomInset = newLayout ? (highlightActions ? 4 : 10) : 5;

        buttons.add(createActionLabel(tooltipAction.getText(), runFixAction, hintHint.getTextBackground()), gridBag.next().insets(topInset, newLayout ? 10 : 8, bottomInset, 4));
        buttons.add(createKeymapHint(shortcutRunActionText), gridBag.next().insets(newLayout ? topInset : 0, 4, newLayout ? bottomInset : 0, 12));

        Consumer<InputEvent> showAllFixes = inputEvent -> {
            hint.hide();
            tooltipAction.showAllActions(editor);
        };


        buttons.add(createActionLabel("More actions...", showAllFixes, hintHint.getTextBackground()), gridBag.next().insets(topInset, 12, bottomInset, 4));
        buttons.add(createKeymapHint(shortcutShowAllActionsText), gridBag.next().fillCellHorizontally().insets(newLayout ? topInset : 0, 4, newLayout ? bottomInset : 0, 20));

        actions.add(new AnAction() {
            {
                registerCustomShortcutSet(runActionCustomShortcutSet, editor.getContentComponent());
            }

            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                runFixAction.accept(e.getInputEvent());
            }
        });

        actions.add(new AnAction() {
            {
                registerCustomShortcutSet(KeymapUtil.getActiveKeymapShortcuts(IdeActions.ACTION_SHOW_INTENTION_ACTIONS), editor.getContentComponent());
            }

            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                showAllFixes.accept(e.getInputEvent());
            }
        });

        GridBagConstraints buttonsConstraints = new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.insetsTop(0), 0, 0);

        grid.add(wrapper, buttonsConstraints);
    }

    private String getKeymap(String key) {
        KeymapManager keymapManager = KeymapManager.getInstance();
        Keymap keymap = keymapManager.getActiveKeymap();

        return KeymapUtil.getShortcutsText(keymap.getShortcuts(key));
    }

    private HyperlinkLabel createActionLabel(String text, Consumer<InputEvent> action, Color background) {
        HyperlinkLabel label = new HyperlinkLabel(text, background) {
            @Override
            public int getTextOffset() {
                return 0;
            }
        };
        label.setOpaque(false);
        label.setBorder(JBUI.Borders.empty());
        label.addHyperlinkListener(e -> action.accept(e.getInputEvent()));

        Font actionFont = getActionFont();
        label.setFont(actionFont);
        return label;
    }

    private JPanel createActionPanelWithBackground(boolean highlight) {
        JPanel wrapper;

        if (highlight) {
            wrapper = new JPanel(new BorderLayout()) {
                @Override
                public void paint(Graphics g) {
                    g.setColor(UIUtil.getToolTipActionBackground());
                    if (JBPopupFactory.getInstance().getParentBalloonFor(this) == null) {
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                    else {
                        Graphics2D graphics2D = (Graphics2D) g;
                        GraphicsConfig cfg = new GraphicsConfig(g);
                        cfg.setAntialiasing(true);

                        Rectangle bounds = getBounds();
                        graphics2D.fill(new RoundRectangle2D.Double(1.0, 0.0, bounds.width - 2.5, (bounds.height / 2), 0.0, 0.0));

                        double arc = BalloonImpl.ARC.get();

                        RoundRectangle2D.Double d = new RoundRectangle2D.Double(1.0, 0.0, bounds.width - 2.5, (bounds.height - 1), arc, arc);

                        graphics2D.fill(d);

                        cfg.restore();
                    }

                    super.paint(g);
                }
            };
        }
        else {
            wrapper = new JPanel(new BorderLayout());
        }

        wrapper.setOpaque(false);
        wrapper.setBorder(JBUI.Borders.empty());
        return wrapper;
    }

    private JComponent createKeymapHint(String shortcutRunAction) {
        JBLabel fixHint = new JBLabel(shortcutRunAction);
        fixHint.setForeground(MorphColor.of(this::getKeymapColor));
        fixHint.setBorder(JBUI.Borders.empty());
        fixHint.setFont(getActionFont());
        return fixHint;
    }

    private Font getActionFont() {
        Font toolTipFont = UIUtil.getToolTipFont();
        if (toolTipFont == null || Platform.current().os().isWindows()) {
            return toolTipFont;
        }

        //if font was changed from default we dont have a good heuristic to customize it
        if (JBUI.Fonts.label() != toolTipFont || UIFontManager.getInstance().isOverrideFont()) {
            return toolTipFont;
        }

        return toolTipFont;
    }

    private Color getKeymapColor() {
        return JBColor.namedColor("ToolTip.Actions.infoForeground", new JBColor(0x99a4ad, 0x919191));
    }

    private JComponent createSettingsComponent(HintHint hintHint, TooltipReloader reloader, boolean hasMore) {
        List<AnAction> actions = new ArrayList<>();
        actions.add(new ShowActionsAction(reloader, tooltipAction != null));
        ShowDocAction docAction = new ShowDocAction(reloader, hasMore);
        actions.add(docAction);

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("DaemonSettingToolbar", new WrapperActionGroup(actions), true);
        toolbar.setTargetComponent(null);
        toolbar.setMiniMode(true);

        JPanel wrapper = new JPanel(new BorderLayout());

        wrapper.add(toolbar.getComponent(), BorderLayout.EAST);

        wrapper.setBorder(JBUI.Borders.empty());

        wrapper.setBackground(hintHint.getTextBackground());

        wrapper.setOpaque(false);

        return wrapper;
    }
}
