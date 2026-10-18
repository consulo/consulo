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
 * Lines of a document a review comment is about, {@code start} to {@code end} both included.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public record CodeReviewLineRange(int start, int end) {
    public CodeReviewLineRange {
        if (start > end) {
            throw new IllegalArgumentException("start " + start + " is after end " + end);
        }
    }

    public boolean contains(int line) {
        return line >= start && line <= end;
    }

    public boolean contains(int startLine, int endLine) {
        return startLine >= start && endLine <= end;
    }
}
