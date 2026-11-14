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
package consulo.ai.impl.internal.debug;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * Where the instrumented code of a Debug turn writes its log lines.
 *
 * @param url the address taking a line per POST - null while the built-in server does not run
 * @author VISTALL
 * @since 2026-11-14
 */
public record AIDebugEndpoint(Path logFile, @Nullable String url) {
}
