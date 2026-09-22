package com.arkflame.flamepearls.listeners;

import com.arkflame.flamepearls.compat.cooldown.UnsupportedPearlCooldownBridge;
import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.config.MessagesConfigHolder;
import com.arkflame.flamepearls.managers.CooldownManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

// CONTRACT-REGRESSION: cancelled offhand pearl use must never copy pearl state into selected main-hand hotbar slot.
// CONTRACT-FROZEN-T1: offhand EquipmentSlot.OFF_HAND pearl denial must not perform inventory writes and must deny item use.
// ADMISSION: this test class exists solely to prevent re-introduction of the cross-slot inventory copy bug
//   where a denied offhand pearl interaction would copy pearl state into the selected hotbar slot,
//   effectively duplicating pearls or corrupting the main-hand slot. All assertions are executable
//   listener behavior, not source-text checks.

public final class PlayerInteractListenerInventorySafetyTest {

    @Test
    public void cooldownDeniedOffHandPearlPerformsNoInventoryWrite() {
        Fixture fixture = newFixture(false);
        fixture.cooldownManager.updateLastPearl(fixture.player);
        PlayerInteractEvent event = new PlayerInteractEvent(
                fixture.player,
                Action.RIGHT_CLICK_AIR,
                new ItemStack(Material.ENDER_PEARL, 16),
                null,
                BlockFace.SELF,
                EquipmentSlot.OFF_HAND);
        fixture.listener.onPlayerInteract(event);
        assertEquals(0, fixture.inventoryWriteCount.get());
    }

    @Test
    public void cooldownDeniedOffHandPearlStillDeniesItemUse() {
        Fixture fixture = newFixture(false);
        fixture.cooldownManager.updateLastPearl(fixture.player);
        PlayerInteractEvent event = new PlayerInteractEvent(
                fixture.player,
                Action.RIGHT_CLICK_AIR,
                new ItemStack(Material.ENDER_PEARL, 16),
                null,
                BlockFace.SELF,
                EquipmentSlot.OFF_HAND);
        fixture.listener.onPlayerInteract(event);
        assertEquals(Event.Result.DENY, event.useItemInHand());
    }

    @Test
    public void clickedBlockPreventionOffHandPearlPerformsNoInventoryWrite() {
        Fixture fixture = newFixture(true);
        PlayerInteractEvent event = new PlayerInteractEvent(
                fixture.player,
                Action.RIGHT_CLICK_BLOCK,
                new ItemStack(Material.ENDER_PEARL, 16),
                null,
                BlockFace.UP,
                EquipmentSlot.OFF_HAND);
        fixture.listener.onPlayerInteract(event);
        assertEquals(0, fixture.inventoryWriteCount.get());
    }

    @Test
    public void cooldownDeniedMainHandPearlDeniesItemUse() {
        Fixture fixture = newFixture(false);
        fixture.cooldownManager.updateLastPearl(fixture.player);
        PlayerInteractEvent event = new PlayerInteractEvent(
                fixture.player,
                Action.RIGHT_CLICK_AIR,
                new ItemStack(Material.ENDER_PEARL, 16),
                null,
                BlockFace.SELF,
                EquipmentSlot.HAND);
        fixture.listener.onPlayerInteract(event);
        assertEquals(Event.Result.DENY, event.useItemInHand());
    }

    @Test
    public void cooldownDeniedPearlCancelsInteraction() {
        Fixture fixture = newFixture(false);
        fixture.cooldownManager.updateLastPearl(fixture.player);
        PlayerInteractEvent event = new PlayerInteractEvent(
                fixture.player,
                Action.RIGHT_CLICK_AIR,
                new ItemStack(Material.ENDER_PEARL, 16),
                null,
                BlockFace.SELF,
                EquipmentSlot.HAND);
        fixture.listener.onPlayerInteract(event);
        assertEquals(true, event.isCancelled());
    }

    @Test
    public void cooldownListenerRunsAtHighestPriority() throws NoSuchMethodException {
        Method method = PlayerInteractListener.class.getDeclaredMethod("onPlayerInteract", PlayerInteractEvent.class);
        EventHandler eventHandler = method.getAnnotation(EventHandler.class);
        assertEquals(EventPriority.HIGHEST, eventHandler.priority());
    }

