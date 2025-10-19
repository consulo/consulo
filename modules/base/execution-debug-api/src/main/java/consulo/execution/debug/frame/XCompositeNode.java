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
package consulo.execution.debug.frame;

import consulo.annotation.DeprecationInfo;
import consulo.execution.debug.Obsolescent;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Represents a node with children in a debugger tree. This interface isn't supposed to be implemented by a plugin.
 *
 * @author nik
 * @see XValueContainer
 */
public interface XCompositeNode extends Obsolescent {
    /**
     * If node has more children than this constant it's recommended to stop adding children and call {@link #tooManyChildren(int)} method
     */
    int MAX_CHILDREN_TO_SHOW = 100;

    /**
     * Add children to the node.
     *
     * @param children child nodes to add
     * @param last     <code>true</code> if all children added
     */
    void addChildren(@Nonnull XValueChildrenList children, boolean last);

    /**
     * Add an ellipsis node ("...") indicating that the node has too many children. If user double-click on that node
     * {@link XValueContainer#computeChildren(XCompositeNode)} method will be called again to add next children.
     *
     * @param remaining number of remaining children or <code>-1</code> if unknown
     * @see #MAX_CHILDREN_TO_SHOW
     */
    void tooManyChildren(int remaining);

    /**
     * Use sort specified in data view settings (alreadySorted false, by default) or not
     */
    void setAlreadySorted(boolean alreadySorted);

    /**
     * Indicates that an error occurs
     *
     * @param errorMessage message describing the error
     */
    default void setErrorMessage(@Nonnull LocalizeValue errorMessage) {
        setErrorMessage(errorMessage, null);
    }

    /**
     * Indicates that an error occurs
     *
     * @param errorMessage message describing the error
     * @param link         describes a hyperlink which will be appended to the error message
     */
    void setErrorMessage(@Nonnull LocalizeValue errorMessage, @Nullable XDebuggerTreeNodeHyperlink link);

    void setMessage(@Nonnull LocalizeValue message,
                    @Nullable Image icon,
                    @Nonnull SimpleTextAttributes attributes,
                    @Nullable XDebuggerTreeNodeHyperlink link);

    @Deprecated
    @DeprecationInfo("Use with localize value")
    default void setMessage(@Nonnull String message,
                            @Nullable Image icon,
                            @Nonnull SimpleTextAttributes attributes,
                            @Nullable XDebuggerTreeNodeHyperlink link) {
        setMessage(LocalizeValue.of(message), icon, attributes, link);
    }

    /**
     * Indicates that an error occurs
     *
     * @param errorMessage message describing the error
     */
    @Deprecated
    @DeprecationInfo("Use #setErrorMessage(LocalizeValue)")
    default void setErrorMessage(@Nonnull String errorMessage) {
        setErrorMessage(LocalizeValue.of(errorMessage));
    }

    /**
     * Indicates that an error occurs
     *
     * @param errorMessage message describing the error
     * @param link         describes a hyperlink which will be appended to the error message
     */
    @Deprecated
    @DeprecationInfo("Use #setErrorMessage(LocalizeValue, XDebuggerTreeNodeHyperlink)")
    default void setErrorMessage(@Nonnull String errorMessage, @Nullable XDebuggerTreeNodeHyperlink link) {
        setErrorMessage(LocalizeValue.of(errorMessage), link);
    }
}
