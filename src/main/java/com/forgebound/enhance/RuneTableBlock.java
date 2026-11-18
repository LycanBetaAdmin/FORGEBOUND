package com.forgebound.enhance;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;

/**
 * The Rune Engraving Table: a two-block violet stone altar with gold trim, a runner with a glowing magic circle under a
 * floating amethyst, a book stand, tomes, candles and potions, and two rune pillars carrying an arch with a plaque and
 * lanterns. It is placed as two blocks side by side: the main half (where it was placed; it renders the whole model)
 * and a side half to the placer's right. Right-click either half to engrave enhancements ({@link EngraveMenu}) or move
 * them ({@link TransferMenu}). Breaking either half removes both; only the main half drops the table.
 */
public class RuneTableBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<RuneTableBlock> CODEC = simpleCodec(RuneTableBlock::new);
    /** true on the side half (no model of its own; the main half draws the whole table). */
    public static final BooleanProperty SIDE = BooleanProperty.create("side");
    private static final DustParticleOptions RUNE = new DustParticleOptions(new Vector3f(0.82F, 0.45F, 1.0F), 0.6F);
    // shapes for a north-facing table (front to the north) in each half's own block space; the side half lies to the west
    private static final double[][] MAIN_BOXES = {{0, 0, 0.5, 15.5, 2, 15.5}, {0, 2, 2, 14, 11, 14}, {0, 11, 0, 16, 13, 16}, {11, 13, 12, 15, 16, 16}};
    private static final double[][] SIDE_BOXES = {{0.5, 0, 0.5, 16, 2, 15.5}, {2, 2, 2, 16, 11, 14}, {0, 11, 0, 16, 13, 16}, {1, 13, 12, 5, 16, 16}};
    private static final Map<Direction, VoxelShape> MAIN_SHAPES = shapes(MAIN_BOXES), SIDE_SHAPES = shapes(SIDE_BOXES);

    public RuneTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SIDE, false));
    }

    /** Rotates north-facing boxes to each facing, the same way the blockstate rotates the model. */
    private static Map<Direction, VoxelShape> shapes(double[][] boxes) {
        Map<Direction, VoxelShape> m = new EnumMap<>(Direction.class);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            VoxelShape s = Shapes.empty();
            for (double[] b : boxes) {
                double x1 = b[0], z1 = b[2], x2 = b[3], z2 = b[5];
                double[] r = switch (d) {
                    case EAST -> new double[]{16 - z2, x1, 16 - z1, x2};
                    case SOUTH -> new double[]{16 - x2, 16 - z2, 16 - x1, 16 - z1};
                    case WEST -> new double[]{z1, 16 - x2, z2, 16 - x1};
                    default -> new double[]{x1, z1, x2, z2};
                };
                s = Shapes.or(s, Block.box(r[0], b[1], r[1], r[2], b[4], r[3]));
            }
            m.put(d, s.optimize());
        }
        return m;
    }

    /** Direction from this half to its partner. */
    public static Direction toPartner(BlockState state) {
        Direction f = state.getValue(FACING);
        return state.getValue(SIDE) ? f.getClockWise() : f.getCounterClockWise();
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SIDE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos side = context.getClickedPos().relative(facing.getCounterClockWise());
        Level level = context.getLevel();
        if (!level.getWorldBorder().isWithinBounds(side) || !level.getBlockState(side).canBeReplaced(context)) return null;
        return defaultBlockState().setValue(FACING, facing).setValue(SIDE, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) level.setBlock(pos.relative(toPartner(state)), state.setValue(SIDE, true), Block.UPDATE_ALL);
    }

    /** Removing one half removes its partner too (no second drop). */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(this)) {
            BlockPos other = pos.relative(toPartner(state));
            BlockState o = level.getBlockState(other);
            if (o.is(this) && o.getValue(FACING) == state.getValue(FACING) && o.getValue(SIDE) != state.getValue(SIDE)) {
                level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(2001, other, Block.getId(o));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(SIDE) ? SIDE_SHAPES : MAIN_SHAPES).get(state.getValue(FACING));
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

    /** Rune motes rising from the magic circle and enchanting glyphs drifting down into it (main half only). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(SIDE)) return;
        Direction s = state.getValue(FACING).getCounterClockWise();
        double cx = pos.getX() + 0.5 + s.getStepX() * 0.5, cz = pos.getZ() + 0.5 + s.getStepZ() * 0.5;
        if (random.nextInt(2) == 0) {
            double a = random.nextDouble() * Math.PI * 2;
            level.addParticle(RUNE, cx + Math.cos(a) * 0.3, pos.getY() + 0.86, cz + Math.sin(a) * 0.3, 0, 0.012, 0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, cx + (random.nextDouble() - 0.5) * 0.8, pos.getY() + 1.7, cz + (random.nextDouble() - 0.5) * 0.8,
                    (random.nextDouble() - 0.5) * 0.4, -0.8, (random.nextDouble() - 0.5) * 0.4);
        }
    }
}
