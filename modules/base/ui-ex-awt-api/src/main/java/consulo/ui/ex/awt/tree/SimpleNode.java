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
package consulo.ui.ex.awt.tree;

import consulo.component.util.ComparableObject;
import consulo.component.util.ComparableObjectCheck;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.tree.LeafState;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.tree.PresentableNodeDescriptor;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.image.Image;
import consulo.util.lang.Comparing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public abstract class SimpleNode extends PresentableNodeDescriptor implements ComparableObject, LeafState.Supplier {
  protected static final SimpleNode[] NO_CHILDREN = new SimpleNode[0];

  private final Project myProject;

  protected SimpleNode(SimpleNode parent) {
    this(parent == null ? null : parent.myProject, parent);
  }

  protected SimpleNode(Project project) {
    this(project, null);
  }

  protected SimpleNode(Project project, @Nullable NodeDescriptor parentDescriptor) {
    super(parentDescriptor);
    myProject = project;
    myName = "";
  }

  @Override
  public PresentableNodeDescriptor getChildToHighlightAt(int index) {
    return getChildAt(index);
  }

  protected SimpleNode() {
    super(null);
    myProject = null;
  }

  public String toString() {
    return getName();
  }

  @Override
  public int getWeight() {
    return 10;
  }

  protected SimpleTextAttributes getErrorAttributes() {
    return new SimpleTextAttributes(SimpleTextAttributes.STYLE_WAVED, TargetAWT.to(getColor()), JBColor.RED);
  }

  protected SimpleTextAttributes getPlainAttributes() {
    return new SimpleTextAttributes(Font.PLAIN, TargetAWT.to(getColor()));
  }

  @Nullable
  protected Object updateElement() {
    return getElement();
  }

  @Override
  protected void update(PresentationData presentation) {
    Object newElement = updateElement();
    if (getElement() != newElement) {
      presentation.setChanged(true);
    }
    if (newElement == null) return;

    ColorValue oldColor = myColor;
    String oldName = myName;
    Image oldIcon = getIcon();
    List<ColoredFragment> oldFragments = new ArrayList<ColoredFragment>(presentation.getColoredText());

    myColor = TargetAWT.from(UIUtil.getTreeTextForeground());

    doUpdate();

    myName = getName();
    presentation.setPresentableText(myName);

    presentation.setChanged(!Comparing.equal(new Object[]{getIcon(), myName, oldFragments, myColor},
                                             new Object[]{oldIcon, oldName, oldFragments, oldColor}));

    presentation.setForcedTextForeground(myColor);
    presentation.setIcon(getIcon());
  }

  /**
   * @deprecated use {@link #getTemplatePresentation()} to set constant presentation right in node's constructor
   * or update presentation dynamically by defining {@link #update(PresentationData)}
   */
  public final void setNodeText(String text, String tooltip, boolean hasError) {
    clearColoredText();
    SimpleTextAttributes attributes = hasError ? getErrorAttributes() : getPlainAttributes();
    getTemplatePresentation().addText(new ColoredFragment(text, tooltip, attributes));
  }

  /**
   * @deprecated use {@link #getTemplatePresentation()} to set constant presentation right in node's constructor
   * or update presentation dynamically by defining {@link #update(PresentationData)}
   */
  public final void setPlainText(String aText) {
    clearColoredText();
    addPlainText(aText);
  }

  /**
   * @deprecated use {@link #getTemplatePresentation()} to set constant presentation right in node's constructor
   * or update presentation dynamically by defining {@link #update(PresentationData)}
   */
  public final void addPlainText(String aText) {
    getTemplatePresentation().addText(new ColoredFragment(aText, getPlainAttributes()));
  }

  /**
   * @deprecated use {@link #getTemplatePresentation()} to set constant presentation right in node's constructor
   * or update presentation dynamically by defining {@link #update(PresentationData)}
   */
  public final void addErrorText(String aText, String errorTooltipText) {
    getTemplatePresentation().addText(new ColoredFragment(aText, errorTooltipText, getErrorAttributes()));
  }

  /**
   * @deprecated use {@link #getTemplatePresentation()} to set constant presentation right in node's constructor
   * or update presentation dynamically by defining {@link #update(PresentationData)}
   */
  public final void clearColoredText() {
    getTemplatePresentation().clearText();
  }

  /**
   * @deprecated use {@link #getTemplatePresentation()} to set constant presentation right in node's constructor
   * or update presentation dynamically by defining {@link #update(PresentationData)}
   */
  public final void addColoredFragment(String aText, SimpleTextAttributes aAttributes) {
    addColoredFragment(aText, null, aAttributes);
  }

  /**
   * @deprecated use {@link #getTemplatePresentation()} to set constant presentation right in node's constructor
   * or update presentation dynamically by defining {@link #update(PresentationData)}
   */
  public final void addColoredFragment(String aText, String toolTip, SimpleTextAttributes aAttributes) {
    getTemplatePresentation().addText(new ColoredFragment(aText, toolTip, aAttributes));
  }

  /**
   * @deprecated use {@link #getTemplatePresentation()} to set constant presentation right in node's constructor
   * or update presentation dynamically by defining {@link #update(PresentationData)}
   */
  public final void addColoredFragment(ColoredFragment fragment) {
    getTemplatePresentation().addText(new ColoredFragment(fragment.getText(), fragment.getAttributes()));
  }

  protected void doUpdate() {
  }

  @Override
  public Object getElement() {
    return this;
  }

  public final SimpleNode getParent() {
    return (SimpleNode)getParentDescriptor();
  }

  public int getIndex(SimpleNode child) {
    final SimpleNode[] kids = getChildren();
    for (int i = 0; i < kids.length; i++) {
      SimpleNode each = kids[i];
      if (each.equals(child)) return i;
    }

    return -1;
  }

  public abstract SimpleNode[] getChildren();

  public void accept(Predicate<SimpleNode> visitor) {
    visitor.test(this);
  }

  public void handleSelection(SimpleTree tree) {
  }

  @Nonnull
  @Override
  public LeafState getLeafState() {
    if (isAlwaysShowPlus()) return LeafState.NEVER;
    if (isAlwaysLeaf()) return LeafState.ALWAYS;
    return LeafState.DEFAULT;
  }

  public void handleDoubleClickOrEnter(SimpleTree tree, InputEvent inputEvent) {
  }

  public boolean isAlwaysShowPlus() {
    return false;
  }

  public boolean isAutoExpandNode() {
    return false;
  }

  public boolean isAlwaysLeaf() {
    return false;
  }

  public boolean shouldHaveSeparator() {
    return false;
  }

  /**
   * @deprecated never called by Tree classes
   */
  public final ColoredFragment[] getColoredText() {
    final List<ColoredFragment> list = getTemplatePresentation().getColoredText();
    return list.toArray(new ColoredFragment[list.size()]);
  }

  @Override
  @Nonnull
  public Object[] getEqualityObjects() {
    return NONE;
  }

  public int getChildCount() {
    return getChildren().length;
  }

  public SimpleNode getChildAt(final int i) {
    return getChildren()[i];
  }


  public final boolean equals(Object o) {
    return ComparableObjectCheck.equals(this, o);
  }

  public final int hashCode() {
    return ComparableObjectCheck.hashCode(this, super.hashCode());
  }

}
