package com.dagonjira.entity;

import com.github.alexthe666.citadel.animation.Animation;
import net.mcreator.benstitans.DagonzillaEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

/**
 * Dagonjira - a smaller, slightly weaker, slightly slower Dagonzilla.
 *
 * <p>All of the AI, multipart tail, beam, nuclear pulse, stomp / scratch / tail / bite animations and
 * sounds are inherited from Ben's Titans' {@link DagonzillaEntity}. This class only changes:
 * <ul>
 *   <li>stats (see {@link #createDagonjiraAttributes()})</li>
 *   <li>size ({@link #getScale()}: 1.5 blocks shorter than Dagonzilla, proportionally narrower)</li>
 *   <li>damage dealt ({@link #DAMAGE_MULTIPLIER}, applied in {@code DagonjiraEvents})</li>
 *   <li>client-side flame particles for the orange thermonuclear power-up and the atomic breath</li>
 *   <li>public helpers used by the {@code /dagonjira} command</li>
 * </ul>
 */
public class DagonjiraEntity extends DagonzillaEntity {

    /** Dagonzilla is 11.7 blocks tall; Dagonjira is 10.2 (1.5 blocks shorter). Width scales by the same factor. */
    public static final float SCALE = 10.2F / 11.7F;

    /** Everything Dagonjira damages (melee, tail, stomp, beam) is multiplied by this. */
    public static final float DAMAGE_MULTIPLIER = 0.8F;

    public DagonjiraEntity(EntityType<? extends DagonjiraEntity> type, Level level) {
        super(type, level);
    }

    /**
     * Dagonzilla: speed 0.36, health 500, armor 7.5, attack 30, follow range 192, knockback resistance 1, swim speed 3.5
     * Dagonjira : a little slower, with less health, a little less armor and attack damage.
     */
    public static AttributeSupplier.Builder createDagonjiraAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.MAX_HEALTH, 400.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 192.0D)
                .add(Attributes.ATTACK_DAMAGE, 24.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(ForgeMod.SWIM_SPEED.get(), 3.0D);
    }

    @Override
    public float getScale() {
        // Ben's Titans' getScale() returns 0.15 for babies and 1.0 otherwise; Entity dimensions, the beam origin
        // (getBeamShootFrom) and our renderer all go through getScale(), so one multiplier resizes everything.
        return super.getScale() * SCALE;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.clientFlameEffects();
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Client-only flame effects (vanilla particles, so no extra registrations are needed)
    // ------------------------------------------------------------------------------------------------------------
    private void clientFlameEffects() {
        // Thermonuclear power-up: embers licking off the spine
        if (this.getNuclearStacks() > 0 && this.tickCount % 2 == 0) {
            for (int i = 0; i < 3; i++) {
                double angle = this.random.nextDouble() * Math.PI * 2.0D;
                double radius = (1.2D + this.random.nextDouble() * 1.4D) * SCALE;
                double x = this.getX() + Math.cos(angle) * radius;
                double z = this.getZ() + Math.sin(angle) * radius;
                double y = this.getY() + (3.0D + this.random.nextDouble() * 6.0D) * SCALE;
                this.level().addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.06D, 0.0D);
                if (this.random.nextInt(4) == 0) {
                    this.level().addParticle(ParticleTypes.LAVA, x, y, z, 0.0D, 0.0D, 0.0D);
                }
            }
        }

        // Atomic breath: a trail of fire + smoke along the beam and a burst where it lands
        float progress = this.getBeamProgress(1.0F);
        if (progress > 0.2F) {
            Vec3 from = this.getBeamShootFrom(1.0F);
            Vec3 to = this.getClientBeamEndPosition(1.0F);
            if (to != null) {
                Vec3 delta = to.subtract(from);
                double length = delta.length();
                if (length > 1.0D) {
                    for (int i = 0; i < 6; i++) {
                        // squared random keeps most flames near the mouth where the beam is hottest
                        double t = Math.min(1.0D, Math.pow(this.random.nextDouble(), 2.0D) * Math.min(1.0D, 70.0D / length));
                        Vec3 p = from.add(delta.scale(t));
                        double spread = 0.5D + t * 1.2D;
                        this.level().addParticle(ParticleTypes.FLAME,
                                p.x + this.random.nextGaussian() * spread,
                                p.y + this.random.nextGaussian() * spread,
                                p.z + this.random.nextGaussian() * spread,
                                delta.x / length * 0.1D, delta.y / length * 0.1D + 0.02D, delta.z / length * 0.1D);
                    }
                    if (this.tickCount % 2 == 0) {
                        double t = this.random.nextDouble() * Math.min(1.0D, 50.0D / length);
                        Vec3 p = from.add(delta.scale(t));
                        this.level().addParticle(ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 0.0D, 0.05D, 0.0D);
                    }
                }
                for (int i = 0; i < 5; i++) {
                    this.level().addParticle(ParticleTypes.FLAME,
                            to.x + this.random.nextGaussian() * 1.5D,
                            to.y + this.random.nextDouble() * 1.5D,
                            to.z + this.random.nextGaussian() * 1.5D,
                            this.random.nextGaussian() * 0.15D, 0.12D + this.random.nextDouble() * 0.15D, this.random.nextGaussian() * 0.15D);
                }
                if (this.tickCount % 3 == 0) {
                    this.level().addParticle(ParticleTypes.LAVA, to.x, to.y + 0.5D, to.z, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability helpers used by /dagonjira (server side). They drive exactly the same fields / animations Ben's
    // Titans' own AI (DagonzillaAttackGoal) uses, so everything the mob normally does is reachable by command.
    // ------------------------------------------------------------------------------------------------------------

    /** Plays one of the inherited animations (roar, scratch, tail whip, stomp, bite, trample, speak). */
    public void playAbility(Animation animation) {
        this.setAnimation(animation);
    }

    /** Charges and fires the atomic breath (same trigger the attack goal uses). */
    public void startBeam() {
        this.wantsToUseBeam = true;
        this.setAnimation(ANIMATION_PREPARE_BREATH);
    }

    /** Starts the nuclear pulse shockwave. */
    public void startPulse() {
        this.wantsToUsePulse = true;
        this.pulseCooldown = 0;
        this.setAnimation(ANIMATION_NUCLEAR_PULSE);
    }

    /** Cancels any pending / running beam, charge or pulse. */
    public void stopAbilities() {
        this.setFiring(false);
        this.setCharge(0);
        this.wantsToUseBeam = false;
        this.wantsToUsePulse = false;
        this.setAnimation(NO_ANIMATION);
    }
}
