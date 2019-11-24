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
package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

/**
 * @author VISTALL
 */
public class StaticLoggerBinder implements LoggerFactoryBinder {
  private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

  public static StaticLoggerBinder getSingleton() {
    return SINGLETON;
  }

  public static String REQUESTED_API_VERSION = "1.6.99";

  private final ILoggerFactory myLoggerFactory;

  private StaticLoggerBinder() {
    myLoggerFactory = new ConsuloBuildInLoggerFactory();
  }

  @Override
  public ILoggerFactory getLoggerFactory() {
    return myLoggerFactory;
  }

  @Override
  public String getLoggerFactoryClassStr() {
    return ConsuloBuildInLoggerFactory.class.getName();
  }
}
