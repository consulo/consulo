/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.pattern;

import consulo.language.util.ProcessingContext;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.function.BiPredicate;

/**
 * @author peter
 */
public abstract class PatternCondition<T> {
  private static final Logger LOG = Logger.getInstance(PatternCondition.class);
  private static final String PARAMETER_FIELD_PREFIX = "val$";
  private final String myDebugMethodName;

  public PatternCondition(@Nullable String debugMethodName) {
    myDebugMethodName = debugMethodName;
  }

  public String getDebugMethodName() {
    return myDebugMethodName;
  }

  private static void appendValue(StringBuilder builder, String indent, Object obj) {
    if (obj instanceof ElementPattern) {
      ((ElementPattern)obj).getCondition().append(builder, indent + "  ");
    }
    else if (obj instanceof Object[]) {
      appendArray(builder, indent, (Object[])obj);
    }
    else if (obj instanceof Collection) {
      appendArray(builder, indent, ((Collection)obj).toArray());
    }
    else if (obj instanceof String) {
      builder.append('\"').append(obj).append('\"');
    }
    else {
      builder.append(obj);
    }
  }

  protected static void appendArray(StringBuilder builder, String indent, Object[] objects) {
    builder.append("[");
    boolean first = true;
    for (Object o : objects) {
      if (!first) {
        builder.append(", ");
      }
      first = false;
      appendValue(builder, indent, o);
    }
    builder.append("]");
  }

  public abstract boolean accepts(@Nonnull T t, ProcessingContext context);

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    append(builder, "");
    return builder.toString();
  }

  public void append(StringBuilder builder, String indent) {
    builder.append(myDebugMethodName);
    builder.append("(");
    appendParams(builder, indent);
    builder.append(")");
  }

  private void appendParams(final StringBuilder builder, final String indent) {
    processParameters(new BiPredicate<String, Object>() {
      int count;
      String prevName;
      int prevOffset;

      @Override
      public boolean test(String name, Object value) {
        count++;
        if (count == 2) builder.insert(prevOffset, prevName + "=");
        if (count > 1) builder.append(", ");
        prevOffset = builder.length();
        if (count > 1) builder.append(name).append("=");
        appendValue(builder, indent, value);
        prevName = name;
        return true;
      }
    });
  }

  // this code eats CPU, for debug purposes ONLY
  public boolean processParameters(BiPredicate<String, Object> processor) {
    for (Class aClass = getClass(); aClass != null; aClass = aClass.getSuperclass()) {
      for (Field field : aClass.getDeclaredFields()) {
        if (!Modifier.isStatic(field.getModifiers()) &&
            (((field.getModifiers() & 0x1000 /*Modifer.SYNTHETIC*/) == 0 && !aClass.equals(PatternCondition.class)) || field.getName().startsWith(PARAMETER_FIELD_PREFIX))) {
          String name = field.getName();
          String fixedName = name.startsWith(PARAMETER_FIELD_PREFIX) ? name.substring(PARAMETER_FIELD_PREFIX.length()) : name;
          Object value = getFieldValue(field);
          if (!processor.test(fixedName, value)) return false;
        }
      }
    }
    return true;
  }

  private Object getFieldValue(Field field) {
    boolean accessible = field.isAccessible();
    try {
      field.setAccessible(true);
      return field.get(this);
    }
    catch (IllegalAccessException e) {
      LOG.error(e);
    }
    finally {
      field.setAccessible(accessible);
    }
    return null;
  }

}
