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
package consulo.web.ui.impl.internal;

import consulo.ui.impl.tree.TreeNodeImpl;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * @author VISTALL
 * @since 2026-09-25
 */
public final class WebTreeRow<N> {
    private final String myId = UUID.randomUUID().toString();
    private final @Nullable TreeNodeImpl<N> myNode;

    private WebTreeRow(@Nullable TreeNodeImpl<N> node) {
        myNode = node;
    }

    public static <N> WebTreeRow<N> of(TreeNodeImpl<N> node) {
        return new WebTreeRow<>(node);
    }

    public static <N> WebTreeRow<N> placeholder() {
        return new WebTreeRow<>(null);
    }

    public String getId() {
        return myId;
    }

    public @Nullable TreeNodeImpl<N> getNode() {
        return myNode;
    }

    @Override
    public String toString() {
        return "WebTreeRow{node=" + myNode + "}";
    }
}
