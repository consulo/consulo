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
 * User: anna
 * Date: 10-Jan-2007
 */
package consulo.ide.impl.idea.codeInspection.offlineViewer;

import consulo.ide.impl.idea.codeInspection.ex.GlobalInspectionContextImpl;
import consulo.ide.impl.idea.codeInspection.ex.InspectionRVContentProvider;
import consulo.ide.impl.idea.codeInspection.ex.QuickFixAction;
import consulo.ide.impl.idea.codeInspection.offline.OfflineProblemDescriptor;
import consulo.ide.impl.idea.codeInspection.ui.*;
import consulo.util.lang.Comparing;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.inspection.CommonProblemDescriptor;
import consulo.language.editor.inspection.QuickFix;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.language.editor.inspection.reference.SmartRefElementPointer;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.impl.inspection.scheme.LocalInspectionToolWrapper;
import consulo.project.Project;
import consulo.ui.ex.awt.tree.TreeUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.*;
import java.util.function.Function;

public class OfflineInspectionRVContentProvider extends InspectionRVContentProvider {
  private final Map<String, Map<String, Set<OfflineProblemDescriptor>>> myContent;

  public OfflineInspectionRVContentProvider(@Nonnull Map<String, Map<String, Set<OfflineProblemDescriptor>>> content, @Nonnull Project project) {
    super(project);
    myContent = content;
  }

  @Override
  public boolean checkReportedProblems(@Nonnull GlobalInspectionContextImpl context, @Nonnull final InspectionToolWrapper toolWrapper) {
    final Map<String, Set<OfflineProblemDescriptor>> content = getFilteredContent(context, toolWrapper);
    return content != null && !content.values().isEmpty();
  }

  @Override
  @Nullable
  public QuickFixAction[] getQuickFixes(@Nonnull final InspectionToolWrapper toolWrapper, @Nonnull final InspectionTree tree) {
    final TreePath[] treePaths = tree.getSelectionPaths();
    final List<RefEntity> selectedElements = new ArrayList<RefEntity>();
    final Map<RefEntity, Set<QuickFix>> actions = new HashMap<RefEntity, Set<QuickFix>>();
    for (TreePath selectionPath : treePaths) {
      TreeUtil.traverseDepth((TreeNode)selectionPath.getLastPathComponent(), new TreeUtil.Traverse() {
        @Override
        public boolean accept(final Object node) {
          if (!((InspectionTreeNode)node).isValid()) return true;
          if (node instanceof OfflineProblemDescriptorNode) {
            final OfflineProblemDescriptorNode descriptorNode = (OfflineProblemDescriptorNode)node;
            final RefEntity element = descriptorNode.getElement();
            selectedElements.add(element);
            Set<QuickFix> quickFixes = actions.get(element);
            if (quickFixes == null) {
              quickFixes = new HashSet<QuickFix>();
              actions.put(element, quickFixes);
            }
            final CommonProblemDescriptor descriptor = descriptorNode.getDescriptor();
            if (descriptor != null) {
              final QuickFix[] fixes = descriptor.getFixes();
              if (fixes != null) {
                ContainerUtil.addAll(quickFixes, fixes);
              }
            }
          }
          else if (node instanceof RefElementNode) {
            selectedElements.add(((RefElementNode)node).getElement());
          }
          return true;
        }
      });
    }

    if (selectedElements.isEmpty()) return null;

    final RefEntity[] selectedRefElements = selectedElements.toArray(new RefEntity[selectedElements.size()]);

    GlobalInspectionContextImpl context = tree.getContext();
    InspectionToolPresentation presentation = context.getPresentation(toolWrapper);
    return presentation.extractActiveFixes(selectedRefElements, actions);
  }

  @Override
  public boolean isContentLoaded() {
    return false;
  }

  @Override
  public void appendToolNodeContent(@Nonnull GlobalInspectionContextImpl context,
                                    @Nonnull final InspectionNode toolNode,
                                    @Nonnull final InspectionTreeNode parentNode,
                                    final boolean showStructure,
                                    @Nonnull final Map<String, Set<RefEntity>> contents,
                                    @Nonnull final Map<RefEntity, CommonProblemDescriptor[]> problems,
                                    final DefaultTreeModel model) {
    InspectionToolWrapper toolWrapper = toolNode.getToolWrapper();
    final Map<String, Set<OfflineProblemDescriptor>> filteredContent = getFilteredContent(context, toolWrapper);
    if (filteredContent != null && !filteredContent.values().isEmpty()) {
      final Function<OfflineProblemDescriptor, UserObjectContainer<OfflineProblemDescriptor>> computeContainer = descriptor -> new OfflineProblemDescriptorContainer(descriptor);
      final List<InspectionTreeNode> list = buildTree(context, filteredContent, false, toolWrapper, computeContainer, showStructure);
      for (InspectionTreeNode node : list) {
        toolNode.add(node);
      }
      parentNode.add(toolNode);
    }
  }

