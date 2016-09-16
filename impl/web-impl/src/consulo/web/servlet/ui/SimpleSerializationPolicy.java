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

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.SerializationPolicy;

import java.io.Serializable;

public class SimpleSerializationPolicy extends SerializationPolicy {

  @Override
  public boolean shouldDeserializeFields(Class<?> clazz) {
    return isSerializable(clazz);
  }

  @Override
  public boolean shouldSerializeFields(Class<?> clazz) {
    return isSerializable(clazz);
  }

  @Override
  public void validateDeserialize(Class<?> clazz) throws SerializationException {
    if (!isSerializable(clazz)) {
      throw new SerializationException();
    }
  }

  @Override
  public void validateSerialize(Class<?> clazz) throws SerializationException {
    if (!isSerializable(clazz)) {
      throw new SerializationException();
    }
  }

  private boolean isSerializable(Class<?> clazz) {
    if (clazz != null) {
      if (clazz.isPrimitive() || Serializable.class.isAssignableFrom(clazz) || IsSerializable.class.isAssignableFrom(clazz)) {
        return true;
      }
    }
    return false;

  }
}