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
package consulo.platform;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 25-Feb-22
 */
public final class CpuArchitecture {
  public static final CpuArchitecture X86 = new CpuArchitecture("X86", 32);
  public static final CpuArchitecture X86_64 = new CpuArchitecture("X86_64", 64);
  public static final CpuArchitecture AARCH64 = new CpuArchitecture("AARCH64", 64);

  private final String myName;
  private final int myWidth;

  public CpuArchitecture(@Nonnull String name, int width) {
    myName = name;
    myWidth = width;
  }

  public String getName() {
    return myName;
  }

  public int getWidth() {
    return myWidth;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CpuArchitecture that = (CpuArchitecture)o;
    return myWidth == that.myWidth && Objects.equals(myName, that.myName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myName, myWidth);
  }

  @Override
  public String toString() {
    return "CpuArchitecture{" + "myName='" + myName + '\'' + ", myWidth=" + myWidth + '}';
  }
}

