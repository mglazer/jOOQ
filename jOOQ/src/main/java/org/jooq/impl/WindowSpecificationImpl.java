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

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
// ...
// ...
import static org.jooq.SQLDialect.CUBRID;
// ...
// ...
import static org.jooq.SQLDialect.H2;
// ...
// ...
// ...
import static org.jooq.SQLDialect.MARIADB;
// ...
import static org.jooq.SQLDialect.MYSQL;
// ...
// ...
// ...
import static org.jooq.SQLDialect.SQLITE;
// ...
// ...
// ...
// ...
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.one;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.Keywords.K_AND;
import static org.jooq.impl.Keywords.K_BETWEEN;
import static org.jooq.impl.Keywords.K_CURRENT_ROW;
import static org.jooq.impl.Keywords.K_EXCLUDE;
import static org.jooq.impl.Keywords.K_FOLLOWING;
import static org.jooq.impl.Keywords.K_ORDER_BY;
import static org.jooq.impl.Keywords.K_PARTITION_BY;
import static org.jooq.impl.Keywords.K_PRECEDING;
import static org.jooq.impl.Keywords.K_UNBOUNDED_FOLLOWING;
import static org.jooq.impl.Keywords.K_UNBOUNDED_PRECEDING;
import static org.jooq.impl.QOM.FrameExclude.CURRENT_ROW;
import static org.jooq.impl.QOM.FrameExclude.GROUP;
import static org.jooq.impl.QOM.FrameExclude.NO_OTHERS;
import static org.jooq.impl.QOM.FrameExclude.TIES;
import static org.jooq.impl.QOM.FrameUnits.GROUPS;
import static org.jooq.impl.QOM.FrameUnits.RANGE;
import static org.jooq.impl.QOM.FrameUnits.ROWS;
import static org.jooq.impl.Tools.EMPTY_FIELD;
import static org.jooq.impl.Tools.EMPTY_SORTFIELD;
import static org.jooq.impl.Tools.isEmpty;
import static org.jooq.impl.Tools.DataExtendedKey.DATA_WINDOW_FUNCTION;
import static org.jooq.tools.StringUtils.defaultIfNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Function1;
import org.jooq.OrderField;
// ...
import org.jooq.QueryPart;
// ...
import org.jooq.SQLDialect;
import org.jooq.SortField;
// ...
import org.jooq.WindowSpecificationExcludeStep;
import org.jooq.WindowSpecificationFinalStep;
import org.jooq.WindowSpecificationOrderByStep;
import org.jooq.WindowSpecificationPartitionByStep;
import org.jooq.WindowSpecificationRowsAndStep;
import org.jooq.conf.RenderImplicitWindowRange;
import org.jooq.impl.QOM.FrameExclude;
import org.jooq.impl.QOM.FrameUnits;
import org.jooq.impl.QOM.UnmodifiableList;

/**
 * @author Lukas Eder
 */
