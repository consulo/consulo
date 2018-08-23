/*
 * Copyright 2013-2018 consulo.io
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
package consulo.injecting.pico;

import com.intellij.util.pico.AssignableToComponentAdapter;
import com.intellij.util.pico.CachingConstructorInjectionComponentAdapter;
import consulo.injecting.key.InjectingKey;
import org.picocontainer.*;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-08-23
 */
public class LazyComponentAdapter implements AssignableToComponentAdapter {
  private ComponentAdapter myDelegate;
  private final InjectingKey<?> myKey;

  public LazyComponentAdapter(InjectingKey<?> key) {
    myKey = key;
    myDelegate = null;
  }

  @Override
  public String getComponentKey() {
    return myKey.getTargetClassName();
  }

  @Override
  public Class getComponentImplementation() {
    return myKey.getTargetClass();
  }

  @Override
  public Object getComponentInstance(@Nonnull PicoContainer container) throws PicoInitializationException, PicoIntrospectionException {
    return getDelegate().getComponentInstance(container);
  }

  public ComponentAdapter getDelegateWithoutInitialize() {
    return myDelegate;
  }

  @Nonnull
  public synchronized ComponentAdapter getDelegate() {
    if (myDelegate == null) {
      Class<?> implClass = myKey.getTargetClass();

      myDelegate = new CachingConstructorInjectionComponentAdapter(getComponentKey(), implClass, null, true);
    }
    return myDelegate;
  }

  @Override
  public void verify(final PicoContainer container) throws PicoIntrospectionException {
    getDelegate().verify(container);
  }

  @Override
  public void accept(final PicoVisitor visitor) {
    visitor.visitComponentAdapter(this);
  }

  @Override
  public String getAssignableToClassName() {
    return myKey.getTargetClassName();
  }

  @Override
  public String toString() {
    return "LazyComponentAdapter[" + myKey.getTargetClassName() + "]";
  }
}