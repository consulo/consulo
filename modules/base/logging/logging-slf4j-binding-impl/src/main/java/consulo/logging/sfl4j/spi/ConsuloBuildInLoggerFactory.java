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

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * @author VISTALL
 */
public class ConsuloBuildInLoggerFactory implements ILoggerFactory {
  @Override
  public Logger getLogger(String name) {
    return new ConsuloBuildInLoggerAdapter(consulo.logging.Logger.getInstance(name));
  }
}
