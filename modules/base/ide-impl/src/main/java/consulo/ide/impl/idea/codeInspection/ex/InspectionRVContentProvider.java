/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.application.ApplicationManager;
import consulo.ide.impl.idea.codeInspection.ui.*;
import consulo.language.editor.inspection.CommonProblemDescriptor;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.util.lang.ref.Ref;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.*;
import java.util.function.Function;

/**
 * @author anna
 * @since 2007-01-10
 */
public abstract class InspectionRVContentProvider {
  private static final Logger LOG = Logger.getInstance(InspectionRVContentProvider.class);
  private final Project myProject;

  public InspectionRVContentProvider(@Nonnull Project project) {
    myProject = project;
  }

  protected interface UserObjectContainer<T> {
    @Nullable
    UserObjectContainer<T> getOwner();

    @Nonnull
    RefElementNode createNode(@Nonnull InspectionToolPresentation presentation);

    @Nonnull
    T getUserObject();

    @Nullable
    String getModule();

    boolean areEqual(T o1, T o2);

    boolean supportStructure();
  }

  public abstract boolean checkReportedProblems(@Nonnull GlobalInspectionContextImpl context, @Nonnull InspectionToolWrapper toolWrapper);

  @Nullable
  public abstract QuickFixAction[] getQuickFixes(@Nonnull InspectionToolWrapper toolWrapper, @Nonnull InspectionTree tree);


  public void appendToolNodeContent(@Nonnull GlobalInspectionContextImpl context,
                                    @Nonnull InspectionNode toolNode,
                                    @Nonnull InspectionTreeNode parentNode,
                                    boolean showStructure) {
    InspectionToolWrapper wrapper = toolNode.getToolWrapper();
    InspectionToolPresentation presentation = context.getPresentation(wrapper);
    Map<String, Set<RefEntity>> content = presentation.getContent();
    Map<RefEntity, CommonProblemDescriptor[]> problems = presentation.getProblemElements();
    Map<String, Set<RefEntity>> contents = content == null ? new HashMap<String, Set<RefEntity>>() : content;
    appendToolNodeContent(context, toolNode, parentNode, showStructure, contents, problems, null);
  }

  public abstract void appendToolNodeContent(@Nonnull GlobalInspectionContextImpl context,
                                             @Nonnull InspectionNode toolNode,
                                             @Nonnull InspectionTreeNode parentNode,
                                             boolean showStructure,
                                             @Nonnull Map<String, Set<RefEntity>> contents,
                                             @Nonnull Map<RefEntity, CommonProblemDescriptor[]> problems,
                                             @Nullable DefaultTreeModel model);

  protected abstract void appendDescriptor(@Nonnull GlobalInspectionContextImpl context,
                                           @Nonnull InspectionToolWrapper toolWrapper,
                                           @Nonnull UserObjectContainer container,
                                           @Nonnull InspectionPackageNode pNode,
                                           boolean canPackageRepeat);

  public boolean isContentLoaded() {
    return true;
  }

