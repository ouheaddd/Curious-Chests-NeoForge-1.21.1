package com.overyourhead.curiouschests.common.blockentity;

import com.overyourhead.curiouschests.common.block.AbstractSpecialChestBlock;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.chest.ChestRules;
import com.overyourhead.curiouschests.common.chest.archivist.ArchivistLogic;
import com.overyourhead.curiouschests.common.chest.archivist.ArchivistBookRuntime;
import com.overyourhead.curiouschests.common.chest.builders.BuilderSupplyLogic;
import com.overyourhead.curiouschests.common.chest.collectors.CollectorLogic;
import com.overyourhead.curiouschests.common.chest.enderdispatch.DispatchChestRuntime;
import com.overyourhead.curiouschests.common.chest.infernal.InfernalLogic;
import com.overyourhead.curiouschests.common.chest.infernal.InfernalClientEffects;
import com.overyourhead.curiouschests.common.chest.resonant.ResonanceLogic;
import com.overyourhead.curiouschests.common.chest.resonant.ResonanceChestRuntime;
import com.overyourhead.curiouschests.common.chest.sentinel.SentinelChestRuntime;
import com.overyourhead.curiouschests.common.chest.witch.WitchLogic;
import com.overyourhead.curiouschests.common.chest.witch.WitchChestRuntime;
import com.overyourhead.curiouschests.common.chest.trapper.TrapperLogic;
import com.overyourhead.curiouschests.common.chest.trapper.TrapperChestRuntime;
import com.overyourhead.curiouschests.common.menu.SpecialChestMenu;
import com.overyourhead.curiouschests.common.sentinel.SentinelIntrusionType;
import com.overyourhead.curiouschests.common.sentinel.SentinelLogEntry;
import com.overyourhead.curiouschests.common.chest.bottomless.BottomlessStorage;
import com.overyourhead.curiouschests.common.chest.bottomless.BottomlessDisplayRuntime;
import com.overyourhead.curiouschests.common.chest.shared.ChestSounds;
import com.overyourhead.curiouschests.common.chest.shared.ChestItemHandlers;
import com.overyourhead.curiouschests.core.ModBlockEntities;
import com.overyourhead.curiouschests.core.ModDataComponents;
import com.overyourhead.curiouschests.core.ModItems;
import com.overyourhead.curiouschests.core.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SpecialChestBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, LidBlockEntity {
    private static final int EVENT_SET_OPEN_COUNT = 1;
    private static final String BOTTOMLESS_DEEP_FORMAT_TAG = "BottomlessDeepFormat";

    private final ChestLidController lidController = new ChestLidController();
    private final TrapperChestRuntime trapperRuntime = new TrapperChestRuntime(this);
    private final WitchChestRuntime witchRuntime = new WitchChestRuntime(this);
    private final SentinelChestRuntime sentinelRuntime = new SentinelChestRuntime(this);
    private final DispatchChestRuntime dispatchRuntime = new DispatchChestRuntime(this);
    private final ArchivistBookRuntime archivistBookRuntime = new ArchivistBookRuntime(this);
    private final BottomlessDisplayRuntime bottomlessDisplayRuntime = new BottomlessDisplayRuntime(this);
    private final ResonanceChestRuntime resonanceRuntime = new ResonanceChestRuntime(this);
    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            ChestSounds.playBase(level, pos, SoundEvents.CHEST_OPEN);
            ChestSounds.playAccent(level, pos, SpecialChestBlockEntity.this.kind(), true);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            ChestSounds.playBase(level, pos, SoundEvents.CHEST_CLOSE);
            ChestSounds.playAccent(level, pos, SpecialChestBlockEntity.this.kind(), false);
        }

        @Override
        protected void openerCountChanged(
                Level level,
                BlockPos pos,
                BlockState state,
                int oldCount,
                int newCount
        ) {
            SpecialChestBlockEntity.this.signalOpenCount(level, pos, state, newCount);
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof SpecialChestMenu menu
                    && menu.blockEntity() == SpecialChestBlockEntity.this;
        }
    };

    private NonNullList<ItemStack> items;
    private final int[] sidedSlots;
    private int workTicker;

    private final ChestItemHandlers itemHandlers = new ChestItemHandlers(this);

    private final Map<UUID, BuilderSupplyLogic.HeldSnapshot> builderSnapshots = new HashMap<>();


    public SpecialChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPECIAL_CHEST.get(), pos, state);
        ChestKind initialKind = kindFromState(state);
        items = NonNullList.withSize(initialKind.slots(), ItemStack.EMPTY);
        if (initialKind == ChestKind.SCULK_SENTINEL || initialKind == ChestKind.TRAPPER) {
            sidedSlots = new int[0];
        } else if (initialKind == ChestKind.RESONANT) {
            sidedSlots = java.util.stream.IntStream.range(0, ResonanceLogic.STORAGE_SLOTS).toArray();
        } else if (initialKind == ChestKind.ARCHIVIST) {
            sidedSlots = new int[]{ArchivistLogic.INPUT_SLOT};
        } else {
            sidedSlots = java.util.stream.IntStream.range(0, initialKind.slots()).toArray();
        }
    }

    public ChestKind kind() {
        return kindFromState(getBlockState());
    }

    @Override
    public void startOpen(Player player) {
        if (!isRemoved() && !player.isSpectator() && level != null) {
            openersCounter.incrementOpeners(player, level, worldPosition, getBlockState());
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!isRemoved() && !player.isSpectator() && level != null) {
            openersCounter.decrementOpeners(player, level, worldPosition, getBlockState());
        }
    }

    public void recheckOpen() {
        if (!isRemoved() && level != null) {
            openersCounter.recheckOpeners(level, worldPosition, getBlockState());
        }
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == EVENT_SET_OPEN_COUNT) {
            lidController.shouldBeOpen(type > 0);
            return true;
        }
        if (witchRuntime.handleBlockEvent(id, type)) return true;
        return super.triggerEvent(id, type);
    }

    @Override
    public float getOpenNess(float partialTick) {
        return lidController.getOpenness(partialTick);
    }

    private void signalOpenCount(Level level, BlockPos pos, BlockState state, int openerCount) {
        level.blockEvent(pos, state.getBlock(), EVENT_SET_OPEN_COUNT, openerCount);
    }


    public ItemStack getStorageDisplayItem() {
        return bottomlessDisplayRuntime.item();
    }

    public boolean setStorageDisplayItem(Player player, ItemStack heldStack) {
        return bottomlessDisplayRuntime.set(player, heldStack);
    }

    public boolean removeStorageDisplayItem(Player player) {
        return bottomlessDisplayRuntime.remove(player);
    }

    public IItemHandler getItemHandler() {
        return itemHandlers.forKind(kind());
    }

    private static ChestKind kindFromState(BlockState state) {
        return state.getBlock() instanceof AbstractSpecialChestBlock chest
                ? chest.kind()
                : ChestKind.COLLECTORS;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (kind() == ChestKind.ENDER_DISPATCH) dispatchRuntime.onOwnerChanged();
    }

    public ItemStack getDispatchPreviewStack() {
        return dispatchRuntime.previewStack();
    }


    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        if (kind() == ChestKind.ENDER_DISPATCH) {
            CompoundTag tag = new CompoundTag();
            dispatchRuntime.writeClientTag(tag, registries);
            return tag;
        }
        if (kind() == ChestKind.BOTTOMLESS) {
            CompoundTag tag = new CompoundTag();
            bottomlessDisplayRuntime.write(tag, registries);
            return tag;
        }
        if (kind() == ChestKind.SCULK_SENTINEL) {
            CompoundTag tag = new CompoundTag();
            sentinelRuntime.writeClientTag(tag);
            return tag;
        }
        if (kind() == ChestKind.WITCH) {
            CompoundTag tag = new CompoundTag();
            witchRuntime.writeClientTag(tag);
            return tag;
        }
        if (kind() == ChestKind.TRAPPER) {
            CompoundTag tag = new CompoundTag();
            trapperRuntime.writeClientTag(tag);
            return tag;
        }
        return super.getUpdateTag(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        if (kind() == ChestKind.ENDER_DISPATCH
                || kind() == ChestKind.BOTTOMLESS
                || kind() == ChestKind.SCULK_SENTINEL
                || kind() == ChestKind.WITCH
                || kind() == ChestKind.TRAPPER) {
            return ClientboundBlockEntityDataPacket.create(this);
        }
        return super.getUpdatePacket();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        if (kind() == ChestKind.ENDER_DISPATCH) {
            dispatchRuntime.readClientTag(tag, registries);
            return;
        }
        if (kind() == ChestKind.BOTTOMLESS) {
            bottomlessDisplayRuntime.read(tag, registries);
            return;
        }
        if (kind() == ChestKind.SCULK_SENTINEL) {
            sentinelRuntime.readClientTag(tag);
            return;
        }
        if (kind() == ChestKind.WITCH) {
            witchRuntime.readClientTag(tag);
            return;
        }
        if (kind() == ChestKind.TRAPPER) {
            trapperRuntime.readClientTag(tag);
            return;
        }
        loadWithComponents(tag, registries);
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries
    ) {
        if (kind() == ChestKind.ENDER_DISPATCH) {
            dispatchRuntime.readClientTag(packet.getTag(), registries);
            return;
        }
        if (kind() == ChestKind.BOTTOMLESS) {
            bottomlessDisplayRuntime.read(packet.getTag(), registries);
            return;
        }
        if (kind() == ChestKind.SCULK_SENTINEL) {
            sentinelRuntime.readClientTag(packet.getTag());
            return;
        }
        if (kind() == ChestKind.WITCH) {
            witchRuntime.readClientTag(packet.getTag());
            return;
        }
        if (kind() == ChestKind.TRAPPER) {
            trapperRuntime.readClientTag(packet.getTag());
            return;
        }
        loadWithComponents(packet.getTag(), registries);
    }

    @Override
    public int getContainerSize() {
        return kind().slots();
    }

    @Override
    public int getMaxStackSize() {
        return switch (kind()) {
            case BOTTOMLESS -> BottomlessStorage.ABSOLUTE_SLOT_LIMIT;
            case ARCHIVIST -> ArchivistLogic.MAX_BOOKS_PER_ENTRY;
            case WITCH -> 64;
            default -> super.getMaxStackSize();
        };
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        if (kind() == ChestKind.BOTTOMLESS) return BottomlessStorage.maxPerSlot(stack);
        if (kind() == ChestKind.ARCHIVIST && ArchivistLogic.isStorableBook(stack)) {
            return ArchivistLogic.MAX_BOOKS_PER_ENTRY;
        }
        if (kind() == ChestKind.WITCH && WitchLogic.isSupported(stack)) {
            return WitchLogic.maxPerSlot(stack);
        }
        return super.getMaxStackSize(stack);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
        if (kind() == ChestKind.WITCH) {
            witchRuntime.onItemsReplaced();
        }
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (kind() == ChestKind.BOTTOMLESS) {
            ItemStack stored = stack;
            int limit = BottomlessStorage.maxPerSlot(stack);
            if (!stack.isEmpty() && stack.getCount() > limit) {
                stored = stack.copyWithCount(limit);
            }
            items.set(slot, stored);
            setChanged();
            return;
        }

        if (kind() == ChestKind.RESONANT && slot == ResonanceLogic.CRYSTAL_SLOT && !stack.isEmpty()) {
            stack = stack.copyWithCount(1);
        }

        if (kind() == ChestKind.ARCHIVIST) {
            ItemStack stored = stack;
            if (slot == ArchivistLogic.INPUT_SLOT && !stack.isEmpty()) {
                stored = stack.copyWithCount(1);
            } else if (slot >= 0 && slot < ArchivistLogic.STORAGE_SLOTS
                    && !stack.isEmpty()
                    && stack.getCount() > ArchivistLogic.MAX_BOOKS_PER_ENTRY) {
                stored = stack.copyWithCount(ArchivistLogic.MAX_BOOKS_PER_ENTRY);
            }
            items.set(slot, stored);
            setChanged();
            return;
        }

        if (kind() == ChestKind.WITCH) {
            ItemStack stored = stack;
            if (!stack.isEmpty() && stack.getCount() > WitchLogic.maxPerSlot(stack)) {
                stored = stack.copyWithCount(WitchLogic.maxPerSlot(stack));
            }
            items.set(slot, stored);
            setChanged();
            return;
        }

        super.setItem(slot, stack);
        if (kind() == ChestKind.RESONANT) {
            onResonanceSlotEdited(slot);
        }
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = super.removeItem(slot, amount);
        if (!removed.isEmpty() && kind() == ChestKind.RESONANT) {
            onResonanceSlotEdited(slot);
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = super.removeItemNoUpdate(slot);
        if (!removed.isEmpty() && kind() == ChestKind.RESONANT) {
            onResonanceSlotEdited(slot);
        }
        return removed;
    }

    private void onResonanceSlotEdited(int slot) {
        resonanceRuntime.onSlotEdited(slot);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (kind() == ChestKind.INFERNAL && slot >= InfernalLogic.OUTPUT_START) return false;
        if (kind() == ChestKind.RESONANT) {
            if (slot == ResonanceLogic.CRYSTAL_SLOT) {
                return stack.is(ModItems.RESONANCE_CRYSTAL.get());
            }
            if (slot >= 0 && slot < ResonanceLogic.STORAGE_SLOTS
                    && stack.is(ModItems.RESONANCE_CRYSTAL.get())) {
                return false;
            }
        }
        if (kind() == ChestKind.ARCHIVIST) {
            if (slot == ArchivistLogic.INPUT_SLOT) {
                return ArchivistLogic.isProcessableBook(stack);
            }
            return slot >= 0
                    && slot < ArchivistLogic.STORAGE_SLOTS
                    && ArchivistLogic.isStorableBook(stack);
        }
        if (kind() == ChestKind.WITCH) {
            return slot >= 0
                    && slot < WitchLogic.STORAGE_SLOTS
                    && WitchLogic.isSupported(stack);
        }
        return ChestRules.canStore(stack) && super.canPlaceItem(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return sidedSlots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return kind() != ChestKind.SCULK_SENTINEL && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        if (kind() == ChestKind.INFERNAL) {
            return slot >= InfernalLogic.OUTPUT_START && slot < InfernalLogic.OUTPUT_END;
        }
        return kind() != ChestKind.SCULK_SENTINEL
                && kind() != ChestKind.ARCHIVIST;
    }

    @Override
    protected Component getDefaultName() {
        return kind().title();
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return SpecialChestMenu.server(
                ModMenus.forKind(kind()),
                containerId,
                inventory,
                this,
                kind(),
                this::stillValid
        );
    }

    public void loadFromPlacedStack(ItemStack stack) {
        // Keep compatibility with chest items created by older versions: their stored
        // inventory can still be restored once when placed. Resonant identity itself
        // is intentionally never restored from an item anymore; a placed chest is a
        // new network node and must be attuned again with a crystal.
        applyComponentsFromItemStack(stack);
        if (kind() == ChestKind.RESONANT) {
            resonanceRuntime.resetForPlacedChest();
        }
        if (kind() == ChestKind.TRAPPER) {
            trapperRuntime.loadFromPlacedStack(stack);
        }
        setChanged();
    }

    public void ensureResonanceInitialized() {
        resonanceRuntime.ensureInitialized();
    }

    /**
     * Drops every real stored item into the world and clears the block inventory.
     * Internal oversized stacks (Storage, Archivist, Witch) are split back into
     * legal vanilla-sized ItemStacks before spawning, so no hidden container data is
     * needed on the dropped chest item.
     */
    public void dropStoredContents(Level level, BlockPos pos) {
        if (level.isClientSide) return;

        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stored = items.get(slot);
            if (stored.isEmpty()) continue;

            int remaining = stored.getCount();
            int legalStackSize = Math.max(1, stored.getMaxStackSize());
            while (remaining > 0) {
                int amount = Math.min(legalStackSize, remaining);
                Containers.dropItemStack(
                        level,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        stored.copyWithCount(amount)
                );
                remaining -= amount;
            }
            items.set(slot, ItemStack.EMPTY);
        }

        bottomlessDisplayRuntime.drop(level, pos);

        if (kind() == ChestKind.TRAPPER && level instanceof ServerLevel serverLevel) {
            trapperRuntime.handleBrokenContents(serverLevel, pos);
        }

        resonanceRuntime.clearReceivedSlots();
        if (kind() == ChestKind.ENDER_DISPATCH) dispatchRuntime.clearPreview(false);
        setChanged();
    }

    public UUID getResonanceNodeId() {
        return resonanceRuntime.nodeId();
    }

    public boolean advanceResonanceAttunement() {
        return resonanceRuntime.advanceAttunement();
    }

    public void resetResonanceAttunement() {
        resonanceRuntime.resetAttunement();
    }

    public void setResonanceCrystalInternal(ItemStack crystal) {
        resonanceRuntime.setCrystalInternal(items, crystal);
    }

    public boolean tickResonanceTransferCooldown() {
        return resonanceRuntime.tickTransferCooldown();
    }

    public void setResonanceTransferCooldown(int ticks) {
        resonanceRuntime.setTransferCooldown(ticks);
    }

    public int findResonanceOutgoingSlot() {
        return resonanceRuntime.findOutgoingSlot(items);
    }

    public int insertResonanceReceived(ItemStack offered) {
        return resonanceRuntime.insertReceived(items, offered);
    }

    public void shrinkResonanceOutgoing(int slot, int amount) {
        resonanceRuntime.shrinkOutgoing(items, slot, amount);
    }

    public boolean hasSentinelOwner() {
        return sentinelRuntime.hasOwner();
    }

    public void claimSentinel(Player player) {
        if (kind() == ChestKind.SCULK_SENTINEL) sentinelRuntime.claim(player);
    }

    public boolean canSentinelAccess(Player player) {
        return kind() != ChestKind.SCULK_SENTINEL || sentinelRuntime.canAccess(player);
    }

    public UUID getSentinelOwner() { return sentinelRuntime.owner(); }
    public String getSentinelOwnerName() { return sentinelRuntime.ownerName(); }

    public void addSentinelLog(Player player, SentinelIntrusionType action, long gameTime) {
        if (kind() == ChestKind.SCULK_SENTINEL) sentinelRuntime.addLog(player, action, gameTime);
    }

    public List<SentinelLogEntry> getSentinelLogEntries() { return sentinelRuntime.logEntries(); }

    public void pulseSentinelAlarm(int ticks) {
        if (kind() == ChestKind.SCULK_SENTINEL) sentinelRuntime.pulseAlarm(ticks);
    }

    public boolean isSentinelAlarmActive() { return sentinelRuntime.isAlarmActive(); }
    public int getSentinelWardenCooldown() { return sentinelRuntime.wardenCooldown(); }
    public void setSentinelWardenCooldown(int ticks) { sentinelRuntime.setWardenCooldown(ticks); }
    public UUID getSentinelGuardWardenId() { return sentinelRuntime.guardWardenId(); }
    public UUID getSentinelGuardIntruderId() { return sentinelRuntime.guardIntruderId(); }
    public long getSentinelGuardExpiresAt() { return sentinelRuntime.guardExpiresAt(); }
    public boolean isSentinelGuardRetiring() { return sentinelRuntime.guardRetiring(); }
    public long getSentinelGuardRetireStartedAt() { return sentinelRuntime.guardRetireStartedAt(); }
    public void trackSentinelGuard(UUID wardenId, UUID intruderId, long expiresAt) { sentinelRuntime.trackGuard(wardenId, intruderId, expiresAt); }
    public void setSentinelGuardIntruder(UUID intruderId) { sentinelRuntime.setGuardIntruder(intruderId); }
    public void beginSentinelGuardRetirement(long gameTime) { sentinelRuntime.beginGuardRetirement(gameTime); }
    public void clearSentinelGuard() { sentinelRuntime.clearGuard(); }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        if (kind() == ChestKind.BOTTOMLESS) {
            ContainerHelper.saveAllItems(tag, BottomlessStorage.splitForSerialization(items), registries);
            tag.putBoolean(BOTTOMLESS_DEEP_FORMAT_TAG, true);
            bottomlessDisplayRuntime.write(tag, registries);
        } else {
            ContainerHelper.saveAllItems(tag, items, registries);
        }

        tag.putInt("WorkTicker", workTicker);
        if (kind() == ChestKind.ENDER_DISPATCH) dispatchRuntime.save(tag);
        if (kind() == ChestKind.WITCH) {
            witchRuntime.save(tag);
        }

        if (kind() == ChestKind.RESONANT) {
            resonanceRuntime.save(tag);
        }

        if (kind() == ChestKind.SCULK_SENTINEL) {
            sentinelRuntime.save(tag);
        }

        if (kind() == ChestKind.TRAPPER) {
            trapperRuntime.save(tag);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (kind() == ChestKind.BOTTOMLESS && tag.getBoolean(BOTTOMLESS_DEEP_FORMAT_TAG)) {
            NonNullList<ItemStack> serialized = NonNullList.withSize(
                    BottomlessStorage.SERIALIZED_SLOTS,
                    ItemStack.EMPTY
            );
            ContainerHelper.loadAllItems(tag, serialized, registries);
            items = BottomlessStorage.mergeSerialized(serialized);
        } else {
            items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, items, registries);
        }

        bottomlessDisplayRuntime.loadPersistent(tag, registries);

        workTicker = tag.getInt("WorkTicker");
        if (kind() == ChestKind.WITCH) {
            witchRuntime.load(tag);
        } else {
            witchRuntime.reset();
        }
        if (kind() == ChestKind.ENDER_DISPATCH) dispatchRuntime.load(tag);

        resonanceRuntime.load(tag);

        if (kind() == ChestKind.SCULK_SENTINEL) {
            sentinelRuntime.load(tag);
        }

        if (kind() == ChestKind.TRAPPER) {
            trapperRuntime.load(tag);
        }
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder builder) {
        // Curious Chest block-items are intentionally ordinary empty block items now.
        // Inventory, Resonant node identity, Sentinel state, etc. remain world/block
        // state only and are never copied onto the dropped chest item.
        super.collectImplicitComponents(builder);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        ItemContainerContents contents = input.getOrDefault(
                DataComponents.CONTAINER,
                ItemContainerContents.EMPTY
        );

        if (kind() == ChestKind.BOTTOMLESS) {
            List<ItemStack> stored = contents.stream().toList();
            NonNullList<ItemStack> serialized = NonNullList.withSize(
                    BottomlessStorage.SERIALIZED_SLOTS,
                    ItemStack.EMPTY
            );
            for (int index = 0; index < Math.min(serialized.size(), stored.size()); index++) {
                serialized.set(index, stored.get(index).copy());
            }
            items = BottomlessStorage.mergeSerialized(serialized);
        } else {
            contents.copyInto(items);
        }

        if (kind() == ChestKind.RESONANT) {
            resonanceRuntime.restoreLegacyComponentState(
                    input.get(ModDataComponents.RESONANCE_ID.get()),
                    input.get(ModDataComponents.RESONANCE_RECEIVED_SLOTS.get())
            );
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Items");
        tag.remove(BOTTOMLESS_DEEP_FORMAT_TAG);
        tag.remove(BottomlessDisplayRuntime.TAG);
        tag.remove(ResonanceChestRuntime.NODE_TAG);
        tag.remove(ResonanceChestRuntime.ATTUNEMENT_TAG);
        tag.remove(ResonanceChestRuntime.TRANSFER_COOLDOWN_TAG);
        tag.remove(ResonanceChestRuntime.RECEIVED_TAG);
        tag.remove(SentinelChestRuntime.OWNER_TAG);
        tag.remove(SentinelChestRuntime.OWNER_NAME_TAG);
        tag.remove(SentinelChestRuntime.LOG_TAG);
        tag.remove(SentinelChestRuntime.ALARM_TAG);
        tag.remove(SentinelChestRuntime.WARDEN_COOLDOWN_TAG);
        tag.remove(SentinelChestRuntime.GUARD_WARDEN_TAG);
        tag.remove(SentinelChestRuntime.GUARD_INTRUDER_TAG);
        tag.remove(SentinelChestRuntime.GUARD_EXPIRES_TAG);
        tag.remove(SentinelChestRuntime.GUARD_RETIRING_TAG);
        tag.remove(SentinelChestRuntime.GUARD_RETIRE_STARTED_TAG);
        tag.remove(TrapperChestRuntime.ENTITIES_TAG);
    }

    public boolean tryScoopWitchBrew(Player player, ItemStack bottle) {
        return kind() == ChestKind.WITCH && witchRuntime.tryScoop(player, bottle);
    }

    public int getArchivistBookTime() { return archivistBookRuntime.time(); }
    public float getArchivistBookFlip() { return archivistBookRuntime.flip(); }
    public float getArchivistBookOldFlip() { return archivistBookRuntime.oldFlip(); }
    public float getArchivistBookOpen() { return archivistBookRuntime.open(); }
    public float getArchivistBookOldOpen() { return archivistBookRuntime.oldOpen(); }
    public float getArchivistBookRot() { return archivistBookRuntime.rot(); }
    public float getArchivistBookOldRot() { return archivistBookRuntime.oldRot(); }

    public int getWorkTicker() {
        return workTicker;
    }

    public UUID getTrapperCaptureTargetId() {
        return trapperRuntime.getCaptureTargetId();
    }

    public int getTrapperCaptureCooldown() {
        return trapperRuntime.getCaptureCooldown();
    }

    public void setTrapperCaptureCooldown(int ticks) {
        trapperRuntime.setCaptureCooldown(ticks);
    }

    public double getTrapperCaptureOriginalScale() {
        return trapperRuntime.getCaptureOriginalScale();
    }

    public boolean wasTrapperCaptureOriginallyInvulnerable() {
        return trapperRuntime.wasCaptureOriginallyInvulnerable();
    }

    public void beginTrapperCapture(UUID targetId, double originalScale, boolean originalInvulnerable) {
        if (kind() == ChestKind.TRAPPER) trapperRuntime.beginCapture(targetId, originalScale, originalInvulnerable);
    }

    public int advanceTrapperCaptureTicks() {
        return trapperRuntime.advanceCaptureTicks();
    }

    public boolean isTrapperFinalSuction() {
        return trapperRuntime.isFinalSuction();
    }

    public void beginTrapperFinalSuction() {
        if (kind() == ChestKind.TRAPPER) trapperRuntime.beginFinalSuction();
    }

    public int advanceTrapperFinalSuctionTicks() {
        return trapperRuntime.advanceFinalSuctionTicks();
    }

    public void finishTrapperCapture(int cooldownTicks) {
        trapperRuntime.finishCapture(cooldownTicks);
    }

    public float getTrapperCaptureOpenNess(float partialTick) {
        return kind() == ChestKind.TRAPPER ? trapperRuntime.getCaptureOpenNess(partialTick) : 0.0F;
    }

    public int getTrappedEntityCount() {
        return kind() == ChestKind.TRAPPER ? trapperRuntime.getEntityCount() : 0;
    }

    public List<CompoundTag> getTrappedEntityTags() {
        return kind() == ChestKind.TRAPPER ? trapperRuntime.copyEntityTags() : List.of();
    }

    public boolean captureTrapperEntity(LivingEntity entity) {
        return kind() == ChestKind.TRAPPER && trapperRuntime.captureEntity(entity);
    }

    public boolean releaseTrappedEntity(ServerLevel level, int index) {
        return kind() == ChestKind.TRAPPER && trapperRuntime.releaseEntity(level, index);
    }

    public void armTrapperPackBreak(ServerLevel level) {
        if (kind() == ChestKind.TRAPPER) trapperRuntime.armPackBreak(level);
    }

    public boolean isTrapperPackOnBreak() {
        return kind() == ChestKind.TRAPPER && trapperRuntime.isPackOnBreak();
    }

    public void clearTrapperPackBreakIntent() {
        trapperRuntime.clearPackBreakIntent();
    }

    public ItemStack createPackedTrapperStack() {
        return kind() == ChestKind.TRAPPER ? trapperRuntime.createPackedStack() : ItemStack.EMPTY;
    }

    public static int getPackedTrapperEntityCount(ItemStack stack) {
        return TrapperChestRuntime.getPackedEntityCount(stack);
    }

    public void updateTrapperOccupiedState() {
        if (kind() == ChestKind.TRAPPER) trapperRuntime.updateOccupiedState();
    }

    public Entity getTrapperPreviewEntity() {
        return kind() == ChestKind.TRAPPER ? trapperRuntime.getPreviewEntity() : null;
    }

    @Override
    public void setRemoved() {
        if (kind() == ChestKind.RESONANT) {
            ResonanceLogic.unregister(this);
        }
        if (kind() == ChestKind.TRAPPER) trapperRuntime.onRemoved();
        super.setRemoved();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, SpecialChestBlockEntity chest) {
        chest.lidController.tickLid();
        if (chest.kind() == ChestKind.TRAPPER) {
            chest.trapperRuntime.tickLid();
        }
        if (chest.kind() == ChestKind.ARCHIVIST) {
            chest.archivistBookRuntime.clientTick(level, pos);
        }
        if (chest.kind() == ChestKind.WITCH) {
            chest.witchRuntime.clientTick(level, pos);
        }
        if (chest.kind() == ChestKind.INFERNAL) {
            InfernalClientEffects.tick(level, pos, chest);
        }
        if (chest.kind() == ChestKind.TRAPPER) {
            chest.trapperRuntime.clientTick(level, pos);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpecialChestBlockEntity chest) {
        chest.workTicker++;

        if (chest.kind() == ChestKind.INFERNAL
                && chest.workTicker % InfernalLogic.TICKS_PER_ITEM == 0) {
            if (InfernalLogic.smeltOne(level, pos, state, chest)) chest.setChanged();
        }

        if (chest.kind() == ChestKind.ENDER_DISPATCH) chest.dispatchRuntime.serverTick(level, pos);

        if (level instanceof ServerLevel serverLevel
                && chest.kind() == ChestKind.BUILDERS
                && chest.workTicker % BuilderSupplyLogic.TICK_INTERVAL == 0) {
            BuilderSupplyLogic.tick(serverLevel, pos, chest, chest.builderSnapshots);
        }

        if (level instanceof ServerLevel serverLevel
                && chest.kind() == ChestKind.COLLECTORS
                && chest.workTicker % CollectorLogic.TICK_INTERVAL == 0) {
            if (CollectorLogic.tick(serverLevel, pos, chest)) chest.setChanged();
        }

        if (level instanceof ServerLevel serverLevel && chest.kind() == ChestKind.RESONANT) {
            ResonanceLogic.tick(serverLevel, pos, state, chest);
        }

        if (level instanceof ServerLevel serverLevel
                && chest.kind() == ChestKind.ARCHIVIST
                && chest.workTicker % ArchivistLogic.PROCESS_INTERVAL_TICKS == 0) {
            ArchivistLogic.processOne(serverLevel, pos, chest);
        }

        if (level instanceof ServerLevel serverLevel && chest.kind() == ChestKind.SCULK_SENTINEL) {
            chest.sentinelRuntime.serverTick(serverLevel, pos, state);
        }

        if (level instanceof ServerLevel serverLevel && chest.kind() == ChestKind.TRAPPER) {
            TrapperLogic.tick(serverLevel, pos, state, chest);
            chest.updateTrapperOccupiedState();
        }

        if (chest.kind() == ChestKind.WITCH) {
            chest.witchRuntime.serverTick(level, pos, state);
        }
    }
}
