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

package consulo.test.junit.impl;

import org.junit.jupiter.api.DisplayNameGenerator;

import java.lang.reflect.Method;

/**
 * @author VISTALL
 * @since 2025-01-13
 */
public class NoParamDisplayNameGenerator extends DisplayNameGenerator.Standard {
    @Override
    public String generateDisplayNameForMethod(Class<?> testClass, Method testMethod) {
        return getName(testMethod.getName(), true);
    }

    public static String getName(String name, boolean lower) {
        if (name.startsWith("test")) {
            name = name.substring(4, name.length());
        }

        if (lower) {
            if (Character.isUpperCase(name.charAt(0))) {
                return Character.toLowerCase(name.charAt(0)) + name.substring(1, name.length());
            }
        }
        else {
            if (Character.isLowerCase(name.charAt(0))) {
                return Character.toUpperCase(name.charAt(0)) + name.substring(1, name.length());
            }
        }
        return name;
    }
}
