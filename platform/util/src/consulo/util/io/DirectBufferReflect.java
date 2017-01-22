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
package consulo.util.io;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;
import sun.nio.ch.DirectBuffer;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * @author VISTALL
 * @since 22-Nov-16
 * <p/>
 * on => Java 8 sun.misc.Cleaner
 * on >= Java 9 jdk.internal.ref.Cleaner
 */
public class DirectBufferReflect {
  public static final Logger LOGGER = Logger.getInstance(DirectBufferReflect.class);

  private static final NotNullLazyValue<Couple<Method>> ourCleanMethods = new NotNullLazyValue<Couple<Method>>() {
    @NotNull
    @Override
    protected Couple<Method> compute() {
      try {
        Method cleanerMethod = DirectBuffer.class.getDeclaredMethod("cleaner");

        Class<?> returnType = cleanerMethod.getReturnType();

        return Couple.of(cleanerMethod, returnType.getDeclaredMethod("clean"));
      }
      catch (Exception e) {
        throw new Error(e);
      }
    }
  };

  public static void clean(ByteBuffer directBuffer) {
    if (!(directBuffer instanceof DirectBuffer)) {
      throw new IllegalArgumentException("Required instance of DirectBuffer");
    }

    Couple<Method> value = ourCleanMethods.getValue();
    try {
      Object cleaner = value.getFirst().invoke(directBuffer);
      if (cleaner != null) {
        value.getSecond().invoke(cleaner);
      }
    }
    catch (Exception e) {
      LOGGER.error(e);
    }
  }
}
