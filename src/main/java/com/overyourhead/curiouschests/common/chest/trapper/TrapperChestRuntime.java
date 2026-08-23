package com.overyourhead.curiouschests.common.chest.trapper;

import com.overyourhead.curiouschests.common.block.AbstractSpecialChestBlock;
import com.overyourhead.curiouschests.common.block.TrapperChestBlock;
import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.core.ModItems;
import com.overyourhead.curiouschests.core.ModParticles;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Owns every piece of Trapper-specific runtime state.
 *
 * The shared block entity exposes a small compatibility facade for networking,
 * events and existing logic classes, while capture/release/storage details stay
 * inside this module.
 */
public final class TrapperChestRuntime {
    public static final String ENTITIES_TAG = "TrapperEntities";
    public static final String CAPTURING_TAG = "TrapperCapturing";

    private final SpecialChestBlockEntity owner;
    private final ChestLidController captureLidController = new ChestLidController();
    private final List<CompoundTag> trappedEntities = new ArrayList<>();
    private final List<Entity> clientPreviewEntities = new ArrayList<>();

    private UUID captureTargetId;
    private int captureTicks;
    private int captureCooldown;
    private double captureOriginalScale = 1.0D;
    private boolean captureOriginalInvulnerable;
    private boolean packOnBreak;
    private boolean finalSuction;
    private int finalSuctionTicks;
    private boolean previewDirty = true;

    public TrapperChestRuntime(SpecialChestBlockEntity owner) {
        this.owner = owner;
    }

    public void tickLid() {
        captureLidController.tickLid();
    }

    public float getCaptureOpenNess(float partialTick) {
        return captureLidController.getOpenness(partialTick);
    }

    public UUID getCaptureTargetId() {
        return captureTargetId;
    }

    public int getCaptureCooldown() {
        return captureCooldown;
    }

    public void setCaptureCooldown(int ticks) {
        captureCooldown = Math.max(0, ticks);
    }

    public double getCaptureOriginalScale() {
        return captureOriginalScale;
    }

    public boolean wasCaptureOriginallyInvulnerable() {
        return captureOriginalInvulnerable;
    }

    public void beginCapture(UUID targetId, double originalScale, boolean originalInvulnerable) {
        captureTargetId = targetId;
        captureTicks = 0;
        captureOriginalScale = Math.max(0.0625D, originalScale);
        captureOriginalInvulnerable = originalInvulnerable;
        finalSuction = false;
        finalSuctionTicks = 0;
        syncClientData();
    }

    public int advanceCaptureTicks() {
        return ++captureTicks;
    }

    public boolean isFinalSuction() {
        return finalSuction;
    }

    public void beginFinalSuction() {
        if (finalSuction) return;
        finalSuction = true;
        finalSuctionTicks = 0;
    }

    public int advanceFinalSuctionTicks() {
        return ++finalSuctionTicks;
    }

    public void finishCapture(int cooldownTicks) {
        captureTargetId = null;
        captureTicks = 0;
        captureOriginalScale = 1.0D;
        captureOriginalInvulnerable = false;
        finalSuction = false;
        finalSuctionTicks = 0;
        captureCooldown = Math.max(0, cooldownTicks);
        syncClientData();
    }

    public int getEntityCount() {
        return trappedEntities.size();
    }

    public List<CompoundTag> copyEntityTags() {
        List<CompoundTag> result = new ArrayList<>(trappedEntities.size());
        for (CompoundTag tag : trappedEntities) result.add(tag.copy());
        return List.copyOf(result);
    }

    public boolean captureEntity(LivingEntity entity) {
        if (trappedEntities.size() >= TrapperLogic.CAPACITY || !TrapperLogic.canCapture(entity)) return false;

        CompoundTag stored = entity.saveWithoutId(new CompoundTag());
        stored.putString("id", EntityType.getKey(entity.getType()).toString());
        stored.remove("HurtTime");
        stored.remove("DeathTime");
        stored.remove("HurtByTimestamp");

        trappedEntities.add(stored.copy());
        previewDirty = true;
        owner.setChanged();
        updateOccupiedState();
        syncClientData();
        return true;
    }