  @Nullable
  @SuppressWarnings({"UnusedAssignment"})
  private Map<String, Set<OfflineProblemDescriptor>> getFilteredContent(@Nonnull GlobalInspectionContextImpl context, @Nonnull InspectionToolWrapper toolWrapper) {
    Map<String, Set<OfflineProblemDescriptor>> content = myContent.get(toolWrapper.getShortName());
    if (content == null) return null;
    if (context.getUIOptions().FILTER_RESOLVED_ITEMS) {
      final Map<String, Set<OfflineProblemDescriptor>> current = new HashMap<String, Set<OfflineProblemDescriptor>>(content);
      content = null; //GC it
      InspectionToolPresentation presentation = context.getPresentation(toolWrapper);
      for (RefEntity refEntity : presentation.getIgnoredRefElements()) {
        if (refEntity instanceof RefElement) {
          excludeProblem(refEntity.getExternalName(), current);
        }
      }
      return current;
    }
    return content;
  }

  private static void excludeProblem(final String externalName, final Map<String, Set<OfflineProblemDescriptor>> content) {
    for (Iterator<String> iter = content.keySet().iterator(); iter.hasNext(); ) {
      final String packageName = iter.next();
      final Set<OfflineProblemDescriptor> excluded = new HashSet<OfflineProblemDescriptor>(content.get(packageName));
      for (Iterator<OfflineProblemDescriptor> it = excluded.iterator(); it.hasNext(); ) {
        final OfflineProblemDescriptor ex = it.next();
        if (Comparing.strEqual(ex.getFQName(), externalName)) {
          it.remove();
        }
      }
      if (excluded.isEmpty()) {
        iter.remove();
      }
      else {
        content.put(packageName, excluded);
      }
    }
  }

  @Override
  protected void appendDescriptor(@Nonnull GlobalInspectionContextImpl context,
                                  @Nonnull final InspectionToolWrapper toolWrapper,
                                  @Nonnull final UserObjectContainer container,
                                  @Nonnull final InspectionPackageNode packageNode,
                                  final boolean canPackageRepeat) {
    InspectionToolPresentation presentation = context.getPresentation(toolWrapper);
    final RefElementNode elemNode = addNodeToParent(container, presentation, packageNode);
    if (toolWrapper instanceof LocalInspectionToolWrapper) {
      elemNode.add(new OfflineProblemDescriptorNode(((OfflineProblemDescriptorContainer)container).getUserObject(), (LocalInspectionToolWrapper)toolWrapper, presentation));
    }
  }


  private static class OfflineProblemDescriptorContainer implements UserObjectContainer<OfflineProblemDescriptor> {
    @Nonnull
    private final OfflineProblemDescriptor myDescriptor;

    public OfflineProblemDescriptorContainer(@Nonnull OfflineProblemDescriptor descriptor) {
      myDescriptor = descriptor;
    }

    @Override
    @Nullable
    public OfflineProblemDescriptorContainer getOwner() {
      final OfflineProblemDescriptor descriptor = myDescriptor.getOwner();
      if (descriptor != null) {
        final OfflineProblemDescriptorContainer container = new OfflineProblemDescriptorContainer(descriptor);
        return container.supportStructure() ? container : null;
      }
      return null;
    }

    @Nonnull
    @Override
    public RefElementNode createNode(@Nonnull InspectionToolPresentation presentation) {
      return new OfflineRefElementNode(myDescriptor, presentation);
    }

    @Override
    @Nonnull
    public OfflineProblemDescriptor getUserObject() {
      return myDescriptor;
    }

    @Override
    public String getModule() {
      return myDescriptor.getModuleName();
    }

    @Override
    public boolean areEqual(final OfflineProblemDescriptor o1, final OfflineProblemDescriptor o2) {
      if (o1 == null || o2 == null) {
        return o1 == o2;
      }

      if (!Comparing.strEqual(o1.getFQName(), o2.getFQName())) return false;
      if (!Comparing.strEqual(o1.getType(), o2.getType())) return false;

      return true;
    }

    @Override
    public boolean supportStructure() {
      return !Comparing.strEqual(myDescriptor.getType(), SmartRefElementPointer.MODULE) &&
             !Comparing.strEqual(myDescriptor.getType(), "package") &&
             !Comparing.strEqual(myDescriptor.getType(), SmartRefElementPointer.PROJECT);
    }
  }
}
