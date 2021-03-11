/*
 * Copyright 2013-2021 consulo.io
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
package consulo.container.impl.classloader.proxy;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 11/03/2021
 */
public class ProxyDescription {
  private final Class mySuperClass;
  private final Class[] myInterfaces;
  private final boolean myInterceptObjectMethods;

  private final int myHashCode;

  public ProxyDescription(final Class superClass, final Class[] interfaces, boolean interceptObjectMethods) {
    mySuperClass = superClass;
    myInterfaces = interfaces;
    myInterceptObjectMethods = interceptObjectMethods;

    //FIXME [VISTALL] use Arrays.hashCode(myInterfaces) for interfaces inside hashCode()
    myHashCode = Objects.hash(mySuperClass, Arrays.hashCode(myInterfaces), myInterceptObjectMethods);
  }

  public Class getSuperClass() {
    return mySuperClass;
  }

  public Class[] getInterfaces() {
    return myInterfaces;
  }

  public boolean isInterceptObjectMethods() {
    return myInterceptObjectMethods;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProxyDescription that = (ProxyDescription)o;
    return myInterceptObjectMethods == that.myInterceptObjectMethods && Objects.equals(mySuperClass, that.mySuperClass) && Arrays.equals(myInterfaces, that.myInterfaces);
  }

  @Override
  public int hashCode() {
    return myHashCode;
  }

  @Override
  public String toString() {
    return "ProxyDescription{" +
           "mySuperClass=" +
           mySuperClass +
           ", myInterfaces=" +
           (myInterfaces == null ? null : Arrays.asList(myInterfaces)) +
           ", myInterceptObjectMethods=" +
           myInterceptObjectMethods +
           '}';
  }
}