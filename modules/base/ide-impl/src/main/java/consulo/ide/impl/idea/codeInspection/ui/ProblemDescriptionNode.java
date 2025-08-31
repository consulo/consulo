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

import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.application.AllIcons;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.language.psi.PsiElement;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static consulo.language.editor.inspection.ProblemDescriptorUtil.APPEND_LINE_NUMBER;
import static consulo.language.editor.inspection.ProblemDescriptorUtil.TRIM_AT_TREE_END;

/**
 * @author max
 */
public class ProblemDescriptionNode extends InspectionTreeNode {
  protected RefEntity myElement;
  private final CommonProblemDescriptor myDescriptor;
  protected final InspectionToolWrapper myToolWrapper;
  @Nonnull
  protected final InspectionToolPresentation myPresentation;

  public ProblemDescriptionNode(@Nonnull Object userObject,
                                @Nonnull InspectionToolWrapper toolWrapper,
                                @Nonnull InspectionToolPresentation presentation) {
    this(userObject, null, null, toolWrapper, presentation);
  }

  public ProblemDescriptionNode(@Nonnull RefEntity element,
                                @Nonnull CommonProblemDescriptor descriptor,
                                @Nonnull InspectionToolWrapper toolWrapper,
                                @Nonnull InspectionToolPresentation presentation) {
    this(descriptor, element, descriptor, toolWrapper, presentation);
  }

  private ProblemDescriptionNode(@Nonnull Object userObject,
                                 RefEntity element,
                                 CommonProblemDescriptor descriptor,
                                 @Nonnull InspectionToolWrapper toolWrapper,
                                 @Nonnull InspectionToolPresentation presentation) {
    super(userObject);
    myElement = element;
    myDescriptor = descriptor;
    myToolWrapper = toolWrapper;
    myPresentation = presentation;
  }

  @Nullable
  public RefEntity getElement() {
    return myElement;
  }

  @Nullable
  public CommonProblemDescriptor getDescriptor() {
    return myDescriptor;
  }

  @Override
  public Image getIcon() {
    if (myDescriptor instanceof ProblemDescriptorBase) {
      ProblemHighlightType problemHighlightType = ((ProblemDescriptorBase)myDescriptor).getHighlightType();
      if (problemHighlightType == ProblemHighlightType.ERROR) return AllIcons.General.Error;
      if (problemHighlightType == ProblemHighlightType.GENERIC_ERROR_OR_WARNING) return AllIcons.General.Warning;
    }
    return AllIcons.General.Information;
  }

  @Override
  public int getProblemCount() {
    return 1;
  }

  @Override
  public boolean isValid() {
    if (myElement instanceof RefElement && !myElement.isValid()) return false;
    CommonProblemDescriptor descriptor = getDescriptor();
    if (descriptor instanceof ProblemDescriptor) {
      PsiElement psiElement = ((ProblemDescriptor)descriptor).getPsiElement();
      return psiElement != null && psiElement.isValid();
    }
    return true;
  }


  @Override
  public boolean isResolved() {
    return myElement instanceof RefElement && getPresentation().isProblemResolved(myElement, getDescriptor());
  }

  @Override
  public void ignoreElement() {
    InspectionToolPresentation presentation = getPresentation();
    presentation.ignoreCurrentElementProblem(getElement(), getDescriptor());
  }

  @Override
  public void amnesty() {
    InspectionToolPresentation presentation = getPresentation();
    presentation.amnesty(getElement());
  }

  @Nonnull
  private InspectionToolPresentation getPresentation() {
    return myPresentation;
  }

  @Override
  public FileStatus getNodeStatus() {
    if (myElement instanceof RefElement){
      return getPresentation().getProblemStatus(myDescriptor);
    }
    return FileStatus.NOT_CHANGED;
  }

  public String toString() {
    CommonProblemDescriptor descriptor = getDescriptor();
    if (descriptor == null) return "";
    PsiElement element = descriptor instanceof ProblemDescriptor ? ((ProblemDescriptor)descriptor).getPsiElement() : null;

    return XmlStringUtil.stripHtml(ProblemDescriptorUtil.renderDescriptionMessage(descriptor, element,
                                                                                  APPEND_LINE_NUMBER | TRIM_AT_TREE_END));
  }
}
