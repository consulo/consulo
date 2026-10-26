// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.details;

import org.jspecify.annotations.Nullable;

import java.util.Date;
import java.util.function.Function;

/**
 * @param committedDate {@code null} when the commit tells no date
 */
public record CommitPresentation(String titleHtml, String descriptionHtml, String author, @Nullable Date committedDate) {
    /**
     * The author and, when known, the date - the way a commit is introduced under its title.
     */
    public String authorAndDate(Function<Date, String> dateFormatter) {
        Date date = committedDate;
        return date == null ? author : author + ", " + dateFormatter.apply(date);
    }
}
