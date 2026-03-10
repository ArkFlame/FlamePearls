package com.arkflame.flamepearls.utils;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import com.arkflame.flamepearls.FlamePearls;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class FoliaAPI {
    private static Map<String, Method> cachedMethods = new HashMap<>(); // cached reflection methods
    private static BukkitScheduler bS = Bukkit.getScheduler(); // bukkit scheduler
    private static Object globalRegionScheduler = getGlobalRegionScheduler(); // folia global scheduler
    private static Object regionScheduler = getRegionScheduler(); // folia region scheduler
    private static Object asyncScheduler = getAsyncScheduler(); // folia async scheduler

    // FIX 1: cache isFolia() result at init time instead of re-checking every call
    private static final boolean IS_FOLIA = detectFolia();

    static {
        cacheMethods(); // populate cache early
    }

    private static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        if (clazz == null) return null; // no class available
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null; // method missing
        }
    }

    private static void cacheMethods() {
        if (globalRegionScheduler != null) {
            Method runAtFixedRateMethod = getMethod(globalRegionScheduler.getClass(), "runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
            if (runAtFixedRateMethod != null) cachedMethods.put("globalRegionScheduler.runAtFixedRate", runAtFixedRateMethod);
            Method runMethod = getMethod(globalRegionScheduler.getClass(), "run", Plugin.class, Consumer.class);
            if (runMethod != null) cachedMethods.put("globalRegionScheduler.run", runMethod);
            Method runDelayedMethod = getMethod(globalRegionScheduler.getClass(), "runDelayed", Plugin.class, Consumer.class, long.class);
            if (runDelayedMethod != null) cachedMethods.put("globalRegionScheduler.runDelayed", runDelayedMethod);
            Method cancelTasksMethod = getMethod(globalRegionScheduler.getClass(), "cancelTasks", Plugin.class);
            if (cancelTasksMethod != null) cachedMethods.put("globalRegionScheduler.cancelTasks", cancelTasksMethod);
        }
        if (regionScheduler != null) {
            Method executeMethod = getMethod(regionScheduler.getClass(), "execute", Plugin.class, World.class, int.class, int.class, Runnable.class);
            if (executeMethod != null) cachedMethods.put("regionScheduler.execute", executeMethod);
            Method executeLocationMethod = getMethod(regionScheduler.getClass(), "execute", Plugin.class, Location.class, Runnable.class);
            if (executeLocationMethod != null) cachedMethods.put("regionScheduler.executeLocation", executeLocationMethod);
            Method runAtFixedRateMethod = getMethod(regionScheduler.getClass(), "runAtFixedRate", Plugin.class, Location.class, Consumer.class, long.class, long.class);
            if (runAtFixedRateMethod != null) cachedMethods.put("regionScheduler.runAtFixedRate", runAtFixedRateMethod);
            Method runDelayedMethod = getMethod(regionScheduler.getClass(), "runDelayed", Plugin.class, Location.class, Consumer.class, long.class);
            if (runDelayedMethod != null) cachedMethods.put("regionScheduler.runDelayed", runDelayedMethod);
        }

        // FIX 2: cache entity scheduler methods against the correct scheduler class, not Entity itself.
        // We look them up by name since we don't have a live instance at static-init time; the
        // per-entity lookup in runTaskForEntity() will then use these cached entries.
        Method getSchedulerMethod = getMethod(Entity.class, "getScheduler");
        if (getSchedulerMethod != null) {
            getSchedulerMethod.setAccessible(true);
            cachedMethods.put("entity.getScheduler", getSchedulerMethod);

            // FIX 3: cache entityScheduler.execute and entityScheduler.runAtFixedRate by resolving
            // the scheduler class from the method's return type instead of re-doing reflection each call.
            Class<?> entitySchedulerClass = getSchedulerMethod.getReturnType();
            if (entitySchedulerClass != null && entitySchedulerClass != void.class) {
                Method executeEntityMethod = getMethod(entitySchedulerClass, "execute",
                        Plugin.class, Runnable.class, Runnable.class, long.class);
                if (executeEntityMethod != null) {
                    executeEntityMethod.setAccessible(true);
                    cachedMethods.put("entityScheduler.execute", executeEntityMethod);
                }
                Method runAtFixedRateEntityMethod = getMethod(entitySchedulerClass, "runAtFixedRate",
                        Plugin.class, Consumer.class, Runnable.class, long.class, long.class);
                if (runAtFixedRateEntityMethod != null) {
                    runAtFixedRateEntityMethod.setAccessible(true);
                    cachedMethods.put("entityScheduler.runAtFixedRate", runAtFixedRateEntityMethod);
                }
            }
        }

        Method teleportAsyncMethod = getMethod(Player.class, "teleportAsync", Location.class);
        if (teleportAsyncMethod != null) cachedMethods.put("player.teleportAsync", teleportAsyncMethod);
        Method teleportAsyncWithCause = getMethod(Player.class, "teleportAsync", Location.class, TeleportCause.class);
        if (teleportAsyncWithCause != null) cachedMethods.put("player.teleportAsyncCause", teleportAsyncWithCause);
        if (asyncScheduler != null) {
            Method cancelTasksMethod = getMethod(asyncScheduler.getClass(), "cancelTasks", Plugin.class);
            if (cancelTasksMethod != null) cachedMethods.put("asyncScheduler.cancelTasks", cancelTasksMethod);
        }

        // FIX 4: cache the three Server scheduler lookup methods so getGlobalRegionScheduler() etc.
        // never repeat the same getMethod(Server.class, ...) call at runtime.
        Method getGlobalMethod = getMethod(Server.class, "getGlobalRegionScheduler");
        if (getGlobalMethod != null) cachedMethods.put("server.getGlobalRegionScheduler", getGlobalMethod);
        Method getRegionMethod = getMethod(Server.class, "getRegionScheduler");
        if (getRegionMethod != null) cachedMethods.put("server.getRegionScheduler", getRegionMethod);
        Method getAsyncMethod = getMethod(Server.class, "getAsyncScheduler");
        if (getAsyncMethod != null) cachedMethods.put("server.getAsyncScheduler", getAsyncMethod);
    }

    private static Object invokeMethod(Method method, Object object, Object... args) {
        try {
            if (method != null && object != null) {
                method.setAccessible(true);
                return method.invoke(object, args);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Object getGlobalRegionScheduler() {
        // uses getMethod directly here because cacheMethods() hasn't run yet at field-init time;
        // the result (the scheduler object) is stored in the static field and reused everywhere.
        Method method = getMethod(Server.class, "getGlobalRegionScheduler");
        return invokeMethod(method, Bukkit.getServer());
    }

    private static Object getRegionScheduler() {
        Method method = getMethod(Server.class, "getRegionScheduler");
        return invokeMethod(method, Bukkit.getServer());
    }

    private static Object getAsyncScheduler() {
        Method method = getMethod(Server.class, "getAsyncScheduler");
        return invokeMethod(method, Bukkit.getServer());
    }

    // FIX 1 (impl): one-time Folia detection called only from the static field initializer.
    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return globalRegionScheduler != null && regionScheduler != null;
        } catch (Exception ig) {
            return false;
        }
    }

    // isFolia() now returns the pre-computed constant — zero overhead per call.
    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public static void runTaskAsync(Runnable run, long delay) {
        if (!isFolia()) {
            bS.runTaskLaterAsynchronously(FlamePearls.getInstance(), run, delay);
            return;
        }
        Executors.defaultThreadFactory().newThread(run).start();
    }

    public static void runTaskAsync(Runnable run) {
        runTaskAsync(run, 1L);
    }

    public static void runTaskTimerAsync(Consumer<Object> run, long delay, long period) {
        if (!isFolia()) {
            bS.runTaskTimerAsynchronously(FlamePearls.getInstance(), () -> run.accept(null), delay, period);
            return;
        }
        Method method = cachedMethods.get("globalRegionScheduler.runAtFixedRate");
        invokeMethod(method, globalRegionScheduler, FlamePearls.getInstance(), run, delay, period);
    }

    public static void runTaskTimerAsync(Runnable runnable, long delay, long period) {
        runTaskTimerAsync(obj -> runnable.run(), delay, period);
    }

    public static void runTaskTimer(Consumer<Object> run, long delay, long period) {
        if (!isFolia()) {
            bS.runTaskTimer(FlamePearls.getInstance(), () -> run.accept(null), delay, period);
            return;
        }
        Method method = cachedMethods.get("globalRegionScheduler.runAtFixedRate");
        invokeMethod(method, globalRegionScheduler, FlamePearls.getInstance(), run, delay, period);
    }

    public static void runTask(Runnable run) {
        if (!isFolia()) {
            bS.runTask(FlamePearls.getInstance(), run);
            return;
        }
        Method method = cachedMethods.get("globalRegionScheduler.run");
        invokeMethod(method, globalRegionScheduler, FlamePearls.getInstance(), (Consumer<Object>) ignored -> run.run());
    }

    public static void runTask(Consumer<Object> run) {
        if (!isFolia()) {
            bS.runTask(FlamePearls.getInstance(), () -> run.accept(null));
            return;
        }
        Method method = cachedMethods.get("globalRegionScheduler.run");
        invokeMethod(method, globalRegionScheduler, FlamePearls.getInstance(), run);
    }

    public static void runTaskLater(Runnable run, long delay) {
        if (!isFolia()) {
            bS.runTaskLater(FlamePearls.getInstance(), run, delay);
            return;
        }
        Method method = cachedMethods.get("globalRegionScheduler.runDelayed");
        invokeMethod(method, globalRegionScheduler, FlamePearls.getInstance(), (Consumer<Object>) ignored -> run.run(), delay);
    }

    public static void runTaskLater(Consumer<Object> run, long delay) {
        if (!isFolia()) {
            bS.runTaskLater(FlamePearls.getInstance(), () -> run.accept(null), delay);
            return;
        }
        Method method = cachedMethods.get("globalRegionScheduler.runDelayed");
        invokeMethod(method, globalRegionScheduler, FlamePearls.getInstance(), run, delay);
    }

    // FIX 2+3: runTaskForEntity() now uses cachedMethods exclusively — no more raw
    // entity.getClass().getMethod(...) calls on the hot path.
    public static void runTaskForEntity(Entity entity, Runnable run, Runnable retired, long delay) {
        if (entity == null) return; // ignore null entity
        if (!isFolia()) {
            Bukkit.getScheduler().runTaskLater(FlamePearls.getInstance(), run, delay); // fallback for non-folia
            return;
        }
        try {
            Method getSchedulerMethod = cachedMethods.get("entity.getScheduler");
            if (getSchedulerMethod == null) { run.run(); return; } // failsafe
            Object entityScheduler = getSchedulerMethod.invoke(entity);
            if (entityScheduler == null) { run.run(); return; } // failsafe

            Method executeMethod = cachedMethods.get("entityScheduler.execute");
            if (executeMethod == null) { run.run(); return; } // failsafe
            executeMethod.invoke(
                entityScheduler,
                FlamePearls.getInstance(),
                run,
                retired,
                Math.max(1L, delay)
            );
        } catch (Exception e) {
            e.printStackTrace();
            run.run(); // failsafe run immediately
        }
    }

    public static void runTaskForEntityRepeating(Entity entity, Consumer<Object> task, Runnable retired,
                                                 long initialDelay, long period) {
        if (!isFolia()) {
            bS.runTaskTimer(FlamePearls.getInstance(), () -> task.accept(null), initialDelay, period);
            return;
        }
        if (entity == null) return;
        Method getSchedulerMethod = cachedMethods.get("entity.getScheduler");
        Object entityScheduler = invokeMethod(getSchedulerMethod, entity);
        Method runAtFixedRateMethod = cachedMethods.get("entityScheduler.runAtFixedRate");
        invokeMethod(runAtFixedRateMethod, entityScheduler, FlamePearls.getInstance(), task, retired, initialDelay, period);
    }

    public static void runTaskForRegion(World world, int chunkX, int chunkZ, Runnable run) {
        if (!isFolia()) {
            bS.runTask(FlamePearls.getInstance(), run);
            return;
        }
        if (world == null) return;
        Method executeMethod = cachedMethods.get("regionScheduler.execute");
        invokeMethod(executeMethod, regionScheduler, FlamePearls.getInstance(), world, chunkX, chunkZ, run);
    }

    public static void runTaskForRegion(Location location, Runnable run) {
        if (!isFolia()) {
            bS.runTask(FlamePearls.getInstance(), run);
            return;
        }
        if (location == null) return;
        Method executeMethod = cachedMethods.get("regionScheduler.executeLocation");
        invokeMethod(executeMethod, regionScheduler, FlamePearls.getInstance(), location, run);
    }

    public static void runTaskForRegionRepeating(Location location, Consumer<Object> task, long initialDelay,
                                                 long period) {
        if (!isFolia()) {
            bS.runTaskTimer(FlamePearls.getInstance(), () -> task.accept(null), initialDelay, period);
            return;
        }
        if (location == null) return;
        Method runAtFixedRateMethod = cachedMethods.get("regionScheduler.runAtFixedRate");
        invokeMethod(runAtFixedRateMethod, regionScheduler, FlamePearls.getInstance(), location, task, initialDelay, period);
    }

    public static void runTaskForRegionDelayed(Location location, Consumer<Object> task, long delay) {
        if (!isFolia()) {
            bS.runTaskLater(FlamePearls.getInstance(), () -> task.accept(null), delay);
            return;
        }
        if (location == null) return;
        Method runDelayedMethod = cachedMethods.get("regionScheduler.runDelayed");
        invokeMethod(runDelayedMethod, regionScheduler, FlamePearls.getInstance(), location, task, delay);
    }

    // helper to attach a CompletableFuture result to 'out' following previous semantics
    private static void attachFutureToOut(Object res, final CompletableFuture<Boolean> out) {
        if (res instanceof CompletableFuture) {
            @SuppressWarnings("unchecked")
            CompletableFuture<Boolean> f = (CompletableFuture<Boolean>) res;
            f.whenComplete((ok, ex) -> {
                if (ex != null) out.complete(false);
                else out.complete(Boolean.TRUE.equals(ok));
            });
        } else {
            out.complete(res != null);
        }
    }

    // central synchronous teleport performer that completes the given CompletableFuture
    private static void performTeleportAndComplete(Player e, Location location, TeleportCause cause, CompletableFuture<Boolean> out) {
        // prefer teleportAsync with cause if available
        Method teleportAsyncWithCause = cachedMethods.get("player.teleportAsyncCause");
        if (teleportAsyncWithCause != null) {
            Object res = invokeMethod(teleportAsyncWithCause, e, location, cause);
            attachFutureToOut(res, out);
            return;
        }
        // try teleportAsync without cause
        Method teleportAsyncMethod = cachedMethods.get("player.teleportAsync");
        if (teleportAsyncMethod != null) {
            Object res = invokeMethod(teleportAsyncMethod, e, location);
            attachFutureToOut(res, out);
            return;
        }
        // fallback to synchronous teleport
        if (cause != null) {
            out.complete(e.teleport(location, cause));
        } else {
            out.complete(e.teleport(location));
        }
    }

    public static CompletableFuture<Boolean> teleportPlayer(Player e, Location location, Boolean async) {
        if (e == null) return CompletableFuture.completedFuture(false); // null guard
        if (isFolia()) {
            final CompletableFuture<Boolean> out = new CompletableFuture<>();
            runTaskForEntity(e, () -> performTeleportAndComplete(e, location, null, out), () -> {}, 1L);
            return out;
        }
        // non-Folia synchronous scheduling, attempt to use teleportAsync methods if present
        Method teleportAsyncWithCause = cachedMethods.get("player.teleportAsyncCause");
        if (teleportAsyncWithCause != null) {
            Object res = invokeMethod(teleportAsyncWithCause, e, location, (TeleportCause) null);
            if (res instanceof CompletableFuture) {
                @SuppressWarnings("unchecked")
                CompletableFuture<Boolean> f = (CompletableFuture<Boolean>) res;
                return f;
            }
            return CompletableFuture.completedFuture(res != null);
        }
        Method teleportAsyncMethod = cachedMethods.get("player.teleportAsync");
        if (teleportAsyncMethod != null) {
            Object res = invokeMethod(teleportAsyncMethod, e, location);
            if (res instanceof CompletableFuture) {
                @SuppressWarnings("unchecked")
                CompletableFuture<Boolean> f = (CompletableFuture<Boolean>) res;
                return f;
            }
            return CompletableFuture.completedFuture(res != null);
        }
        // fallback synchronous teleport
        e.teleport(location);
        return CompletableFuture.completedFuture(true);
    }

    public static CompletableFuture<Boolean> teleportPlayer(Player e, Location location, TeleportCause cause) {
        if (e == null) return CompletableFuture.completedFuture(false); // null guard
        if (isFolia()) {
            final CompletableFuture<Boolean> out = new CompletableFuture<>();
            runTaskForEntity(e, () -> performTeleportAndComplete(e, location, cause, out), () -> {}, 2L);
            return out;
        }
        Method teleportAsyncWithCause = cachedMethods.get("player.teleportAsyncCause");
        if (teleportAsyncWithCause != null) {
            Object res = invokeMethod(teleportAsyncWithCause, e, location, cause);
            if (res instanceof CompletableFuture) {
                @SuppressWarnings("unchecked")
                CompletableFuture<Boolean> f = (CompletableFuture<Boolean>) res;
                return f;
            }
            return CompletableFuture.completedFuture(res != null);
        }
        Method teleportAsyncMethod = cachedMethods.get("player.teleportAsync");
        if (teleportAsyncMethod != null) {
            Object res = invokeMethod(teleportAsyncMethod, e, location);
            if (res instanceof CompletableFuture) {
                @SuppressWarnings("unchecked")
                CompletableFuture<Boolean> f = (CompletableFuture<Boolean>) res;
                return f;
            }
            return CompletableFuture.completedFuture(res != null);
        }
        if (cause != null) {
            return CompletableFuture.completedFuture(e.teleport(location, cause));
        }
        runTaskForEntity(e, () -> e.teleport(location), () -> {}, 1L);
        return CompletableFuture.completedFuture(true);
    }

    public static CompletableFuture<Boolean> teleportPlayer(Player e, Location location, TeleportCause cause, long delay) {
        if (e == null) return CompletableFuture.completedFuture(false); // null guard
        final long safeDelay = Math.max(1L, delay); // ensure at least 1 tick for folia entity scheduler
        if (isFolia()) {
            final CompletableFuture<Boolean> out = new CompletableFuture<>();
            runTaskForEntity(e, () -> performTeleportAndComplete(e, location, cause, out), () -> {}, safeDelay);
            return out;
        }
        final CompletableFuture<Boolean> out = new CompletableFuture<>();
        Runnable task = () -> performTeleportAndComplete(e, location, cause, out); // scheduleable task
        bS.runTaskLater(FlamePearls.getInstance(), task, delay); // non-folia uses sync scheduling as before
        return out;
    }

    public static CompletableFuture<Boolean> teleportPlayer(Player e, Location location, Boolean async, long delay) {
        if (e == null) return CompletableFuture.completedFuture(false); // null guard
        final long safeDelay = Math.max(1L, delay);
        if (isFolia()) {
            final CompletableFuture<Boolean> out = new CompletableFuture<>();
            runTaskForEntity(e, () -> performTeleportAndComplete(e, location, null, out), () -> {}, safeDelay);
            return out;
        }
        final CompletableFuture<Boolean> out = new CompletableFuture<>();
        Runnable task = () -> performTeleportAndComplete(e, location, null, out);
        if (Boolean.TRUE.equals(async)) {
            bS.runTaskLaterAsynchronously(FlamePearls.getInstance(), task, delay);
        } else {
            bS.runTaskLater(FlamePearls.getInstance(), task, delay);
        }
        return out;
    }

    public static void cancelAllTasks() {
        Plugin plugin = FlamePearls.getInstance();
        if (!isFolia()) {
            bS.cancelTasks(plugin); // cancel bukkit tasks
            return;
        }
        Method cancelGlobalMethod = cachedMethods.get("globalRegionScheduler.cancelTasks");
        invokeMethod(cancelGlobalMethod, globalRegionScheduler, plugin); // cancel folia global tasks
        Method cancelAsyncMethod = cachedMethods.get("asyncScheduler.cancelTasks");
        invokeMethod(cancelAsyncMethod, asyncScheduler, plugin); // cancel modern async scheduler tasks
    }
}