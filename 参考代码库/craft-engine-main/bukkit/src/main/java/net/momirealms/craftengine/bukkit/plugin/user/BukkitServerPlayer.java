package net.momirealms.craftengine.bukkit.plugin.user;

import com.google.common.collect.Lists;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.gui.CraftEngineInventoryHolder;
import net.momirealms.craftengine.bukkit.plugin.network.payload.DiscardedPayload;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MAttributeHolders;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MMobEffects;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.block.BlockSettings;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.entity.player.GameMode;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.CooldownData;
import net.momirealms.craftengine.core.plugin.network.ConnectionState;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.ProtocolVersion;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldEvents;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitServerPlayer extends Player {
    private final BukkitCraftEngine plugin;
    // handshake
    private ProtocolVersion protocolVersion = ProtocolVersion.UNKNOWN;
    // connection state
    private final Channel channel;
    private ChannelHandler connection;
    private String name;
    private UUID uuid;
    private ConnectionState decoderState;
    private ConnectionState encoderState;
    private final Set<UUID> resourcePackUUID = Collections.synchronizedSet(new HashSet<>());
    private boolean sentResourcePack = !Config.sendPackOnJoin();
    // some references
    private Reference<org.bukkit.entity.Player> playerRef;
    private Reference<Object> serverPlayerRef;
    // client side dimension info
    private int sectionCount;
    private Key clientSideDimension;
    // check main hand/offhand interaction
    private int lastSuccessfulInteraction;
    // re-sync attribute timely to prevent some bugs
    private long lastAttributeSyncTime;
    // for breaking blocks
    private int lastSentState = -1;
    private int lastHitBlockTime;
    private BlockPos destroyPos;
    private Object destroyedState;
    private boolean isDestroyingBlock;
    private boolean isDestroyingCustomBlock;
    private boolean swingHandAck;
    private float miningProgress;
    // for client visual sync
    private int resentSoundTick;
    private int resentSwingTick;
    // cache used recipe
    private Key lastUsedRecipe = null;
    // has fabric client mod or not
    private boolean hasClientMod = false;
    // cache if player can break blocks
    private boolean clientSideCanBreak = true;
    // prevent AFK players from consuming too much CPU resource on predicting
    private Location previousEyeLocation;
    // a cooldown for better breaking experience
    private int lastSuccessfulBreak;
    // player's game tick
    private int gameTicks;
    // cache interaction range here
    private int lastUpdateInteractionRangeTick;
    private double cachedInteractionRange;
    // cooldown data
    private CooldownData cooldownData;

    private final Map<Integer, EntityPacketHandler> entityTypeView = new ConcurrentHashMap<>();

    public BukkitServerPlayer(BukkitCraftEngine plugin, Channel channel) {
        this.channel = channel;
        this.plugin = plugin;
        for (String name : channel.pipeline().names()) {
            ChannelHandler handler = channel.pipeline().get(name);
            if (NetworkReflections.clazz$Connection.isInstance(handler)) {
                this.connection = handler;
                break;
            }
        }
    }

    public void setPlayer(org.bukkit.entity.Player player) {
        this.playerRef = new WeakReference<>(player);
        this.serverPlayerRef = new WeakReference<>(FastNMS.INSTANCE.method$CraftPlayer$getHandle(player));
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        byte[] bytes = player.getPersistentDataContainer().get(KeyUtils.toNamespacedKey(CooldownData.COOLDOWN_KEY), PersistentDataType.BYTE_ARRAY);
        try {
            this.cooldownData = CooldownData.fromBytes(bytes);
        } catch (IOException e) {
            this.cooldownData = new CooldownData();
            this.plugin.logger().warn("Failed to parse cooldown data", e);
        }
    }

    @Override
    public Channel nettyChannel() {
        return this.channel;
    }

    @Override
    public CraftEngine plugin() {
        return this.plugin;
    }

    @Override
    public boolean isMiningBlock() {
        return this.destroyPos != null;
    }

    public void setDestroyedState(Object destroyedState) {
        this.destroyedState = destroyedState;
    }

    public void setDestroyPos(BlockPos destroyPos) {
        this.destroyPos = destroyPos;
    }

    @Override
    public boolean shouldSyncAttribute() {
        long current = gameTicks();
        if (current - this.lastAttributeSyncTime > 100) {
            this.lastAttributeSyncTime = current;
            return true;
        }
        return false;
    }

    @Override
    public boolean isSneaking() {
        return platformPlayer().isSneaking();
    }

    @Override
    public GameMode gameMode() {
        return switch (platformPlayer().getGameMode()) {
            case CREATIVE -> GameMode.CREATIVE;
            case SPECTATOR -> GameMode.SPECTATOR;
            case ADVENTURE -> GameMode.ADVENTURE;
            case SURVIVAL -> GameMode.SURVIVAL;
        };
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        platformPlayer().setGameMode(Objects.requireNonNull(org.bukkit.GameMode.getByValue(gameMode.id())));
    }

    @Override
    public boolean canBreak(BlockPos pos, @Nullable Object state) {
        return AdventureModeUtils.canBreak(platformPlayer().getInventory().getItemInMainHand(), new Location(platformPlayer().getWorld(), pos.x(), pos.y(), pos.z()), state);
    }

    @Override
    public boolean canPlace(BlockPos pos, @Nullable Object state) {
        return AdventureModeUtils.canPlace(platformPlayer().getInventory().getItemInMainHand(), new Location(platformPlayer().getWorld(), pos.x(), pos.y(), pos.z()), state);
    }

    @Override
    public void sendActionBar(Component text) {
        Object packet = FastNMS.INSTANCE.constructor$ClientboundActionBarPacket(ComponentUtils.adventureToMinecraft(text));
        sendPacket(packet, false);
    }

    @Override
    public void sendTitle(Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        Object titlePacket = FastNMS.INSTANCE.constructor$ClientboundSetTitleTextPacket(ComponentUtils.adventureToMinecraft(title));
        Object subtitlePacket = FastNMS.INSTANCE.constructor$ClientboundSetSubtitleTextPacket(ComponentUtils.adventureToMinecraft(subtitle));
        Object timePacket = FastNMS.INSTANCE.constructor$ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut);
        sendPackets(List.of(titlePacket, subtitlePacket, timePacket), false);
    }

    @Override
    public void sendMessage(Component text, boolean overlay) {
        Object packet = FastNMS.INSTANCE.constructor$ClientboundSystemChatPacket(ComponentUtils.adventureToMinecraft(text), overlay);
        sendPacket(packet, false);
    }

    @Override
    public boolean updateLastSuccessfulInteractionTick(int tick) {
        if (this.lastSuccessfulInteraction != tick) {
            this.lastSuccessfulInteraction = tick;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int lastSuccessfulInteractionTick() {
        return this.lastSuccessfulInteraction;
    }

    @Override
    public int gameTicks() {
        return this.gameTicks;
    }

    @Override
    public void swingHand(InteractionHand hand) {
        platformPlayer().swingHand(hand == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND);
    }

    @Override
    public boolean hasPermission(String permission) {
        return platformPlayer().hasPermission(permission);
    }

    @Override
    public boolean canInstabuild() {
        try {
            Object abilities = CoreReflections.field$Player$abilities.get(serverPlayer());
            return (boolean) CoreReflections.field$Abilities$instabuild.get(abilities);
        } catch (ReflectiveOperationException e) {
            CraftEngine.instance().logger().warn("Failed to get canInstabuild for " + name(), e);
            return false;
        }
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        if (this.name != null) return;
        this.name = name;
    }

    @Override
    public UUID uuid() {
        return this.uuid;
    }

    @Override
    public void setUUID(UUID uuid) {
        if (this.uuid != null) return;
        this.uuid = uuid;
    }

    @Override
    public void playSound(Key sound, SoundSource source, float volume, float pitch) {
        platformPlayer().playSound(platformPlayer(), sound.toString(), SoundUtils.toBukkit(source), volume, pitch);
    }

    @Override
    public void playSound(Key sound, BlockPos blockPos, SoundSource source, float volume, float pitch) {
        platformPlayer().playSound(new Location(null, blockPos.x() + 0.5, blockPos.y() + 0.5, blockPos.z() + 0.5), sound.toString(), SoundUtils.toBukkit(source), volume, pitch);
    }

    @Override
    public void giveItem(Item<?> item) {
        PlayerUtils.giveItem(platformPlayer(), (ItemStack) item.getItem(), item.count());
    }

    @Override
    public void closeInventory() {
        platformPlayer().closeInventory();
    }

    @Override
    public void sendPacket(Object packet, boolean immediately) {
        this.plugin.networkManager().sendPacket(this, packet, immediately);
    }

    @Override
    public void sendCustomPayload(Key channel, byte[] data) {
        try {
            Object channelKey = KeyUtils.toResourceLocation(channel);
            Object dataPayload;
            if (DiscardedPayload.useNewMethod) {
                dataPayload = NetworkReflections.constructor$DiscardedPayload.newInstance(channelKey, data);
            } else {
                dataPayload = NetworkReflections.constructor$DiscardedPayload.newInstance(channelKey, Unpooled.wrappedBuffer(data));
            }
            Object responsePacket = NetworkReflections.constructor$ClientboundCustomPayloadPacket.newInstance(dataPayload);
            this.sendPacket(responsePacket, true);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to send custom payload to " + name(), e);
        }
    }

    @Override
    public void kick(Component message) {
        try {
            Object reason = ComponentUtils.adventureToMinecraft(message);
            Object kickPacket = NetworkReflections.constructor$ClientboundDisconnectPacket.newInstance(reason);
            this.sendPacket(kickPacket, true);
            this.nettyChannel().disconnect();
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to kick " + name(), e);
        }
    }

    @Override
    public void sendPackets(List<Object> packet, boolean immediately) {
        this.plugin.networkManager().sendPackets(this, packet, immediately);
    }

    @Override
    public void simulatePacket(Object packet) {
        this.plugin.networkManager().simulatePacket(this, packet);
    }

    @Override
    public ConnectionState decoderState() {
        return decoderState;
    }

    @Override
    public ConnectionState encoderState() {
        return encoderState;
    }

    @Override
    public int clientSideSectionCount() {
        return sectionCount;
    }

    public void setClientSideSectionCount(int sectionCount) {
        this.sectionCount = sectionCount;
    }

    @Override
    public Key clientSideDimension() {
        return clientSideDimension;
    }

    public void setClientSideDimension(Key clientSideDimension) {
        this.clientSideDimension = clientSideDimension;
    }

    public void setConnectionState(ConnectionState connectionState) {
        this.encoderState = connectionState;
        this.decoderState = connectionState;
    }

    public void setDecoderState(ConnectionState decoderState) {
        this.decoderState = decoderState;
    }

    public void setEncoderState(ConnectionState encoderState) {
        this.encoderState = encoderState;
    }

    @Override
    public void tick() {
        // not fully online
        if (serverPlayer() == null) return;
        if (VersionHelper.isFolia()) {
            try {
                Object serverPlayer = serverPlayer();
                Object gameMode = FastNMS.INSTANCE.field$ServerPlayer$gameMode(serverPlayer);
                this.gameTicks = (int) CoreReflections.field$ServerPlayerGameMode$gameTicks.get(gameMode);
            } catch (ReflectiveOperationException e) {
                CraftEngine.instance().logger().warn("Failed to get game tick for " + name(), e);
            }
        } else {
            this.gameTicks = FastNMS.INSTANCE.field$MinecraftServer$currentTick();
        }
        if (this.gameTicks % 30 == 0) {
            this.updateGUI();
        }
        if (this.isDestroyingBlock)  {
            this.tickBlockDestroy();
        }
        if (Config.predictBreaking() && !this.isDestroyingCustomBlock) {
            // if it's not destroying blocks, we do predict
            if ((gameTicks() + entityID()) % Config.predictBreakingInterval() == 0) {
                Location eyeLocation = platformPlayer().getEyeLocation();
                if (eyeLocation.equals(this.previousEyeLocation)) {
                    return;
                }
                this.previousEyeLocation = eyeLocation;
                this.predictNextBlockToMine();
            }
        }
    }

    private void updateGUI() {
        org.bukkit.inventory.Inventory top = !VersionHelper.isOrAbove1_21() ? LegacyInventoryUtils.getTopInventory(platformPlayer()) : platformPlayer().getOpenInventory().getTopInventory();
        if (top.getHolder() instanceof CraftEngineInventoryHolder holder) {
            holder.gui().onTimer();
        }
    }

    @Override
    public float getDestroyProgress(Object blockState, BlockPos pos) {
        return FastNMS.INSTANCE.method$BlockStateBase$getDestroyProgress(blockState, serverPlayer(), FastNMS.INSTANCE.field$CraftWorld$ServerLevel(platformPlayer().getWorld()), LocationUtils.toBlockPos(pos));
    }

    private void predictNextBlockToMine() {
        double range = getCachedInteractionRange() + Config.extendedInteractionRange();
        RayTraceResult result = platformPlayer().rayTraceBlocks(range, FluidCollisionMode.NEVER);
        if (result == null) {
            if (!this.clientSideCanBreak) {
                setClientSideCanBreakBlock(true);
            }
            return;
        }
        Block hitBlock = result.getHitBlock();
        if (hitBlock == null) {
            if (!this.clientSideCanBreak) {
                setClientSideCanBreakBlock(true);
            }
            return;
        }
        ImmutableBlockState nextBlock = CraftEngineBlocks.getCustomBlockState(hitBlock);
        if (nextBlock == null) {
            if (!this.clientSideCanBreak) {
                setClientSideCanBreakBlock(true);
            }
        } else {
            if (this.clientSideCanBreak) {
                setClientSideCanBreakBlock(false);
            }
        }
    }

    public void startMiningBlock(BlockPos pos, Object state, @Nullable ImmutableBlockState immutableBlockState) {
        // instant break
        boolean custom = immutableBlockState != null;
        if (custom && getDestroyProgress(state, pos) >= 1f) {
            BlockStateWrapper vanillaBlockState = immutableBlockState.vanillaBlockState();
            // if it's not an instant break on client side, we should resend level event
            if (vanillaBlockState != null && getDestroyProgress(vanillaBlockState.handle(), pos) < 1f) {
                Object levelEventPacket = FastNMS.INSTANCE.constructor$ClientboundLevelEventPacket(
                        WorldEvents.BLOCK_BREAK_EFFECT, LocationUtils.toBlockPos(pos), BlockStateUtils.blockStateToId(state), false);
                sendPacket(levelEventPacket, false);
            }
            return;
        }
        if (!custom && !this.clientSideCanBreak && getDestroyProgress(state, pos) >= 1f) {
            Object levelEventPacket = FastNMS.INSTANCE.constructor$ClientboundLevelEventPacket(
                    WorldEvents.BLOCK_BREAK_EFFECT, LocationUtils.toBlockPos(pos), BlockStateUtils.blockStateToId(state), false);
            sendPacket(levelEventPacket, false);
        }
        // if it's a custom one, we prevent it, otherwise we allow it
        setClientSideCanBreakBlock(!custom);
        // set some base info
        setDestroyPos(pos);
        setDestroyedState(state);
        setIsDestroyingBlock(true, custom);
    }

    @Override
    public void setClientSideCanBreakBlock(boolean canBreak) {
        try {
            if (this.clientSideCanBreak == canBreak && !shouldSyncAttribute()) {
                return;
            }
            this.clientSideCanBreak = canBreak;
            if (canBreak) {
                if (VersionHelper.isOrAbove1_20_5()) {
                    Object serverPlayer = serverPlayer();
                    Object attributeInstance = CoreReflections.methodHandle$ServerPlayer$getAttributeMethod.invokeExact(serverPlayer, MAttributeHolders.BLOCK_BREAK_SPEED);
                    Object newPacket = NetworkReflections.methodHandle$ClientboundUpdateAttributesPacket0Constructor.invokeExact(entityID(), (List<?>) Lists.newArrayList(attributeInstance));
                    sendPacket(newPacket, true);
                } else {
                    resetEffect(MMobEffects.MINING_FATIGUE);
                    resetEffect(MMobEffects.HASTE);
                }
            } else {
                if (VersionHelper.isOrAbove1_20_5()) {
                    Object attributeModifier = VersionHelper.isOrAbove1_21() ?
                            CoreReflections.constructor$AttributeModifier.newInstance(KeyUtils.toResourceLocation(Key.DEFAULT_NAMESPACE, "custom_hardness"), -9999d, CoreReflections.instance$AttributeModifier$Operation$ADD_VALUE) :
                            CoreReflections.constructor$AttributeModifier.newInstance(UUID.randomUUID(), Key.DEFAULT_NAMESPACE + ":custom_hardness", -9999d, CoreReflections.instance$AttributeModifier$Operation$ADD_VALUE);
                    Object attributeSnapshot = NetworkReflections.constructor$ClientboundUpdateAttributesPacket$AttributeSnapshot.newInstance(MAttributeHolders.BLOCK_BREAK_SPEED, 1d, Lists.newArrayList(attributeModifier));
                    Object newPacket = NetworkReflections.constructor$ClientboundUpdateAttributesPacket1.newInstance(entityID(), Lists.newArrayList(attributeSnapshot));
                    sendPacket(newPacket, true);
                } else {
                    Object fatiguePacket = MobEffectUtils.createPacket(MMobEffects.MINING_FATIGUE, entityID(), (byte) 9, -1, false, false, false);
                    Object hastePacket = MobEffectUtils.createPacket(MMobEffects.HASTE, entityID(), (byte) 0, -1, false, false, false);
                    sendPackets(List.of(fatiguePacket, hastePacket), true);
                }
            }
        } catch (Throwable e) {
            plugin.logger().warn("Failed to set attribute for player " + platformPlayer().getName(), e);
        }
    }

    @Override
    public void stopMiningBlock() {
        setClientSideCanBreakBlock(true);
        setIsDestroyingBlock(false, false);
    }

    @Override
    public void preventMiningBlock() {
        setClientSideCanBreakBlock(false);
        setIsDestroyingBlock(false, false);
        abortMiningBlock();
    }

    @Override
    public void abortMiningBlock() {
        this.swingHandAck = false;
        this.miningProgress = 0;
        BlockPos pos = this.destroyPos;
        if (pos != null && this.isDestroyingCustomBlock) {
            // 只纠正自定义方块的
            this.broadcastDestroyProgress(platformPlayer(), pos, LocationUtils.toBlockPos(pos), -1);
        }
    }

    private void resetEffect(Object mobEffect) throws ReflectiveOperationException {
        Object effectInstance = CoreReflections.method$ServerPlayer$getEffect.invoke(serverPlayer(), mobEffect);
        Object packet;
        if (effectInstance != null) {
            packet = NetworkReflections.constructor$ClientboundUpdateMobEffectPacket.newInstance(entityID(), effectInstance);
        } else {
            packet = NetworkReflections.constructor$ClientboundRemoveMobEffectPacket.newInstance(entityID(), mobEffect);
        }
        sendPacket(packet, true);
    }

    private void tickBlockDestroy() {
        // if player swings hand is this tick
        if (!this.swingHandAck) return;
        this.swingHandAck = false;
        int currentTick = gameTicks();
        // optimize break speed, otherwise it would be too fast
        if (currentTick - this.lastSuccessfulBreak <= 5) return;
        Object destroyedState = this.destroyedState;
        if (destroyedState == null) return;
        try {
            org.bukkit.entity.Player player = platformPlayer();
            double range = getCachedInteractionRange();
            RayTraceResult result = player.rayTraceBlocks(range, FluidCollisionMode.NEVER);
            if (result == null) return;
            Block hitBlock = result.getHitBlock();
            if (hitBlock == null) return;
            Location location = hitBlock.getLocation();
            BlockPos hitPos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            if (!hitPos.equals(this.destroyPos)) {
                return;
            }
            Object blockPos = LocationUtils.toBlockPos(hitPos);
            Object serverPlayer = serverPlayer();

            // send hit sound if the sound is removed
            if (currentTick - this.lastHitBlockTime > 3) {
                Object blockOwner = FastNMS.INSTANCE.method$BlockState$getBlock(destroyedState);
                Object soundType = CoreReflections.field$BlockBehaviour$soundType.get(blockOwner);
                Object soundEvent = CoreReflections.field$SoundType$hitSound.get(soundType);
                Object soundId = FastNMS.INSTANCE.field$SoundEvent$location(soundEvent);
                player.playSound(location, soundId.toString(), SoundCategory.BLOCKS, 0.5F, 0.5F);
                this.lastHitBlockTime = currentTick;
            }

            // accumulate progress (custom blocks only)
            if (this.isDestroyingCustomBlock) {
                // prevent server from taking over breaking custom blocks
                Object gameMode = FastNMS.INSTANCE.field$ServerPlayer$gameMode(serverPlayer);
                CoreReflections.field$ServerPlayerGameMode$isDestroyingBlock.set(gameMode, false);
                // check item in hand
                Item<ItemStack> item = this.getItemInHand(InteractionHand.MAIN_HAND);
                if (item != null) {
                    Material itemMaterial = item.getItem().getType();
                    // creative mode + invalid item in hand
                    if (canInstabuild() && (itemMaterial == Material.DEBUG_STICK
                            || itemMaterial == Material.TRIDENT
                            || (VersionHelper.isOrAbove1_20_5() && itemMaterial == MaterialUtils.MACE)
                            || item.is(ItemTags.SWORDS))) {
                        return;
                    }
                }

                float progressToAdd = getDestroyProgress(destroyedState, hitPos);
                Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(destroyedState);
                // double check custom block
                if (optionalCustomState.isPresent()) {
                    ImmutableBlockState customState = optionalCustomState.get();
                    BlockSettings blockSettings = customState.settings();
                    if (blockSettings.requireCorrectTool()) {
                        if (item != null) {
                            // it's correct on plugin side
                            if (blockSettings.isCorrectTool(item.id())) {
                                // but not on serverside
                                if (!FastNMS.INSTANCE.method$ItemStack$isCorrectToolForDrops(item.getLiteralObject(), destroyedState)) {
                                    // we fix the speed
                                    progressToAdd = progressToAdd * (10f / 3f);
                                }
                            } else {
                                // not a correct tool on plugin side and not a correct tool on serverside
                                if (!blockSettings.respectToolComponent() || !FastNMS.INSTANCE.method$ItemStack$isCorrectToolForDrops(item.getLiteralObject(), destroyedState)) {
                                    progressToAdd = progressToAdd * (10f / 3f) * blockSettings.incorrectToolSpeed();
                                }
                            }
                        } else {
                            // item is null, but it requires correct tool, then we reset the speed
                            progressToAdd = progressToAdd * (10f / 3f) * blockSettings.incorrectToolSpeed();
                        }
                    }

                    // accumulate progress
                    this.miningProgress = progressToAdd + miningProgress;
                    int packetStage = (int) (this.miningProgress * 10.0F);
                    if (packetStage != this.lastSentState) {
                        this.lastSentState = packetStage;
                        // broadcast changes
                        broadcastDestroyProgress(player, hitPos, blockPos, packetStage);
                    }

                    // can break now
                    if (this.miningProgress >= 1f) {
                        // for simplified adventure break, switch mayBuild temporarily
                        if (isAdventureMode() && Config.simplifyAdventureBreakCheck()) {
                            // check the appearance state
                            if (canBreak(hitPos, customState.vanillaBlockState().handle())) {
                                // Error might occur so we use try here
                                try {
                                    FastNMS.INSTANCE.field$Player$mayBuild(serverPlayer, true);
                                    CoreReflections.method$ServerPlayerGameMode$destroyBlock.invoke(gameMode, blockPos);
                                } finally {
                                    FastNMS.INSTANCE.field$Player$mayBuild(serverPlayer, false);
                                }
                            }
                        } else {
                            // normal break check
                            CoreReflections.method$ServerPlayerGameMode$destroyBlock.invoke(gameMode, blockPos);
                        }
                        // send break particle + (removed sounds)
                        sendPacket(FastNMS.INSTANCE.constructor$ClientboundLevelEventPacket(WorldEvents.BLOCK_BREAK_EFFECT, blockPos, customState.customBlockState().registryId(), false), false);
                        this.lastSuccessfulBreak = currentTick;
                        this.destroyPos = null;
                        this.setIsDestroyingBlock(false, false);
                    }
                }
            }
        } catch (Exception e) {
            plugin.logger().warn("Failed to tick destroy for player " + platformPlayer().getName(), e);
        }
    }

    @Override
    public void breakBlock(int x, int y, int z) {
        platformPlayer().breakBlock(new Location(platformPlayer().getWorld(), x, y, z).getBlock());
    }

    private void broadcastDestroyProgress(org.bukkit.entity.Player player, BlockPos hitPos, Object blockPos, int stage) {
        Object packet = FastNMS.INSTANCE.constructor$ClientboundBlockDestructionPacket(Integer.MAX_VALUE - entityID(), blockPos, stage);
        for (org.bukkit.entity.Player other : player.getWorld().getPlayers()) {
            Location otherLocation = other.getLocation();
            double d0 = (double) hitPos.x() - otherLocation.getX();
            double d1 = (double) hitPos.y() - otherLocation.getY();
            double d2 = (double) hitPos.z() - otherLocation.getZ();
            if (d0 * d0 + d1 * d1 + d2 * d2 < 1024.0D) {
                FastNMS.INSTANCE.method$Connection$send(
                        FastNMS.INSTANCE.field$ServerGamePacketListenerImpl$connection(
                                FastNMS.INSTANCE.field$Player$connection(
                                        FastNMS.INSTANCE.method$CraftPlayer$getHandle(player)
                                )
                        ),
                        packet
                );
            }
        }
    }

    @Override
    public double getCachedInteractionRange() {
        if (this.lastUpdateInteractionRangeTick + 20 > gameTicks()) {
            return this.cachedInteractionRange;
        }
        this.cachedInteractionRange = FastNMS.INSTANCE.method$Player$getInteractionRange(serverPlayer());
        this.lastUpdateInteractionRangeTick = gameTicks();
        return this.cachedInteractionRange;
    }

    public void setIsDestroyingBlock(boolean is, boolean custom) {
        this.miningProgress = 0;
        this.isDestroyingBlock = is;
        if (is) {
            this.swingHandAck = true;
            this.isDestroyingCustomBlock = custom;
        } else {
            this.swingHandAck = false;
            this.destroyedState = null;
            if (this.destroyPos != null) {
                // 只纠正自定义方块的
                if (this.isDestroyingCustomBlock) {
                    this.broadcastDestroyProgress(platformPlayer(), this.destroyPos, LocationUtils.toBlockPos(this.destroyPos), -1);
                }
                this.destroyPos = null;
            }
            this.isDestroyingCustomBlock = false;
        }
    }

    @Override
    public void onSwingHand() {
        this.swingHandAck = true;
    }

    @Override
    public int entityID() {
        return platformPlayer().getEntityId();
    }

    @Override
    public boolean isOnline() {
        org.bukkit.entity.Player player = platformPlayer();
        if (player == null) return false;
        return player.isOnline();
    }

    @Override
    public float yRot() {
        return platformPlayer().getPitch();
    }

    @Override
    public float xRot() {
        return platformPlayer().getYaw();
    }

    @Override
    public boolean isSecondaryUseActive() {
        return isSneaking();
    }

    @Override
    public Direction getDirection() {
        return DirectionUtils.toDirection(platformPlayer().getFacing());
    }

    @Nullable
    @Override
    public Item<ItemStack> getItemInHand(InteractionHand hand) {
        PlayerInventory inventory = platformPlayer().getInventory();
        return BukkitItemManager.instance().wrap(hand == InteractionHand.MAIN_HAND ? inventory.getItemInMainHand() : inventory.getItemInOffHand());
    }

    @Override
    public World world() {
        return new BukkitWorld(platformPlayer().getWorld());
    }

    @Override
    public double x() {
        return platformPlayer().getX();
    }

    @Override
    public double y() {
        return platformPlayer().getY();
    }

    @Override
    public double z() {
        return platformPlayer().getZ();
    }

    @Override
    public Object serverPlayer() {
        if (serverPlayerRef == null) return null;
        return serverPlayerRef.get();
    }

    @Override
    public org.bukkit.entity.Player platformPlayer() {
        if (playerRef == null) return null;
        return playerRef.get();
    }

    @Override
    public ChannelHandler connection() {
        if (this.connection == null) {
            Object serverPlayer = serverPlayer();
            if (serverPlayer != null) {
                this.connection = (ChannelHandler) FastNMS.INSTANCE.field$ServerGamePacketListenerImpl$connection(
                        FastNMS.INSTANCE.field$Player$connection(serverPlayer)
                );
            } else {
                throw new IllegalStateException("Cannot init or find connection instance for player " + name());
            }
        }
        return this.connection;
    }

    @Override
    public org.bukkit.entity.Player literalObject() {
        return platformPlayer();
    }

    @Override
    public Map<Integer, EntityPacketHandler> entityPacketHandlers() {
        return this.entityTypeView;
    }

    public void setResendSound() {
        resentSoundTick = gameTicks();
    }

    public void setResendSwing() {
        resentSwingTick = gameTicks();
    }

    public boolean shouldResendSound() {
        return resentSoundTick == gameTicks();
    }

    public boolean shouldResendSwing() {
        return resentSwingTick == gameTicks();
    }

    public Key lastUsedRecipe() {
        return lastUsedRecipe;
    }

    public void setLastUsedRecipe(Key lastUsedRecipe) {
        this.lastUsedRecipe = lastUsedRecipe;
    }

    public boolean clientModEnabled() {
        return this.hasClientMod;
    }

    public void setClientModState(boolean enable) {
        this.hasClientMod = enable;
    }

    @Override
    public void addResourcePackUUID(UUID uuid) {
        if (VersionHelper.isOrAbove1_20_3()) {
            this.resourcePackUUID.add(uuid);
        }
    }

    @Override
    public ProtocolVersion protocolVersion() {
        return this.protocolVersion;
    }

    @Override
    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = ProtocolVersion.getById(protocolVersion);
    }

    @Override
    public boolean sentResourcePack() {
        return this.sentResourcePack;
    }

    @Override
    public void setSentResourcePack(boolean sentResourcePack) {
        this.sentResourcePack = sentResourcePack;
    }

    @Override
    public void clearView() {
        this.entityTypeView.clear();
    }

    @Override
    public void unloadCurrentResourcePack() {
        if (!VersionHelper.isOrAbove1_20_3()) {
            return;
        }
        if (decoderState() == ConnectionState.PLAY && !this.resourcePackUUID.isEmpty()) {
            for (UUID u : this.resourcePackUUID) {
                sendPacket(FastNMS.INSTANCE.constructor$ClientboundResourcePackPopPacket(u), true);
            }
            this.resourcePackUUID.clear();
        }
    }

    @Override
    public void performCommand(String command) {
        platformPlayer().performCommand(command);
    }

    @Override
    public double luck() {
        if (VersionHelper.isOrAbove1_21_3()) {
            return Optional.ofNullable(platformPlayer().getAttribute(Attribute.LUCK)).map(AttributeInstance::getValue).orElse(1d);
        } else {
            return LegacyAttributeUtils.getLuck(platformPlayer());
        }
    }

    @Override
    public boolean isFlying() {
        return platformPlayer().isFlying();
    }

    @Override
    public int foodLevel() {
        return platformPlayer().getFoodLevel();
    }

    @Override
    public void setFoodLevel(int foodLevel) {
        this.platformPlayer().setFoodLevel(Math.min(Math.max(0, foodLevel), 20));
    }

    @Override
    public float saturation() {
        return platformPlayer().getSaturation();
    }

    @Override
    public void setSaturation(float saturation) {
        this.platformPlayer().setSaturation(saturation);
    }

    @Override
    public void addPotionEffect(Key potionEffectType, int duration, int amplifier, boolean ambient, boolean particles) {
        PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(KeyUtils.toNamespacedKey(potionEffectType));
        if (type == null) return;
        this.platformPlayer().addPotionEffect(new PotionEffect(type, duration, amplifier, ambient, particles));
    }

    @Override
    public void removePotionEffect(Key potionEffectType) {
        PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(KeyUtils.toNamespacedKey(potionEffectType));
        if (type == null) return;
        this.platformPlayer().removePotionEffect(type);
    }

    @Override
    public void clearPotionEffects() {
        this.platformPlayer().clearActivePotionEffects();
    }

    @Override
    public CooldownData cooldown() {
        return this.cooldownData;
    }
}
