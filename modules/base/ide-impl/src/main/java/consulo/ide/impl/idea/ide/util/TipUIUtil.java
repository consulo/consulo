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
import consulo.ide.IdeBundle;
import consulo.webBrowser.BrowserUtil;
import consulo.ide.impl.idea.ide.ui.LafManager;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.keymap.impl.DefaultKeymap;
import consulo.ide.impl.util.URLDictionatyLoader;
import consulo.logging.Logger;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.IdeFrameUtil;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.awt.JBHtmlEditorKit;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.style.StyleManager;
import consulo.util.io.ResourceUtil;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.image.BufferedImage;
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
  public static String getPoweredByText(@Nonnull Pair<String, PluginDescriptor> tipInfo) {
    PluginDescriptor descriptor = tipInfo.getSecond();
    return !PluginIds.isPlatformPlugin(descriptor.getPluginId()) ? descriptor.getName() : "";
  }

  public static void openTipInBrowser(@Nullable Pair<String, PluginDescriptor> tipInfo, JEditorPane browser) {
    if (tipInfo == null) return;
    try {
      URL url = null;
      try {
        url = tipInfo.getSecond().getPluginClassLoader().getResource(tipInfo.getFirst());
      }
      catch (Exception e) {
        LOG.warn(e);
      }

      if (url == null) {
        setCantReadText(browser, tipInfo);
        return;
      }

      StringBuilder text = new StringBuilder(ResourceUtil.loadText(url));
      updateShortcuts(text);
      updateImages(text, tipInfo.getSecond().getPluginClassLoader(), browser);
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
        String message = "reinit JEditorPane.ui: " + (succeed ? "OK" : "FAIL") + ", laf=" + LafManager.getInstance().getCurrentLookAndFeel();
        if (succeed) {
          LOG.warn(message);
        }
        else {
          LOG.error(message);
        }
      }
      browser.read(new StringReader(replaced), url);
    }
    catch (IOException e) {
      setCantReadText(browser, tipInfo);
    }
  }

  private static void setCantReadText(JEditorPane browser, Pair<String, PluginDescriptor> tipInfo) {
    try {
      String plugin = getPoweredByText(tipInfo);
      String product = Application.get().getName().get();
      if (!plugin.isEmpty()) {
        product += " and " + plugin + " plugin";
      }
      String message = IdeBundle.message("error.unable.to.read.tip.of.the.day", tipInfo.getFirst(), product);
      browser.read(new StringReader(message), null);
    }
    catch (IOException ignored) {
    }
  }

  private static void updateImages(StringBuilder text, ClassLoader tipLoader, JEditorPane browser) {
    final boolean dark = StyleManager.get().getCurrentStyle().isDark();

    IdeFrame af = IdeFrameUtil.findActiveRootIdeFrame();
    Component comp = af != null ? TargetAWT.to(af.getWindow()) : browser;
    int index = text.indexOf("<img", 0);
    while (index != -1) {
      final int end = text.indexOf(">", index + 1);
      if (end == -1) return;
      final String img = text.substring(index, end + 1).replace('\r', ' ').replace('\n', ' ');
      final int srcIndex = img.indexOf("src=");
      final int endIndex = img.indexOf(".png", srcIndex);
      if (endIndex != -1) {
        String path = img.substring(srcIndex + 5, endIndex);
        if (!path.endsWith("_dark") && !path.endsWith("@2x")) {
          boolean hidpi = JBUI.isPixHiDPI(comp);
          path += (hidpi ? "@2x" : "") + (dark ? "_dark" : "") + ".png";
          URL url = ResourceUtil.getResource(tipLoader, "/tips/", path);
          if (url != null) {
            String newImgTag = "<img src=\"" + path + "\" ";
            try {
              BufferedImage image = ImageIO.read(url.openStream());
              int w = image.getWidth();
              int h = image.getHeight();
              if (UIUtil.isJreHiDPI(comp)) {
                // compensate JRE scale
                float sysScale = JBUI.sysScale(comp);
                w = (int)(w / sysScale);
                h = (int)(h / sysScale);
              }
              else {
                // compensate image scale
                float imgScale = hidpi ? 2f : 1f;
                w = (int)(w / imgScale);
                h = (int)(h / imgScale);
              }
              // fit the user scale
              w = (int)(JBUI.scale((float)w));
              h = (int)(JBUI.scale((float)h));

              newImgTag += "width=\"" + w + "\" height=\"" + h + "\"";
            }
            catch (Exception ignore) {
              newImgTag += "width=\"400\" height=\"200\"";
            }
            newImgTag += "/>";
            text.replace(index, end + 1, newImgTag);
          }
        }
      }
      index = text.indexOf("<img", index + 1);
    }
  }

  private static void updateShortcuts(StringBuilder text) {
    int lastIndex = 0;
    while (true) {
      lastIndex = text.indexOf(SHORTCUT_ENTITY, lastIndex);
      if (lastIndex < 0) return;
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
    JEditorPane browser = new JEditorPane() {
      @Override
      public void setDocument(Document document) {
        super.setDocument(document);
        document.putProperty("imageCache", new URLDictionatyLoader());
      }
    };
    browser.setEditable(false);
    browser.setBackground(UIUtil.getTextFieldBackground());
    browser.addHyperlinkListener(e -> {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        BrowserUtil.browse(e.getURL());
      }
    });
    URL resource = ResourceUtil.getResource(TipUIUtil.class, "/tips/css/", UIUtil.isUnderDarcula() ? "tips_darcula.css" : "tips.css");
    HTMLEditorKit kit = JBHtmlEditorKit.create(false);
    kit.getStyleSheet().addStyleSheet(UIUtil.loadStyleSheet(resource));
    browser.setEditorKit(kit);
    return browser;
  }
}