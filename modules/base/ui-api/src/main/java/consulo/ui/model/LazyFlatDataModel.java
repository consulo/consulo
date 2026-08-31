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

import java.util.List;

/**
 * A model whose items a component may take a page at a time rather than all at once.
 * <p/>
 * What a backend does with this is its own business. Every row of a browser frontend is a piece of the document, and
 * handing it a few hundred of them costs a pause the user sees - so it takes the rows it shows and no more. Swing
 * holds the whole model and paints only what is on screen, so it has nothing to gain and keeps reading it whole.
 *
 * @author VISTALL
 */
public interface LazyFlatDataModel<E> extends FlatDataModel<E> {
    /**
     * @return the items in {@code [offset, offset + limit)}, shorter when the model ends before that
     */
    List<E> fetch(int offset, int limit);
}
