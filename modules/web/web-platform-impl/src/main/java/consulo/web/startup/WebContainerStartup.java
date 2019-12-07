/*
 * Copyright 2013-2019 consulo.io
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
package consulo.web.startup;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.idea.ApplicationStarter;
import com.intellij.idea.StartupUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import consulo.container.boot.ContainerPathManager;
import consulo.container.boot.ContainerStartup;
import consulo.container.util.StatCollector;
import consulo.ui.web.servlet.UIIconServlet;
import consulo.ui.web.servlet.UIServlet;
import consulo.web.main.WebPostStarter;
import consulo.web.servlet.RootUIBuilder;
import consulo.web.start.WebImportantFolderLocker;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebServlet;
import java.io.File;
import java.util.Arrays;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2019-08-08
 */
public class WebContainerStartup implements ContainerStartup {
  @WebServlet(urlPatterns = "/app/*")
  public static class RootUIServlet extends UIServlet {
    public RootUIServlet() {
      super(RootUIBuilder.class, "/app");
    }
  }

  @Nonnull
  @Override
  public ContainerPathManager createPathManager(@Nonnull Map<String, Object> args) {
    File platformPath = (File)args.get("platformPath");
    return new WebContainerPathManager(platformPath);
  }

  @Override
  public void run(@Nonnull Map<String, Object> map) {
    StatCollector stat = (StatCollector)map.get(ContainerStartup.STAT_COLLECTOR);
    String[] args = (String[])map.get(ContainerStartup.ARGS);

    ServletContext servletContext = (ServletContext)map.get(ServletContext.class.getName());

    registerServlets(servletContext);

    // FIXME [VISTALL] we ned this?
    System.setProperty("java.awt.headless", "true");

    new Thread(() -> startApplication(stat, args), "Consulo App Start").start();
  }

  private void startApplication(@Nonnull StatCollector stat, @Nonnull String[] args) {
    Runnable appInitializeMark = stat.mark(StatCollector.APP_INITIALIZE);

    StartupUtil.prepareAndStart(args, WebImportantFolderLocker::new, (newConfigFolder, commandLineArgs) -> {
      ApplicationStarter app = new ApplicationStarter(WebPostStarter.class, commandLineArgs);

      AppExecutorUtil.getAppExecutorService().execute(() -> {
        PluginManager.installExceptionHandler();
        app.run(stat, appInitializeMark, newConfigFolder);
      });
    });
  }

  private void registerServlets(ServletContext servletContext) {
    Class[] classes = new Class[]{RootUIServlet.class, UIIconServlet.class};

    for (Class aClass : classes) {
      Servlet servlet = (Servlet)ReflectionUtil.newInstance(aClass);

      ServletRegistration.Dynamic dynamic = servletContext.addServlet(aClass.getName(), servlet);

      WebServlet declaredAnnotation = (WebServlet)aClass.getDeclaredAnnotation(WebServlet.class);

      String[] urls = declaredAnnotation.urlPatterns();
      dynamic.addMapping(urls);

      System.out.println(aClass.getName() + " registered to: " + Arrays.asList(urls));
    }
  }

  @Override
  public void destroy() {
    Application application = ApplicationManager.getApplication();
    if (application != null) {
      Disposer.dispose(application);
    }
  }
}
