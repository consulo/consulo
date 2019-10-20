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

import org.picocontainer.ComponentAdapter;
import org.picocontainer.Parameter;
import org.picocontainer.PicoContainer;
import org.picocontainer.PicoVisitor;

import javax.inject.Provider;

/**
 * @author VISTALL
 * @since 2018-08-26
 */
class ProviderParameter implements Parameter {
  private static class ProviderImpl implements Provider {
    private PicoContainer myContainer;
    private Class myClass;

    private volatile Object myValue;

    private ProviderImpl(PicoContainer container, Class aClass) {
      myContainer = container;
      myClass = aClass;
    }

    @Override
    public Object get() {
      Object value = myValue;
      if (value != null) {
        myContainer = null;
        myClass = null;
        return value;
      }

      value = myContainer.getComponentInstance(myClass);
      myValue = value;
      return value;
    }
  }

  private final Class<?> myType;

  ProviderParameter(Class<?> type) {
    myType = type;
  }

  @Override
  public Object resolveInstance(PicoContainer picoContainer, ComponentAdapter componentAdapter, Class aClass) {
    return new ProviderImpl(picoContainer, myType);
  }

  @Override
  public boolean isResolvable(PicoContainer picoContainer, ComponentAdapter componentAdapter, Class aClass) {
    return picoContainer.getComponentAdapter(myType) != null;
  }

  @Override
  public void verify(PicoContainer picoContainer, ComponentAdapter componentAdapter, Class aClass) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void accept(PicoVisitor picoVisitor) {
    throw new UnsupportedOperationException();
  }
}
