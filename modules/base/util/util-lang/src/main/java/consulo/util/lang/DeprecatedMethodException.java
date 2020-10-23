// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.lang;

import consulo.util.lang.reflect.ReflectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class DeprecatedMethodException extends RuntimeException {
  private static final Logger LOG = LoggerFactory.getLogger(DeprecatedMethodException.class);

  private DeprecatedMethodException(@Nonnull String message) {
    super(message);
  }

  public static void report(@Nonnull String message) {
    String msg = "This method in " + ReflectionUtil.findCallerClass(2) + " is deprecated and going to be removed soon. " + message;
    LOG.warn(msg, new DeprecatedMethodException(msg));
  }
}
