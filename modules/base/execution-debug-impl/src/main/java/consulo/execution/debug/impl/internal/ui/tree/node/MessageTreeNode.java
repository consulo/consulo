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
package consulo.execution.debug.impl.internal.ui.tree.node;

import consulo.execution.debug.frame.XDebuggerTreeNodeHyperlink;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.ui.XDebuggerUIConstants;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTree;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.IJSwingUtilities;
import consulo.ui.image.Image;
import consulo.util.collection.SmartList;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.event.HyperlinkListener;
import javax.swing.tree.TreeNode;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author nik
 */
public class MessageTreeNode extends XDebuggerTreeNode {
  private final boolean myEllipsis;
  private XDebuggerTreeNodeHyperlink myLink;

  private MessageTreeNode(XDebuggerTree tree, @Nullable final XDebuggerTreeNode parent, final String message, final SimpleTextAttributes attributes, @Nullable Image icon) {
    this(tree, parent, message, attributes, icon, null);
  }

  private MessageTreeNode(
    XDebuggerTree tree,
    final XDebuggerTreeNode parent,
    final String message,
    final SimpleTextAttributes attributes,
    @Nullable Image icon,
    final XDebuggerTreeNodeHyperlink link
  ) {
    this(tree, parent, message, attributes, icon, false, link);
  }

  private MessageTreeNode(
    XDebuggerTree tree,
    final XDebuggerTreeNode parent,
    final String message,
    final SimpleTextAttributes attributes,
    @Nullable Image icon,
    final boolean ellipsis,
    final XDebuggerTreeNodeHyperlink link
  ) {
    super(tree, parent, true);
    myEllipsis = ellipsis;
    myLink = link;
    setIcon(icon);
    myText.append(message, attributes);
  }

  protected MessageTreeNode(
    XDebuggerTree tree,
    @Nullable XDebuggerTreeNode parent,
    boolean leaf
  ) {
    super(tree, parent, leaf);
    myEllipsis = false;
  }

  @Nonnull
  @Override
  public List<? extends TreeNode> getChildren() {
    return Collections.emptyList();
  }

  public boolean isEllipsis() {
    return myEllipsis;
  }

  @Nullable
  @Override
  protected XDebuggerTreeNodeHyperlink getLink() {
    return myLink;
  }

  @Nonnull
  @Override
  public List<? extends XDebuggerTreeNode> getLoadedChildren() {
    return Collections.emptyList();
  }

  @Override
  public void clearChildren() {
  }

  public static MessageTreeNode createEllipsisNode(XDebuggerTree tree, XDebuggerTreeNode parent, final int remaining) {
    LocalizeValue message = remaining == -1
      ? XDebuggerLocalize.nodeTextEllipsis0UnknownMoreNodesDoubleClickToShow()
      : XDebuggerLocalize.nodeTextEllipsis0MoreNodesDoubleClickToShow(remaining);
    return new MessageTreeNode(tree, parent, message.get(), SimpleTextAttributes.GRAYED_ATTRIBUTES, null, true, null);
  }

  public static MessageTreeNode createMessageNode(
    XDebuggerTree tree,
    XDebuggerTreeNode parent,
    String message,
    @Nullable Image icon
  ) {
    return new MessageTreeNode(tree, parent, message, SimpleTextAttributes.REGULAR_ATTRIBUTES, icon);
  }

  public static MessageTreeNode createLoadingMessage(XDebuggerTree tree, final XDebuggerTreeNode parent) {
    return new MessageTreeNode(
      tree,
      parent,
      XDebuggerUIConstants.COLLECTING_DATA_MESSAGE,
      XDebuggerUIConstants.COLLECTING_DATA_HIGHLIGHT_ATTRIBUTES,
      null
    );
  }

  public static MessageTreeNode createEvaluatingMessage(XDebuggerTree tree, @Nullable XDebuggerTreeNode parent) {
    return new MessageTreeNode(
      tree,
      parent,
      XDebuggerUIConstants.EVALUATING_EXPRESSION_MESSAGE,
      XDebuggerUIConstants.EVALUATING_EXPRESSION_HIGHLIGHT_ATTRIBUTES,
      null
    );
  }

  public static List<MessageTreeNode> createMessages(
    XDebuggerTree tree,
    final XDebuggerTreeNode parent,
    @Nonnull String errorMessage,
    XDebuggerTreeNodeHyperlink link,
    final Image icon,
    final SimpleTextAttributes attributes
  ) {
    List<MessageTreeNode> messages = new SmartList<>();
    final List<String> lines = StringUtil.split(errorMessage, "\n", true, false);
    for (int i = 0; i < lines.size(); i++) {
      messages.add(new MessageTreeNode(tree, parent, lines.get(i), attributes, icon, i == lines.size() - 1 ? link : null));
    }
    return messages;
  }

  public static MessageTreeNode createInfoMessage(XDebuggerTree tree, @Nonnull String message) {
    return createInfoMessage(tree, message, null);
  }

  public static MessageTreeNode createInfoMessage(
    XDebuggerTree tree,
    @Nonnull String message,
    @Nullable HyperlinkListener hyperlinkListener
  ) {
    Matcher matcher = MessageTreeNodeWithLinks.HREF_PATTERN.matcher(message);
    if (hyperlinkListener == null || !matcher.find()) {
      return new MessageTreeNode(
        tree,
        null,
        message,
        SimpleTextAttributes.REGULAR_ATTRIBUTES,
        XDebuggerUIConstants.INFORMATION_MESSAGE_ICON
      );
    }

    List<Object> objects = new ArrayList<>();
    int prev = 0;
    do {
      if (matcher.start() != prev) {
        objects.add(message.substring(prev, matcher.start()));
      }
      objects.add(new HyperlinkListenerDelegator(matcher.group(2), matcher.group(1), hyperlinkListener));
      prev = matcher.end();
    }
    while (matcher.find());

    if (prev < message.length()) {
      objects.add(message.substring(prev));
    }
    return new MessageTreeNodeWithLinks(tree, objects);
  }

  private static class MessageTreeNodeWithLinks extends MessageTreeNode {
    private static final Pattern HREF_PATTERN = Pattern.compile("<a(?:\\s+href\\s*=\\s*[\"']([^\"']*)[\"'])?\\s*>([^<]*)</a>");
    private final List<Object> objects;

    private MessageTreeNodeWithLinks(XDebuggerTree tree, List<Object> objects) {
      super(tree, null, true);
      setIcon(XDebuggerUIConstants.INFORMATION_MESSAGE_ICON);
      this.objects = objects;
    }

    @Override
    public void appendToComponent(@Nonnull ColoredTextContainer component) {
      for (Object object : objects) {
        if (object instanceof String) {
          component.append((String)object, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
        else {
          XDebuggerTreeNodeHyperlink hyperlink = (XDebuggerTreeNodeHyperlink)object;
          component.append(hyperlink.getLinkText(), SimpleTextAttributes.LINK_ATTRIBUTES, hyperlink);
        }
      }
    }
  }

  public static final class HyperlinkListenerDelegator extends XDebuggerTreeNodeHyperlink {
    private final HyperlinkListener hyperlinkListener;
    private final String href;

    public HyperlinkListenerDelegator(@Nonnull String linkText, @Nullable String href, @Nonnull HyperlinkListener hyperlinkListener) {
      super(linkText);

      this.hyperlinkListener = hyperlinkListener;
      this.href = href;
    }

    @Override
    public void onClick(MouseEvent event) {
      hyperlinkListener.hyperlinkUpdate(IJSwingUtilities.createHyperlinkEvent(href, getLinkText()));
    }
  }
}
