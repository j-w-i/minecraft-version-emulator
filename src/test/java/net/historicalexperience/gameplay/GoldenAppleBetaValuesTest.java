package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Guards the pre-Release-1.1 golden apple, which was implemented wrong the first time.
 *
 * <p>The original implementation gave 10 hunger and no Regeneration, because only the hunger and saturation were
 * read out of the evidence; the effect clause was in the same string. The evidence is an official client artifact -
 * the Beta 1.8 client jar, class {@code sv} item registry:
 *
 * <pre>(66,10,1.2F,false).o().a(Regeneration,30,0,1.0F).a("appleGold")</pre>
 *
 * <p>These tests exist because that mistake was invisible: the food worked, the hunger was right, and only a
 * player would have noticed the missing effect. A test that asserts the numbers cannot be talked around.
 */
class GoldenAppleBetaValuesTest {
    @Test
    void theBetaValuesMatchTheClientArtifact() {
        assertEquals(10, HistoricalFoodHelper.goldenAppleBetaHunger(),
            "hunger 10, not the modern 4");
        assertEquals(600, HistoricalFoodHelper.goldenAppleBetaRegenerationTicks(),
            "30 seconds, which is 600 ticks - the artifact passes 30 to an effect helper that takes seconds");
        assertEquals(0, HistoricalFoodHelper.goldenAppleBetaRegenerationAmplifier(),
            "amplifier 0, i.e. Regeneration I. The artifact passes 0, and reading it as II would double the "
                + "healing rate");
    }

    @Test
    void theEffectIsAppliedToTheProfilesThatPredateReleaseOnePointOne() {
        String[] beforeTheChange = {
            "historical_experience:beta_1_8_1",
            "historical_experience:beta_1_9_pre_3",
            "historical_experience:release_1_0_0"
        };
        String[] afterTheChange = {
            "historical_experience:release_1_1",
            "historical_experience:release_1_2_5",
            "historical_experience:release_1_6_4"
        };

        for (String version : beforeTheChange) {
            assertTrue(HistoricalGameplayTweaks.betaGoldenApple(version),
                version + " predates Release 1.1, so it carries the 10-hunger regenerating form");
        }

        for (String version : afterTheChange) {
            assertFalse(HistoricalGameplayTweaks.betaGoldenApple(version),
                version + " is Release 1.1 or later, where the golden apple is the 4-hunger form");
        }
    }

    @Test
    void preHungerErasAreNotGivenTheGoldenAppleForm() {
        // Before Beta 1.8 there is no hunger bar at all, so the golden apple's hunger value is meaningless there
        // and the item heals directly like every other food. Release 1.6.4 is the proxy for the Release 1.1 and
        // 1.3.1 changes, which is why it must not answer true either.
        for (String version : new String[] {
            "historical_experience:alpha",
            "historical_experience:beta_1_7_3",
            "historical_experience:infdev_420"
        }) {
            assertFalse(HistoricalGameplayTweaks.betaGoldenApple(version),
                version + " has no hunger meter, so the Beta 1.8 golden apple does not apply to it");
        }
    }
}
