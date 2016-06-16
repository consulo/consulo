package consulo.web.servlet;

import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.util.TimeoutUtil;
import consulo.ui.Components;
import consulo.ui.Layouts;
import consulo.ui.ValueComponent;
import consulo.ui.Window;
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

    WGwtModalWindowImpl modalWindow = new WGwtModalWindowImpl();
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
      }
    });
    modalWindow.setContent(Layouts.horizontalSplit().setFirstComponent(list).setSecondComponent(Components.label("Some labe")));
    modalWindow.setVisible(true);

    window.setContent(Components.label("Loaded"));
  }
}
