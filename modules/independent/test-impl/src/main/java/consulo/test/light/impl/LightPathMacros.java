/*
 * Copyright 2013-2018 consulo.io
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
package consulo.test.light.impl;

import com.intellij.openapi.application.PathMacros;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightPathMacros extends PathMacros {
  @Override
  public Set<String> getAllMacroNames() {
    return null;
  }

  @Override
  public String getValue(String name) {
    return null;
  }

  @Override
  public void setMacro(String name, String value) {

  }

  @Override
  public void addLegacyMacro(@Nonnull String name, @Nonnull String value) {

  }

  @Override
  public void removeMacro(String name) {

  }

  @Override
  public Set<String> getUserMacroNames() {
    return null;
  }

  @Override
  public Set<String> getSystemMacroNames() {
    return null;
  }

  @Override
  public Collection<String> getIgnoredMacroNames() {
    return null;
  }

  @Override
  public void setIgnoredMacroNames(@Nonnull Collection<String> names) {

  }

  @Override
  public void addIgnoredMacro(@Nonnull String name) {

  }

  @Override
  public boolean isIgnoredMacroName(@Nonnull String macro) {
    return false;
  }

  @Override
  public void removeAllMacros() {

  }

  @Override
  public Collection<String> getLegacyMacroNames() {
    return null;
  }
}
