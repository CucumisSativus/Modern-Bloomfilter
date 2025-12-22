package orestes.bloomfilter.memory;

import java.util.Map;

/**
 * Helper class for deserializing counting bloom filters.
 * Provides access to package-protected methods needed for deserialization.
 */
public class CountingBloomFilterDeserializer {

    /**
     * Restores a counting bloom filter from a count map.
     * This method is used during deserialization to restore the filter state.
     *
     * @param filter The counting bloom filter to restore
     * @param countMap The map of positions to counts
     * @param <T> The type of elements in the filter
     */
    public static <T> void restoreFromCountMap(CountingBloomFilterMemory<T> filter, Map<Integer, Long> countMap) {
        for (Map.Entry<Integer, Long> entry : countMap.entrySet()) {
            int position = entry.getKey();
            long count = entry.getValue();
            filter.set(position, count);
            filter.getBloomFilter().setBit(position, count > 0);
        }
    }
}
