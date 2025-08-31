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

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class PresentableNodeDescriptor<E> extends NodeDescriptor<E> {

  private PresentationData myTemplatePresentation;
  private PresentationData myUpdatedPresentation;

  protected PresentableNodeDescriptor(@Nullable NodeDescriptor parentDescriptor) {
    super(parentDescriptor);
  }

  @RequiredUIAccess
  @Override
  public final boolean update() {
    if (shouldUpdateData()) {
      PresentationData before = getPresentation().clone();
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
    if (desc instanceof PresentableNodeDescriptor) {
      PresentableNodeDescriptor pnd = (PresentableNodeDescriptor)desc;
      apply(pnd.getPresentation());
    }
    else {
      super.applyFrom(desc);
    }
  }

  protected final boolean apply(PresentationData presentation, @Nullable PresentationData before) {
    setIcon(presentation.getIcon());
    myName = presentation.getPresentableText();
    myColor = presentation.getForcedTextForeground();
    boolean updated = before == null || !presentation.equals(before);

    if (myUpdatedPresentation == null) {
      myUpdatedPresentation = createPresentation();
    }

    myUpdatedPresentation.copyFrom(presentation);

    if (myTemplatePresentation != null) {
      myUpdatedPresentation.applyFrom(myTemplatePresentation);
    }

    updated |= myUpdatedPresentation.isChanged();
    myUpdatedPresentation.setChanged(false);

    return updated;
  }

  private PresentationData getUpdatedPresentation() {
    PresentationData presentation = myUpdatedPresentation != null ? myUpdatedPresentation : createPresentation();
    myUpdatedPresentation = presentation;
    presentation.clear();
    update(presentation);

    if (shouldPostprocess()) {
      postprocess(presentation);
    }

    return presentation;
  }

  @Nonnull
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

  protected abstract void update(PresentationData presentation);

  @Nonnull
  public final PresentationData getPresentation() {
    PresentationData result;
    if (myUpdatedPresentation == null) {
      result = getTemplatePresentation();
    }
    else {
      result = myUpdatedPresentation;
    }
    return result;
  }

  protected final PresentationData getTemplatePresentation() {
    if (myTemplatePresentation == null) {
      myTemplatePresentation = createPresentation();
    }

    return myTemplatePresentation;
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

  public static class ColoredFragment {
    private final LocalizeValue myText;
    private final LocalizeValue myToolTip;
    private final SimpleTextAttributes myAttributes;

    public ColoredFragment(@Nonnull LocalizeValue aText, SimpleTextAttributes aAttributes) {
      this(aText, LocalizeValue.of(), aAttributes);
    }

    public ColoredFragment(@Nonnull LocalizeValue aText, @Nonnull LocalizeValue toolTip, SimpleTextAttributes aAttributes) {
      myText = aText;
      myAttributes = aAttributes;
      myToolTip = toolTip;
    }

    @Nonnull
    public LocalizeValue getToolTip() {
      return myToolTip;
    }

    @Nonnull
    public LocalizeValue getText() {
      return myText;
    }

    public SimpleTextAttributes getAttributes() {
      return myAttributes;
    }


    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ColoredFragment that = (ColoredFragment)o;

      if (myAttributes != null ? !myAttributes.equals(that.myAttributes) : that.myAttributes != null) return false;
      if (myText != null ? !myText.equals(that.myText) : that.myText != null) return false;
      if (myToolTip != null ? !myToolTip.equals(that.myToolTip) : that.myToolTip != null) return false;

      return true;
    }

    public int hashCode() {
      int result;
      result = (myText != null ? myText.hashCode() : 0);
      result = 31 * result + (myToolTip != null ? myToolTip.hashCode() : 0);
      result = 31 * result + (myAttributes != null ? myAttributes.hashCode() : 0);
      return result;
    }
  }

  @Override
  public String getName() {
    if (!getPresentation().getColoredText().isEmpty()) {
      StringBuilder result = new StringBuilder("");
      for (ColoredFragment each : getPresentation().getColoredText()) {
        result.append(each.getText());
      }
      return result.toString();
    }
    return myName;
  }
}
