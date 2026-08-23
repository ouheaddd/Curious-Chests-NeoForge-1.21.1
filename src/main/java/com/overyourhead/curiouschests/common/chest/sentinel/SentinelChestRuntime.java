package com.overyourhead.curiouschests.common.chest.sentinel;

import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.sentinel.SentinelIntrusionType;
import com.overyourhead.curiouschests.common.sentinel.SentinelLogEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** State holder for Sculk Sentinel ownership, intrusion journal and Warden guard. */
public final class SentinelChestRuntime {
    public static final String OWNER_TAG = "SentinelOwner";
    public static final String OWNER_NAME_TAG = "SentinelOwnerName";
    public static final String LOG_TAG = "SentinelLog";
    public static final String ALARM_TAG = "SentinelAlarmTicks";
    public static final String WARDEN_COOLDOWN_TAG = "SentinelWardenCooldown";
    public static final String GUARD_WARDEN_TAG = "SentinelGuardWarden";
    public static final String GUARD_INTRUDER_TAG = "SentinelGuardIntruder";
    public static final String GUARD_EXPIRES_TAG = "SentinelGuardExpires";
    public static final String GUARD_RETIRING_TAG = "SentinelGuardRetiring";
    public static final String GUARD_RETIRE_STARTED_TAG = "SentinelGuardRetireStarted";

    private final SpecialChestBlockEntity ownerChest;
    private UUID owner;
    private String ownerName = "";
    private final List<SentinelLogEntry> log = new ArrayList<>();
    private int alarmTicks;
    private int wardenCooldown;
    private UUID guardWardenId;
    private UUID guardIntruderId;
    private long guardExpiresAt;
    private boolean guardRetiring;
    private long guardRetireStartedAt;

    public SentinelChestRuntime(SpecialChestBlockEntity ownerChest) {
        this.ownerChest = ownerChest;
    }

    public boolean hasOwner() {
        return owner != null;
    }

    public void claim(Player player) {
        owner = player.getUUID();
        ownerName = player.getGameProfile().getName();
        log.clear();
        alarmTicks = 0;
        wardenCooldown = 0;
        clearGuard();
        ownerChest.setChanged();
        syncClientData();
    }

    public boolean canAccess(Player player) {
        return player.getAbilities().instabuild || owner == null || owner.equals(player.getUUID());
    }

    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public void addLog(Player player, SentinelIntrusionType action, long gameTime) {
        int attempts = 1;
        for (int index = 0; index < log.size(); index++) {
            SentinelLogEntry old = log.get(index);
            if (old.playerId().equals(player.getUUID())
                    && old.action() == action
                    && gameTime - old.gameTime() < SentinelLogic.LOG_DEDUPLICATION_TICKS) {
                attempts = old.attempts() + 1;
                log.remove(index);
                break;
            }
        }

        log.add(0, new SentinelLogEntry(
                player.getUUID(),
                player.getGameProfile().getName(),
                action,
                gameTime,
                attempts
        ));
        while (log.size() > SentinelLogic.MAX_LOG_ENTRIES) log.remove(log.size() - 1);
        ownerChest.setChanged();
    }

    public List<SentinelLogEntry> logEntries() {
        return Collections.unmodifiableList(log);
    }

    public void pulseAlarm(int ticks) {
        boolean wasActive = alarmTicks > 0;
        alarmTicks = Math.max(alarmTicks, ticks);
        ownerChest.setChanged();
        Level level = ownerChest.getLevel();
        if (!wasActive && level != null) {
            level.updateNeighborsAt(ownerChest.getBlockPos(), ownerChest.getBlockState().getBlock());
        }
    }

    public boolean isAlarmActive() {
        return alarmTicks > 0;
    }

    public int wardenCooldown() {
        return wardenCooldown;
    }

    public void setWardenCooldown(int ticks) {
        wardenCooldown = Math.max(0, ticks);
        ownerChest.setChanged();
    }

    public UUID guardWardenId() { return guardWardenId; }
    public UUID guardIntruderId() { return guardIntruderId; }
    public long guardExpiresAt() { return guardExpiresAt; }
    public boolean guardRetiring() { return guardRetiring; }
    public long guardRetireStartedAt() { return guardRetireStartedAt; }

    public void trackGuard(UUID wardenId, UUID intruderId, long expiresAt) {
        guardWardenId = wardenId;
        guardIntruderId = intruderId;
        guardExpiresAt = expiresAt;
        guardRetiring = false;
        guardRetireStartedAt = 0L;
        ownerChest.setChanged();
    }

