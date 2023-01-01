/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.webBrowser.firefox;

import consulo.configurable.Configurable;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.Tag;
import consulo.webBrowser.BrowserSpecificSettings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class FirefoxSettings extends BrowserSpecificSettings {
  private String myProfilesIniPath;
  private String myProfile;

  public FirefoxSettings() {
  }

  public FirefoxSettings(@Nullable String profilesIniPath, @Nullable String profile) {
    myProfilesIniPath = StringUtil.nullize(profilesIniPath);
    myProfile = StringUtil.nullize(profile);
  }

  @Nullable
  @Tag("profiles-ini-path")
  public String getProfilesIniPath() {
    return myProfilesIniPath;
  }

  public void setProfilesIniPath(@Nullable String value) {
    myProfilesIniPath = PathUtil.toSystemIndependentName(StringUtil.nullize(value));
  }

  @Nullable
  @Tag("profile")
  public String getProfile() {
    return myProfile;
  }

  public void setProfile(@Nullable String value) {
    myProfile = StringUtil.nullize(value);
  }

  @Nonnull
  @Override
  public Configurable createConfigurable() {
    return new FirefoxSettingsConfigurable(this);
  }

  @Nullable
  public File getProfilesIniFile() {
    if (myProfilesIniPath != null) {
      return new File(FileUtil.toSystemDependentName(myProfilesIniPath));
    }
    return FirefoxUtil.getDefaultProfileIniPath();
  }

  @Nonnull
  @Override
  public List<String> getAdditionalParameters() {
    List<FirefoxProfile> profiles = FirefoxUtil.computeProfiles(getProfilesIniFile());
    if (profiles.size() >= 2) {
      FirefoxProfile profile = FirefoxUtil.findProfileByNameOrDefault(myProfile, profiles);
      if (profile != null && !profile.isDefault()) {
        return Arrays.asList("-P", profile.getName());
      }
    }
    return Collections.emptyList();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    FirefoxSettings settings = (FirefoxSettings)o;
    return Comparing.equal(myProfilesIniPath, settings.myProfilesIniPath) &&
           Comparing.equal(myProfile, settings.myProfile);
  }
}
