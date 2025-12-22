package orestes.bloomfilter.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.CountingBloomFilter;
import orestes.bloomfilter.FilterBuilder;
import orestes.bloomfilter.HashProvider;
import orestes.bloomfilter.json.BloomFilterConverter;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class ConverterTest {
    @Test
    public void testCorrectJSON() throws Exception {
        BloomFilter<String> bf = new FilterBuilder().expectedElements(50).falsePositiveProbability(0.1).buildBloomFilter();
        bf.add("Ululu");
        JsonElement json = BloomFilterConverter.toJson(bf);
        BloomFilter<String> otherBf = BloomFilterConverter.fromJson(json);
        assertTrue(otherBf.contains("Ululu"));
    }

    @Test
    public void testCorrectJSONfromCountingFilter() throws Exception {
        BloomFilter<String> bf = new FilterBuilder().expectedElements(50).falsePositiveProbability(0.1).buildCountingBloomFilter();
        bf.add("Ululu");
        JsonElement json = BloomFilterConverter.toJson(bf);
        BloomFilter<String> otherBf = BloomFilterConverter.fromJson(json);
        assertTrue(otherBf.contains("Ululu"));
    }

    @Test
    public void testCountingBloomFilterSerialization() throws Exception {
        // Create a counting bloom filter and add elements
        CountingBloomFilter<String> cbf = new FilterBuilder()
            .expectedElements(50)
            .falsePositiveProbability(0.1)
            .buildCountingBloomFilter();

        cbf.add("Element1");
        cbf.add("Element2");
        cbf.add("Element1"); // Add Element1 again to test counting

        // Serialize to JSON
        JsonElement json = BloomFilterConverter.toJson(cbf);

        // Verify JSON structure contains counting information
        JsonObject jsonObj = json.getAsJsonObject();
        assertTrue("JSON should have 'm' property", jsonObj.has("m"));
        assertTrue("JSON should have 'h' property", jsonObj.has("h"));
        assertTrue("JSON should have 'c' property for counting bits", jsonObj.has("c"));
        assertTrue("JSON should have 'counts' property", jsonObj.has("counts"));

        // Deserialize from JSON
        BloomFilter<String> restored = BloomFilterConverter.fromJson(json);

        // Verify it's a counting bloom filter
        assertTrue("Restored filter should be a CountingBloomFilter", restored instanceof CountingBloomFilter);

        CountingBloomFilter<String> restoredCbf = (CountingBloomFilter<String>) restored;

        // Verify the elements are present
        assertTrue("Restored filter should contain Element1", restoredCbf.contains("Element1"));
        assertTrue("Restored filter should contain Element2", restoredCbf.contains("Element2"));
    }

    @Test
    public void testCountingBloomFilterCountPreservation() throws Exception {
        // Create a counting bloom filter
        CountingBloomFilter<String> cbf = new FilterBuilder()
            .expectedElements(100)
            .falsePositiveProbability(0.01)
            .countingBits(16)
            .buildCountingBloomFilter();

        // Add elements with different counts
        cbf.add("Once");
        cbf.add("Twice");
        cbf.add("Twice");
        cbf.add("Thrice");
        cbf.add("Thrice");
        cbf.add("Thrice");

        // Get count map before serialization
        Map<Integer, Long> countMapBefore = cbf.getCountMap();

        // Serialize and deserialize
        JsonElement json = BloomFilterConverter.toJson(cbf);
        BloomFilter<String> restored = BloomFilterConverter.fromJson(json);

        assertTrue("Restored filter should be a CountingBloomFilter", restored instanceof CountingBloomFilter);
        CountingBloomFilter<String> restoredCbf = (CountingBloomFilter<String>) restored;

        // Get count map after deserialization
        Map<Integer, Long> countMapAfter = restoredCbf.getCountMap();

        // Verify count maps match
        assertEquals("Count maps should have same size", countMapBefore.size(), countMapAfter.size());

        // Verify each count is preserved
        for (Map.Entry<Integer, Long> entry : countMapBefore.entrySet()) {
            assertTrue("Position " + entry.getKey() + " should exist in restored count map",
                      countMapAfter.containsKey(entry.getKey()));
            assertEquals("Count at position " + entry.getKey() + " should match",
                        entry.getValue(), countMapAfter.get(entry.getKey()));
        }

        // Verify elements are still present
        assertTrue("Restored filter should contain 'Once'", restoredCbf.contains("Once"));
        assertTrue("Restored filter should contain 'Twice'", restoredCbf.contains("Twice"));
        assertTrue("Restored filter should contain 'Thrice'", restoredCbf.contains("Thrice"));
    }

    @Test
    public void testCountingBloomFilterRemovalAfterDeserialization() throws Exception {
        // Create a counting bloom filter
        CountingBloomFilter<String> cbf = new FilterBuilder()
            .expectedElements(50)
            .falsePositiveProbability(0.1)
            .buildCountingBloomFilter();

        // Add elements multiple times
        cbf.add("Multi");
        cbf.add("Multi");
        cbf.add("Single");

        // Serialize and deserialize
        JsonElement json = BloomFilterConverter.toJson(cbf);
        BloomFilter<String> restored = BloomFilterConverter.fromJson(json);

        assertTrue("Restored filter should be a CountingBloomFilter", restored instanceof CountingBloomFilter);
        CountingBloomFilter<String> restoredCbf = (CountingBloomFilter<String>) restored;

        // Verify elements are present
        assertTrue("Restored filter should contain 'Multi'", restoredCbf.contains("Multi"));
        assertTrue("Restored filter should contain 'Single'", restoredCbf.contains("Single"));

        // Remove 'Multi' once - it should still be present
        restoredCbf.remove("Multi");
        assertTrue("'Multi' should still be present after one removal", restoredCbf.contains("Multi"));

        // Remove 'Multi' again - now it should be gone
        restoredCbf.remove("Multi");
        // Note: Due to false positives, we can't guarantee it's not present, but counts should be correct

        // Remove 'Single' once - it should be gone
        restoredCbf.remove("Single");
        // Note: Due to false positives, we can't guarantee it's not present, but counts should be correct
    }

    @Test
    public void testCountingBloomFilterMetadataPreservation() throws Exception {
        // Create a counting bloom filter with specific parameters
        int expectedSize = 1000;
        double fpp = 0.01;
        int countingBits = 32;

        CountingBloomFilter<String> cbf = new FilterBuilder()
            .expectedElements(expectedSize)
            .falsePositiveProbability(fpp)
            .countingBits(countingBits)
            .buildCountingBloomFilter();

        cbf.add("Test");

        // Serialize
        JsonElement json = BloomFilterConverter.toJson(cbf);
        JsonObject jsonObj = json.getAsJsonObject();

        // Verify metadata
        assertNotNull("Size (m) should be present", jsonObj.get("m"));
        assertNotNull("Hashes (h) should be present", jsonObj.get("h"));
        assertNotNull("Counting bits (c) should be present", jsonObj.get("c"));

        int m = jsonObj.get("m").getAsInt();
        int h = jsonObj.get("h").getAsInt();
        int c = jsonObj.get("c").getAsInt();

        assertEquals("Counting bits should be preserved", countingBits, c);

        // Deserialize
        BloomFilter<String> restored = BloomFilterConverter.fromJson(json);
        CountingBloomFilter<String> restoredCbf = (CountingBloomFilter<String>) restored;

        // Verify metadata matches
        assertEquals("Filter size should match", m, restoredCbf.getSize());
        assertEquals("Number of hashes should match", h, restoredCbf.getHashes());
        assertEquals("Counting bits should match", countingBits, restoredCbf.getCountingBits());
    }

    @Ignore
    @Test
    public void testMurmur3() throws Exception {
        byte[] test1 = "Erik".getBytes(FilterBuilder.defaultCharset());
        byte[] test2 = "Witt".getBytes(FilterBuilder.defaultCharset());

        System.out.println(HashProvider.murmur3(0, test1));
        System.out.println(HashProvider.murmur3(0, test2));
        System.out.println(HashProvider.murmur3(666, test1));
        System.out.println(HashProvider.murmur3(666, test2));
        System.out.println(Arrays.toString(HashProvider.hashCassandra(test1, 10000, 5)));


        BloomFilter<String> bf = new FilterBuilder().expectedElements(50).falsePositiveProbability(0.1).buildBloomFilter();
        for (int i = 0; i < 100000; i++) {
            bf.add(UUID.randomUUID().toString());
        }
        JsonElement json = BloomFilterConverter.toJson(bf);
        System.out.println(json.toString());
    }
}
