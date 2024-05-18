// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.impl.internal.service;

import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.JBCurrentTheme;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tree.NodeRenderer;
import consulo.ui.ex.awt.util.ComponentUtil;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;

import java.awt.*;

import static consulo.execution.impl.internal.service.RepaintLinkMouseListenerBase.ACTIVE_TAG;

abstract class ServiceViewTreeCellRendererBase extends NodeRenderer {
  private boolean myAppendingTag;

  protected abstract Object getTag(String fragment);

  @Override
  public void append(@Nls @Nonnull String fragment, @Nonnull SimpleTextAttributes attributes, boolean isMainText) {
    Object tag = myAppendingTag ? null : getTag(fragment);
    if (tag == null) {
      super.append(fragment, attributes, isMainText);
      return;
    }

    boolean isActive = mySelected || tag.equals(ComponentUtil.getClientProperty(myTree, ACTIVE_TAG));
    int linkStyle = getLinkStyle(attributes, isActive);
    Color linkColor = getLinkColor(isActive);
    myAppendingTag = true;
    try {
      append(fragment, new SimpleTextAttributes(linkStyle, linkColor), tag);
    }
    finally {
      myAppendingTag = false;
    }
  }

  private Color getLinkColor(boolean isActive) {
    return mySelected && isFocused()
      ? UIUtil.getTreeSelectionForeground(true)
      : isActive ? JBCurrentTheme.Link.Foreground.HOVERED : JBCurrentTheme.Link.Foreground.ENABLED;
  }

  @SimpleTextAttributes.StyleAttributeConstant
  private static int getLinkStyle(@Nonnull SimpleTextAttributes attributes, boolean isActive) {
    int linkStyle = attributes.getStyle() & ~SimpleTextAttributes.STYLE_WAVED & ~SimpleTextAttributes.STYLE_BOLD_DOTTED_LINE;
    if (isActive) {
      linkStyle |= SimpleTextAttributes.STYLE_UNDERLINE;
    }
    else {
      linkStyle &= ~SimpleTextAttributes.STYLE_UNDERLINE;
    }
    return linkStyle;
  }
}
