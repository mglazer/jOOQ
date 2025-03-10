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

import org.jooq.Converter;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record5;

import org.jetbrains.annotations.NotNull;

/**
 * A general purpose record, typically used for ad-hoc types.
 * <p>
 * This type implements both the general-purpose, type-unsafe {@link Record}
 * interface, as well as the more specific, type-safe {@link Record5}
 * interfaces
 *
 * @author Lukas Eder
 */
@SuppressWarnings({ "unchecked" })
final class RecordImpl5<T1, T2, T3, T4, T5> extends AbstractRecord implements InternalRecord, Record5<T1, T2, T3, T4, T5> {

    RecordImpl5(AbstractRow<?> row) {
        super(row);
    }

    // ------------------------------------------------------------------------
    // XXX: Type-safe Record APIs
    // ------------------------------------------------------------------------

    @Override
    public RowImpl5<T1, T2, T3, T4, T5> fieldsRow() {
        return new RowImpl5<>(field1(), field2(), field3(), field4(), field5());
    }

    @Override
    public final RowImpl5<T1, T2, T3, T4, T5> valuesRow() {
        return new RowImpl5<>(Tools.field(value1(), field1()), Tools.field(value2(), field2()), Tools.field(value3(), field3()), Tools.field(value4(), field4()), Tools.field(value5(), field5()));
    }

    @Override
    public final Field<T1> field1() {
        return (@NotNull Field<T1>) fields.field(0);
    }

    @Override
    public final Field<T2> field2() {
        return (@NotNull Field<T2>) fields.field(1);
    }

    @Override
    public final Field<T3> field3() {
        return (@NotNull Field<T3>) fields.field(2);
    }

    @Override
    public final Field<T4> field4() {
        return (@NotNull Field<T4>) fields.field(3);
    }

    @Override
    public final Field<T5> field5() {
        return (@NotNull Field<T5>) fields.field(4);
    }

    @Override
    public final T1 value1() {
        return (T1) get(0);
    }

    @Override
    public final T2 value2() {
        return (T2) get(1);
    }

    @Override
    public final T3 value3() {
        return (T3) get(2);
    }

    @Override
    public final T4 value4() {
        return (T4) get(3);
    }

    @Override
    public final T5 value5() {
        return (T5) get(4);
    }

    @Override
    public final Record5<T1, T2, T3, T4, T5> value1(T1 value) {
        set(0, value);
        return this;
    }

    @Override
    public final Record5<T1, T2, T3, T4, T5> value2(T2 value) {
        set(1, value);
        return this;
    }

    @Override
    public final Record5<T1, T2, T3, T4, T5> value3(T3 value) {
        set(2, value);
        return this;
    }

    @Override
    public final Record5<T1, T2, T3, T4, T5> value4(T4 value) {
        set(3, value);
        return this;
    }

    @Override
    public final Record5<T1, T2, T3, T4, T5> value5(T5 value) {
        set(4, value);
        return this;
    }

    @Override
    public final Record5<T1, T2, T3, T4, T5> values(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        set(0, t1);
        set(1, t2);
        set(2, t3);
        set(3, t4);
        set(4, t5);
        return this;
    }

    @Override
    public <T> Record5<T1, T2, T3, T4, T5> with(Field<T> field, T value) {
        return (Record5<T1, T2, T3, T4, T5>) super.with(field, value);
    }

    @Override
    public <T, U> Record5<T1, T2, T3, T4, T5> with(Field<T> field, U value, Converter<? extends T, ? super U> converter) {
        return (Record5<T1, T2, T3, T4, T5>) super.with(field, value, converter);
    }

    @Override
    public final T1 component1() {
        return value1();
    }

    @Override
    public final T2 component2() {
        return value2();
    }

    @Override
    public final T3 component3() {
        return value3();
    }

    @Override
    public final T4 component4() {
        return value4();
    }

    @Override
    public final T5 component5() {
        return value5();
    }
}
