package dev.lumen.client.modules.qol;

import dev.lumen.client.LumenClient;
import dev.lumen.client.core.Module;
import dev.lumen.client.core.Category;
import dev.lumen.client.core.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Middle-click inventory sorting. Queues deterministic swap click sequences
 * executed over several ticks to stay server friendly.
 */
public class InventorySortModule extends Module {
    public final BooleanSetting sortByRarity = new BooleanSetting("Sort By Rarity", false);

    private final List<int[]> pendingSwaps = new ArrayList<>();
    private int clickPhase;
    private int slotA;
    private int slotB;
    private boolean middleWasDown;

    public InventorySortModule() {
        super("Inventory Sort", "Middle-click to sort your inventory", Category.QOL);
        addSettings(sortByRarity);
    }

    public boolean hasPendingSwaps() {
        return !pendingSwaps.isEmpty();
    }

    public void requestSort(int[] playerInvSlots) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) return;
        List<ItemStack> stacks = new ArrayList<>(playerInvSlots.length);
        for (int slot : playerInvSlots) {
            stacks.add(screen.getMenu().getSlot(slot).getItem());
        }
        Comparator<ItemStack> comparator = sortByRarity.get()
                ? Comparator.comparing((ItemStack s) -> -s.getItem().getDefaultMaxStackSize()).thenComparing(s -> s.getHoverName().getString())
                : Comparator.comparing((ItemStack s) -> s.isEmpty()).thenComparing(s -> s.getHoverName().getString());
        Integer[] order = new Integer[stacks.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> comparator.compare(stacks.get(a), stacks.get(b)));

        pendingSwaps.clear();
        for (int target = 0; target < order.length; target++) {
            int source = order[target];
            if (source != target) {
                pendingSwaps.add(new int[]{playerInvSlots[source], playerInvSlots[target]});
                int tmp = order[source];
                order[source] = order[target];
                order[target] = tmp;
            }
        }
    }

    @Override
    public void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AbstractContainerScreen<?> container && mc.mouseHandler.isMiddlePressed()) {
            if (!middleWasDown && pendingSwaps.isEmpty()) {
                requestSort(collectPlayerSlots(container));
            }
            middleWasDown = true;
        } else {
            middleWasDown = false;
        }

        if (pendingSwaps.isEmpty() || !(mc.screen instanceof AbstractContainerScreen<?> screen) || mc.gameMode == null) {
            pendingSwaps.clear();
            clickPhase = 0;
            return;
        }
        if (clickPhase == 0) {
            int[] swap = pendingSwaps.get(0);
            slotA = swap[0];
            slotB = swap[1];
            mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, slotA, 0, ClickType.PICKUP, mc.player);
            clickPhase = 1;
        } else if (clickPhase == 1) {
            mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, slotB, 0, ClickType.PICKUP, mc.player);
            clickPhase = 2;
        } else {
            mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, slotA, 0, ClickType.PICKUP, mc.player);
            pendingSwaps.remove(0);
            clickPhase = 0;
        }
    }

    private int[] collectPlayerSlots(AbstractContainerScreen<?> screen) {
        Minecraft mc = Minecraft.getInstance();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < screen.getMenu().slots.size(); i++) {
            if (screen.getMenu().slots.get(i).container == mc.player.getInventory()) {
                slots.add(i);
            }
        }
        int[] result = new int[slots.size()];
        for (int i = 0; i < result.length; i++) result[i] = slots.get(i);
        return result;
    }
}
