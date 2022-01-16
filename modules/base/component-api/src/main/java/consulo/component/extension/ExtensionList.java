package consulo.component.extension;

import consulo.component.ComponentManager;
import consulo.component.util.PluginExceptionUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2020-09-01
 */
public final class ExtensionList<E, C extends ComponentManager> {
  private static final Logger LOG = Logger.getInstance(ExtensionList.class);

  @Nonnull
  @SuppressWarnings("unchecked")
  public static <C1 extends ComponentManager, T1> ExtensionList<T1, C1> of(@Nonnull Class<T1> extensionClass) {
    Extension annotation = extensionClass.getAnnotation(Extension.class);
    if (annotation == null) {
      throw new IllegalArgumentException(extensionClass + " is not annotated by @" + Extension.class.getSimpleName());
    }

    return new ExtensionList<>((Class<C1>)annotation.component(), extensionClass, annotation.name());
  }

  @Nonnull
  private final Class<C> myComponentClass;
  @Nonnull
  private final Class<E> myExtensionClass;
  @Nonnull
  private final String myName;
  @Nullable
  private ExtensionPointId<E> myExtensionPointId;

  private ExtensionList(@Nonnull Class<C> componentClass, @Nonnull Class<E> extensionClass, @Nonnull String name) {
    myComponentClass = componentClass;
    myExtensionClass = extensionClass;
    myName = name;
  }

  @Nonnull
  private ExtensionPointId<E> getId() {
    if (myExtensionPointId != null) {
      return myExtensionPointId;
    }

    PluginDescriptor plugin = PluginManager.getPlugin(myExtensionClass);
    if (plugin == null) {
      throw new IllegalArgumentException(myExtensionClass + " is not inside registered in container");
    }

    ExtensionPointId<E> id = ExtensionPointId.of(plugin.getPluginId() + "." + myName);
    myExtensionPointId = id;
    return id;
  }

  private void checkComponent(@Nonnull C component) {
    if (!myComponentClass.isInstance(component)) {
      throw new IllegalArgumentException("Component " + component.getClass() + " is not instance of " + myComponentClass);
    }
  }

  public boolean hasAnyExtensions(@Nonnull C component) {
    checkComponent(component);
    return component.getExtensionPoint(getId()).hasAnyExtensions();
  }

  @Nonnull
  public List<E> getExtensionList(@Nonnull C component) {
    checkComponent(component);
    return component.getExtensionPoint(getId()).getExtensionList();
  }

  @Nullable
  public <V extends E> V findExtension(@Nonnull C component, @Nonnull Class<V> instanceOf) {
    checkComponent(component);
    return component.getExtensionPoint(getId()).findExtension(instanceOf);
  }

  @Nonnull
  public <V extends E> V findExtensionOrFail(@Nonnull C component, @Nonnull Class<V> instanceOf) {
    checkComponent(component);

    V extension = component.getExtensionPoint(getId()).findExtension(instanceOf);
    if (extension == null) {
      throw new IllegalArgumentException("Extension point: " + getId() + " not contains extension of type: " + instanceOf);
    }
    return extension;
  }

  public void forEachExtensionSafe(@Nonnull C component, @Nonnull Consumer<E> consumer) {
    processWithPluginDescriptor(component, (value, pluginDescriptor) -> {
      try {
        consumer.accept(value);
      }
      catch (Throwable e) {
        PluginExceptionUtil.logPluginError(LOG, e.getMessage(), e, value.getClass());
      }
    });
  }

  public void processWithPluginDescriptor(@Nonnull C component, @Nonnull BiConsumer<? super E, ? super PluginDescriptor> consumer) {
    checkComponent(component);

    component.getExtensionPoint(getId()).processWithPluginDescriptor(consumer);
  }
}
