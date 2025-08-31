/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInspection.actions;

import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.Language;
import consulo.language.file.LanguageFileType;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.speedSearch.SpeedSearchUtil;
import consulo.application.util.matcher.Matcher;
import consulo.application.util.matcher.MatcherHolder;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
@SuppressWarnings({"GtkPreferredJComboBoxRenderer"})
public class InspectionListCellRenderer extends DefaultListCellRenderer implements MatcherHolder {
  private Matcher myMatcher;
  private final SimpleTextAttributes SELECTED;
  private final SimpleTextAttributes PLAIN;

  public InspectionListCellRenderer() {
    SELECTED = new SimpleTextAttributes(UIUtil.getListSelectionBackground(),
                                        UIUtil.getListSelectionForeground(),
                                        JBColor.RED,
                                        SimpleTextAttributes.STYLE_PLAIN);
    PLAIN = new SimpleTextAttributes(UIUtil.getListBackground(),
                                     UIUtil.getListForeground(),
                                     JBColor.RED,
                                     SimpleTextAttributes.STYLE_PLAIN);
  }


  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean sel, boolean focus) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(true);

    Color bg = sel ? UIUtil.getListSelectionBackground() : UIUtil.getListBackground();
    Color fg = sel ? UIUtil.getListSelectionForeground() : UIUtil.getListForeground();
    panel.setBackground(bg);
    panel.setForeground(fg);

    SimpleTextAttributes attr = sel ? SELECTED : PLAIN;
    if (value instanceof InspectionToolWrapper) {
      InspectionToolWrapper toolWrapper = (InspectionToolWrapper)value;
      SimpleColoredComponent c = new SimpleColoredComponent();
      SpeedSearchUtil.appendColoredFragmentForMatcher("  " + toolWrapper.getDisplayName(), c, attr, myMatcher, bg, sel);
      panel.add(c, BorderLayout.WEST);

      SimpleColoredComponent group = new SimpleColoredComponent();
      SpeedSearchUtil.appendColoredFragmentForMatcher(toolWrapper.getJoinedGroupPath() + "  ", group, attr, myMatcher, bg, sel);
      JPanel right = new JPanel(new BorderLayout());
      right.setBackground(bg);
      right.setForeground(fg);
      right.add(group, BorderLayout.CENTER);
      JLabel icon = new JLabel(TargetAWT.to(getIcon(toolWrapper)));
      icon.setBackground(bg);
      icon.setForeground(fg);
      right.add(icon, BorderLayout.EAST);
      panel.add(right, BorderLayout.EAST);
    }
    else {
      // E.g. "..." item
      return super.getListCellRendererComponent(list, value, index, sel, focus);
    }

    return panel;
  }

  @Nonnull
  private static Image getIcon(@Nonnull InspectionToolWrapper tool) {
    Image icon = null;
    Language language = tool.getLanguage();
    if (language != null) {
      LanguageFileType fileType = language.getAssociatedFileType();
      if (fileType != null) {
        icon = fileType.getIcon();
      }
    }
    if (icon == null) {
      icon = UnknownFileType.INSTANCE.getIcon();
    }
    assert icon != null;
    return icon;
  }

  @Override
  public void setPatternMatcher(Matcher matcher) {
    myMatcher = matcher;
  }
}
