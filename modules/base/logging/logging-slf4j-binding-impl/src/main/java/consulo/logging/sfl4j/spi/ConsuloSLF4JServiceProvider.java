/*
 * Copyright 2013-2020 consulo.io
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

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMDCAdapter;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * @author VISTALL
 * @since 2020-10-23
 */
public class ConsuloSLF4JServiceProvider implements SLF4JServiceProvider {
  private static ConsuloBuildInLoggerFactory ourLoggerFactory = new ConsuloBuildInLoggerFactory();
  private final IMarkerFactory myMarkerFactory = new BasicMarkerFactory();

  @Override
  public ILoggerFactory getLoggerFactory() {
    return ourLoggerFactory;
  }

  @Override
  public IMarkerFactory getMarkerFactory() {
    return myMarkerFactory;
  }

  @Override
  public MDCAdapter getMDCAdapter() {
    return new BasicMDCAdapter();
  }

  @Override
  public String getRequesteApiVersion() {
    return "1.8";
  }

  @Override
  public void initialize() {

  }
}
