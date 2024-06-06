/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.externalSystem.service.notification;

import consulo.application.AllIcons;
import consulo.ide.impl.idea.ide.errorTreeView.*;
import consulo.util.lang.StringUtil;
import consulo.navigation.Navigatable;
import consulo.ide.impl.idea.ui.CustomizeColoredTreeCellRenderer;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.tree.LoadingNode;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.JBHtmlEditorKit;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.internal.laf.WideSelectionTreeUI;
import consulo.ui.ex.errorTreeView.ErrorTreeElementKind;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;
import java.awt.*;

/**
 * @author Vladislav.Soroka
 * @since 3/24/2014
 */
public class NotificationMessageElement extends NavigatableMessageElement {
  public static final String MSG_STYLE = "messageStyle";
  public static final String LINK_STYLE = "linkStyle";

  @Nonnull
  private final CustomizeColoredTreeCellRenderer myLeftTreeCellRenderer;
  @Nonnull
  private final CustomizeColoredTreeCellRenderer myRightTreeCellRenderer;

  public NotificationMessageElement(@Nonnull final ErrorTreeElementKind kind,
                                    @Nullable GroupingElement parent,
                                    String[] message,
                                    Navigatable navigatable,
                                    String exportText,
                                    String rendererTextPrefix) {
    super(kind, parent, message, navigatable, exportText, rendererTextPrefix);
    myLeftTreeCellRenderer = new CustomizeColoredTreeCellRenderer() {
      public void customizeCellRenderer(SimpleColoredComponent renderer,
                                        JTree tree,
                                        Object value,
                                        boolean selected,
                                        boolean expanded,
                                        boolean leaf,
                                        int row,
                                        boolean hasFocus) {
        renderer.setIcon(getIcon(kind));
        renderer.setFont(tree.getFont());
        renderer.append(NewErrorTreeRenderer.calcPrefix(NotificationMessageElement.this));
      }

      @Nonnull
      private Image getIcon(@Nonnull ErrorTreeElementKind kind) {
        Image icon = AllIcons.General.Mdot_empty;
        switch (kind) {
          case INFO:
            icon = AllIcons.General.Information;
            break;
          case ERROR:
            icon = AllIcons.General.Error;
            break;
          case WARNING:
            icon = AllIcons.General.Warning;
            break;
          case NOTE:
            icon = AllIcons.General.Tip;
            break;
          case GENERIC:
            icon = AllIcons.General.Mdot_empty;
            break;
        }
        return icon;
      }
    };

    myRightTreeCellRenderer = new MyCustomizeColoredTreeCellRendererReplacement();
  }

  @jakarta.annotation.Nullable
  @Override
  public CustomizeColoredTreeCellRenderer getRightSelfRenderer() {
    return myRightTreeCellRenderer;
  }

  @Nullable
  @Override
  public CustomizeColoredTreeCellRenderer getLeftSelfRenderer() {
    return myLeftTreeCellRenderer;
  }

  protected JEditorPane installJep(@Nonnull JEditorPane myEditorPane) {
    String message = StringUtil.join(this.getText(), "<br>");
    myEditorPane.setEditable(false);
    myEditorPane.setOpaque(false);
    myEditorPane.setEditorKit(JBHtmlEditorKit.create());
    myEditorPane.setHighlighter(null);

    final StyleSheet styleSheet = ((HTMLDocument)myEditorPane.getDocument()).getStyleSheet();
    final Style style = styleSheet.addStyle(MSG_STYLE, null);
    styleSheet.addStyle(LINK_STYLE, style);
    myEditorPane.setText(message);

    return myEditorPane;
  }

  protected void updateStyle(@Nonnull JEditorPane editorPane, @Nullable JTree tree, Object value, boolean selected, boolean hasFocus) {
    final HTMLDocument htmlDocument = (HTMLDocument)editorPane.getDocument();
    final Style style = htmlDocument.getStyleSheet().getStyle(MSG_STYLE);
    if (value instanceof LoadingNode) {
      StyleConstants.setForeground(style, JBColor.GRAY);
    }
    else {
      if (selected) {
        StyleConstants.setForeground(style, hasFocus ? UIUtil.getTreeSelectionForeground() : UIUtil.getTreeTextForeground());
      }
      else {
        StyleConstants.setForeground(style, UIUtil.getTreeTextForeground());
      }
    }

    if (UIUtil.isUnderGTKLookAndFeel() ||
        UIUtil.isUnderNimbusLookAndFeel() && selected && hasFocus ||
        tree != null && WideSelectionTreeUI.isWideSelection(tree)) {
      editorPane.setOpaque(false);
    }
    else {
      editorPane.setOpaque(selected && hasFocus);
    }

    htmlDocument.setCharacterAttributes(0, htmlDocument.getLength(), style, false);
  }

  private class MyCustomizeColoredTreeCellRendererReplacement extends CustomizeColoredTreeCellRendererReplacement {
    @Nonnull
    private final JEditorPane myEditorPane;

    private MyCustomizeColoredTreeCellRendererReplacement() {
      myEditorPane = installJep(new JEditorPane());
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean selected,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus) {
      updateStyle(myEditorPane, tree, value, selected, hasFocus);
      return myEditorPane;
    }
  }
}
