#!/usr/bin/env node
/**
 * Exports all 15 Riftworld v0.47.0 `.bbmodel` creatures via Blockbench MCP
 * (`bbmodel_export_modded_entity`, Mojmap 1.17+ / 1.20.1, `flip_y: true`) into
 * `mod/src/main/java/ru/aiforge/riftworld/client/model/fauna/*Model.java`
 * with full multi-state biomechanical `setupAnim` implementations.
 */

import fs from 'node:fs';
import path from 'node:path';
import { BlockbenchMcpSession } from './builder_lib.mjs';

const OUT_DIR = path.resolve('mod/src/main/java/ru/aiforge/riftworld/client/model/fauna');
fs.mkdirSync(OUT_DIR, { recursive: true });

const ANIM_BODIES = {
  porcelain_crane: `
		resetToBindPose();
		float pt = ageInTicks - entity.tickCount;
		float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
		float yaw = Mth.clamp(netHeadYaw, -50.0F, 50.0F);
		float pitch = Mth.clamp(headPitch, -30.0F, 30.0F);
		float dance = entity.getDanceProgress(pt);
		float flutter = entity.getFlutterProgress(pt);
		float breath = Mth.sin(ageInTicks * 0.07F);

		// Graceful crane stride
		float w = limbSwing * 0.62F;
		float legSwingL = Mth.cos(w) * 34.0F * move * (1.0F - 0.65F * flutter);
		float legSwingR = Mth.cos(w + (float) Math.PI) * 34.0F * move * (1.0F - 0.65F * flutter);
		addBbDeg(this.leg_l, legSwingL + flutter * 22.0F, 0.0F, 0.0F);
		addBbDeg(this.leg_r, legSwingR + flutter * 22.0F, 0.0F, 0.0F);
		addBbPos(this.body, 0.0F, Math.abs(Mth.sin(w)) * 1.1F * move, 0.0F);

		// S-neck idle poise, walking nod, and porcelain flower bow dance
		float walkNod = Mth.cos(w * 2.0F) * 4.5F * move;
		float bowNeck = dance * 42.0F;
		float bowHead = -dance * 28.0F + Mth.sin(ageInTicks * 0.35F) * 6.0F * dance;
		addBbDeg(this.neck_base, -pitch * 0.35F + breath * 1.6F + walkNod + bowNeck, yaw * 0.45F, 0.0F);
		addBbDeg(this.head, -pitch * 0.45F - walkNod * 0.7F + bowHead, yaw * 0.45F, 0.0F);

		// Wings: folded at rest, graceful half-fan in dance, broad crane beats in startled flutter
		float flap = Mth.sin(ageInTicks * 0.58F) * 44.0F * flutter;
		float danceSpread = dance * 26.0F;
		addBbDeg(this.wing_l, dance * -10.0F, danceSpread * 0.4F, -(danceSpread + flap + flutter * 24.0F));
		addBbDeg(this.wing_r, dance * -10.0F, -danceSpread * 0.4F, (danceSpread + flap + flutter * 24.0F));
		addBbDeg(this.tail_base, breath * 1.8F - dance * 14.0F, Mth.sin(ageInTicks * 0.08F) * 3.0F, 0.0F);
`,

  basalt_beetle: `
		resetToBindPose();
		float pt = ageInTicks - entity.tickCount;
		float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
		float ram = entity.getRamProgress(pt);
		float munch = entity.getMunchTicks() > 0 ? 1.0F : 0.0F;

		// Alternating insect tripod gait: (FL, MR, BL) vs (FR, ML, BR)
		float w = limbSwing * 0.95F;
		float sA = Mth.cos(w) * 24.0F * move;
		float sB = Mth.cos(w + (float) Math.PI) * 24.0F * move;
		float lA = Math.max(0.0F, Mth.sin(w)) * 16.0F * move;
		float lB = Math.max(0.0F, Mth.sin(w + (float) Math.PI)) * 16.0F * move;

		addBbDeg(this.leg_front_l, 0.0F, sA, -lA);
		addBbDeg(this.leg_mid_r, 0.0F, -sA, lA);
		addBbDeg(this.leg_back_l, 0.0F, sA, -lA);

		addBbDeg(this.leg_front_r, 0.0F, -sB, lB);
		addBbDeg(this.leg_mid_l, 0.0F, sB, -lB);
		addBbDeg(this.leg_back_r, 0.0F, -sB, lB);

		addBbDeg(this.body, Mth.sin(ageInTicks * 0.06F) * 0.8F - ram * 6.0F, 0.0F, Mth.cos(w) * 2.2F * move);

		// Rhino-horn head ram + coal munching mandibles + feeler antennae
		float chew = munch * Mth.sin(ageInTicks * 0.85F) * 14.0F;
		float idlePinch = Mth.sin(ageInTicks * 0.09F) * 3.5F;
		addBbDeg(this.head, -Mth.clamp(headPitch, -20.0F, 20.0F) * 0.35F + ram * 26.0F + munch * Mth.sin(ageInTicks * 0.45F) * 4.0F,
				Mth.clamp(netHeadYaw, -25.0F, 25.0F) * 0.4F, 0.0F);
		addBbDeg(this.mandible_l, 0.0F, idlePinch + chew + ram * 12.0F, 0.0F);
		addBbDeg(this.mandible_r, 0.0F, -(idlePinch + chew + ram * 12.0F), 0.0F);
		addBbDeg(this.antenna_l, Mth.sin(ageInTicks * 0.16F) * 6.0F, Mth.cos(ageInTicks * 0.12F) * 5.0F, 0.0F);
		addBbDeg(this.antenna_r, Mth.cos(ageInTicks * 0.16F) * 6.0F, -Mth.sin(ageInTicks * 0.12F) * 5.0F, 0.0F);
`,

  ash_moth: `
		resetToBindPose();
		boolean swoop = entity.getSwoopTicks() > 0;
		float freq = swoop ? 0.82F : 0.56F;
		float phase = ageInTicks * freq + (entity.getId() * 0.9F);
		float foreFlap = Mth.sin(phase) * (swoop ? 48.0F : 38.0F);
		float hindFlap = Mth.sin(phase - 0.48F) * (swoop ? 40.0F : 31.0F);

		addBbPos(this.thorax, 0.0F, Mth.cos(phase) * 1.25F, 0.0F);
		addBbDeg(this.thorax, (swoop ? 22.0F : 8.0F) + Mth.cos(phase) * 3.5F, 0.0F, 0.0F);
		addBbDeg(this.abdomen_base, -10.0F + Mth.sin(phase - 0.8F) * 7.5F, 0.0F, 0.0F);
		addBbDeg(this.head, -Mth.clamp(headPitch, -25.0F, 25.0F) * 0.4F, Mth.clamp(netHeadYaw, -35.0F, 35.0F) * 0.5F, 0.0F);

		addBbDeg(this.forewing_l, swoop ? 8.0F : 0.0F, 0.0F, -foreFlap);
		addBbDeg(this.forewing_r, swoop ? 8.0F : 0.0F, 0.0F, foreFlap);
		addBbDeg(this.hindwing_l, 0.0F, 0.0F, -hindFlap);
		addBbDeg(this.hindwing_r, 0.0F, 0.0F, hindFlap);

		addBbDeg(this.antenna_l, Mth.sin(ageInTicks * 0.28F) * 5.5F, 0.0F, -Mth.cos(ageInTicks * 0.22F) * 3.5F);
		addBbDeg(this.antenna_r, Mth.cos(ageInTicks * 0.28F) * 5.5F, 0.0F, Mth.sin(ageInTicks * 0.22F) * 3.5F);
		addBbPos(this.ash_motes_l, 0.0F, Mth.sin(ageInTicks * 0.14F) * 1.4F, 0.0F);
		addBbPos(this.ash_motes_r, 0.0F, Mth.cos(ageInTicks * 0.14F) * 1.4F, 0.0F);
`,

  amethyst_fox: `
		resetToBindPose();
		float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
		boolean sniffing = entity.getSniffTicks() > 0;
		float w = limbSwing * 0.72F;

		// Agile diagonal fox trot
		float dA = Mth.cos(w) * 34.0F * move;
		float dB = Mth.cos(w + (float) Math.PI) * 34.0F * move;
		addBbDeg(this.leg_front_l, dA, 0.0F, 0.0F);
		addBbDeg(this.leg_back_r, dA * 0.92F, 0.0F, 0.0F);
		addBbDeg(this.leg_front_r, dB, 0.0F, 0.0F);
		addBbDeg(this.leg_back_l, dB * 0.92F, 0.0F, 0.0F);
		addBbDeg(this.body, Mth.sin(w * 2.0F) * 2.2F * move, 0.0F, 0.0F);

		// Head look vs crystal-vein sniffing sweep
		float yaw = Mth.clamp(netHeadYaw, -45.0F, 45.0F);
		float pitch = Mth.clamp(headPitch, -25.0F, 25.0F);
		if (sniffing) {
			addBbDeg(this.head, 24.0F + Mth.sin(ageInTicks * 0.7F) * 4.5F, Mth.sin(ageInTicks * 0.42F) * 18.0F, 0.0F);
		} else {
			addBbDeg(this.head, -pitch * 0.55F + Mth.sin(ageInTicks * 0.06F) * 1.5F, yaw * 0.65F, 0.0F);
		}

		// Independent asymmetric ear flicks & crystal-plume tail sway
		addBbDeg(this.ear_l, Mth.sin(ageInTicks * 0.11F) * 4.0F, 0.0F, -Math.max(0.0F, Mth.sin(ageInTicks * 0.19F)) * 8.0F);
		addBbDeg(this.ear_r, Mth.cos(ageInTicks * 0.13F) * 4.0F, 0.0F, Math.max(0.0F, Mth.sin(ageInTicks * 0.23F + 1.4F)) * 8.0F);

		float tailFreq = sniffing ? 0.45F : (entity.hasTrustedPlayer() ? 0.20F : 0.10F);
		float tailAmp = sniffing ? 20.0F : 10.0F + move * 8.0F;
		addBbDeg(this.tail_base, -move * 12.0F + Mth.sin(ageInTicks * 0.07F) * 3.0F, Mth.sin(ageInTicks * tailFreq) * tailAmp, 0.0F);
`,

  sizo_leafwing: `
		resetToBindPose();
		float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
		boolean gliding = entity.isGliding();
		boolean sitting = entity.isInSittingPose();
		boolean gust = entity.getGustTicks() > 0;
		float w = limbSwing * 0.68F;

		if (sitting) {
			addBbPos(this.body, 0.0F, -5.0F, 0.0F);
			addBbDeg(this.body, -18.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_back_l, -42.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_back_r, -42.0F, 0.0F, 0.0F);
			addBbDeg(this.tail_base, 16.0F, Mth.sin(ageInTicks * 0.08F) * 5.0F, 0.0F);
		} else if (gliding) {
			float soarTremble = Mth.sin(ageInTicks * 0.28F) * 5.5F;
			addBbDeg(this.body, 14.0F, 0.0F, Mth.sin(ageInTicks * 0.10F) * 2.5F);
			addBbDeg(this.wing_l, 0.0F, 8.0F, -32.0F + soarTremble);
			addBbDeg(this.wing_r, 0.0F, -8.0F, 32.0F - soarTremble);
			addBbDeg(this.arm_l, -12.0F, 0.0F, -18.0F);
			addBbDeg(this.arm_r, -12.0F, 0.0F, 18.0F);
			addBbDeg(this.leg_back_l, 32.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_back_r, 32.0F, 0.0F, 0.0F);
			addBbDeg(this.tail_base, -10.0F, Mth.sin(ageInTicks * 0.14F) * 6.0F, 0.0F);
		} else {
			addBbDeg(this.leg_back_l, Mth.cos(w) * 32.0F * move, 0.0F, 0.0F);
			addBbDeg(this.leg_back_r, Mth.cos(w + (float) Math.PI) * 32.0F * move, 0.0F, 0.0F);
			addBbPos(this.body, 0.0F, Math.abs(Mth.sin(w)) * 1.0F * move, 0.0F);
			addBbDeg(this.wing_l, 0.0F, -6.0F, 18.0F - move * 8.0F);
			addBbDeg(this.wing_r, 0.0F, 6.0F, -18.0F + move * 8.0F);
			addBbDeg(this.tail_base, -move * 10.0F, Mth.sin(ageInTicks * 0.09F) * 6.5F, 0.0F);
		}

		if (gust) {
			float clap = Mth.sin(ageInTicks * 0.9F) * 46.0F;
			addBbDeg(this.wing_l, -14.0F, 0.0F, -clap);
			addBbDeg(this.wing_r, -14.0F, 0.0F, clap);
		}

		addBbDeg(this.head, -Mth.clamp(headPitch, -25.0F, 25.0F) * 0.5F + Mth.sin(ageInTicks * 0.07F) * 1.5F,
				Mth.clamp(netHeadYaw, -45.0F, 45.0F) * 0.6F, 0.0F);
		addBbDeg(this.antler_l, Mth.sin(ageInTicks * 0.08F) * 2.0F, 0.0F, 0.0F);
		addBbDeg(this.antler_r, Mth.sin(ageInTicks * 0.08F) * 2.0F, 0.0F, 0.0F);
`,

  azure_frog: `
		resetToBindPose();
		float pt = ageInTicks - entity.tickCount;
		float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
		boolean leaping = entity.isLeaping();
		float tongue = entity.getTongueProgress(pt);

		// Throat/collar vocal-sac breathing pulse
		float throatPulse = 0.5F + 0.5F * Mth.sin(ageInTicks * 0.16F);
		this.collar.xScale *= (1.0F + 0.06F * throatPulse + 0.10F * tongue);
		this.collar.yScale *= (1.0F + 0.08F * throatPulse + 0.12F * tongue);

		if (leaping) {
			addBbDeg(this.body, -24.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_back_l, 52.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_back_r, 52.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_front_l, -34.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_front_r, -34.0F, 0.0F, 0.0F);
		} else {
			float hop = limbSwing * 0.9F;
			float bounce = Math.abs(Mth.sin(hop)) * move;
			addBbPos(this.body, 0.0F, bounce * 2.2F, 0.0F);
			addBbDeg(this.body, -bounce * 12.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_back_l, bounce * 34.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_back_r, bounce * 34.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_front_l, -bounce * 22.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_front_r, -bounce * 22.0F, 0.0F, 0.0F);
		}

		addBbDeg(this.head, -Mth.clamp(headPitch, -20.0F, 20.0F) * 0.45F - tongue * 18.0F,
				Mth.clamp(netHeadYaw, -35.0F, 35.0F) * 0.5F, 0.0F);
		addBbPos(this.head, 0.0F, 0.0F, -tongue * 1.8F);
`,

  blossom_snail: `
		resetToBindPose();
		float pt = ageInTicks - entity.tickCount;
		float retract = entity.getRetractProgress(pt);
		float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F) * (1.0F - retract);

		// Gentle gastropod foot wave & shell rock
		float glide = ageInTicks * 0.12F;
		this.body.zScale *= (1.0F + Mth.sin(glide) * 0.035F * (0.4F + move) - 0.32F * retract);
		this.body.xScale *= (1.0F - Mth.sin(glide) * 0.025F * (0.4F + move) - 0.18F * retract);
		addBbDeg(this.shell, Mth.sin(glide * 0.8F) * 2.5F * (0.3F + move) + retract * 5.0F,
				0.0F, Mth.cos(glide * 0.7F) * 2.2F * move);
		addBbPos(this.shell, 0.0F, -retract * 1.8F, 0.0F);

		// Retract head, neck, and eyestalks cleanly into the blossom shell on hit
		float visScale = Math.max(0.08F, 1.0F - 0.88F * retract);
		this.neck.xScale *= visScale;
		this.neck.yScale *= visScale;
		this.neck.zScale *= visScale;
		addBbPos(this.neck, 0.0F, -retract * 2.5F, retract * 6.5F);
		addBbDeg(this.neck, retract * 24.0F + Mth.sin(glide) * 3.0F * (1.0F - retract),
				Mth.clamp(netHeadYaw, -25.0F, 25.0F) * 0.35F * (1.0F - retract), 0.0F);
		addBbDeg(this.head, -Mth.clamp(headPitch, -18.0F, 18.0F) * 0.35F * (1.0F - retract),
				Mth.clamp(netHeadYaw, -25.0F, 25.0F) * 0.35F * (1.0F - retract), 0.0F);

		addBbDeg(this.eyestalk_l, Mth.sin(ageInTicks * 0.14F) * 8.0F * (1.0F - retract), 0.0F,
				-Mth.cos(ageInTicks * 0.11F) * 5.0F * (1.0F - retract));
		addBbDeg(this.eyestalk_r, Mth.cos(ageInTicks * 0.15F) * 8.0F * (1.0F - retract), 0.0F,
				Mth.sin(ageInTicks * 0.12F) * 5.0F * (1.0F - retract));
`,

  phantom_wolf: `
		resetToBindPose();
		// Spectral translucency: ghostly in mist/dark, solidified in bright lantern light
		this.renderAlpha = entity.isRevealedByLight() ? 0.90F : 0.48F;

		float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
		boolean leaping = entity.getLeapTicks() > 0;
		boolean howling = entity.getHowlTicks() > 0;
		float w = limbSwing * 0.68F;

		if (leaping) {
			addBbDeg(this.body, -16.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_front_l, -42.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_front_r, -38.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_back_l, 44.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_back_r, 40.0F, 0.0F, 0.0F);
			addBbDeg(this.tail_base, -18.0F, 0.0F, 0.0F);
		} else {
			float dA = Mth.cos(w) * 36.0F * move;
			float dB = Mth.cos(w + (float) Math.PI) * 36.0F * move;
			addBbDeg(this.leg_front_l, dA, 0.0F, 0.0F);
			addBbDeg(this.leg_back_r, dA * 0.9F, 0.0F, 0.0F);
			addBbDeg(this.leg_front_r, dB, 0.0F, 0.0F);
			addBbDeg(this.leg_back_l, dB * 0.9F, 0.0F, 0.0F);
			addBbDeg(this.tail_base, -8.0F * move + Mth.sin(ageInTicks * 0.08F) * 3.0F,
					Mth.sin(ageInTicks * 0.12F) * (7.0F + 6.0F * move), 0.0F);
		}

		if (howling) {
			addBbDeg(this.neck, -26.0F, 0.0F, 0.0F);
			addBbDeg(this.head, -34.0F + Mth.sin(ageInTicks * 0.55F) * 3.0F, 0.0F, 0.0F);
		} else {
			addBbDeg(this.neck, 6.0F * move + Mth.sin(ageInTicks * 0.07F) * 1.5F,
					Mth.clamp(netHeadYaw, -40.0F, 40.0F) * 0.35F, 0.0F);
			addBbDeg(this.head, -Mth.clamp(headPitch, -25.0F, 25.0F) * 0.55F,
					Mth.clamp(netHeadYaw, -40.0F, 40.0F) * 0.45F, 0.0F);
		}
`,

  crystal_gecko: `
		resetToBindPose();
		this.renderAlpha = entity.isCamouflaged() ? 0.36F : 0.96F;

		float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
		boolean climbing = entity.isClimbingWall();
		boolean whip = entity.getTailWhipTicks() > 0;
		float w = limbSwing * 0.92F;

		if (climbing) {
			addBbDeg(this.root, 72.0F, 0.0F, 0.0F);
			addBbPos(this.root, 0.0F, 4.0F, -3.0F);
		}

		// Reptilian sprawling crawl & lateral S-body wave
		float bodyWave = Mth.sin(w) * 10.0F * move;
		addBbDeg(this.body, 0.0F, bodyWave, 0.0F);
		addBbDeg(this.head, -Mth.clamp(headPitch, -20.0F, 20.0F) * 0.45F,
				Mth.clamp(netHeadYaw, -40.0F, 40.0F) * 0.55F - bodyWave * 0.7F, 0.0F);

		float legA = Mth.cos(w) * 30.0F * move;
		float legB = Mth.cos(w + (float) Math.PI) * 30.0F * move;
		addBbDeg(this.leg_front_l, 0.0F, legA, -Math.max(0.0F, Mth.sin(w)) * 14.0F * move);
		addBbDeg(this.leg_back_r, 0.0F, -legA, Math.max(0.0F, Mth.sin(w)) * 14.0F * move);
		addBbDeg(this.leg_front_r, 0.0F, -legB, Math.max(0.0F, Mth.sin(w + (float) Math.PI)) * 14.0F * move);
		addBbDeg(this.leg_back_l, 0.0F, legB, -Math.max(0.0F, Mth.sin(w + (float) Math.PI)) * 14.0F * move);

		float whipSwing = whip ? Mth.sin(ageInTicks * 1.15F) * 48.0F : 0.0F;
		addBbDeg(this.tail_base, Mth.sin(ageInTicks * 0.07F) * 2.0F,
				-bodyWave * 1.6F + Mth.sin(ageInTicks * 0.09F) * 7.0F + whipSwing, 0.0F);
`,

  faceted_owl: `
		resetToBindPose();
		boolean flying = entity.isOwlFlying();
		boolean signaling = entity.getSignalTicks() > 0;

		// Iconic wide-angle owl head tracking + curious head-tilt
		float owlYaw = Mth.clamp(netHeadYaw, -75.0F, 75.0F);
		float owlPitch = Mth.clamp(headPitch, -30.0F, 30.0F);
		float curiousTilt = Mth.sin(ageInTicks * 0.05F + entity.getId()) * 4.5F;
		addBbDeg(this.head, -owlPitch * 0.65F + (signaling ? Mth.sin(ageInTicks * 0.6F) * 6.0F : 0.0F),
				owlYaw * 0.85F, curiousTilt);

		if (flying) {
			float flap = Mth.sin(ageInTicks * 0.52F) * 44.0F;
			addBbPos(this.body, 0.0F, Mth.cos(ageInTicks * 0.52F) * 1.2F, 0.0F);
			addBbDeg(this.body, 18.0F, 0.0F, 0.0F);
			addBbDeg(this.wing_l, -8.0F, 12.0F, -(28.0F + flap));
			addBbDeg(this.wing_r, -8.0F, -12.0F, (28.0F + flap));
			addBbDeg(this.leg_l, 26.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_r, 26.0F, 0.0F, 0.0F);
			addBbDeg(this.tail, -12.0F, 0.0F, 0.0F);
		} else {
			float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
			float w = limbSwing * 0.75F;
			addBbDeg(this.leg_l, Mth.cos(w) * 22.0F * move, 0.0F, 0.0F);
			addBbDeg(this.leg_r, Mth.cos(w + (float) Math.PI) * 22.0F * move, 0.0F, 0.0F);
			addBbDeg(this.body, Mth.sin(ageInTicks * 0.06F) * 1.5F, 0.0F, 0.0F);
			float mantle = signaling ? (22.0F + Mth.sin(ageInTicks * 0.5F) * 10.0F) : 0.0F;
			addBbDeg(this.wing_l, 0.0F, 0.0F, -mantle);
			addBbDeg(this.wing_r, 0.0F, 0.0F, mantle);
			addBbDeg(this.tail, Mth.sin(ageInTicks * 0.07F) * 2.5F, 0.0F, 0.0F);
		}
`,

  bone_raven: `
		resetToBindPose();
		boolean diving = entity.getDiveTicks() > 0;
		boolean deferring = entity.isDeferringToApex();

		if (diving) {
			// Teardrop raptor stoop: wings swept back, nose pitched sharply down
			addBbDeg(this.body, 36.0F, 0.0F, 0.0F);
			addBbDeg(this.neck, -14.0F, 0.0F, 0.0F);
			addBbDeg(this.head, -12.0F, Mth.clamp(netHeadYaw, -25.0F, 25.0F) * 0.4F, 0.0F);
			addBbDeg(this.wing_l, 18.0F, -24.0F, -12.0F);
			addBbDeg(this.wing_r, 18.0F, 24.0F, 12.0F);
			addBbDeg(this.leg_l, 38.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_r, 38.0F, 0.0F, 0.0F);
			addBbDeg(this.tail, 10.0F, 0.0F, 0.0F);
		} else {
			// Slow ominous corvid wingbeats alternating with high circling glides
			float speed = deferring ? 0.34F : 0.48F;
			float phase = ageInTicks * speed + entity.getId();
			float flap = Mth.sin(phase) * (deferring ? 24.0F : 38.0F);
			addBbPos(this.body, 0.0F, Mth.cos(phase) * 1.1F, 0.0F);
			addBbDeg(this.body, 10.0F, 0.0F, Mth.sin(ageInTicks * 0.06F) * 4.5F);
			addBbDeg(this.neck, Mth.sin(phase) * 3.5F, Mth.clamp(netHeadYaw, -40.0F, 40.0F) * 0.35F, 0.0F);
			addBbDeg(this.head, -Mth.clamp(headPitch, -25.0F, 25.0F) * 0.45F,
					Mth.clamp(netHeadYaw, -40.0F, 40.0F) * 0.5F, 0.0F);
			addBbDeg(this.wing_l, 0.0F, 0.0F, -(18.0F + flap));
			addBbDeg(this.wing_r, 0.0F, 0.0F, (18.0F + flap));
			addBbDeg(this.leg_l, 28.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_r, 28.0F, 0.0F, 0.0F);
			addBbDeg(this.tail, -8.0F + Mth.cos(phase) * 4.0F, Mth.sin(ageInTicks * 0.09F) * 4.0F, 0.0F);
		}
`,

  coral_hermit_crab: `
		resetToBindPose();
		float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
		boolean sifting = entity.getSiftTicks() > 0;
		boolean hunkered = entity.getHunkerTicks() > 0;
		boolean pinching = entity.getPinchTicks() > 0;
		float w = limbSwing * 1.15F;

		// 6-legged crustacean scuttle wave
		ModelPart[] leftLegs = { this.leg_l_1, this.leg_l_2, this.leg_l_3 };
		ModelPart[] rightLegs = { this.leg_r_1, this.leg_r_2, this.leg_r_3 };
		for (int i = 0; i < 3; ++i) {
			float ph = w + i * 1.05F;
			addBbDeg(leftLegs[i], 0.0F, Mth.cos(ph) * 20.0F * move, -Math.max(0.0F, Mth.sin(ph)) * 18.0F * move);
			addBbDeg(rightLegs[i], 0.0F, -Mth.cos(ph + (float) Math.PI) * 20.0F * move,
					Math.max(0.0F, Mth.sin(ph + (float) Math.PI)) * 18.0F * move);
		}

		if (hunkered) {
			addBbPos(this.body, 0.0F, -2.6F, 1.5F);
			addBbPos(this.shell, 0.0F, -1.8F, 0.0F);
			addBbDeg(this.claw_major_r, -18.0F, -26.0F, 0.0F);
			addBbDeg(this.claw_minor_l, -16.0F, 24.0F, 0.0F);
			addBbDeg(this.eyestalk_l, 26.0F, 0.0F, 0.0F);
			addBbDeg(this.eyestalk_r, 26.0F, 0.0F, 0.0F);
		} else {
			addBbDeg(this.shell, Mth.sin(w) * 3.5F * move + Mth.sin(ageInTicks * 0.06F) * 1.2F,
					0.0F, Mth.cos(w) * 3.0F * move);
			addBbDeg(this.eyestalk_l, Mth.sin(ageInTicks * 0.13F) * 5.0F, 0.0F, 0.0F);
			addBbDeg(this.eyestalk_r, Mth.cos(ageInTicks * 0.15F) * 5.0F, 0.0F, 0.0F);

			if (sifting) {
				addBbDeg(this.claw_minor_l, 18.0F + Mth.sin(ageInTicks * 0.65F) * 16.0F, 10.0F, 0.0F);
				addBbDeg(this.claw_major_r, 14.0F + Mth.cos(ageInTicks * 0.65F) * 12.0F, -8.0F, 0.0F);
			} else if (pinching) {
				addBbDeg(this.claw_major_r, -28.0F, 22.0F, -14.0F);
			} else {
				addBbDeg(this.claw_major_r, Mth.sin(ageInTicks * 0.08F) * 4.0F, -Mth.cos(ageInTicks * 0.07F) * 3.0F, 0.0F);
				addBbDeg(this.claw_minor_l, Mth.cos(ageInTicks * 0.09F) * 4.0F, Mth.sin(ageInTicks * 0.08F) * 3.0F, 0.0F);
			}
		}
`,

  reef_seahorse: `
		resetToBindPose();
		boolean anchored = entity.isAnchoredToCoral();
		float t = ageInTicks + entity.getId() * 3.1F;

		// Delicate hummingbird-fast dorsal & pectoral cheek-fin flutter
		float dorsalFlutter = Mth.sin(t * 0.85F) * (anchored ? 14.0F : 26.0F);
		float cheekFlutter = Mth.cos(t * 0.78F) * (anchored ? 12.0F : 22.0F);
		addBbDeg(this.dorsal_fin, 0.0F, dorsalFlutter, 0.0F);
		addBbDeg(this.cheek_fin_l, 0.0F, cheekFlutter, -8.0F);
		addBbDeg(this.cheek_fin_r, 0.0F, -cheekFlutter, 8.0F);
		addBbDeg(this.trunk_fin_l, 0.0F, Mth.sin(t * 0.45F) * 12.0F, 0.0F);
		addBbDeg(this.trunk_fin_r, 0.0F, -Mth.sin(t * 0.45F) * 12.0F, 0.0F);

		// Upright seahorse hover bob & prehensile tail curl
		addBbPos(this.trunk, 0.0F, Mth.sin(t * 0.09F) * (anchored ? 0.4F : 1.3F), 0.0F);
		addBbDeg(this.trunk, Mth.sin(t * 0.07F) * 4.5F, 0.0F, Mth.cos(t * 0.06F) * 2.5F);
		addBbDeg(this.head, 4.0F + Mth.sin(t * 0.08F) * 3.5F, Mth.clamp(netHeadYaw, -25.0F, 25.0F) * 0.4F, 0.0F);
		addBbDeg(this.tail_curl, (anchored ? 26.0F : 8.0F) + Mth.sin(t * 0.10F - 0.9F) * 10.0F, 0.0F, 0.0F);
`,

  ghost_squid: `
		resetToBindPose();
		this.renderAlpha = 0.86F;
		float t = ageInTicks + entity.getId() * 2.3F;
		boolean jetting = entity.getJetTicks() > 0;
		boolean lashing = entity.getLashTicks() > 0;

		// Rhythmic mantle-jet pulse & graceful abyssal ear-fin undulation
		float pulse = jetting ? Mth.sin(t * 0.55F) : Mth.sin(t * 0.13F);
		float finWave = Mth.sin(t * (jetting ? 0.42F : 0.18F)) * 28.0F;
		addBbDeg(this.mantle_root, (jetting ? 12.0F : 4.0F) + pulse * 3.5F, 0.0F, Mth.cos(t * 0.07F) * 3.0F);
		this.mantle_root.xScale *= (1.0F - 0.045F * pulse);
		this.mantle_root.yScale *= (1.0F - 0.045F * pulse);
		addBbDeg(this.fin_l, Mth.cos(t * 0.18F) * 8.0F, 0.0F, -finWave);
		addBbDeg(this.fin_r, Mth.cos(t * 0.18F) * 8.0F, 0.0F, finWave);

		// Bell-opening intake vs streamlined jet-thrust tentacle cone + cornered whip lash
		float spread = jetting ? -8.0F : (10.0F + Math.max(0.0F, -pulse) * 16.0F);
		addBbDeg(this.arms_upper, -spread, 0.0F, 0.0F);
		addBbDeg(this.arms_lower, spread, 0.0F, 0.0F);
		if (lashing) {
			float whip = Mth.sin(t * 0.95F) * 38.0F;
			addBbDeg(this.tentacle_long_l, -24.0F, whip, 0.0F);
			addBbDeg(this.tentacle_long_r, 24.0F, -whip, 0.0F);
		} else {
			addBbDeg(this.tentacle_long_l, Mth.sin(t * 0.14F - 0.8F) * 10.0F, -spread * 0.7F + Mth.cos(t * 0.11F) * 8.0F, 0.0F);
			addBbDeg(this.tentacle_long_r, Mth.cos(t * 0.14F - 0.8F) * 10.0F, spread * 0.7F - Mth.sin(t * 0.11F) * 8.0F, 0.0F);
		}
`,

  silver_sea_serpent: `
		resetToBindPose();
		float speed = (float) entity.getDeltaMovement().horizontalDistance();
		boolean biting = entity.getBiteTicks() > 0;
		boolean slapping = entity.getTailSlapTicks() > 0;
		boolean retreating = entity.getRetreatTicks() > 0;

		float effort = Mth.clamp(0.45F + speed * 3.2F + (retreating ? 0.55F : 0.0F), 0.45F, 1.65F);
		float wave = (ageInTicks + entity.getId() * 1.9F) * (0.16F + 0.08F * effort);

		// Travelling pelagic S-wave from neck through tail fluke
		float slapBoost = slapping ? Mth.sin(ageInTicks * 0.95F) * 34.0F : 0.0F;
		addBbDeg(this.serpent_root, Mth.sin(wave * 0.5F) * 2.5F, Mth.sin(wave) * 6.0F * effort, Mth.cos(wave * 0.5F) * 3.5F);
		addBbDeg(this.neck, Mth.sin(wave + 0.5F) * 3.0F - (biting ? 10.0F : 0.0F),
				-Mth.sin(wave - 0.4F) * 7.5F * effort + Mth.clamp(netHeadYaw, -30.0F, 30.0F) * 0.35F, 0.0F);
		addBbDeg(this.head, -Mth.clamp(headPitch, -22.0F, 22.0F) * 0.45F - (biting ? 8.0F : 0.0F),
				-Mth.sin(wave + 0.3F) * 5.0F + Mth.clamp(netHeadYaw, -30.0F, 30.0F) * 0.4F, 0.0F);
		addBbDeg(this.jaw, biting ? (28.0F + Mth.sin(ageInTicks * 0.8F) * 6.0F) : (2.5F + Mth.sin(ageInTicks * 0.07F) * 1.8F), 0.0F, 0.0F);

		addBbDeg(this.body_arch, Mth.sin(wave - 0.9F) * 3.0F, Mth.sin(wave - 0.95F) * 12.0F * effort + slapBoost * 0.4F, 0.0F);
		addBbDeg(this.tail_dip, Mth.sin(wave - 1.8F) * 4.0F, Mth.sin(wave - 1.90F) * 18.0F * effort + slapBoost * 0.75F, 0.0F);
		addBbDeg(this.tail_tip, Mth.sin(wave - 2.6F) * 5.0F, Mth.sin(wave - 2.85F) * 25.0F * effort + slapBoost, 0.0F);

		// 4-flipper hydrofoil rowing: fore-flippers lead, hind-flippers follow with phase lag
		float foreLift = Mth.sin(wave) * 24.0F * effort;
		float hindLift = Mth.sin(wave - 1.6F) * 19.0F * effort;
		addBbDeg(this.flipper_l, Mth.cos(wave) * 9.0F, 0.0F, -(12.0F + foreLift));
		addBbDeg(this.flipper_r, Mth.cos(wave) * 9.0F, 0.0F, (12.0F + foreLift));
		addBbDeg(this.hind_flipper_l, Mth.cos(wave - 1.6F) * 7.0F, 0.0F, -(8.0F + hindLift));
		addBbDeg(this.hind_flipper_r, Mth.cos(wave - 1.6F) * 7.0F, 0.0F, (8.0F + hindLift));
`,
};

