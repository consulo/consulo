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
package consulo.web.internal.startup;

import com.vaadin.base.devserver.BrowserLiveReloadAccessorImpl;
import com.vaadin.base.devserver.DevModeHandlerManagerImpl;
import com.vaadin.base.devserver.startup.DevModeStartupListener;
import com.vaadin.flow.di.LookupInitializer;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.LookupServletContainerInitializer;
import com.vaadin.flow.server.startup.VaadinAppShellInitializer;
import com.vaadin.flow.server.startup.VaadinInitializerException;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ApplicationProperties;
import consulo.application.impl.internal.start.ApplicationStarter;
import consulo.application.impl.internal.start.StartupUtil;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.container.boot.ContainerPathManager;
import consulo.container.boot.ContainerStartup;
import consulo.container.util.StatCollector;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.web.internal.servlet.RootUIBuilder;
import consulo.web.internal.servlet.UIIconServlet;
import consulo.web.internal.servlet.UIServlet;
import consulo.web.internal.servlet.VaadinRootLayout;
import consulo.web.main.WebApplicationStarter;
import jakarta.annotation.Nonnull;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.annotation.WebServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * @author VISTALL
 * @since 2019-08-08
 */
public class WebContainerStartup implements ContainerStartup {
  @WebServlet(urlPatterns = "/*")
  public static class RootUIServlet extends UIServlet {
    public RootUIServlet() {
      super(RootUIBuilder::new, "/");
    }
  }

  @Nonnull
  @Override
  public ContainerPathManager createPathManager(@Nonnull Map<String, Object> args) {
    return new WebContainerPathManager();
  }

  @Override
  public void run(@Nonnull Map<String, Object> map) {
    StatCollector stat = (StatCollector)map.get(STAT_COLLECTOR);
    String[] args = (String[])map.get(ARGS);

    StartupUtil.initializeLogger();

    Server server = new Server(8080);

    ServletContextHandler handler = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS | ServletContextHandler.SECURITY);
    handler.setClassLoader(getClass().getClassLoader());

    List<URL> urls = new ArrayList<>();
    try {
      Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/resources/");
      while (resources.hasMoreElements()) {
        urls.add(resources.nextElement());
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }

    handler.setBaseResource(new ResourceCollection(ContainerUtil.map(urls, Resource::newResource)));

    handler.addServletContainerInitializer(new JakartaWebSocketServletContainerInitializer());
    Set<Class<?>> classes = new HashSet<>();
    classes.addAll(LookupInitializer.getDefaultImplementations());
    classes.add(LookupInitializer.class);
    classes.add(ConsuloAppShellConfigurator.class);
    classes.add(VaadinRootLayout.class);

    if (ApplicationProperties.isInSandbox()) {
      classes.add(DevModeHandlerManagerImpl.class);
      classes.add(BrowserLiveReloadAccessorImpl.class);
    }

    handler.addServletContainerInitializer(new LookupServletContainerInitializer(), classes.toArray(Class[]::new));
    handler.addEventListener(new VaadinAppShellInitializer() {
      @Override
      public void contextInitialized(ServletContextEvent sce) {
        initialize(classes, new VaadinServletContext(sce.getServletContext()));
      }
    });

    if (ApplicationProperties.isInSandbox()) {
      handler.addEventListener(new DevModeStartupListener() {
        @Override
        public void contextInitialized(ServletContextEvent ctx) {
          try {
            initialize(classes, new VaadinServletContext(ctx.getServletContext()));
          }
          catch (VaadinInitializerException e) {
            throw new RuntimeException(e);
          }
        }
      });
    }
    server.setHandler(handler);

    registerServlets(handler);

    try {
      server.start();

      new Thread(() -> startApplication(stat, args), "Consulo App Start").start();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void startApplication(@Nonnull StatCollector stat, @Nonnull String[] args) {
    ApplicationStarter.installExceptionHandler(() -> Logger.getInstance(WebContainerStartup.class));

    Runnable appInitializeMark = stat.mark(StatCollector.APP_INITIALIZE);

    StartupUtil.prepareAndStart(args, stat, WebImportantFolderLocker::new, (newConfigFolder, commandLineArgs) -> {
      ApplicationStarter starter = new WebApplicationStarter(commandLineArgs, stat);

      AppExecutorUtil.getAppExecutorService().execute(() -> starter.run(stat, appInitializeMark, newConfigFolder));
    });
  }

  private void registerServlets(ServletContextHandler handler) {
    List<Class<? extends Servlet>> classes = List.of(RootUIServlet.class, UIIconServlet.class);

    for (Class<? extends Servlet> servletClass : classes) {
      WebServlet declaredAnnotation = servletClass.getDeclaredAnnotation(WebServlet.class);

      String[] urls = declaredAnnotation.urlPatterns();

      int i = 0;
      for (String url : urls) {
        ServletHolder servletHolder = handler.addServlet(servletClass, url);
        servletHolder.setInitOrder(++i);
      }

      System.out.println(servletClass.getName() + " registered to: " + Arrays.asList(urls));
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
