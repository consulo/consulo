// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.daemon.impl.analysis;

import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.language.editor.localize.DaemonLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.style.StyleManager;
import jakarta.annotation.Nonnull;

public class DaemonTooltipsUtil {
  @Nonnull
  public static String getWrappedTooltip(String message, String shortName, String shortcutText, boolean showToolDescription) {
    String link = "";
    if (showToolDescription) {
      //noinspection HardCodedStringLiteral
      link = LocalizeValue.localizeTODO(
        " <a href=\"#inspection/" + shortName + "\"" +
          (StyleManager.get().getCurrentStyle().isDark() ? " color=\"7AB4C9\" " : "") +
          ">" +
          DaemonLocalize.inspectionExtendedDescription() +
          "</a> " +
          shortcutText
      ).get();
    }
    return XmlStringUtil.wrapInHtml((message.startsWith("<html>") ? XmlStringUtil.stripHtml(message) : XmlStringUtil.escapeString(message)) + link);
  }
}
