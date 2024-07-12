/*
 * Copyright 2013-2019 consulo.io
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
package consulo.container;

import consulo.util.nodep.SystemInfoRt;

/**
 * @author VISTALL
 * @since 2019-07-16
 */
public interface ExitCodes {
  public static final int USAGE_INFO = 0;
  public static final int VERSION_INFO = 0;
  public static final int STARTUP_EXCEPTION = 1;
  public static final int INSTANCE_CHECK_FAILED = 2;
  public static final int PLUGIN_ERROR = 3;
  public static final int UNSUPPORTED_JAVA_VERSION = 4;
  public static final int OUT_OF_MEMORY = 9;

  public static final int MIN_JAVA_VERSION = 21;

  public static String validateJavaRuntime() {
    if (!SystemInfoRt.isJavaVersionAtLeast(MIN_JAVA_VERSION)) {
      return "Cannot start under Java " + SystemInfoRt.JAVA_RUNTIME_VERSION + ": Java " + MIN_JAVA_VERSION + " or later is required.";
    }
    
    return null;
  }
}
