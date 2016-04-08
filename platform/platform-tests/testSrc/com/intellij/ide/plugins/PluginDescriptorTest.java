package com.intellij.ide.plugins;

import consulo.testFramework.util.PathManagerEx;
import junit.framework.TestCase;

import java.io.File;

/**
 * @author Dmitry Avdeev
 *         Date: 7/14/11
 */
public class PluginDescriptorTest extends TestCase {

  public void testDescriptorLoading() throws Exception {
    String path = PathManagerEx.getTestDataPath().replace(File.separatorChar, '/') + "/ide/plugins/pluginDescriptor";
    File file = new File(path + "/asp.jar");
    assertTrue(file + " not exist", file.exists());
    IdeaPluginDescriptorImpl descriptor = PluginManagerCore.loadDescriptorFromJar(file);
    assertNotNull(descriptor);
  }
}