    @Test
    public void cooldownDeniedRightClickBlockOverridesAllowedItemUseToDeny() {
        Fixture fixture = newFixture(false);
        fixture.cooldownManager.updateLastPearl(fixture.player);
        PlayerInteractEvent event = new PlayerInteractEvent(
                fixture.player,
                Action.RIGHT_CLICK_BLOCK,
                new ItemStack(Material.ENDER_PEARL, 16),
                null,
                BlockFace.UP,
                EquipmentSlot.HAND);
        fixture.listener.onPlayerInteract(event);
        assertEquals(Event.Result.DENY, event.useItemInHand());
    }

    // -- fixture --

    private static final class Fixture {
        final Player player;
        final CooldownManager cooldownManager;
        final PlayerInteractListener listener;
        final AtomicInteger inventoryWriteCount;

        Fixture(Player player, CooldownManager cooldownManager, PlayerInteractListener listener, AtomicInteger inventoryWriteCount) {
            this.player = player;
            this.cooldownManager = cooldownManager;
            this.listener = listener;
            this.inventoryWriteCount = inventoryWriteCount;
        }
    }

    private static Fixture newFixture(boolean preventPearlOnClickBlock) {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("cooldown.enabled", true);
        config.set("cooldown.time", 10.0D);
        config.set("prevent-pearl-on-click-block", preventPearlOnClickBlock);

        GeneralConfigHolder generalConfigHolder = new GeneralConfigHolder();
        generalConfigHolder.load(config);

        MemoryConfiguration messagesConfig = new MemoryConfiguration();
        messagesConfig.set("messages.cooldown", "&cCooldown {time}s");
        MessagesConfigHolder messagesConfigHolder = new MessagesConfigHolder();
        messagesConfigHolder.load(messagesConfig);

        UnsupportedPearlCooldownBridge bridge = new UnsupportedPearlCooldownBridge();
        CooldownManager cooldownManager = new CooldownManager(generalConfigHolder, bridge);

        AtomicInteger inventoryWriteCount = new AtomicInteger(0);
        World world = newWorldProxy();
        PlayerInventory inventory = newInventoryProxy(inventoryWriteCount);
        Player player = newPlayerProxy(inventory, world);

        PlayerInteractListener listener = new PlayerInteractListener(cooldownManager, messagesConfigHolder, generalConfigHolder);

        return new Fixture(player, cooldownManager, listener, inventoryWriteCount);
    }

    private static World newWorldProxy() {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if ("getName".equals(name)) {
                    return "world";
                }
                if ("hashCode".equals(name)) {
                    return System.identityHashCode(proxy);
                }
                if ("equals".equals(name)) {
                    return proxy == args[0];
                }
                if ("toString".equals(name)) {
                    return "WorldProxy[world]";
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class}, handler);
    }

    private static PlayerInventory newInventoryProxy(final AtomicInteger writeCount) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if ("getHeldItemSlot".equals(name)) {
                    return 4;
                }
                if ("setItem".equals(name) || "setItemInMainHand".equals(name) || "setItemInOffHand".equals(name) || "setHeldItemSlot".equals(name) || "setItemInHand".equals(name)) {
                    writeCount.incrementAndGet();
                    return null;
                }
                if ("hashCode".equals(name)) {
                    return System.identityHashCode(proxy);
                }
                if ("equals".equals(name)) {
                    return proxy == args[0];
                }
                if ("toString".equals(name)) {
                    return "PlayerInventoryProxy";
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class<?>[]{PlayerInventory.class}, handler);
    }

    private static Player newPlayerProxy(final PlayerInventory inventory, final World world) {
        final UUID uuid = UUID.randomUUID();
        final Location location = new Location(world, 0.0D, 64.0D, 0.0D);
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if ("getInventory".equals(name)) {
                    return inventory;
                }
                if ("getLocation".equals(name)) {
                    return location;
                }
                if ("getWorld".equals(name)) {
                    return world;
                }
                if ("getUniqueId".equals(name)) {
                    return uuid;
                }
                if ("sendMessage".equals(name)) {
                    return null;
                }
                if ("hashCode".equals(name)) {
                    return System.identityHashCode(proxy);
                }
                if ("equals".equals(name)) {
                    return proxy == args[0];
                }
                if ("toString".equals(name)) {
                    return "PlayerProxy[" + uuid + "]";
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class}, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == double.class) {
            return 0.0d;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == void.class) {
            return null;
        }
        return null;
    }
}
