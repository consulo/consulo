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
package consulo.container.internal;

/**
 * @author VISTALL
 * @since 2020-06-01
 */
public class SystemContainerLogger implements ContainerLogger {
  public static final ContainerLogger INSTANCE = new SystemContainerLogger();

  @Override
  public void info(String message) {
    System.out.println(message);
  }

  @Override
  public void warn(String message) {
    System.out.println(message);
  }

  @Override
  public void info(String message, Throwable t) {
    System.out.println(message);
    t.printStackTrace(System.out);
  }

  @Override
  public void error(String message, Throwable t) {
    System.err.println(message);
    t.printStackTrace(System.err);
  }
}
