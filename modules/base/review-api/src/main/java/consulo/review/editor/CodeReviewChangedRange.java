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
package consulo.review.editor;

/**
 * Lines a review changed, as the editor shows them: lines {@code line1} until {@code line2} of the document stand where
 * lines {@code baseLine1} until {@code baseLine2} of the review base did. Equal lines are a deletion.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public record CodeReviewChangedRange(int line1, int line2, int baseLine1, int baseLine2) {
    public boolean isDeletion() {
        return line1 == line2;
    }
}