    public void setGuardIntruder(UUID intruderId) {
        guardIntruderId = intruderId;
        ownerChest.setChanged();
    }

    public void beginGuardRetirement(long gameTime) {
        guardRetiring = true;
        guardRetireStartedAt = gameTime;
        ownerChest.setChanged();
    }

    public void clearGuard() {
        guardWardenId = null;
        guardIntruderId = null;
        guardExpiresAt = 0L;
        guardRetiring = false;
        guardRetireStartedAt = 0L;
        ownerChest.setChanged();
    }

    public void writeClientTag(CompoundTag tag) {
        if (owner != null) tag.putUUID(OWNER_TAG, owner);
        tag.putString(OWNER_NAME_TAG, ownerName);
    }

    public void readClientTag(CompoundTag tag) {
        owner = tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
        ownerName = tag.getString(OWNER_NAME_TAG);
    }

    public void save(CompoundTag tag) {
        if (owner != null) tag.putUUID(OWNER_TAG, owner);
        tag.putString(OWNER_NAME_TAG, ownerName);
        tag.putInt(ALARM_TAG, alarmTicks);
        tag.putInt(WARDEN_COOLDOWN_TAG, wardenCooldown);
        if (guardWardenId != null) tag.putUUID(GUARD_WARDEN_TAG, guardWardenId);
        if (guardIntruderId != null) tag.putUUID(GUARD_INTRUDER_TAG, guardIntruderId);
        tag.putLong(GUARD_EXPIRES_TAG, guardExpiresAt);
        tag.putBoolean(GUARD_RETIRING_TAG, guardRetiring);
        tag.putLong(GUARD_RETIRE_STARTED_TAG, guardRetireStartedAt);

        ListTag logTag = new ListTag();
        for (SentinelLogEntry entry : log) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("Player", entry.playerId());
            entryTag.putString("Name", entry.playerName());
            entryTag.putInt("Action", entry.action().ordinal());
            entryTag.putLong("GameTime", entry.gameTime());
            entryTag.putInt("Attempts", entry.attempts());
            logTag.add(entryTag);
        }
        tag.put(LOG_TAG, logTag);
    }

    public void load(CompoundTag tag) {
        owner = tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
        ownerName = tag.getString(OWNER_NAME_TAG);
        alarmTicks = tag.getInt(ALARM_TAG);
        wardenCooldown = tag.getInt(WARDEN_COOLDOWN_TAG);
        guardWardenId = tag.hasUUID(GUARD_WARDEN_TAG) ? tag.getUUID(GUARD_WARDEN_TAG) : null;
        guardIntruderId = tag.hasUUID(GUARD_INTRUDER_TAG) ? tag.getUUID(GUARD_INTRUDER_TAG) : null;
        guardExpiresAt = tag.getLong(GUARD_EXPIRES_TAG);
        guardRetiring = tag.getBoolean(GUARD_RETIRING_TAG);
        guardRetireStartedAt = tag.getLong(GUARD_RETIRE_STARTED_TAG);

        log.clear();
        ListTag logTag = tag.getList(LOG_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < logTag.size() && log.size() < SentinelLogic.MAX_LOG_ENTRIES; index++) {
            CompoundTag entryTag = logTag.getCompound(index);
            if (!entryTag.hasUUID("Player")) continue;
            log.add(new SentinelLogEntry(
                    entryTag.getUUID("Player"),
                    entryTag.getString("Name"),
                    SentinelIntrusionType.byId(entryTag.getInt("Action")),
                    entryTag.getLong("GameTime"),
                    entryTag.contains("Attempts", Tag.TAG_INT) ? entryTag.getInt("Attempts") : 1
            ));
        }
    }

    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (wardenCooldown > 0) wardenCooldown--;
        SentinelLogic.tickGuard(level, pos, ownerChest);
        if (alarmTicks > 0) {
            alarmTicks--;
            if (alarmTicks == 0) {
                level.updateNeighborsAt(pos, state.getBlock());
                ownerChest.setChanged();
            }
        }
    }

    private void syncClientData() {
        Level level = ownerChest.getLevel();
        if (level == null || level.isClientSide) return;
        BlockState state = ownerChest.getBlockState();
        level.sendBlockUpdated(ownerChest.getBlockPos(), state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }
}
