package com.overyourhead.curiouschests.common.block;

import com.mojang.serialization.MapCodec;
import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.logic.ArchivistLogic;
import com.overyourhead.curiouschests.common.logic.ResonanceLogic;
import com.overyourhead.curiouschests.common.logic.SentinelLogic;
import com.overyourhead.curiouschests.common.network.ArchivistCatalogPayload;
import com.overyourhead.curiouschests.common.sentinel.SentinelIntrusionType;
import com.overyourhead.curiouschests.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.network.PacketDistributor;

public abstract class AbstractSpecialChestBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected AbstractSpecialChestBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    public abstract ChestKind kind();

    @Override
    protected abstract MapCodec<? extends BaseEntityBlock> codec();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            net.minecraft.world.level.BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!player.isShiftKeyDown() || kind() != ChestKind.BOTTOMLESS) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof SpecialChestBlockEntity chest) {
            chest.setStorageDisplayItem(player, stack);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SpecialChestBlockEntity chest) {
                if (chest.kind() == ChestKind.BOTTOMLESS && player.isShiftKeyDown()) {
                    chest.removeStorageDisplayItem(player);
                    return InteractionResult.CONSUME;
                }

                if (chest.kind() == ChestKind.ARCHIVIST
                        && player.isShiftKeyDown()
                        && player instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(
                            serverPlayer,
                            new ArchivistCatalogPayload(ArchivistLogic.collectCatalog(chest))
                    );
                    return InteractionResult.CONSUME;
                }

                if (chest.kind() == ChestKind.SCULK_SENTINEL) {
                    if (!chest.hasSentinelOwner()) {
                        chest.claimSentinel(player);
                    } else if (!chest.canSentinelAccess(player)) {
                        SentinelLogic.trigger(
                                (ServerLevel) level,
                                pos,
                                chest,
                                player,
                                SentinelIntrusionType.OPEN
                        );
                        return InteractionResult.CONSUME;
                    }
                }

                player.openMenu(chest);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof SpecialChestBlockEntity chest) {
            chest.loadFromPlacedStack(stack);
            if (!level.isClientSide
                    && chest.kind() == ChestKind.SCULK_SENTINEL
                    && placer instanceof Player player) {
                chest.claimSentinel(player);
            }
            if (!level.isClientSide && chest.kind() == ChestKind.RESONANT && level instanceof ServerLevel serverLevel) {
                chest.ensureResonanceInitialized();
                ResonanceLogic.registerPlaced(serverLevel, chest);
            }
        }
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!level.isClientSide
                && state.getBlock() != newState.getBlock()
                && level.getBlockEntity(pos) instanceof SpecialChestBlockEntity chest) {
            // Vanilla-container behavior: the block item is empty and every stored
            // item is materialized into the world when the chest is actually removed.
            chest.dropStoredContents(level, pos);

            if (kind() == ChestKind.RESONANT && level instanceof ServerLevel serverLevel) {
                ResonanceLogic.unregisterPlaced(serverLevel, chest);
            }

            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpecialChestBlockEntity(pos, state);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof SpecialChestBlockEntity chest) {
            chest.recheckOpen();
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return createTickerHelper(
                    type,
                    ModBlockEntities.SPECIAL_CHEST.get(),
                    SpecialChestBlockEntity::clientTick
            );
        }
        return createTickerHelper(
                type,
                ModBlockEntities.SPECIAL_CHEST.get(),
                SpecialChestBlockEntity::serverTick
        );
    }
}
