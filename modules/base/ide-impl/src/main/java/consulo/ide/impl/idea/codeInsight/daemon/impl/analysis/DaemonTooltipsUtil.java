// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.daemon.impl.analysis;

import consulo.language.editor.DaemonBundle;
import consulo.ui.ex.awt.UIUtil;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import jakarta.annotation.Nonnull;

public class DaemonTooltipsUtil {
  @Nonnull
  public static String getWrappedTooltip(String message, String shortName, String shortcutText, boolean showToolDescription) {
    String link = "";
    if (showToolDescription) {
      //noinspection HardCodedStringLiteral
      link = " <a " +
             "href=\"#inspection/" +
             shortName +
             "\"" +
             (UIUtil.isUnderDarcula() ? " color=\"7AB4C9\" " : "") +
             ">" +
             DaemonBundle.message("inspection.extended.description") +
             "</a> " +
             shortcutText;
    }
    return XmlStringUtil.wrapInHtml((message.startsWith("<html>") ? XmlStringUtil.stripHtml(message) : XmlStringUtil.escapeString(message)) + link);
  }
}
