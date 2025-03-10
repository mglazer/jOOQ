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

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.jooq.RecordListener.onStoreEnd;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.row;
import static org.jooq.impl.Tools.EMPTY_RECORD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.jooq.Condition;
import org.jooq.Configuration;
import org.jooq.DAO;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.RecordContext;
import org.jooq.RecordListenerProvider;
import org.jooq.RecordMapper;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.UpdatableRecord;
import org.jooq.conf.Settings;

/**
 * A common base implementation for generated {@link DAO}.
 * <p>
 * Unlike many other elements in the jOOQ API, <code>DAO</code> may be used in
 * the context of Spring, CDI, or EJB lifecycle management. This means that no
 * methods in the <code>DAO</code> type hierarchy must be made final. See also
 * <a href="https://github.com/jOOQ/jOOQ/issues/4696">https://github.com/jOOQ/
 * jOOQ/issues/4696</a> for more details.
 *
 * @author Lukas Eder
 */
public abstract class DAOImpl<R extends UpdatableRecord<R>, P, T> implements DAO<R, P, T> {

    private final Table<R>     table;
    private final Class<P>     type;
    private RecordMapper<R, P> mapper;
    private Configuration      configuration;

    // -------------------------------------------------------------------------
    // XXX: Constructors and initialisation
    // -------------------------------------------------------------------------

    protected DAOImpl(Table<R> table, Class<P> type) {
        this(table, type, null);
    }

    protected DAOImpl(Table<R> table, Class<P> type, Configuration configuration) {
        this.table = table;
        this.type = type;

        setConfiguration(configuration);
    }

    /**
     * Inject a configuration.
     * <p>
     * This method is maintained to be able to configure a <code>DAO</code>
     * using Spring. It is not exposed in the public API.
     */
    public /* non-final */ void setConfiguration(Configuration configuration) {
        this.mapper = ((FieldsImpl<R>) table.recordType()).mapper(this.configuration = Tools.configuration(configuration), type);
    }

    public /* non-final */ DSLContext ctx() {
        return configuration().dsl();
    }

    @Override
    public /* non-final */ Configuration configuration() {
        return configuration;
    }

    @Override
    public /* non-final */ Settings settings() {
        return Tools.settings(configuration());
    }

    @Override
    public /* non-final */ SQLDialect dialect() {
        return Tools.configuration(configuration()).dialect();
    }

