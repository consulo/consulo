/*
 * User: anna
 * Date: 29-Jan-2007
 */
package consulo.ide.impl.idea.codeInspection.ui.actions;

import consulo.application.ApplicationManager;
import consulo.ide.impl.idea.codeInspection.ex.GlobalInspectionContextImpl;
import consulo.ide.impl.idea.codeInspection.ex.InspectionManagerEx;
import consulo.ide.impl.idea.codeInspection.ui.InspectionTreeNode;
import consulo.ide.impl.idea.codeInspection.ui.ProblemDescriptionNode;
import consulo.ide.impl.idea.codeInspection.ui.RefElementNode;
import consulo.language.editor.inspection.CommonProblemDescriptor;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.SuppressIntentionActionFromFix;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.intention.SuppressIntentionAction;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CompactActionGroup;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.HashSet;
import java.util.Set;

public class SuppressActionWrapper extends ActionGroup implements CompactActionGroup {
  private final Project myProject;
  private final InspectionManagerEx myManager;
  private final Set<InspectionTreeNode> myNodesToSuppress = new HashSet<InspectionTreeNode>();
  private final InspectionToolWrapper myToolWrapper;
  private static final Logger LOG = Logger.getInstance(SuppressActionWrapper.class);

  public SuppressActionWrapper(@Nonnull final Project project, @Nonnull final InspectionToolWrapper toolWrapper, @Nonnull final TreePath[] paths) {
    super(InspectionsBundle.message("suppress.inspection.problem"), false);
    myProject = project;
    myManager = (InspectionManagerEx)InspectionManager.getInstance(myProject);
    for (TreePath path : paths) {
      final Object node = path.getLastPathComponent();
      if (!(node instanceof TreeNode)) continue;
      TreeUtil.traverse((TreeNode)node, node1 -> {    //fetch leaves
        final InspectionTreeNode n = (InspectionTreeNode)node1;
        if (n.isLeaf()) {
          myNodesToSuppress.add(n);
        }
        return true;
      });
    }
    myToolWrapper = toolWrapper;
  }

  @Override
  @Nonnull
  public SuppressTreeAction[] getChildren(@Nullable final AnActionEvent e) {
    final SuppressIntentionAction[] suppressActions = InspectionManagerEx.getSuppressActions(myToolWrapper);
    if (suppressActions == null || suppressActions.length == 0) return new SuppressTreeAction[0];
    final SuppressTreeAction[] actions = new SuppressTreeAction[suppressActions.length];
    for (int i = 0; i < suppressActions.length; i++) {
      final SuppressIntentionAction suppressAction = suppressActions[i];
      actions[i] = new SuppressTreeAction(suppressAction);
    }
    return actions;
  }

  private boolean suppress(final PsiElement element,
                           final CommonProblemDescriptor descriptor,
                           final SuppressIntentionAction action,
                           final RefEntity refEntity) {
    final PsiModificationTracker tracker = PsiManager.getInstance(myProject).getModificationTracker();
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();
        try {
          final long startModificationCount = tracker.getModificationCount();

          PsiElement container = null;
          if (action instanceof SuppressIntentionActionFromFix) {
            container = ((SuppressIntentionActionFromFix)action).getContainer(element);
          }
          if (container == null) {
            container = element;
          }

          if (action.isAvailable(myProject, null, element)) {
            action.invoke(myProject, null, element);
          }
          if (startModificationCount != tracker.getModificationCount()) {
            final Set<GlobalInspectionContextImpl> globalInspectionContexts = myManager.getRunningContexts();
            for (GlobalInspectionContextImpl context : globalInspectionContexts) {
              context.ignoreElement(myToolWrapper.getTool(), container);
              if (descriptor != null) {
                context.getPresentation(myToolWrapper).ignoreCurrentElementProblem(refEntity, descriptor);
              }
            }
          }
        }
        catch (IncorrectOperationException e1) {
          LOG.error(e1);
        }
      }
    });
    return true;
  }

  @Override
  public void update(final AnActionEvent e) {
    super.update(e);
    e.getPresentation().setEnabled(InspectionManagerEx.getSuppressActions(myToolWrapper) != null);
  }

  private static Pair<PsiElement, CommonProblemDescriptor> getContentToSuppress(InspectionTreeNode node) {
    RefElement refElement = null;
    CommonProblemDescriptor descriptor = null;
    if (node instanceof RefElementNode) {
      final RefElementNode elementNode = (RefElementNode)node;
      final RefEntity element = elementNode.getElement();
      refElement = element instanceof RefElement ? (RefElement)element : null;
      descriptor = elementNode.getProblem();
    }
    else if (node instanceof ProblemDescriptionNode) {
      final ProblemDescriptionNode descriptionNode = (ProblemDescriptionNode)node;
      final RefEntity element = descriptionNode.getElement();
      refElement = element instanceof RefElement ? (RefElement)element : null;
      descriptor = descriptionNode.getDescriptor();
    }
    PsiElement element =
            descriptor instanceof ProblemDescriptor ? ((ProblemDescriptor)descriptor).getPsiElement() : refElement != null ? refElement.getPsiElement() : null;
    return Pair.create(element, descriptor);
  }

  public class SuppressTreeAction extends AnAction {
    private final SuppressIntentionAction mySuppressAction;

    public SuppressTreeAction(final SuppressIntentionAction suppressAction) {
      super(suppressAction.getText());
      mySuppressAction = suppressAction;
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          CommandProcessor.getInstance().executeCommand(myProject, () -> {
            for (InspectionTreeNode node : myNodesToSuppress) {
              final Pair<PsiElement, CommonProblemDescriptor> content = getContentToSuppress(node);
              if (content.first == null) break;
              final PsiElement element = content.first;
              RefEntity refEntity = null;
              if (node instanceof RefElementNode) {
                refEntity = ((RefElementNode)node).getElement();
              }
              else if (node instanceof ProblemDescriptionNode) {
                refEntity = ((ProblemDescriptionNode)node).getElement();
              }
              if (!suppress(element, content.second, mySuppressAction, refEntity)) break;
            }
            final Set<GlobalInspectionContextImpl> globalInspectionContexts = myManager.getRunningContexts();
            for (GlobalInspectionContextImpl context : globalInspectionContexts) {
              context.refreshViews();
            }
            CommandProcessor.getInstance().markCurrentCommandAsGlobal(myProject);
          }, getTemplatePresentation().getText(), null);
        }
      });
    }

    @Override
    public void update(final AnActionEvent e) {
      super.update(e);
      if (!isAvailable()) e.getPresentation().setVisible(false);
    }

    public boolean isAvailable() {
      for (InspectionTreeNode node : myNodesToSuppress) {
        final Pair<PsiElement, CommonProblemDescriptor> content = getContentToSuppress(node);
        if (content.first == null) continue;
        final PsiElement element = content.first;
        if (mySuppressAction.isAvailable(myProject, null, element)) {
          return true;
        }
      }
      return false;
    }
  }
}
