package com.dagonjira.command;

import com.dagonjira.DagonjiraMod;
import com.dagonjira.entity.DagonjiraEntity;
import com.github.alexthe666.citadel.animation.Animation;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.mcreator.benstitans.DagonzillaEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * /dagonjira ...  (permission level 2)
 *
 * Every ability acts on the nearest Dagonjira within 128 blocks of the command source, or on the Dagonjiras in an
 * optional trailing entity selector, e.g. "/dagonjira beam" or "/dagonjira roar @e[type=dagonjira:dagonjira]".
 */
@Mod.EventBusSubscriber(modid = DagonjiraMod.MODID)
public class DagonjiraCommands {
    private static final double SEARCH_RADIUS = 128.0D;

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("dagonjira").requires(s -> s.hasPermission(2));

        root.then(Commands.literal("spawn").executes(DagonjiraCommands::spawn));

        // --- vocal ---
        root.then(ability("speak", "Dagonjira speaks", e -> e.playAbility(DagonzillaEntity.ANIMATION_SPEAK)));
        root.then(ability("roar", "Dagonjira roars", e -> e.playAbility(DagonzillaEntity.ANIMATION_ROAR_1)));
        root.then(ability("roar2", "Dagonjira lets out its long roar", e -> e.playAbility(DagonzillaEntity.ANIMATION_ROAR_2)));

        // --- thermonuclear breath / pulse ---
        root.then(ability("beam", "Dagonjira charges its atomic breath", DagonjiraEntity::startBeam));
        root.then(ability("pulse", "Dagonjira charges a nuclear pulse", DagonjiraEntity::startPulse));
        root.then(ability("stop", "Dagonjira stands down", DagonjiraEntity::stopAbilities));

        // --- melee ---
        root.then(side("scratch", "scratches", DagonzillaEntity.ANIMATION_LEFT_SCRATCH, DagonzillaEntity.ANIMATION_RIGHT_SCRATCH));
        root.then(side("tail", "whips its tail", DagonzillaEntity.ANIMATION_LEFT_TAIL, DagonzillaEntity.ANIMATION_RIGHT_TAIL));
        root.then(side("stomp", "stomps", DagonzillaEntity.ANIMATION_LEFT_STOMP, DagonzillaEntity.ANIMATION_RIGHT_STOMP));
        root.then(ability("bite", "Dagonjira bites", e -> e.playAbility(DagonzillaEntity.ANIMATION_BITE)));
        root.then(ability("trample", "Dagonjira tramples", e -> e.playAbility(DagonzillaEntity.ANIMATION_TRAMPLE)));

        // --- thermonuclear power-up ---
        root.then(Commands.literal("thermonuclear")
                .then(Commands.argument("stacks", IntegerArgumentType.integer(0, 20))
                        .executes(ctx -> run(ctx, null, e -> e.setNuclearStacks(IntegerArgumentType.getInteger(ctx, "stacks")),
                                "Thermonuclear stacks set to " + IntegerArgumentType.getInteger(ctx, "stacks")))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(ctx -> run(ctx, EntityArgument.getEntities(ctx, "targets"),
                                        e -> e.setNuclearStacks(IntegerArgumentType.getInteger(ctx, "stacks")),
                                        "Thermonuclear stacks set to " + IntegerArgumentType.getInteger(ctx, "stacks"))))));
        root.then(Commands.literal("charge")
                .then(Commands.argument("amount", IntegerArgumentType.integer(0, DagonzillaEntity.MAX_CHARGE))
                        .executes(ctx -> run(ctx, null, e -> e.setCharge(IntegerArgumentType.getInteger(ctx, "amount")),
                                "Charge set to " + IntegerArgumentType.getInteger(ctx, "amount")))));

        // --- utility ---
        root.then(Commands.literal("target")
                .then(Commands.argument("victim", EntityArgument.entity())
                        .executes(ctx -> {
                            Entity victim = EntityArgument.getEntity(ctx, "victim");
                            if (!(victim instanceof LivingEntity living)) {
                                ctx.getSource().sendFailure(Component.literal("Target must be a living entity"));
                                return 0;
                            }
                            return run(ctx, null, e -> e.setTarget(living), "Dagonjira is now hunting " + victim.getName().getString());
                        })));
        root.then(ability("calm", "Dagonjira calms down", e -> {
            e.setTarget(null);
            e.stopAbilities();
        }));
        root.then(ability("heal", "Dagonjira healed", e -> e.setHealth(e.getMaxHealth())));
        root.then(Commands.literal("info").executes(ctx -> {
            List<DagonjiraEntity> found = find(ctx.getSource());
            if (found.isEmpty()) {
                return none(ctx);
            }
            DagonjiraEntity e = found.get(0);
            ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                    "Dagonjira: %.0f/%.0f HP, %.1f attack, speed %.2f, height %.1f blocks, thermonuclear stacks %d",
                    e.getHealth(), e.getMaxHealth(), e.getAttributeValue(Attributes.ATTACK_DAMAGE),
                    e.getAttributeValue(Attributes.MOVEMENT_SPEED), e.getBbHeight(), e.getNuclearStacks())), false);
            return 1;
        }));

        d.register(root);
    }

    // ------------------------------------------------------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> ability(String name, String message, Consumer<DagonjiraEntity> action) {
        return Commands.literal(name)
                .executes(ctx -> run(ctx, null, action, message))
                .then(Commands.argument("targets", EntityArgument.entities())
                        .executes(ctx -> run(ctx, EntityArgument.getEntities(ctx, "targets"), action, message)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> side(String name, String verb, Animation left, Animation right) {
        return Commands.literal(name)
                .then(ability("left", "Dagonjira " + verb + " (left)", e -> e.playAbility(left)))
                .then(ability("right", "Dagonjira " + verb + " (right)", e -> e.playAbility(right)));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, java.util.Collection<? extends Entity> selected,
                           Consumer<DagonjiraEntity> action, String message) {
        List<DagonjiraEntity> targets = new ArrayList<>();
        if (selected == null) {
            List<DagonjiraEntity> nearest = find(ctx.getSource());
            if (!nearest.isEmpty()) {
                targets.add(nearest.get(0));
            }
        } else {
            for (Entity e : selected) {
                if (e instanceof DagonjiraEntity dj) {
                    targets.add(dj);
                }
            }
        }
        if (targets.isEmpty()) {
            return none(ctx);
        }
        for (DagonjiraEntity e : targets) {
            action.accept(e);
        }
        ctx.getSource().sendSuccess(() -> Component.literal(message), true);
        return targets.size();
    }

    private static int none(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendFailure(Component.literal("No Dagonjira found within " + (int) SEARCH_RADIUS + " blocks!"));
        return 0;
    }

    private static List<DagonjiraEntity> find(CommandSourceStack source) {
        Vec3 pos = source.getPosition();
        List<DagonjiraEntity> list = new ArrayList<>(source.getLevel().getEntitiesOfClass(DagonjiraEntity.class,
                new AABB(pos, pos).inflate(SEARCH_RADIUS)));
        list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(pos)));
        return list;
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 pos = ctx.getSource().getPosition();
        DagonjiraEntity e = DagonjiraMod.DAGONJIRA.get().create(level);
        if (e == null) {
            return 0;
        }
        e.moveTo(pos.x, pos.y, pos.z, ctx.getSource().getRotation().y, 0.0F);
        level.addFreshEntity(e);
        ctx.getSource().sendSuccess(() -> Component.literal("Dagonjira has arrived"), true);
        return 1;
    }
}
