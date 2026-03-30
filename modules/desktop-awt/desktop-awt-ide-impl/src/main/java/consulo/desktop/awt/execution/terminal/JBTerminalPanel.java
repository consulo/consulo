/*
 * Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.awt.execution.terminal;

import com.jediterm.terminal.ui.TerminalCopyPasteHandler;
import com.jediterm.terminal.TextStyle;
import com.jediterm.terminal.model.StyleState;
import com.jediterm.terminal.model.TerminalTextBuffer;
import com.jediterm.terminal.ui.TerminalPanel;
import consulo.codeEditor.impl.ComplementaryFontsRegistry;
import consulo.codeEditor.impl.FontInfo;
import consulo.disposer.Disposable;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.ide.GeneralSettings;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.internal.JBHiDPIScaledImage;
import consulo.ui.ex.awt.util.UISettingsUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Consulo terminal panel adapted from JetBrains JBTerminalPanel.
 */
public class JBTerminalPanel extends TerminalPanel implements FocusListener, TerminalSettingsListener, Disposable {
    private static final String[] ACTIONS_TO_SKIP = new String[]{
        "ActivateTerminalToolWindow",
        "ActivateProjectToolWindow",
        "ActivateFavoritesToolWindow",
        "ActivateBookmarksToolWindow",
        "ActivateFindToolWindow",
        "ActivateRunToolWindow",
        "ActivateDebugToolWindow",
        "ActivateProblemsViewToolWindow",
        "ActivateTODOToolWindow",
        "ActivateStructureToolWindow",
        "ActivateHierarchyToolWindow",
        "ActivateServicesToolWindow",
        "ActivateCommitToolWindow",
        "ActivateVersionControlToolWindow",
        "HideActiveWindow",
        "HideAllWindows",

        "NextWindow",
        "PreviousWindow",
        "NextProjectWindow",
        "PreviousProjectWindow",

        "ShowBookmarks",
        "ShowTypeBookmarks",
        "FindInPath",
        "GotoBookmark0",
        "GotoBookmark1",
        "GotoBookmark2",
        "GotoBookmark3",
        "GotoBookmark4",
        "GotoBookmark5",
        "GotoBookmark6",
        "GotoBookmark7",
        "GotoBookmark8",
        "GotoBookmark9",

        "GotoAction",
        "GotoFile",
        "GotoClass",
        "GotoSymbol",

        "Vcs.Push",

        "ShowSettings",
        "RecentFiles",
        "Switcher",

        "ResizeToolWindowLeft",
        "ResizeToolWindowRight",
        "ResizeToolWindowUp",
        "ResizeToolWindowDown",
        "MaximizeToolWindow",

        "TerminalIncreaseFontSize",
        "TerminalDecreaseFontSize",
        "TerminalResetFontSize"
    };

    private final TerminalEventDispatcher myEventDispatcher = new TerminalEventDispatcher();
    private final JBTerminalSystemSettingsProvider mySettingsProvider;

    private List<AnAction> myActionsToSkip;

    public JBTerminalPanel(
        JBTerminalSystemSettingsProvider settingsProvider,
        TerminalTextBuffer backBuffer,
        StyleState styleState
    ) {
        super(settingsProvider, backBuffer, styleState);

        mySettingsProvider = settingsProvider;

        addFocusListener(this);

        mySettingsProvider.addListener(this);
    }

    private boolean skipKeyEvent(KeyEvent e) {
        return skipAction(e, myActionsToSkip);
    }