    public boolean releaseEntity(ServerLevel level, int index) {
        return releaseEntity(level, index, true);
    }

    private boolean releaseEntity(ServerLevel level, int index, boolean syncAfterRelease) {
        if (index < 0 || index >= trappedEntities.size()) return false;
        CompoundTag stored = trappedEntities.get(index).copy();
        Direction facing = owner.getBlockState().hasProperty(AbstractSpecialChestBlock.FACING)
                ? owner.getBlockState().getValue(AbstractSpecialChestBlock.FACING)
                : Direction.NORTH;

        Entity restored = EntityType.loadEntityRecursive(stored, level, entity -> entity);
        if (!(restored instanceof LivingEntity living) || !TrapperLogic.canCapture(living)) return false;

        if (!positionReleasedEntity(level, restored, facing, !syncAfterRelease)) return false;
        restored.setDeltaMovement(Vec3.ZERO);
        restored.fallDistance = 0.0F;

        trappedEntities.remove(index);
        TrapperLogic.grantTrapperImmunity(living, TrapperLogic.RELEASE_IMMUNITY_TICKS);
        level.addFreshEntityWithPassengers(restored);
        level.playSound(null, owner.getBlockPos(), SoundEvents.VAULT_EJECT_ITEM, SoundSource.BLOCKS, 0.82F, 1.0F);
        level.sendParticles(
                ModParticles.TRAPPER_ORBIT.get(),
                restored.getX(), restored.getY() + restored.getBbHeight() * 0.45D, restored.getZ(),
                20, 0.35D, 0.45D, 0.35D, 0.10D
        );
        previewDirty = true;
        owner.setChanged();
        if (syncAfterRelease) {
            updateOccupiedState();
            syncClientData();
        }
        return true;
    }

    public void releaseAll(ServerLevel level) {
        while (!trappedEntities.isEmpty()) {
            if (!releaseEntity(level, 0, false)) trappedEntities.remove(0);
        }
    }

    public void armPackBreak(ServerLevel level) {
        TrapperLogic.cancelCapture(level, owner);
        packOnBreak = true;
    }

    public boolean isPackOnBreak() {
        return packOnBreak;
    }

    public void clearPackBreakIntent() {
        packOnBreak = false;
    }

    public ItemStack createPackedStack() {
        ItemStack packed = new ItemStack(ModItems.TRAPPERS_CHEST_ITEM.get());
        if (!trappedEntities.isEmpty()) {
            CompoundTag packedData = new CompoundTag();
            writeEntities(packedData);
            packed.set(DataComponents.CUSTOM_DATA, CustomData.of(packedData));
            packed.set(DataComponents.MAX_STACK_SIZE, 1);
        }
        return packed;
    }

    public static int getPackedEntityCount(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return 0;
        return Math.min(TrapperLogic.CAPACITY, data.copyTag().getList(ENTITIES_TAG, Tag.TAG_COMPOUND).size());
    }

