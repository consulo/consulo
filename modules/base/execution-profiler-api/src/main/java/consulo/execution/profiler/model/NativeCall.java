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
package consulo.execution.profiler.model;

import consulo.execution.profiler.BaseCallStackElement;
import org.jspecify.annotations.Nullable;

/**
 * A native frame: the library it lives in, plus its class (empty for a plain function) and method or function.
 * Its full name is written {@code library`Class::method}, as DTrace and perf print it.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public class NativeCall extends BaseCallStackElement {
    private static final char LIBRARY_SEPARATOR = '`';
    private static final String CLASS_SEPARATOR = "::";
    private static final String OFFSET_PREFIX = "+0x";

    private final String myLibrary;
    private final String myClassName;
    private final String myMethodOrFunction;

    public NativeCall(String library, String className, String methodOrFunction) {
        myLibrary = library;
        myClassName = className;
        myMethodOrFunction = methodOrFunction;
    }

    /**
     * Reads a frame written as {@code library`Class::method+0xOffset}. The library, the class and the offset are optional.
     *
     * @return the call, or null when the text holds no symbol
     */
    public static @Nullable NativeCall read(String text) {
        String trimmed = text.trim();
        int libraryEnd = trimmed.lastIndexOf(LIBRARY_SEPARATOR);
        String library = libraryEnd < 0 ? "" : trimmed.substring(0, libraryEnd);
        String symbolWithOffset = libraryEnd < 0 ? trimmed : trimmed.substring(libraryEnd + 1);
        int offsetStart = symbolWithOffset.lastIndexOf(OFFSET_PREFIX);
        String symbol = offsetStart > 0 ? symbolWithOffset.substring(0, offsetStart) : symbolWithOffset;
        if (symbol.isEmpty()) {
            return null;
        }

        int parametersStart = symbol.indexOf('(');
        int searchEnd = parametersStart < 0 ? symbol.length() : parametersStart;
        int classEnd = searchEnd < CLASS_SEPARATOR.length()
            ? -1
            : symbol.lastIndexOf(CLASS_SEPARATOR, searchEnd - CLASS_SEPARATOR.length());
        if (classEnd <= 0) {
            return new NativeCall(library, "", symbol);
        }
        return new NativeCall(library, symbol.substring(0, classEnd), symbol.substring(classEnd + CLASS_SEPARATOR.length()));
    }

    public String getLibrary() {
        return myLibrary;
    }

    public String getClassName() {
        return myClassName;
    }

    public String getMethodOrFunction() {
        return myMethodOrFunction;
    }

    /**
     * @return {@code Class::method}, or the bare function name when there is no class
     */
    public String methodWithClassOrFunction() {
        return myClassName.isEmpty() ? myMethodOrFunction : myClassName + CLASS_SEPARATOR + myMethodOrFunction;
    }

    @Override
    public String fullName() {
        return myLibrary.isEmpty() ? methodWithClassOrFunction() : myLibrary + LIBRARY_SEPARATOR + methodWithClassOrFunction();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NativeCall that = (NativeCall) o;
        return myLibrary.equals(that.myLibrary)
            && myClassName.equals(that.myClassName)
            && myMethodOrFunction.equals(that.myMethodOrFunction);
    }

    @Override
    public int hashCode() {
        int result = myLibrary.hashCode();
        result = 31 * result + myClassName.hashCode();
        result = 31 * result + myMethodOrFunction.hashCode();
        return result;
    }
}
