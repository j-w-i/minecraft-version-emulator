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
package net.historicalexperience.client;

import java.util.List;
import net.historicalexperience.gameplay.HistoricalCreativeGrants;
import net.historicalexperience.gameplay.HistoricalCreativeRules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Own 8-column ×9-row single-page presentation. Native packets remain server-authoritative.
 * Runtime item icons only; no copied GUI assets or Minecraft implementation.
 */
public final class LegacyCreativeScreen extends AbstractContainerScreen<LegacyCreativeScreen.Picker> {
    private final String selection;
    private boolean scrolling;
    private boolean outsideClick;
    private static final int HOTBAR_Y = 185;
    public LegacyCreativeScreen(Player player, String selection) {
        super(new Picker(player, HistoricalCreativeInventory.entries(selection, player.registryAccess())), player.getInventory(), Component.literal("Item selection"));
        this.selection = selection;
        this.imageWidth = 176; this.imageHeight = 208;
        player.containerMenu = this.menu;
    }
    @Override protected void containerTick() {
        if (!minecraft.gameMode.hasInfiniteItems()) minecraft.setScreen(new InventoryScreen(minecraft.player));
        else if (!HistoricalCreativeRules.usesOldMenu(HistoricalCreativeInventory.selection()))
            minecraft.setScreen(new CreativeModeInventoryScreen(minecraft.player, minecraft.player.connection.enabledFeatures(), minecraft.options.operatorItemsTab().get()));
    }
    @Override public void removed() {
        super.removed();
        if (minecraft.player != null && minecraft.player.containerMenu == menu) minecraft.player.containerMenu = minecraft.player.inventoryMenu;
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xff404040, false);
    }
    @Override protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xff373737);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xffffffff);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xffc6c6c6);
        for (Slot slot : menu.slots) {
            int sx = x + slot.x, sy = y + slot.y;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xff373737);
            g.fill(sx, sy, sx + 17, sy + 17, 0xffffffff);
            g.fill(sx, sy, sx + 16, sy + 16, 0xff8b8b8b);
        }
        g.fill(x + 156, y + 18, x + 168, y + 179, 0xff373737);
        int thumb = y + 19 + (menu.maxRow() == 0 ? 0 : (int)(145.0 * menu.row / menu.maxRow()));
        g.fill(x + 157, thumb, x + 167, thumb + 14, menu.maxRow() > 0 ? 0xffa0a0a0 : 0xff666666);
        g.fill(x + 158, thumb + 1, x + 166, thumb + 13, 0xffc6c6c6);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta); renderTooltip(graphics, mouseX, mouseY);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        menu.scroll(menu.row - (int)Math.signum(vertical)); return true;
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        outsideClick = outsidePanel(x, y);
        // A carried stack clicked anywhere below the catalogue belongs to the hotbar slot under the
        // pointer. Without this, the band between the two grids - and the panel margin around the
        // hotbar - has no slot to match, so the click looks like it did nothing and the item stays on
        // the cursor until the pointer happens to land squarely on a painted hotbar box.
        if ((button == 0 || button == 1) && !hasShiftDown() && !menu.getCarried().isEmpty()
                && inHotbarBand(x, y)) {
            Slot slot = hotbarSlotFor(x);
            if (slot != null) { slotClicked(slot, slot.index, button, ClickType.PICKUP); return true; }
        }
        if (button == 0 && x >= leftPos + 156 && x < leftPos + 168 && y >= topPos + 18 && y < topPos + 179) {
            scrolling = true; scrollAt(y); return true;
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (scrolling) { scrollAt(y); return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        outsideClick = outsidePanel(x, y);
        scrolling = false; return super.mouseReleased(x, y, button);
    }
    private boolean outsidePanel(double x, double y) {
        return x < leftPos || x >= leftPos + imageWidth || y < topPos || y >= topPos + imageHeight;
    }
    /** The lowest painted row of the catalogue grid; everything below belongs to the hotbar band. */
    private int catalogBottom() {
        int bottom = 0;
        for (Slot slot : menu.slots) if (slot.container == menu.catalog) bottom = Math.max(bottom, slot.y + 16);
        return bottom;
    }
    /** Which hotbar column a click at this x belongs to: the nearest slot centre, clamped to nine slots. */
    private Slot hotbarSlotFor(double x) {
        int column = (int) Math.max(0, Math.min(8, Math.round((x - leftPos - 16.0) / 18.0)));
        for (Slot slot : menu.slots) {
            if (slot.container == minecraft.player.getInventory() && slot.getContainerSlot() == column) return slot;
        }
        return null;
    }
    /** The band from the catalogue's last painted row to the panel's bottom edge. */
    private boolean inHotbarBand(double x, double y) {
        return x >= leftPos && x < leftPos + imageWidth
            && y >= topPos + catalogBottom() && y < topPos + imageHeight;
    }
    @Override protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        // Native slot lookup delegates here. Give each hotbar slot the painted boxes, the narrow
        // separator above it and the margin below it, so hover and click agree on where the band is.
        if (width == 16 && height == 16 && y == catalogBottom() - 16 && x >= 8 && x <= 134 && (x - 8) % 18 == 0
                && mouseY >= topPos + catalogBottom()) {
            // The bottom catalogue row stops at its painted interior: the separator row under it belongs
            // to the hotbar, exactly as the click routing above decides. Otherwise the highlight would
            // name one slot while the click filled another.
            return false;
        }
        if (y == HOTBAR_Y && width == 16 && height == 16 && x >= 8 && x <= 152 && (x - 8) % 18 == 0)
            return mouseX >= leftPos + x - 1 && mouseX < leftPos + x + 17
                && mouseY >= topPos + catalogBottom() && mouseY < topPos + imageHeight;
        return super.isHovering(x, y, width, height, mouseX, mouseY);
    }
    private void scrollAt(double y) { menu.scroll((int)Math.round((y - topPos - 25) / 145.0 * menu.maxRow())); }
    @Override protected void slotClicked(Slot slot, int slotId, int button, ClickType type) {
        if (slot != null && slot.container == menu.catalog) {
            // The old picker treats an ordinary catalog click with a held stack as discard,
            // even over a different item. Do not replace it or manufacture another grant/drop.
            if (!menu.getCarried().isEmpty() && type != ClickType.SWAP && type != ClickType.THROW && type != ClickType.CLONE) {
                menu.setCarried(ItemStack.EMPTY); return;
            }
            ItemStack chosen = slot.getItem();
            if (chosen.isEmpty()) { menu.setCarried(ItemStack.EMPTY); return; }
            if (type == ClickType.SWAP && button >= 0 && button < 9) {
                setHotbar(button, chosen.copyWithCount(chosen.getMaxStackSize())); return;
            }
            if (type == ClickType.THROW) {
                minecraft.gameMode.handleCreativeModeItemDrop(chosen.copyWithCount(button == 0 ? 1 : chosen.getMaxStackSize())); return;
            }
            int count = type == ClickType.CLONE || type == ClickType.QUICK_MOVE ? chosen.getMaxStackSize() : 1;
            menu.setCarried(chosen.copyWithCount(count)); return;
        }
        if (slotId == -999 && !menu.getCarried().isEmpty()) {
            if (!outsideClick) return; // Null-slot padding inside the panel is not the outside world.
            ItemStack held = menu.getCarried();
            int count = button == 0 ? held.getCount() : 1;
            minecraft.gameMode.handleCreativeModeItemDrop(held.copyWithCount(count));
            held.shrink(count); return;
        }
        if (slot != null && slot.container == minecraft.player.getInventory() && type == ClickType.QUICK_MOVE) {
            setHotbar(slot.getContainerSlot(), ItemStack.EMPTY); return;
        }
        if (slot != null && !HistoricalCreativeGrants.allowed(selection, slot.getItem())) return;
        if (!HistoricalCreativeGrants.allowed(selection, menu.getCarried())) return;
        ItemStack[] before = new ItemStack[9];
        for (int i = 0; i < 9; i++) before[i] = minecraft.player.getInventory().getItem(i).copy();
        menu.clicked(slotId, button, type, minecraft.player);
        for (int i = 0; i < 9; i++) if (!ItemStack.matches(before[i], minecraft.player.getInventory().getItem(i)))
            minecraft.gameMode.handleCreativeModeItemAdd(minecraft.player.getInventory().getItem(i).copy(), 36 + i);
    }
    private void setHotbar(int index, ItemStack stack) {
        if (!HistoricalCreativeGrants.allowed(selection, minecraft.player.getInventory().getItem(index))) return;
        minecraft.player.getInventory().setItem(index, stack);
        minecraft.gameMode.handleCreativeModeItemAdd(stack.copy(), 36 + index);
    }
    public static final class Picker extends AbstractContainerMenu {
        public final SimpleContainer catalog = new SimpleContainer(72);
        private final Player player;
        private final List<ItemStack> entries;
        private int row;
        Picker(Player player, List<ItemStack> entries) {
            super(null, 0); this.player = player; this.entries = List.copyOf(entries);
            for (int r = 0; r < 9; r++) for (int c = 0; c < 8; c++) addSlot(new Slot(catalog, r * 8 + c, 8 + c * 18, 18 + r * 18) {
                @Override public boolean mayPickup(Player who) { return false; }
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
            for (int c = 0; c < 9; c++) addSlot(new Slot(player.getInventory(), c, 8 + c * 18, 185));
            scroll(0);
        }
        public int maxRow() { return Math.max(0, (entries.size() + 7) / 8 - 9); }
        public void scroll(int requested) {
            row = Math.max(0, Math.min(maxRow(), requested));
            for (int i = 0; i < 72; i++) catalog.setItem(i, row * 8 + i < entries.size() ? entries.get(row * 8 + i).copy() : ItemStack.EMPTY);
        }
        @Override public ItemStack quickMoveStack(Player who, int index) { return ItemStack.EMPTY; }
        @Override public boolean stillValid(Player who) { return who.getAbilities().instabuild; }
        @Override public boolean canDragTo(Slot slot) { return slot.container != catalog; }
        @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot.container != catalog; }
        @Override public ItemStack getCarried() { return player.inventoryMenu.getCarried(); }
        @Override public void setCarried(ItemStack stack) { player.inventoryMenu.setCarried(stack); }
        @Override public void removed(Player who) { setCarried(ItemStack.EMPTY); }
    }
}
