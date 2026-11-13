package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.gamerules.GameRules;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeWrapper;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageEndpoint;

public final class RemoteCraftingUpgradeContainer extends CraftingUpgradeContainer {
    public static final UpgradeContainerType<CraftingUpgradeWrapper, CraftingUpgradeContainer> TYPE =
            new UpgradeContainerType<>(RemoteCraftingUpgradeContainer::new);
    private static final String RECIPE = "remoteCraftingRecipe";
    private static final String MENU = "remoteCraftingMenu";
    private static final String PRIORITY = "remoteCraftingPriority";
    private static final String JEI_REQUEST = "remoteCraftingJeiRequest";
    private static final String JEI_MAX_TRANSFER = "remoteCraftingJeiMaxTransfer";
    private static final String MESSAGE = "message.sophisticated_backpacks_digital_storages_expansion.crafting.";

    private Identifier selectedRecipe;
    private static final String CATALOG = "remoteCraftingCatalog";
    private final java.util.List<CraftingCatalogPayload.Entry> catalog = new java.util.ArrayList<>();
    private long catalogRequest;
    private long lastCatalogTick = Long.MIN_VALUE;
    private java.util.function.Consumer<java.util.List<CraftingCatalogPayload.Entry>> catalogConsumer = ignored -> {};

    public java.util.List<CraftingCatalogPayload.Entry> getCatalog() { return java.util.List.copyOf(catalog); }

    public void requestCatalog(java.util.function.Consumer<java.util.List<CraftingCatalogPayload.Entry>> consumer) {
        catalogConsumer = consumer;
        catalogRequest = java.util.concurrent.ThreadLocalRandom.current().nextLong();
        CompoundTag data = request();
        data.putLong(CATALOG, catalogRequest);
        sendDataToServer(() -> data);
    }

    public void receiveCatalog(CraftingCatalogPayload payload) {
        if (!player.level().isClientSide() || payload.request() != catalogRequest) return;
        if (payload.first()) catalog.clear();
        catalog.addAll(payload.entries());
        catalogConsumer.accept(getCatalog());
    }