    private static boolean skipAction(KeyEvent e, List<AnAction> actionsToSkip) {
        if (actionsToSkip != null) {
            KeyboardShortcut eventShortcut = new KeyboardShortcut(KeyStroke.getKeyStrokeForEvent(e), null);
            for (AnAction action : actionsToSkip) {
                for (Shortcut sc : action.getShortcutSet().getShortcuts()) {
                    if (sc.isKeyboard() && sc.startsWith(eventShortcut)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void setupAntialiasing(Graphics graphics) {
        UIUtil.setupComposite((Graphics2D) graphics);
        UISettingsUtil.setupAntialiasing(graphics);
    }

    @Override
    protected TerminalCopyPasteHandler createCopyPasteHandler() {
        return new TerminalCopyPasteHandler() {
            @Override
            public void setContents(String text, boolean useSystemSelectionClipboardIfAvailable) {
                CopyPasteManager.getInstance().setContents(new StringSelection(text));
            }

            @Override
            public String getContents(boolean useSystemSelectionClipboardIfAvailable) {
                return CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);
            }
        };
    }

    @Override
    protected void drawImage(Graphics2D gfx, BufferedImage image, int x, int y, ImageObserver observer) {
        UIUtil.drawImage(gfx, image, x, y, observer);
    }

    @Override
    protected void drawImage(
        Graphics2D g,
        BufferedImage image,
        int dx1,
        int dy1,
        int dx2,
        int dy2,
        int sx1,
        int sy1,
        int sx2,
        int sy2
    ) {
        drawImage(g, image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
    }

    public static void drawImage(
        Graphics g,
        Image image,
        int dx1,
        int dy1,
        int dx2,
        int dy2,
        int sx1,
        int sy1,
        int sx2,
        int sy2,
        ImageObserver observer
    ) {
        if (image instanceof JBHiDPIScaledImage) {
            Graphics2D newG = (Graphics2D) g.create(0, 0, image.getWidth(observer), image.getHeight(observer));
            newG.scale(0.5, 0.5);
            Image img = ((JBHiDPIScaledImage) image).getDelegate();
            if (img == null) {
                img = image;
            }
            newG.drawImage(img, 2 * dx1, 2 * dy1, 2 * dx2, 2 * dy2, sx1 * 2, sy1 * 2, sx2 * 2, sy2 * 2, observer);
            newG.scale(1, 1);
            newG.dispose();
        }
        else {
            g.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
        }
    }

    @Override
    protected BufferedImage createBufferedImage(int width, int height) {
        return UIUtil.createImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public void focusGained(FocusEvent event) {
        if (mySettingsProvider.overrideIdeShortcuts()) {
            myActionsToSkip = setupActionsToSkip();
            myEventDispatcher.register();
        }
        else {
            myActionsToSkip = null;
            myEventDispatcher.unregister();
        }

        if (GeneralSettings.getInstance().isSaveOnFrameDeactivation()) {
            FileDocumentManager.getInstance().saveAllDocuments();
        }
    }

    private static List<AnAction> setupActionsToSkip() {
        List<AnAction> res = new ArrayList<>();
        ActionManager actionManager = ActionManager.getInstance();
        for (String actionId : ACTIONS_TO_SKIP) {
            AnAction action = actionManager.getAction(actionId);
            if (action != null) {
                res.add(action);
            }
        }
        return res;
    }

    @Override
    public void focusLost(FocusEvent event) {
        myActionsToSkip = null;
        myEventDispatcher.unregister();
    }

    @Override
    protected Font getFontToDisplay(char[] text, int start, int end, TextStyle style) {
        int fontStyle = Font.PLAIN;
        if (style.hasOption(TextStyle.Option.BOLD)) {
            fontStyle |= Font.BOLD;
        }
        if (style.hasOption(TextStyle.Option.ITALIC)) {
            fontStyle |= Font.ITALIC;
        }
        FontInfo fontInfo = ComplementaryFontsRegistry.getFontAbleToDisplay(
            text, start, end, fontStyle,
            mySettingsProvider.getFontPreferences(),
            null);
        return fontInfo.getFont().deriveFont(mySettingsProvider.getTerminalFontSize());
    }

    @Override
    public void fontChanged() {
        reinitFontAndResize();
    }

    @Override
    public void dispose() {
        mySettingsProvider.removeListener(this);
    }

    /**
     * Adds "Override IDE shortcuts" terminal feature allowing terminal to process all the key events.
     * Without own event dispatcher, terminal won't receive key events corresponding to IDE action shortcuts.
     */
    private final class TerminalEventDispatcher implements Predicate<AWTEvent> {

        private boolean myRegistered = false;

        @Override
        public boolean test(AWTEvent e) {
            if (e instanceof KeyEvent keyEvent) {
                return dispatchKeyEvent(keyEvent);
            }
            return false;
        }

        private boolean dispatchKeyEvent(KeyEvent e) {
            if (e.getID() == KeyEvent.KEY_PRESSED && !skipKeyEvent(e)) {
                if (!JBTerminalPanel.this.isFocusOwner()) {
                    unregister();
                    return false;
                }
                JBTerminalPanel.this.dispatchEvent(e);
                return true;
            }
            return false;
        }

        void register() {
            if (!myRegistered) {
                IdeEventQueue.getInstance().addDispatcher(this, JBTerminalPanel.this);
                myRegistered = true;
            }
        }

        void unregister() {
            if (myRegistered) {
                IdeEventQueue.getInstance().removeDispatcher(this);
            }
            myRegistered = false;
        }
    }
}
