package consulo.ide.impl.idea.codeInspection.ui.actions;

import consulo.application.CommonBundle;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.inspection.scheme.ModifiableModel;
import consulo.ide.impl.idea.codeInspection.actions.RunInspectionIntention;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.ide.impl.idea.codeInspection.ui.InspectionResultsView;
import consulo.ide.impl.idea.codeInspection.ui.InspectionTree;
import consulo.application.AllIcons;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.dataContext.DataContext;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ide.impl.idea.profile.codeInspection.InspectionProjectProfileManager;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: anna
 * Date: 11-Jan-2006
 */
public class InspectionsOptionsToolbarAction extends AnAction {
  private final InspectionResultsView myView;

  public InspectionsOptionsToolbarAction(final InspectionResultsView view) {
    super(getToolOptions(null), getToolOptions(null), AllIcons.General.InspectionsTrafficOff);
    myView = view;
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final DefaultActionGroup options = new DefaultActionGroup();
    final List<AnAction> actions = createActions();
    for (AnAction action : actions) {
      options.add(action);
    }
    final DataContext dataContext = e.getDataContext();
    final ListPopup popup = JBPopupFactory.getInstance()
      .createActionGroupPopup(getSelectedToolWrapper().getDisplayName(), options, dataContext,
                              JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
    InspectionResultsView.showPopup(e, popup);
  }

  @Nullable
  private InspectionToolWrapper getSelectedToolWrapper() {
    return myView.getTree().getSelectedToolWrapper();
  }

  @Override
  public void update(AnActionEvent e) {
    if (!myView.isSingleToolInSelection()) {
      e.getPresentation().setEnabled(false);
      return;
    }
    InspectionToolWrapper toolWrapper = getSelectedToolWrapper();
    assert toolWrapper != null;
    final HighlightDisplayKey key = HighlightDisplayKey.find(toolWrapper.getShortName());
    if (key == null) {
      e.getPresentation().setEnabled(false);
    }
    e.getPresentation().setEnabled(true);
    final String text = getToolOptions(toolWrapper);
    e.getPresentation().setText(text);
    e.getPresentation().setDescription(text);
  }

  @Nonnull
  private static String getToolOptions(@Nullable final InspectionToolWrapper toolWrapper) {
    return InspectionsBundle.message("inspections.view.options.title", toolWrapper != null ? toolWrapper.getDisplayName() : "");
  }

  public List<AnAction> createActions() {
    final List<AnAction> result = new ArrayList<AnAction>();
    final InspectionTree tree = myView.getTree();
    final InspectionToolWrapper toolWrapper = tree.getSelectedToolWrapper();
    if (toolWrapper == null) return result;
    final HighlightDisplayKey key = HighlightDisplayKey.find(toolWrapper.getShortName());
    if (key == null) return result;

    result.add(new DisableInspectionAction(key));

    result.add(new AnAction(InspectionsBundle.message("run.inspection.on.file.intention.text")) {
      @Override
      public void actionPerformed(final AnActionEvent e) {
        final PsiElement psiElement = getPsiElement(tree);
        assert psiElement != null;
        new RunInspectionIntention(toolWrapper).invoke(myView.getProject(), null, psiElement.getContainingFile());
      }

      @Override
      public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(getPsiElement(tree) != null);
      }

      @Nullable
      private PsiElement getPsiElement(InspectionTree tree) {
        final RefEntity[] selectedElements = tree.getSelectedElements();

        final PsiElement psiElement;
        if (selectedElements.length > 0 && selectedElements[0] instanceof RefElement) {
          psiElement = ((RefElement)selectedElements[0]).getPsiElement();
        }
        else {
          psiElement = null;
        }
        return psiElement;
      }
    });

    result.add(new SuppressActionWrapper(myView.getProject(), toolWrapper, tree.getSelectionPaths()));


    return result;
  }

  private class DisableInspectionAction extends AnAction {
    private final HighlightDisplayKey myKey;

    public DisableInspectionAction(final HighlightDisplayKey key) {
      super(InspectionsBundle.message("disable.inspection.action.name"));
      myKey = key;
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
      try {
        if (myView.isProfileDefined()) {
          final ModifiableModel model = myView.getCurrentProfile().getModifiableModel();
          model.disableTool(myKey.toString(), myView.getProject());
          model.commit();
          myView.updateCurrentProfile();
        } else {
          final RefEntity[] selectedElements = myView.getTree().getSelectedElements();
          final Set<PsiElement> files = new HashSet<PsiElement>();
          final Project project = myView.getProject();
          final InspectionProjectProfileManager profileManager = InspectionProjectProfileManager.getInstance(project);
          for (RefEntity selectedElement : selectedElements) {
            if (selectedElement instanceof RefElement) {
              final PsiElement element = ((RefElement)selectedElement).getPsiElement();
              files.add(element);
            }
          }
          ModifiableModel model = ((InspectionProfileImpl)profileManager.getProjectProfileImpl()).getModifiableModel();
          for (PsiElement element : files) {
            model.disableTool(myKey.toString(), element);
          }
          model.commit();
          DaemonCodeAnalyzer.getInstance(project).restart();
        }
      }
      catch (IOException e1) {
        Messages.showErrorDialog(myView.getProject(), e1.getMessage(), CommonBundle.getErrorTitle());
      }
    }
  }
}
