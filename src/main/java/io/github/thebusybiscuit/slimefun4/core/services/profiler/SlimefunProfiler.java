package io.github.thebusybiscuit.slimefun4.core.services.profiler;

import io.github.thebusybiscuit.cscorelib2.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.implementation.tasks.TickerTask;
import io.github.thebusybiscuit.slimefun4.utils.NumberUtils;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import org.apache.commons.lang.Validate;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@link SlimefunProfiler} works closely to the {@link TickerTask} and is
 * responsible for monitoring that task.
 * It collects timings data for any ticked {@link Block} and the corresponding {@link SlimefunItem}.
 * This allows developers to identify laggy {@link SlimefunItem SlimefunItems} or {@link SlimefunAddon SlimefunAddons}.
 * But it also enables Server Admins to locate lag-inducing areas on the {@link Server}.
 *
 * @author TheBusyBiscuit
 *
 * @see TickerTask
 *
 */
public class SlimefunProfiler {
    // A minecraft server tick is 50ms and Slimefun ticks are stretched across
    // two ticks (sync and async blocks), so we use 100ms as a reference here
    private static final int MAX_TICK_DURATION = 100;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger queued = new AtomicInteger(0);

    private long totalElapsedTime;

    private final Map<ProfiledBlock, Long> timings = new ConcurrentHashMap<>();
    private final Queue<CommandSender> requests = new ConcurrentLinkedQueue<>();

    /**
     * This method starts the profiling, data from previous runs will be cleared.
     */
    public void start() {
        running.set(true);
        queued.set(0);
        timings.clear();
    }

    /**
     * This method starts a new profiler entry.
     *
     * @return A timestamp, best fed back into {@link #closeEntry(Location, SlimefunItem, long)}
     */
    public long newEntry() {
        if (!running.get()) {
            return 0;
        }

        queued.incrementAndGet();
        return System.nanoTime();
    }

    /**
     * This method schedules a given amount of entries for the future.
     * Be careful to {@link #closeEntry(Location, SlimefunItem, long)} all of them again!
     * No {@link PerformanceSummary} will be sent until all entires were closed.
     *
     * @param amount The amount of entries that should be scheduled.
     */
    public void scheduleEntries(int amount) {
        if (running.get()) {
            queued.getAndAdd(amount);
        }
    }

    /**
     * This method closes a previously started entry.
     * Make sure to call {@link #newEntry()} to get the timestamp in advance.
     *
     * @param l         The {@link Location} of our {@link Block}
     * @param item      The {@link SlimefunItem} at this {@link Location}
     * @param timestamp The timestamp marking the start of this entry, you can retrieve it using {@link #newEntry()}
     * @return The total timings of this entry
     */
    public long closeEntry(Location l, SlimefunItem item, long timestamp) {
        Validate.notNull(l, "Location must not be null!");
        Validate.notNull(item, "You need to specify a SlimefunItem!");

        if (timestamp == 0) {
            return 0;
        }

        long elapsedTime = System.nanoTime() - timestamp;

        executor.execute(() -> {
            ProfiledBlock block = new ProfiledBlock(new BlockPosition(l), item);
            timings.put(block, elapsedTime);
            queued.decrementAndGet();
        });

        return elapsedTime;
    }

    /**
     * This stops the profiling.
     */
    public void stop() {
        running.set(false);

        if (SlimefunPlugin.instance() == null || !SlimefunPlugin.instance().isEnabled()) {
            // Slimefun has been disabled
            return;
        }

        // Since we got more than one Thread in our pool, blocking this one is completely fine
        executor.execute(() -> {

            // Wait for all timing results to come in
            while (queued.get() > 0 && !running.get()) {
                // Ideally we would wait some time here but the ticker task may be faster
                // than 1ms, so it would halt this summary for up to 7 minutes
                // Not perfect performance-wise but this is a seperate Thread anyway
            }

            if (running.get()) {
                // Looks like the next profiling has already started, abort!
                return;
            }

            totalElapsedTime = timings.values().stream().mapToLong(Long::longValue).sum();

            if (!requests.isEmpty()) {
                PerformanceSummary summary = new PerformanceSummary(this, totalElapsedTime, timings.size());
                Iterator<CommandSender> iterator = requests.iterator();

                while (iterator.hasNext()) {
                    summary.send(iterator.next());
                    iterator.remove();
                }
            }
        });

    }

