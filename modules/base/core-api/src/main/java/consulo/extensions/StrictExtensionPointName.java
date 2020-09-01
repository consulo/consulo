package consulo.extensions;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import consulo.container.plugin.PluginDescriptor;
import consulo.logging.Logger;
import consulo.util.PluginExceptionUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2020-09-01
 */
public final class StrictExtensionPointName<C extends ComponentManager, T> {
  private static final Logger LOG = Logger.getInstance(StrictExtensionPointName.class);

  @Nonnull
  public static <E> StrictExtensionPointName<Application, E> forApplication(@Nonnull String name) {
    return create(Application.class, name);
  }

  @Nonnull
  public static <E> StrictExtensionPointName<Project, E> forProject(@Nonnull String name) {
    return create(Project.class, name);
  }

  @Nonnull
  public static <E> StrictExtensionPointName<Module, E> forModule(@Nonnull String name) {
    return create(Module.class, name);
  }

  @Nonnull
  private static <C1 extends ComponentManager, T1> StrictExtensionPointName<C1, T1> create(@Nonnull Class<C1> componentClass, @Nonnull String name) {
    return new StrictExtensionPointName<>(componentClass, name);
  }

  @Nonnull
  private final Class<C> myComponentClass;
  @Nonnull
  private final ExtensionPointName<T> myExtensionPointName;

  public StrictExtensionPointName(@Nonnull Class<C> componentClass, @Nonnull String name) {
    myComponentClass = componentClass;
    myExtensionPointName = ExtensionPointName.create(name);
  }

  private void checkComponent(@Nonnull C component) {
    if (!myComponentClass.isInstance(component)) {
      throw new IllegalArgumentException("Component " + component.getClass() + " is not instance of " + myComponentClass);
    }
  }

  public boolean hasAnyExtensions(@Nonnull C component) {
    checkComponent(component);
    return component.getExtensionPoint(myExtensionPointName).hasAnyExtensions();
  }

  @Nonnull
  public List<T> getExtensionList(@Nonnull C component) {
    checkComponent(component);
    return component.getExtensionList(myExtensionPointName);
  }

  @Nullable
  public <V extends T> V findExtension(@Nonnull C component, @Nonnull Class<V> instanceOf) {
    checkComponent(component);
    return component.findExtension(myExtensionPointName, instanceOf);
  }

  @Nonnull
  public <V extends T> V findExtensionOrFail(@Nonnull C component, @Nonnull Class<V> instanceOf) {
    checkComponent(component);

    V extension = component.findExtension(myExtensionPointName, instanceOf);
    if (extension == null) {
      throw new IllegalArgumentException("Extension point: " + myExtensionPointName.getName() + " not contains extension of type: " + instanceOf);
    }
    return extension;
  }

  public void forEachExtensionSafe(@Nonnull C component, @Nonnull Consumer<T> consumer) {
    processWithPluginDescriptor(component, (value, pluginDescriptor) -> {
      try {
        consumer.accept(value);
      }
      catch (Throwable e) {
        PluginExceptionUtil.logPluginError(LOG, e.getMessage(), e, value.getClass());
      }
    });
  }

  public void processWithPluginDescriptor(@Nonnull C component, @Nonnull BiConsumer<? super T, ? super PluginDescriptor> consumer) {
    checkComponent(component);

    component.getExtensionPoint(myExtensionPointName).processWithPluginDescriptor(consumer);
  }
}