    @Override
    public /* non-final */ SQLDialect family() {
        return dialect().family();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Subclasses may override this method to provide custom implementations.
     */
    @Override
    public /* non-final */ RecordMapper<R, P> mapper() {
        return mapper;
    }

    // -------------------------------------------------------------------------
    // XXX: DAO API
    // -------------------------------------------------------------------------

    @Override
    public /* non-final */ void insert(P object) {
        insert(singletonList(object));
    }

    @SuppressWarnings("unchecked")
    @Override
    public /* non-final */ void insert(P... objects) {
        insert(asList(objects));
    }

    @Override
    public /* non-final */ void insert(Collection<P> objects) {

        // Execute a batch INSERT
        if (objects.size() > 1)

            // [#2536] [#3327] We cannot batch INSERT RETURNING calls yet
            if (!FALSE.equals(settings().isReturnRecordToPojo()))
                for (R record : records(objects, false))
                    record.insert();
            else
                ctx().batchInsert(records(objects, false)).execute();

        // Execute a regular INSERT
        else if (objects.size() == 1)
            records(objects, false).get(0).insert();
    }

    @Override
    public /* non-final */ void update(P object) {
        update(singletonList(object));
    }

    @SuppressWarnings("unchecked")
    @Override
    public /* non-final */ void update(P... objects) {
        update(asList(objects));
    }

    @Override
    public /* non-final */ void update(Collection<P> objects) {

        // Execute a batch UPDATE
        if (objects.size() > 1)

            // [#2536] [#3327] We cannot batch UPDATE RETURNING calls yet
            if (!FALSE.equals(settings().isReturnRecordToPojo()) &&
                 TRUE.equals(settings().isReturnAllOnUpdatableRecord()))
                for (R record : records(objects, true))
                    record.update();
            else
                ctx().batchUpdate(records(objects, true)).execute();

        // Execute a regular UPDATE
        else if (objects.size() == 1)
            records(objects, true).get(0).update();
    }

    @Override
    public /* non-final */ void merge(P object) {
        merge(singletonList(object));
    }

    @SuppressWarnings("unchecked")
    @Override
    public /* non-final */ void merge(P... objects) {
        merge(asList(objects));
    }

    @Override
    public /* non-final */ void merge(Collection<P> objects) {

        // Execute a batch MERGE
        if (objects.size() > 1)

            // [#2536] [#3327] We cannot batch MERGE RETURNING calls yet
            if (!FALSE.equals(settings().isReturnRecordToPojo()) &&
                 TRUE.equals(settings().isReturnAllOnUpdatableRecord()))
                for (R record : records(objects, false))
                    record.merge();
            else
                ctx().batchMerge(records(objects, false)).execute();

        // Execute a regular MERGE
        else if (objects.size() == 1)
            records(objects, false).get(0).merge();
    }

    @Override
    public /* non-final */ void delete(P object) {
        delete(singletonList(object));
    }

    @SuppressWarnings("unchecked")
    @Override
    public /* non-final */ void delete(P... objects) {
        delete(asList(objects));
    }

    @Override
    public /* non-final */ void delete(Collection<P> objects) {

        // Execute a batch DELETE
        if (objects.size() > 1)

            // [#2536] [#3327] We cannot batch DELETE RETURNING calls yet
            if (!FALSE.equals(settings().isReturnRecordToPojo()) &&
                 TRUE.equals(settings().isReturnAllOnUpdatableRecord()))
                for (R record : records(objects, true))
                    record.delete();
            else
                ctx().batchDelete(records(objects, true)).execute();

        // Execute a regular DELETE
        else if (objects.size() == 1)
            records(objects, true).get(0).delete();
    }

    @Override
    public /* non-final */ void deleteById(T ids) {
        deleteById(singletonList(ids));
    }

    @SuppressWarnings("unchecked")
    @Override
    public /* non-final */ void deleteById(T... ids) {
        deleteById(asList(ids));
    }

    @Override
    public /* non-final */ void deleteById(Collection<T> ids) {
        Field<?>[] pk = pk();

        if (pk != null)
            ctx().delete(table).where(equal(pk, ids)).execute();
    }

    @Override
    public /* non-final */ boolean exists(P object) {
        return existsById(getId(object));
    }

    @Override
    public /* non-final */ boolean existsById(T id) {
        Field<?>[] pk = pk();

        if (pk != null)
            return ctx()
                     .selectCount()
                     .from(table)
                     .where(equal(pk, id))
                     .fetchOne(0, Integer.class) > 0;
        else
            return false;
    }

    @Override
    public /* non-final */ long count() {
        return ctx()
                 .selectCount()
                 .from(table)
                 .fetchOne(0, Long.class);
    }

    @Override
    public /* non-final */ List<P> findAll() {
        return ctx()
                 .selectFrom(table)
                 .fetch(mapper());
    }

    @Override
    public /* non-final */ P findById(T id) {
        Field<?>[] pk = pk();

        if (pk != null)
            return ctx().selectFrom(table)
                        .where(equal(pk, id))
                        .fetchOne(mapper());

        return null;
    }

    @Override
    public /* non-final */ Optional<P> findOptionalById(T id) {
        return Optional.ofNullable(findById(id));
    }

    @Override
    public /* non-final */ <Z> List<P> fetchRange(Field<Z> field, Z lowerInclusive, Z upperInclusive) {
        return ctx()
                .selectFrom(table)
                .where(
                    lowerInclusive == null
                  ? upperInclusive == null
                    ? noCondition()
                    : field.le(upperInclusive)
                  : upperInclusive == null
                    ? field.ge(lowerInclusive)
                    : field.between(lowerInclusive, upperInclusive))
                .fetch(mapper());
    }

    @SuppressWarnings("unchecked")
    @Override
    public /* non-final */ <Z> List<P> fetch(Field<Z> field, Z... values) {
        return fetch(field, Arrays.asList(values));
    }

    @Override
    public /* non-final */ <Z> List<P> fetch(Field<Z> field, Collection<? extends Z> values) {
        return ctx()
            .selectFrom(table)
            .where(field.in(values))
            .fetch(mapper());
    }

    @Override
    public /* non-final */ <Z> P fetchOne(Field<Z> field, Z value) {
        return ctx()
            .selectFrom(table)
            .where(field.equal(value))
            .fetchOne(mapper());
    }

    @Override
    public /* non-final */ <Z> Optional<P> fetchOptional(Field<Z> field, Z value) {
        return Optional.ofNullable(fetchOne(field, value));
    }

    @Override
    public /* non-final */ Table<R> getTable() {
        return table;
    }

    @Override
    public /* non-final */ Class<P> getType() {
        return type;
    }

    // ------------------------------------------------------------------------
    // XXX: Template methods for generated subclasses
    // ------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    protected /* non-final */ T compositeKeyRecord(Object... values) {
        UniqueKey<R> key = table.getPrimaryKey();
        if (key == null)
            return null;

        TableField<R, Object>[] fields = (TableField<R, Object>[]) key.getFieldsArray();
        Record result = configuration().dsl().newRecord(fields);

        for (int i = 0; i < values.length; i++)
            result.set(fields[i], fields[i].getDataType().convert(values[i]));

        return (T) result;
    }

