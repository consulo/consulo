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
package consulo.compiler;

import consulo.navigation.Navigatable;

/**
 * @author UNV
 * @since 2026-06-24
 */
public class CompilerMessageBuilderWrapper implements CompileContext.MessageBuilder {
    private final CompileContext.MessageBuilder myDelegate;

    private CompilerMessageBuilderWrapper(CompileContext.MessageBuilder delegate) {
        myDelegate = delegate;
    }

    @Override
    public CompileContext.MessageBuilder url(String url) {
        return myDelegate.url(url);
    }

    @Override
    public CompileContext.MessageBuilder position(int line, int column) {
        return myDelegate.position(line, column);
    }

    @Override
    public CompileContext.MessageBuilder navigatable(Navigatable navigatable) {
        return myDelegate.navigatable(navigatable);
    }

    @Override
    public void add() {
        myDelegate.add();
    }
}
