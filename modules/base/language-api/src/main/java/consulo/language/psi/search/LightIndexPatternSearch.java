/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.psi.search;

import consulo.application.util.query.ExtensibleQueryFactory;

/**
 * @author irengrig
 * @since 2011-02-19
 */
public class LightIndexPatternSearch extends ExtensibleQueryFactory<IndexPatternOccurrence, IndexPatternSearch.SearchParameters> {
    public static final LightIndexPatternSearch SEARCH = new LightIndexPatternSearch();

    public LightIndexPatternSearch() {
        super(LightIndexPatternSearcher.class);
    }
}