    public void updateOccupiedState() {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide) return;
        BlockState state = owner.getBlockState();
        if (!state.hasProperty(TrapperChestBlock.OCCUPIED)) return;
        boolean occupied = !trappedEntities.isEmpty() || captureTargetId != null;
        if (state.getValue(TrapperChestBlock.OCCUPIED) != occupied) {
            level.setBlock(owner.getBlockPos(), state.setValue(TrapperChestBlock.OCCUPIED, occupied), Block.UPDATE_ALL);
        }
    }

    public void writeClientTag(CompoundTag tag) {
        writeEntities(tag);
        tag.putBoolean(CAPTURING_TAG, captureTargetId != null);
    }

    public void readClientTag(CompoundTag tag) {
        readEntities(tag);
        captureLidController.shouldBeOpen(tag.getBoolean(CAPTURING_TAG));
    }

    public void save(CompoundTag tag) {
        writeEntities(tag);
    }

    public void load(CompoundTag tag) {
        readEntities(tag);
    }

    public void loadFromPlacedStack(ItemStack stack) {
        CustomData packedData = stack.get(DataComponents.CUSTOM_DATA);
        if (packedData != null) {
            readEntities(packedData.copyTag());
        } else {
            clearEntities();
        }
        updateOccupiedState();
    }

    public void clearEntities() {
        trappedEntities.clear();
        previewDirty = true;
        clientPreviewEntities.clear();
    }

    public Entity getPreviewEntity() {
        Level level = owner.getLevel();
        if (level == null || trappedEntities.isEmpty()) return null;
        if (previewDirty) rebuildPreviewEntities(level);
        if (clientPreviewEntities.isEmpty()) return null;
        int index = (int) ((level.getGameTime() / 100L) % clientPreviewEntities.size());
        return clientPreviewEntities.get(index);
    }

    public void clientTick(Level level, net.minecraft.core.BlockPos pos) {
        if (trappedEntities.isEmpty()) return;
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 0.395D;
        double cz = pos.getZ() + 0.5D;

        if (level.random.nextFloat() < 0.34F) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.26D + level.random.nextDouble() * 0.20D;
            double x = cx + Math.cos(angle) * radius;
            double y = cy + (level.random.nextDouble() - 0.5D) * 0.34D;
            double z = cz + Math.sin(angle) * radius;
            level.addParticle(ModParticles.TRAPPER_LINK.get(), cx, cy, cz, x - cx, y - cy, z - cz);
        }
        if (level.getGameTime() % 5L == 0L) {
            double sideY = cy + (level.random.nextDouble() - 0.5D) * 0.16D;
            double sideOffset = 0.43D;
            level.addParticle(ModParticles.TRAPPER_LINK.get(), cx, cy, cz, -sideOffset, sideY - cy, 0.0D);
            level.addParticle(ModParticles.TRAPPER_LINK.get(), cx, cy, cz, sideOffset, sideY - cy, 0.0D);
        }
        if (level.random.nextFloat() < 0.10F) {
            level.addParticle(
                    ModParticles.TRAPPER_ORBIT.get(),
                    cx + (level.random.nextDouble() - 0.5D) * 0.45D,
                    cy + (level.random.nextDouble() - 0.5D) * 0.30D,
                    cz + (level.random.nextDouble() - 0.5D) * 0.45D,
                    0.0D, 0.005D, 0.0D
            );
        }
        if (level.random.nextFloat() < 0.12F) {
            double angle = (level.getGameTime() * 0.18D) + level.random.nextDouble() * 0.7D;
            double radius = 0.24D + level.random.nextDouble() * 0.08D;
            level.addParticle(
                    ParticleTypes.SMALL_FLAME,
                    cx + Math.cos(angle) * radius,
                    cy + 0.04D + (level.random.nextDouble() - 0.5D) * 0.20D,
                    cz + Math.sin(angle) * radius,
                    -Math.sin(angle) * 0.006D,
                    0.006D,
                    Math.cos(angle) * 0.006D
            );
        }
        if (level.random.nextFloat() < 0.008F) {
            level.playLocalSound(cx, cy, cz, SoundEvents.VAULT_AMBIENT, SoundSource.BLOCKS, 0.22F, 1.0F, false);
        }
    }

    public void onRemoved() {
        Level level = owner.getLevel();
        if (captureTargetId != null && level instanceof ServerLevel serverLevel) {
            TrapperLogic.cancelCapture(serverLevel, owner);
        }
    }

    public void handleBrokenContents(ServerLevel level, net.minecraft.core.BlockPos pos) {
        TrapperLogic.cancelCapture(level, owner);
        if (packOnBreak) return;
        if (!trappedEntities.isEmpty()) {
            level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.48F, 1.28F);
            level.sendParticles(
                    ParticleTypes.EXPLOSION,
                    pos.getX() + 0.5D, pos.getY() + 0.65D, pos.getZ() + 0.5D,
                    1, 0.0D, 0.0D, 0.0D, 0.0D
            );
            level.sendParticles(
                    ModParticles.TRAPPER_ORBIT.get(),
                    pos.getX() + 0.5D, pos.getY() + 0.65D, pos.getZ() + 0.5D,
                    36, 0.55D, 0.45D, 0.55D, 0.15D
            );
        }
        releaseAll(level);
    }

    private void syncClientData() {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide) return;
        BlockState state = owner.getBlockState();
        level.sendBlockUpdated(owner.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
    }

    private void writeEntities(CompoundTag tag) {
        if (trappedEntities.isEmpty()) return;
        ListTag list = new ListTag();
        for (int i = 0; i < Math.min(TrapperLogic.CAPACITY, trappedEntities.size()); i++) {
            list.add(trappedEntities.get(i).copy());
        }
        tag.put(ENTITIES_TAG, list);
    }

    private void readEntities(CompoundTag tag) {
        trappedEntities.clear();
        ListTag list = tag.getList(ENTITIES_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(TrapperLogic.CAPACITY, list.size()); i++) {
            CompoundTag entityTag = list.getCompound(i);
            if (entityTag.contains("id", Tag.TAG_STRING)) trappedEntities.add(entityTag.copy());
        }
        previewDirty = true;
        clientPreviewEntities.clear();
    }

    private void rebuildPreviewEntities(Level level) {
        clientPreviewEntities.clear();
        for (CompoundTag tag : trappedEntities) {
            Entity entity = EntityType.loadEntityRecursive(tag.copy(), level, loaded -> loaded);
            if (entity instanceof LivingEntity) {
                entity.setCustomNameVisible(false);
                clientPreviewEntities.add(entity);
            }
        }
        previewDirty = false;
    }

    private boolean positionReleasedEntity(ServerLevel level, Entity entity, Direction facing, boolean emergencySearch) {
        float yaw = facing.toYRot();
        float pitch = entity.getXRot();
        net.minecraft.core.BlockPos pos = owner.getBlockPos();
        Vec3 preferred = new Vec3(
                pos.getX() + 0.5D + facing.getStepX() * 1.35D,
                pos.getY() + 0.10D,
                pos.getZ() + 0.5D + facing.getStepZ() * 1.35D
        );
        if (tryReleasePosition(level, entity, preferred, yaw, pitch)) return true;
        if (!emergencySearch) return false;

        for (int yOffset = 0; yOffset <= 4; yOffset++) {
            Vec3 shaftCandidate = new Vec3(
                    pos.getX() + 0.5D,
                    pos.getY() + yOffset + 0.10D,
                    pos.getZ() + 0.5D
            );
            if (tryReleasePosition(level, entity, shaftCandidate, yaw, pitch)) return true;
        }

        for (int yOffset = 0; yOffset <= 4; yOffset++) {
            for (int radius = 1; radius <= 4; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                        Vec3 candidate = new Vec3(
                                pos.getX() + dx + 0.5D,
                                pos.getY() + yOffset + 0.10D,
                                pos.getZ() + dz + 0.5D
                        );
                        if (tryReleasePosition(level, entity, candidate, yaw, pitch)) return true;
                    }
                }
            }
        }

        int surfaceY = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                pos.getX(),
                pos.getZ()
        );
        Vec3 surface = new Vec3(pos.getX() + 0.5D, surfaceY + 0.05D, pos.getZ() + 0.5D);
        return tryReleasePosition(level, entity, surface, yaw, pitch);
    }

    private static boolean tryReleasePosition(ServerLevel level, Entity entity, Vec3 position, float yaw, float pitch) {
        entity.moveTo(position.x, position.y, position.z, yaw, pitch);
        return level.getWorldBorder().isWithinBounds(entity.getBoundingBox())
                && level.noCollision(entity, entity.getBoundingBox());
    }
}
