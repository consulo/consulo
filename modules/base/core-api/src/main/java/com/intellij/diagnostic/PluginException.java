// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.diagnostic;

import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;
import consulo.util.PluginExceptionUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * <p>Represents an internal error caused by a plugin. It may happen if the plugin's code fails with an exception, or if the plugin violates
 * some contract of IntelliJ Platform. If such exceptions are thrown or logged via {@link Logger#error(Throwable)}
 * method and reported to JetBrains by user, they may be automatically attributed to corresponding plugins.</p>
 *
 * <p>If the problem is caused by a class, use {@link #createByClass} to create
 * an instance. If the problem is caused by an extension, implement {@link com.intellij.openapi.extensions.PluginAware} in its extension class
 * to get the plugin ID.</p>
 */
public class PluginException extends consulo.container.PluginException {

  public PluginException(String message, Throwable cause, PluginId pluginId) {
    super(message, cause, pluginId);
  }

  public PluginException(Throwable e, PluginId pluginId) {
    super(e, pluginId);
  }

  public PluginException(String message, PluginId pluginId) {
    super(message, pluginId);
  }

  /**
   * Creates an exception caused by a problem in a plugin's code.
   *
   * @param pluginClass a problematic class which caused the error
   */
  @Nonnull
  public static PluginException createByClass(@Nonnull String errorMessage, @Nullable Throwable cause, @Nonnull Class<?> pluginClass) {
    PluginId pluginId = PluginManager.getPluginId(pluginClass);
    return new PluginException(errorMessage, cause, pluginId);
  }

  /**
   * Creates an exception caused by a problem in a plugin's code, takes error message from the cause exception.
   *
   * @param pluginClass a problematic class which caused the error
   */
  @Nonnull
  public static PluginException createByClass(@Nonnull Throwable cause, @Nonnull Class<?> pluginClass) {
    String message = cause.getMessage();

    PluginId pluginId = PluginManager.getPluginId(pluginClass);
    return new PluginException(message != null ? message : "", cause, pluginId);
  }

  /**
   * Log an error caused by a problem in a plugin's code.
   *
   * @param pluginClass a problematic class which caused the error
   */
  public static void logPluginError(@Nonnull Logger logger, @Nonnull String errorMessage, @Nullable Throwable cause, @Nonnull Class<?> pluginClass) {
    PluginExceptionUtil.logPluginError(logger, errorMessage, cause, pluginClass);
  }

  public static void reportDeprecatedUsage(@Nonnull String signature, @Nonnull String details) {
    String message = "'" + signature + "' is deprecated and going to be removed soon. " + details;
    Logger.getInstance(PluginException.class).error(message);
  }

  public static void reportDeprecatedDefault(@Nonnull Class<?> violator, @Nonnull String methodName, @Nonnull String details) {
    String message = "The default implementation of method '" + methodName + "' is deprecated, you need to override it in '" + violator + "'. " + details;
    Logger logger = Logger.getInstance(violator);
    PluginExceptionUtil.logPluginError(logger, message, null, violator);
  }
}
