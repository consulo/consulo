/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.execution.configurations;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.openapi.vfs.encoding.EncodingProjectManager;
import com.intellij.util.EnvironmentUtil;
import consulo.annotation.UsedInPlugin;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;

@UsedInPlugin
public class EncodingEnvironmentUtil {
  private static final Logger LOG = Logger.getInstance(EncodingEnvironmentUtil.class);

  private static final String LC_ALL = "LC_ALL";
  private static final String LC_CTYPE = "LC_CTYPE";
  private static final String LANG = "LANG";

  /**
   * @param commandLine GeneralCommandLine instance
   * @deprecated GeneralCommandLine now contains the variable by default
   * <p>
   * Sets default encoding on Mac if it's undefined. <br/>
   * On Mac default character encoding is defined by several environment variables: LC_ALL, LC_CTYPE and LANG.
   * See <a href='http://www.gnu.org/software/gettext/manual/html_node/Locale-Environment-Variables.html'>details</a>.
   * <p>
   * Unfortunately, Mac OSX has a special behavior:<br/>
   * These environment variables aren't passed to an IDE, if the IDE is launched from Spotlight.<br/>
   * Unfortunately, even {@link EnvironmentUtil#getEnvironment()} doesn't have these variables.<p/>
   * As a result, no encoding environment variables are passed to Ruby/Node.js/Python/other processes that are launched from IDE.
   * Thus, these processes wrongly assume that the default encoding is US-ASCII.
   * <p>
   * <p>
   * The workaround this method applies is to set LC_CTYPE environment variable if LC_ALL, LC_CTYPE or LANG aren't set before. <br/>
   * LC_CTYPE value is taken from "Settings | File Encodings".
   */
  public static void setLocaleEnvironmentIfMac(@Nonnull GeneralCommandLine commandLine) {
    if (SystemInfo.isMac && !isLocaleDefined(commandLine)) {
      setLocaleEnvironment(commandLine.getEnvironment(), commandLine.getCharset());
    }
  }

  /**
   * @deprecated use {@link EnvironmentUtil#getEnvironmentMap()}
   * <p>
   * Sets default encoding on Mac if it's undefined. <br/>
   */
  public static void setLocaleEnvironmentIfMac(@Nonnull Map<String, String> env, @Nonnull Charset charset) {
    if (SystemInfo.isMac && !isLocaleDefined(env)) {
      setLocaleEnvironment(env, charset);
    }
  }

  private static void setLocaleEnvironment(@Nonnull Map<String, String> env, @Nonnull Charset charset) {
    env.put(LC_CTYPE, formatLocaleValue(charset));
    if (LOG.isDebugEnabled()) {
      LOG.debug("Fixed mac locale: " + charset.name());
    }
  }

  @Nonnull
  private static String formatLocaleValue(@Nonnull Charset charset) {
    Locale locale = Locale.getDefault();
    String language = locale.getLanguage();
    String country = locale.getCountry();
    return (language.isEmpty() || country.isEmpty() ? "en_US" : language + "_" + country) + "." + charset.name();
  }

  private static boolean isLocaleDefined(@Nonnull GeneralCommandLine commandLine) {
    return isLocaleDefined(commandLine.getEnvironment()) || isLocaleDefined(commandLine.getParentEnvironment());
  }

  private static boolean isLocaleDefined(@Nonnull Map<String, String> env) {
    return !env.isEmpty() && (env.containsKey(LC_CTYPE) || env.containsKey(LC_ALL) || env.containsKey(LANG));
  }

  /**
   * @deprecated use {@link #setLocaleEnvironmentIfMac(GeneralCommandLine)} instead (to be removed in IDEA 16)
   */
  public static void fixDefaultEncodingIfMac(@Nonnull GeneralCommandLine commandLine, @Nullable Project project) {
    if (SystemInfo.isMac && !isLocaleDefined(commandLine)) {
      setLocaleEnvironment(commandLine.getEnvironment(), getCharset(project));
    }
  }

  /**
   * @deprecated use {@link #setLocaleEnvironmentIfMac(Map, Charset)} instead (to be removed in IDEA 16)
   */
  public static void fixDefaultEncodingIfMac(@Nonnull Map<String, String> env, @Nullable Project project) {
    if (SystemInfo.isMac && !isLocaleDefined(env)) {
      setLocaleEnvironment(env, getCharset(project));
    }
  }

  private static Charset getCharset(Project project) {
    return (project != null ? EncodingProjectManager.getInstance(project) : EncodingManager.getInstance()).getDefaultCharset();
  }
}
