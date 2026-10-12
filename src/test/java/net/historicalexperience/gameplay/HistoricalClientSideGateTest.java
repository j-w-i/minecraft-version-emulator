package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Guards the rule that has now broken twice: a client-visible behaviour must ask the session, never a level.
 *
 * <p>Sprinting broke this way in round 36 and sword blocking broke it the same way, found only when the owner
 * reported that the block animation did not play. The cause is identical both times: the level a client holds is
 * a {@code ClientLevel}, so a level-based question returns null there, the rule answers false, and the behaviour
 * switches off on the side that has to draw it. On the server it still looks enabled, so a gate read from the
 * server side reports everything as working.
 *
 * <p>This is a source-level guard rather than a behavioural one, and deliberately so: the failure is a wiring
 * mistake, and no unit test can construct a client player to catch it behaviourally. The same technique is
 * already used to assert that every mixin on disk is registered.
 */
class HistoricalClientSideGateTest {
    private static final Path GAMEPLAY = Path.of("src/main/java/net/historicalexperience/gameplay");

    private static String read(String fileName) throws Exception {
        return Files.readString(GAMEPLAY.resolve(fileName));
    }

    @Test
    void swordBlockingAsksTheSessionSoTheClientCanAnswer() throws Exception {
        String blocking = read("HistoricalSwordBlocking.java");
        String canBlock = blocking.substring(blocking.indexOf("public static boolean canBlock"));

        assertTrue(canBlock.contains("HistoricalActiveSession.current()"),
            "canBlock must ask the active session: it is asked on the client, where a level-based question "
                + "always answers false and the block becomes invisible");
        assertFalse(canBlock.contains("player.level()"),
            "canBlock must not ask the player's level: a client's level is a ClientLevel, so this is the exact "
                + "line that made the block animation fail to play");
        assertTrue(blocking.contains("HistoricalActiveSession"),
            "the session must be referenced at all");
    }

    @Test
    void versionBoundariesAreAskableWithoutALevel() throws Exception {
        String tweaks = read("HistoricalGameplayTweaks.java");

        assertTrue(tweaks.contains("public static boolean swordBlockingEnabled(String versionId)"),
            "a version-id overload must exist so a rule can ask about a version rather than a level");
        assertTrue(tweaks.contains("return swordBlockingEnabled(versionOf(level));"),
            "the level overload must delegate to the version overload, so there is still only one boundary");
    }

    @Test
    void noMixinAsksAVersionGateAboutALevel() throws Exception {
        java.util.List<String> offenders = new java.util.ArrayList<>();
        Path mixins = Path.of("src/main/java/net/historicalexperience/mixin");

        try (java.util.stream.Stream<Path> files = Files.list(mixins)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("(?:Disabled|Enabled)\\(\\s*[\\w.]+\\.level\\(\\)")
                    .matcher(source);

                if (matcher.find()) {
                    offenders.add(file.getFileName().toString());
                }
            }
        }

        assertTrue(offenders.isEmpty(),
            "a mixin asks a version gate about a level, which answers false on the client and switches the "
                + "behaviour off on the side that draws it: " + offenders);
    }
}
