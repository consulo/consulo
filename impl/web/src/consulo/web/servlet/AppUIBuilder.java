package consulo.web.servlet;

import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
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
  protected void build(@NotNull Window window) {
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

    List<String> c = new ArrayList<String>();
    for (int i = 0; i < 200; i++) {
      c.add("Some: " + i);
    }

    WGwtListBoxImpl<String> list = new WGwtListBoxImpl<String>(new ImmutableListModel<String>(c));
    list.addValueListener(new ValueComponent.ValueListener<String>() {
      @Override
      public void valueChanged(@NotNull ValueComponent.ValueEvent<String> event) {
        System.out.println(event.getValue() + " selected");

        modalWindow.setVisible(false);
      }
    });
    modalWindow.setContent(Layouts.horizontalSplit().setFirstComponent(list).setSecondComponent(Components.label("Some labe")));
    modalWindow.setVisible(true);

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
}
