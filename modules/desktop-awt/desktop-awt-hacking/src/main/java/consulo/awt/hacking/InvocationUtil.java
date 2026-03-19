// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.awt.hacking;

import org.jspecify.annotations.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InvocationEvent;
import java.lang.reflect.Field;

public class InvocationUtil {
  
  public static final Class<? extends Runnable> REPAINT_PROCESSING_CLASS = findProcessingClass();
  
  private static final Field INVOCATION_EVENT_RUNNABLE_FIELD = findRunnableField();

  public InvocationUtil() {
  }

  public static @Nullable Runnable extractRunnable(AWTEvent event) {
    if (event instanceof InvocationEvent) {
      try {
        return (Runnable)INVOCATION_EVENT_RUNNABLE_FIELD.get(event);
      }
      catch (IllegalAccessException ignore) {
      }
    }
    return null;
  }

  
  private static Class<? extends Runnable> findProcessingClass() {
    try {
      return Class.forName("javax.swing.RepaintManager$ProcessingRunnable", false, InvocationUtil.class.getClassLoader()).asSubclass(Runnable.class);
    }
    catch (ClassNotFoundException e) {
      throw new InternalAPIChangedException(RepaintManager.class, e);
    }
  }

  
  private static Field findRunnableField() {
    for (Class<?> aClass = InvocationEvent.class; aClass != null; aClass = aClass.getSuperclass()) {
      try {
        Field result = aClass.getDeclaredField("runnable");
        result.setAccessible(true);
        return result;
      }
      catch (NoSuchFieldException ignore) {
      }
    }

    throw new InternalAPIChangedException(InvocationEvent.class, new NoSuchFieldException("Class: " + InvocationEvent.class + " fieldName: " + "runnable" + " fieldType: " + Runnable.class));
  }

  private static final class InternalAPIChangedException extends RuntimeException {
    InternalAPIChangedException(Class<?> targetClass, @Nullable ReflectiveOperationException cause) {
      super(targetClass + " class internal API has been changed", cause);
    }
  }
}
