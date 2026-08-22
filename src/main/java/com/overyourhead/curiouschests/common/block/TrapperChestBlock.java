package com.overyourhead.curiouschests.common.block;

import com.mojang.serialization.MapCodec;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Trapper chest block. OCCUPIED selects the active texture in the block-entity
 * renderer while captured-entity data lives in SpecialChestBlockEntity.
 */
public final class TrapperChestBlock extends AbstractSpecialChestBlock {
    public static final MapCodec<TrapperChestBlock> CODEC = simpleCodec(TrapperChestBlock::new);
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;

    public TrapperChestBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(OCCUPIED, false));
    }

    @Override
    public ChestKind kind() {
        return ChestKind.TRAPPER;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OCCUPIED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, net.minecraft.core.BlockPos pos, CollisionContext context) {
        return super.getShape(state, level, pos, context);
    }
}
