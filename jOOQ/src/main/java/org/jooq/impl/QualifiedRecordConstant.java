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

import static org.jooq.conf.ParamType.INLINED;
import static org.jooq.impl.DSL.val;
import static org.jooq.impl.Keywords.K_ROW;
import static org.jooq.impl.Tools.getMappedUDTName;

import org.jooq.BindContext;
import org.jooq.Context;
import org.jooq.Field;
import org.jooq.QualifiedRecord;
import org.jooq.RenderContext;
import org.jooq.conf.ParamType;
import org.jooq.exception.SQLDialectNotSupportedException;
import org.jooq.impl.QOM.UNotYetImplemented;

/**
 * @author Lukas Eder
 */
final class QualifiedRecordConstant<R extends QualifiedRecord<R>> extends AbstractParam<R> implements UNotYetImplemented {

    QualifiedRecordConstant(R value) {
        super(value, value.getQualifier().getDataType());
    }

    @Override
    public void accept(Context<?> ctx) {
        if (ctx instanceof RenderContext)
            toSQL0((RenderContext) ctx);
        else
            bind0((BindContext) ctx);
    }

    final void toSQL0(RenderContext ctx) {
        ParamType paramType = ctx.paramType();
        if (isInline())
            ctx.paramType(INLINED);

        switch (ctx.family()) {



































            // Due to lack of UDT support in the Postgres JDBC drivers, all UDT's
            // have to be inlined
            case POSTGRES:
            case YUGABYTEDB: {
                toSQLInline(ctx);
                break;
            }

            // Assume default behaviour if dialect is not available
            default:
                toSQLInline(ctx);
                break;
        }

        if (isInline())
            ctx.paramType(paramType);
    }

    private final void toSQLInline(RenderContext ctx) {
        switch (ctx.family()) {


            case POSTGRES:
            case YUGABYTEDB:
                ctx.visit(K_ROW);
                break;

            default: {
                ctx.visit(value.getQualifier());
                break;
            }
        }

        ctx.sql('(');

        String separator = "";
        for (Field<?> field : value.fields()) {
            ctx.sql(separator);
            ctx.visit(val(value.get(field), field));
            separator = ", ";
        }

        ctx.sql(')');

        // [#13174] Need to cast inline UDT ROW expressions to the UDT type
        switch (ctx.family()) {


            case POSTGRES:
            case YUGABYTEDB:
                ctx.sql("::").visit(value.getQualifier());
                break;
        }
    }

    @Deprecated
    private final String getInlineConstructor(RenderContext ctx) {
        switch (ctx.family()) {


            case POSTGRES:
            case YUGABYTEDB:
                return "ROW";

            default:
                return getMappedUDTName(ctx, value);
        }
    }

    final void bind0(BindContext ctx) {
        switch (ctx.family()) {














            // Postgres cannot bind a complete structured type. The type is
            // inlined instead: ROW(.., .., ..)
            case POSTGRES:
            case YUGABYTEDB:  {
                for (Field<?> field : value.fields())
                    ctx.visit(val(value.get(field)));

                break;
            }

            default:
                throw new SQLDialectNotSupportedException("UDTs not supported in dialect " + ctx.dialect());
        }
    }
}
