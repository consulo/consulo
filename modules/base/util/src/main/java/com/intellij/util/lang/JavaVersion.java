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
package com.intellij.util.lang;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-11-23
 * <p>
 * Do not export JavaVersion from nodep module
 */
public final class JavaVersion implements Comparable<JavaVersion> {
  @Nonnull
  public static JavaVersion parse(String version) {
    return new JavaVersion(consulo.util.nodep.JavaVersion.parse(version));
  }

  @Nullable
  public static JavaVersion tryParse(String versionString) {
    consulo.util.nodep.JavaVersion version = consulo.util.nodep.JavaVersion.tryParse(versionString);
    return version == null ? null : new JavaVersion(version);
  }

  @Nonnull
  public static JavaVersion compose(int feature) {
    return new JavaVersion(consulo.util.nodep.JavaVersion.compose(feature));
  }

  private final consulo.util.nodep.JavaVersion myDelegate;

  /**
   * The major version.
   * Corresponds to the first number of a Java 9+ version string and to the second number of Java 1.0 to 1.8 strings.
   */
  public final int feature;

  /**
   * The minor version.
   * Corresponds to the second number of a Java 9+ version string and to the third number of Java 1.0 to 1.8 strings.
   * Used in version strings prior to Java 1.5, in newer strings is always {@code 0}.
   */
  public final int minor;

  /**
   * The patch version.
   * Corresponds to the third number of a Java 9+ version string and to the number of Java 1.0 to 1.8 strings (one after an underscore).
   */
  public final int update;

  /**
   * The build number.
   * Corresponds to a number prefixed by a plus sign in a Java 9+ version string and by "-b" string in earlier versions.
   */
  public final int build;

  /**
   * {@code true} if the platform is an early access release, {@code false} otherwise (or when not known).
   */
  public final boolean ea;

  public JavaVersion(consulo.util.nodep.JavaVersion delegate) {
    myDelegate = delegate;
    feature = delegate.feature;
    minor = delegate.minor;
    update = delegate.update;
    build = delegate.build;
    ea = delegate.ea;
  }

  public boolean isAtLeast(int feature) {
    return myDelegate.isAtLeast(feature);
  }

  @Override
  public int compareTo(@Nonnull JavaVersion o) {
    return myDelegate.compareTo(o.myDelegate);
  }

  @Override
  public boolean equals(Object obj) {
    if(obj instanceof JavaVersion) {
      return myDelegate.equals(((JavaVersion)obj).myDelegate);
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return myDelegate.hashCode();
  }

  @Override
  public String toString() {
    return myDelegate.toString();
  }
}