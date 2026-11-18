package com.forgebound.disassembly;

import com.forgebound.content.ModContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

import static com.forgebound.disassembly.RuinforgedCrucibleBlockEntity.*;

/** The Crucible's furnace-style menu: gear on top, coal/charcoal below, the recovered materials in a 3x2 grid. */
public class CrucibleMenu extends AbstractContainerMenu {
    public static final int INV_Y = 108;
    public static final int OUT_X = 108, OUT_Y = 26;
    private final Container container;
    private final ContainerData data;

    /** Client side. */
    public CrucibleMenu(int id, Inventory inv) {
        this(id, inv, new SimpleContainer(SIZE), new SimpleContainerData(D_COUNT));
    }

    public CrucibleMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(ModContent.CRUCIBLE_MENU.get(), id);
        checkContainerSize(container, SIZE);
        checkContainerDataCount(data, D_COUNT);
        this.container = container;
        this.data = data;
        container.startOpen(inv.player);
        addSlot(new Slot(container, INPUT, 40, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return CoreMaterials.isGear(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(container, FUEL, 40, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isFuel(stack);
            }
        });
        for (int i = 0; i < OUTPUTS; i++) {
            addSlot(new Slot(container, OUT_FIRST + i, OUT_X + (i % 3) * 18, OUT_Y + (i / 3) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, INV_Y + 58));
        addDataSlots(data);
    }

    public float burnFraction() {
        int total = data.get(D_BURN_TOTAL);
        return total <= 0 ? 0 : (float) data.get(D_BURN) / total;
    }

    public float progressFraction() {
        return (float) data.get(D_PROGRESS) / MELT_TICKS;
    }

    public boolean lit() {
        return data.get(D_BURN) > 0;
    }

    public int status() {
        return data.get(D_STATUS);
    }

    public record Preview(Item item, double expected) {
    }

    /** What the gear in the input will give back (expected amounts). */
    public List<Preview> preview() {
        List<Preview> out = new ArrayList<>();
        for (int i = 0; i < PREVIEW; i++) {
            int lo = data.get(D_PARTS + i * 3), hi = data.get(D_PARTS + i * 3 + 1);
            if (lo <= 0) break;
            Item item = BuiltInRegistries.ITEM.byId(((hi << 15) | (lo - 1)));
            if (item != Items.AIR) out.add(new Preview(item, data.get(D_PARTS + i * 3 + 2) / 100.0));
        }
        return out;
    }

    public ItemStack input() {
        return slots.get(INPUT).getItem();
    }

    public boolean hasFuel() {
        return !slots.get(FUEL).getItem().isEmpty();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        int invStart = SIZE, invEnd = slots.size();
        if (index < invStart) {
            if (!moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);
        } else if (isFuel(stack)) {
            if (!moveItemStackTo(stack, FUEL, FUEL + 1, false)) return ItemStack.EMPTY;
        } else if (CoreMaterials.isGear(stack)) {
            if (!moveItemStackTo(stack, INPUT, INPUT + 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
