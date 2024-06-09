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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Nov 23, 2001
 * Time: 10:31:03 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package consulo.ide.impl.idea.codeInspection.ui;

import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.language.editor.inspection.CommonProblemDescriptor;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.ide.impl.idea.codeInspection.offline.OfflineProblemDescriptor;
import consulo.ide.impl.idea.codeInspection.offlineViewer.OfflineProblemDescriptorNode;
import consulo.ide.impl.idea.codeInspection.offlineViewer.OfflineRefElementNode;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.document.Document;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.profile.codeInspection.ui.inspectionsTree.InspectionsConfigTreeComparator;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiQualifiedNamedElement;
import consulo.language.psi.PsiUtilCore;
import consulo.logging.Logger;

import java.util.Comparator;

public class InspectionResultsViewComparator implements Comparator {
  private static final Logger LOG = Logger.getInstance(InspectionResultsViewComparator.class);

  @Override
  public int compare(Object o1, Object o2) {
    InspectionTreeNode node1 = (InspectionTreeNode)o1;
    InspectionTreeNode node2 = (InspectionTreeNode)o2;

    if (node1 instanceof InspectionSeverityGroupNode && node2 instanceof InspectionSeverityGroupNode) {
      final InspectionSeverityGroupNode groupNode1 = (InspectionSeverityGroupNode)node1;
      final InspectionSeverityGroupNode groupNode2 = (InspectionSeverityGroupNode)node2;
      return -SeverityRegistrarImpl.getSeverityRegistrar(groupNode1.getProject()).compare(groupNode1.getSeverityLevel().getSeverity(), groupNode2.getSeverityLevel().getSeverity());
    }
    if (node1 instanceof InspectionSeverityGroupNode) return -1;
    if (node2 instanceof InspectionSeverityGroupNode) return 1;

    if (node1 instanceof InspectionGroupNode && node2 instanceof InspectionGroupNode) {
      return ((InspectionGroupNode)node1).getGroupTitle().compareToIgnoreCase(((InspectionGroupNode)node2).getGroupTitle());
    }
    if (node1 instanceof InspectionGroupNode) return -1;
    if (node2 instanceof InspectionGroupNode) return 1;

    if (node1 instanceof InspectionNode && node2 instanceof InspectionNode)
      return InspectionsConfigTreeComparator.getDisplayTextToSort(node1.toString())
              .compareToIgnoreCase(InspectionsConfigTreeComparator.getDisplayTextToSort(node2.toString()));
    if (node1 instanceof InspectionNode) return -1;
    if (node2 instanceof InspectionNode) return 1;

    if (node1 instanceof InspectionModuleNode && node2 instanceof InspectionModuleNode) {
      return Comparing.compare(node1.toString(), node2.toString());
    }
    if (node1 instanceof InspectionModuleNode) return -1;
    if (node2 instanceof InspectionModuleNode) return 1;

    if (node1 instanceof InspectionPackageNode && node2 instanceof InspectionPackageNode) {
      return ((InspectionPackageNode)node1).getPackageName().compareToIgnoreCase(((InspectionPackageNode)node2).getPackageName());
    }
    if (node1 instanceof InspectionPackageNode) return -1;
    if (node2 instanceof InspectionPackageNode) return 1;

    if (node1 instanceof OfflineRefElementNode && node2 instanceof OfflineRefElementNode ||
        node1 instanceof OfflineProblemDescriptorNode && node2 instanceof OfflineProblemDescriptorNode) {
      final Object userObject1 = node1.getUserObject();
      final Object userObject2 = node2.getUserObject();
      if (userObject1 instanceof OfflineProblemDescriptor && userObject2 instanceof OfflineProblemDescriptor) {
        final OfflineProblemDescriptor descriptor1 = (OfflineProblemDescriptor)userObject1;
        final OfflineProblemDescriptor descriptor2 = (OfflineProblemDescriptor)userObject2;
        if (descriptor1.getLine() != descriptor2.getLine()) return descriptor1.getLine() - descriptor2.getLine();
        return descriptor1.getFQName().compareTo(descriptor2.getFQName());
      }
      if (userObject1 instanceof OfflineProblemDescriptor) {
        return compareLineNumbers(userObject2, (OfflineProblemDescriptor)userObject1);
      }
      if (userObject2 instanceof OfflineProblemDescriptor) {
        return -compareLineNumbers(userObject1, (OfflineProblemDescriptor)userObject2);
      }
    }

    if (node1 instanceof RefElementNode && node2 instanceof RefElementNode){   //sort by filename and inside file by start offset
      return compareEntities(((RefElementNode)node1).getElement(), ((RefElementNode)node2).getElement());
    }
    if (node1 instanceof ProblemDescriptionNode && node2 instanceof ProblemDescriptionNode) {
      final CommonProblemDescriptor descriptor1 = ((ProblemDescriptionNode)node1).getDescriptor();
      final CommonProblemDescriptor descriptor2 = ((ProblemDescriptionNode)node2).getDescriptor();
      if (descriptor1 instanceof ProblemDescriptor && descriptor2 instanceof ProblemDescriptor) {
        //TODO: Do not materialise lazy pointers
        return ((ProblemDescriptor)descriptor1).getLineNumber() - ((ProblemDescriptor)descriptor2).getLineNumber();
      }
      if (descriptor1 != null && descriptor2 != null) {
        return descriptor1.getDescriptionTemplate().compareToIgnoreCase(descriptor2.getDescriptionTemplate());
      }
      if (descriptor1 == null) return descriptor2 == null ? 0 : -1;
      return 1;
    }

    if (node1 instanceof RefElementNode && node2 instanceof ProblemDescriptionNode) {
      final CommonProblemDescriptor descriptor = ((ProblemDescriptionNode)node2).getDescriptor();
      if (descriptor instanceof ProblemDescriptor) {
        return compareEntity(((RefElementNode)node1).getElement(), ((ProblemDescriptor)descriptor).getPsiElement());
      }
      return compareEntities(((RefElementNode)node1).getElement(), ((ProblemDescriptionNode)node2).getElement());
    }

    if (node2 instanceof RefElementNode && node1 instanceof ProblemDescriptionNode) {
      final CommonProblemDescriptor descriptor = ((ProblemDescriptionNode)node1).getDescriptor();
      if (descriptor instanceof ProblemDescriptor) {
        return -compareEntity(((RefElementNode)node2).getElement(), ((ProblemDescriptor)descriptor).getPsiElement());
      }
      return -compareEntities(((RefElementNode)node2).getElement(), ((ProblemDescriptionNode)node1).getElement());
    }

    LOG.error("node1: " + node1 + ", node2: " + node2);
    return 0;
  }