  protected <T> List<InspectionTreeNode> buildTree(@Nonnull GlobalInspectionContextImpl context,
                                                   @Nonnull Map<String, Set<T>> packageContents,
                                                   boolean canPackageRepeat,
                                                   @Nonnull InspectionToolWrapper toolWrapper,
                                                   @Nonnull Function<T, UserObjectContainer<T>> computeContainer,
                                                   boolean showStructure) {
    List<InspectionTreeNode> content = new ArrayList<InspectionTreeNode>();
    Map<String, Map<String, InspectionPackageNode>> module2PackageMap = new HashMap<String, Map<String, InspectionPackageNode>>();
    boolean supportStructure = showStructure;
    for (String packageName : packageContents.keySet()) {
      Set<T> elements = packageContents.get(packageName);
      for (T userObject : elements) {
        UserObjectContainer<T> container = computeContainer.apply(userObject);
        supportStructure &= container.supportStructure();
        String moduleName = showStructure ? container.getModule() : null;
        Map<String, InspectionPackageNode> packageNodes = module2PackageMap.get(moduleName);
        if (packageNodes == null) {
          packageNodes = new HashMap<String, InspectionPackageNode>();
          module2PackageMap.put(moduleName, packageNodes);
        }
        InspectionPackageNode pNode = packageNodes.get(packageName);
        if (pNode == null) {
          pNode = new InspectionPackageNode(packageName);
          packageNodes.put(packageName, pNode);
        }
        appendDescriptor(context, toolWrapper, container, pNode, canPackageRepeat);
      }
    }
    if (supportStructure) {
      HashMap<String, InspectionModuleNode> moduleNodes = new HashMap<String, InspectionModuleNode>();
      for (String moduleName : module2PackageMap.keySet()) {
        Map<String, InspectionPackageNode> packageNodes = module2PackageMap.get(moduleName);
        for (InspectionPackageNode packageNode : packageNodes.values()) {
          if (packageNode.getChildCount() > 0) {
            InspectionModuleNode moduleNode = moduleNodes.get(moduleName);
            if (moduleNode == null) {
              if (moduleName != null) {
                Module module = ModuleManager.getInstance(myProject).findModuleByName(moduleName);
                if (module != null) {
                  moduleNode = new InspectionModuleNode(module);
                  moduleNodes.put(moduleName, moduleNode);
                }
                else { //module content was removed ?
                  continue;
                }
              } else {
                content.addAll(packageNodes.values());
                break;
              }
            }
            if (packageNode.getPackageName() != null) {
              moduleNode.add(packageNode);
            } else {
              for(int i = packageNode.getChildCount() - 1; i >= 0; i--) {
                moduleNode.add((MutableTreeNode)packageNode.getChildAt(i));
              }
            }
          }
        }
      }
      content.addAll(moduleNodes.values());
    }
    else {
      for (Map<String, InspectionPackageNode> packageNodes : module2PackageMap.values()) {
        for (InspectionPackageNode pNode : packageNodes.values()) {
          for (int i = 0; i < pNode.getChildCount(); i++) {
            TreeNode childNode = pNode.getChildAt(i);
            if (childNode instanceof ProblemDescriptionNode) {
              content.add(pNode);
              break;
            }
            LOG.assertTrue(childNode instanceof RefElementNode, childNode.getClass().getName());
            RefElementNode elementNode = (RefElementNode)childNode;
            Set<RefElementNode> parentNodes = new LinkedHashSet<RefElementNode>();
            if (pNode.getPackageName() != null) {
              parentNodes.add(elementNode);
            } else {
              boolean hasElementNodeUnder = true;
              for(int e = 0; e < elementNode.getChildCount(); e++) {
                TreeNode grandChildNode = elementNode.getChildAt(e);
                if (grandChildNode instanceof ProblemDescriptionNode) {
                  hasElementNodeUnder = false;
                  break;
                }
                LOG.assertTrue(grandChildNode instanceof RefElementNode);
                parentNodes.add((RefElementNode)grandChildNode);
              }
              if (!hasElementNodeUnder) {
                content.add(elementNode);
                continue;
              }
            }
            for (RefElementNode parentNode : parentNodes) {
              final List<ProblemDescriptionNode> nodes = new ArrayList<ProblemDescriptionNode>();
              TreeUtil.traverse(parentNode, new TreeUtil.Traverse() {
                @Override
                public boolean accept(Object node) {
                  if (node instanceof ProblemDescriptionNode) {
                    nodes.add((ProblemDescriptionNode)node);
                  }
                  return true;
                }
              });
              if (nodes.isEmpty()) continue;  //FilteringInspectionTool == DeadCode
              parentNode.removeAllChildren();
              for (ProblemDescriptionNode node : nodes) {
                parentNode.add(node);
              }
            }
            content.addAll(parentNodes);
          }
        }
      }
    }
    return content;
  }

