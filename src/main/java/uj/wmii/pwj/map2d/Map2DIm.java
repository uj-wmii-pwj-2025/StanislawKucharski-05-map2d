package uj.wmii.pwj.map2d;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.Collections;

public class Map2DIm<R, C, V> implements Map2D<R, C, V> {

    private final Map<R, Map<C, V>> mapRC;
    private final Map<C, Map<R, V>> mapCR;
    private long size;

    public Map2DIm() {
        mapRC = new HashMap<>();
        mapCR = new HashMap<>();
        size = 0;
    }

    private void checkNull(Object key, String name) {
        if (key == null) throw new NullPointerException(name + " cannot be null");
    }

    @Override
    public V put(R rowKey, C columnKey, V value) {
        checkNull(rowKey, "rowKey");
        checkNull(columnKey, "columnKey");

        Map<C, V> row = mapRC.computeIfAbsent(rowKey, k -> new HashMap<>());
        V oldValue = row.put(columnKey, value);

        Map<R, V> column = mapCR.computeIfAbsent(columnKey, k -> new HashMap<>());
        column.put(rowKey, value);

        if (oldValue == null) size++;

        return oldValue;
    }

    @Override
    public V get(R rowKey, C columnKey) {
        checkNull(rowKey, "rowKey");
        checkNull(columnKey, "columnKey");

        Map<C, V> row = mapRC.get(rowKey);
        return row != null ? row.get(columnKey) : null;
    }

    @Override
    public V getOrDefault(R rowKey, C columnKey, V defaultValue) {
        V val = get(rowKey, columnKey);
        return val != null ? val : defaultValue;
    }

    @Override
    public V remove(R rowKey, C columnKey) {
        checkNull(rowKey, "rowKey");
        checkNull(columnKey, "columnKey");

        Map<C, V> row = mapRC.get(rowKey);
        if (row == null) return null;

        V removed = row.remove(columnKey);
        if (removed != null) size--;

        if (row.isEmpty()) mapRC.remove(rowKey);

        Map<R, V> column = mapCR.get(columnKey);
        if (column != null) {
            column.remove(rowKey);
            if (column.isEmpty()) mapCR.remove(columnKey);
        }

        return removed;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean nonEmpty() {
        return size > 0;
    }

    @Override
    public int size() {
        return Math.toIntExact(size);
    }

    @Override
    public void clear() {
        mapRC.clear();
        mapCR.clear();
        size = 0;
    }

    @Override
    public Map<C, V> rowView(R rowKey) {
        checkNull(rowKey, "rowKey");
        Map<C, V> row = mapRC.get(rowKey);
        if (row == null) return Map.of();
        return Map.copyOf(row);
    }

    @Override
    public Map<R, V> columnView(C columnKey) {
        checkNull(columnKey, "columnKey");
        Map<R, V> column = mapCR.get(columnKey);
        if (column == null) return Map.of();
        return Map.copyOf(column);
    }

    @Override
    public boolean containsValue(V value) {
        for (Map<C, V> row : mapRC.values()) {
            if (row.containsValue(value)) return true;
        }
        return false;
    }

    @Override
    public boolean containsKey(R rowKey, C columnKey) {
        checkNull(rowKey, "rowKey");
        checkNull(columnKey, "columnKey");

        Map<C, V> row = mapRC.get(rowKey);
        return row != null && row.containsKey(columnKey);
    }

    @Override
    public boolean containsRow(R rowKey) {
        checkNull(rowKey, "rowKey");
        return mapRC.containsKey(rowKey) && !mapRC.get(rowKey).isEmpty();
    }

    @Override
    public boolean containsColumn(C columnKey) {
        checkNull(columnKey, "columnKey");
        return mapCR.containsKey(columnKey) && !mapCR.get(columnKey).isEmpty();
    }

    @Override
    public Map<R, Map<C, V>> rowMapView() {

        Map<R, Map<C, V>> copy = new HashMap<>();
        for (Map.Entry<R, Map<C, V>> e : mapRC.entrySet()) {
            copy.put(e.getKey(), Map.copyOf(e.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    @Override
    public Map<C, Map<R, V>> columnMapView() {

        Map<C, Map<R, V>> copy = new HashMap<>();
        for (Map.Entry<C, Map<R, V>> e : mapCR.entrySet()) {
            copy.put(e.getKey(), Map.copyOf(e.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    @Override
    public Map2D<R, C, V> fillMapFromRow(Map<? super C, ? super V> target, R rowKey) {
        checkNull(target, "target");
        checkNull(rowKey, "rowKey");

        Map<C, V> row = mapRC.get(rowKey);
        if (row != null) target.putAll(row);
        return this;
    }

    @Override
    public Map2D<R, C, V> fillMapFromColumn(Map<? super R, ? super V> target, C columnKey) {
        checkNull(target, "target");
        checkNull(columnKey, "columnKey");

        Map<R, V> column = mapCR.get(columnKey);
        if (column != null) target.putAll(column);
        return this;
    }

    @Override
    public Map2D<R, C, V> putAll(Map2D<? extends R, ? extends C, ? extends V> source) {
        checkNull(source, "source");

        source.rowMapView().forEach((rowKey, row) -> row.forEach(
                (colKey, value) -> put(rowKey, colKey, value)
        ));
        return this;
    }

    @Override
    public Map2D<R, C, V> putAllToRow(Map<? extends C, ? extends V> source, R rowKey) {
        checkNull(source, "source");
        checkNull(rowKey, "rowKey");

        source.forEach((colKey, value) -> put(rowKey, colKey, value));
        return this;
    }

    @Override
    public Map2D<R, C, V> putAllToColumn(Map<? extends R, ? extends V> source, C columnKey) {
        checkNull(source, "source");
        checkNull(columnKey, "columnKey");

        source.forEach((rowKey, value) -> put(rowKey, columnKey, value));
        return this;
    }

    @Override
    public <R2, C2, V2> Map2D<R2, C2, V2> copyWithConversion(
            Function<? super R, ? extends R2> rowFunction,
            Function<? super C, ? extends C2> columnFunction,
            Function<? super V, ? extends V2> valueFunction) {

        checkNull(rowFunction, "rowFunction");
        checkNull(columnFunction, "columnFunction");
        checkNull(valueFunction, "valueFunction");

        Map2D<R2, C2, V2> result = Map2D.createInstance();
        mapRC.forEach((r, row) -> row.forEach((c, v) ->
                result.put(
                        rowFunction.apply(r),
                        columnFunction.apply(c),
                        valueFunction.apply(v)
                )));
        return result;
    }
}
