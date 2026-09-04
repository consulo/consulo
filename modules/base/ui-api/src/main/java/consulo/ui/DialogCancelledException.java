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
package consulo.ui;

/**
 * The user dismissed a box which was asked to treat that as a failure.
 * <p>
 * It is its own type so that a dismissal stays distinguishable from a box which could not be shown
 * at all. It is deliberately not a {@code CancellationException}: a result carrying one of those
 * reports itself as cancelled and hands back a fresh instance, losing the type.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public class DialogCancelledException extends RuntimeException {
    public DialogCancelledException() {
        super("dialog cancelled");
    }
}
