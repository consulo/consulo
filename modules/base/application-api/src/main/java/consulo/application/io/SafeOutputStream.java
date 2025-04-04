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
package consulo.application.io;

import java.io.OutputStream;

/**
 * <p>Attempts to prevent data loss if OS crash happens during write.</p>
 *
 * <p>The class creates a backup copy before overwriting the target file, and issues {@code fsync()} afterwards. The behavior is based
 * on an assumption that after a crash either a target remains unmodified (i.e. unfinished write doesn't reach the disc),
 * or a backup file exists along with a partially overwritten target file.</p>
 *
 * <p><b>The class is not thread-safe</b>; expected to be used within try-with-resources or an equivalent statement.</p>
 */
public abstract class SafeOutputStream extends OutputStream {
}
