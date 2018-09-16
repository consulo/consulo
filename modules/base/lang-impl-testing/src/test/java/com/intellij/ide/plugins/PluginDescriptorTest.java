package com.intellij.ide.plugins;

import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Paths;

/**
 * @author Dmitry Avdeev
 *         Date: 7/14/11
 */
public class PluginDescriptorTest extends Assert {
  @Test
  public void testDescriptorLoading() throws Exception {
    URL url = PluginDescriptorTest.class.getResource("/ide/plugins/pluginDescriptor/asp.jar");
    assertNotNull(url);

    IdeaPluginDescriptorImpl descriptor = PluginManagerCore.loadDescriptorFromJar(Paths.get(url.toURI()).toFile(), true);
    assertNotNull(descriptor);
  }
}
