package consulo.component.extension;

import consulo.annotation.DeprecationInfo;
import consulo.component.ComponentManager;
import consulo.container.plugin.PluginDescriptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2020-09-01
 */
@Deprecated
@DeprecationInfo("Prefer ComponentManager.getExtensionPoint() methods")
public final class ExtensionList<E, C extends ComponentManager> {
  @Nonnull
  @SuppressWarnings("unchecked")
  public static <C1 extends ComponentManager, T1> ExtensionList<T1, C1> of(@Nonnull Class<T1> extensionClass) {
    return new ExtensionList<>(extensionClass);
  }

  @Nonnull
  private final Class<E> myExtensionClass;

  private ExtensionList(@Nonnull Class<E> extensionClass) {
    myExtensionClass = extensionClass;
  }

  public boolean hasAnyExtensions(@Nonnull C component) {
    return component.getExtensionPoint(myExtensionClass).hasAnyExtensions();
  }

  @Nonnull
  public List<E> getExtensionList(@Nonnull C component) {
    return component.getExtensionPoint(myExtensionClass).getExtensionList();
  }

  @Nullable
  public <V extends E> V findExtension(@Nonnull C component, @Nonnull Class<V> instanceOf) {
    return component.getExtensionPoint(myExtensionClass).findExtension(instanceOf);
  }

  @Nonnull
  public <V extends E> V findExtensionOrFail(@Nonnull C component, @Nonnull Class<V> instanceOf) {
    return component.getExtensionPoint(myExtensionClass).findExtensionOrFail(instanceOf);
  }

  public void forEachExtensionSafe(@Nonnull C component, @Nonnull Consumer<E> consumer) {
    component.getExtensionPoint(myExtensionClass).forEachExtensionSafe(consumer);
  }

  public void processWithPluginDescriptor(@Nonnull C component, @Nonnull BiConsumer<? super E, ? super PluginDescriptor> consumer) {
    component.getExtensionPoint(myExtensionClass).processWithPluginDescriptor(consumer);
  }

  @Nullable
  public <R> R computeSafeIfAny(@Nonnull C componentManager, @Nonnull Function<? super E, ? extends R> processor) {
    return componentManager.getExtensionPoint(myExtensionClass).computeSafeIfAny(processor);
  }

  @Nullable
  public E findFirstSafe(@Nonnull C componentManager, @Nonnull Predicate<E> predicate) {
    return componentManager.getExtensionPoint(myExtensionClass).findFirstSafe(predicate);
  }
}
