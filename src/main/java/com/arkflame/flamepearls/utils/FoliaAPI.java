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
    private static Map<String, Method> cachedMethods = new HashMap<>();

    private static BukkitScheduler bS = Bukkit.getScheduler();
    private static Object globalRegionScheduler = getGlobalRegionScheduler();
    private static Object regionScheduler = getRegionScheduler();
    private static Object asyncScheduler = getAsyncScheduler();

    // Cache methods as early as possible
    static {
        cacheMethods();
    }

    private static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        if (clazz == null) {
            return null;
        }
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            // Gracefully handle the case where the method does not exist
            return null;
        }
    }

    private static void cacheMethods() {
        // Cache methods for globalRegionScheduler
        if (globalRegionScheduler != null) {
            Method runAtFixedRateMethod = getMethod(globalRegionScheduler.getClass(), "runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
            if (runAtFixedRateMethod != null) {
                cachedMethods.put("globalRegionScheduler.runAtFixedRate", runAtFixedRateMethod);
            }

            Method runMethod = getMethod(globalRegionScheduler.getClass(), "run", Plugin.class, Consumer.class);
            if (runMethod != null) {
                cachedMethods.put("globalRegionScheduler.run", runMethod);
            }

            Method runDelayedMethod = getMethod(globalRegionScheduler.getClass(), "runDelayed", Plugin.class, Consumer.class, long.class);
            if (runDelayedMethod != null) {
                cachedMethods.put("globalRegionScheduler.runDelayed", runDelayedMethod);
            }

            Method cancelTasksMethod = getMethod(globalRegionScheduler.getClass(), "cancelTasks", Plugin.class);
            if (cancelTasksMethod != null) {
                cachedMethods.put("globalRegionScheduler.cancelTasks", cancelTasksMethod);
            }
        }

        // Cache methods for regionScheduler
        if (regionScheduler != null) {
            Method executeMethod = getMethod(regionScheduler.getClass(), "execute", Plugin.class, World.class, int.class, int.class, Runnable.class);
            if (executeMethod != null) {
                cachedMethods.put("regionScheduler.execute", executeMethod);
            }

            Method executeLocationMethod = getMethod(regionScheduler.getClass(), "execute", Plugin.class, Location.class, Runnable.class);
            if (executeLocationMethod != null) {
                cachedMethods.put("regionScheduler.executeLocation", executeLocationMethod);
            }

            Method runAtFixedRateMethod = getMethod(regionScheduler.getClass(), "runAtFixedRate", Plugin.class, Location.class, Consumer.class, long.class, long.class);
            if (runAtFixedRateMethod != null) {
                cachedMethods.put("regionScheduler.runAtFixedRate", runAtFixedRateMethod);
            }

            Method runDelayedMethod = getMethod(regionScheduler.getClass(), "runDelayed", Plugin.class, Location.class, Consumer.class, long.class);
            if (runDelayedMethod != null) {
                cachedMethods.put("regionScheduler.runDelayed", runDelayedMethod);
            }
        }

        // Cache methods for entity scheduler
        Method getSchedulerMethod = getMethod(Entity.class, "getScheduler");
        if (getSchedulerMethod != null) {
            cachedMethods.put("entity.getScheduler", getSchedulerMethod);
        }

        Method executeEntityMethod = getMethod(Entity.class, "execute", Plugin.class, Runnable.class, Runnable.class, long.class);
        if (executeEntityMethod != null) {
            cachedMethods.put("entityScheduler.execute", executeEntityMethod);
        }

        Method runAtFixedRateEntityMethod = getMethod(Entity.class, "runAtFixedRate", Plugin.class, Consumer.class, Runnable.class, long.class, long.class);
        if (runAtFixedRateEntityMethod != null) {
            cachedMethods.put("entityScheduler.runAtFixedRate", runAtFixedRateEntityMethod);
        }

        // Cache method for Player teleportAsync
        Method teleportAsyncMethod = getMethod(Player.class, "teleportAsync", Location.class);
        if (teleportAsyncMethod != null) {
            cachedMethods.put("player.teleportAsync", teleportAsyncMethod);
        }

        Method teleportAsyncWithCause = getMethod(Player.class, "teleportAsync", Location.class, TeleportCause.class);
        if (teleportAsyncWithCause != null) {
            cachedMethods.put("player.teleportAsyncCause", teleportAsyncWithCause);
        }

        // Cache methods for asyncScheduler
        if (asyncScheduler != null) {
            Method cancelTasksMethod = getMethod(asyncScheduler.getClass(), "cancelTasks", Plugin.class);
            if (cancelTasksMethod != null) {
                cachedMethods.put("asyncScheduler.cancelTasks", cancelTasksMethod);
            }
        }
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

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return globalRegionScheduler != null && regionScheduler != null;
        } catch (Exception ig) {
            return false;
        }
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

public static void runTaskForEntity(Entity entity, Runnable run, Runnable retired, long delay) {
    if (entity == null) return;

    if (!isFolia()) {
        Bukkit.getScheduler().runTaskLater(FlamePearls.getInstance(), run, delay);
        return;
    }

    try {
        // 1. Get the scheduler: entity.getScheduler()
        Method getSchedulerMethod = entity.getClass().getMethod("getScheduler");
        Object entityScheduler = getSchedulerMethod.invoke(entity);

        // 2. Get the execute method: execute(Plugin, Runnable, Runnable, long)
        // Note: The last parameter MUST be long.class (primitive)
        Method executeMethod = entityScheduler.getClass().getMethod(
            "execute", 
            org.bukkit.plugin.Plugin.class, 
            Runnable.class, 
            Runnable.class, 
            long.class
        );

        // 3. Invoke
        executeMethod.invoke(
            entityScheduler, 
            FlamePearls.getInstance(), 
            run, 
            retired, 
            Math.max(1L, delay)
        );
    } catch (Exception e) {
        e.printStackTrace();
        // If reflection fails, run it immediately as a failsafe so the code doesn't "stuck"
        run.run(); 
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

    public static CompletableFuture<Boolean> teleportPlayer(Player e, Location location, Boolean async) {
        if (isFolia()) {
            CompletableFuture<Boolean> out = new CompletableFuture<>();
            runTaskForEntity(e, () -> {
                Method teleportAsyncWithCause = cachedMethods.get("player.teleportAsyncCause");
                if (teleportAsyncWithCause != null) {
                    Object res = invokeMethod(teleportAsyncWithCause, e, location, null);
                    if (res instanceof CompletableFuture) {
                        @SuppressWarnings("unchecked")
                        CompletableFuture<Boolean> f = (CompletableFuture<Boolean>) res;
                        f.whenComplete((ok, ex) -> {
                            if (ex != null) {
                                out.complete(false);
                            } else {
                                out.complete(Boolean.TRUE.equals(ok));
                            }
                        });
                        return;
                    }
                    out.complete(res != null);
                    return;
                }
                Method teleportAsyncMethod = cachedMethods.get("player.teleportAsync");
                if (teleportAsyncMethod != null) {
                    Object res = invokeMethod(teleportAsyncMethod, e, location);
                    if (res instanceof CompletableFuture) {
                        @SuppressWarnings("unchecked")
                        CompletableFuture<Boolean> f = (CompletableFuture<Boolean>) res;
                        f.whenComplete((ok, ex) -> {
                            if (ex != null) {
                                out.complete(false);
                            } else {
                                out.complete(Boolean.TRUE.equals(ok));
                            }
                        });
                        return;
                    }
                    out.complete(res != null);
                    return;
                }
                out.complete(e.teleport(location));
            }, () -> {}, 1L);
            return out;
        }
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
        e.teleport(location);
        return CompletableFuture.completedFuture(true);
    }

    public static CompletableFuture<Boolean> teleportPlayer(Player e, Location location, TeleportCause cause) {
        if (isFolia()) {
            CompletableFuture<Boolean> out = new CompletableFuture<>();
            runTaskForEntity(e, () -> {
                Method teleportAsyncWithCause = cachedMethods.get("player.teleportAsyncCause");
                if (teleportAsyncWithCause != null) {
                    Object res = invokeMethod(teleportAsyncWithCause, e, location, cause);
                    if (res instanceof CompletableFuture) {
                        @SuppressWarnings("unchecked")
                        CompletableFuture<Boolean> f = (CompletableFuture<Boolean>) res;
                        f.whenComplete((ok, ex) -> {
                            if (ex != null) {
                                out.complete(false);
                            } else {
                                out.complete(Boolean.TRUE.equals(ok));
                            }
                        });
                        return;
                    }
                    out.complete(res != null);
                    return;
                }
                Method teleportAsyncMethod = cachedMethods.get("player.teleportAsync");
                if (teleportAsyncMethod != null) {
                    Object res = invokeMethod(teleportAsyncMethod, e, location);
                    if (res instanceof CompletableFuture) {
                        @SuppressWarnings("unchecked")
                        CompletableFuture<Boolean> f = (CompletableFuture<Boolean>) res;
                        f.whenComplete((ok, ex) -> {
                            if (ex != null) {
                                out.complete(false);
                            } else {
                                out.complete(Boolean.TRUE.equals(ok));
                            }
                        });
                        return;
                    }
                    out.complete(res != null);
                    return;
                }
                if (cause != null) {
                    out.complete(e.teleport(location, cause));
                    return;
                }
                out.complete(e.teleport(location));
            }, () -> {}, 2L);
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
        e.teleport(location);
        return CompletableFuture.completedFuture(true);
    }

    public static void cancelAllTasks() {
        Plugin plugin = FlamePearls.getInstance();
        if (!isFolia()) {
            // Standard Bukkit/Spigot/Paper: cancel all tasks for the plugin
            bS.cancelTasks(plugin);
            return;
        }

        // 1. Cancel tasks on the GlobalRegionScheduler
        Method cancelGlobalMethod = cachedMethods.get("globalRegionScheduler.cancelTasks");
        invokeMethod(cancelGlobalMethod, globalRegionScheduler, plugin);

        // 2. Cancel tasks on the modern AsyncScheduler
        Method cancelAsyncMethod = cachedMethods.get("asyncScheduler.cancelTasks");
        invokeMethod(cancelAsyncMethod, asyncScheduler, plugin);
    }
}