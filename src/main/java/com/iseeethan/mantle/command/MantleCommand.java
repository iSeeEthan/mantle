package com.iseeethan.mantle.command;

import com.iseeethan.mantle.world.Locator;
import com.iseeethan.mantle.world.MantleWorld;
import com.iseeethan.mantle.world.gen.biome.MantleBiomeClassifier;
import com.iseeethan.mantle.world.gen.biome.MantleBiomes;
import com.iseeethan.mantle.world.gen.tectonics.Climate;
import com.iseeethan.mantle.world.gen.tectonics.Flora;
import com.iseeethan.mantle.world.gen.tectonics.PlateSim;
import com.iseeethan.mantle.world.gen.tectonics.Soil;
import com.iseeethan.mantle.world.gen.tectonics.Strata;
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

        root.then(Commands.literal("info").executes(MantleCommand::runInfo));

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

    private static int runInfo(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayer();
        int wx = player != null ? player.getBlockX() : (int) src.getPosition().x;
        int wz = player != null ? player.getBlockZ() : (int) src.getPosition().z;

        MantleWorld world = MantleWorld.get();
        if (!world.inWorld(wx, wz)) {
            src.sendFailure(Component.literal("Outside the Mantle world bounds.").withStyle(ChatFormatting.RED));
            return 0;
        }

        PlateSim sim = world.sim();
        int surfaceY = world.solidTopY(wx, wz);
        boolean ocean = world.isOcean(wx, wz);

        Strata.Rock rock = surfaceY >= MantleWorld.SEA_Y ? world.rockAt(wx, wz, surfaceY - 1) : null;
        Soil.Sample soil = sim.soil().sample(wx, wz, surfaceY, new Soil.Sample());
        Climate climate = sim.climate();
        double rain = climate.rainfall(wx, wz);
        double temp = climate.temperature(wx, wz);
        double slope = sim.macroSlope(wx, wz);
        int flow = sim.flowAccumAt(wx, wz);
        double erod = sim.erodibilityAt(wx, wz);

        Flora.Cover cover = sim.flora().cover(wx, wz, surfaceY, soil, rain, temp, slope,
                Math.min(1.0, flow / 80.0), new Flora.Cover());
        MantleBiomes biome = new MantleBiomeClassifier(sim, MantleWorld.SEA_Y, 4).classify(wx, wz);

        src.sendSuccess(() -> header("Mantle info @ " + wx + ", " + surfaceY + ", " + wz), false);
        src.sendSuccess(() -> line("Biome", pretty(biome.name())), false);
        src.sendSuccess(() -> line("Surface", surfaceY + (ocean ? " (ocean floor)" : "")), false);
        src.sendSuccess(() -> line("Rock", rock != null ? pretty(rock.name()) : "submerged"), false);
        src.sendSuccess(() -> line("Soil", pretty(soil.type.name()) + ", depth " + soil.depth
                + ", wetness " + pct(soil.wetness)), false);
        src.sendSuccess(() -> line("Climate", "rain " + pct(rain) + ", temp " + pct(temp)), false);
        src.sendSuccess(() -> line("Erosion", "slope " + fmt(slope) + ", flow " + flow
                + ", erodibility " + fmt(erod)), false);
        src.sendSuccess(() -> line("Vegetation", pretty(cover.biome.name()) + ", trees "
                + pct(cover.treeDensity) + ", grass " + pct(cover.grassDensity)
                + (cover.tree != Flora.TreeKind.NONE ? ", " + pretty(cover.tree.name()) : "")), false);
        src.sendSuccess(() -> line("Tectonics", tectonics(sim, wx, wz)), false);
        return 1;
    }

    private static String tectonics(PlateSim sim, int wx, int wz) {
        StringBuilder sb = new StringBuilder();
        sb.append(pretty(sim.boundaryTypeAt(wx, wz))).append(" boundary ")
          .append((int) sim.boundaryDistAt(wx, wz)).append("m");
        if (sim.isMountainBelt(wx, wz)) sb.append(", mountain belt");
        if (sim.isRift(wx, wz)) sb.append(", rift");
        if (sim.isFoldRidge(wx, wz)) sb.append(", fold ridge");
        if (sim.isFaultScarp(wx, wz)) sb.append(", fault scarp");
        return sb.toString();
    }

    private static MutableComponent header(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    private static MutableComponent line(String label, String value) {
        return Component.literal(label + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    private static String pretty(String enumName) {
        String s = enumName.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String pct(double v) {
        return Math.round(v * 100) + "%";
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.ROOT, "%.2f", v);
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
