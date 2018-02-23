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
package com.intellij.openapi.extensions;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl;
import com.intellij.openapi.util.Disposer;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Extensions {
  private static LogProvider ourLogger = new SimpleLogProvider();

  public static final ExtensionPointName<AreaListener> AREA_LISTENER_EXTENSION_POINT = new ExtensionPointName<AreaListener>("com.intellij.arealistener");

  private static Map<AreaInstance,ExtensionsAreaImpl> ourAreaInstance2area = new THashMap<AreaInstance, ExtensionsAreaImpl>();
  private static Map<String,AreaClassConfiguration> ourAreaClass2Configuration = new THashMap<String, AreaClassConfiguration>();

  @Nonnull
  private static ExtensionsAreaImpl ourRootArea = createRootArea();

  @Nonnull
  private static ExtensionsAreaImpl createRootArea() {
    ExtensionsAreaImpl rootArea = new ExtensionsAreaImpl(null, null, null, ourLogger);
    rootArea.registerExtensionPoint(AREA_LISTENER_EXTENSION_POINT.getName(), AreaListener.class.getName());
    return rootArea;
  }

  private Extensions() {
  }

  public static void setSynchronized() {
    assert ourAreaInstance2area.isEmpty();
    assert ourAreaClass2Configuration.isEmpty();

    ourAreaInstance2area = new ConcurrentHashMap<AreaInstance, ExtensionsAreaImpl>();
    ourAreaClass2Configuration = new ConcurrentHashMap<String, AreaClassConfiguration>();
  }

  @Nonnull
  public static ExtensionsArea getRootArea() {
    return ourRootArea;
  }

  @Nonnull
  public static ExtensionsArea getArea(@Nullable AreaInstance areaInstance) {
    if (areaInstance == null) {
      return ourRootArea;
    }
    ExtensionsAreaImpl area = ourAreaInstance2area.get(areaInstance);
    if (area == null) {
      throw new IllegalArgumentException("No area instantiated for: " + areaInstance);
    }
    return area;
  }

  @TestOnly
  public static void cleanRootArea(@Nonnull Disposable parentDisposable) {
    final ExtensionsAreaImpl oldRootArea = (ExtensionsAreaImpl)getRootArea();
    final ExtensionsAreaImpl newArea = createRootArea();
    ourRootArea = newArea;
    oldRootArea.notifyAreaReplaced();
    Disposer.register(parentDisposable, new Disposable() {
      @Override
      public void dispose() {
        ourRootArea = oldRootArea;
        newArea.notifyAreaReplaced();
      }
    });
  }

  @Nonnull
  public static Object[] getExtensions(@NonNls String extensionPointName) {
    return getExtensions(extensionPointName, null);
  }

  @Nonnull
  @SuppressWarnings({"unchecked"})
  public static <T> T[] getExtensions(@Nonnull ExtensionPointName<T> extensionPointName) {
    return (T[])getExtensions(extensionPointName.getName(), null);
  }

  @Nonnull
  @SuppressWarnings({"unchecked"})
  public static <T> T[] getExtensions(@Nonnull ExtensionPointName<T> extensionPointName, AreaInstance areaInstance) {
    return Extensions.<T>getExtensions(extensionPointName.getName(), areaInstance);
  }

  @Nonnull
  public static <T> T[] getExtensions(String extensionPointName, @Nullable AreaInstance areaInstance) {
    ExtensionsArea area = getArea(areaInstance);
    ExtensionPoint<T> extensionPoint = area.getExtensionPoint(extensionPointName);
    return extensionPoint.getExtensions();
  }

  @Nonnull
  public static <T, U extends T> U findExtension(@Nonnull ExtensionPointName<T> extensionPointName, @Nonnull Class<U> extClass) {
    for (T t : getExtensions(extensionPointName)) {
      if (extClass.isInstance(t)) {
        //noinspection unchecked
        return (U) t;
      }
    }
    throw new IllegalArgumentException("could not find extension implementation " + extClass);
  }

  @Nonnull
  public static <T, U extends T> U findExtension(@Nonnull ExtensionPointName<T> extensionPointName, AreaInstance areaInstance, @Nonnull Class<U> extClass) {
    for (T t : getExtensions(extensionPointName, areaInstance)) {
      if (extClass.isInstance(t)) {
        //noinspection unchecked
        return (U) t;
      }
    }
    throw new IllegalArgumentException("could not find extension implementation " + extClass);
  }

  public static void instantiateArea(@NonNls @Nonnull String areaClass, @Nonnull AreaInstance areaInstance, @Nullable AreaInstance parentAreaInstance) {
    AreaClassConfiguration configuration = ourAreaClass2Configuration.get(areaClass);
    if (configuration == null) {
      throw new IllegalArgumentException("Area class is not registered: " + areaClass);
    }
    ExtensionsArea parentArea = getArea(parentAreaInstance);
    if (!equals(parentArea.getAreaClass(), configuration.getParentClassName())) {
      throw new IllegalArgumentException("Wrong parent area. Expected class: " + configuration.getParentClassName() + " actual class: " + parentArea.getAreaClass());
    }
    ExtensionsAreaImpl area = new ExtensionsAreaImpl(areaClass, areaInstance, parentArea.getPicoContainer(), ourLogger);
    if (ourAreaInstance2area.put(areaInstance, area) != null) {
      throw new IllegalArgumentException("Area already instantiated for: " + areaInstance);
    }
    for (AreaListener listener : getAreaListeners()) {
      listener.areaCreated(areaClass, areaInstance);
    }
  }

  @Nonnull
  private static AreaListener[] getAreaListeners() {
    return getRootArea().getExtensionPoint(AREA_LISTENER_EXTENSION_POINT).getExtensions();
  }

  public static void registerAreaClass(@NonNls @Nonnull String areaClass, @javax.annotation.Nullable @NonNls String parentAreaClass) {
    if (ourAreaClass2Configuration.containsKey(areaClass)) {
      // allow duplicate area class registrations if they are the same - fixing duplicate registration in tests is much more trouble
      AreaClassConfiguration configuration = ourAreaClass2Configuration.get(areaClass);
      if (!equals(configuration.getParentClassName(), parentAreaClass)) {
        throw new RuntimeException("Area class already registered: " + areaClass + ", "+ ourAreaClass2Configuration.get(areaClass));
      }
      else {
        return;
      }
    }
    AreaClassConfiguration configuration = new AreaClassConfiguration(areaClass, parentAreaClass);
    ourAreaClass2Configuration.put(areaClass, configuration);
  }

  public static void disposeArea(@Nonnull AreaInstance areaInstance) {
    assert ourAreaInstance2area.containsKey(areaInstance);

    String areaClass = ourAreaInstance2area.get(areaInstance).getAreaClass();
    if (areaClass == null) {
      throw new IllegalArgumentException("Area class is null (area never instantiated?). Instance: " + areaInstance);
    }
    try {
      for (AreaListener listener : getAreaListeners()) {
        listener.areaDisposing(areaClass, areaInstance);
      }
    }
    finally {
      ourAreaInstance2area.remove(areaInstance);
    }
  }

  private static boolean equals(@javax.annotation.Nullable Object object1, @javax.annotation.Nullable Object object2) {
    return object1 == object2 || object1 != null && object2 != null && object1.equals(object2);
  }

  public static void setLogProvider(@Nonnull LogProvider logProvider) {
    ourLogger = logProvider;
  }

  private static class AreaClassConfiguration {
    private final String myClassName;
    private final String myParentClassName;

    AreaClassConfiguration(@Nonnull String className, String parentClassName) {
      myClassName = className;
      myParentClassName = parentClassName;
    }

    @Nonnull
    public String getClassName() {
      return myClassName;
    }

    public String getParentClassName() {
      return myParentClassName;
    }
  }

  @SuppressWarnings("CallToPrintStackTrace")
  public static class SimpleLogProvider implements LogProvider {
    @Override
    public void error(String message) {
      new Throwable(message).printStackTrace();
    }

    @Override
    public void error(String message, @Nonnull Throwable t) {
      System.err.println(message);
      t.printStackTrace();
    }

    @Override
    public void error(@Nonnull Throwable t) {
      t.printStackTrace();
    }

    @Override
    public void warn(String message) {
      System.err.println(message);
    }

    @Override
    public void warn(String message, @Nonnull Throwable t) {
      System.err.println(message);
      t.printStackTrace();
    }

    @Override
    public void warn(@Nonnull Throwable t) {
      t.printStackTrace();
    }
  }
}
