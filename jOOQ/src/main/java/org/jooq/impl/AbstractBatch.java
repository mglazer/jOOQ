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

import static org.jooq.impl.Tools.blocking;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import org.jooq.Batch;
import org.jooq.Configuration;
import org.jooq.DSLContext;

import org.reactivestreams.Subscriber;

/**
 * @author Lukas Eder
 */
abstract class AbstractBatch implements Batch {

    final Configuration configuration;
    final DSLContext    dsl;

    AbstractBatch(Configuration configuration) {
        this.configuration = configuration;
        this.dsl = DSL.using(configuration);
    }

    @Override
    public void subscribe(Subscriber<? super Integer> s) {

        // [#11700] TODO: Implement this
        throw new UnsupportedOperationException();
    }

    @Override
    public final CompletionStage<int[]> executeAsync() {
        return executeAsync(configuration.executorProvider().provide());
    }

    @Override
    public final CompletionStage<int[]> executeAsync(Executor executor) {
        return ExecutorProviderCompletionStage.of(CompletableFuture.supplyAsync(blocking(this::execute), executor), () -> executor);
    }
}
