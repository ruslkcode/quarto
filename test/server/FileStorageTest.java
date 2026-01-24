package server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FileStorage.
 * Verifies basic rating logic and protocol formatting.
 */
public class FileStorageTest {

    private FileStorage storage;

    @BeforeEach
    void setUp() {
        // Create fresh FileStorage instance before each test
        storage = new FileStorage();
    }

    @Test
    void testDefaultMmrForUnknownUser() {
        // Unknown users should have default MMR value
        assertEquals(1000, storage.getMmr("unknownPlayer"));
    }

    @Test
    void testUpdateMmrIncreasesValue() {
        // Updating MMR with positive value should increase rating
        storage.updateMmr("player1", 50);
        assertEquals(1050, storage.getMmr("player1"));
    }

    @Test
    void testMmrDoesNotGoBelowZero() {
        // MMR should not become negative
        storage.updateMmr("player2", -2000);
        assertEquals(0, storage.getMmr("player2"));
    }

    @Test
    void testGetRankingsForProtocolNotNull() {
        // Rankings string should be created correctly
        storage.updateMmr("Alice", 200);
        storage.updateMmr("Bob", 100);

        String rankings = storage.getRankingsForProtocol();
        assertNotNull(rankings);
        assertTrue(rankings.contains("Alice"));
        assertTrue(rankings.contains("Bob"));
    }

    @Test
    void testLoadDoesNotThrow() {
        // load() should not crash even if file exists or is empty
        assertDoesNotThrow(() -> storage.load());
    }

}

