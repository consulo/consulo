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
package consulo.annotation.component;

/**
 * @author VISTALL
 * @since 13-Jun-22
 */
public final class ComponentProfiles {
  public static final int ANY = 0;

  public static final int PRODUCTION = 1 << 1;

  /**
   * Marker for AWT profile implementation, if service inside base impl code, do not conflict with unified impl
   */
  public static final int AWT = 1 << 2;

  /**
   * Marker for Unified profile implementation, used of SWT & Web
   */
  public static final int UNIFIED = 1 << 3;

  private ComponentProfiles() {
  }
}
