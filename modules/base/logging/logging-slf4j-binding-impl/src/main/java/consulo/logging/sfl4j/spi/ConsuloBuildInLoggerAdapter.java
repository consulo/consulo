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
package consulo.logging.sfl4j.spi;

import com.intellij.BundleBase;
import consulo.logging.Logger;
import org.slf4j.helpers.MarkerIgnoringBase;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author VISTALL
 * @since 09.01.15
 */
public class ConsuloBuildInLoggerAdapter extends MarkerIgnoringBase implements org.slf4j.Logger, Serializable {
  private final Logger myLogger;

  public ConsuloBuildInLoggerAdapter(Logger logger) {
    myLogger = logger;
  }

  @Override
  public boolean isTraceEnabled() {
    return false;
  }

  @Override
  public void trace(String msg) {

  }

  @Override
  public void trace(String format, Object arg) {

  }

  @Override
  public void trace(String format, Object arg1, Object arg2) {

  }

  @Override
  public void trace(String format, Object... arguments) {

  }

  @Override
  public void trace(String msg, Throwable t) {

  }

  @Override
  public boolean isDebugEnabled() {
    return myLogger.isDebugEnabled();
  }

  @Override
  public void debug(String msg) {
    myLogger.debug(msg);
  }

  @Override
  public void debug(String format, Object arg) {
    try {
      myLogger.debug(buildMessage(format, arg));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void debug(String format, Object arg1, Object arg2) {
    debug(buildMessage(format, arg1, arg2));
  }

  @Override
  public void debug(String format, Object... arguments) {
    debug(buildMessage(format, arguments));
  }

  @Override
  public void debug(String msg, Throwable t) {
    myLogger.debug(msg, t);
  }

  @Override
  public boolean isInfoEnabled() {
    return true;
  }

  @Override
  public void info(String msg) {
    myLogger.info(msg);
  }

  @Override
  public void info(String format, Object arg) {
    info(buildMessage(format, arg));
  }

  @Override
  public void info(String format, Object arg1, Object arg2) {
    info(buildMessage(format, arg1, arg2));
  }

  @Override
  public void info(String format, Object... arguments) {
    info(buildMessage(format, arguments));
  }

  @Override
  public void info(String msg, Throwable t) {
    myLogger.info(msg, t);
  }

  @Override
  public boolean isWarnEnabled() {
    return true;
  }

  @Override
  public void warn(String msg) {
    myLogger.warn(msg);
  }

  @Override
  public void warn(String format, Object arg) {
    warn(buildMessage(format, arg));
  }

  @Override
  public void warn(String format, Object... arguments) {
    warn(buildMessage(format, arguments));
  }

  @Override
  public void warn(String format, Object arg1, Object arg2) {
    warn(buildMessage(format, arg1, arg2));
  }

  private String buildMessage(String format, Object... args) {
    try {
      return BundleBase.format(format, args);
    }
    catch (Exception e) {
      return "Fail to build '" + format + "' args: " + Arrays.asList(args);
    }
  }

  @Override
  public void warn(String msg, Throwable t) {
    myLogger.warn(msg, t);
  }

  @Override
  public boolean isErrorEnabled() {
    return true;
  }

  @Override
  public void error(String msg) {
    myLogger.error(msg);
  }

  @Override
  public void error(String format, Object arg) {
    error(buildMessage(format, arg));
  }

  @Override
  public void error(String format, Object arg1, Object arg2) {
    error(buildMessage(format, arg1, arg2));
  }

  @Override
  public void error(String format, Object... arguments) {
    error(buildMessage(format, arguments));
  }

  @Override
  public void error(String msg, Throwable t) {
    myLogger.error(msg, t);
  }
}
