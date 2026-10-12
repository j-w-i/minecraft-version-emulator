package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guards the ore drop rule: every declared profile drops the ore block, and nothing else does.
 *
 * <p>Raw metals arrive in Release 1.17, which is later than every declared profile, so all ten drop the block -
 * and that dating is already in this project's own availability data, which marks raw_iron, raw_gold and
 * raw_copper as 1.17 additions. Nostalgic Tweaks implements the replacement as {@code SELF_BLOCK_DROPS}, a listing
 * of six ore blocks, against a server flag that defaults to off; here the version decides.
 *
 * <p>Written because the owner asked for the fix and then had to ask whether it existed: "did you implement the
 * fact that iron gold etc drop as an ore rather than as raw iron". A test makes the answer checkable rather than
 * dependent on my memory of what I ported.
 */
class OreSelfDropTest {
    private static final List<String> DECLARED = List.of(
        "historical_experience:infdev_420",
        "historical_experience:infdev_611",
        "historical_experience:alpha",
        "historical_experience:beta_1_7_3",
        "historical_experience:beta_1_8_1",
        "historical_experience:beta_1_9_pre_3",
        "historical_experience:release_1_0_0",
        "historical_experience:release_1_1",
        "historical_experience:release_1_2_5",
        "historical_experience:release_1_6_4");

    @Test
    void everyDeclaredProfileDropsTheOreBlock() {
        List<String> without = DECLARED.stream()
            .filter(version -> !answersTrueFor(version))
            .toList();

        assertTrue(without.isEmpty(),
            "these declared profiles would drop raw metal instead of the ore block: " + without);
    }

    @Test
    void ordinaryVersionsAreLeftAlone() {
        assertFalse(answersTrueFor(null), "a world with no declared version must keep vanilla drops");
        assertFalse(answersTrueFor("minecraft:overworld"), "an undeclared version must keep vanilla drops");
    }

    /**
     * The predicate reads the active session, so the session is set for the duration of the question and restored
     * afterwards. That is the same state a joining player would produce.
     */
    private static boolean answersTrueFor(String versionId) {
        String previous = HistoricalActiveSession.current();
        try {
            HistoricalActiveSession.set(versionId);
            return HistoricalGameplayTweaks.oreDropsAsBlocks();
        } finally {
            HistoricalActiveSession.set(previous);
        }
    }
}
