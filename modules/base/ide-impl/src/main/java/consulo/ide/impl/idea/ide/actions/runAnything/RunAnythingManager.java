// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.disposer.Disposer;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.ide.actions.BigPopupUI;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.ui.IdeEventQueueProxy;
import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;
import consulo.project.ui.ProjectWindowStateService;
import consulo.ui.Coordinate2D;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.JBInsets;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.awt.*;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class RunAnythingManager {
  private static final String LOCATION_SETTINGS_KEY = "run.anything.popup";
  private final Project myProject;
  private JBPopup myBalloon;
  private RunAnythingPopupUI myRunAnythingUI;
  private Dimension myBalloonFullSize;
  @Nullable
  private String mySelectedText;

  @Inject
  public RunAnythingManager(@Nonnull Project project) {
    myProject = project;
  }

  public static RunAnythingManager getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, RunAnythingManager.class);
  }

  public void show(@Nullable String searchText, @Nonnull AnActionEvent initEvent) {
    show(searchText, true, initEvent);
  }

  public void show(@Nullable String searchText, boolean selectSearchText, @Nonnull AnActionEvent initEvent) {
    IdeEventQueueProxy.getInstance().closeAllPopups(false);

    Project project = initEvent.getData(CommonDataKeys.PROJECT);

    myRunAnythingUI = createView(initEvent);

    if (searchText != null && !searchText.isEmpty()) {
      myRunAnythingUI.getSearchField().setValue(searchText);
    }
    if (selectSearchText) {
      myRunAnythingUI.getSearchField().selectAll();
    }

    predefineSelectedText(searchText);

    myBalloon = JBPopupFactory.getInstance().createComponentPopupBuilder(myRunAnythingUI, (JComponent)TargetAWT.to(myRunAnythingUI.getSearchField())).setProject(myProject).setModalContext(false)
            .setCancelOnClickOutside(true).setRequestFocus(true).setCancelKeyEnabled(false).setCancelCallback(() -> {
              saveSearchText();
              return true;
            }).addUserData("SIMPLE_WINDOW").setResizable(true).setMovable(true).setDimensionServiceKey(myProject, LOCATION_SETTINGS_KEY, true).setLocateWithinScreenBounds(false).createPopup();
    Disposer.register(myBalloon, myRunAnythingUI);
    if (project != null) {
      Disposer.register(project, myBalloon);
    }

    Dimension size = myRunAnythingUI.getMinimumSize();
    JBInsets.addTo(size, myBalloon.getContent().getInsets());
    myBalloon.setMinimumSize(size);

    Disposer.register(myBalloon, () -> {
      saveSize();
      myRunAnythingUI = null;
      myBalloon = null;
      myBalloonFullSize = null;
    });

    if (myRunAnythingUI.getViewType() == RunAnythingPopupUI.ViewType.SHORT) {
      myBalloonFullSize = TargetAWT.to(ProjectWindowStateService.getInstance(myProject).getSize(LOCATION_SETTINGS_KEY));
      Dimension prefSize = myRunAnythingUI.getPreferredSize();
      myBalloon.setSize(prefSize);
    }
    calcPositionAndShow(project, myBalloon);
  }

  private void predefineSelectedText(@Nullable String searchText) {
    if (StringUtil.isEmpty(searchText)) {
      searchText = mySelectedText;
    }

    if (StringUtil.isNotEmpty(searchText)) {
      myRunAnythingUI.getSearchField().setValue(searchText);
      myRunAnythingUI.getSearchField().selectAll();
    }
  }

  private void saveSearchText() {
    if (!isShown()) {
      return;
    }

    mySelectedText = myRunAnythingUI.getSearchField().getValue();
  }

  private void calcPositionAndShow(Project project, JBPopup balloon) {
    Coordinate2D savedLocation = ProjectWindowStateService.getInstance(myProject).getLocation(LOCATION_SETTINGS_KEY);

    if (project != null) {
      balloon.showCenteredInCurrentWindow(project);
    }
    else {
      balloon.showInFocusCenter();
    }

    //for first show and short mode popup should be shifted to the top screen half
    if (savedLocation == null && myRunAnythingUI.getViewType() == RunAnythingPopupUI.ViewType.SHORT) {
      Point location = balloon.getLocationOnScreen();
      location.y /= 2;
      balloon.setLocation(location);
    }
  }

  public boolean isShown() {
    return myRunAnythingUI != null && myBalloon != null && !myBalloon.isDisposed();
  }

  @SuppressWarnings("Duplicates")
  @Nonnull
  private RunAnythingPopupUI createView(@Nonnull AnActionEvent event) {
    RunAnythingPopupUI view = new RunAnythingPopupUI(event);

    view.setSearchFinishedHandler(() -> {
      if (isShown()) {
        myBalloon.cancel();
      }
    });

    view.addViewTypeListener(viewType -> {
      if (!isShown()) {
        return;
      }

      ApplicationManager.getApplication().invokeLater(() -> {
        Dimension minSize = view.getMinimumSize();
        JBInsets.addTo(minSize, myBalloon.getContent().getInsets());
        myBalloon.setMinimumSize(minSize);

        if (viewType == BigPopupUI.ViewType.SHORT) {
          myBalloonFullSize = myBalloon.getSize();
          JBInsets.removeFrom(myBalloonFullSize, myBalloon.getContent().getInsets());
          myBalloon.pack(false, true);
        }
        else {
          if (myBalloonFullSize == null) {
            myBalloonFullSize = view.getPreferredSize();
            JBInsets.addTo(myBalloonFullSize, myBalloon.getContent().getInsets());
          }
          myBalloonFullSize.height = Integer.max(myBalloonFullSize.height, minSize.height);
          myBalloonFullSize.width = Integer.max(myBalloonFullSize.width, minSize.width);

          myBalloon.setSize(myBalloonFullSize);
        }
      });
    });

    return view;
  }

  private void saveSize() {
    if (myRunAnythingUI.getViewType() == RunAnythingPopupUI.ViewType.SHORT) {
      ProjectWindowStateService.getInstance(myProject).putSize(LOCATION_SETTINGS_KEY, TargetAWT.from(myBalloonFullSize));
    }
  }
}
