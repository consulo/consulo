package consulo.web.servlet;

import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.TimeoutUtil;
import consulo.ui.*;
import consulo.ui.internal.WGwtListBoxImpl;
import consulo.ui.internal.WGwtModalWindowImpl;
import consulo.ui.model.ImmutableListModel;
import consulo.ui.shared.Size;
import consulo.web.AppInit;
import consulo.web.servlet.ui.UIBuilder;
import consulo.web.servlet.ui.UIServlet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.annotation.WebServlet;
import java.util.ArrayList;
import java.util.List;

public class AppUIBuilder extends UIBuilder {
  @WebServlet("/app")
  public static class Servlet extends UIServlet {
    public Servlet() {
      super(AppUIBuilder.class);
    }
  }

  @Override
  protected void build(@NotNull final Window window) {
    ApplicationEx application = ApplicationManagerEx.getApplicationEx();
    if (application == null || !application.isLoaded()) {
      AppInit.initApplication();

      while (true) {
        application = ApplicationManagerEx.getApplicationEx();
        if (application != null && application.isLoaded()) {
          break;
        }

        TimeoutUtil.sleep(500L);
      }
    }

    final WGwtModalWindowImpl modalWindow = new WGwtModalWindowImpl();
    modalWindow.setSize(new Size(777, 460));

    List<String> projectList = new ArrayList<String>();
    projectList.add("R:/_github.com/consulo/mssdw");

    final Ref<String> selected = Ref.create();
    WGwtListBoxImpl<String> listBox = new WGwtListBoxImpl<String>(new ImmutableListModel<String>(projectList));
    listBox.addValueListener(new ValueComponent.ValueListener<String>() {
      @Override
      public void valueChanged(@NotNull ValueComponent.ValueEvent<String> event) {
        selected.set(event.getValue());

        modalWindow.hide(true);
      }
    });

    modalWindow.setContent(Layouts.horizontalSplit().setFirstComponent(listBox).setSecondComponent(Components.label("Choose project")));
    modalWindow.show(new Runnable() {
      @Override
      public void run() {
        final String projectPath = selected.get();
        System.out.println(projectPath + " selected");

        final Project project = getOrLoadProject(projectPath);
        if (project == null) {
          System.out.println("project is null");
          return;
        }

        buildContent(window, project);
      }
    });
  }

  private void buildContent(@NotNull Window window, @NotNull Project project) {
    final Menu file = MenuItems.menu("File");
    file.add(MenuItems.menu("New").add(MenuItems.item("Class")));
    file.separate();
    file.add(MenuItems.item("Exit"));

    window.setMenuBar(MenuItems.menuBar().add(file).add(MenuItems.item("Help")));

    final SplitLayout splitLayout = Layouts.horizontalSplit();
    final TabbedLayout tabbed = Layouts.tabbed();

    final VerticalLayout vertical = Layouts.vertical();

    BooleanValueGroup group = new BooleanValueGroup();

    final RadioButton component = Components.radioButton("Test 1", true);
    vertical.add(component);
    final RadioButton component1 = Components.radioButton("Test 2");
    vertical.add(component1);

    group.add(component).add(component1);

    tabbed.addTab("Hello", vertical);

    final LabeledLayout labeled = Layouts.labeled("Some Panel Label");
    tabbed.addTab("Hello2", labeled.set(Components.label("test 1")));

    splitLayout.setFirstComponent(Components.label("tree"));
    splitLayout.setSecondComponent(tabbed);
    splitLayout.setProportion(20);

    window.setContent(splitLayout);
  }

  @Nullable
  private Project getOrLoadProject(String path) {
    final VirtualFile fileByPath = LocalFileSystem.getInstance().findFileByPath(path);
    if (fileByPath == null) {
      return null;
    }
    try {
      final Project project;
      ProjectManagerEx projectManager = ProjectManagerEx.getInstanceEx();
      Project[] openProjects = projectManager.getOpenProjects();
      for (Project temp : openProjects) {
        if (fileByPath.equals(temp.getBaseDir())) {
          return temp;
        }
      }

      project = projectManager.loadProject(path);
      if (project == null) {
        return null;
      }
      projectManager.openTestProject(project);
      final StartupManagerImpl startupManager = (StartupManagerImpl)StartupManager.getInstance(project);
      startupManager.runStartupActivities();
      startupManager.startCacheUpdate();
      return project;
    }
    catch (Exception e) {
      e.getMessage();
    }
    return null;
  }
}
