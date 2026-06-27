package com.iseeethan.mantle.command;

import com.iseeethan.mantle.world.Locator;
import com.iseeethan.mantle.world.MantleWorld;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class MantleCommand {

    private MantleCommand() {}

    private static final Locator.Target[] TARGETS = Locator.Target.values();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("mantle");

        LiteralArgumentBuilder<CommandSourceStack> locate = Commands.literal("locate");
        for (Locator.Target t : TARGETS) {
            locate.then(Commands.literal(name(t)).executes(ctx -> runLocate(ctx, t)));
        }
        root.then(locate);

        LiteralArgumentBuilder<CommandSourceStack> tp = Commands.literal("tp");
        tp.then(Commands.literal("spawn").executes(MantleCommand::tpSpawn));
        tp.then(Commands.literal("coast").executes(MantleCommand::tpCoast));
        tp.then(Commands.argument("x", IntegerArgumentType.integer())
                .then(Commands.argument("z", IntegerArgumentType.integer())
                        .executes(MantleCommand::tpCoords)));
        root.then(tp);

        dispatcher.register(root);
    }

    private static String name(Locator.Target t) {
        return t.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static int runLocate(CommandContext<CommandSourceStack> ctx, Locator.Target target) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayer();
        int fromX = player != null ? player.getBlockX() : (int) src.getPosition().x;
        int fromZ = player != null ? player.getBlockZ() : (int) src.getPosition().z;

        MantleWorld world = MantleWorld.get();
        Locator locator = new Locator(world);
        Locator.Found found = locator.locate(target, fromX, fromZ);

        if (found == null) {
            src.sendFailure(Component.literal("No " + name(target) + " found.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        int dist = (int) Math.round(Math.sqrt(
                (double) (found.x - fromX) * (found.x - fromX)
                        + (double) (found.z - fromZ) * (found.z - fromZ)));

        MutableComponent msg = Component.literal("Nearest " + name(target) + ": ")
                .withStyle(ChatFormatting.GRAY)
                .append(coordLink(found.x, found.y, found.z))
                .append(Component.literal(" (" + dist + " blocks away)").withStyle(ChatFormatting.DARK_GRAY));
        src.sendSuccess(() -> msg, false);
        return 1;
    }

    private static MutableComponent coordLink(int x, int y, int z) {
        String tpCmd = "/tp @s " + x + " " + y + " " + z;
        Style style = Style.EMPTY
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, tpCmd))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Click to fill teleport command")));
        return Component.literal("[" + x + ", " + y + ", " + z + "]").withStyle(style);
    }

    private static int tpSpawn(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        ServerLevel level = player.serverLevel();
        net.minecraft.core.BlockPos spawn = level.getSharedSpawnPos();
        return teleport(ctx, player, spawn.getX(), spawn.getZ());
    }

    private static int tpCoast(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        Locator locator = new Locator(MantleWorld.get());
        Locator.Found f = locator.findCoastalSpawn();
        if (f == null) {
            ctx.getSource().sendFailure(Component.literal("No coast found.").withStyle(ChatFormatting.RED));
            return 0;
        }
        return teleportTo(ctx, player, f.x, f.y, f.z);
    }

    private static int tpCoords(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        int x = IntegerArgumentType.getInteger(ctx, "x");
        int z = IntegerArgumentType.getInteger(ctx, "z");
        return teleport(ctx, player, x, z);
    }

    private static int teleport(CommandContext<CommandSourceStack> ctx, ServerPlayer player, int x, int z) {
        MantleWorld world = MantleWorld.get();
        int y = Math.max(world.solidTopY(x, z) + 1, MantleWorld.SEA_Y + 1);
        return teleportTo(ctx, player, x, y, z);
    }

    private static int teleportTo(CommandContext<CommandSourceStack> ctx, ServerPlayer player, int x, int y, int z) {
        ServerLevel level = player.serverLevel();
        player.teleportTo(level, x + 0.5, y, z + 0.5, player.getYRot(), player.getXRot());
        ctx.getSource().sendSuccess(() -> Component.literal("Teleported to ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal("[" + x + ", " + y + ", " + z + "]").withStyle(ChatFormatting.GREEN)), false);
        return 1;
    }
}
