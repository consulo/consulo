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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author max
 */
public class BuildNumber implements Comparable<BuildNumber> {
  private static final String SNAPSHOT = "SNAPSHOT";
  private static final String FALLBACK_VERSION = "999.SNAPSHOT";

  private final int myBaselineVersion;
  private final int myBuildNumber;

  public BuildNumber(int baselineVersion, int buildNumber) {
    myBaselineVersion = baselineVersion;
    myBuildNumber = buildNumber;
  }

  public String asString() {
    StringBuilder builder = new StringBuilder();

    builder.append(myBaselineVersion).append('.');

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

    String code = version;

    int baselineVersionSeparator = code.indexOf('.');
    int baselineVersion;
    int buildNumber;
    if (baselineVersionSeparator > 0) {
      try {
        final String baselineVersionString = code.substring(0, baselineVersionSeparator);
        if (baselineVersionString.trim().isEmpty()) return null;
        baselineVersion = Integer.parseInt(baselineVersionString);
        code = code.substring(baselineVersionSeparator + 1);
      }
      catch (NumberFormatException e) {
        throw new RuntimeException("Invalid version number: " + version + "; plugin name: " + name);
      }

      buildNumber = parseBuildNumber(version, code, name);
    }
    else {
      buildNumber = parseBuildNumber(version, code, name);

      baselineVersion = getBaseLineForHistoricBuilds(buildNumber);
    }

    return new BuildNumber(baselineVersion, buildNumber);
  }

  private static int parseBuildNumber(String version, String code, String name) {
    if (SNAPSHOT.equals(code)) {
      return Integer.MAX_VALUE;
    }
    try {
      return Integer.parseInt(code);
    }
    catch (NumberFormatException e) {
      throw new RuntimeException("Invalid version number: " + version + "; plugin name: " + name);
    }
  }

  public static BuildNumber fallback() {
    return fromString(FALLBACK_VERSION);
  }

  @Override
  public String toString() {
    return asString();
  }

  @Override
  public int compareTo(@NotNull BuildNumber o) {
    if (myBaselineVersion == o.myBaselineVersion) return myBuildNumber - o.myBuildNumber;
    return myBaselineVersion - o.myBaselineVersion;
  }

  public int getBaselineVersion() {
    return myBaselineVersion;
  }

  public int getBuildNumber() {
    return myBuildNumber;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BuildNumber that = (BuildNumber)o;

    if (myBaselineVersion != that.myBaselineVersion) return false;
    if (myBuildNumber != that.myBuildNumber) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = 0;
    result = 31 * result + myBaselineVersion;
    result = 31 * result + myBuildNumber;
    return result;
  }

  private static int getBaseLineForHistoricBuilds(int bn) {
    if (bn == Integer.MAX_VALUE) {
      return Integer.MAX_VALUE; // SNAPSHOTS
    }

    return 1;
  }

  public boolean isSnapshot() {
    return myBuildNumber == Integer.MAX_VALUE;
  }
}
