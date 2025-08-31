/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change.commited;

import consulo.application.util.DateFormatUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.CachingCommittedChangesProvider;
import consulo.versionControlSystem.change.commited.CommittedChangeListDecorator;
import consulo.versionControlSystem.change.commited.ReceivedChangeList;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.awt.IssueLinkRenderer;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.plaf.TreeUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.Date;
import java.util.List;

public class CommittedChangeListRenderer extends ColoredTreeCellRenderer {
  private final IssueLinkRenderer myRenderer;
  private final List<CommittedChangeListDecorator> myDecorators;
  private final Project myProject;
  private int myDateWidth;
  private int myFontSize;

  public CommittedChangeListRenderer(Project project, List<CommittedChangeListDecorator> decorators) {
    myProject = project;
    myRenderer = new IssueLinkRenderer(project, this);
    myDecorators = decorators;
    myDateWidth = 0;
    myFontSize = -1;
  }

  public static String getDateOfChangeList(Date date) {
    return DateFormatUtil.formatPrettyDateTime(date);
  }

  public static Pair<String, Boolean> getDescriptionOfChangeList(String text) {
    return new Pair<String, Boolean>(text.replaceAll("\n", " // "), text.contains("\n"));
  }

  public static String truncateDescription(String initDescription, FontMetrics fontMetrics, int maxWidth) {
    String description = initDescription;
    int descWidth = fontMetrics.stringWidth(description);
    while(description.length() > 0 && (descWidth > maxWidth)) {
      description = trimLastWord(description);
      descWidth = fontMetrics.stringWidth(description + " ");
    }
    return description;
  }

  public void customizeCellRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
    customize(tree, value, selected, expanded, leaf, row, hasFocus);
  }

  public void customize(JComponent tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
    if (node.getUserObject() instanceof CommittedChangeList) {
      CommittedChangeList changeList = (CommittedChangeList) node.getUserObject();

      renderChangeList(tree, changeList);
    }
    else if (node.getUserObject() != null) {
      append(node.getUserObject().toString(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    }
  }

  @NonNls
  public void renderChangeList(JComponent tree, CommittedChangeList changeList) {
    Container parent = tree.getParent();
    int rowX = getRowX(myTree, 2);
    int availableWidth = parent == null ? 100 : parent.getWidth() - rowX;
    String date = ", " + getDateOfChangeList(changeList.getCommitDate());
    FontMetrics fontMetrics = tree.getFontMetrics(tree.getFont());
    FontMetrics boldMetrics = tree.getFontMetrics(tree.getFont().deriveFont(Font.BOLD));
    FontMetrics italicMetrics = tree.getFontMetrics(tree.getFont().deriveFont(Font.ITALIC));
    if (myDateWidth <= 0 || (fontMetrics.getFont().getSize() != myFontSize)) {
      myDateWidth = Math.max(fontMetrics.stringWidth(", Yesterday 00:00 PM "), fontMetrics.stringWidth(", 00/00/00 00:00 PM "));
      myDateWidth = Math.max(myDateWidth, fontMetrics.stringWidth(getDateOfChangeList(new Date(2000, 11, 31))));
      myFontSize = fontMetrics.getFont().getSize();
    }
    int dateCommitterSize = myDateWidth + boldMetrics.stringWidth(changeList.getCommitterName());

    Pair<String, Boolean> descriptionInfo = getDescriptionOfChangeList(changeList.getName().trim());
    boolean truncated = descriptionInfo.getSecond().booleanValue();
    String description = descriptionInfo.getFirst();

    for (CommittedChangeListDecorator decorator : myDecorators) {
      Image icon = decorator.decorate(changeList);
      if (icon != null) {
        setIcon(icon);
      }
    }

    int descMaxWidth = availableWidth - dateCommitterSize - 8;
    boolean partial = (changeList instanceof ReceivedChangeList) && ((ReceivedChangeList)changeList).isPartial();
    int descWidth = 0;
    if (partial) {
      String partialMarker = VcsLocalize.committedChangesPartialList().get() + " ";
      append(partialMarker, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
      descWidth += boldMetrics.stringWidth(partialMarker);
    }

    descWidth += fontMetrics.stringWidth(description);

    int numberWidth = 0;
    AbstractVcs vcs = changeList.getVcs();
    if (vcs != null) {
      CachingCommittedChangesProvider provider = vcs.getCachingCommittedChangesProvider();
      if (provider != null && provider.getChangelistTitle() != null) {
        String number = "#" + changeList.getNumber() + "  ";
        numberWidth = fontMetrics.stringWidth(number);
        descWidth += numberWidth;
        append(number, SimpleTextAttributes.GRAY_ATTRIBUTES);
      }
    }

    int branchWidth = 0;
    String branch = changeList.getBranch();
    if (branch != null) {
      branch += " ";
      branchWidth = italicMetrics.stringWidth(branch);
      descWidth += branchWidth;
      append(branch, SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
    }

    if (description.isEmpty() && !truncated) {
      append(VcsLocalize.committedChangesEmptyComment().get(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
      appendFixedTextFragmentWidth(descMaxWidth);
    }
    else if (descMaxWidth < 0) {
      myRenderer.appendTextWithLinks(description);
    }
    else if (descWidth < descMaxWidth && !truncated) {
      myRenderer.appendTextWithLinks(description);
      appendFixedTextFragmentWidth(descMaxWidth);
    }
    else {
      LocalizeValue moreMarker = VcsLocalize.changesBrowserDetailsMarker();
      int moreWidth = fontMetrics.stringWidth(moreMarker.get());
      int remainingWidth = descMaxWidth - moreWidth - numberWidth - branchWidth;
      description = truncateDescription(description, fontMetrics, remainingWidth);
      myRenderer.appendTextWithLinks(description);
      if (!StringUtil.isEmpty(description)) {
        append(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
        append(moreMarker.get(), SimpleTextAttributes.LINK_ATTRIBUTES, new CommittedChangesTreeBrowser.MoreLauncher(myProject, changeList));
      } else if (remainingWidth > 0) {
        append(moreMarker.get(), SimpleTextAttributes.LINK_ATTRIBUTES, new CommittedChangesTreeBrowser.MoreLauncher(myProject, changeList));
      }
      // align value is for the latest added piece
      appendFixedTextFragmentWidth(descMaxWidth);
    }

    append(changeList.getCommitterName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    append(date, SimpleTextAttributes.REGULAR_ATTRIBUTES);
  }

  private static String trimLastWord(String description) {
    int pos = description.trim().lastIndexOf(' ');
    if (pos >= 0) {
      return description.substring(0, pos).trim();
    }
    return description.substring(0, description.length()-1);
  }

  @Nonnull
  public Dimension getPreferredSize() {
    return new Dimension(2000, super.getPreferredSize().height);
  }

  public static int getRowX(JTree tree, int depth) {
    if (tree == null) return 0;
    TreeUI ui = tree.getUI();
    if (ui instanceof BasicTreeUI) {
      BasicTreeUI treeUI = ((BasicTreeUI)ui);
      return (treeUI.getLeftChildIndent() + treeUI.getRightChildIndent()) * depth;
    }

    int leftIndent = UIUtil.getTreeLeftChildIndent();
    int rightIndent = UIUtil.getTreeRightChildIndent();

    return (leftIndent + rightIndent) * depth;
  }
}
