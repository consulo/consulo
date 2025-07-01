/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.compiler;

import consulo.compiler.localize.CompilerLocalize;

/**
 * @author Eugene Zhuravlev
 * @since 2003-07-10
 */
public class CacheCorruptedException extends Exception {
    public CacheCorruptedException(String message) {
        super((message == null || message.length() == 0) ? CompilerLocalize.errorDependencyInfoOnDiskCorrupted().get() : message);
    }

    public CacheCorruptedException(Throwable cause) {
        super(CompilerLocalize.errorDependencyInfoOnDiskCorrupted().get(), cause);
    }

    public CacheCorruptedException(String message, Throwable cause) {
        super(
            (message == null || message.length() == 0) ? CompilerLocalize.errorDependencyInfoOnDiskCorrupted().get() : message,
            cause
        );
    }
}
