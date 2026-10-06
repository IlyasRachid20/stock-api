package com.ilyas.stockapi.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.util.Locale;

/**
 * "Contains, ignoring case" for the search boxes. % and _ typed by the user are matched as they
 * are, not as SQL wildcards, so searching "100%" finds "Case 100% recycled" and nothing else.
 *
 * The searches are built as Specifications rather than @Query strings so that Spring Data still
 * checks ?sort= fields: an unknown field is a 400, not a database error.
 */
final class SearchText {

    private static final char ESCAPE = '\\';

    private SearchText() {
    }

    static Predicate contains(CriteriaBuilder cb, Expression<String> field, String text) {
        String escaped = text.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return cb.like(cb.lower(field), "%" + escaped + "%", ESCAPE);
    }

    static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
