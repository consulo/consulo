package consulo.component.extension;

import consulo.annotation.DeprecationInfo;
import consulo.component.ComponentManager;
import consulo.container.plugin.PluginDescriptor;

import org.jspecify.annotations.Nullable;
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
  
  @SuppressWarnings("unchecked")
  public static <C1 extends ComponentManager, T1> ExtensionList<T1, C1> of(Class<T1> extensionClass) {
    return new ExtensionList<>(extensionClass);
  }

  
  private final Class<E> myExtensionClass;

  private ExtensionList(Class<E> extensionClass) {
    myExtensionClass = extensionClass;
  }

  public boolean hasAnyExtensions(C component) {
    return component.getExtensionPoint(myExtensionClass).hasAnyExtensions();
  }

  
  public List<E> getExtensionList(C component) {
    return component.getExtensionPoint(myExtensionClass).getExtensionList();
  }

  @Nullable
  public <V extends E> V findExtension(C component, Class<V> instanceOf) {
    return component.getExtensionPoint(myExtensionClass).findExtension(instanceOf);
  }

  
  public <V extends E> V findExtensionOrFail(C component, Class<V> instanceOf) {
    return component.getExtensionPoint(myExtensionClass).findExtensionOrFail(instanceOf);
  }

  public void forEachExtensionSafe(C component, Consumer<E> consumer) {
    component.getExtensionPoint(myExtensionClass).forEachExtensionSafe(consumer);
  }

  public void processWithPluginDescriptor(C component, BiConsumer<? super E, ? super PluginDescriptor> consumer) {
    component.getExtensionPoint(myExtensionClass).processWithPluginDescriptor(consumer);
  }

  @Nullable
  public <R> R computeSafeIfAny(C componentManager, Function<? super E, ? extends R> processor) {
    return componentManager.getExtensionPoint(myExtensionClass).computeSafeIfAny(processor);
  }

  @Nullable
  public E findFirstSafe(C componentManager, Predicate<E> predicate) {
    return componentManager.getExtensionPoint(myExtensionClass).findFirstSafe(predicate);
  }
}
