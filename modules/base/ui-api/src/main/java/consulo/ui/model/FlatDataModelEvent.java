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
package consulo.ui.model;

/**
 * @author VISTALL
 * @since 2026-08-02
 */
public final class FlatDataModelEvent {
    public enum Type {
        ADDED,
        REMOVED,
        UPDATED,
        RESET
    }

    public static FlatDataModelEvent reset() {
        return new FlatDataModelEvent(Type.RESET, -1, -1);
    }

    public static FlatDataModelEvent of(Type type, int index) {
        return new FlatDataModelEvent(type, index, index);
    }

    public static FlatDataModelEvent of(Type type, int fromIndex, int toIndex) {
        return new FlatDataModelEvent(type, fromIndex, toIndex);
    }

    private final Type myType;
    private final int myFromIndex;
    private final int myToIndex;

    private FlatDataModelEvent(Type type, int fromIndex, int toIndex) {
        myType = type;
        myFromIndex = fromIndex;
        myToIndex = toIndex;
    }

    public Type getType() {
        return myType;
    }

    /**
     * First affected index, inclusive. {@code -1} for {@link Type#RESET}.
     */
    public int getFromIndex() {
        return myFromIndex;
    }

    /**
     * Last affected index, inclusive. {@code -1} for {@link Type#RESET}.
     */
    public int getToIndex() {
        return myToIndex;
    }

    @Override
    public String toString() {
        return "FlatDataModelEvent{" + myType + " [" + myFromIndex + ", " + myToIndex + "]}";
    }
}
