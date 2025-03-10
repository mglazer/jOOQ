/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Other licenses:
 * -----------------------------------------------------------------------------
 * Commercial licenses for this work are available. These replace the above
 * ASL 2.0 and offer limited warranties, support, maintenance, and commercial
 * database integrations.
 *
 * For more information, please visit: http://www.jooq.org/licenses
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package org.jooq.impl;

// ...
import static org.jooq.SQLDialect.HSQLDB;
// ...
import static org.jooq.impl.DSL.asterisk;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.Names.N_SELECT;
import static org.jooq.impl.Tools.visitSubquery;

import java.util.Set;

import org.jooq.Context;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Function1;
import org.jooq.Record1;
import org.jooq.SQLDialect;
import org.jooq.Select;

/**
 * @author Lukas Eder
 */
@SuppressWarnings("unchecked")
final class ScalarSubquery<T> extends AbstractField<T> implements QOM.ScalarSubquery<T> {

    static final Set<SQLDialect> NO_SUPPORT_WITH_IN_SCALAR_SUBQUERY = SQLDialect.supportedBy(HSQLDB);
    final Select<?>              query;
    final boolean                predicandSubquery;

    ScalarSubquery(Select<?> query, DataType<T> type, boolean predicandSubquery) {
        super(N_SELECT, type);

        this.query = query;
        this.predicandSubquery = predicandSubquery;
    }

    @Override
    public final void accept(Context<?> ctx) {
        SelectQueryImpl<?> q = Tools.selectQueryImpl(query);







        // HSQLDB allows for using WITH inside of IN, see: https://sourceforge.net/p/hsqldb/bugs/1617/
        // We'll still emulate CTE in scalar subqueries with a derived tables in all cases.
        if (q != null && q.with != null && NO_SUPPORT_WITH_IN_SCALAR_SUBQUERY.contains(ctx.dialect()))
            visitSubquery(ctx, select(asterisk()).from(query.asTable("t")), false, false, predicandSubquery);
        else
            visitSubquery(ctx, query, false, false, predicandSubquery);
    }

    // -------------------------------------------------------------------------
    // XXX: Query Object Model
    // -------------------------------------------------------------------------

    @Override
    public final Function1<? super Select<? extends Record1<T>>, ? extends Field<T>> $constructor() {
        return s -> new ScalarSubquery<>((Select<?>) s, (DataType<T>) Tools.scalarType(s), predicandSubquery);
    }

    @Override
    public final Select<? extends Record1<T>> $arg1() {
        return (Select<? extends Record1<T>>) query;
    }
}
