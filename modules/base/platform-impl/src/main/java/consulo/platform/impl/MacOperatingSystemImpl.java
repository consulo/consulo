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

import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.ptr.IntByReference;
import consulo.platform.CpuArchitecture;
import consulo.platform.os.MacOperatingSystem;
import consulo.util.lang.lazy.LazyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 25/04/2023
 */
public class MacOperatingSystemImpl extends PlatformOperatingSystemImpl implements MacOperatingSystem {
  private static final Logger LOG = LoggerFactory.getLogger(MacOperatingSystemImpl.class);

  public final boolean isMacOSMojave;

  private final LazyValue<String> myArchString = LazyValue.notNull(this::detectArchString);

  public MacOperatingSystemImpl(Map<String, String> jvmProperties,
                                Function<String, String> getEnvFunc,
                                Supplier<Map<String, String>> getEnvsSup) {
    super(jvmProperties, getEnvFunc, getEnvsSup);
    isMacOSMojave = isOsVersionAtLeast("10.14");
  }

  @Override
  public boolean isMacMojave() {
    return isMacOSMojave;
  }

  @Override
  public String archString() {
    return myArchString.get();
  }

  /**
   * An x86-64 JVM on an Apple silicon Mac runs translated by Rosetta 2 and sees "x86_64" - the machine is an arm64 one.
   */
  private String detectArchString() {
    String jvmArch = jvmArchString();
    if (CpuArchitecture.of(jvmArch) == CpuArchitecture.X86_64 && isUnderRosetta()) {
      return "arm64";
    }
    return jvmArch;
  }

  // https://developer.apple.com/documentation/apple-silicon/about-the-rosetta-translation-environment
  private static boolean isUnderRosetta() {
    try {
      IntByReference translated = new IntByReference(0);
      IntByReference size = new IntByReference(Integer.BYTES);
      // fails with ENOENT on an Intel Mac - nothing translates there
      int result = SystemB.INSTANCE.sysctlbyname("sysctl.proc_translated", translated.getPointer(), size, null, 0);
      // the value is an int - anything else is not the key asked for
      return result == 0 && size.getValue() == Integer.BYTES && translated.getValue() == 1;
    }
    catch (Throwable e) {
      LOG.debug("Cannot ask whether the process is translated", e);
      return false;
    }
  }
}
