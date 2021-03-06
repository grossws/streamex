/*
 * Copyright 2015 Tagir Valeev
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javax.util.streamex;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link Stream} of {@link Map.Entry} objects which provides additional specific functionality
 * 
 * @author Tagir Valeev
 *
 * @param <K> the type of {@code Entry} keys
 * @param <V> the type of {@code Entry} values
 */
public class EntryStream<K, V> extends AbstractStreamEx<Entry<K, V>, EntryStream<K, V>> {
    private static class EntryImpl<K, V> implements Entry<K, V> {
        private final K key;
        private V value;

        EntryImpl(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        };

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof Entry))
                return false;
            if (!Objects.equals(key, ((Entry<?, ?>) obj).getKey()))
                return false;
            if (!Objects.equals(value, ((Entry<?, ?>) obj).getValue()))
                return false;
            return true;
        }
    }

    EntryStream(Stream<Entry<K, V>> stream) {
        super(stream);
    }

    @Override
    EntryStream<K, V> supply(Stream<Map.Entry<K, V>> stream) {
        return new EntryStream<>(stream);
    };

    <T> EntryStream(Stream<T> stream, Function<T, K> keyMapper, Function<T, V> valueMapper) {
        this(stream.map(e -> new EntryImpl<>(keyMapper.apply(e), valueMapper.apply(e))));
    }

    /**
     * Returns a stream consisting of the results of applying the given
     * function to the elements of this stream.
     *
     * <p>This is an intermediate operation.
     *
     * @param <R> The element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return the new stream
     */
    @Override
    public <R> StreamEx<R> map(Function<? super Entry<K, V>, ? extends R> mapper) {
        return new StreamEx<>(stream.map(mapper));
    }

    /**
     * Returns a stream consisting of the results of applying the given
     * function to the keys and values of this stream.
     *
     * <p>This is an intermediate operation.
     *
     * @param <R> The element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to key and value of each {@link Entry} in this stream
     * @return the new stream
     */
    public <R> StreamEx<R> mapKeyValue(BiFunction<? super K, ? super V, ? extends R> mapper) {
        return new StreamEx<>(stream.map(entry -> mapper.apply(entry.getKey(), entry.getValue())));
    }

    @Override
    public <R> StreamEx<R> flatMap(Function<? super Entry<K, V>, ? extends Stream<? extends R>> mapper) {
        return new StreamEx<>(stream.flatMap(mapper));
    }
    
    public <KK> EntryStream<KK, V> flatMapKeys(Function<? super K, ? extends Stream<? extends KK>> mapper) {
        return new EntryStream<>(stream.flatMap(e -> mapper.apply(e.getKey()).map(k -> new EntryImpl<KK, V>(k, e.getValue()))));
    }

    public <VV> EntryStream<K, VV> flatMapValues(Function<? super V, ? extends Stream<? extends VV>> mapper) {
        return new EntryStream<>(stream.flatMap(e -> mapper.apply(e.getValue()).map(v -> new EntryImpl<>(e.getKey(), v))));
    }
    
    public <R> StreamEx<R> flatCollection(Function<? super Entry<K, V>, ? extends Collection<? extends R>> mapper) {
        return flatMap(mapper.andThen(Collection::stream));
    }

    public EntryStream<K, V> append(K key, V value) {
        return new EntryStream<>(Stream.concat(stream, Stream.of(new EntryImpl<>(key, value))));
    }

    public EntryStream<K, V> prepend(K key, V value) {
        return new EntryStream<>(Stream.concat(Stream.of(new EntryImpl<>(key, value)), stream));
    }

    public <KK> EntryStream<KK, V> mapKeys(Function<K, KK> keyMapper) {
        return new EntryStream<>(stream.map(e -> new EntryImpl<>(keyMapper.apply(e.getKey()), e.getValue())));
    }

    public <VV> EntryStream<K, VV> mapValues(Function<V, VV> valueMapper) {
        return new EntryStream<>(stream.map(e -> new EntryImpl<>(e.getKey(), valueMapper.apply(e.getValue()))));
    }

    public <KK> EntryStream<KK, V> mapEntryKeys(Function<Entry<K, V>, KK> keyMapper) {
        return new EntryStream<>(stream.map(e -> new EntryImpl<>(keyMapper.apply(e), e.getValue())));
    }

    public <VV> EntryStream<K, VV> mapEntryValues(Function<Entry<K, V>, VV> valueMapper) {
        return new EntryStream<>(stream.map(e -> new EntryImpl<>(e.getKey(), valueMapper.apply(e))));
    }
    
    /**
     * Returns a stream consisting of the {@link Entry} objects which keys are the values
     * of this stream elements and vice versa 
     *
     * <p>
     * This is an intermediate operation.
     *
     * @return the new stream
     */
    public EntryStream<V, K> invert() {
        return new EntryStream<>(stream.map(e -> new EntryImpl<>(e.getValue(), e.getKey())));
    }

    /**
     * Returns a stream consisting of the elements of this stream which keys
     * match the given predicate.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param keyPredicate
     *            a non-interfering, stateless predicate to apply to the 
     *            key of each element to determine if it should be included
     * @return the new stream
     */
    public EntryStream<K, V> filterKeys(Predicate<K> keyPredicate) {
        return new EntryStream<>(stream.filter(e -> keyPredicate.test(e.getKey())));
    }

    /**
     * Returns a stream consisting of the elements of this stream which values
     * match the given predicate.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param valuePredicate
     *            a non-interfering, stateless predicate to apply to the 
     *            value of each element to determine if it should be included
     * @return the new stream
     */
    public EntryStream<K, V> filterValues(Predicate<V> valuePredicate) {
        return new EntryStream<>(stream.filter(e -> valuePredicate.test(e.getValue())));
    }

    /**
     * Returns a stream consisting of the elements of this stream which keys
     * don't match the given predicate.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param keyPredicate
     *            a non-interfering, stateless predicate to apply to the 
     *            key of each element to determine if it should be excluded
     * @return the new stream
     */
    public EntryStream<K, V> removeKeys(Predicate<K> keyPredicate) {
        return filterKeys(keyPredicate.negate());
    }

    /**
     * Returns a stream consisting of the elements of this stream which values
     * don't match the given predicate.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param valuePredicate
     *            a non-interfering, stateless predicate to apply to the 
     *            value of each element to determine if it should be excluded
     * @return the new stream
     */
    public EntryStream<K, V> removeValues(Predicate<V> valuePredicate) {
        return filterValues(valuePredicate.negate());
    }

    /**
     * Returns a stream consisting of the elements of this stream which key is not null.
     *
     * <p>This is an intermediate operation.
     *
     * @return the new stream
     */
    public EntryStream<K, V> nonNullKeys() {
        return new EntryStream<>(stream.filter(e -> e.getKey() != null));
    }

    /**
     * Returns a stream consisting of the elements of this stream which value is not null.
     *
     * <p>This is an intermediate operation.
     *
     * @return the new stream
     */
    public EntryStream<K, V> nonNullValues() {
        return new EntryStream<>(stream.filter(e -> e.getValue() != null));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <KK extends K> EntryStream<KK, V> selectKeys(Class<KK> clazz) {
        return new EntryStream<>((Stream) stream.filter(e -> clazz.isInstance(e.getKey())));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <VV extends V> EntryStream<K, VV> selectValues(Class<VV> clazz) {
        return new EntryStream<>((Stream) stream.filter(e -> clazz.isInstance(e.getValue())));
    }

    /**
     * Returns a stream consisting of the keys of this stream elements.
     *
     * <p>This is an intermediate operation.
     *
     * @return the new stream
     */
    public StreamEx<K> keys() {
        return new StreamEx<>(stream.map(Entry::getKey));
    }

    /**
     * Returns a stream consisting of the values of this stream elements.
     *
     * <p>This is an intermediate operation.
     *
     * @return the new stream
     */
    public StreamEx<V> values() {
        return new StreamEx<>(stream.map(Entry::getValue));
    }

    /**
     * Returns a {@link Map} containing the elements of this stream. There are
     * no guarantees on the type, mutability, serializability, or thread-safety
     * of the {@code Map} returned; if more control over the returned
     * {@code Map} is required, use {@link #toMap(Supplier)}.
     *
     * <p>This is a terminal operation.
     *
     * @return a {@code Map} containing the elements of this stream
     * @throws IllegalStateException if duplicate key was encountered in the stream
     * @see Collectors#toSet()
     */
    public Map<K, V> toMap() {
        return stream.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    public <M extends Map<K, V>> M toMap(Supplier<M> mapSupplier) {
        return stream.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        }, mapSupplier));
    }

    public <M extends Map<K, V>> M toMap(BinaryOperator<V> mergeFunction, Supplier<M> mapSupplier) {
        return stream.collect(Collectors.toMap(Entry::getKey, Entry::getValue, mergeFunction, mapSupplier));
    }
    
    public Map<K, List<V>> grouping() {
        return stream.collect(Collectors.groupingBy(Entry::getKey,
                Collectors.mapping(Entry::getValue, Collectors.toList())));
    }

    public <M extends Map<K, List<V>>> M grouping(Supplier<M> mapSupplier) {
        return stream.collect(Collectors.groupingBy(Entry::getKey, mapSupplier,
                Collectors.mapping(Entry::getValue, Collectors.toList())));
    }

    public <A, D> Map<K, D> grouping(Collector<? super V, A, D> downstream) {
        return stream.collect(Collectors.groupingBy(Entry::getKey,
                Collectors.<Entry<K, V>, V, A, D> mapping(Entry::getValue, downstream)));
    }

    public <A, D, M extends Map<K, D>> M grouping(Supplier<M> mapSupplier, Collector<? super V, A, D> downstream) {
        return stream.collect(Collectors.groupingBy(Entry::getKey, mapSupplier,
                Collectors.<Entry<K, V>, V, A, D> mapping(Entry::getValue, downstream)));
    }

    public <C extends Collection<V>> Map<K, C> groupingTo(Supplier<C> collectionFactory) {
        return stream.collect(Collectors.groupingBy(Entry::getKey,
                Collectors.mapping(Entry::getValue, Collectors.toCollection(collectionFactory))));
    }

    public <C extends Collection<V>, M extends Map<K, C>> M groupingTo(Supplier<M> mapSupplier,
            Supplier<C> collectionFactory) {
        return stream.collect(Collectors.groupingBy(Entry::getKey, mapSupplier,
                Collectors.mapping(Entry::getValue, Collectors.toCollection(collectionFactory))));
    }
    
    /**
     * Performs an action for each key-value pair of this stream.
     *
     * <p>This is a terminal operation.
     *
     * <p>The behavior of this operation is explicitly nondeterministic.
     * For parallel stream pipelines, this operation does <em>not</em>
     * guarantee to respect the encounter order of the stream, as doing so
     * would sacrifice the benefit of parallelism.  For any given element, the
     * action may be performed at whatever time and in whatever thread the
     * library chooses.  If the action accesses shared state, it is
     * responsible for providing the required synchronization.
     *
     * @param action a non-interfering action to perform on the key and value
     * @see #forEach(java.util.function.Consumer)
     */
    public void forKeyValue(BiConsumer<? super K, ? super V> action) {
        stream.forEach(entry -> action.accept(entry.getKey(), entry.getValue()));
    }

    public static <K, V> EntryStream<K, V> of(Stream<Entry<K, V>> stream) {
        return new EntryStream<>(stream);
    }

    public static <K, V> EntryStream<K, V> of(Map<K, V> map) {
        return new EntryStream<>(map.entrySet().stream());
    }
    
    public static <K, V> EntryStream<K, V> of(K key, V value) {
        return new EntryStream<>(Stream.of(new EntryImpl<>(key, value)));
    }
}
