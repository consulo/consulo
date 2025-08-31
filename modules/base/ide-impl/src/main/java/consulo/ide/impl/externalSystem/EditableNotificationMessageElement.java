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
package consulo.ide.impl.externalSystem;

import consulo.ide.impl.idea.ide.IdeTooltipManagerImpl;
import consulo.ide.impl.idea.ide.errorTreeView.*;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.navigation.Navigatable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.errorTreeView.ErrorTreeElementKind;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 * @since 2014-03-24
 */
public class EditableNotificationMessageElement extends NotificationMessageElement implements EditableMessageElement {

  @Nonnull
  private final TreeCellEditor myRightTreeCellEditor;
  @Nonnull
  private final Notification myNotification;
  @Nonnull
  private final Map<String/*url*/, String/*link text to replace*/> disabledLinks;

  public EditableNotificationMessageElement(
    @Nonnull Notification notification,
    @Nonnull ErrorTreeElementKind kind,
    @Nullable GroupingElement parent,
    String[] message,
    Navigatable navigatable,
    String exportText, String rendererTextPrefix
  ) {
    super(kind, parent, message, navigatable, exportText, rendererTextPrefix);
    myNotification = notification;
    disabledLinks = new HashMap<>();
    myRightTreeCellEditor = new MyCellEditor();
  }

  public void addDisabledLink(@Nonnull String url, @Nullable String text) {
    disabledLinks.put(url, text);
  }

  @Nonnull
  @Override
  public TreeCellEditor getRightSelfEditor() {
    return myRightTreeCellEditor;
  }

  @Override
  public boolean startEditingOnMouseMove() {
    return true;
  }

  public static void disableLink(@Nonnull HyperlinkEvent event) {
    disableLink(event, null);
  }

  private static void disableLink(@Nonnull HyperlinkEvent event, @Nullable String linkText) {
    if (event.getSource() instanceof MyJEditorPane editorPane) {
      UIUtil.invokeLaterIfNeeded(() -> {
        editorPane.myElement.addDisabledLink(event.getDescription(), linkText);
        editorPane.myElement.updateStyle(editorPane, null, null, true, false);
      });
    }
  }

  @Override
  protected void updateStyle(@Nonnull JEditorPane editorPane, @Nullable JTree tree, Object value, boolean selected, boolean hasFocus) {
    super.updateStyle(editorPane, tree, value, selected, hasFocus);

    HTMLDocument htmlDocument = (HTMLDocument)editorPane.getDocument();
    Style linkStyle = htmlDocument.getStyleSheet().getStyle(NotificationMessageElement.LINK_STYLE);
    StyleConstants.setForeground(linkStyle, IdeTooltipManagerImpl.getInstanceImpl().getLinkForeground(false));
    StyleConstants.setItalic(linkStyle, true);
    HTMLDocument.Iterator iterator = htmlDocument.getIterator(HTML.Tag.A);
    while (iterator.isValid()) {
      boolean disabledLink = false;
      AttributeSet attributes = iterator.getAttributes();
      if (attributes instanceof SimpleAttributeSet simpleAttributeSet) {
        Object attribute = attributes.getAttribute(HTML.Attribute.HREF);
        if (attribute instanceof String && disabledLinks.containsKey(attribute)) {
          disabledLink = true;
          //TODO [Vlad] add support for disabled link text update
          ////final String linkText = disabledLinks.get(attribute);
          //if (linkText != null) {
          //}
          simpleAttributeSet.removeAttribute(HTML.Attribute.HREF);
        }
        if (attribute == null) {
          disabledLink = true;
        }
      }
      if (!disabledLink) {
        htmlDocument.setCharacterAttributes(
                iterator.getStartOffset(), iterator.getEndOffset() - iterator.getStartOffset(), linkStyle, false);
      }
      iterator.next();
    }
  }

  private static class MyJEditorPane extends JEditorPane {
    @Nonnull
    private final EditableNotificationMessageElement myElement;

    public MyJEditorPane(@Nonnull EditableNotificationMessageElement element) {
      myElement = element;
    }
  }

  private class MyCellEditor extends AbstractCellEditor implements TreeCellEditor {
    private final JEditorPane editorComponent;
    @Nullable
    private JTree myTree;

    private MyCellEditor() {
      editorComponent = installJep(new MyJEditorPane(EditableNotificationMessageElement.this));

      HyperlinkListener hyperlinkListener = new ActivatedHyperlinkListener();
      editorComponent.addHyperlinkListener(hyperlinkListener);
      editorComponent.addMouseListener(new PopupHandler() {
        @Override
        public void invokePopup(Component comp, int x, int y) {
          if (myTree == null) return;

          TreePath path = myTree.getLeadSelectionPath();
          if (path == null) {
            return;
          }
          DefaultActionGroup group = new DefaultActionGroup();
          group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_SOURCE));
          group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_COPY));

          ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.COMPILER_MESSAGES_POPUP, group);
          menu.getComponent().show(comp, x, y);
        }
      });
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row) {
      myTree = tree;
      updateStyle(editorComponent, tree, value, selected, false);
      return editorComponent;
    }

    @Override
    public Object getCellEditorValue() {
      return null;
    }

    private class ActivatedHyperlinkListener implements HyperlinkListener {
      @Override
      @RequiredUIAccess
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          NotificationListener notificationListener = myNotification.getListener();
          if (notificationListener != null) {
            notificationListener.hyperlinkUpdate(myNotification, e);
          }
        }
      }
    }
  }
}
