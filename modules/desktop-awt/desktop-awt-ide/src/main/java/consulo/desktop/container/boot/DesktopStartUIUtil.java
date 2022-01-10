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
package consulo.desktop.container.boot;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.SystemProperties;
import com.intellij.util.ui.JBHtmlEditorKit;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.accessibility.ScreenReader;
import consulo.logging.Logger;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2019-10-15
 */
public class DesktopStartUIUtil {
  public static void hackAWT() {
    blockATKWrapper();

    UIUtil.configureHtmlKitStylesheet(() -> JBHtmlEditorKit.createStyleSheet(false));
  }

  public static void initSystemFontData() {
    JBUIScale.getSystemFontData();
  }

  public static void initDefaultLAF() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception ignored) {
    }
  }

  private static void blockATKWrapper() {
    /*
     * The method should be called before java.awt.Toolkit.initAssistiveTechnologies()
     * which is called from Toolkit.getDefaultToolkit().
     */
    if (!SystemInfo.isLinux || !SystemProperties.getBooleanProperty("linux.jdk.accessibility.atkwrapper.block", true)) return;

    if (ScreenReader.isEnabled(ScreenReader.ATK_WRAPPER)) {
      // Replace AtkWrapper with a dummy Object. It'll be instantiated & GC'ed right away, a NOP.
      System.setProperty("javax.accessibility.assistive_technologies", "java.lang.Object");
      getLogger().info(ScreenReader.ATK_WRAPPER + " is blocked, see IDEA-149219");
    }
  }

  private static Logger getLogger() {
    return Logger.getInstance(DesktopStartUIUtil.class);
  }
}

