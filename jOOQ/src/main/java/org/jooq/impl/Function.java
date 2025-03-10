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

import static org.jooq.impl.DSL.unquotedName;
import static org.jooq.impl.Tools.EMPTY_FIELD;

import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.QueryPart;
// ...
import org.jooq.impl.QOM.UnmodifiableList;

/**
 * @author Lukas Eder
 */
final class Function<T> extends AbstractFunction<T> {

    private final QueryPartList<Field<?>> arguments;

    Function(String name, DataType<T> type, Field<?>... arguments) {
        this(unquotedName(name), type, arguments);
    }

    Function(Name name, DataType<T> type, Field<?>... arguments) {
        this(name, type, true, arguments);
    }

    Function(Name name, DataType<T> type, boolean applySchemaMapping, Field<?>... arguments) {
        super(name, type, applySchemaMapping);

        this.arguments = new QueryPartList<>(arguments);
    }

    @Override
    final QueryPart arguments() {
        return arguments;
    }

    // -------------------------------------------------------------------------
    // XXX: Query Object Model
    // -------------------------------------------------------------------------

    @Override
    public final UnmodifiableList<? extends Field<?>> $args() {
        return QOM.unmodifiable(arguments);
    }















    // -------------------------------------------------------------------------
    // The Object API
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object that) {
        if (that instanceof Function)
            return getQualifiedName().equals(((Function<?>) that).getQualifiedName())
                && arguments.equals(((Function<?>) that).arguments);
        else
            return super.equals(that);
    }
}