  private static int compareEntity(final RefEntity entity, final PsiElement element) {
    if (entity instanceof RefElement) {
      final PsiElement psiElement = ((RefElement)entity).getPsiElement();
      if (psiElement != null && element != null) {
        return PsiUtilCore.compareElementsByPosition(psiElement, element);
      }
      if (element == null) return psiElement == null ? 0 : 1;
    }
    if (element instanceof PsiQualifiedNamedElement) {
      return StringUtil.compare(entity.getQualifiedName(), ((PsiQualifiedNamedElement)element).getQualifiedName(), true);
    }
    if (element instanceof PsiNamedElement) {
      return StringUtil.compare(entity.getName(), ((PsiNamedElement)element).getName(), true);
    }
    return -1;
  }

  private static int compareEntities(final RefEntity entity1, final RefEntity entity2) {
    if (entity1 instanceof RefElement && entity2 instanceof RefElement) {
      return PsiUtilCore.compareElementsByPosition(((RefElement)entity1).getPsiElement(), ((RefElement)entity2).getPsiElement());
    }
    if (entity1 != null && entity2 != null) {
      return entity1.getName().compareToIgnoreCase(entity2.getName());
    }
    if (entity1 != null) return -1;
    return entity2 != null ? 1 : 0;
  }

  private static int compareLineNumbers(final Object userObject, final OfflineProblemDescriptor descriptor) {
    if (userObject instanceof RefElement) {
      final RefElement refElement = (RefElement)userObject;
      final PsiElement psiElement = refElement.getPsiElement();
      if (psiElement != null) {
        Document document = PsiDocumentManager.getInstance(psiElement.getProject()).getDocument(psiElement.getContainingFile());
        if (document != null) {
          return descriptor.getLine() - document.getLineNumber(psiElement.getTextOffset()) -1;
        }
      }
    }
    return -1;
  }

  private static class InspectionResultsViewComparatorHolder {
    private static final InspectionResultsViewComparator ourInstance = new InspectionResultsViewComparator();
  }

  public static InspectionResultsViewComparator getInstance() {

    return InspectionResultsViewComparatorHolder.ourInstance;
  }
}
