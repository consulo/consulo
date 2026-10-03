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
package consulo.execution.profiler.ui;

import consulo.execution.profiler.BaseCallStackElement;
import consulo.execution.profiler.model.CantBeParsedCall;
import consulo.execution.profiler.model.NativeCall;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;

/**
 * Renders any frame: a {@link NativeCall} as its method followed by its greyed library, an unparsed frame greyed,
 * and every other frame as its full name.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public final class NativeCallStackElementRenderer implements BaseCallStackElementRenderer {
    public static final NativeCallStackElementRenderer INSTANCE = new NativeCallStackElementRenderer();

    private NativeCallStackElementRenderer() {
    }

    @Override
    public void render(BaseCallStackElement element, TextItemPresentation presentation) {
        if (element instanceof NativeCall nativeCall) {
            presentation.append(nativeCall.methodWithClassOrFunction(), TextAttribute.REGULAR);
            if (!nativeCall.getLibrary().isEmpty()) {
                presentation.append(" " + nativeCall.getLibrary(), TextAttribute.GRAYED);
            }
        }
        else if (element instanceof CantBeParsedCall) {
            presentation.append(element.fullName(), TextAttribute.GRAYED);
        }
        else {
            presentation.append(element.fullName(), TextAttribute.REGULAR);
        }
    }

    @Override
    public String getText(BaseCallStackElement element) {
        if (element instanceof NativeCall nativeCall) {
            return nativeCall.methodWithClassOrFunction();
        }
        return element.fullName();
    }
}
