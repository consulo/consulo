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
package consulo.continuousIntegration.impl.internal.toolWindow;

import consulo.continuousIntegration.CIBuild;
import consulo.continuousIntegration.CIJob;
import consulo.continuousIntegration.CIServer;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

/**
 * Value of a node in the CI tree. Nodes are equal by {@link #key()}, so a reloaded job or build stays the same node and
 * keeps its expansion and selection; the latest snapshot of a job or build is looked up by the key when rendering.
 *
 * @since 2026-11-09
 */
sealed interface CITreeNode {
    String key();

    boolean isLeaf();

    record Server(CIServer server, String key) implements CITreeNode {
        @Override
        public boolean isLeaf() {
            return false;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return o instanceof Server other && key.equals(other.key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }

    record Job(CIServer server, CIJob job, String key) implements CITreeNode {
        @Override
        public boolean isLeaf() {
            return false;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return o instanceof Job other && key.equals(other.key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }

    record Build(CIServer server, CIJob job, CIBuild build, String key) implements CITreeNode {
        @Override
        public boolean isLeaf() {
            return true;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return o instanceof Build other && key.equals(other.key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }

    /**
     * Placeholder child: loading, nothing found, or the error of the parent.
     */
    record Message(String key, MessageKind kind, LocalizeValue text) implements CITreeNode {
        @Override
        public boolean isLeaf() {
            return true;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return o instanceof Message other && key.equals(other.key) && kind == other.kind;
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }

    enum MessageKind {
        LOADING,
        EMPTY,
        ERROR
    }
}
