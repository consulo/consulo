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
package consulo.ide.impl.idea.codeInspection.ex;

import consulo.ide.impl.idea.codeInspection.ui.*;
import consulo.util.lang.Comparing;
import consulo.language.editor.inspection.CommonProblemDescriptor;
import consulo.language.editor.inspection.reference.*;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.project.Project;
import consulo.virtualFileSystem.status.FileStatus;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.tree.DefaultTreeModel;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * @author anna
 * @since 2007-01-10
 */
public class InspectionRVContentProviderImpl extends InspectionRVContentProvider {
  public InspectionRVContentProviderImpl(final Project project) {
    super(project);
  }

  @Override
  public boolean checkReportedProblems(@Nonnull GlobalInspectionContextImpl context,
                                       @Nonnull final InspectionToolWrapper toolWrapper) {
    InspectionToolPresentation presentation = context.getPresentation(toolWrapper);
    presentation.updateContent();
    return presentation.hasReportedProblems();
  }

  @Override
  @Nullable
  public QuickFixAction[] getQuickFixes(@Nonnull final InspectionToolWrapper toolWrapper, @Nonnull final InspectionTree tree) {
    final RefEntity[] refEntities = tree.getSelectedElements();
    InspectionToolPresentation presentation = tree.getContext().getPresentation(toolWrapper);
    return refEntities.length == 0 ? null : presentation.getQuickFixes(refEntities);
  }


  @Override
  public void appendToolNodeContent(@Nonnull GlobalInspectionContextImpl context,
                                    @Nonnull final InspectionNode toolNode,
                                    @Nonnull final InspectionTreeNode parentNode,
                                    final boolean showStructure,
                                    @Nonnull final Map<String, Set<RefEntity>> contents,
                                    @Nonnull final Map<RefEntity, CommonProblemDescriptor[]> problems,
                                    DefaultTreeModel model) {
    final InspectionToolWrapper toolWrapper = toolNode.getToolWrapper();

    Function<RefEntity, UserObjectContainer<RefEntity>> computeContainer = refElement -> new RefElementContainer(refElement, problems.get(refElement));
    InspectionToolPresentation presentation = context.getPresentation(toolWrapper);
    final Set<RefModule> moduleProblems = presentation.getModuleProblems();
    if (moduleProblems != null && !moduleProblems.isEmpty()) {
      Set<RefEntity> entities = contents.get("");
      if (entities == null) {
        entities = new HashSet<RefEntity>();
        contents.put("", entities);
      }
      entities.addAll(moduleProblems);
    }
    List<InspectionTreeNode> list = buildTree(context, contents, false, toolWrapper, computeContainer, showStructure);

    for (InspectionTreeNode node : list) {
      merge(model, node, toolNode, true);
    }

    if (presentation.isOldProblemsIncluded()) {
      final Map<RefEntity, CommonProblemDescriptor[]> oldProblems = presentation.getOldProblemElements();
      computeContainer = refElement -> new RefElementContainer(refElement, oldProblems != null ? oldProblems.get(refElement) : null);

      list = buildTree(context, presentation.getOldContent(), true, toolWrapper, computeContainer, showStructure);

      for (InspectionTreeNode node : list) {
        merge(model, node, toolNode, true);
      }
    }
    merge(model, toolNode, parentNode, false);
  }

  @Override
  protected void appendDescriptor(@Nonnull GlobalInspectionContextImpl context,
                                  @Nonnull final InspectionToolWrapper toolWrapper,
                                  @Nonnull final UserObjectContainer container,
                                  @Nonnull final InspectionPackageNode pNode,
                                  final boolean canPackageRepeat) {
    final RefElementContainer refElementDescriptor = (RefElementContainer)container;
    final RefEntity refElement = refElementDescriptor.getUserObject();
    InspectionToolPresentation presentation = context.getPresentation(toolWrapper);
    if (context.getUIOptions().SHOW_ONLY_DIFF && presentation.getElementStatus(refElement) == FileStatus.NOT_CHANGED) return;
    final CommonProblemDescriptor[] problems = refElementDescriptor.getProblemDescriptors();
    if (problems != null) {
      final RefElementNode elemNode = addNodeToParent(container, presentation, pNode);
      for (CommonProblemDescriptor problem : problems) {
        assert problem != null;
        if (context.getUIOptions().SHOW_ONLY_DIFF && presentation.getProblemStatus(problem) == FileStatus.NOT_CHANGED) {
          continue;
        }
        elemNode.add(new ProblemDescriptionNode(refElement, problem, toolWrapper,presentation));
        if (problems.length == 1) {
          elemNode.setProblem(problems[0]);
        }
      }
    }
    else {
      if (canPackageRepeat) {
        final Set<RefEntity> currentElements = presentation.getContent().get(pNode.getPackageName());
        if (currentElements != null) {
          final Set<RefEntity> currentEntities = new HashSet<RefEntity>(currentElements);
          if (RefUtil.contains(refElement, currentEntities)) return;
        }
      }
      addNodeToParent(container, presentation, pNode);
    }
  }

  private static class RefElementContainer implements UserObjectContainer<RefEntity> {
    @Nonnull
    private final RefEntity myElement;
    private final CommonProblemDescriptor[] myDescriptors;

    public RefElementContainer(@Nonnull RefEntity element, CommonProblemDescriptor[] descriptors) {
      myElement = element;
      myDescriptors = descriptors;
    }

    @Override
    @Nullable
    public RefElementContainer getOwner() {
      final RefEntity entity = myElement.getOwner();
      if (entity instanceof RefElement) {
        return new RefElementContainer(entity, myDescriptors);
      }
      return null;
    }

    @Nonnull
    @Override
    public RefElementNode createNode(@Nonnull InspectionToolPresentation presentation) {
      return new RefElementNode(myElement, presentation);
    }

    @Override
    @Nonnull
    public RefEntity getUserObject() {
      return myElement;
    }

    @Override
    @Nullable
    public String getModule() {
      final RefModule refModule = myElement instanceof RefElement
                                  ? ((RefElement)myElement).getModule()
                                  : myElement instanceof RefModule ? (RefModule)myElement : null;
      return refModule != null ? refModule.getName() : null;
    }

    @Override
    public boolean areEqual(final RefEntity o1, final RefEntity o2) {
      return Comparing.equal(o1, o2);
    }

    @Override
    public boolean supportStructure() {
      return myElement instanceof RefElement && !(myElement instanceof RefDirectory); //do not show structure for refModule and refPackage
    }

    public CommonProblemDescriptor[] getProblemDescriptors() {
      return myDescriptors;
    }
  }
}
