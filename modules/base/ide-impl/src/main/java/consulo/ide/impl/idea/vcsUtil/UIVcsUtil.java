/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.vcsUtil;

import consulo.util.lang.StringUtil;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.UIUtil;

import javax.swing.*;
import java.awt.*;

/**
 * @author irengrig
 * @since 2011-07-01
 */
public class UIVcsUtil {
  private UIVcsUtil() {
  }

  public static JPanel errorPanel(final String text, boolean isError) {
    final JLabel label = new JLabel("<html><body>" + escapeXmlAndAddBr(text) + "</body></html>");
    label.setForeground(isError ? SimpleTextAttributes.ERROR_ATTRIBUTES.getFgColor() : UIUtil.getInactiveTextColor());
    final JPanel wrapper = new JPanel(new GridBagLayout());
    wrapper.add(label, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.CENTER, GridBagConstraints.NONE,
                                                         new Insets(1,1,1,1), 0,0));
    return wrapper;
  }

  private static String escapeXmlAndAddBr(String text) {
    String escaped = StringUtil.escapeXml(text);
    escaped = StringUtil.replace(escaped, "\n", "<br/>");
    return escaped;
  }

  public static JPanel infoPanel(final String header, final String text) {
    final JLabel label = new JLabel("<html><body><h4>" + StringUtil.escapeXml(header) +
                                    "</h4>" + escapeXmlAndAddBr(text) + "</body></html>");
    final JPanel wrapper = new JPanel(new GridBagLayout());
    wrapper.add(label, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.CENTER, GridBagConstraints.NONE,
                                                         new Insets(1,1,1,1), 0,0));
    return wrapper;
  }
}
