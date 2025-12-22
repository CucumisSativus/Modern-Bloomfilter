package orestes.bloomfilter.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.CountingBloomFilter;
import orestes.bloomfilter.FilterBuilder;
import orestes.bloomfilter.HashProvider.HashMethod;
import orestes.bloomfilter.memory.BloomFilterMemory;
import orestes.bloomfilter.memory.CountingBloomFilterDeserializer;
import orestes.bloomfilter.memory.CountingBloomFilterMemory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class BloomFilterConverter {

    /**
     * Converts a normal or Counting Bloom filter to a JSON representation.
     * For counting bloom filters, the count map is serialized.
     * For normal bloom filters, the bit set is serialized.
     *
     * @param source the Bloom filter to convert
     * @return the JSON representation of the Bloom filter
     */
    public static JsonElement toJson(BloomFilter<?> source) {
        JsonObject root = new JsonObject();
        root.addProperty("m", source.getSize());
        root.addProperty("h", source.getHashes());
        //root.addProperty("HashMethod", source.config().hashMethod().name());

        // Check if this is a counting bloom filter
        if (source instanceof CountingBloomFilter) {
            CountingBloomFilter<?> cbf = (CountingBloomFilter<?>) source;
            root.addProperty("c", cbf.getCountingBits());

            // Serialize the count map
            JsonObject countsJson = new JsonObject();
            Map<Integer, Long> countMap = cbf.getCountMap();
            for (Map.Entry<Integer, Long> entry : countMap.entrySet()) {
                countsJson.addProperty(entry.getKey().toString(), entry.getValue());
            }
            root.add("counts", countsJson);
        } else {
            // For normal bloom filters, serialize the bit set
            byte[] bits = source.getBitSet().toByteArray();
            // Encode using base64 -> AAAAAQAAQAAAAAAgA
            root.addProperty("b", toBase64(bits));
        }

        return root;
    }

    /**
     * Converts a normal or Counting Bloom filter to a Base64 encoded string containing its bits.
     *
     * @param source the Bloom filter to convert
     * @return the Base64 representation of the Bloom filter
     */
    public static String toBase64(BloomFilter<?> source) {
        return toBase64(source.getBitSet().toByteArray());
    }

    private static String toBase64(byte[] bits) {
        return new String(Base64.getEncoder().encode(bits), StandardCharsets.UTF_8);
    }

    /**
     * Constructs a Bloom filter from its JSON representation.
     *
     * @param source the the JSON source
     * @return the constructed Bloom filter
     */
    public static BloomFilter<String> fromJson(JsonElement source) {
        return fromJson(source, String.class);
    }

    /**
     * Constructs a Bloom filter from its JSON representation.
     * Automatically detects whether it's a counting or normal bloom filter.
     *
     * @param source the JSON source
     * @param type   The class of the generic type
     * @param <T>    Generic type parameter of the Bloom filter
     * @return the Bloom filter
     */
    public static <T> BloomFilter<T> fromJson(JsonElement source, Class<T> type) {
        JsonObject root = source.getAsJsonObject();
        int m = root.get("m").getAsInt();
        int k = root.get("h").getAsInt();
        //String hashMethod = root.get("HashMethod").getAsString();

        FilterBuilder builder = new FilterBuilder(m, k).hashFunction(HashMethod.Murmur3KirschMitzenmacher);

        // Check if this is a counting bloom filter JSON
        if (root.has("c") && root.has("counts")) {
            // Deserialize counting bloom filter
            int countingBits = root.get("c").getAsInt();
            builder.countingBits(countingBits);

            CountingBloomFilterMemory<T> filter = new CountingBloomFilterMemory<>(builder.complete());

            // Restore the count map
            JsonObject countsJson = root.getAsJsonObject("counts");
            Map<Integer, Long> countMap = new HashMap<>();
            for (String key : countsJson.keySet()) {
                int position = Integer.parseInt(key);
                long count = countsJson.get(key).getAsLong();
                countMap.put(position, count);
            }

            CountingBloomFilterDeserializer.restoreFromCountMap(filter, countMap);

            return filter;
        } else {
            // Deserialize normal bloom filter
            byte[] bits = Base64.getDecoder().decode(root.get("b").getAsString());

            BloomFilterMemory<T> filter = new BloomFilterMemory<>(builder.complete());
            filter.setBitSet(BitSet.valueOf(bits));

            return filter;
        }
    }


}
