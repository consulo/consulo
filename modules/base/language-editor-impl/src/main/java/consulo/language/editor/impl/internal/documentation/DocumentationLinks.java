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
package consulo.language.editor.impl.internal.documentation;

import consulo.language.editor.documentation.DocumentationManagerProtocol;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
public final class DocumentationLinks {
    public static final String EXTERNAL_DOC = "external_doc";

    private DocumentationLinks() {
    }

    public static boolean isPsiElementLink(String href) {
        return href.startsWith(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL);
    }

    public static String getPsiElementReference(String href) {
        String reference = href.substring(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL.length());
        int separator = reference.lastIndexOf(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL_REF_SEPARATOR);
        return separator >= 0 ? reference.substring(0, separator) : reference;
    }

    public static @Nullable String getPsiElementAnchor(String href) {
        String reference = href.substring(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL.length());
        int separator = reference.lastIndexOf(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL_REF_SEPARATOR);
        if (separator < 0) {
            return null;
        }
        return reference.substring(separator + DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL_REF_SEPARATOR.length());
    }
}