  @Nonnull
  protected static RefElementNode addNodeToParent(@Nonnull UserObjectContainer container,
                                                  @Nonnull InspectionToolPresentation presentation,
                                                  InspectionTreeNode parentNode) {
    final RefElementNode nodeToBeAdded = container.createNode(presentation);
    final Ref<Boolean> firstLevel = new Ref<Boolean>(true);
    RefElementNode prevNode = null;
    final Ref<RefElementNode> result = new Ref<RefElementNode>();
    while (true) {
      RefElementNode currentNode = firstLevel.get() ? nodeToBeAdded : container.createNode(presentation);
      final UserObjectContainer finalContainer = container;
      final RefElementNode finalPrevNode = prevNode;
      TreeUtil.traverseDepth(parentNode, new TreeUtil.Traverse() {
        @Override
        public boolean accept(Object node) {
          if (node instanceof RefElementNode) {
            RefElementNode refElementNode = (RefElementNode)node;
            if (finalContainer.areEqual(refElementNode.getUserObject(), finalContainer.getUserObject())) {
              if (firstLevel.get()) {
                result.set(refElementNode);
                return false;
              }
              else {
                insertByIndex(finalPrevNode, refElementNode);
                result.set(nodeToBeAdded);
                return false;
              }
            }
          }
          return true;
        }
      });
      if(!result.isNull()) return result.get();

      if (!firstLevel.get()) {
        insertByIndex(prevNode, currentNode);
      }
      UserObjectContainer owner = container.getOwner();
      if (owner == null) {
        insertByIndex(currentNode, parentNode);
        return nodeToBeAdded;
      }
      container = owner;
      prevNode = currentNode;
      firstLevel.set(false);
    }
  }

  @SuppressWarnings({"ConstantConditions"}) //class cast suppression
  protected static void merge(@Nullable DefaultTreeModel model, InspectionTreeNode child, InspectionTreeNode parent, boolean merge) {
    if (merge) {
      for (int i = 0; i < parent.getChildCount(); i++) {
        InspectionTreeNode current = (InspectionTreeNode)parent.getChildAt(i);
        if (child.getClass() != current.getClass()) {
          continue;
        }
        if (current instanceof InspectionPackageNode) {
          if (((InspectionPackageNode)current).getPackageName().compareTo(((InspectionPackageNode)child).getPackageName()) == 0) {
            processDepth(model, child, current);
            return;
          }
        }
        else if (current instanceof RefElementNode) {
          if (((RefElementNode)current).getElement().getName().compareTo(((RefElementNode)child).getElement().getName()) == 0) {
            processDepth(model, child, current);
            return;
          }
        }
        else if (current instanceof InspectionNode) {
          if (((InspectionNode)current).getToolWrapper().getShortName().compareTo(((InspectionNode)child).getToolWrapper().getShortName()) == 0) {
            processDepth(model, child, current);
            return;
          }
        }
        else if (current instanceof InspectionModuleNode) {
          if (((InspectionModuleNode)current).getName().compareTo(((InspectionModuleNode)child).getName()) == 0) {
            processDepth(model, child, current);
            return;
          }
        }
      }
    }
    add(model, child, parent);
  }

  protected static void add(@Nullable DefaultTreeModel model, InspectionTreeNode child, InspectionTreeNode parent) {
    if (model == null) {
      insertByIndex(child, parent);
    }
    else {
      if (parent.getIndex(child) < 0) {
        model.insertNodeInto(child, parent, child.getParent() == parent ? parent.getChildCount() - 1 : parent.getChildCount());
      }
    }
  }

  private static void insertByIndex(InspectionTreeNode child, InspectionTreeNode parent) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      parent.add(child);
      return;
    }
    int i = TreeUtil.indexedBinarySearch(parent, child, InspectionResultsViewComparator.getInstance());
    if (i >= 0){
      parent.add(child);
      return;
    }
    parent.insert(child, -i -1);
  }

  private static void processDepth(@Nullable DefaultTreeModel model, InspectionTreeNode child, InspectionTreeNode current) {
    InspectionTreeNode[] children = new InspectionTreeNode[child.getChildCount()];
    for (int i = 0; i < children.length; i++) {
      children[i] = (InspectionTreeNode)child.getChildAt(i);
    }
    for (InspectionTreeNode node : children) {
      merge(model, node, current, true);
    }
  }
}
