/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.extensions.impl;

import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.PluginDescriptor;
import junit.framework.TestCase;

import javax.annotation.Nonnull;

/**
 * @author AKireyev
 */
public class ExtensionsImplTest extends TestCase {
  public static final String EXTENSION_POINT_NAME_1 = "ext.point.one";
  public static final String AREA_1 = "the.area.one";

  public void testCreateAndAccess() {
    ExtensionsAreaImpl extensionsArea = new ExtensionsAreaImpl(new Extensions.SimpleLogProvider());
    int numEP = extensionsArea.getExtensionPoints().length;
    extensionsArea.registerExtensionPoint(EXTENSION_POINT_NAME_1, Integer.class.getName());
    assertEquals("Additional EP available", numEP + 1, extensionsArea.getExtensionPoints().length);
    assertNotNull("EP by name available", extensionsArea.getExtensionPoint(EXTENSION_POINT_NAME_1));
  }

  public void testInvalidActions() {
    ExtensionsAreaImpl extensionsArea = new ExtensionsAreaImpl(new Extensions.SimpleLogProvider());
    extensionsArea.registerExtensionPoint(EXTENSION_POINT_NAME_1, Integer.class.getName());
    try {
      extensionsArea.registerExtensionPoint(EXTENSION_POINT_NAME_1, Boolean.class.getName());
      fail("Should not allow duplicate registration");
    } catch (RuntimeException e) {
    }
  }

  public void testUnregisterEP() {
    ExtensionsAreaImpl extensionsArea = new ExtensionsAreaImpl(new Extensions.SimpleLogProvider());
    int numEP = extensionsArea.getExtensionPoints().length;
    extensionsArea.registerExtensionPoint(EXTENSION_POINT_NAME_1, Integer.class.getName());
    final boolean removed[] = new boolean[1];
    removed[0] = false;
    extensionsArea.getExtensionPoint(EXTENSION_POINT_NAME_1).addExtensionPointListener(new ExtensionPointListener() {
      @Override
      public void extensionAdded(@Nonnull Object extension, final PluginDescriptor pluginDescriptor) {
      }

      @Override
      public void extensionRemoved(@Nonnull Object extension, final PluginDescriptor pluginDescriptor) {
        removed[0] = true;
      }
    });
    extensionsArea.getExtensionPoint(EXTENSION_POINT_NAME_1).registerExtension(new Integer(123));
    extensionsArea.unregisterExtensionPoint(EXTENSION_POINT_NAME_1);
    assertTrue("Extension point should be removed", extensionsArea.getExtensionPoints().length == numEP);
    assertTrue("Extension point disposed", removed[0]);
  }

  public void testExtensionsNamespaces() {
    ExtensionsAreaImpl extensionsArea = new ExtensionsAreaImpl(new Extensions.SimpleLogProvider());
    extensionsArea.registerExtensionPoint("plugin.ep1", TestExtensionClassOne.class.getName(), ExtensionPoint.Kind.BEAN_CLASS);
    extensionsArea.registerExtension("plugin", ExtensionComponentAdapterTest.readElement(
        "<plugin:ep1 xmlns:plugin=\"plugin\" order=\"LAST\"><text>3</text></plugin:ep1>"));
    extensionsArea.registerExtension("plugin", ExtensionComponentAdapterTest.readElement(
        "<ep1 xmlns=\"plugin\" order=\"FIRST\"><text>1</text></ep1>"));
    extensionsArea.registerExtension("plugin", ExtensionComponentAdapterTest.readElement(
        "<extension point=\"plugin.ep1\"><text>2</text></extension>"));
    ExtensionPoint extensionPoint = extensionsArea.getExtensionPoint("plugin.ep1");
    TestExtensionClassOne[] extensions = (TestExtensionClassOne[]) extensionPoint.getExtensions();
    assertEquals(3, extensions.length);
    assertEquals("1", extensions[0].getText());
    assertEquals("2", extensions[1].getText());
    assertEquals("3", extensions[2].getText());
  }