final class WindowSpecificationImpl
extends AbstractQueryPart
implements

    // Cascading interface implementations for window specification behaviour
    WindowSpecificationPartitionByStep,
    WindowSpecificationRowsAndStep,
    WindowSpecificationExcludeStep
{

    private static final Set<SQLDialect>  OMIT_PARTITION_BY_ONE                       = SQLDialect.supportedBy(CUBRID, MYSQL, SQLITE);

    private static final Set<SQLDialect>  REQUIRES_ORDER_BY_IN_LEAD_LAG               = SQLDialect.supportedBy(H2, MARIADB);
    private static final Set<SQLDialect>  REQUIRES_ORDER_BY_IN_NTILE                  = SQLDialect.supportedBy(H2);
    private static final Set<SQLDialect>  REQUIRES_ORDER_BY_IN_RANK_DENSE_RANK        = SQLDialect.supportedBy(H2, MARIADB);
    private static final Set<SQLDialect>  REQUIRES_ORDER_BY_IN_PERCENT_RANK_CUME_DIST = SQLDialect.supportedBy(MARIADB);








    private final WindowDefinitionImpl    windowDefinition;
    private final QueryPartList<Field<?>> partitionBy;
    private final SortFieldList           orderBy;
    private Integer                       frameStart;
    private Integer                       frameEnd;
    private FrameUnits                    frameUnits;
    private FrameExclude                  exclude;
    private boolean                       partitionByOne;

    WindowSpecificationImpl() {
        this(null);
    }

    WindowSpecificationImpl(WindowDefinitionImpl windowDefinition) {
        this.windowDefinition = windowDefinition;
        this.partitionBy = new QueryPartList<>();
        this.orderBy = new SortFieldList();
    }

    WindowSpecificationImpl copy() {
        WindowSpecificationImpl copy = new WindowSpecificationImpl(this.windowDefinition);
        copy.partitionBy.addAll(this.partitionBy);
        copy.orderBy.addAll(this.orderBy);
        copy.frameStart = this.frameStart;
        copy.frameEnd = this.frameEnd;
        copy.frameUnits = this.frameUnits;
        copy.exclude = this.exclude;
        copy.partitionByOne = this.partitionByOne;
        return copy;
    }

    @Override
    public final void accept(Context<?> ctx) {







        SortFieldList o = orderBy;

        // [#8414] [#8593] [#11021] [#11851] Some RDBMS require ORDER BY in some window functions
        AbstractWindowFunction<?> w = (AbstractWindowFunction<?>) ctx.data(DATA_WINDOW_FUNCTION);

        if (o.isEmpty()) {
            boolean ordered =
                    w instanceof Ntile && REQUIRES_ORDER_BY_IN_NTILE.contains(ctx.dialect())
                 || w instanceof Lead && REQUIRES_ORDER_BY_IN_LEAD_LAG.contains(ctx.dialect())
                 || w instanceof Lag && REQUIRES_ORDER_BY_IN_LEAD_LAG.contains(ctx.dialect())
                 || w instanceof Rank && REQUIRES_ORDER_BY_IN_RANK_DENSE_RANK.contains(ctx.dialect())
                 || w instanceof DenseRank && REQUIRES_ORDER_BY_IN_RANK_DENSE_RANK.contains(ctx.dialect())
                 || w instanceof PercentRank && REQUIRES_ORDER_BY_IN_PERCENT_RANK_CUME_DIST.contains(ctx.dialect())
                 || w instanceof CumeDist && REQUIRES_ORDER_BY_IN_PERCENT_RANK_CUME_DIST.contains(ctx.dialect())






            ;

            if (ordered) {
                Field<Integer> constant;

                switch (ctx.family()) {





                    default:
                        constant = field(select(one())); break;
                }

                o = new SortFieldList();
                o.add(constant.sortDefault());
            }
        }

        boolean hasWindowDefinitions = windowDefinition != null;
        boolean hasPartitionBy = !partitionBy.isEmpty();
        boolean hasOrderBy = !o.isEmpty();
        boolean hasFrame = frameStart != null





        ;

        int clauses = 0;

        if (hasWindowDefinitions)
            clauses++;
        if (hasPartitionBy)
            clauses++;
        if (hasOrderBy)
            clauses++;
        if (hasFrame)
            clauses++;

        boolean indent = clauses > 1;

        if (indent)
            ctx.formatIndentStart()
               .formatNewLine();

        if (windowDefinition != null)
            ctx.declareWindows(false, c -> c.visit(windowDefinition));

        if (hasPartitionBy) {

            // Ignore PARTITION BY 1 clause. These databases erroneously map the
            // 1 literal onto the column index (CUBRID, Sybase), or do not support
            // constant expressions in the PARTITION BY clause (HANA)
            if (partitionByOne && OMIT_PARTITION_BY_ONE.contains(ctx.dialect())) {
            }
            else {
                if (hasWindowDefinitions)
                    ctx.formatSeparator();

                ctx.visit(K_PARTITION_BY).separatorRequired(true)
                   .visit(partitionBy);
            }
        }

        if (hasOrderBy) {
            if (hasWindowDefinitions || hasPartitionBy)
                ctx.formatSeparator();

            ctx.visit(K_ORDER_BY).separatorRequired(true)
               .visit(o);
        }

        if (hasFrame) {
            if (hasWindowDefinitions || hasPartitionBy || hasOrderBy)
                ctx.formatSeparator();

            FrameUnits u = frameUnits;
            Integer s = frameStart;
            Integer e = frameEnd;

































            ctx.visit(u.keyword).sql(' ');

            if (e != null) {
                ctx.visit(K_BETWEEN).sql(' ');
                toSQLRows(ctx, s);

                ctx.sql(' ').visit(K_AND).sql(' ');
                toSQLRows(ctx, e);
            }
            else {
                toSQLRows(ctx, s);
            }

            if (exclude != null)
                ctx.sql(' ').visit(K_EXCLUDE).sql(' ').visit(exclude.keyword);
        }

        if (indent)
            ctx.formatIndentEnd()
               .formatNewLine();
    }






























































    private final void toSQLRows(Context<?> ctx, Integer rows) {
        if (rows == Integer.MIN_VALUE)
            ctx.visit(K_UNBOUNDED_PRECEDING);
        else if (rows == Integer.MAX_VALUE)
            ctx.visit(K_UNBOUNDED_FOLLOWING);
        else if (rows < 0)
            ctx.sql(-rows).sql(' ').visit(K_PRECEDING);
        else if (rows > 0)
            ctx.sql(rows).sql(' ').visit(K_FOLLOWING);
        else
            ctx.visit(K_CURRENT_ROW);
    }

    @Override
    public final WindowSpecificationPartitionByStep partitionBy(Field<?>... fields) {
        return partitionBy(Arrays.asList(fields));
    }

    @Override
    public final WindowSpecificationPartitionByStep partitionBy(Collection<? extends Field<?>> fields) {
        partitionBy.addAll(fields);
        return this;
    }

    @Override
    @Deprecated
    public final WindowSpecificationOrderByStep partitionByOne() {
        partitionByOne = true;
        partitionBy.add(one());
        return this;
    }

    @Override
    public final WindowSpecificationOrderByStep orderBy(OrderField<?>... fields) {
        return orderBy(Arrays.asList(fields));
    }

    @Override
    public final WindowSpecificationOrderByStep orderBy(Collection<? extends OrderField<?>> fields) {
        orderBy.addAll(Tools.sortFields(fields));
        return this;
    }

    @Override
    public final WindowSpecificationExcludeStep rowsUnboundedPreceding() {
        frameUnits = ROWS;
        frameStart = Integer.MIN_VALUE;
        return this;
    }

    @Override
    public final WindowSpecificationExcludeStep rowsPreceding(int number) {
        frameUnits = ROWS;
        frameStart = -number;
        return this;
    }

    @Override
    public final WindowSpecificationExcludeStep rowsCurrentRow() {
        frameUnits = ROWS;
        frameStart = 0;
        return this;
    }

    @Override
    public final WindowSpecificationExcludeStep rowsUnboundedFollowing() {
        frameUnits = ROWS;
        frameStart = Integer.MAX_VALUE;
        return this;
    }

    @Override
    public final WindowSpecificationExcludeStep rowsFollowing(int number) {
        frameUnits = ROWS;
        frameStart = number;
        return this;
    }

    @Override
    public final WindowSpecificationRowsAndStep rowsBetweenUnboundedPreceding() {
        rowsUnboundedPreceding();
        return this;
    }

    @Override
    public final WindowSpecificationRowsAndStep rowsBetweenPreceding(int number) {
        rowsPreceding(number);
        return this;
    }

    @Override
    public final WindowSpecificationRowsAndStep rowsBetweenCurrentRow() {
        rowsCurrentRow();
        return this;
    }

    @Override
    public final WindowSpecificationRowsAndStep rowsBetweenUnboundedFollowing() {
        rowsUnboundedFollowing();
        return this;
    }

    @Override
    public final WindowSpecificationRowsAndStep rowsBetweenFollowing(int number) {
        rowsFollowing(number);
        return this;
    }

    @Override
    public final WindowSpecificationExcludeStep rangeUnboundedPreceding() {
        frameUnits = RANGE;
        frameStart = Integer.MIN_VALUE;
        return this;
    }

    @Override
    public final WindowSpecificationExcludeStep rangePreceding(int number) {
        frameUnits = RANGE;
        frameStart = -number;
        return this;
    }

    @Override
    public final WindowSpecificationExcludeStep rangeCurrentRow() {
        frameUnits = RANGE;
        frameStart = 0;
        return this;
    }

    @Override
    public final WindowSpecificationExcludeStep rangeUnboundedFollowing() {
        frameUnits = RANGE;
        frameStart = Integer.MAX_VALUE;
        return this;
    }

    @Override
    public final WindowSpecificationExcludeStep rangeFollowing(int number) {
        frameUnits = RANGE;
        frameStart = number;
        return this;
    }

    @Override
    public final WindowSpecificationRowsAndStep rangeBetweenUnboundedPreceding() {
        rangeUnboundedPreceding();
        return this;
    }

    @Override
    public final WindowSpecificationRowsAndStep rangeBetweenPreceding(int number) {
        rangePreceding(number);
        return this;
    }

    @Override
    public final WindowSpecificationRowsAndStep rangeBetweenCurrentRow() {
        rangeCurrentRow();
        return this;
    }

    @Override
    public final WindowSpecificationRowsAndStep rangeBetweenUnboundedFollowing() {
        rangeUnboundedFollowing();
        return this;
    }

    @Override
    public final WindowSpecificationRowsAndStep rangeBetweenFollowing(int number) {
        rangeFollowing(number);
        return this;
    }

    @Override
    public final WindowSpecificationExcludeStep groupsUnboundedPreceding() {
        frameUnits = GROUPS;
        frameStart = Integer.MIN_VALUE;
        return this;
    }

    @Override
    public final WindowSpecificationExcludeStep groupsPreceding(int number) {
        frameUnits = GROUPS;
        frameStart = -number;
        return this;
    }

    @Override
    public final WindowSpecificationExcludeStep groupsCurrentRow() {
        frameUnits = GROUPS;
        frameStart = 0;
        return this;
    }

    @Override
    public final WindowSpecificationExcludeStep groupsUnboundedFollowing() {
        frameUnits = GROUPS;
        frameStart = Integer.MAX_VALUE;
        return this;
    }

    @Override
    public final WindowSpecificationExcludeStep groupsFollowing(int number) {
        frameUnits = GROUPS;
        frameStart = number;
        return this;
    }

    @Override
    public final WindowSpecificationRowsAndStep groupsBetweenUnboundedPreceding() {
        groupsUnboundedPreceding();
        return this;
    }

    @Override
    public final WindowSpecificationRowsAndStep groupsBetweenPreceding(int number) {
        groupsPreceding(number);
        return this;
    }

    @Override
    public final WindowSpecificationRowsAndStep groupsBetweenCurrentRow() {
        groupsCurrentRow();
        return this;
    }

    @Override
    public final WindowSpecificationRowsAndStep groupsBetweenUnboundedFollowing() {
        groupsUnboundedFollowing();
        return this;
    }

    @Override
    public final WindowSpecificationRowsAndStep groupsBetweenFollowing(int number) {
        groupsFollowing(number);
        return this;
    }

    @Override
    public final WindowSpecificationExcludeStep andUnboundedPreceding() {
        frameEnd = Integer.MIN_VALUE;
        return this;
    }

    @Override
    public final WindowSpecificationExcludeStep andPreceding(int number) {
        frameEnd = -number;
        return this;
    }

    @Override
    public final WindowSpecificationExcludeStep andCurrentRow() {
        frameEnd = 0;
        return this;
    }

    @Override
    public final WindowSpecificationExcludeStep andUnboundedFollowing() {
        frameEnd = Integer.MAX_VALUE;
        return this;
    }

    @Override
    public final WindowSpecificationExcludeStep andFollowing(int number) {
        frameEnd = number;
        return this;
    }

    @Override
    public final WindowSpecificationFinalStep excludeCurrentRow() {
        exclude = CURRENT_ROW;
        return this;
    }

    @Override
    public final WindowSpecificationFinalStep excludeGroup() {
        exclude = GROUP;
        return this;
    }

    @Override
    public final WindowSpecificationFinalStep excludeTies() {
        exclude = TIES;
        return this;
    }

    @Override
    public final WindowSpecificationFinalStep excludeNoOthers() {
        exclude = NO_OTHERS;
        return this;
    }

    // -------------------------------------------------------------------------
    // XXX: Query Object Model
    // -------------------------------------------------------------------------

    @Override
    public final WindowDefinitionImpl $windowDefinition() {
        return windowDefinition;
    }

    @Override
    public final UnmodifiableList<? extends Field<?>> $partitionBy() {
        return QOM.unmodifiable(partitionBy);
    }

    @Override
    public final UnmodifiableList<? extends SortField<?>> $orderBy() {
        return QOM.unmodifiable(orderBy);
    }

    @Override
    public final FrameUnits $frameUnits() {
        return frameUnits;
    }

    @Override
    public final Integer $frameStart() {
        return frameStart;
    }

    @Override
    public final Integer $frameEnd() {
        return frameEnd;
    }

    @Override
    public final FrameExclude $exclude() {
        return exclude;
    }



























}