function toPascal(id) {
  return id.split('_').map((w) => w[0].toUpperCase() + w.slice(1)).join('');
}

const session = await new BlockbenchMcpSession(process.cwd()).connect();

for (const [id, animCode] of Object.entries(ANIM_BODIES)) {
  const pascal = toPascal(id);
  const modelClass = `${pascal}Model`;
  const entityClass = `${pascal}Entity`;
  const res = await session.call('bbmodel_export_modded_entity', {
    file: `blockbench/riftworld_fauna/${id}.bbmodel`,
    template: '1.17',
    model_name: modelClass,
    entity_class: entityClass,
    flip_y: true,
  });

  if (res.notes && res.notes.length > 0) {
    throw new Error(`Unexpected export notes on ${id}: ${JSON.stringify(res.notes)}`);
  }

  const raw = res.code;
  const fields = [...raw.matchAll(/private final ModelPart (\w+);/g)].map((m) => m[1]);
  const rootField = fields[0];

  const ctorMatch = raw.match(/public \w+\(ModelPart root\) \{([\s\S]*?)\n\t\}/);
  const layerMatch = raw.match(/public static LayerDefinition createBodyLayer\(\) \{([\s\S]*?)\n\t\}/);
  if (!ctorMatch || !layerMatch) {
    throw new Error(`Failed to parse generated constructor or createBodyLayer for ${id}`);
  }

  const captureList = fields.map((f) => `this.${f}`).join(', ');
  const javaOut = `package ru.aiforge.riftworld.client.model.fauna;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.fauna.${entityClass};

/**
 * Generated from {@code blockbench/riftworld_fauna/${id}.bbmodel} via Blockbench MCP
 * ({@code bbmodel_export_modded_entity}, Mojmap 1.17+ / 1.20.1, {@code flip_y: true})
 * with bespoke multi-state Riftworld v0.47.0 biomechanical animations.
 */
public class ${modelClass} extends FaunaBaseModel<${entityClass}> {
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(new ResourceLocation(RiftWorld.MODID, "${id}"), "main");

${fields.map((f) => `\tprivate final ModelPart ${f};`).join('\n')}

	public ${modelClass}(ModelPart root) {${ctorMatch[1]}
		captureRestPoses(${captureList});
	}

	public static LayerDefinition createBodyLayer() {${layerMatch[1]}
	}

	@Override
	protected ModelPart rootPart() {
		return this.${rootField};
	}

	@Override
	public void setupAnim(${entityClass} entity, float limbSwing, float limbSwingAmount,
	                      float ageInTicks, float netHeadYaw, float headPitch) {${animCode}\t}
}
`;

  fs.writeFileSync(path.join(OUT_DIR, `${modelClass}.java`), javaOut);
  console.log(`Generated ${modelClass}.java (${fields.length} bones: ${fields.join(', ')})`);
}

await session.client.close();
