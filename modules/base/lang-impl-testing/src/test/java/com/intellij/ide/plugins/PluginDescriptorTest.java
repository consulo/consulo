package com.intellij.ide.plugins;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

/**
 * @author Dmitry Avdeev
 *         Date: 7/14/11
 */
public class PluginDescriptorTest extends Assert {
  @Test
  public void testDescriptorLoading() throws Exception {
    URL url = PluginDescriptorTest.class.getResource("/ide/plugins/pluginDescriptor/consulo.nunit-2-SNAPSHOT.jar");
    assertNotNull(url);

    File jarFile = Paths.get(url.toURI()).toFile();
    IdeaPluginDescriptorImpl descriptor = PluginManagerCore.loadDescriptorFromJar(jarFile, jarFile, PluginManagerCore.PLUGIN_XML, true, false);
    assertNotNull(descriptor);
  }
}
