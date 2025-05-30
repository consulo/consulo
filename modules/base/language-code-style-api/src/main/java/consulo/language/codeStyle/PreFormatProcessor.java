/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.codeStyle;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.ast.ASTNode;
import consulo.component.extension.ExtensionPointName;
import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface PreFormatProcessor {
    ExtensionPointName<PreFormatProcessor> EP_NAME = ExtensionPointName.create(PreFormatProcessor.class);

    /**
     * Callback to be invoked before formatting. Implementation is allowed to do the following:
     * <pre>
     * <ul>
     *   <li>
     *     return not given text range but adjusted one. E.g. we want to reformat a field at a java class but
     *     have 'align field in columns' settings on. That's why the range should be expanded in order to
     *     cover all fields group;
     *   </li>
     *   <li>
     *     another use-case might be target PSI file/document modification (e.g. change words case). The only requirement here
     *     is that given element stays valid;
     *   </li>
     * </ul>
     * </pre>
     *
     * <b>Note:</b> the callback might expect to be called for every injected root at the target formatting context.
     *
     * @param element target element which contents can be adjusted if necessary
     * @param range   target range within the given element
     * @return range recommended to use for further processing
     */
    @Nonnull
    TextRange process(@Nonnull ASTNode element, @Nonnull TextRange range);

    /**
     * Returns true if this preprocessor changes only whitespaces and can run when the canChangeWhiteSpacesOnly flag is passed to the formatter.
     */
    default boolean changesWhitespacesOnly() {
        return false;
    }
}
