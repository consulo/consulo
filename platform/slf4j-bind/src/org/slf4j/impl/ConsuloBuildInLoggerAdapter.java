/*
 * Copyright 2013-2015 must-be.org
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
package org.slf4j.impl;

import com.intellij.BundleBase;
import com.intellij.openapi.diagnostic.Logger;
import org.slf4j.helpers.MarkerIgnoringBase;

import java.io.Serializable;

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
    myLogger.debug(BundleBase.format(format, arg));
  }

  @Override
  public void debug(String format, Object arg1, Object arg2) {
    debug(BundleBase.format(format, arg1, arg2));
  }

  @Override
  public void debug(String format, Object... arguments) {
    debug(BundleBase.format(format, arguments));
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
    info(BundleBase.format(format, arg));
  }

  @Override
  public void info(String format, Object arg1, Object arg2) {
    info(BundleBase.format(format, arg1, arg2));
  }

  @Override
  public void info(String format, Object... arguments) {
    info(BundleBase.format(format, arguments));
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
    warn(BundleBase.format(format, arg));
  }

  @Override
  public void warn(String format, Object... arguments) {
    warn(BundleBase.format(format, arguments));
  }

  @Override
  public void warn(String format, Object arg1, Object arg2) {
    warn(BundleBase.format(format, arg1, arg2));
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
    error(BundleBase.format(format, arg));
  }

  @Override
  public void error(String format, Object arg1, Object arg2) {
    error(BundleBase.format(format, arg1, arg2));
  }

  @Override
  public void error(String format, Object... arguments) {
    error(BundleBase.format(format, arguments));
  }

  @Override
  public void error(String msg, Throwable t) {
    myLogger.error(msg, t);
  }
}