    private void sendCatalog(ServerPlayer player, long requestId) {
        long tick = player.level().getGameTime();
        if (lastCatalogTick != Long.MIN_VALUE && tick - lastCatalogTick < 10) return;
        lastCatalogTick = tick;
        var displayContext = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(player.level());
        var entries = player.level().getServer().getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.value() instanceof CraftingRecipe recipe && RemoteCraftingRecipes.isSupported(recipe))
                .filter(holder -> !player.level().getGameRules().get(GameRules.LIMITED_CRAFTING) || player.getRecipeBook().contains(holder.id()))
                .map(holder -> new CraftingCatalogPayload.Entry(holder.id().identifier(), holder.value().display().stream()
                        .map(display -> display.result().resolveForFirstStack(displayContext)).filter(stack -> !stack.isEmpty()).findFirst().orElse(net.minecraft.world.item.ItemStack.EMPTY)))
                .filter(entry -> !entry.result().isEmpty() && entry.result().isItemEnabled(player.level().enabledFeatures()))
                .sorted(java.util.Comparator.comparing(entry -> entry.id().toString())).toList();
        for (int offset = 0; offset < Math.max(1, entries.size()); offset += CraftingCatalogPayload.PAGE_SIZE) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new CraftingCatalogPayload(
                    player.containerMenu.containerId, getUpgradeContainerId(), requestId, offset == 0,
                    entries.subList(offset, Math.min(offset + CraftingCatalogPayload.PAGE_SIZE, entries.size()))));
        }
    }

    private RemoteCraftingUpgradeContainer(Player player, int id, CraftingUpgradeWrapper wrapper,
            UpgradeContainerType<CraftingUpgradeWrapper, CraftingUpgradeContainer> type) {
        super(player, id, wrapper, type);
    }

    private RemoteCraftingUpgradeWrapper remoteWrapper() {
        return (RemoteCraftingUpgradeWrapper) getUpgradeWrapper();
    }

    public Optional<StorageEndpoint> getEndpoint() {
        return remoteWrapper().getEndpoint();
    }

    public SourcePriority getSourcePriority() {
        return remoteWrapper().getSourcePriority();
    }

    public void toggleSourcePriority() {
        SourcePriority priority = getSourcePriority().next();
        remoteWrapper().setSourcePriority(priority);
        CompoundTag data = request();
        data.putBoolean(PRIORITY, priority == SourcePriority.BACKPACK_FIRST);
        sendDataToServer(() -> data);
    }

    public Identifier getSelectedRecipe() {
        return selectedRecipe;
    }

    public void selectRemoteRecipe(Identifier recipeId) {
        selectedRecipe = recipeId;
    }

    public void requestIngredients() {
        if (selectedRecipe != null) {
            CompoundTag data = request();
            data.putString(RECIPE, selectedRecipe.toString());
            sendDataToServer(() -> data);
        }
    }

    public void requestJeiIngredients(Identifier recipeId, boolean maxTransfer) {
        selectedRecipe = recipeId;
        CompoundTag data = request();
        data.putString(RECIPE, recipeId.toString());
        data.putBoolean(JEI_REQUEST, true);
        data.putBoolean(JEI_MAX_TRANSFER, maxTransfer);
        sendDataToServer(() -> data);
    }

    private CompoundTag request() {
        CompoundTag data = new CompoundTag();
        data.putInt(MENU, player.containerMenu.containerId);
        return data;
    }

    @Override
    public void handlePacket(CompoundTag data) {
        if (!data.contains(RECIPE) && !data.contains(PRIORITY) && !data.contains(CATALOG)) {
            super.handlePacket(data);
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !serverPlayer.isAlive() || serverPlayer.isSpectator()
                || !com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.NbtChecks.has(data, MENU, net.minecraft.nbt.Tag.TAG_INT)
                || data.getIntOr(MENU, 0) != player.containerMenu.containerId || !remoteWrapper().isEnabled()) {
            return;
        }
        UpgradeOperationContext.fromCurrentMenu(serverPlayer, this).ifPresent(context -> {
            if (data.getLong(CATALOG).isPresent()) {
                sendCatalog(serverPlayer, data.getLongOr(CATALOG, 0L));
                return;
            }
            if (com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.NbtChecks.has(data, PRIORITY, net.minecraft.nbt.Tag.TAG_BYTE)) {
                remoteWrapper().setSourcePriority(data.getBooleanOr(PRIORITY, false)
                        ? SourcePriority.BACKPACK_FIRST : SourcePriority.NETWORK_FIRST);
            } else if (com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.NbtChecks.has(data, RECIPE, net.minecraft.nbt.Tag.TAG_STRING)
                    && remoteWrapper().beginFill(serverPlayer.level().getGameTime())) {
                boolean fromJei = com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.NbtChecks.has(data, JEI_REQUEST, net.minecraft.nbt.Tag.TAG_BYTE)
                        && data.getBooleanOr(JEI_REQUEST, false);
                boolean maxTransfer = fromJei
                        && com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.NbtChecks.has(data, JEI_MAX_TRANSFER, net.minecraft.nbt.Tag.TAG_BYTE)
                        && data.getBooleanOr(JEI_MAX_TRANSFER, false);
                if (!fromJei || remoteWrapper().getEndpoint().isPresent()) {
                    fill(context, data.getStringOr(RECIPE, ""), fromJei, maxTransfer);
                }
            }
        });
    }

    private void fill(UpgradeOperationContext context, String recipeId, boolean fromJei, boolean maxTransfer) {
        Identifier id = recipeId.length() <= 512 ? Identifier.tryParse(recipeId) : null;
        ServerPlayer serverPlayer = context.player();
        if (id == null) {
            feedback(serverPlayer, "unavailable");
            return;
        }
        var level = serverPlayer.level();
        var recipeKey = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id);
        var entry = level.getServer().getRecipeManager().byKey(recipeKey).orElse(null);
        if (!(entry != null && entry.value() instanceof CraftingRecipe recipe) || !RemoteCraftingRecipes.isSupported(recipe)
                || (level.getGameRules().get(GameRules.LIMITED_CRAFTING) && !serverPlayer.getRecipeBook().contains(entry.id()))) {
            feedback(serverPlayer, "unavailable");
            return;
        }
        var output = recipe.display().stream().map(display -> display.result().resolveForFirstStack(
                net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(level)))
                .filter(stack -> !stack.isEmpty()).findFirst().orElse(net.minecraft.world.item.ItemStack.EMPTY);
        if (output.isEmpty() || !output.isItemEnabled(level.enabledFeatures())) {
            feedback(serverPlayer, "unavailable");
            return;
        }
        int missing = RemoteCraftingFill.fill(context, remoteWrapper(), getCraftMatrix(), recipe, fromJei, maxTransfer);
        if (missing < 0) {
            feedback(serverPlayer, "incompatible");
        } else if (missing == 0) {
            setRecipeUsed(recipeKey);
            feedback(serverPlayer, "ready");
        } else {
            feedback(serverPlayer, "missing", missing);
        }
    }

    private static void feedback(ServerPlayer player, String message, Object... arguments) {
        player.sendSystemMessage(Component.translatable(MESSAGE + message, arguments), true);
    }
}
