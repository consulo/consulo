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
package consulo.ide.impl.idea.dvcs.push.ui;

import consulo.language.editor.ui.awt.HintUtil;
import consulo.webBrowser.BrowserUtil;
import consulo.ui.ex.popup.ComponentPopupBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.awt.HyperlinkAdapter;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.Wrapper;
import consulo.ui.ex.awt.UIUtil;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.tree.TreePath;
import java.awt.*;

public class VcsCommitInfoBalloon {
  private static final String EMPTY_COMMIT_INFO = "<i style='color:gray;'>No commit information found</i>";

  @Nonnull
  private final JTree myTree;
  @Nonnull
  private final Wrapper myWrapper;
  @jakarta.annotation.Nullable
  private JBPopup myBalloon;
  @Nonnull
  private final JEditorPane myEditorPane;
  @Nonnull
  private final ComponentPopupBuilder myPopupBuilder;

  public VcsCommitInfoBalloon(@Nonnull JTree tree) {
    myTree = tree;
    myEditorPane = new JEditorPane(UIUtil.HTML_MIME, "");
    myEditorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
    myEditorPane.setEditable(false);
    myEditorPane.setBackground(HintUtil.INFORMATION_COLOR);
    myEditorPane.setFont(UIUtil.getToolTipFont());
    myEditorPane.setBorder(HintUtil.createHintBorder());
    Border margin = IdeBorderFactory.createEmptyBorder(3, 3, 3, 3);
    myEditorPane.setBorder(new CompoundBorder(myEditorPane.getBorder(), margin));
    myEditorPane.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      protected void hyperlinkActivated(HyperlinkEvent e) {
        BrowserUtil.browse(e.getURL());
      }
    });
    myWrapper = new Wrapper(myEditorPane);
    myPopupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(myWrapper, null);
    myPopupBuilder.setCancelOnClickOutside(true).setResizable(true).setMovable(true).setRequestFocus(false)
            .setMinSize(new Dimension(80, 30));
  }

  public void updateCommitDetails() {
    if (myBalloon != null && myBalloon.isVisible()) {
      TreePath[] selectionPaths = myTree.getSelectionPaths();
      if (selectionPaths == null || selectionPaths.length != 1) {
        myBalloon.cancel();
      }
      else {
        Object node = selectionPaths[0].getLastPathComponent();
        myEditorPane.setText(
                XmlStringUtil.wrapInHtml(node instanceof TooltipNode ? ((TooltipNode)node).getTooltip().replaceAll("\n", "<br>") :
                                         EMPTY_COMMIT_INFO));
        //workaround: fix initial size for JEditorPane
        RepaintManager rp = RepaintManager.currentManager(myEditorPane);
        rp.markCompletelyDirty(myEditorPane);
        rp.validateInvalidComponents();
        rp.paintDirtyRegions();
        //
        myBalloon.setSize(myWrapper.getPreferredSize());
        myBalloon.setLocation(calculateBestPopupLocation());
      }
    }
  }

  @Nonnull
  private Point calculateBestPopupLocation() {
    Point defaultLocation = myTree.getLocationOnScreen();
    TreePath selectionPath = myTree.getSelectionPath();
    if (selectionPath == null) return defaultLocation;
    Rectangle rectangle = myTree.getPathBounds(selectionPath);
    if (rectangle == null) return defaultLocation;
    Point location = rectangle.getLocation();
    SwingUtilities.convertPointToScreen(location, myTree);
    return new Point(location.x, location.y + rectangle.height);
  }

  private void createNewCommitInfoBalloon() {
    myBalloon = myPopupBuilder.createPopup();
    myBalloon.setSize(myEditorPane.getPreferredSize());
  }

  public void showCommitDetails() {
    if (myBalloon == null || !myBalloon.isVisible()) {
      createNewCommitInfoBalloon();
      myBalloon.show(new RelativePoint(calculateBestPopupLocation()));
    }
    updateCommitDetails();
  }
}
