package com.intellij.ide.plugins;

import consulo.container.impl.ContainerLogger;
import consulo.container.impl.PluginDescriptorImpl;
import consulo.container.impl.PluginDescriptorLoader;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

/**
 * @author Dmitry Avdeev
 * Date: 7/14/11
 */
public class PluginDescriptorTest extends Assert {
  @Test
  public void testDescriptorLoading() throws Exception {
    URL url = PluginDescriptorTest.class.getResource("/ide/plugins/pluginDescriptor/consulo.nunit-2-SNAPSHOT.jar");
    assertNotNull(url);

    File jarFile = Paths.get(url.toURI()).toFile();
    PluginDescriptorImpl descriptor = PluginDescriptorLoader.loadDescriptorFromJar(jarFile, jarFile, PluginDescriptorLoader.PLUGIN_XML, true, false, new ContainerLogger() {

      @Override
      public void info(String message) {
        System.out.println(message);
      }

      @Override
      public void warn(String message) {
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
    });
    assertNotNull(descriptor);
  }
}
