/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.vcs.log.ui.frame;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.changes.issueLinks.IssueLinkHtmlRenderer;
import com.intellij.openapi.vcs.ui.FontUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.*;
import com.intellij.vcs.log.VcsFullCommitDetails;
import com.intellij.vcs.log.VcsRef;
import com.intellij.vcs.log.VcsUser;
import com.intellij.vcs.log.data.LoadingDetails;
import com.intellij.vcs.log.data.VcsLogData;
import com.intellij.vcs.log.ui.VcsLogColorManager;
import com.intellij.vcs.log.ui.render.RectanglePainter;
import com.intellij.vcs.log.util.VcsUserUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.intellij.openapi.vcs.history.VcsHistoryUtil.getCommitDetailsFont;

class CommitPanel extends JBPanel {
  public static final int BOTTOM_BORDER = 2;
  private static final int REFERENCES_BORDER = 12;
  private static final int TOP_BORDER = 4;

  @Nonnull
  private final VcsLogData myLogData;

  @Nonnull
  private final ReferencesPanel myBranchesPanel;
  @Nonnull
  private final ReferencesPanel myTagsPanel;
  @Nonnull
  private final DataPanel myDataPanel;
  @Nonnull
  private final BranchesPanel myContainingBranchesPanel;
  @Nonnull
  private final RootPanel myRootPanel;
  @Nonnull
  private final VcsLogColorManager myColorManager;

  @Nullable private VcsFullCommitDetails myCommit;

