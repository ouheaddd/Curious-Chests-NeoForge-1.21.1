package com.overyourhead.curiouschests.common.block;

import com.mojang.serialization.MapCodec;
import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
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

public abstract class AbstractSpecialChestBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);
    private static final int BOTTOMLESS_OPEN_XP_COST = 7;
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
        return RenderShape.MODEL;
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
                if (chest.kind() == ChestKind.BOTTOMLESS && !payBottomlessOpeningCost(level, pos, player)) {
                    return InteractionResult.CONSUME;
                }
                player.openMenu(chest);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean payBottomlessOpeningCost(Level level, BlockPos pos, Player player) {
        if (player.getAbilities().instabuild) return true;

        if (player.totalExperience < BOTTOMLESS_OPEN_XP_COST) {
            player.displayClientMessage(
                    Component.translatable("message.curiouschests.bottomless_not_enough_xp", BOTTOMLESS_OPEN_XP_COST),
                    true
            );
            return false;
        }

        player.giveExperiencePoints(-BOTTOMLESS_OPEN_XP_COST);
        level.playSound(
                null,
                pos,
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.BLOCKS,
                0.35F,
                0.65F
        );
        return true;
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
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpecialChestBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide) return null;
        return createTickerHelper(
                type,
                com.overyourhead.curiouschests.core.ModBlockEntities.SPECIAL_CHEST.get(),
                SpecialChestBlockEntity::serverTick
        );
    }
}
