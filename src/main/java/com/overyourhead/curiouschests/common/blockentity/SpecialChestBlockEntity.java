package com.overyourhead.curiouschests.common.blockentity;

import com.overyourhead.curiouschests.common.block.AbstractSpecialChestBlock;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.chest.ChestRules;
import com.overyourhead.curiouschests.common.logic.ArchivistLogic;
import com.overyourhead.curiouschests.common.logic.BuilderSupplyLogic;
import com.overyourhead.curiouschests.common.logic.CollectorLogic;
import com.overyourhead.curiouschests.common.logic.DispatchLogic;
import com.overyourhead.curiouschests.common.logic.InfernalLogic;
import com.overyourhead.curiouschests.common.logic.ResonanceLogic;
import com.overyourhead.curiouschests.common.logic.SentinelLogic;
import com.overyourhead.curiouschests.common.logic.WitchLogic;
import com.overyourhead.curiouschests.common.menu.SpecialChestMenu;
import com.overyourhead.curiouschests.common.sentinel.SentinelIntrusionType;
import com.overyourhead.curiouschests.common.sentinel.SentinelLogEntry;
import com.overyourhead.curiouschests.common.storage.BottomlessStorage;
import com.overyourhead.curiouschests.core.ModBlockEntities;
import com.overyourhead.curiouschests.core.ModDataComponents;
import com.overyourhead.curiouschests.core.ModItems;
import com.overyourhead.curiouschests.core.ModMenus;
import com.overyourhead.curiouschests.core.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SpecialChestBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, LidBlockEntity {
    private static final int EVENT_SET_OPEN_COUNT = 1;
    private static final int EVENT_WITCH_BREW_BURST = 2;
    private static final String BOTTOMLESS_DEEP_FORMAT_TAG = "BottomlessDeepFormat";
    private static final String SENTINEL_OWNER_TAG = "SentinelOwner";
    private static final String SENTINEL_OWNER_NAME_TAG = "SentinelOwnerName";
    private static final String SENTINEL_LOG_TAG = "SentinelLog";
    private static final String SENTINEL_ALARM_TAG = "SentinelAlarmTicks";
    private static final String SENTINEL_WARDEN_COOLDOWN_TAG = "SentinelWardenCooldown";
    private static final String RESONANCE_NODE_TAG = "ResonanceNode";
    private static final String RESONANCE_ATTUNEMENT_TAG = "ResonanceAttunement";
    private static final String RESONANCE_TRANSFER_COOLDOWN_TAG = "ResonanceTransferCooldown";
    private static final String RESONANCE_RECEIVED_TAG = "ResonanceReceivedSlots";
    private static final String DISPATCH_PREVIEW_TAG = "DispatchPreview";

    private final ChestLidController lidController = new ChestLidController();
    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            if (SpecialChestBlockEntity.this.kind() == ChestKind.BOTTOMLESS) {
                playCompressionPistonSound(level, pos, true);
            } else {
                playChestSound(level, pos, SoundEvents.CHEST_OPEN);
            }
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            if (SpecialChestBlockEntity.this.kind() == ChestKind.BOTTOMLESS) {
                playCompressionPistonSound(level, pos, false);
            } else {
                playChestSound(level, pos, SoundEvents.CHEST_CLOSE);
            }
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
    private int dispatchCooldown = DispatchLogic.TRANSFER_DELAY_TICKS;
    private int dispatchPreviewTicks;
    private int dispatchPreviewSlot = -1;
    private ItemStack dispatchPreviewStack = ItemStack.EMPTY;
    private boolean dispatchInternalMutation;
    private boolean witchPotionCountInitialized;
    private int witchLastPotionCount;
    private int witchClientBurstTicks;
    private int witchAmbientSoundCooldown;

    // Client-only visual state for the vanilla-style floating Archivist book.
    private int archivistBookTime;
    private float archivistBookFlip;
    private float archivistBookOldFlip;
    private float archivistBookFlipTarget;
    private float archivistBookFlipVelocity;
    private float archivistBookOpen;
    private float archivistBookOldOpen;
    private float archivistBookRot;
    private float archivistBookOldRot;
    private float archivistBookTargetRot;
    private final InvWrapper fullItemHandler = new InvWrapper(this);
    private final IItemHandler infernalAutomationHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return InfernalLogic.OUTPUT_END;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return fullItemHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot < InfernalLogic.INPUT_START || slot >= InfernalLogic.INPUT_END) {
                return stack;
            }
            return fullItemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < InfernalLogic.OUTPUT_START || slot >= InfernalLogic.OUTPUT_END) {
                return ItemStack.EMPTY;
            }
            return fullItemHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return fullItemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot >= InfernalLogic.INPUT_START
                    && slot < InfernalLogic.INPUT_END
                    && fullItemHandler.isItemValid(slot, stack);
        }
    };
    private final IItemHandler resonanceStorageHandler = new RangedWrapper(
            fullItemHandler,
            0,
            ResonanceLogic.STORAGE_SLOTS
    );
    private final IItemHandler archivistInputHandler = new RangedWrapper(
            fullItemHandler,
            ArchivistLogic.INPUT_SLOT,
            ArchivistLogic.INPUT_SLOT + 1
    );
    private final Map<UUID, BuilderSupplyLogic.HeldSnapshot> builderSnapshots = new HashMap<>();

    private UUID sentinelOwner;
    private String sentinelOwnerName = "";
    private final List<SentinelLogEntry> sentinelLog = new ArrayList<>();
    private int sentinelAlarmTicks;
    private int sentinelWardenCooldown;

    private UUID resonanceNodeId;
    private int resonanceAttunementTicks;
    private int resonanceTransferCooldown = ResonanceLogic.TRANSFER_DELAY_TICKS;
    private final BitSet resonanceReceivedSlots = new BitSet(ResonanceLogic.STORAGE_SLOTS);

    public SpecialChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPECIAL_CHEST.get(), pos, state);
        ChestKind initialKind = kindFromState(state);
        items = NonNullList.withSize(initialKind.slots(), ItemStack.EMPTY);
        if (initialKind == ChestKind.SCULK_SENTINEL) {
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
        if (id == EVENT_WITCH_BREW_BURST) {
            witchClientBurstTicks = Math.max(witchClientBurstTicks, 24 + type * 6);
            return true;
        }
        return super.triggerEvent(id, type);
    }

    @Override
    public float getOpenNess(float partialTick) {
        return lidController.getOpenness(partialTick);
    }

    private void signalOpenCount(Level level, BlockPos pos, BlockState state, int openerCount) {
        level.blockEvent(pos, state.getBlock(), EVENT_SET_OPEN_COUNT, openerCount);
    }

    private static void playChestSound(Level level, BlockPos pos, SoundEvent sound) {
        level.playSound(
                null,
                pos,
                sound,
                SoundSource.BLOCKS,
                0.5F,
                level.random.nextFloat() * 0.1F + 0.9F
        );
    }

    private static void playCompressionPistonSound(Level level, BlockPos pos, boolean extending) {
        SoundEvent sound = extending ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT;
        float pitch = extending
                ? level.random.nextFloat() * 0.25F + 0.60F
                : level.random.nextFloat() * 0.15F + 0.60F;
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.5F, pitch);
    }

    public IItemHandler getItemHandler() {
        return switch (kind()) {
            case INFERNAL -> infernalAutomationHandler;
            case RESONANT -> resonanceStorageHandler;
            case ARCHIVIST -> archivistInputHandler;
            default -> fullItemHandler;
        };
    }

    private static ChestKind kindFromState(BlockState state) {
        return state.getBlock() instanceof AbstractSpecialChestBlock chest
                ? chest.kind()
                : ChestKind.COLLECTORS;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (kind() == ChestKind.ENDER_DISPATCH
                && !dispatchInternalMutation
                && (level == null || !level.isClientSide)) {
            // Preserve the old behavior of delaying a fresh dispatch after an
            // inventory edit. If a preview is already running, it stays visible
            // until its transfer tick and is validated again there.
            dispatchCooldown = DispatchLogic.TRANSFER_DELAY_TICKS;
        }
    }

    public ItemStack getDispatchPreviewStack() {
        return dispatchPreviewStack;
    }

    private void beginDispatchPreview(DispatchLogic.Preview preview) {
        dispatchPreviewSlot = preview.sourceSlot();
        dispatchPreviewStack = preview.stack().copy();
        dispatchPreviewTicks = DispatchLogic.PREVIEW_TICKS;
        syncDispatchPreview();
    }

    private void clearDispatchPreview(boolean sync) {
        boolean hadPreview = !dispatchPreviewStack.isEmpty();
        dispatchPreviewSlot = -1;
        dispatchPreviewTicks = 0;
        dispatchPreviewStack = ItemStack.EMPTY;
        if (sync && hadPreview) {
            syncDispatchPreview();
        }
    }

    private void syncDispatchPreview() {
        if (level == null || level.isClientSide || kind() != ChestKind.ENDER_DISPATCH) return;
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    private void readDispatchPreviewTag(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains(DISPATCH_PREVIEW_TAG, Tag.TAG_COMPOUND)) {
            dispatchPreviewStack = ItemStack.parseOptional(
                    registries,
                    tag.getCompound(DISPATCH_PREVIEW_TAG)
            );
        } else {
            dispatchPreviewStack = ItemStack.EMPTY;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        if (kind() != ChestKind.ENDER_DISPATCH) {
            return super.getUpdateTag(registries);
        }

        CompoundTag tag = new CompoundTag();
        if (!dispatchPreviewStack.isEmpty()) {
            tag.put(DISPATCH_PREVIEW_TAG, dispatchPreviewStack.save(registries));
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        if (kind() == ChestKind.ENDER_DISPATCH) {
            return ClientboundBlockEntityDataPacket.create(this);
        }
        return super.getUpdatePacket();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        if (kind() == ChestKind.ENDER_DISPATCH) {
            readDispatchPreviewTag(tag, registries);
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
            readDispatchPreviewTag(packet.getTag(), registries);
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
            case WITCH -> WitchLogic.MAX_POTIONS_PER_SLOT;
            default -> super.getMaxStackSize();
        };
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        if (kind() == ChestKind.BOTTOMLESS) return BottomlessStorage.maxPerSlot(stack);
        if (kind() == ChestKind.ARCHIVIST && stack.is(Items.ENCHANTED_BOOK)) {
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
            witchLastPotionCount = countSupportedWitchItems();
            witchPotionCountInitialized = true;
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
        if (kind() == ChestKind.ENDER_DISPATCH) {
            dispatchCooldown = DispatchLogic.TRANSFER_DELAY_TICKS;
        }
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
        if (slot >= 0 && slot < ResonanceLogic.STORAGE_SLOTS) {
            resonanceReceivedSlots.clear(slot);
        }
        if (slot == ResonanceLogic.CRYSTAL_SLOT) {
            resonanceAttunementTicks = 0;
        }
        resonanceTransferCooldown = ResonanceLogic.TRANSFER_DELAY_TICKS;
        setChanged();
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
            if (!ArchivistLogic.isProcessableBook(stack)) return false;
            return slot == ArchivistLogic.INPUT_SLOT
                    || (slot >= 0 && slot < ArchivistLogic.STORAGE_SLOTS);
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
        applyComponentsFromItemStack(stack);
        setChanged();
    }

    public void ensureResonanceInitialized() {
        if (kind() != ChestKind.RESONANT || resonanceNodeId != null) return;
        resonanceNodeId = UUID.randomUUID();
        if (items.get(ResonanceLogic.CRYSTAL_SLOT).isEmpty()) {
            items.set(ResonanceLogic.CRYSTAL_SLOT, new ItemStack(ModItems.RESONANCE_CRYSTAL.get()));
        }
        resonanceTransferCooldown = ResonanceLogic.TRANSFER_DELAY_TICKS;
        setChanged();
    }

    public UUID getResonanceNodeId() {
        return resonanceNodeId;
    }

    public boolean advanceResonanceAttunement() {
        resonanceAttunementTicks++;
        if (resonanceAttunementTicks < ResonanceLogic.ATTUNEMENT_TICKS) return false;
        resonanceAttunementTicks = 0;
        return true;
    }

    public void resetResonanceAttunement() {
        if (resonanceAttunementTicks != 0) {
            resonanceAttunementTicks = 0;
            setChanged();
        }
    }

    public void setResonanceCrystalInternal(ItemStack crystal) {
        items.set(ResonanceLogic.CRYSTAL_SLOT, crystal.copyWithCount(1));
        resonanceAttunementTicks = 0;
        resonanceTransferCooldown = ResonanceLogic.TRANSFER_DELAY_TICKS;
        setChanged();
    }

    public boolean tickResonanceTransferCooldown() {
        if (resonanceTransferCooldown <= 0) return false;
        resonanceTransferCooldown--;
        return true;
    }

    public void setResonanceTransferCooldown(int ticks) {
        resonanceTransferCooldown = Math.max(0, ticks);
    }

    public int findResonanceOutgoingSlot() {
        for (int slot = 0; slot < ResonanceLogic.STORAGE_SLOTS; slot++) {
            if (!items.get(slot).isEmpty() && !resonanceReceivedSlots.get(slot)) {
                return slot;
            }
        }
        return -1;
    }

    public int insertResonanceReceived(ItemStack offered) {
        if (kind() != ChestKind.RESONANT || offered.isEmpty()) return 0;

        ItemStack remaining = offered.copy();
        int originalCount = remaining.getCount();

        for (int slot = 0; slot < ResonanceLogic.STORAGE_SLOTS && !remaining.isEmpty(); slot++) {
            if (!resonanceReceivedSlots.get(slot)) continue;
            ItemStack existing = items.get(slot);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, remaining)) continue;

            int limit = Math.min(existing.getMaxStackSize(), getMaxStackSize(existing));
            int moved = Math.min(limit - existing.getCount(), remaining.getCount());
            if (moved <= 0) continue;
            existing.grow(moved);
            remaining.shrink(moved);
        }

        for (int slot = 0; slot < ResonanceLogic.STORAGE_SLOTS && !remaining.isEmpty(); slot++) {
            if (!items.get(slot).isEmpty()) continue;
            int moved = Math.min(remaining.getMaxStackSize(), remaining.getCount());
            items.set(slot, remaining.copyWithCount(moved));
            resonanceReceivedSlots.set(slot);
            remaining.shrink(moved);
        }

        int inserted = originalCount - remaining.getCount();
        if (inserted > 0) setChanged();
        return inserted;
    }

    public void shrinkResonanceOutgoing(int slot, int amount) {
        if (slot < 0 || slot >= ResonanceLogic.STORAGE_SLOTS || amount <= 0) return;
        ItemStack stack = items.get(slot);
        if (stack.isEmpty()) return;
        stack.shrink(Math.min(amount, stack.getCount()));
        if (stack.isEmpty()) items.set(slot, ItemStack.EMPTY);
        resonanceReceivedSlots.clear(slot);
        setChanged();
    }

    public boolean hasSentinelOwner() {
        return sentinelOwner != null;
    }

    public void claimSentinel(Player player) {
        if (kind() != ChestKind.SCULK_SENTINEL) return;
        sentinelOwner = player.getUUID();
        sentinelOwnerName = player.getGameProfile().getName();
        sentinelLog.clear();
        sentinelAlarmTicks = 0;
        sentinelWardenCooldown = 0;
        setChanged();
    }

    public boolean canSentinelAccess(Player player) {
        if (kind() != ChestKind.SCULK_SENTINEL) return true;
        return player.getAbilities().instabuild
                || sentinelOwner == null
                || sentinelOwner.equals(player.getUUID());
    }

    public UUID getSentinelOwner() {
        return sentinelOwner;
    }

    public String getSentinelOwnerName() {
        return sentinelOwnerName;
    }

    public void addSentinelLog(Player player, SentinelIntrusionType action, long gameTime) {
        if (kind() != ChestKind.SCULK_SENTINEL) return;

        for (int index = 0; index < sentinelLog.size(); index++) {
            SentinelLogEntry old = sentinelLog.get(index);
            if (old.playerId().equals(player.getUUID())
                    && old.action() == action
                    && gameTime - old.gameTime() < SentinelLogic.LOG_DEDUPLICATION_TICKS) {
                sentinelLog.remove(index);
                break;
            }
        }

        sentinelLog.add(0, new SentinelLogEntry(
                player.getUUID(),
                player.getGameProfile().getName(),
                action,
                gameTime
        ));
        while (sentinelLog.size() > SentinelLogic.MAX_LOG_ENTRIES) {
            sentinelLog.remove(sentinelLog.size() - 1);
        }
        setChanged();
    }

    public List<SentinelLogEntry> getSentinelLogEntries() {
        return Collections.unmodifiableList(sentinelLog);
    }

    public void pulseSentinelAlarm(int ticks) {
        if (kind() != ChestKind.SCULK_SENTINEL) return;
        boolean wasActive = sentinelAlarmTicks > 0;
        sentinelAlarmTicks = Math.max(sentinelAlarmTicks, ticks);
        setChanged();
        if (!wasActive && level != null) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    public boolean isSentinelAlarmActive() {
        return sentinelAlarmTicks > 0;
    }

    public int getSentinelWardenCooldown() {
        return sentinelWardenCooldown;
    }

    public void setSentinelWardenCooldown(int ticks) {
        sentinelWardenCooldown = Math.max(0, ticks);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        if (kind() == ChestKind.BOTTOMLESS) {
            ContainerHelper.saveAllItems(tag, BottomlessStorage.splitForSerialization(items), registries);
            tag.putBoolean(BOTTOMLESS_DEEP_FORMAT_TAG, true);
        } else {
            ContainerHelper.saveAllItems(tag, items, registries);
        }

        tag.putInt("WorkTicker", workTicker);
        tag.putInt("DispatchCooldown", dispatchCooldown);

        if (kind() == ChestKind.RESONANT) {
            if (resonanceNodeId != null) tag.putUUID(RESONANCE_NODE_TAG, resonanceNodeId);
            tag.putInt(RESONANCE_ATTUNEMENT_TAG, resonanceAttunementTicks);
            tag.putInt(RESONANCE_TRANSFER_COOLDOWN_TAG, resonanceTransferCooldown);
            tag.putLongArray(RESONANCE_RECEIVED_TAG, resonanceReceivedSlots.toLongArray());
        }

        if (kind() == ChestKind.SCULK_SENTINEL) {
            if (sentinelOwner != null) tag.putUUID(SENTINEL_OWNER_TAG, sentinelOwner);
            tag.putString(SENTINEL_OWNER_NAME_TAG, sentinelOwnerName);
            tag.putInt(SENTINEL_ALARM_TAG, sentinelAlarmTicks);
            tag.putInt(SENTINEL_WARDEN_COOLDOWN_TAG, sentinelWardenCooldown);

            ListTag logTag = new ListTag();
            for (SentinelLogEntry entry : sentinelLog) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putUUID("Player", entry.playerId());
                entryTag.putString("Name", entry.playerName());
                entryTag.putInt("Action", entry.action().ordinal());
                entryTag.putLong("GameTime", entry.gameTime());
                logTag.add(entryTag);
            }
            tag.put(SENTINEL_LOG_TAG, logTag);
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

        workTicker = tag.getInt("WorkTicker");
        dispatchCooldown = tag.contains("DispatchCooldown")
                ? tag.getInt("DispatchCooldown")
                : DispatchLogic.TRANSFER_DELAY_TICKS;
        dispatchPreviewTicks = 0;
        dispatchPreviewSlot = -1;
        dispatchPreviewStack = ItemStack.EMPTY;

        resonanceNodeId = tag.hasUUID(RESONANCE_NODE_TAG) ? tag.getUUID(RESONANCE_NODE_TAG) : null;
        resonanceAttunementTicks = tag.getInt(RESONANCE_ATTUNEMENT_TAG);
        resonanceTransferCooldown = tag.contains(RESONANCE_TRANSFER_COOLDOWN_TAG)
                ? tag.getInt(RESONANCE_TRANSFER_COOLDOWN_TAG)
                : ResonanceLogic.TRANSFER_DELAY_TICKS;
        resonanceReceivedSlots.clear();
        resonanceReceivedSlots.or(BitSet.valueOf(tag.getLongArray(RESONANCE_RECEIVED_TAG)));

        sentinelOwner = tag.hasUUID(SENTINEL_OWNER_TAG) ? tag.getUUID(SENTINEL_OWNER_TAG) : null;
        sentinelOwnerName = tag.getString(SENTINEL_OWNER_NAME_TAG);
        sentinelAlarmTicks = tag.getInt(SENTINEL_ALARM_TAG);
        sentinelWardenCooldown = tag.getInt(SENTINEL_WARDEN_COOLDOWN_TAG);
        sentinelLog.clear();
        ListTag logTag = tag.getList(SENTINEL_LOG_TAG, Tag.TAG_COMPOUND);
        for (int index = 0;
             index < logTag.size() && sentinelLog.size() < SentinelLogic.MAX_LOG_ENTRIES;
             index++) {
            CompoundTag entryTag = logTag.getCompound(index);
            if (!entryTag.hasUUID("Player")) continue;
            sentinelLog.add(new SentinelLogEntry(
                    entryTag.getUUID("Player"),
                    entryTag.getString("Name"),
                    SentinelIntrusionType.byId(entryTag.getInt("Action")),
                    entryTag.getLong("GameTime")
            ));
        }
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        NonNullList<ItemStack> componentItems = kind() == ChestKind.BOTTOMLESS
                ? BottomlessStorage.splitForSerialization(items)
                : items;
        builder.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(componentItems));
        if (kind() == ChestKind.RESONANT && resonanceNodeId != null) {
            builder.set(ModDataComponents.RESONANCE_ID.get(), resonanceNodeId);
            List<Integer> receivedSlots = resonanceReceivedSlots.stream().boxed().toList();
            if (!receivedSlots.isEmpty()) {
                builder.set(ModDataComponents.RESONANCE_RECEIVED_SLOTS.get(), receivedSlots);
            }
        }
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
            resonanceNodeId = input.get(ModDataComponents.RESONANCE_ID.get());
            resonanceReceivedSlots.clear();
            Object receivedSlotsComponent = input.get(
                    ModDataComponents.RESONANCE_RECEIVED_SLOTS.get()
            );
            if (receivedSlotsComponent instanceof List<?> receivedSlots) {
                for (Object value : receivedSlots) {
                    if (value instanceof Integer slot
                            && slot >= 0
                            && slot < ResonanceLogic.STORAGE_SLOTS) {
                        resonanceReceivedSlots.set(slot);
                    }
                }
            }
            resonanceAttunementTicks = 0;
            resonanceTransferCooldown = ResonanceLogic.TRANSFER_DELAY_TICKS;
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Items");
        tag.remove(BOTTOMLESS_DEEP_FORMAT_TAG);
        tag.remove(RESONANCE_NODE_TAG);
        tag.remove(RESONANCE_ATTUNEMENT_TAG);
        tag.remove(RESONANCE_TRANSFER_COOLDOWN_TAG);
        tag.remove(RESONANCE_RECEIVED_TAG);
        tag.remove(SENTINEL_OWNER_TAG);
        tag.remove(SENTINEL_OWNER_NAME_TAG);
        tag.remove(SENTINEL_LOG_TAG);
        tag.remove(SENTINEL_ALARM_TAG);
        tag.remove(SENTINEL_WARDEN_COOLDOWN_TAG);
    }

    private int countSupportedWitchItems() {
        int total = 0;
        for (ItemStack stack : items) {
            if (WitchLogic.isSupported(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private void serverTickWitch(Level level, BlockPos pos, BlockState state) {
        int potionCount = countSupportedWitchItems();
        if (!witchPotionCountInitialized) {
            witchLastPotionCount = potionCount;
            witchPotionCountInitialized = true;
            return;
        }

        int added = potionCount - witchLastPotionCount;
        if (added > 0) {
            level.blockEvent(pos, state.getBlock(), EVENT_WITCH_BREW_BURST, Math.min(8, added));
        }
        witchLastPotionCount = potionCount;
    }

    private void clientTickWitch(Level level, BlockPos pos) {
        if (witchAmbientSoundCooldown > 0) {
            witchAmbientSoundCooldown--;
        } else {
            float burstFactor = witchClientBurstTicks > 0 ? 0.045F : 0.016F;
            if (level.random.nextFloat() < burstFactor) {
                level.playLocalSound(
                        pos.getX() + 0.5D,
                        pos.getY() + 0.35D,
                        pos.getZ() + 0.5D,
                        SoundEvents.CAMPFIRE_CRACKLE,
                        SoundSource.BLOCKS,
                        0.36F,
                        1.02F + level.random.nextFloat() * 0.12F,
                        false
                );
                witchAmbientSoundCooldown = 65 + level.random.nextInt(55);
            }
        }

        if (witchClientBurstTicks > 0) {
            witchClientBurstTicks--;
            if (witchClientBurstTicks % 16 == 0) {
                level.playLocalSound(
                        pos.getX() + 0.5D,
                        pos.getY() + 0.8D,
                        pos.getZ() + 0.5D,
                        SoundEvents.BREWING_STAND_BREW,
                        SoundSource.BLOCKS,
                        0.252F,
                        1.15F + (level.random.nextFloat() - 0.5F) * 0.10F,
                        false
                );
            }
        }

        spawnWitchAmbientParticles(level, pos);
        // A burst is deliberately pulsed instead of emitted every tick. This keeps
        // the "new potion" reaction readable without turning into a dense fountain.
        if (witchClientBurstTicks > 0 && witchClientBurstTicks % 3 == 0) {
            spawnWitchBurstParticles(level, pos);
        }
    }

    private void spawnWitchAmbientParticles(Level level, BlockPos pos) {
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY();
        double centerZ = pos.getZ() + 0.5D;

        // Low vapor: spawn on a loose ring OUTSIDE the chest body. The previous
        // center spawn was mostly occluded by the model and made all wisps overlap.
        if (level.random.nextFloat() < 0.10F) {
            spawnWitchBaseSteam(level, centerX, centerY, centerZ, false);
        }

        // Tiny motes above the liquid. Use a wider annulus so they do not stack in
        // one spot over the middle of the lid.
        if (level.random.nextFloat() < 0.035F) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.20D + level.random.nextDouble() * 0.19D;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;
            level.addParticle(
                    ModParticles.WITCH_SPARK.get(),
                    x,
                    centerY + 0.91D + level.random.nextDouble() * 0.035D,
                    z,
                    Math.cos(angle) * (0.0010D + level.random.nextDouble() * 0.0015D),
                    0.0025D + level.random.nextDouble() * 0.0025D,
                    Math.sin(angle) * (0.0010D + level.random.nextDouble() * 0.0015D)
            );
        }

        // Rare liquid-surface wisp: very slow and offset from center.
        if (level.random.nextFloat() < 0.022F) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.16D + level.random.nextDouble() * 0.23D;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;
            level.addParticle(
                    ModParticles.WITCH_STEAM.get(),
                    x,
                    centerY + 0.935D,
                    z,
                    Math.cos(angle) * 0.0012D,
                    0.0025D + level.random.nextDouble() * 0.0018D,
                    Math.sin(angle) * 0.0012D
            );
        }

        // Sparse side spark outside the silhouette rather than inside the block.
        if (level.random.nextFloat() < 0.035F) {
            int side = level.random.nextInt(4);
            double tangent = (level.random.nextDouble() - 0.5D) * 0.70D;
            double distance = 0.58D + level.random.nextDouble() * 0.10D;
            double ox;
            double oz;
            double vx;
            double vz;
            switch (side) {
                case 0 -> { ox = distance; oz = tangent; vx = 0.0025D; vz = tangent * 0.002D; }
                case 1 -> { ox = -distance; oz = tangent; vx = -0.0025D; vz = tangent * 0.002D; }
                case 2 -> { ox = tangent; oz = distance; vx = tangent * 0.002D; vz = 0.0025D; }
                default -> { ox = tangent; oz = -distance; vx = tangent * 0.002D; vz = -0.0025D; }
            }
            level.addParticle(
                    ModParticles.WITCH_SPARK.get(),
                    centerX + ox,
                    centerY + 0.18D + level.random.nextDouble() * 0.23D,
                    centerZ + oz,
                    vx,
                    0.0020D + level.random.nextDouble() * 0.0025D,
                    vz
            );
        }
    }

    private void spawnWitchBaseSteam(
            Level level,
            double centerX,
            double centerY,
            double centerZ,
            boolean burst
    ) {
        int side = level.random.nextInt(4);
        double tangent = (level.random.nextDouble() - 0.5D) * (burst ? 0.90D : 0.76D);
        double distance = (burst ? 0.62D : 0.59D) + level.random.nextDouble() * (burst ? 0.15D : 0.11D);
        double ox;
        double oz;
        double outwardX;
        double outwardZ;
        switch (side) {
            case 0 -> { ox = distance; oz = tangent; outwardX = 1.0D; outwardZ = 0.0D; }
            case 1 -> { ox = -distance; oz = tangent; outwardX = -1.0D; outwardZ = 0.0D; }
            case 2 -> { ox = tangent; oz = distance; outwardX = 0.0D; outwardZ = 1.0D; }
            default -> { ox = tangent; oz = -distance; outwardX = 0.0D; outwardZ = -1.0D; }
        }

        double speed = burst
                ? 0.0030D + level.random.nextDouble() * 0.0030D
                : 0.0012D + level.random.nextDouble() * 0.0018D;
        double sideDrift = (level.random.nextDouble() - 0.5D) * (burst ? 0.0030D : 0.0014D);
        double vx = outwardX * speed + (outwardZ == 0.0D ? 0.0D : sideDrift);
        double vz = outwardZ * speed + (outwardX == 0.0D ? 0.0D : sideDrift);

        level.addParticle(
                ModParticles.WITCH_STEAM.get(),
                centerX + ox,
                centerY + 0.055D + level.random.nextDouble() * 0.075D,
                centerZ + oz,
                vx,
                (burst ? 0.0045D : 0.0025D) + level.random.nextDouble() * 0.0025D,
                vz
        );
    }

    private void spawnWitchBurstParticles(Level level, BlockPos pos) {
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY();
        double centerZ = pos.getZ() + 0.5D;

        // Two separated low puffs around the outside of the feet, never from the
        // solid center of the block.
        spawnWitchBaseSteam(level, centerX, centerY, centerZ, true);
        if (level.random.nextFloat() < 0.70F) {
            spawnWitchBaseSteam(level, centerX, centerY, centerZ, true);
        }

        // A complete custom reaction cloud. It starts outside the solid body, so the
        // chest cannot eat half the sprite, and moves only slightly outward/upward.
        if (level.random.nextFloat() < 0.34F) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.64D + level.random.nextDouble() * 0.14D;
            level.addParticle(
                    ModParticles.WITCH_BURST.get(),
                    centerX + Math.cos(angle) * radius,
                    centerY + 0.10D + level.random.nextDouble() * 0.08D,
                    centerZ + Math.sin(angle) * radius,
                    Math.cos(angle) * (0.0018D + level.random.nextDouble() * 0.0018D),
                    0.0022D + level.random.nextDouble() * 0.0018D,
                    Math.sin(angle) * (0.0018D + level.random.nextDouble() * 0.0018D)
            );
        }

        // A low "reaction" mote travels away from the block when a potion arrives.
        if (level.random.nextFloat() < 0.55F) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.60D + level.random.nextDouble() * 0.16D;
            level.addParticle(
                    ModParticles.WITCH_SPARK.get(),
                    centerX + Math.cos(angle) * radius,
                    centerY + 0.12D + level.random.nextDouble() * 0.10D,
                    centerZ + Math.sin(angle) * radius,
                    Math.cos(angle) * (0.0040D + level.random.nextDouble() * 0.0030D),
                    0.0030D + level.random.nextDouble() * 0.0035D,
                    Math.sin(angle) * (0.0040D + level.random.nextDouble() * 0.0030D)
            );
        }

        // Top reaction is intentionally sparse and spread across the liquid.
        if (level.random.nextFloat() < 0.55F) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.14D + level.random.nextDouble() * 0.26D;
            level.addParticle(
                    ModParticles.WITCH_SPARK.get(),
                    centerX + Math.cos(angle) * radius,
                    centerY + 0.94D,
                    centerZ + Math.sin(angle) * radius,
                    Math.cos(angle) * (0.0025D + level.random.nextDouble() * 0.0025D),
                    0.0050D + level.random.nextDouble() * 0.0030D,
                    Math.sin(angle) * (0.0025D + level.random.nextDouble() * 0.0025D)
            );
        }

        if (level.random.nextFloat() < 0.28F) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.18D + level.random.nextDouble() * 0.22D;
            level.addParticle(
                    ModParticles.WITCH_STEAM.get(),
                    centerX + Math.cos(angle) * radius,
                    centerY + 0.935D,
                    centerZ + Math.sin(angle) * radius,
                    Math.cos(angle) * 0.0020D,
                    0.0040D + level.random.nextDouble() * 0.0025D,
                    Math.sin(angle) * 0.0020D
            );
        }
    }

    public int getArchivistBookTime() {
        return archivistBookTime;
    }

    public float getArchivistBookFlip() {
        return archivistBookFlip;
    }

    public float getArchivistBookOldFlip() {
        return archivistBookOldFlip;
    }

    public float getArchivistBookOpen() {
        return archivistBookOpen;
    }

    public float getArchivistBookOldOpen() {
        return archivistBookOldOpen;
    }

    public float getArchivistBookRot() {
        return archivistBookRot;
    }

    public float getArchivistBookOldRot() {
        return archivistBookOldRot;
    }

    private void clientTickArchivist(Level level, BlockPos pos) {
        archivistBookOldOpen = archivistBookOpen;
        archivistBookOldRot = archivistBookRot;

        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        Player player = level.getNearestPlayer(centerX, centerY, centerZ, 3.0D, false);

        if (player != null) {
            double dx = player.getX() - centerX;
            double dz = player.getZ() - centerZ;
            archivistBookTargetRot = (float) Mth.atan2(dz, dx);
            archivistBookOpen += 0.1F;

            if (archivistBookOpen < 0.5F || level.random.nextInt(40) == 0) {
                float previousTarget = archivistBookFlipTarget;
                do {
                    archivistBookFlipTarget += level.random.nextInt(4) - level.random.nextInt(4);
                } while (previousTarget == archivistBookFlipTarget);
            }
        } else {
            archivistBookTargetRot += 0.02F;
            archivistBookOpen -= 0.1F;
        }

        while (archivistBookRot >= Math.PI) archivistBookRot -= (float) (Math.PI * 2.0D);
        while (archivistBookRot < -Math.PI) archivistBookRot += (float) (Math.PI * 2.0D);
        while (archivistBookTargetRot >= Math.PI) archivistBookTargetRot -= (float) (Math.PI * 2.0D);
        while (archivistBookTargetRot < -Math.PI) archivistBookTargetRot += (float) (Math.PI * 2.0D);

        float rotationDelta = archivistBookTargetRot - archivistBookRot;
        while (rotationDelta >= Math.PI) rotationDelta -= (float) (Math.PI * 2.0D);
        while (rotationDelta < -Math.PI) rotationDelta += (float) (Math.PI * 2.0D);
        archivistBookRot += rotationDelta * 0.4F;

        archivistBookOpen = Mth.clamp(archivistBookOpen, 0.0F, 1.0F);
        archivistBookOldFlip = archivistBookFlip;
        float flipDelta = (archivistBookFlipTarget - archivistBookFlip) * 0.4F;
        flipDelta = Mth.clamp(flipDelta, -0.2F, 0.2F);
        archivistBookFlipVelocity += (flipDelta - archivistBookFlipVelocity) * 0.9F;
        archivistBookFlip += archivistBookFlipVelocity;
        archivistBookTime++;
    }

    private void clientTickInfernal(Level level, BlockPos pos) {
        if (level.random.nextDouble() < 0.10D) {
            level.playLocalSound(
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    SoundEvents.BLASTFURNACE_FIRE_CRACKLE,
                    SoundSource.BLOCKS,
                    0.62F,
                    1.0F,
                    false
            );
        }

        if (level.random.nextFloat() < 0.12F) {
            spawnInfernalVanillaParticles(level, pos);
        }
    }

    private void spawnInfernalVanillaParticles(Level level, BlockPos pos) {
        BlockState state = getBlockState();
        Direction facing = state.hasProperty(AbstractSpecialChestBlock.FACING)
                ? state.getValue(AbstractSpecialChestBlock.FACING)
                : Direction.NORTH;

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 2.0D / 16.0D + level.random.nextDouble() * 6.0D / 16.0D;
        double z = pos.getZ() + 0.5D;
        double tangent = level.random.nextDouble() * 0.6D - 0.3D;
        double forward = 0.52D;

        switch (facing) {
            case WEST -> {
                x -= forward;
                z += tangent;
            }
            case EAST -> {
                x += forward;
                z += tangent;
            }
            case NORTH -> {
                x += tangent;
                z -= forward;
            }
            case SOUTH -> {
                x += tangent;
                z += forward;
            }
            default -> {
                x += tangent;
                z -= forward;
            }
        }

        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
        if (level.random.nextFloat() < 0.65F) {
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public void setRemoved() {
        if (kind() == ChestKind.RESONANT) {
            ResonanceLogic.unregister(this);
        }
        super.setRemoved();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, SpecialChestBlockEntity chest) {
        chest.lidController.tickLid();
        if (chest.kind() == ChestKind.ARCHIVIST) {
            chest.clientTickArchivist(level, pos);
        }
        if (chest.kind() == ChestKind.WITCH) {
            chest.clientTickWitch(level, pos);
        }
        if (chest.kind() == ChestKind.INFERNAL) {
            chest.clientTickInfernal(level, pos);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpecialChestBlockEntity chest) {
        chest.workTicker++;

        if (chest.kind() == ChestKind.INFERNAL
                && chest.workTicker % InfernalLogic.TICKS_PER_ITEM == 0) {
            if (InfernalLogic.smeltOne(level, pos, state, chest)) chest.setChanged();
        }

        if (chest.kind() == ChestKind.ENDER_DISPATCH) {
            if (!chest.dispatchPreviewStack.isEmpty()) {
                if (chest.dispatchPreviewTicks > 0) {
                    chest.dispatchPreviewTicks--;
                }

                if (chest.dispatchPreviewTicks <= 0) {
                    boolean moved;
                    chest.dispatchInternalMutation = true;
                    try {
                        moved = DispatchLogic.dispatchPreviewed(
                                level,
                                pos,
                                chest,
                                chest.dispatchPreviewSlot,
                                chest.dispatchPreviewStack
                        );
                    } finally {
                        chest.dispatchInternalMutation = false;
                    }

                    chest.clearDispatchPreview(true);
                    chest.dispatchCooldown = moved
                            ? DispatchLogic.POST_TRANSFER_GAP_TICKS
                            : DispatchLogic.RETRY_DELAY_TICKS;
                }
            } else {
                if (chest.dispatchCooldown > 0) {
                    chest.dispatchCooldown--;
                }

                if (chest.dispatchCooldown <= 0) {
                    DispatchLogic.Preview preview = DispatchLogic.findPreview(level, pos, chest);
                    if (preview != null) {
                        chest.beginDispatchPreview(preview);
                    } else {
                        chest.dispatchCooldown = DispatchLogic.RETRY_DELAY_TICKS;
                    }
                }
            }
        }

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

        if (chest.kind() == ChestKind.SCULK_SENTINEL) {
            if (chest.sentinelWardenCooldown > 0) chest.sentinelWardenCooldown--;
            if (chest.sentinelAlarmTicks > 0) {
                chest.sentinelAlarmTicks--;
                if (chest.sentinelAlarmTicks == 0) {
                    level.updateNeighborsAt(pos, state.getBlock());
                    chest.setChanged();
                }
            }
        }

        if (chest.kind() == ChestKind.WITCH) {
            chest.serverTickWitch(level, pos, state);
        }
    }
}
