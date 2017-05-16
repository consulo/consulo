/*
 * Copyright 2013-2016 consulo.io
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
package consulo.web.servlet.ui;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;
import com.google.gwt.user.server.rpc.SerializationPolicyProvider;

import java.io.InputStream;
import java.util.Set;

/**
 * @author VISTALL
 * @since 15-Jun-16
 */
public class CustomSerializationPolicyProvider implements SerializationPolicyProvider {
  private ClassLoader myClassLoader;

  public CustomSerializationPolicyProvider(ClassLoader classLoader) {
    myClassLoader = classLoader;
  }

  @Override
  public SerializationPolicy getSerializationPolicy(String moduleBaseURL, String serializationPolicyStrongName) {
    try {
      String rpc = "/webResources/consulo.web.gwtUI.impl/" + serializationPolicyStrongName + ".gwt.rpc";

      InputStream resourceAsStream = myClassLoader.getResourceAsStream(rpc);
      SerializationPolicy serializationPolicy = SerializationPolicyLoader.loadFromStream(resourceAsStream, null);

      return new SerializationPolicy() {
        @Override
        public boolean shouldDeserializeFields(Class<?> clazz) {
          return serializationPolicy.shouldDeserializeFields(clazz);
        }

        @Override
        public Set<String> getClientFieldNamesForEnhancedClass(Class<?> clazz) {
          return serializationPolicy.getClientFieldNamesForEnhancedClass(clazz);
        }

        @Override
        public boolean shouldSerializeFields(Class<?> clazz) {
          return serializationPolicy.shouldSerializeFields(clazz);
        }

        @Override
        public void validateDeserialize(Class<?> clazz) throws SerializationException {

        }

        @Override
        public void validateSerialize(Class<?> clazz) throws SerializationException {

        }
      };
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
