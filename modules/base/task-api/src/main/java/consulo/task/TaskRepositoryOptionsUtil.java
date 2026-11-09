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
package consulo.task;

import consulo.util.jdom.JDOMUtil;
import consulo.util.xml.serializer.XmlSerializer;
import org.jdom.Element;

/**
 * @author VISTALL
 * @since 2026-10-31
 */
public final class TaskRepositoryOptionsUtil {
    public static final String OPTIONS_ELEMENT = "options";

    private TaskRepositoryOptionsUtil() {
    }

    public static Element serialize(TaskRepositoryOptions options) {
        Element element = new Element(OPTIONS_ELEMENT);
        XmlSerializer.serializeInto(options, element);
        return element;
    }

    public static <O extends TaskRepositoryOptions> O deserializeInto(Element element, O target) {
        XmlSerializer.deserializeInto(target, element);
        return target;
    }

    public static boolean isEqual(TaskRepositoryOptions left, TaskRepositoryOptions right) {
        return left.getClass() == right.getClass() && JDOMUtil.areElementsEqual(serialize(left), serialize(right));
    }
}
