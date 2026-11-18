package com.forgebound.enhance;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
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
 * The Rune Engraving Table: a dark wooden writing desk with gold trim, an open spellbook, violet crystals and a
 * glowing blue rune circle. Right-click to engrave enhancements ({@link EngraveMenu}) or move them ({@link TransferMenu}).
 */
public class RuneTableBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<RuneTableBlock> CODEC = simpleCodec(RuneTableBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 2, 16), Block.box(1, 2, 1, 15, 11, 15), Block.box(0, 11, 0, 16, 13, 16));
    private static final DustParticleOptions RUNE = new DustParticleOptions(new Vector3f(0.3F, 0.76F, 0.97F), 0.6F);

    public RuneTableBlock(Properties properties) {
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
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) player.openMenu(state.getMenuProvider(level, pos));
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return EngraveMenu.provider(level, pos);
    }

    /** Rune motes over the circle and glyphs drifting into the book. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double cx = pos.getX() + 0.5, cz = pos.getZ() + 0.5;
        if (random.nextInt(2) == 0) {
            double a = random.nextDouble() * Math.PI * 2;
            level.addParticle(RUNE, cx + Math.cos(a) * 0.3, pos.getY() + 0.85, cz + Math.sin(a) * 0.3, 0, 0.01, 0);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, cx + (random.nextDouble() - 0.5), pos.getY() + 1.6, cz + (random.nextDouble() - 0.5),
                    (random.nextDouble() - 0.5) * 0.5, -0.8, (random.nextDouble() - 0.5) * 0.5);
        }
    }
}
