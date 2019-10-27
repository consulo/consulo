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
package consulo.actionSystem.ex;

import com.intellij.openapi.util.SystemInfo;

/**
 * @author VISTALL
 * @since 8/1/19
 */
public class TopApplicationMenuUtil {
  public static final boolean isMacSystemMenu = SystemInfo.isMac && "true".equals(System.getProperty("apple.laf.useScreenMenuBar"));
}
