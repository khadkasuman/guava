/*
 * Copyright (C) 2009 The Guava Authors
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

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Tables.AbstractCell;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;

/** Collectors utilities for {@code common.collect.Table} internals. */
@GwtCompatible
@IgnoreJRERequirement // used only from APIs with Java 8 types in them
final class TableCollectors {

  static <T extends @Nullable Object, R, C, V>
      Collector<T, ?, ImmutableTable<R, C, V>> toImmutableTable(
          Function<? super T, ? extends R> rowFunction,
          Function<? super T, ? extends C> columnFunction,
          Function<? super T, ? extends V> valueFunction) {
    checkNotNull(rowFunction, "rowFunction");
    checkNotNull(columnFunction, "columnFunction");
    checkNotNull(valueFunction, "valueFunction");
    return Collector.of(
        (Supplier<ImmutableTable.Builder<R, C, V>>) ImmutableTable.Builder::new,
        (builder, t) ->
            builder.put(rowFunction.apply(t), columnFunction.apply(t), valueFunction.apply(t)),
        ImmutableTable.Builder::combine,
        ImmutableTable.Builder::buildOrThrow);
  }

  static <T extends @Nullable Object, R, C, V>
      Collector<T, ?, ImmutableTable<R, C, V>> toImmutableTable(
          Function<? super T, ? extends R> rowFunction,
          Function<? super T, ? extends C> columnFunction,
          Function<? super T, ? extends V> valueFunction,
          BinaryOperator<V> mergeFunction) {

    checkNotNull(rowFunction, "rowFunction");
    checkNotNull(columnFunction, "columnFunction");
    checkNotNull(valueFunction, "valueFunction");
    checkNotNull(mergeFunction, "mergeFunction");

    /*
     * No mutable Table exactly matches the insertion order behavior of ImmutableTable.Builder, but
     * the Builder can't efficiently support merging of duplicate values.  Getting around this
     * requires some work.
     */

    return Collector.of(
        ImmutableTableCollectorState<R, C, V>::new,
        (state, input) ->
            state.put(
                rowFunction.apply(input),
                columnFunction.apply(input),
                valueFunction.apply(input),
                mergeFunction),
        (s1, s2) -> s1.combine(s2, mergeFunction),
        state -> state.toTable());
  }

  static <
          T extends @Nullable Object,
          R extends @Nullable Object,
          C extends @Nullable Object,
          V,
          I extends Table<R, C, V>>
      Collector<T, ?, I> toTable(
          Function<? super T, ? extends R> rowFunction,
          Function<? super T, ? extends C> columnFunction,
          Function<? super T, ? extends V> valueFunction,
          Supplier<I> tableSupplier) {
    return TableCollectors.<T, R, C, V, I>toTable(
        rowFunction,
        columnFunction,
        valueFunction,
        (v1, v2) -> {
          throw new IllegalStateException("Conflicting values " + v1 + " and " + v2);
        },
        tableSupplier);
  }

  static <
          T extends @Nullable Object,
          R extends @Nullable Object,
          C extends @Nullable Object,
          V,
          I extends Table<R, C, V>>
      Collector<T, ?, I> toTable(
          Function<? super T, ? extends R> rowFunction,
          Function<? super T, ? extends C> columnFunction,
          Function<? super T, ? extends V> valueFunction,
          BinaryOperator<V> mergeFunction,
          Supplier<I> tableSupplier) {
    checkNotNull(rowFunction);
    checkNotNull(columnFunction);
    checkNotNull(valueFunction);
    checkNotNull(mergeFunction);
    checkNotNull(tableSupplier);
    return Collector.of(
        tableSupplier,
        (table, input) ->
            mergeTables(
                table,
                rowFunction.apply(input),
                columnFunction.apply(input),
                valueFunction.apply(input),
                mergeFunction),
        (table1, table2) -> {
          for (Table.Cell<R, C, V> cell2 : table2.cellSet()) {
            mergeTables(
                table1, cell2.getRowKey(), cell2.getColumnKey(), cell2.getValue(), mergeFunction);
          }
          return table1;
        });
  }

  private static final class ImmutableTableCollectorState<R, C, V> {
    final List<MutableCell<R, C, V>> insertionOrder = new ArrayList<>();
    final Table<R, C, MutableCell<R, C, V>> table = HashBasedTable.create();

    void put(R row, C column, V value, BinaryOperator<V> merger) {
      MutableCell<R, C, V> oldCell = table.get(row, column);
      if (oldCell == null) {
        MutableCell<R, C, V> cell = new MutableCell<>(row, column, value);
        insertionOrder.add(cell);
        table.put(row, column, cell);
      } else {
        oldCell.merge(value, merger);
      }
    }

    /*
     * While the current implementation returns `this`, that's not something we mean to guarantee.
     * Anyway, the purpose of this method is to implement a BinaryOperator combiner for a Collector,
     * so its return value will get used naturally.
     */
    @SuppressWarnings("CanIgnoreReturnValueSuggester")
    ImmutableTableCollectorState<R, C, V> combine(
        ImmutableTableCollectorState<R, C, V> other, BinaryOperator<V> merger) {
      for (MutableCell<R, C, V> cell : other.insertionOrder) {
        put(cell.getRowKey(), cell.getColumnKey(), cell.getValue(), merger);
      }
      return this;
    }

    ImmutableTable<R, C, V> toTable() {
      return ImmutableTable.copyOf(insertionOrder);
    }
  }

  @IgnoreJRERequirement // see enclosing class (whose annotation Animal Sniffer ignores here...)
  private static final class MutableCell<R, C, V> extends AbstractCell<R, C, V> {
    private final R row;
    private final C column;
    private V value;

    MutableCell(R row, C column, V value) {
      this.row = checkNotNull(row, "row");
      this.column = checkNotNull(column, "column");
      this.value = checkNotNull(value, "value");
    }

    @Override
    public R getRowKey() {
      return row;
    }

    @Override
    public C getColumnKey() {
      return column;
    }

    @Override
    public V getValue() {
      return value;
    }

    void merge(V value, BinaryOperator<V> mergeFunction) {
      checkNotNull(value, "value");
      this.value = checkNotNull(mergeFunction.apply(this.value, value), "mergeFunction.apply");
    }
  }

  private static <R extends @Nullable Object, C extends @Nullable Object, V> void mergeTables(
      Table<R, C, V> table,
      @ParametricNullness R row,
      @ParametricNullness C column,
      V value,
      BinaryOperator<V> mergeFunction) {
    checkNotNull(value);
    V oldValue = table.get(row, column);
    if (oldValue == null) {
      table.put(row, column, value);
    } else {
      V newValue = mergeFunction.apply(oldValue, value);
      if (newValue == null) {
        table.remove(row, column);
      } else {
        table.put(row, column, newValue);
      }
    }
  }

  private TableCollectors() {}
}
