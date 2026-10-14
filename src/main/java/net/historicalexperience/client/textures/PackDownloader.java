/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.client.textures;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.LongConsumer;

/**
 * Fetches the pinned dependency packs into the mod's own folder, verifying every byte before it is kept.
 *
 * <h2>Safety properties</h2>
 *
 * <ul>
 *   <li><b>Pinned, exact.</b> Only {@link PackPin} entries can be fetched: a fixed HTTPS URL on Modrinth's CDN, an
 *       exact size and an exact SHA-512. There is no "latest" and no URL taken from anywhere else.</li>
 *   <li><b>Verified before use.</b> The body is written to a temporary file, hashed while streaming, and only moved to
 *       its final name if the length and SHA-512 both match. A mismatch, a short read or a network error leaves no
 *       file at the final path.</li>
 *   <li><b>Bounded.</b> A response longer than the pinned size is abandoned, so a wrong or hostile body cannot fill
 *       the disk.</li>
 *   <li><b>Never touches the player's pack folder or selection.</b> The files live in the mod's own directory, so they
 *       do not appear in the resource-pack menu and are not duplicated there.</li>
 *   <li><b>Off the render thread.</b> Downloads run on one background thread; progress is read through
 *       {@link #state}.</li>
 * </ul>
 */
public final class PackDownloader {
    public enum Phase { IDLE, DOWNLOADING, DONE, FAILED }

    /** A snapshot of one pack's download. */
    public record State(Phase phase, long bytes, long total, String message) {
        static final State IDLE = new State(Phase.IDLE, 0, 0, "");
    }

    /** Where bytes come from. The real one is HTTPS; tests substitute an in-memory source. */
    @FunctionalInterface
    public interface Fetcher {
        InputStream open(PackPin pin) throws IOException;
    }

    private final Path directory;
    private final Fetcher fetcher;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "historical-experience-pack-download");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<TextureSource, State> states = new EnumMap<>(TextureSource.class);
    private volatile int running;

    public PackDownloader(Path directory, Fetcher fetcher) {
        this.directory = directory;
        this.fetcher = fetcher;
    }

    /** The production fetcher: HTTPS GET of the pin's own URL, redirects followed, no other host accepted. */
    public static Fetcher httpsFetcher() {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15)).build();
        return pin -> {
            URI uri = URI.create(pin.url());
            if (!"https".equals(uri.getScheme()) || !"cdn.modrinth.com".equals(uri.getHost())) {
                throw new IOException("refusing a pin that is not a Modrinth CDN HTTPS URL: " + uri);
            }
            try {
                HttpResponse<InputStream> response = client.send(
                    HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(2))
                        .header("User-Agent", "historical-experience-mod/0.1 (pinned texture dependency)").GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    response.body().close();
                    throw new IOException("HTTP " + response.statusCode() + " from " + uri.getHost());
                }
                return response.body();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IOException("download interrupted", interrupted);
            }
        };
    }

    public Path directory() {
        return directory;
    }

    public synchronized State state(TextureSource source) {
        return states.getOrDefault(source, State.IDLE);
    }

    public boolean isRunning() {
        return running > 0;
    }

    private synchronized void set(TextureSource source, State state) {
        states.put(source, state);
    }

    /** Queues a download for each source, then runs {@code done} on the background thread when all have finished. */
    public void start(Collection<TextureSource> sources, Runnable done) {
        synchronized (this) {
            running++;
            for (TextureSource source : sources) {
                PackPin pin = PackPin.of(source);
                if (pin != null) {
                    states.put(source, new State(Phase.DOWNLOADING, 0, pin.size(), ""));
                }
            }
        }
        executor.execute(() -> {
            try {
                for (TextureSource source : sources) {
                    PackPin pin = PackPin.of(source);
                    if (pin != null) {
                        fetch(pin);
                    }
                }
            } finally {
                synchronized (this) {
                    running--;
                }
                if (done != null) {
                    done.run();
                }
            }
        });
    }

    /** Synchronous single fetch; public so tests can drive it deterministically. Never throws. */
    public State fetch(PackPin pin) {
        TextureSource source = pin.source();
        Path target = directory.resolve(pin.fileName());
        Path partial = directory.resolve(pin.fileName() + ".part");
        try {
            Files.createDirectories(directory);
            set(source, new State(Phase.DOWNLOADING, 0, pin.size(), ""));
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            long total = 0;
            LongConsumer progress = bytes -> set(source, new State(Phase.DOWNLOADING, bytes, pin.size(), ""));
            try (InputStream in = fetcher.open(pin); var out = Files.newOutputStream(partial)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    total += read;
                    if (total > pin.size()) {
                        throw new IOException("more data than the pinned size of " + pin.size() + " bytes");
                    }
                    digest.update(buffer, 0, read);
                    out.write(buffer, 0, read);
                    progress.accept(total);
                }
            }
            if (total != pin.size()) {
                throw new IOException("received " + total + " bytes, expected " + pin.size());
            }
            String actual = HexFormat.of().formatHex(digest.digest());
            if (!actual.equalsIgnoreCase(pin.sha512())) {
                throw new IOException("SHA-512 mismatch; the file is not the pinned release");
            }
            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            State done = new State(Phase.DONE, total, pin.size(), "");
            set(source, done);
            return done;
        } catch (IOException | java.security.GeneralSecurityException | RuntimeException failure) {
            try {
                Files.deleteIfExists(partial);
            } catch (IOException ignored) {
                // A leftover .part file is never read as a pack; it is replaced by the next attempt.
            }
            State failed = new State(Phase.FAILED, 0, pin.size(), String.valueOf(failure.getMessage()));
            set(source, failed);
            return failed;
        }
    }
}
