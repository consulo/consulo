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
package consulo.web.boot;

import consulo.container.impl.classloader.BootstrapClassLoaderUtil;
import consulo.util.nodep.collection.HashMap;
import consulo.container.boot.ContainerStartup;
import consulo.container.impl.ContainerLogger;
import consulo.container.util.StatCollector;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.File;
import java.util.Map;

/**
 * @author VISTALL
 * @since 16-May-17
 */
@WebListener
public class WebInitializer implements ServletContextListener {
  private static class ContainerLoggerImpl implements ContainerLogger {

    @Override
    public void info(String message) {
      System.out.println(message);
    }

    @Override
    public void info(String message, Throwable t) {
      System.out.println(message);
      t.printStackTrace(System.out);
    }

    @Override
    public void error(String message, Throwable t) {
      System.err.println(message);
      t.printStackTrace(System.err);
    }
  }

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    ServletContext servletContext = servletContextEvent.getServletContext();

    File platformDirectory;

    String workDirectoryProperty = System.getProperty("consulo.web.work.directory");
    if (workDirectoryProperty != null) {
      platformDirectory = new File(workDirectoryProperty, "platform").listFiles()[0];
    }
    else {
      String platformPath = servletContext.getRealPath("/platform");
      platformDirectory = new File(platformPath).listFiles()[0];
    }

    StatCollector stat = new StatCollector();

    File modulesDirectory = new File(platformDirectory, "modules");

    try {
      ContainerStartup containerStartup = BootstrapClassLoaderUtil.buildContainerStartup(stat, modulesDirectory, new ContainerLoggerImpl());

      Map<String, Object> map = new HashMap<>();
      map.put(ContainerStartup.STAT_COLLECTOR, stat);
      map.put(ServletContext.class.getName(), servletContext);
      map.put("platformPath", platformDirectory);

      servletContext.setAttribute(ContainerStartup.class.getName(), containerStartup);

      containerStartup.run(map, stat, new String[0]);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent) {
    ContainerStartup containerStartup = (ContainerStartup)servletContextEvent.getServletContext().getAttribute(ContainerStartup.class.getName());
    if (containerStartup == null) {
      return;
    }

    servletContextEvent.getServletContext().setAttribute(ContainerStartup.class.getName(), null);

    containerStartup.destroy();
  }
}
