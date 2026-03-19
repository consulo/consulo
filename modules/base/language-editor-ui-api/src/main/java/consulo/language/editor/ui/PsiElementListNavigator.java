// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.ui;

import consulo.application.progress.ProgressManager;
import consulo.codeEditor.Editor;
import consulo.language.editor.ui.internal.LanguageEditorPopupFactory;
import consulo.language.editor.ui.navigation.BackgroundUpdaterTask;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.popup.IPopupChooserBuilder;
import consulo.ui.ex.popup.JBPopup;

import org.jspecify.annotations.Nullable;
import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class PsiElementListNavigator {
  private PsiElementListNavigator() {
  }

  public static void openTargets(MouseEvent e, NavigatablePsiElement[] targets, String title, String findUsagesTitle, ListCellRenderer listRenderer) {
    openTargets(e, targets, title, findUsagesTitle, listRenderer, (BackgroundUpdaterTask)null);
  }

  public static void openTargets(MouseEvent e,
                                 NavigatablePsiElement[] targets,
                                 String title,
                                 String findUsagesTitle,
                                 ListCellRenderer listRenderer,
                                 @Nullable BackgroundUpdaterTask listUpdaterTask) {
    JBPopup popup = navigateOrCreatePopup(targets, title, findUsagesTitle, listRenderer, listUpdaterTask);
    if (popup != null) {
      RelativePoint point = new RelativePoint(e);
      if (listUpdaterTask != null) {
        runActionAndListUpdaterTask(() -> popup.show(point), listUpdaterTask);
      }
      else {
        popup.show(point);
      }
    }
  }

  public static void openTargets(Editor e, NavigatablePsiElement[] targets, String title, String findUsagesTitle, ListCellRenderer listRenderer) {
    openTargets(e, targets, title, findUsagesTitle, listRenderer, null);
  }

  public static void openTargets(Editor e,
                                 NavigatablePsiElement[] targets,
                                 String title,
                                 String findUsagesTitle,
                                 ListCellRenderer listRenderer,
                                 @Nullable BackgroundUpdaterTask listUpdaterTask) {
    JBPopup popup = navigateOrCreatePopup(targets, title, findUsagesTitle, listRenderer, listUpdaterTask);
    if (popup != null) {
      if (listUpdaterTask != null) {
        runActionAndListUpdaterTask(() -> e.showPopupInBestPositionFor(popup), listUpdaterTask);
      }
      else {
        e.showPopupInBestPositionFor(popup);
      }
    }
  }

  /**
   * @see #navigateOrCreatePopup(NavigatablePsiElement[], String, String, ListCellRenderer, BackgroundUpdaterTask, Consumer)
   */
  private static void runActionAndListUpdaterTask(Runnable action, BackgroundUpdaterTask listUpdaterTask) {
    action.run();
    ProgressManager.getInstance().run(listUpdaterTask);
  }

  public static @Nullable JBPopup navigateOrCreatePopup(NavigatablePsiElement[] targets,
                                              String title,
                                              String findUsagesTitle,
                                              ListCellRenderer listRenderer,
                                              @Nullable BackgroundUpdaterTask listUpdaterTask) {
    return navigateOrCreatePopup(targets, title, findUsagesTitle, listRenderer, listUpdaterTask, selectedElements -> {
      for (Object element : selectedElements) {
        PsiElement selected = (PsiElement)element;
        if (selected.isValid()) {
          ((NavigatablePsiElement)selected).navigate(true);
        }
      }
    });
  }

  /**
   * listUpdaterTask should be started after alarm is initialized so one-item popup won't blink
   */
  public static @Nullable JBPopup navigateOrCreatePopup(NavigatablePsiElement[] targets,
                                              String title,
                                              String findUsagesTitle,
                                              ListCellRenderer listRenderer,
                                              @Nullable BackgroundUpdaterTask listUpdaterTask,
                                              Consumer<Object[]> consumer) {
    return builder(targets, title).setFindUsagesTitle(findUsagesTitle).setListRenderer(listRenderer).setListUpdaterTask(listUpdaterTask).setTargetsConsumer(consumer).build();
  }

  private static NavigateOrPopupBuilder builder(NavigatablePsiElement[] targets, String title) {
    return LanguageEditorPopupFactory.getInstance().builder(targets, title);
  }

  // Helper makes it easier to customize shown popup.
  public static abstract class NavigateOrPopupBuilder {

    
    protected final NavigatablePsiElement[] myTargets;

    protected final String myTitle;

    
    protected Consumer<Object[]> myTargetsConsumer;

    protected @Nullable String myFindUsagesTitle;

    protected @Nullable ListCellRenderer myListRenderer;

    protected @Nullable BackgroundUpdaterTask myListUpdaterTask;

    protected @Nullable Project myProject;

    public NavigateOrPopupBuilder(NavigatablePsiElement[] targets, String title) {
      myTargets = targets;
      myTitle = title;
      myTargetsConsumer = selectedElements -> {
        for (Object element : selectedElements) {
          PsiElement selected = (PsiElement)element;
          if (selected.isValid()) {
            ((NavigatablePsiElement)selected).navigate(true);
          }
        }
      };
    }

    
    public NavigateOrPopupBuilder setFindUsagesTitle(@Nullable String findUsagesTitle) {
      myFindUsagesTitle = findUsagesTitle;
      return this;
    }

    
    public NavigateOrPopupBuilder setListRenderer(@Nullable ListCellRenderer listRenderer) {
      myListRenderer = listRenderer;
      return this;
    }

    
    public NavigateOrPopupBuilder setListUpdaterTask(@Nullable BackgroundUpdaterTask listUpdaterTask) {
      myListUpdaterTask = listUpdaterTask;
      return this;
    }

    
    public NavigateOrPopupBuilder setTargetsConsumer(Consumer<Object[]> targetsConsumer) {
      myTargetsConsumer = targetsConsumer;
      return this;
    }

    
    public NavigateOrPopupBuilder setProject(@Nullable Project project) {
      myProject = project;
      return this;
    }

    public abstract @Nullable JBPopup build();

    
    public Project getProject() {
      if (myProject != null) {
        return myProject;
      }
      assert !allowEmptyTargets() : "Project was not set and cannot be taken from targets";
      return myTargets[0].getProject();
    }

    protected boolean allowEmptyTargets() {
      return false;
    }

    protected void afterPopupBuilderCreated(IPopupChooserBuilder<NavigatablePsiElement> builder) {
      // Do nothing by default
    }
  }
}
