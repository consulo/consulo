/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author max
 */
public class BuildNumber implements Comparable<BuildNumber> {
  private static final String SNAPSHOT = "SNAPSHOT";

  private final int myBuildNumber;

  public BuildNumber(int buildNumber) {
    myBuildNumber = buildNumber;
  }

  public String asString() {
    StringBuilder builder = new StringBuilder();

    if (myBuildNumber != Integer.MAX_VALUE) {
      builder.append(myBuildNumber);
    }
    else {
      builder.append(SNAPSHOT);
    }

    return builder.toString();
  }

  public static BuildNumber fromString(String version) {
    return fromString(version, null);
  }

  public static BuildNumber fromString(String version, @Nullable String name) {
    if (version == null) return null;

    int buildNumber = parseBuildNumber(version,  name);

    return new BuildNumber(buildNumber);
  }

  private static int parseBuildNumber(String version, String name) {
    if (SNAPSHOT.equals(version)) {
      return Integer.MAX_VALUE;
    }
    try {
      return Integer.parseInt(version);
    }
    catch (NumberFormatException e) {
      throw new RuntimeException("Invalid version number: " + version + "; plugin name: " + name);
    }
  }

  public static BuildNumber fallback() {
    return fromString(SNAPSHOT);
  }

  @Override
  public String toString() {
    return asString();
  }

  @Override
  public int compareTo(@Nonnull BuildNumber o) {
    return myBuildNumber - o.myBuildNumber;
  }

  public int getBuildNumber() {
    return myBuildNumber;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BuildNumber that = (BuildNumber)o;
    if (myBuildNumber != that.myBuildNumber) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = 0;
    result = 31 * result + myBuildNumber;
    return result;
  }

  public boolean isSnapshot() {
    return myBuildNumber == Integer.MAX_VALUE;
  }
}
