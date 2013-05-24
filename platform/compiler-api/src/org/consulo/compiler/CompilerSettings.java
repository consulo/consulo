package org.consulo.compiler;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.options.Configurable;
import org.jdom.Element;

/**
 * @author VISTALL
 * @since 19:58/24.05.13
 */
public interface CompilerSettings<T> extends PersistentStateComponent<Element>{
  Configurable createConfigurable();
}