    // ------------------------------------------------------------------------
    // XXX: Private utility methods
    // ------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private /* non-final */ Condition equal(Field<?>[] pk, T id) {
        if (pk.length == 1)
            return ((Field<Object>) pk[0]).equal(pk[0].getDataType().convert(id));

        // [#2573] Composite key T types are of type Record[N]
        else
            return row(pk).equal((Record) id);
    }

    @SuppressWarnings("unchecked")
    private /* non-final */ Condition equal(Field<?>[] pk, Collection<T> ids) {
        if (pk.length == 1)
            if (ids.size() == 1)
                return equal(pk, ids.iterator().next());
            else
                return ((Field<Object>) pk[0]).in(pk[0].getDataType().convert(ids));

        // [#2573] Composite key T types are of type Record[N]
        else
            return row(pk).in(ids.toArray(EMPTY_RECORD));
    }

    private /* non-final */ Field<?>[] pk() {
        UniqueKey<?> key = table.getPrimaryKey();
        return key == null ? null : key.getFieldsArray();
    }

    private /* non-final */ List<R> records(Collection<P> objects, boolean forUpdate) {
        List<R> result = new ArrayList<>(objects.size());
        Field<?>[] pk = pk();
        DSLContext ctx;

        // [#7731] Create a Record -> POJO mapping to allow for reusing the below
        //         derived Configuration for improved reflection caching.
        IdentityHashMap<R, Object> mapping = !FALSE.equals(settings().isReturnRecordToPojo()) ? new IdentityHashMap<>() : null;

        // [#2536] Upon store(), insert(), update(), delete(), returned values in the record
        //         are copied back to the relevant POJO using the RecordListener SPI
        if (mapping != null) {
            Consumer<? super RecordContext> end = c -> {
                Record record = c.record();

                // TODO: [#2536] Use mapper()
                if (record != null)
                    record.into(mapping.get(record));
            };

            ctx = configuration().deriveAppending(
                onStoreEnd(end)
                .onInsertEnd(end)
                .onUpdateEnd(end)
                .onDeleteEnd(end)
            ).dsl();
        }
        else
            ctx = ctx();

        for (P object : objects) {
            R record = ctx.newRecord(table, object);

            if (mapping != null)
                mapping.put(record, object);

            if (forUpdate && pk != null)
                for (Field<?> field : pk)
                    record.changed(field, false);

            Tools.resetChangedOnNotNull(record);
            result.add(record);
        }

        return result;
    }

    private /* non-final */ RecordListenerProvider[] providers(final RecordListenerProvider[] providers, final IdentityHashMap<R, Object> mapping) {
        RecordListenerProvider[] result = Arrays.copyOf(providers, providers.length + 1);

        Consumer<? super RecordContext> end = ctx -> {
            Record record = ctx.record();

            // TODO: [#2536] Use mapper()
            if (record != null)
                record.into(mapping.get(record));
        };

        result[providers.length] = new DefaultRecordListenerProvider(
            onStoreEnd(end)
            .onInsertEnd(end)
            .onUpdateEnd(end)
            .onDeleteEnd(end)
        );

        return result;
    }
}
