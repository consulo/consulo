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

import consulo.platform.PlatformUser;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.Map;

/**
 * @author VISTALL
 * @since 25/04/2023
 */
public class PlatformUserImpl implements PlatformUser {
  private final String myUserName;
  private final Path myUserPath;

  public PlatformUserImpl(Map<String, String> jvmProperties) {
    myUserName = jvmProperties.get("user.name");
    myUserPath = Path.of(jvmProperties.get("user.home"));
  }

  @Override
  public boolean superUser() {
    return false;
  }

  @Nonnull
  @Override
  public String name() {
    return myUserName;
  }

  @Nonnull
  @Override
  public Path homePath() {
    return myUserPath;
  }
}
