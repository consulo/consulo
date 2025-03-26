/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.editor.internal;

/**
 * @author VISTALL
 * @since 2025-03-26
 */
public interface ColorPageWeights {
    int GENERAL = Integer.MAX_VALUE;
    int FONT = Integer.MAX_VALUE - 1;
    int DEFAULT_COLORS = Integer.MAX_VALUE - 2;
    int CONSOLE_FONT = Integer.MAX_VALUE - 3;
    int CONSOLE_COLORS = Integer.MAX_VALUE - 4;
    int DEBUGGER = Integer.MAX_VALUE - 5;
    int SCOPES = Integer.MAX_VALUE - 6;
}
