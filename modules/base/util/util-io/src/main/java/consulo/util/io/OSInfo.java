/*
 * Copyright 2013-2022 consulo.io
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
package consulo.util.io;

/**
 * @author VISTALL
 * @since 24/01/2022
 * <p>
 * Internal version of OS check
 */
class OSInfo {
  static final String OS_NAME = System.getProperty("os.name");

  private static final String _OS_NAME = OS_NAME.toLowerCase();
  static final boolean isWindows = _OS_NAME.startsWith("windows");
  static final boolean isOS2 = _OS_NAME.startsWith("os/2") || _OS_NAME.startsWith("os2");
  static final boolean isMac = _OS_NAME.startsWith("mac");
  static final boolean isLinux = _OS_NAME.startsWith("linux");
  static final boolean isUnix = !isWindows && !isOS2;

   static final boolean isFileSystemCaseSensitive = isUnix && !isMac || "true".equalsIgnoreCase(System.getProperty("idea.case.sensitive.fs"));
}