  public void testExtensionsWithOrdering() {
    ExtensionsAreaImpl extensionsArea = new ExtensionsAreaImpl(new Extensions.SimpleLogProvider());
    extensionsArea.registerExtensionPoint("ep1", TestExtensionClassOne.class.getName(), ExtensionPoint.Kind.BEAN_CLASS);
    extensionsArea.registerExtension("", ExtensionComponentAdapterTest.readElement(
        "<extension point=\"ep1\" order=\"LAST\"><text>3</text></extension>"));
    extensionsArea.registerExtension("", ExtensionComponentAdapterTest.readElement(
        "<extension point=\"ep1\" order=\"FIRST\"><text>1</text></extension>"));
    extensionsArea.registerExtension("", ExtensionComponentAdapterTest.readElement(
        "<extension point=\"ep1\"><text>2</text></extension>"));
    ExtensionPoint extensionPoint = extensionsArea.getExtensionPoint("ep1");
    TestExtensionClassOne[] extensions = (TestExtensionClassOne[]) extensionPoint.getExtensions();
    assertEquals(3, extensions.length);
    assertEquals("1", extensions[0].getText());
    assertEquals("2", extensions[1].getText());
    assertEquals("3", extensions[2].getText());
  }

  public void testExtensionsWithOrderingUpdate() {
    ExtensionsAreaImpl extensionsArea = new ExtensionsAreaImpl(new Extensions.SimpleLogProvider());
    extensionsArea.registerExtensionPoint("ep1", TestExtensionClassOne.class.getName(), ExtensionPoint.Kind.BEAN_CLASS);
    extensionsArea.registerExtension("", ExtensionComponentAdapterTest.readElement(
        "<extension point=\"ep1\" id=\"_7\" order=\"LAST\"><text>7</text></extension>"));
    extensionsArea.registerExtension("", ExtensionComponentAdapterTest.readElement(
        "<extension point=\"ep1\" id=\"fst\" order=\"FIRST\"><text>1</text></extension>"));
    extensionsArea.registerExtension("", ExtensionComponentAdapterTest.readElement(
        "<extension point=\"ep1\" id=\"id\"><text>3</text></extension>"));
    ExtensionPoint extensionPoint = extensionsArea.getExtensionPoint("ep1");
    TestExtensionClassOne[] extensions = (TestExtensionClassOne[]) extensionPoint.getExtensions();
    assertEquals(3, extensions.length);
    assertEquals("1", extensions[0].getText());
    assertEquals("3", extensions[1].getText());
    assertEquals("7", extensions[2].getText());
    TestExtensionClassOne extension = new TestExtensionClassOne("xxx");
    extensionPoint.registerExtension(extension);
    extensionPoint.unregisterExtension(extension);
    extensionsArea.registerExtension("", ExtensionComponentAdapterTest.readElement(
        "<extension point=\"ep1\" order=\"BEFORE id\"><text>2</text></extension>"));
    extensionsArea.registerExtension("", ExtensionComponentAdapterTest.readElement(
        "<extension point=\"ep1\" order=\"AFTER id\"><text>4</text></extension>"));
    extensionsArea.registerExtension("", ExtensionComponentAdapterTest.readElement(
        "<extension point=\"ep1\" order=\"last, after _7\"><text>8</text></extension>"));
    extensionsArea.registerExtension("", ExtensionComponentAdapterTest.readElement(
        "<extension point=\"ep1\" order=\"after:id, before _7, after fst\"><text>5</text></extension>"));
    extensionPoint.registerExtension(new TestExtensionClassOne("6"));
    extensions = (TestExtensionClassOne[]) extensionPoint.getExtensions();
    assertEquals(8, extensions.length);
    assertEquals("1", extensions[0].getText());
    assertEquals("2", extensions[1].getText());
    assertEquals("3", extensions[2].getText());
    assertEquals("4", extensions[3].getText());
    assertEquals("5", extensions[4].getText());
    assertEquals("6", extensions[5].getText());
    assertEquals("7", extensions[6].getText());
    assertEquals("8", extensions[7].getText());
  }

  public interface MyInterface1 extends Runnable {
  }

  public interface MyInterface2 {
  }

  public interface MyInterface3 extends Runnable {
  }

  public interface MyInterface4 extends MyInterface1, MyInterface2, MyInterface3 {
  }

  public static class MyClass implements MyInterface4 {
    @Override
    public void run() {
    }
  }

  public static class MyComponent1 {
    public MyInterface1 testObject;

    public MyComponent1(MyInterface1 testObject) {
      this.testObject = testObject;
    }
  }

  public static class MyComponent2 {
    public MyInterface1 testObject;
    public MyComponent1[] comp1;

    public MyComponent2(MyComponent1[] comp1, MyInterface1 testObject) {
      this.comp1 = comp1;
      this.testObject = testObject;
    }
  }

  public static class MyTestComponent {
    public MyComponent1[] comp1;
    public MyComponent2 comp2;

    public MyTestComponent(MyComponent1[] comp1, MyComponent2 comp2) {
      this.comp1 = comp1;
      this.comp2 = comp2;
    }
  }

}
