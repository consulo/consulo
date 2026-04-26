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
package consulo.ui.ex.tree;

import consulo.annotation.access.RequiredReadAction;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.SimpleTextAttributes;

import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.Objects;

public abstract class PresentableNodeDescriptor<E> extends NodeDescriptor<E> {
  private volatile @Nullable PresentationData myTemplatePresentation = null;
  private volatile @Nullable PresentationData myUpdatedPresentation = null;

  protected PresentableNodeDescriptor(@Nullable NodeDescriptor parentDescriptor) {
    super(parentDescriptor);
  }

  @Override
  @RequiredUIAccess
  public final boolean update() {
    if (shouldUpdateData()) {
      PresentationData before = getPresentation();
      PresentationData updated = getUpdatedPresentation();
      return shouldApply() && apply(updated, before);
    }
    return false;
  }

  protected final boolean apply(PresentationData presentation) {
    return apply(presentation, null);
  }

  @Override
  public void applyFrom(NodeDescriptor desc) {
    if (desc instanceof PresentableNodeDescriptor pnd) {
      apply(pnd.getPresentation());
    }
    else {
      super.applyFrom(desc);
    }
  }

  protected final boolean apply(PresentationData presentation, @Nullable PresentationData before) {
    setIcon(presentation.getIcon());
    // If the node has both plain and colored text, the plain one takes priority for myName because it's also supposed to be plain,
    // and it can be used, e.g. for sorting, while the colored version may contain information not needed for sorting such as inplace comments.
    myName = presentation.getPresentableText();
    if (myName == null) {
      myName = getColoredTextAsPlainText(presentation);
    }
    myColor = presentation.getForcedTextForeground();
    boolean updated = !presentation.equals(before);

    PresentationData updatedPresentation = myUpdatedPresentation;
    if (updatedPresentation == null) {
      updatedPresentation = createPresentation();
    } else {
      updatedPresentation = updatedPresentation.clone();
    }

    updatedPresentation.copyFrom(presentation);

    PresentationData templatePresentation = myTemplatePresentation;
    if (templatePresentation != null) {
      updatedPresentation.applyFrom(templatePresentation);
    }

    updated |= updatedPresentation.isChanged();
    updatedPresentation.setChanged(false);

    myUpdatedPresentation = updatedPresentation;
    return updated;
  }

  private PresentationData getUpdatedPresentation() {
    PresentationData presentation = getPresentation().clone();
    presentation.clear();
    presentation.setBackground(computeBackgroundColor());
    update(presentation);

    if (shouldPostprocess()) {
      postprocess(presentation);
    }

    myUpdatedPresentation = presentation;
    return presentation;
  }

  @RequiredReadAction
  protected @Nullable ColorValue computeBackgroundColor() {
    return null;
  }

  protected PresentationData createPresentation() {
    return new PresentationData();
  }

  protected void postprocess(PresentationData date) {
  }

  protected boolean shouldPostprocess() {
    return true;
  }

  protected boolean shouldApply() {
    return true;
  }

  protected boolean shouldUpdateData() {
    return true;
  }

  protected @Nullable Color computeBackgroundColor() {
    return null;
  }

  protected abstract void update(PresentationData presentation);

  public final PresentationData getPresentation() {
    PresentationData updatedPresentation = myUpdatedPresentation;
    return updatedPresentation == null ? getTemplatePresentation() : updatedPresentation;
  }

  protected final PresentationData getTemplatePresentation() {
    PresentationData templatePresentation = myTemplatePresentation;
    if (templatePresentation == null) {
      templatePresentation = createPresentation();
      myTemplatePresentation = templatePresentation;
    }
    return templatePresentation;
  }

  public boolean isContentHighlighted() {
    return false;
  }

  public boolean isHighlightableContentNode(PresentableNodeDescriptor kid) {
    return true;
  }

  public PresentableNodeDescriptor getChildToHighlightAt(int index) {
    return null;
  }

  public boolean isParentOf(NodeDescriptor eachNode) {
    NodeDescriptor eachParent = eachNode.getParentDescriptor();
    while (eachParent != null) {
      if (eachParent == this) return true;
      eachParent = eachParent.getParentDescriptor();
    }
    return false;
  }

  public boolean isAncestorOrSelf(NodeDescriptor selectedNode) {
    NodeDescriptor node = selectedNode;
    while (node != null) {
      if (equals(node)) return true;
      node = node.getParentDescriptor();
    }
    return false;
  }

  public static final class ColoredFragment {
    private final LocalizeValue myText;
    private final LocalizeValue myToolTip;
    private final SimpleTextAttributes myAttributes;

    public ColoredFragment(LocalizeValue text, SimpleTextAttributes attributes) {
      this(text, LocalizeValue.empty(), attributes);
    }

    public ColoredFragment(LocalizeValue text, LocalizeValue toolTip, SimpleTextAttributes attributes) {
      myText = text;
      myAttributes = attributes;
      myToolTip = toolTip;
    }

    public LocalizeValue getToolTip() {
      return myToolTip;
    }

    public LocalizeValue getText() {
      return myText;
    }

    public SimpleTextAttributes getAttributes() {
      return myAttributes;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ColoredFragment that = (ColoredFragment) o;

      return Objects.equals(myAttributes, that.myAttributes)
        && myText.equals(that.myText)
        && myToolTip.equals(that.myToolTip);
    }

    @Override
    public int hashCode() {
      int result = myText.hashCode();
      result = 31 * result + myToolTip.hashCode();
      result = 31 * result + Objects.hashCode(myAttributes);
      return result;
    }
  }

  @Override
  public String getName() {
    String result = getColoredTextAsPlainText(getPresentation());
    return result == null ? myName : result;
  }

  protected static @Nullable String getColoredTextAsPlainText(PresentationData presentation) {
    List<ColoredFragment> textFragments = presentation.getColoredText();
    int size = textFragments.size();
    if (size == 0) {
      return null;
    }
    else if (size == 1) {
      return textFragments.get(0).getText().getNullIfEmpty();
    }
    else {
      StringBuilder result = new StringBuilder();
      for (ColoredFragment each : textFragments) {
        result.append(each.getText().get());
      }
      return result.toString();
    }
  }
}
