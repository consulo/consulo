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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.lang.UrlClassLoader;
import consulo.web.boot.util.logger.WebLoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 16-May-17
 */
@WebListener
public class Initializer implements ServletContextListener {
  private static final String ourWebLoaderObject = "ourWebLoaderObject";

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

    Logger.setFactory(WebLoggerFactory.class);
    try {
      initApplication(platformDirectory, servletContext);
    }
    catch (Exception e) {
      Logger.getInstance(Initializer.class).error(e);
    }
  }

  private void initApplication(File file, ServletContext servletContext) throws Exception {
    List<URL> libs = new ArrayList<>();

    File libFile = new File(file, "lib");
    if (libFile.exists()) {
      File[] files = libFile.listFiles();
      for (File child : files) {
        String name = child.getName();
        if (name.endsWith(".jar")) {
          libs.add(child.toURI().toURL());
        }
      }
      File superGwtDirectory = new File(libFile, "gwt");
      if (superGwtDirectory.exists()) {
        libs.add(superGwtDirectory.getCanonicalFile().toURI().toURL());
      }
    }

    UrlClassLoader.Builder build = UrlClassLoader.build();
    build.parent(Initializer.class.getClassLoader().getParent());
    build.urls(libs);

    UrlClassLoader urlClassLoader = build.get();

    Class<?> webMain = urlClassLoader.loadClass("consulo.web.WebLoader");

    Object webLoader = ReflectionUtil.newInstance(webMain);

    servletContext.setAttribute(ourWebLoaderObject, webLoader);

    Method getServletClassesMethod = webMain.getDeclaredMethod("getServletClasses");

    Class[] classes = (Class[])getServletClassesMethod.invoke(webLoader);

    for (Class aClass : classes) {
      Servlet servlet = (Servlet)ReflectionUtil.newInstance(aClass);

      ServletRegistration.Dynamic dynamic = servletContext.addServlet(aClass.getName(), servlet);

      WebServlet declaredAnnotation = (WebServlet)aClass.getDeclaredAnnotation(WebServlet.class);

      String[] urls = declaredAnnotation.urlPatterns();
      dynamic.addMapping(urls);

      System.out.println(aClass.getName() + " registered to: " + Arrays.asList(urls));
    }

    new Thread(() -> startApplication(webMain, webLoader, file), "App Start").start();
  }

  private void startApplication(Class<?> webMain, Object webLoader, File file) {
    try {
      Method startMethod = webMain.getDeclaredMethod("start", String[].class);
      startMethod.setAccessible(true);

      String[] args = {file.getPath()};
      startMethod.invoke(webLoader, (Object)args);
    }
    catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      Logger.getInstance(Initializer.class).error(e);
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent) {
    Object attribute = servletContextEvent.getServletContext().getAttribute(ourWebLoaderObject);
    if (attribute == null) {
      return;
    }

    servletContextEvent.getServletContext().setAttribute(ourWebLoaderObject, null);

    Method destroyMethod = ReflectionUtil.getDeclaredMethod(attribute.getClass(), "destroy");
    if (destroyMethod != null) {
      try {
        destroyMethod.invoke(null);
      }
      catch (IllegalAccessException | InvocationTargetException e) {
        Logger.getInstance(Initializer.class).error(e);
      }
    }
  }
}
