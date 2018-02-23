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
package com.intellij.dvcs.push.ui;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.MouseEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VcsLinkedTextComponent extends JLabel {
  private static final Pattern HREF_PATTERN = Pattern.compile("<a(?:\\s+href\\s*=\\s*[\"']([^\"']*)[\"'])?\\s*>([^<]*)</a>");

  @Nonnull
  private String myTextBefore;
  @Nonnull
  private String myTextAfter;
  @Nonnull
  private String myHandledLink;

  @Nullable private final VcsLinkListener myLinkListener;
  private boolean mySelected;
  private boolean myUnderlined;
  private boolean myTransparent;

  public VcsLinkedTextComponent(@Nonnull String text, @javax.annotation.Nullable VcsLinkListener listener) {
    Matcher aMatcher = HREF_PATTERN.matcher(text);
    if (aMatcher.find()) {
      myTextBefore = text.substring(0, aMatcher.start());
      myHandledLink = aMatcher.group(2);
      myTextAfter = text.substring(aMatcher.end(), text.length());
    }
    else {
      myTextBefore = text;
      myHandledLink = "";
      myTextAfter = "";
    }
    myLinkListener = listener;
  }

  public void updateLinkText(@Nonnull String text) {
    myHandledLink = text;
  }

  public void fireOnClick(@Nonnull DefaultMutableTreeNode relatedNode, @Nonnull MouseEvent event) {
    if (myLinkListener != null) {
      myLinkListener.hyperlinkActivated(relatedNode, event);
    }
  }

  public void render(@Nonnull ColoredTreeCellRenderer renderer) {
    boolean isActive = mySelected || myUnderlined;
    SimpleTextAttributes linkTextAttributes = isActive ? SimpleTextAttributes.LINK_ATTRIBUTES : SimpleTextAttributes.SYNTHETIC_ATTRIBUTES;
    isActive = isActive || !myTransparent;
    SimpleTextAttributes wrappedTextAttributes = PushLogTreeUtil
      .addTransparencyIfNeeded(SimpleTextAttributes.REGULAR_ATTRIBUTES, isActive);
    if (!StringUtil.isEmptyOrSpaces(myTextBefore)) {
      renderer.append(myTextBefore, wrappedTextAttributes);
      renderer.append(" ");
    }
    if (!StringUtil.isEmptyOrSpaces(myHandledLink)) {
      renderer.append(myHandledLink, PushLogTreeUtil.addTransparencyIfNeeded(linkTextAttributes, isActive), this);
    }
    renderer.append(myTextAfter, wrappedTextAttributes);
  }

  public void setUnderlined(boolean underlined) {
    myUnderlined = underlined;
  }

  public void setSelected(boolean selected) {
    mySelected = selected;
  }

  public void setTransparent(boolean transparent) {
    myTransparent = transparent;
  }

  @Nonnull
  public String getText() {
    return myTextBefore + myHandledLink + myTextAfter;
  }
}
