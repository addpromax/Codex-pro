package net.momirealms.craftengine.bukkit.plugin.injector;

import com.google.common.collect.ImmutableList;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.bukkit.block.BukkitBlockShape;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.util.NoteBlockChainUpdateUtils;
import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.BlockKeys;
import net.momirealms.craftengine.core.block.BlockShape;
import net.momirealms.craftengine.core.block.DelegatingBlock;
import net.momirealms.craftengine.core.block.behavior.EmptyBlockBehavior;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ObjectHolder;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class BlockGenerator {
    private static final BukkitBlockShape STONE_SHAPE =
            new BukkitBlockShape(MBlocks.STONE$defaultState, MBlocks.STONE$defaultState);
    private static MethodHandle constructor$CraftEngineBlock;
    private static Field field$CraftEngineBlock$behavior;
    private static Field field$CraftEngineBlock$shape;
    private static Field field$CraftEngineBlock$isNoteBlock;
    private static Field field$CraftEngineBlock$isTripwire;

    public static void init() throws ReflectiveOperationException {
        ByteBuddy byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V17);
        // CraftEngine Blocks
        String packageWithName = BlockGenerator.class.getName();
        String generatedClassName = packageWithName.substring(0, packageWithName.lastIndexOf('.')) + ".CraftEngineBlock";
        DynamicType.Builder<?> builder = byteBuddy
                .subclass(CoreReflections.clazz$Block, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name(generatedClassName)
                .defineField("behaviorHolder", ObjectHolder.class, Visibility.PUBLIC)
                .defineField("shapeHolder", ObjectHolder.class, Visibility.PUBLIC)
                .defineField("isClientSideNoteBlock", boolean.class, Visibility.PUBLIC)
                .defineField("isClientSideTripwire", boolean.class, Visibility.PUBLIC)
                // should always implement this interface
                .implement(DelegatingBlock.class)
                .implement(CoreReflections.clazz$Fallable)
                .implement(CoreReflections.clazz$BonemealableBlock)
                .implement(CoreReflections.clazz$SimpleWaterloggedBlock)
                // internal interfaces
                .method(ElementMatchers.named("behaviorDelegate"))
                .intercept(FieldAccessor.ofField("behaviorHolder"))
                .method(ElementMatchers.named("shapeDelegate"))
                .intercept(FieldAccessor.ofField("shapeHolder"))
                .method(ElementMatchers.named("isNoteBlock"))
                .intercept(FieldAccessor.ofField("isClientSideNoteBlock"))
                .method(ElementMatchers.named("isTripwire"))
                .intercept(FieldAccessor.ofField("isClientSideTripwire"))
                // getShape
                .method(ElementMatchers.is(CoreReflections.method$BlockBehaviour$getShape))
                .intercept(MethodDelegation.to(GetShapeInterceptor.INSTANCE))
                // getCollisionShape
                .method(ElementMatchers.is(CoreReflections.method$BlockBehaviour$getCollisionShape))
                .intercept(MethodDelegation.to(GetCollisionShapeInterceptor.INSTANCE))
                // getSupportShape
                .method(ElementMatchers.is(CoreReflections.method$BlockBehaviour$getBlockSupportShape))
                .intercept(MethodDelegation.to(GetSupportShapeInterceptor.INSTANCE))
                // isPathFindable
                .method(ElementMatchers.is(CoreReflections.method$BlockBehaviour$isPathFindable))
                .intercept(MethodDelegation.to(IsPathFindableInterceptor.INSTANCE))
                // mirror
                .method(ElementMatchers.is(CoreReflections.method$BlockBehaviour$mirror))
                .intercept(MethodDelegation.to(MirrorInterceptor.INSTANCE))
                // rotate
                .method(ElementMatchers.is(CoreReflections.method$BlockBehaviour$rotate))
                .intercept(MethodDelegation.to(RotateInterceptor.INSTANCE))
                // tick
                .method(ElementMatchers.is(CoreReflections.method$BlockBehaviour$tick))
                .intercept(MethodDelegation.to(TickInterceptor.INSTANCE))
                // isValidBoneMealTarget
                .method(ElementMatchers.is(CoreReflections.method$BonemealableBlock$isValidBonemealTarget))
                .intercept(MethodDelegation.to(IsValidBoneMealTargetInterceptor.INSTANCE))
                // isBoneMealSuccess
                .method(ElementMatchers.is(CoreReflections.method$BonemealableBlock$isBonemealSuccess))
                .intercept(MethodDelegation.to(IsBoneMealSuccessInterceptor.INSTANCE))
                // performBoneMeal
                .method(ElementMatchers.is(CoreReflections.method$BonemealableBlock$performBonemeal))
                .intercept(MethodDelegation.to(PerformBoneMealInterceptor.INSTANCE))
                // random tick
                .method(ElementMatchers.is(CoreReflections.method$BlockBehaviour$randomTick))
                .intercept(MethodDelegation.to(RandomTickInterceptor.INSTANCE))
                // onPlace
                .method(ElementMatchers.takesArguments(5)
                        .and(ElementMatchers.takesArgument(0, CoreReflections.clazz$BlockState))
                        .and(ElementMatchers.takesArgument(1, CoreReflections.clazz$Level))
                        .and(ElementMatchers.takesArgument(2, CoreReflections.clazz$BlockPos))
                        .and(ElementMatchers.takesArgument(3, CoreReflections.clazz$BlockState))
                        .and(ElementMatchers.takesArgument(4, boolean.class))
                        .and(ElementMatchers.named("onPlace").or(ElementMatchers.named("a")))
                )
                .intercept(MethodDelegation.to(OnPlaceInterceptor.INSTANCE))
                // onBrokenAfterFall
                .method(ElementMatchers.takesArguments(3)
                        .and(ElementMatchers.takesArgument(0, CoreReflections.clazz$Level))
                        .and(ElementMatchers.takesArgument(1, CoreReflections.clazz$BlockPos))
                        .and(ElementMatchers.takesArgument(2, CoreReflections.clazz$FallingBlockEntity))
                )
                .intercept(MethodDelegation.to(OnBrokenAfterFallInterceptor.INSTANCE))
                // onLand
                .method(ElementMatchers.takesArguments(5)
                        .and(ElementMatchers.takesArgument(0, CoreReflections.clazz$Level))
                        .and(ElementMatchers.takesArgument(1, CoreReflections.clazz$BlockPos))
                        .and(ElementMatchers.takesArgument(2, CoreReflections.clazz$BlockState))
                        .and(ElementMatchers.takesArgument(3, CoreReflections.clazz$BlockState))
                        .and(ElementMatchers.takesArgument(4, CoreReflections.clazz$FallingBlockEntity))
                )
                .intercept(MethodDelegation.to(OnLandInterceptor.INSTANCE))
                // canSurvive
                .method(ElementMatchers.takesArguments(3)
                        .and(ElementMatchers.takesArgument(0, CoreReflections.clazz$BlockState))
                        .and(ElementMatchers.takesArgument(1, CoreReflections.clazz$LevelReader))
                        .and(ElementMatchers.takesArgument(2, CoreReflections.clazz$BlockPos))
                )
                .intercept(MethodDelegation.to(CanSurviveInterceptor.INSTANCE))
                // updateShape
                .method(ElementMatchers.returns(CoreReflections.clazz$BlockState)
                        .and(ElementMatchers.takesArgument(0, CoreReflections.clazz$BlockState))
                        // LevelReader 1.21.3+                                                     // 1.20-1.12.2
                        .and(ElementMatchers.takesArgument(1, CoreReflections.clazz$LevelReader).or(ElementMatchers.takesArgument(1, CoreReflections.clazz$Direction)))
                        .and(ElementMatchers.named("updateShape").or(ElementMatchers.named("a"))))
                .intercept(MethodDelegation.to(UpdateShapeInterceptor.INSTANCE))
                // onExplosionHit 1.21+
                .method(ElementMatchers.returns(void.class)
                        .and(ElementMatchers.takesArgument(0, CoreReflections.clazz$BlockState))
                        .and(ElementMatchers.takesArgument(1, CoreReflections.clazz$ServerLevel))
                        .and(ElementMatchers.takesArgument(2, CoreReflections.clazz$BlockPos))
                        .and(ElementMatchers.takesArgument(3, CoreReflections.clazz$Explosion))
                        .and(ElementMatchers.takesArgument(4, BiConsumer.class))
                )
                .intercept(MethodDelegation.to(OnExplosionHitInterceptor.INSTANCE))
                // neighborChanged
                .method(ElementMatchers.is(CoreReflections.method$BlockBehaviour$neighborChanged))
                .intercept(MethodDelegation.to(NeighborChangedInterceptor.INSTANCE))
                // pickupBlock
                .method(ElementMatchers.is(CoreReflections.method$SimpleWaterloggedBlock$pickupBlock))
                .intercept(MethodDelegation.to(PickUpBlockInterceptor.INSTANCE))
                // placeLiquid
                .method(ElementMatchers.is(CoreReflections.method$SimpleWaterloggedBlock$placeLiquid))
                .intercept(MethodDelegation.to(PlaceLiquidInterceptor.INSTANCE))
                // canPlaceLiquid
                .method(ElementMatchers.is(CoreReflections.method$SimpleWaterloggedBlock$canPlaceLiquid))
                .intercept(MethodDelegation.to(CanPlaceLiquidInterceptor.INSTANCE))
                // entityInside
                .method(ElementMatchers.is(CoreReflections.method$BlockBehaviour$entityInside))
                .intercept(MethodDelegation.to(EntityInsideInterceptor.INSTANCE))
                // getSignal
                .method(ElementMatchers.is(CoreReflections.method$BlockBehaviour$getSignal))
                .intercept(MethodDelegation.to(GetSignalInterceptor.INSTANCE))
                // getDirectSignal
                .method(ElementMatchers.is(CoreReflections.method$BlockBehaviour$getDirectSignal))
                .intercept(MethodDelegation.to(GetDirectSignalInterceptor.INSTANCE))
                // isSignalSource
                .method(ElementMatchers.is(CoreReflections.method$BlockBehaviour$isSignalSource))
                .intercept(MethodDelegation.to(IsSignalSourceInterceptor.INSTANCE));
        if (CoreReflections.method$BlockBehaviour$affectNeighborsAfterRemoval != null) {
            builder.method(ElementMatchers.is(CoreReflections.method$BlockBehaviour$affectNeighborsAfterRemoval))
                    .intercept(MethodDelegation.to(AffectNeighborsAfterRemovalInterceptor.INSTANCE));
        }

        Class<?> clazz$CraftEngineBlock = builder.make().load(BlockGenerator.class.getClassLoader()).getLoaded();
        constructor$CraftEngineBlock = MethodHandles.publicLookup().in(clazz$CraftEngineBlock)
                .findConstructor(clazz$CraftEngineBlock, MethodType.methodType(void.class, CoreReflections.clazz$BlockBehaviour$Properties))
                .asType(MethodType.methodType(CoreReflections.clazz$Block, CoreReflections.clazz$BlockBehaviour$Properties));
        field$CraftEngineBlock$behavior = clazz$CraftEngineBlock.getField("behaviorHolder");
        field$CraftEngineBlock$shape = clazz$CraftEngineBlock.getField("shapeHolder");
        field$CraftEngineBlock$isNoteBlock = clazz$CraftEngineBlock.getField("isClientSideNoteBlock");
        field$CraftEngineBlock$isTripwire = clazz$CraftEngineBlock.getField("isClientSideTripwire");
    }

    public static Object generateBlock(Key replacedBlock, Object ownerBlock, Object properties) throws Throwable {
        Object ownerProperties = CoreReflections.field$BlockBehaviour$properties.get(ownerBlock);
        CoreReflections.field$BlockBehaviour$Properties$hasCollision.set(properties, CoreReflections.field$BlockBehaviour$Properties$hasCollision.get(ownerProperties));

        ObjectHolder<BlockBehavior> behaviorHolder = new ObjectHolder<>(EmptyBlockBehavior.INSTANCE);
        ObjectHolder<BlockShape> shapeHolder = new ObjectHolder<>(STONE_SHAPE);

        Object newBlockInstance = constructor$CraftEngineBlock.invoke(properties);
        field$CraftEngineBlock$behavior.set(newBlockInstance, behaviorHolder);
        field$CraftEngineBlock$shape.set(newBlockInstance, shapeHolder);
        field$CraftEngineBlock$isNoteBlock.set(newBlockInstance, replacedBlock.equals(BlockKeys.NOTE_BLOCK));
        field$CraftEngineBlock$isTripwire.set(newBlockInstance, replacedBlock.equals(BlockKeys.TRIPWIRE));

        Object stateDefinitionBuilder = CoreReflections.constructor$StateDefinition$Builder.newInstance(newBlockInstance);
        Object stateDefinition = CoreReflections.method$StateDefinition$Builder$create.invoke(stateDefinitionBuilder,
                (Function<Object, Object>) FastNMS.INSTANCE::method$Block$defaultState, BlockStateGenerator.instance$StateDefinition$Factory);
        CoreReflections.field$Block$StateDefinition.set(newBlockInstance, stateDefinition);
        CoreReflections.field$Block$defaultBlockState.set(newBlockInstance, ((ImmutableList<?>) CoreReflections.field$StateDefinition$states.get(stateDefinition)).getFirst());
        return newBlockInstance;
    }

    public static class UpdateShapeInterceptor {
        public static final UpdateShapeInterceptor INSTANCE = new UpdateShapeInterceptor();
        public static final int levelIndex = VersionHelper.isOrAbove1_21_2() ? 1 : 3;
        public static final int directionIndex = VersionHelper.isOrAbove1_21_2() ? 4 : 1;
        public static final int posIndex = VersionHelper.isOrAbove1_21_2() ? 3 : 4;

        @SuppressWarnings("deprecation")
        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            DelegatingBlock indicator = (DelegatingBlock) thisObj;
            // todo chain updater
            if (indicator.isNoteBlock()) {
                if (CoreReflections.clazz$ServerLevel.isInstance(args[levelIndex])) {
                    startNoteBlockChain(args);
                }
            } else if (indicator.isTripwire()) {
                if (CoreReflections.clazz$ServerLevel.isInstance(args[posIndex])) {

                }
            }
            try {
                return holder.value().updateShape(thisObj, args, superMethod);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run updateShape", e);
                return args[0];
            }
        }

        private static void startNoteBlockChain(Object[] args) {
            Object direction = args[directionIndex];
            Object serverLevel = args[levelIndex];
            Object blockPos = args[posIndex];
            // Y axis
            if (direction == CoreReflections.instance$Direction$DOWN) {
                Object chunkSource = FastNMS.INSTANCE.method$ServerLevel$getChunkSource(serverLevel);
                FastNMS.INSTANCE.method$ServerChunkCache$blockChanged(chunkSource, blockPos);
                NoteBlockChainUpdateUtils.noteBlockChainUpdate(serverLevel, chunkSource, CoreReflections.instance$Direction$UP, blockPos, Config.maxNoteBlockChainUpdate());
            } else if (direction == CoreReflections.instance$Direction$UP) {
                Object chunkSource = FastNMS.INSTANCE.method$ServerLevel$getChunkSource(serverLevel);
                FastNMS.INSTANCE.method$ServerChunkCache$blockChanged(chunkSource, blockPos);
                NoteBlockChainUpdateUtils.noteBlockChainUpdate(serverLevel, chunkSource, CoreReflections.instance$Direction$DOWN, blockPos, Config.maxNoteBlockChainUpdate());
            }
        }
    }

    public static class GetShapeInterceptor {
        public static final GetShapeInterceptor INSTANCE = new GetShapeInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) throws Exception {
            ObjectHolder<BlockShape> holder = ((DelegatingBlock) thisObj).shapeDelegate();
            try {
                return holder.value().getShape(thisObj, args);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run getShape", e);
                return superMethod.call();
            }
        }
    }

    public static class GetCollisionShapeInterceptor {
        public static final GetCollisionShapeInterceptor INSTANCE = new GetCollisionShapeInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) throws Exception {
            ObjectHolder<BlockShape> holder = ((DelegatingBlock) thisObj).shapeDelegate();
            try {
                return holder.value().getCollisionShape(thisObj, args);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run getCollisionShape", e);
                return superMethod.call();
            }
        }
    }

    public static class GetSupportShapeInterceptor {
        public static final GetSupportShapeInterceptor INSTANCE = new GetSupportShapeInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) throws Exception {
            ObjectHolder<BlockShape> holder = ((DelegatingBlock) thisObj).shapeDelegate();
            try {
                return holder.value().getSupportShape(thisObj, args);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run getSupportShape", e);
                return superMethod.call();
            }
        }
    }

    public static class IsPathFindableInterceptor {
        public static final IsPathFindableInterceptor INSTANCE = new IsPathFindableInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) throws Exception {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                return holder.value().isPathFindable(thisObj, args, superMethod);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run isPathFindable", e);
                return superMethod.call();
            }
        }
    }

    public static class MirrorInterceptor {
        public static final MirrorInterceptor INSTANCE = new MirrorInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) throws Exception {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                return holder.value().mirror(thisObj, args, superMethod);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run mirror", e);
                return superMethod.call();
            }
        }
    }

    public static class RotateInterceptor {
        public static final RotateInterceptor INSTANCE = new RotateInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) throws Exception {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                return holder.value().rotate(thisObj, args, superMethod);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run rotate", e);
                return superMethod.call();
            }
        }
    }

    public static class RandomTickInterceptor {
        public static final RandomTickInterceptor INSTANCE = new RandomTickInterceptor();

        @RuntimeType
        public void intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                holder.value().randomTick(thisObj, args, superMethod);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run randomTick", e);
            }
        }
    }

    public static class TickInterceptor {
        public static final TickInterceptor INSTANCE = new TickInterceptor();

        @RuntimeType
        public void intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                holder.value().tick(thisObj, args, superMethod);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run tick", e);
            }
        }
    }

    public static class OnPlaceInterceptor {
        public static final OnPlaceInterceptor INSTANCE = new OnPlaceInterceptor();

        @RuntimeType
        public void intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                holder.value().onPlace(thisObj, args, superMethod);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run onPlace", e);
            }
        }
    }

    public static class OnLandInterceptor {
        public static final OnLandInterceptor INSTANCE = new OnLandInterceptor();

        @RuntimeType
        public void intercept(@This Object thisObj, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                holder.value().onLand(thisObj, args);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run onLand", e);
            }
        }
    }

    public static class OnBrokenAfterFallInterceptor {
        public static final OnBrokenAfterFallInterceptor INSTANCE = new OnBrokenAfterFallInterceptor();

        @RuntimeType
        public void intercept(@This Object thisObj, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                holder.value().onBrokenAfterFall(thisObj, args);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run onBrokenAfterFall", e);
            }
        }
    }

    public static class CanSurviveInterceptor {
        public static final CanSurviveInterceptor INSTANCE = new CanSurviveInterceptor();

        @RuntimeType
        public boolean intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                return holder.value().canSurvive(thisObj, args, superMethod);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run canSurvive", e);
                return true;
            }
        }
    }

    public static class IsBoneMealSuccessInterceptor {
        public static final IsBoneMealSuccessInterceptor INSTANCE = new IsBoneMealSuccessInterceptor();

        @RuntimeType
        public boolean intercept(@This Object thisObj, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                return holder.value().isBoneMealSuccess(thisObj, args);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run isBoneMealSuccess", e);
                return true;
            }
        }
    }

    public static class IsValidBoneMealTargetInterceptor {
        public static final IsValidBoneMealTargetInterceptor INSTANCE = new IsValidBoneMealTargetInterceptor();

        @RuntimeType
        public boolean intercept(@This Object thisObj, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                return holder.value().isValidBoneMealTarget(thisObj, args);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run isValidBoneMealTarget", e);
                return true;
            }
        }
    }

    public static class PerformBoneMealInterceptor {
        public static final PerformBoneMealInterceptor INSTANCE = new PerformBoneMealInterceptor();

        @RuntimeType
        public void intercept(@This Object thisObj, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                holder.value().performBoneMeal(thisObj, args);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run performBoneMeal", e);
            }
        }
    }

    public static class NeighborChangedInterceptor {
        public static final NeighborChangedInterceptor INSTANCE = new NeighborChangedInterceptor();

        @RuntimeType
        public void intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                holder.value().neighborChanged(thisObj, args, superMethod);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run neighborChanged", e);
            }
        }
    }

    public static class OnExplosionHitInterceptor {
        public static final OnExplosionHitInterceptor INSTANCE = new OnExplosionHitInterceptor();

        @RuntimeType
        public void intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                holder.value().onExplosionHit(thisObj, args, superMethod);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run onExplosionHit", e);
            }
        }
    }

    public static class PickUpBlockInterceptor {
        public static final PickUpBlockInterceptor INSTANCE = new PickUpBlockInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                return holder.value().pickupBlock(thisObj, args, () -> CoreReflections.instance$ItemStack$EMPTY);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run pickupBlock", e);
                return CoreReflections.instance$ItemStack$EMPTY;
            }
        }
    }

    public static class PlaceLiquidInterceptor {
        public static final PlaceLiquidInterceptor INSTANCE = new PlaceLiquidInterceptor();

        @RuntimeType
        public boolean intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) throws Exception {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                return holder.value().placeLiquid(thisObj, args, superMethod);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run placeLiquid", e);
                return false;
            }
        }
    }

    public static class CanPlaceLiquidInterceptor {
        public static final CanPlaceLiquidInterceptor INSTANCE = new CanPlaceLiquidInterceptor();

        @RuntimeType
        public boolean intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                return holder.value().canPlaceLiquid(thisObj, args, superMethod);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run canPlaceLiquid", e);
                return false;
            }
        }
    }

    public static class GetDirectSignalInterceptor {
        public static final GetDirectSignalInterceptor INSTANCE = new GetDirectSignalInterceptor();

        @RuntimeType
        public int intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                return holder.value().getDirectSignal(thisObj, args, superMethod);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run getDirectSignal", e);
                return 0;
            }
        }
    }

    public static class GetSignalInterceptor {
        public static final GetSignalInterceptor INSTANCE = new GetSignalInterceptor();

        @RuntimeType
        public int intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                return holder.value().getSignal(thisObj, args, superMethod);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run getSignal", e);
                return 0;
            }
        }
    }

    public static class IsSignalSourceInterceptor {
        public static final IsSignalSourceInterceptor INSTANCE = new IsSignalSourceInterceptor();

        @RuntimeType
        public boolean intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                return holder.value().isSignalSource(thisObj, args, superMethod);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run isSignalSource", e);
                return false;
            }
        }
    }

    public static class AffectNeighborsAfterRemovalInterceptor {
        public static final AffectNeighborsAfterRemovalInterceptor INSTANCE = new AffectNeighborsAfterRemovalInterceptor();

        @RuntimeType
        public void intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                holder.value().affectNeighborsAfterRemoval(thisObj, args, superMethod);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run affectNeighborsAfterRemoval", e);
            }
        }
    }

    public static class EntityInsideInterceptor {
        public static final EntityInsideInterceptor INSTANCE = new EntityInsideInterceptor();

        @RuntimeType
        public void intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            try {
                holder.value().entityInside(thisObj, args, superMethod);
            } catch (Exception e) {
                CraftEngine.instance().logger().severe("Failed to run entityInside", e);
            }
        }
    }
}