  public CommitPanel(@Nonnull VcsLogData logData, @Nonnull VcsLogColorManager colorManager) {
    myLogData = logData;
    myColorManager = colorManager;

    setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, false));
    setOpaque(false);

    myRootPanel = new RootPanel();
    myBranchesPanel = new ReferencesPanel();
    myBranchesPanel.setBorder(JBUI.Borders.empty(REFERENCES_BORDER, 0, 0, 0));
    myTagsPanel = new ReferencesPanel();
    myTagsPanel.setBorder(JBUI.Borders.empty(REFERENCES_BORDER, 0, 0, 0));
    myDataPanel = new DataPanel(myLogData.getProject());
    myContainingBranchesPanel = new BranchesPanel();

    add(myRootPanel);
    add(myDataPanel);
    add(myBranchesPanel);
    add(myTagsPanel);
    add(myContainingBranchesPanel);

    setBorder(getDetailsBorder());
  }

  public void setCommit(@Nonnull VcsFullCommitDetails commitData) {
    if (!Comparing.equal(myCommit, commitData)) {
      if (commitData instanceof LoadingDetails) {
        myDataPanel.setData(null);
        myRootPanel.setRoot("", null);
      }
      else {
        myDataPanel.setData(commitData);
        VirtualFile root = commitData.getRoot();
        if (myColorManager.isMultipleRoots()) {
          myRootPanel.setRoot(root.getName(), VcsLogGraphTable.getRootBackgroundColor(root, myColorManager));
        }
        else {
          myRootPanel.setRoot("", null);
        }
      }
      myCommit = commitData;
    }

    List<String> branches = null;
    if (!(commitData instanceof LoadingDetails)) {
      branches = myLogData.getContainingBranchesGetter().requestContainingBranches(commitData.getRoot(), commitData.getId());
    }
    myContainingBranchesPanel.setBranches(branches);

    myDataPanel.update();
    myContainingBranchesPanel.update();
    revalidate();
  }

  public void setRefs(@Nonnull Collection<VcsRef> refs) {
    List<VcsRef> references = sortRefs(refs);
    myBranchesPanel.setReferences(references.stream().filter(ref -> ref.getType().isBranch()).collect(Collectors.toList()));
    myTagsPanel.setReferences(references.stream().filter(ref -> !ref.getType().isBranch()).collect(Collectors.toList()));
  }

  public void update() {
    myDataPanel.update();
    myRootPanel.update();
    myBranchesPanel.update();
    myTagsPanel.update();
    myContainingBranchesPanel.update();
  }

  public void updateBranches() {
    if (myCommit != null) {
      myContainingBranchesPanel
              .setBranches(myLogData.getContainingBranchesGetter().getContainingBranchesFromCache(myCommit.getRoot(), myCommit.getId()));
    }
    else {
      myContainingBranchesPanel.setBranches(null);
    }
    myContainingBranchesPanel.update();
  }

  @Nonnull
  private List<VcsRef> sortRefs(@Nonnull Collection<VcsRef> refs) {
    VcsRef ref = ContainerUtil.getFirstItem(refs);
    if (ref == null) return ContainerUtil.emptyList();
    return ContainerUtil.sorted(refs, myLogData.getLogProvider(ref.getRoot()).getReferenceManager().getLabelsOrderComparator());
  }

  @Nonnull
  public static JBEmptyBorder getDetailsBorder() {
    return JBUI.Borders.empty();
  }

  @Override
  public Color getBackground() {
    return getCommitDetailsBackground();
  }

  public boolean isExpanded() {
    return myContainingBranchesPanel.isExpanded();
  }

  @Nonnull
  public static Color getCommitDetailsBackground() {
    return UIUtil.getTableBackground();
  }

  @Nonnull
  public static String formatDateTime(long time) {
    return " on " + DateFormatUtil.formatDate(time) + " at " + DateFormatUtil.formatTime(time);
  }

  private static class DataPanel extends HtmlPanel {
    @Nonnull
    private final Project myProject;
    @Nullable private String myMainText;

    DataPanel(@Nonnull Project project) {
      myProject = project;

      DefaultCaret caret = (DefaultCaret)getCaret();
      caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

      setBorder(JBUI.Borders.empty(0, ReferencesPanel.H_GAP, BOTTOM_BORDER, 0));
    }

    @Override
    public void updateUI() {
      super.updateUI();
      update();
    }

    void setData(@Nullable VcsFullCommitDetails commit) {
      if (commit == null) {
        myMainText = null;
      }
      else {
        String hash = commit.getId().toShortString();
        String hashAndAuthor = getHtmlWithFonts(hash + " " + getAuthorText(commit, hash.length() + 1));
        String messageText = getMessageText(commit);
        myMainText = messageText + "<br/><br/>" + hashAndAuthor;
      }
    }

    private void customizeLinksStyle() {
      Document document = getDocument();
      if (document instanceof HTMLDocument) {
        StyleSheet styleSheet = ((HTMLDocument)document).getStyleSheet();
        String linkColor = "#" + ColorUtil.toHex(JBUI.CurrentTheme.Link.linkColor());
        styleSheet.addRule("a { color: " + linkColor + "; text-decoration: none;}");
      }
    }

    @Nonnull
    private static String getHtmlWithFonts(@Nonnull String input) {
      return getHtmlWithFonts(input, getCommitDetailsFont().getStyle());
    }

    @Nonnull
    private static String getHtmlWithFonts(@Nonnull String input, int style) {
      return FontUtil.getHtmlWithFonts(input, style, getCommitDetailsFont());
    }

    void update() {
      if (myMainText == null) {
        setText("");
      }
      else {
        setText("<html><head>" +
                UIUtil.getCssFontDeclaration(getCommitDetailsFont()) +
                "</head><body>" +
                myMainText +
                "</body></html>");
      }
      customizeLinksStyle();
      revalidate();
      repaint();
    }

    @Nonnull
    private String getMessageText(@Nonnull VcsFullCommitDetails commit) {
      String fullMessage = commit.getFullMessage();
      int separator = fullMessage.indexOf("\n\n");
      String subject = separator > 0 ? fullMessage.substring(0, separator) : fullMessage;
      String description = fullMessage.substring(subject.length());
      return "<b>" +
             getHtmlWithFonts(escapeMultipleSpaces(IssueLinkHtmlRenderer.formatTextWithLinks(myProject, subject)), Font.BOLD) +
             "</b>" +
             getHtmlWithFonts(escapeMultipleSpaces(IssueLinkHtmlRenderer.formatTextWithLinks(myProject, description)));
    }

    @Nonnull
    private static String escapeMultipleSpaces(@Nonnull String text) {
      StringBuilder result = new StringBuilder();
      for (int i = 0; i < text.length(); i++) {
        if (text.charAt(i) == ' ') {
          if (i == text.length() - 1 || text.charAt(i + 1) != ' ') {
            result.append(' ');
          }
          else {
            result.append("&nbsp;");
          }
        }
        else {
          result.append(text.charAt(i));
        }
      }
      return result.toString();
    }

    @Nonnull
    private static String getAuthorText(@Nonnull VcsFullCommitDetails commit, int offset) {
      long authorTime = commit.getAuthorTime();
      long commitTime = commit.getCommitTime();

      String authorText = getAuthorName(commit.getAuthor()) + formatDateTime(authorTime);
      if (!VcsUserUtil.isSamePerson(commit.getAuthor(), commit.getCommitter())) {
        String commitTimeText;
        if (authorTime != commitTime) {
          commitTimeText = formatDateTime(commitTime);
        }
        else {
          commitTimeText = "";
        }
        authorText += getCommitterText(commit.getCommitter(), commitTimeText, offset);
      }
      else if (authorTime != commitTime) {
        authorText += getCommitterText(null, formatDateTime(commitTime), offset);
      }
      return authorText;
    }

    @Nonnull
    private static String getCommitterText(@Nullable VcsUser committer, @Nonnull String commitTimeText, int offset) {
      String alignment = "<br/>" + StringUtil.repeat("&nbsp;", offset);
      String gray = ColorUtil.toHex(JBColor.GRAY);

      String graySpan = "<span style='color:#" + gray + "'>";

      String text = alignment + graySpan + "committed";
      if (committer != null) {
        text += " by " + VcsUserUtil.getShortPresentation(committer);
        if (!committer.getEmail().isEmpty()) {
          text += "</span>" + getEmailText(committer) + graySpan;
        }
      }
      text += commitTimeText + "</span>";
      return text;
    }

    @Nonnull
    private static String getAuthorName(@Nonnull VcsUser user) {
      String username = VcsUserUtil.getShortPresentation(user);
      return user.getEmail().isEmpty() ? username : username + getEmailText(user);
    }

    @Nonnull
    private static String getEmailText(@Nonnull VcsUser user) {
      return " <a href='mailto:" + user.getEmail() + "'>&lt;" + user.getEmail() + "&gt;</a>";
    }

    @Override
    public Color getBackground() {
      return getCommitDetailsBackground();
    }
  }

  private static class BranchesPanel extends HtmlPanel {
    private static final int PER_ROW = 2;
    private static final String LINK_HREF = "show-hide-branches";

    @Nullable private List<String> myBranches;
    private boolean myExpanded = false;

    BranchesPanel() {
      DefaultCaret caret = (DefaultCaret)getCaret();
      caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

      setBorder(JBUI.Borders.empty(REFERENCES_BORDER, ReferencesPanel.H_GAP, BOTTOM_BORDER, 0));
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && LINK_HREF.equals(e.getDescription())) {
        myExpanded = !myExpanded;
        update();
      }
    }

    @Override
    public void updateUI() {
      super.updateUI();
      update();
    }

    void setBranches(@Nullable List<String> branches) {
      if (branches == null) {
        myBranches = null;
      }
      else {
        myBranches = branches;
      }
      myExpanded = false;
    }

    void update() {
      setText("<html><head>" +
              UIUtil.getCssFontDeclaration(getCommitDetailsFont()) +
              "</head><body>" +
              getBranchesText() +
              "</body></html>");
      revalidate();
      repaint();
    }

    @Nonnull
    private String getBranchesText() {
      if (myBranches == null) {
        return "<i>In branches: loading...</i>";
      }
      if (myBranches.isEmpty()) return "<i>Not in any branch</i>";

      if (myExpanded) {
        int rowCount = (int)Math.ceil((double)myBranches.size() / PER_ROW);

        int[] means = new int[PER_ROW - 1];
        int[] max = new int[PER_ROW - 1];

        for (int i = 0; i < rowCount; i++) {
          for (int j = 0; j < PER_ROW - 1; j++) {
            int index = rowCount * j + i;
            if (index < myBranches.size()) {
              means[j] += myBranches.get(index).length();
              max[j] = Math.max(myBranches.get(index).length(), max[j]);
            }
          }
        }
        for (int j = 0; j < PER_ROW - 1; j++) {
          means[j] /= rowCount;
        }

        HtmlTableBuilder builder = new HtmlTableBuilder();
        for (int i = 0; i < rowCount; i++) {
          builder.startRow();
          for (int j = 0; j < PER_ROW; j++) {
            int index = rowCount * j + i;
            if (index >= myBranches.size()) {
              builder.append("");
            }
            else {
              String branch = myBranches.get(index);
              if (index != myBranches.size() - 1) {
                int space = 0;
                if (j < PER_ROW - 1 && branch.length() == max[j]) {
                  space = Math.max(means[j] + 20 - max[j], 5);
                }
                builder.append(branch + StringUtil.repeat("&nbsp;", space), "left");
              }
              else {
                builder.append(branch, "left");
              }
            }
          }

          builder.endRow();
        }

        return "<i>In " + myBranches.size() + " branches:</i> " +
               "<a href=\"" + LINK_HREF + "\"><i>(click to hide)</i></a><br>" +
               builder.build();
      }
      else {
        int totalMax = 0;
        int charCount = 0;
        for (String b : myBranches) {
          totalMax++;
          charCount += b.length();
          if (charCount >= 50) break;
        }

        String branchText;
        if (myBranches.size() <= totalMax) {
          branchText = StringUtil.join(myBranches, ", ");
        }
        else {
          branchText = StringUtil.join(ContainerUtil.getFirstItems(myBranches, totalMax), ", ") +
                       "… <a href=\"" +
                       LINK_HREF +
                       "\"><i>(click to show all)</i></a>";
        }
        return "<i>In " + myBranches.size() + StringUtil.pluralize(" branch", myBranches.size()) + ":</i> " + branchText;
      }
    }

    @Override
    public Color getBackground() {
      return getCommitDetailsBackground();
    }

    public boolean isExpanded() {
      return myExpanded;
    }
  }

  private static class RootPanel extends JPanel {
    private static final int RIGHT_BORDER = Math.max(UIUtil.getScrollBarWidth(), JBUI.scale(14));
    @Nonnull
    private final RectanglePainter myLabelPainter;
    @Nonnull
    private String myText = "";
    @Nonnull
    private Color myColor = getCommitDetailsBackground();

    RootPanel() {
      myLabelPainter = new RectanglePainter(true) {
        @Override
        protected Font getLabelFont() {
          return RootPanel.getLabelFont();
        }
      };
      setOpaque(false);
    }

    @Nonnull
    private static Font getLabelFont() {
      Font font = getCommitDetailsFont();
      return font.deriveFont(font.getSize() - 2f);
    }

    public void setRoot(@Nonnull String text, @Nullable Color color) {
      myText = text;
      if (text.isEmpty() || color == null) {
        myColor = getCommitDetailsBackground();
      }
      else {
        myColor = color;
      }
    }

    public void update() {
      revalidate();
      repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
      if (!myText.isEmpty()) {
        Dimension painterSize = myLabelPainter.calculateSize(myText, getFontMetrics(getLabelFont()));
        JBScrollPane scrollPane = UIUtil.getParentOfType(JBScrollPane.class, this);
        int width;
        if (scrollPane == null) {
          width = getWidth();
        }
        else {
          Rectangle rect = scrollPane.getViewport().getViewRect();
          width = rect.x + rect.width;
        }
        myLabelPainter.paint((Graphics2D)g, myText, width - painterSize.width - RIGHT_BORDER, 0, myColor);
      }
    }

    @Override
    public Color getBackground() {
      return getCommitDetailsBackground();
    }

    @Override
    public Dimension getMinimumSize() {
      return getPreferredSize();
    }

    @Override
    public Dimension getPreferredSize() {
      if (myText.isEmpty()) return new JBDimension(0, TOP_BORDER);
      Dimension size = myLabelPainter.calculateSize(myText, getFontMetrics(getLabelFont()));
      return new Dimension(size.width + JBUI.scale(RIGHT_BORDER), size.height);
    }

    @Override
    public Dimension getMaximumSize() {
      return getPreferredSize();
    }
  }
}
