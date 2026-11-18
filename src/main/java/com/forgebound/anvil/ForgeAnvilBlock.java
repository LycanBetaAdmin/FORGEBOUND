package com.forgebound.anvil;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * The Forge Anvil: a dark steel anvil with antique gold trim and glowing blue runes. Right-click to raise a piece of
 * equipment's tier ({@link ForgeAnvilMenu}); like a crafting table it keeps nothing. Its long side faces the placer.
 */
public class ForgeAnvilBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ForgeAnvilBlock> CODEC = simpleCodec(ForgeAnvilBlock::new);
    private static final VoxelShape SHAPE_X = Shapes.or(Block.box(1, 0, 2, 15, 4, 14), Block.box(4, 4, 5, 12, 10, 11), Block.box(0, 10, 3, 16, 16, 13));
    private static final VoxelShape SHAPE_Z = Shapes.or(Block.box(2, 0, 1, 14, 4, 15), Block.box(5, 4, 4, 11, 10, 12), Block.box(3, 10, 0, 13, 16, 16));
    private static final DustParticleOptions RUNE = new DustParticleOptions(new Vector3f(0.3F, 0.76F, 0.97F), 0.7F);

    public ForgeAnvilBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_X : SHAPE_Z;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) player.openMenu(state.getMenuProvider(level, pos));
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return ForgeAnvilMenu.provider(level, pos);
    }

    /** Blue rune motes drifting off the anvil face. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) return;
        level.addParticle(RUNE, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.02, pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                0, 0.015, 0);
    }
}
