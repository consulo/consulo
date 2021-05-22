/*
 * Copyright 2013-2019 consulo.io
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
package consulo.util.lang.reflect.unsafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

/**
 * @author VISTALL
 * @since 2019-12-01
 */
public class UnsafeDelegate {
  private static final Logger LOG = LoggerFactory.getLogger(UnsafeDelegate.class);

  @Nonnull
  public static UnsafeDelegate get() {
    return ourInstance;
  }

  private static final UnsafeDelegate ourInstance = new UnsafeDelegate();

  private Unsafe myUnsafe;

  public UnsafeDelegate() {
    try {
      Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
      theUnsafe.setAccessible(true);
      myUnsafe = (Unsafe)theUnsafe.get(null);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public final boolean compareAndSwapInt(Object o, long offset, int expected, int x) {
    return myUnsafe.compareAndSwapInt(o, offset, expected, x);
  }

  public final boolean compareAndSwapLong(Object o, long offset, long expected, long x) {
    return myUnsafe.compareAndSwapLong(o, offset, expected, x);
  }

  public long objectFieldOffset(Field f) {
    return myUnsafe.objectFieldOffset(f);
  }

  public int getIntVolatile(Object o, long offset) {
    return myUnsafe.getIntVolatile(o, offset);
  }

  public int arrayBaseOffset(Class<?> arrayClass) {
    return myUnsafe.arrayBaseOffset(arrayClass);
  }

  public int arrayIndexScale(Class<?> arrayClass) {
    return myUnsafe.arrayIndexScale(arrayClass);
  }

  public Object getObjectVolatile(Object o, long offset) {
    return myUnsafe.getObjectVolatile(o, offset);
  }

  public final int getAndAddInt(Object o, long offset, int delta) {
    return myUnsafe.getAndAddInt(o, offset, delta);
  }

  public final boolean compareAndSwapObject(Object o, long offset, Object expected, Object x) {
    return myUnsafe.compareAndSwapObject(o, offset, expected, x);
  }

  public void putObjectVolatile(Object o, long offset, Object x) {
    myUnsafe.putObjectVolatile(o, offset, x);
  }

  public boolean shouldBeInitialized(Class<?> clazz) {
    try {
      return myUnsafe.shouldBeInitialized(clazz);
    }
    catch (Throwable e) {
      LOG.error(e.getMessage(), e);
      return false;
    }
  }

  public boolean invokeCleaner(@Nonnull ByteBuffer buffer) {
    try {
      myUnsafe.invokeCleaner(buffer);
      return true;
    }
    catch (Throwable e) {
      LOG.error(e.getMessage(), e);
      return false;
    }
  }
}
