package io.github.haykam821.cakewars.game.player;

import io.github.haykam821.cakewars.game.phase.CakeWarsActivePhase;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.plasmid.map.template.TemplateRegion;
import xyz.nucleoid.plasmid.util.ColoredBlocks;

public class Beacon {
	private final CakeWarsActivePhase phase;
	private final TemplateRegion region;
	private final Item item;
	private final int maxHealth;
	private final int maxGeneratorCooldown;
	private int health;
	private int generatorCooldown;
	private TeamEntry controller = null;

	public Beacon(CakeWarsActivePhase phase, TemplateRegion region, Item item, int maxGeneratorCooldown) {
		this.phase = phase;
		this.region = region;
		this.item = item;

		this.maxHealth = this.phase.getConfig().getMaxBeaconHealth();
		this.health = this.maxHealth;

		this.maxGeneratorCooldown = maxGeneratorCooldown;
		this.generatorCooldown = this.maxGeneratorCooldown;
	}

	public boolean isUnreplaceableBlock(BlockState state) {
		if (state.isOf(Blocks.IRON_BLOCK)) return true;
		if (state.isOf(Blocks.BEACON)) return true;
		if (state.isAir()) return true;

		return false;
	}

	public Block getBlock(BlockPos pos, BlockState state, int minY) {
		if (this.isUnreplaceableBlock(state)) return null;

		DyeColor dye = this.controller.getGameTeam().getDye();
		return pos.getY() == minY ? ColoredBlocks.wool(dye) : ColoredBlocks.glass(dye);
	}

	public void setController(TeamEntry controller) {
		this.controller = controller;
		this.health = this.maxHealth;

		ServerWorld world = this.phase.getGameSpace().getWorld();
		int minY =  this.region.getBounds().getMin().getY();
		for (BlockPos pos : this.region.getBounds()) {
			BlockState state = world.getBlockState(pos);

			Block newBlock = this.getBlock(pos, state, minY);
			if (newBlock != null) {
				world.setBlockState(pos, newBlock.getDefaultState());
			}
		}
	}

	private boolean isStandingOnBeacon(PlayerEntry player) {
		return this.region.getBounds().contains(player.getPlayer().getBlockPos());
	}

	public void tick() {
		TeamEntry newController = null;
		for (PlayerEntry player : this.phase.getPlayers()) {
			if (this.isStandingOnBeacon(player)) {
				this.health += player.getTeam() == this.controller ? 1 : -1;
				newController = player.getTeam();
			}
		}

		this.health = Math.min(this.health, this.maxHealth);

		if (this.health < 0 && newController != null) {
			this.setController(newController);
		}

		if (this.controller != null) {
			this.generatorCooldown -= 1;
			if (this.generatorCooldown < 0) {
				this.generatorCooldown = this.maxGeneratorCooldown;
				this.controller.spawnGeneratorItem(this.item);
			}
		}
	}
}