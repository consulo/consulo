/*
 * Copyright 2013-2023 consulo.io
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
package consulo.platform.impl;

import consulo.platform.CpuArchitecture;
import consulo.platform.PlatformJvm;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
* @author VISTALL
* @since 25/04/2023
*/
class PlatformJvmImpl implements PlatformJvm {
  private final String myJavaVersion;
  private final String myJavaRuntimeVersion;
  private final String myJavaVendor;
  private final String myJavaName;
  private final CpuArchitecture myCpuArchitecture;

  PlatformJvmImpl(Map<String, String> jvmProperties) {
    myJavaVersion = jvmProperties.getOrDefault("java.version", "n/a");
    myJavaRuntimeVersion = jvmProperties.getOrDefault("java.runtime.version", "n/a");
    myJavaVendor = jvmProperties.getOrDefault("java.vendor", "n/a");
    myJavaName = jvmProperties.getOrDefault("java.vm.name", "n/a");

    String osArch = jvmProperties.getOrDefault("os.arch", "");
    switch (osArch) {
      case "x86_64":
      case "amd64":
        myCpuArchitecture = CpuArchitecture.X86_64;
        break;
      case "i386":
      case "x86":
        myCpuArchitecture = CpuArchitecture.X86;
        break;
      case "arm64":
      case "aarch64":
        myCpuArchitecture = CpuArchitecture.AARCH64;
        break;
      default:
        String name = osArch.toUpperCase(Locale.ROOT);
        int width = 0;
        String sunArchModel = jvmProperties.get("sun.arch.data.model");
        if (sunArchModel != null) {
          width = StringUtil.parseInt(sunArchModel, 0);
        }

        myCpuArchitecture = new CpuArchitecture(name, width);
        break;
    }
  }

  @Nonnull
  @Override
  public String version() {
    return myJavaVersion;
  }

  @Nonnull
  @Override
  public String runtimeVersion() {
    return myJavaRuntimeVersion;
  }

  @Nonnull
  @Override
  public String vendor() {
    return myJavaVendor;
  }

  @Nonnull
  @Override
  public String name() {
    return myJavaName;
  }

  @Nullable
  @Override
  public String getRuntimeProperty(@Nonnull String key) {
    return System.getProperty(key);
  }

  @Nonnull
  @Override
  public Map<String, String> getRuntimeProperties() {
    Properties properties = System.getProperties();
    Map<String, String> map = new LinkedHashMap<>();
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
    }
    return map;
  }

  @Override
  @Nonnull
  public CpuArchitecture arch() {
    return myCpuArchitecture;
  }
}
