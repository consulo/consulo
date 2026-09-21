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
package consulo.ui.ex.errorTreeView;

import consulo.localize.LocalizeValue;
import consulo.ui.ex.MessageCategory;
import consulo.ui.ex.localize.UILocalize;

/**
 * @author Eugene Zhuravlev
 * @since 2004-11-12
 */
public enum ErrorTreeElementKind {
    INFO("INFO", UILocalize.errorTreeInformation()),
    ERROR("ERROR", UILocalize.errorTreeError()),
    WARNING("WARNING", UILocalize.errorTreeWarning()),
    NOTE("NOTE", UILocalize.errorTreeNote()),
    GENERIC("GENERIC", LocalizeValue.empty());

    private final String myText;
    private final LocalizeValue myPresentableText;

    private ErrorTreeElementKind(String text, LocalizeValue presentableText) {
        myText = text;
        myPresentableText = presentableText;
    }

    @Override
    public String toString() {
        return myText; // for debug purposes
    }

    public LocalizeValue getPresentableText() {
        return myPresentableText;
    }

    public static ErrorTreeElementKind convertMessageFromCompilerErrorType(int type) {
        return switch (type) {
            case MessageCategory.ERROR -> ERROR;
            case MessageCategory.WARNING -> WARNING;
            case MessageCategory.INFORMATION -> INFO;
            case MessageCategory.STATISTICS -> INFO;
            case MessageCategory.SIMPLE -> GENERIC;
            case MessageCategory.NOTE -> NOTE;
            default -> GENERIC;
        };
    }
}
