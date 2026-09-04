/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ui;

/**
 * How the body of a message box is to be read. Stated by the caller and never guessed from the
 * content - a frontend sniffing for markup is what makes one message render differently in
 * different places. PLAIN is always literal; how much of RICH a frontend can express is its own
 * business, so a caller must not depend on any particular markup surviving.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public enum MessageTextFormat {
    PLAIN,
    RICH
}
