package consulo.web.servlet;

import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.util.TimeoutUtil;
import consulo.ui.Components;
import consulo.ui.Window;
import consulo.ui.internal.WGwtModalWindowImpl;
import consulo.ui.shared.Size;
import consulo.web.AppInit;
import consulo.web.servlet.ui.UIBuilder;
import consulo.web.servlet.ui.UIServlet;
import org.jetbrains.annotations.NotNull;

import javax.servlet.annotation.WebServlet;

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

    window.setContent(Components.label("Loaded"));

    WGwtModalWindowImpl modalWindow = new WGwtModalWindowImpl();
    modalWindow.setSize(new Size(777, 460));
    modalWindow.setContent(Components.label("Hello World"));
    modalWindow.setVisible(true);
  }
}
