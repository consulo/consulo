/*
 * Copyright 2013-2024 consulo.io
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
package consulo.language.editor.impl.internal.template;

import consulo.annotation.DeprecationInfo;
import org.jdom.Element;

/**
 * @author VISTALL
 * @since 2024-10-26
 */
@Deprecated
@DeprecationInfo("Drop after fully migration to live template contributors")
public class JavaLegacy {
    public static final String TO_SHORTEN_FQ_NAMES = "toShortenFQNames";
    public static final String USE_STATIC_IMPORT = "useStaticImport";

    public static final String JAVA_SHORTED_FQ_NAMES = "java-shorted-fq-names";
    public static final String JAVA_USE_STATIC_IMPORT = "java-use-static-import";

    public static void read(TemplateImpl template, Element element) {
        String shortedFqName = element.getAttributeValue(TO_SHORTEN_FQ_NAMES);
        if (shortedFqName != null) {
            template.setOptionViaString(JAVA_SHORTED_FQ_NAMES, Boolean.parseBoolean(shortedFqName));
        }

        String useStaticImport = element.getAttributeValue(USE_STATIC_IMPORT);
        if (useStaticImport != null) {
            template.setOptionViaString(JAVA_USE_STATIC_IMPORT, Boolean.parseBoolean(useStaticImport));
        }
    }
}
