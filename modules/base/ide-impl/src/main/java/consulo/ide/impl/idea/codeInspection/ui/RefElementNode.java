/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInspection.ui;

import consulo.language.editor.inspection.CommonProblemDescriptor;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.tree.MutableTreeNode;

/**
 * @author max
 */
public class RefElementNode extends InspectionTreeNode {
  private boolean myHasDescriptorsUnder = false;
  private CommonProblemDescriptor mySingleDescriptor = null;
  protected final InspectionToolPresentation myToolPresentation;
  private final Image myIcon;

  public RefElementNode(@Nonnull Object userObject, @Nonnull InspectionToolPresentation presentation) {
    super(userObject);
    myToolPresentation = presentation;
    myIcon = userObject instanceof RefEntity ? ((RefEntity)userObject).getIcon(false) : null;
  }

  public RefElementNode(@Nonnull RefElement element, @Nonnull InspectionToolPresentation presentation) {
    this((Object)element, presentation);
  }

  public boolean hasDescriptorsUnder() {
    return myHasDescriptorsUnder;
  }

  @Nullable
  public RefEntity getElement() {
    return (RefEntity)getUserObject();
  }

  @Override
  @Nullable
  public Image getIcon() {
    return myIcon;
  }

  @Override
  public int getProblemCount() {
    return Math.max(1, super.getProblemCount());
  }

  public String toString() {
    RefEntity element = getElement();
    if (element == null || !element.isValid()) {
      return InspectionsBundle.message("inspection.reference.invalid");
    }
    return element.getRefManager().getRefinedElement(element).getQualifiedName();
  }

  @Override
  public boolean isValid() {
    RefEntity refEntity = getElement();
    return refEntity != null && refEntity.isValid();
  }

  @Override
  public boolean isResolved() {
    return myToolPresentation.isElementIgnored(getElement());
  }


  @Override
  public void ignoreElement() {
    myToolPresentation.ignoreCurrentElement(getElement());
    super.ignoreElement();
  }

  @Override
  public void amnesty() {
    myToolPresentation.amnesty(getElement());
    super.amnesty();
  }

  @Override
  public FileStatus getNodeStatus() {
    return  myToolPresentation.getElementStatus(getElement());
  }

  @Override
  public void add(MutableTreeNode newChild) {
    super.add(newChild);
    if (newChild instanceof ProblemDescriptionNode) {
      myHasDescriptorsUnder = true;
    }
  }

  public void setProblem(@Nonnull CommonProblemDescriptor descriptor) {
    mySingleDescriptor = descriptor;
  }

  public CommonProblemDescriptor getProblem() {
    return mySingleDescriptor;
  }

}
