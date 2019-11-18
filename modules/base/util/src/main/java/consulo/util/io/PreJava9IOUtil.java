/*
 * Copyright 2013-2017 consulo.io
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

import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.Processor;
import com.intellij.util.concurrency.AtomicFieldUpdater;
import consulo.annotations.ReviewAfterMigrationToJRE;
import consulo.logging.Logger;

import javax.annotation.Nonnull;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * @author VISTALL
 * @since 05-Oct-17
 */
@ReviewAfterMigrationToJRE(9)
public class PreJava9IOUtil {
  private static final Logger LOGGER = Logger.getInstance(PreJava9IOUtil.class);

  private static class Java9Cleaner implements Processor<ByteBuffer> {
    private Method myInvokeCleanerMethod;
    private Object myUnsafe;

    Java9Cleaner() {
      // code replace
      // AtomicFieldUpdater.getUnsafe().invokeCleaner(buffer);

      // in JDK9 the "official" dispose method is sun.misc.Unsafe#invokeCleaner
      // since we have to target both jdk 8 and 9 we have to use reflection
      myUnsafe = AtomicFieldUpdater.getUnsafe();
      try {
        myInvokeCleanerMethod = myUnsafe.getClass().getMethod("invokeCleaner", ByteBuffer.class);
        myInvokeCleanerMethod.setAccessible(true);
      }
      catch (Exception e) {
        // something serious, needs to be logged
        LOGGER.error(e);
        throw new RuntimeException(e);
      }
    }

    @Override
    public boolean process(ByteBuffer buffer) {
      try {
        myInvokeCleanerMethod.invoke(myUnsafe, buffer);
        return true;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class Java8Cleaner implements Processor<ByteBuffer> {
    private Method myCleanerMethod;
    private Method myCleanMethod;

    private Java8Cleaner() {
      try {
        Class<?> directBufferClass = Class.forName("sun.nio.ch.DirectBuffer");

        myCleanerMethod = directBufferClass.getDeclaredMethod("cleaner");
        myCleanerMethod.setAccessible(true);

        Class<?> cleanerClass = Class.forName("sun.misc.Cleaner");

        myCleanMethod = cleanerClass.getDeclaredMethod("clean");
        myCleanMethod.setAccessible(true);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public boolean process(ByteBuffer buffer) {
      try {
        Object cleaner = myCleanerMethod.invoke(buffer);
        if (cleaner != null) myCleanMethod.invoke(cleaner); // Already cleaned otherwise
        return true;
      }
      catch (Throwable e) {
        return false;
      }
    }
  }

  private static Processor<ByteBuffer> ourCleaner = SystemInfo.IS_AT_LEAST_JAVA9 ? new Java9Cleaner() : new Java8Cleaner();

  public static boolean invokeCleaner(@Nonnull ByteBuffer buffer) {
    return ourCleaner.process(buffer);
  }
}
