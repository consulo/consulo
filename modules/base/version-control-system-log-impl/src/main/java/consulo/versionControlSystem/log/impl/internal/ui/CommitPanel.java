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
package consulo.versionControlSystem.log.impl.internal.ui;

import consulo.application.util.DateFormatUtil;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.history.VcsHistoryUtil;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.log.VcsRef;
import consulo.versionControlSystem.log.VcsUser;
import consulo.versionControlSystem.log.impl.internal.data.LoadingDetails;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogDataImpl;
import consulo.versionControlSystem.log.impl.internal.ui.render.RectanglePainter;
import consulo.versionControlSystem.log.ui.VcsLogColorManager;
import consulo.versionControlSystem.log.util.VcsUserUtil;
import consulo.versionControlSystem.ui.awt.IssueLinkHtmlRenderer;
import consulo.versionControlSystem.ui.awt.VcsFontUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

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

class CommitPanel extends JBPanel {
  public static final int BOTTOM_BORDER = 2;
  private static final int REFERENCES_BORDER = 12;
  private static final int TOP_BORDER = 4;

  
  private final VcsLogDataImpl myLogData;

  
  private final ReferencesPanel myBranchesPanel;
  
  private final ReferencesPanel myTagsPanel;
  
  private final DataPanel myDataPanel;
  
  private final BranchesPanel myContainingBranchesPanel;
  
  private final RootPanel myRootPanel;
  
  private final VcsLogColorManager myColorManager;

  private @Nullable VcsFullCommitDetails myCommit;

  public CommitPanel(VcsLogDataImpl logData, VcsLogColorManager colorManager) {
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

  public void setCommit(VcsFullCommitDetails commitData) {
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

  public void setRefs(Collection<VcsRef> refs) {
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

  
  private List<VcsRef> sortRefs(Collection<VcsRef> refs) {
    VcsRef ref = ContainerUtil.getFirstItem(refs);
    if (ref == null) return List.of();
    return ContainerUtil.sorted(refs, myLogData.getLogProvider(ref.getRoot()).getReferenceManager().getLabelsOrderComparator());
  }

  
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

  
  public static Color getCommitDetailsBackground() {
    return UIUtil.getTableBackground();
  }

  
  public static String formatDateTime(long time) {
    return " on " + DateFormatUtil.formatDate(time) + " at " + DateFormatUtil.formatTime(time);
  }

  private static class DataPanel extends HtmlPanel {
    
    private final Project myProject;
    private @Nullable String myMainText;

    DataPanel(Project project) {
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
        String linkColor = "#" + ColorUtil.toHex(JBCurrentTheme.Link.linkColor());
        styleSheet.addRule("a { color: " + linkColor + "; text-decoration: none;}");
      }
    }

    
    private static String getHtmlWithFonts(String input) {
      return getHtmlWithFonts(input, VcsHistoryUtil.getCommitDetailsFont().getStyle());
    }

    
    private static String getHtmlWithFonts(String input, int style) {
      return VcsFontUtil.getHtmlWithFonts(input, style, VcsHistoryUtil.getCommitDetailsFont());
    }

    void update() {
      if (myMainText == null) {
        setText("");
      }
      else {
        setText("<html><head>" +
                UIUtil.getCssFontDeclaration(VcsHistoryUtil.getCommitDetailsFont()) +
                "</head><body>" +
                myMainText +
                "</body></html>");
      }
      customizeLinksStyle();
      revalidate();
      repaint();
    }

    
    private String getMessageText(VcsFullCommitDetails commit) {
      String fullMessage = commit.getFullMessage();
      int separator = fullMessage.indexOf("\n\n");
      String subject = separator > 0 ? fullMessage.substring(0, separator) : fullMessage;
      String description = fullMessage.substring(subject.length());
      return "<b>" +
             getHtmlWithFonts(escapeMultipleSpaces(IssueLinkHtmlRenderer.formatTextWithLinks(myProject, subject)), Font.BOLD) +
             "</b>" +
             getHtmlWithFonts(escapeMultipleSpaces(IssueLinkHtmlRenderer.formatTextWithLinks(myProject, description)));
    }

    
    private static String escapeMultipleSpaces(String text) {
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

    
    private static String getAuthorText(VcsFullCommitDetails commit, int offset) {
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

    
    private static String getCommitterText(@Nullable VcsUser committer, String commitTimeText, int offset) {
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

    
    private static String getAuthorName(VcsUser user) {
      String username = VcsUserUtil.getShortPresentation(user);
      return user.getEmail().isEmpty() ? username : username + getEmailText(user);
    }

    
    private static String getEmailText(VcsUser user) {
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

    private @Nullable List<String> myBranches;
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
              UIUtil.getCssFontDeclaration(VcsHistoryUtil.getCommitDetailsFont()) +
              "</head><body>" +
              getBranchesText() +
              "</body></html>");
      revalidate();
      repaint();
    }

    
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
          branchText = StringUtil.join(Lists.getFirstItems(myBranches, totalMax), ", ") +
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
    
    private final RectanglePainter myLabelPainter;
    
    private String myText = "";
    
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

    
    private static Font getLabelFont() {
      Font font = VcsHistoryUtil.getCommitDetailsFont();
      return font.deriveFont(font.getSize() - 2f);
    }

    public void setRoot(String text, @Nullable Color color) {
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