    /**
     * This method requests a summary for the given {@link CommandSender}.
     * The summary will be sent upon the next available moment in time.
     *
     * @param sender
     *            The {@link CommandSender} who shall receive this summary.
     */
    public void requestSummary(CommandSender sender) {
        Validate.notNull(sender, "Cannot request a summary for null");

        requests.add(sender);
    }

    protected Map<String, Long> getByItem() {
        Map<String, Long> map = new HashMap<>();

        for (Map.Entry<ProfiledBlock, Long> entry : timings.entrySet()) {
            map.merge(entry.getKey().getId(), entry.getValue(), Long::sum);
        }

        return map;
    }

    protected Map<String, Long> getByPlugin() {
        Map<String, Long> map = new HashMap<>();

        for (Map.Entry<ProfiledBlock, Long> entry : timings.entrySet()) {
            map.merge(entry.getKey().getAddon().getName(), entry.getValue(), Long::sum);
        }

        return map;
    }

    protected Map<String, Long> getByChunk() {
        Map<String, Long> map = new HashMap<>();

        for (Map.Entry<ProfiledBlock, Long> entry : timings.entrySet()) {
            String world = entry.getKey().getPosition().getWorld().getName();
            int x = entry.getKey().getPosition().getChunkX();
            int z = entry.getKey().getPosition().getChunkZ();

            map.merge(world + " (" + x + ',' + z + ')', entry.getValue(), Long::sum);
        }

        return map;
    }

    protected int getBlocksInChunk(String chunk) {
        int blocks = 0;

        for (ProfiledBlock block : timings.keySet()) {
            String world = block.getPosition().getWorld().getName();
            int x = block.getPosition().getChunkX();
            int z = block.getPosition().getChunkZ();

            if (chunk.equals(world + " (" + x + ',' + z + ')')) {
                blocks++;
            }
        }

        return blocks;
    }

    protected int getBlocksOfId(String id) {
        int blocks = 0;

        for (ProfiledBlock block : timings.keySet()) {
            if (block.getId().equals(id)) {
                blocks++;
            }
        }

        return blocks;
    }

    protected int getBlocksFromPlugin(String id) {
        int blocks = 0;

        for (ProfiledBlock block : timings.keySet()) {
            if (block.getAddon().getName().equals(id)) {
                blocks++;
            }
        }

        return blocks;
    }

    protected float getPercentageOfTick() {
        float millis = totalElapsedTime / 1000000.0F;
        float fraction = (millis * 100.0F) / MAX_TICK_DURATION;
        return Math.round((fraction * 100.0F) / 100.0F);
    }

    /**
     * This method returns the current {@link PerformanceRating}.
     *
     * @return The current performance grade
     */
    public PerformanceRating getPerformance() {
        float percentage = getPercentageOfTick();

        for (PerformanceRating rating : PerformanceRating.values()) {
            if (rating.test(percentage)) {
                return rating;
            }
        }

        return PerformanceRating.UNKNOWN;
    }

    public String getTime() {
        return NumberUtils.getAsMillis(totalElapsedTime);
    }

    /**
     * This method checks whether the {@link SlimefunProfiler} has collected timings on
     * the given {@link Block}
     *
     * @param b The {@link Block}
     * @return Whether timings of this {@link Block} have been collected
     */
    public boolean hasTimings(Block b) {
        Validate.notNull("Cannot get timings for a null Block");
        return timings.containsKey(new ProfiledBlock(b));
    }

    public String getTime(Block b) {
        Validate.notNull("Cannot get timings for a null Block");

        long time = timings.getOrDefault(new ProfiledBlock(b), 0L);
        return NumberUtils.getAsMillis(time);
    }

    public String getTime(Chunk chunk) {
        Validate.notNull("Cannot get timings for a null Chunk");

        long time = getByChunk().getOrDefault(chunk.getWorld().getName() + " (" + chunk.getX() + ',' + chunk.getZ() + ')', 0L);
        return NumberUtils.getAsMillis(time);
    }

    public String getTime(SlimefunItem item) {
        Validate.notNull("Cannot get timings for a null SlimefunItem");

        long time = getByItem().getOrDefault(item.getID(), 0L);
        return NumberUtils.getAsMillis(time);
    }

}