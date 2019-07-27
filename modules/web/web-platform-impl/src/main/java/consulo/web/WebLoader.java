/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.idea.ApplicationStarter;
import com.intellij.idea.StartupUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.concurrency.AppExecutorUtil;
import consulo.ui.web.servlet.UIIconServlet;
import consulo.ui.web.servlet.UIServlet;
import consulo.web.main.WebPostStarter;
import consulo.web.servlet.RootUIBuilder;
import consulo.web.start.WebImportantFolderLocker;

import javax.annotation.Nonnull;
import javax.servlet.annotation.WebServlet;
import java.io.File;

/**
 * @author VISTALL
 * @since 16-May-17
 */
public class WebLoader {
  @WebServlet(urlPatterns = "/app/*")
  public static class RootUIServlet extends UIServlet {
    public RootUIServlet() {
      super(RootUIBuilder.class, "/app");
    }
  }

  public void start(String[] args) {
    File home = new File(args[0]);

    System.setProperty("java.awt.headless", "true");
    System.setProperty(PathManager.PROPERTY_HOME_PATH, home.getPath());
    System.setProperty(PathManager.PROPERTY_CONFIG_PATH, home.getPath() + "/.config/sandbox/config");
    System.setProperty(PathManager.PROPERTY_SYSTEM_PATH, home.getPath() + "/.config/sandbox/system");

    //Main.setFlags(new String[0]);

    StartupUtil.prepareAndStart(args, WebImportantFolderLocker::new, (newConfigFolder, commandLineArgs) -> {
      ApplicationStarter app = new ApplicationStarter(WebPostStarter.class,commandLineArgs);

      AppExecutorUtil.getAppExecutorService().execute(() -> {
        PluginManager.installExceptionHandler();
        app.run(newConfigFolder);
      });
    });
  }

  public void destroy() {
    Application application = ApplicationManager.getApplication();
    if (application != null) {
      Disposer.dispose(application);
    }
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  public Class<? extends RootUIServlet>[] getServletClasses() {
    return new Class[]{RootUIServlet.class, UIIconServlet.class};
  }
}
