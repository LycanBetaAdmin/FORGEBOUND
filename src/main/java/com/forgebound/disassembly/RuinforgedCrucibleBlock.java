package com.forgebound.disassembly;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * The Ruinforged Crucible: a blackstone-and-obsidian furnace with a violet firebox, a recycling sigil over it, an
 * amethyst crystal on top and a lantern hanging at each front corner. It faces whoever places it and glows brighter
 * while it burns ({@link #LIT}). Right-click to open {@link CrucibleMenu}; it keeps its items (hoppers work: top =
 * gear, sides = fuel, bottom = output).
 */
public class RuinforgedCrucibleBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<RuinforgedCrucibleBlock> CODEC = simpleCodec(RuinforgedCrucibleBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 3.5, 16), Block.box(1.5, 3.5, 1.5, 14.5, 14, 14.5),
            Block.box(0, 14, 0, 16, 16, 16));
    private static final DustParticleOptions EMBER = new DustParticleOptions(new Vector3f(0.78F, 0.42F, 1.0F), 0.9F);

    public RuinforgedCrucibleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RuinforgedCrucibleBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != com.forgebound.content.ModContent.CRUCIBLE_BE.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<RuinforgedCrucibleBlockEntity>) (l, p, s, be) -> be.serverTick();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RuinforgedCrucibleBlockEntity be) player.openMenu(be);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        Containers.dropContentsOnDestroy(state, newState, level, pos);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    /** Violet embers rising from the crystal; while lit, sparks and crackle at the firebox. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double cx = pos.getX() + 0.5, cz = pos.getZ() + 0.5;
        if (random.nextInt(3) == 0) {
            level.addParticle(EMBER, cx + (random.nextDouble() - 0.5) * 0.4, pos.getY() + 1.15 + random.nextDouble() * 0.5,
                    cz + (random.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
        }
        if (!state.getValue(LIT)) return;
        if (random.nextDouble() < 0.1) {
            level.playLocalSound(cx, pos.getY(), cz, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 0.8F, false);
        }
        Direction f = state.getValue(FACING);
        double side = (random.nextDouble() - 0.5) * 0.35;
        double x = cx + f.getStepX() * 0.5 + (f.getAxis() == Direction.Axis.Z ? side : 0);
        double z = cz + f.getStepZ() * 0.5 + (f.getAxis() == Direction.Axis.X ? side : 0);
        double y = pos.getY() + 0.3 + random.nextDouble() * 0.2;
        level.addParticle(ParticleTypes.SMOKE, x, y + 0.1, z, 0, 0.01, 0);
        if (random.nextInt(2) == 0) level.addParticle(EMBER, x, y, z, 0, 0.01, 0);
        if (random.nextInt(2) == 0) level.addParticle(ParticleTypes.WITCH, x, y + 0.15, z, 0, 0, 0);
    }
}
