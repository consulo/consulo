/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.util;

import consulo.application.Application;
import consulo.application.CommonBundle;
import consulo.application.internal.ApplicationInfo;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.keymap.impl.DefaultKeymap;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.awt.JBHtmlEditorKit;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.style.StyleManager;
import consulo.util.io.ResourceUtil;
import consulo.util.lang.Pair;
import consulo.webBrowser.BrowserUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

/**
 * @author dsl
 * @author Konstantin Bulenkov
 */
public class TipUIUtil {
    private static final Logger LOG = Logger.getInstance(TipUIUtil.class);
    private static final String SHORTCUT_ENTITY = "&shortcut:";

    private TipUIUtil() {
    }

    @Nonnull
    public static String getPoweredByTextByLocalize(@Nonnull Pair<LocalizeValue, PluginDescriptor> tipInfo) {
        PluginDescriptor descriptor = tipInfo.getSecond();
        return !PluginIds.isPlatformPlugin(descriptor.getPluginId()) ? descriptor.getName() : "";
    }

    public static void openTipInBrowserByLocalize(@Nonnull Pair<LocalizeValue, PluginDescriptor> tipInfo, JEditorPane browser) {
        try {
            StringBuilder text = new StringBuilder(tipInfo.getFirst().get());
            updateShortcuts(text);
            String replaced = text.toString().replace("&productName;", Application.get().getName().get());
            String major = ApplicationInfo.getInstance().getMajorVersion();
            replaced = replaced.replace("&majorVersion;", major);
            String minor = ApplicationInfo.getInstance().getMinorVersion();
            replaced = replaced.replace("&minorVersion;", minor);
            replaced = replaced.replace("&majorMinorVersion;", major + ("0".equals(minor) ? "" : ("." + minor)));
            replaced = replaced.replace("&settingsPath;", CommonBundle.settingsActionPath());
            replaced = replaced.replaceFirst("<link rel=\"stylesheet\".*tips\\.css\">", ""); // don't reload the styles
            if (browser.getUI() == null) {
                browser.updateUI();
                boolean succeed = browser.getUI() != null;
                String message = "reinit JEditorPane.ui: " + (succeed ? "OK" : "FAIL") + ", themeId=" + StyleManager.get().getCurrentStyle().getId();
                if (succeed) {
                    LOG.warn(message);
                }
                else {
                    LOG.error(message);
                }
            }
            browser.read(new StringReader(replaced), null);
        }
        catch (IOException e) {
            LOG.error(e);
        }
    }

    private static void updateShortcuts(StringBuilder text) {
        int lastIndex = 0;
        while (true) {
            lastIndex = text.indexOf(SHORTCUT_ENTITY, lastIndex);
            if (lastIndex < 0) {
                return;
            }
            final int actionIdStart = lastIndex + SHORTCUT_ENTITY.length();
            int actionIdEnd = text.indexOf(";", actionIdStart);
            if (actionIdEnd < 0) {
                return;
            }
            final String actionId = text.substring(actionIdStart, actionIdEnd);
            String shortcutText = getShortcutText(actionId, KeymapManager.getInstance().getActiveKeymap());
            if (shortcutText == null) {
                Keymap defKeymap = KeymapManager.getInstance().getKeymap(DefaultKeymap.getInstance().getDefaultKeymapName());
                if (defKeymap != null) {
                    shortcutText = getShortcutText(actionId, defKeymap);
                    if (shortcutText != null) {
                        shortcutText += " in default keymap";
                    }
                }
            }
            if (shortcutText == null) {
                shortcutText = "<no shortcut for action " + actionId + ">";
            }
            text.replace(lastIndex, actionIdEnd + 1, shortcutText);
            lastIndex += shortcutText.length();
        }
    }

    @Nullable
    private static String getShortcutText(String actionId, Keymap keymap) {
        for (final Shortcut shortcut : keymap.getShortcuts(actionId)) {
            if (shortcut instanceof KeyboardShortcut) {
                return KeymapUtil.getShortcutText(shortcut);
            }
        }
        return null;
    }

    @Nonnull
    public static JEditorPane createTipBrowser() {
        JEditorPane browser = new JEditorPane();
        browser.setEditable(false);
        browser.setBackground(UIUtil.getTextFieldBackground());
        browser.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse(e.getURL());
            }
        });
        URL resource = ResourceUtil.getResource(TipUIUtil.class, "/tips/css/", StyleManager.get().getCurrentStyle().isDark() ? "tips_darcula.css" : "tips.css");
        HTMLEditorKit kit = JBHtmlEditorKit.create(false);
        kit.getStyleSheet().addStyleSheet(UIUtil.loadStyleSheet(resource));
        browser.setEditorKit(kit);
        return browser;
    }
}