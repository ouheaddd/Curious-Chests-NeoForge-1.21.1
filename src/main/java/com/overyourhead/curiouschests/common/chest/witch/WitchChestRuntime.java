package com.overyourhead.curiouschests.common.chest.witch;

import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.core.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Runtime state and behavior owned exclusively by the Witch Chest.
 *
 * Keeping the brew timer and client ambience here prevents the shared block
 * entity from accumulating another independent state machine.
 */
public final class WitchChestRuntime {
    public static final int EVENT_BREW_BURST = 2;
    public static final String BREW_READY_AT_TAG = "WitchBrewReadyAt";
    public static final String BREW_READY_TAG = "WitchBrewReady";
    public static final int BREW_MIN_TICKS = 2 * 60 * 20;
    public static final int BREW_MAX_TICKS = 10 * 60 * 20;

    private final SpecialChestBlockEntity owner;
    private boolean potionCountInitialized;
    private int lastPotionCount;
    private int clientBurstTicks;
    private int ambientSoundCooldown;
    private long brewReadyAt;
    private boolean brewReady;

    public WitchChestRuntime(SpecialChestBlockEntity owner) {
        this.owner = owner;
    }

    public boolean handleBlockEvent(int id, int type) {
        if (id != EVENT_BREW_BURST) return false;
        clientBurstTicks = Math.max(clientBurstTicks, 24 + type * 6);
        return true;
    }

    public void writeClientTag(CompoundTag tag) {
        tag.putBoolean(BREW_READY_TAG, brewReady);
    }

    public void readClientTag(CompoundTag tag) {
        brewReady = tag.getBoolean(BREW_READY_TAG);
    }

    public void save(CompoundTag tag) {
        tag.putLong(BREW_READY_AT_TAG, brewReadyAt);
    }

    public void load(CompoundTag tag) {
        brewReadyAt = tag.getLong(BREW_READY_AT_TAG);
        brewReady = false;
        potionCountInitialized = false;
        clientBurstTicks = 0;
        ambientSoundCooldown = 0;
    }

    public void reset() {
        brewReadyAt = 0L;
        brewReady = false;
        potionCountInitialized = false;
        lastPotionCount = 0;
        clientBurstTicks = 0;
        ambientSoundCooldown = 0;
    }

    public void onItemsReplaced() {
        lastPotionCount = countSupportedItems();
        potionCountInitialized = true;
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        int potionCount = countSupportedItems();
        if (!potionCountInitialized) {
            lastPotionCount = potionCount;
            potionCountInitialized = true;
        } else {
            int added = potionCount - lastPotionCount;
            if (added > 0) {
                level.blockEvent(pos, state.getBlock(), EVENT_BREW_BURST, Math.min(8, added));
            }
            lastPotionCount = potionCount;
        }

        if (!(level instanceof ServerLevel serverLevel)) return;
        ensureBrewTimer(serverLevel);
        boolean readyNow = serverLevel.getGameTime() >= brewReadyAt;
        if (readyNow != brewReady) {
            brewReady = readyNow;
            if (readyNow) {
                level.blockEvent(pos, state.getBlock(), EVENT_BREW_BURST, 2);
            }
            syncBrewState();
            owner.setChanged();
        }
    }

    public boolean tryScoop(Player player, ItemStack bottle) {
        if (bottle.isEmpty()
                || !bottle.is(Items.GLASS_BOTTLE)
                || !(owner.getLevel() instanceof ServerLevel serverLevel)) {
            return false;
        }

        ensureBrewTimer(serverLevel);
        if (serverLevel.getGameTime() < brewReadyAt) {
            player.displayClientMessage(
                    Component.translatable("message.curiouschests.witch.brew_not_ready"),
                    true
            );
            return true;
        }

        ItemStack potion = WitchLogic.randomVanillaPotion(serverLevel.random);
        if (potion.isEmpty()) return true;

        if (!player.getAbilities().instabuild) {
            bottle.shrink(1);
        }
        if (!player.addItem(potion)) {
            player.drop(potion, false);
        }

        serverLevel.playSound(
                null,
                owner.getBlockPos(),
                SoundEvents.BOTTLE_FILL,
                SoundSource.BLOCKS,
                0.85F,
                0.92F + serverLevel.random.nextFloat() * 0.16F
        );
        serverLevel.playSound(
                null,
                owner.getBlockPos(),
                SoundEvents.BREWING_STAND_BREW,
                SoundSource.BLOCKS,
                0.32F,
                1.08F + serverLevel.random.nextFloat() * 0.10F
        );
        serverLevel.blockEvent(owner.getBlockPos(), owner.getBlockState().getBlock(), EVENT_BREW_BURST, 5);
        scheduleNextBrew(serverLevel);
        return true;
    }

    public void clientTick(Level level, BlockPos pos) {
        if (ambientSoundCooldown > 0) {
            ambientSoundCooldown--;
        } else {
            float burstFactor = brewReady
                    ? 0.055F
                    : (clientBurstTicks > 0 ? 0.045F : 0.016F);
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
                ambientSoundCooldown = brewReady
                        ? 38 + level.random.nextInt(34)
                        : 65 + level.random.nextInt(55);
            }
        }

        if (clientBurstTicks > 0) {
            clientBurstTicks--;
            if (clientBurstTicks % 16 == 0) {
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

        spawnAmbientParticles(level, pos);
        if (clientBurstTicks > 0 && clientBurstTicks % 3 == 0) {
            spawnBurstParticles(level, pos);
        }
    }

    private int countSupportedItems() {
        int total = 0;
        for (int slot = 0; slot < owner.getContainerSize(); slot++) {
            ItemStack stack = owner.getItem(slot);
            if (WitchLogic.isSupported(stack)) total += stack.getCount();
        }
        return total;
    }

    private void ensureBrewTimer(ServerLevel level) {
        if (brewReadyAt > 0L) return;
        scheduleNextBrew(level);
    }

    private void scheduleNextBrew(ServerLevel level) {
        int span = BREW_MAX_TICKS - BREW_MIN_TICKS + 1;
        brewReadyAt = level.getGameTime() + BREW_MIN_TICKS + level.random.nextInt(span);
        brewReady = false;
        owner.setChanged();
        syncBrewState();
    }

    private void syncBrewState() {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide) return;
        BlockState state = owner.getBlockState();
        level.sendBlockUpdated(owner.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
    }

    private void spawnAmbientParticles(Level level, BlockPos pos) {
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY();
        double centerZ = pos.getZ() + 0.5D;
        float readyActivity = brewReady ? 1.85F : 1.0F;

        if (level.random.nextFloat() < 0.10F * readyActivity) {
            spawnBaseSteam(level, centerX, centerY, centerZ, false);
        }
        if (level.random.nextFloat() < 0.035F * readyActivity) {
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
        if (level.random.nextFloat() < 0.022F * readyActivity) {
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
        if (level.random.nextFloat() < 0.035F * readyActivity) {
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

    private void spawnBaseSteam(Level level, double centerX, double centerY, double centerZ, boolean burst) {
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

    private void spawnBurstParticles(Level level, BlockPos pos) {
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY();
        double centerZ = pos.getZ() + 0.5D;

        spawnBaseSteam(level, centerX, centerY, centerZ, true);
        if (level.random.nextFloat() < 0.70F) spawnBaseSteam(level, centerX, centerY, centerZ, true);

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
}
