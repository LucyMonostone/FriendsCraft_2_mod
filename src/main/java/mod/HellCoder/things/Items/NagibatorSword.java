package mod.HellCoder.things.Items;

import java.util.List;

import org.lwjgl.input.Keyboard;

import mod.HellCoder.things.FriendsCraft2mod;
import mod.HellCoder.things.lib.RegItems;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import cofh.api.energy.IEnergyContainerItem;
import cofh.api.item.IEmpowerableItem;
import cofh.util.DamageHelper;
import cofh.util.EnergyHelper;
import cofh.util.KeyBindingEmpower;
import cofh.util.MathHelper;
import cofh.util.StringHelper;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class NagibatorSword extends ItemSword implements IEmpowerableItem, IEnergyContainerItem{
	
	public int maxEnergy = 160000;
	public int maxTransfer = 1600;
	public int energyPerUse = 200;
	public int energyPerUseCharged = 800;

	public int damage = 8;
	public int damageCharged = 4;

	public NagibatorSword(Item.ToolMaterial toolMaterial) {
		
		super(toolMaterial);
		setNoRepair();
	}
	
	protected int useEnergy(ItemStack stack, boolean simulate) {

		int unbreakingLevel = MathHelper.clampI(EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack), 0, 4);
		return extractEnergy(stack, isEmpowered(stack) ? energyPerUseCharged * (5 - unbreakingLevel) / 5 : energyPerUse * (5 - unbreakingLevel) / 5, simulate);
	}
	
	protected int getEnergyPerUse(ItemStack stack) {

		int unbreakingLevel = MathHelper.clampI(EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack), 0, 4);
		return (isEmpowered(stack) ? energyPerUseCharged : energyPerUse) * (5 - unbreakingLevel) / 5;
	}

	public void onCreated(ItemStack itemStack, World world, EntityPlayer player) {
		itemStack.stackTagCompound = new NBTTagCompound();

		itemStack.stackTagCompound.setString("player_name",
				player.getDisplayName());
	}

	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean show) {
		initTags(stack);
		
		super.addInformation(stack, player, list, show);
		
		if ((GuiScreen.isShiftKeyDown()) || (show)) {
			
			if (stack.stackTagCompound == null) {
				EnergyHelper.setDefaultEnergyTag(stack, 0);
			}
			list.add("Charge: " + stack.stackTagCompound.getInteger("Energy") + " / " + maxEnergy + " RF");
			
			list.add(StringHelper.ORANGE + getEnergyPerUse(stack) + " " + StringHelper.localize("info.redstonearsenal.tool.energyPerUse") + StringHelper.END);
			
			if (isEmpowered(stack)) {
				list.add(StringHelper.YELLOW + StringHelper.ITALIC + StringHelper.localize("info.cofh.press") + " "
						+ Keyboard.getKeyName(KeyBindingEmpower.instance.getKey()) + " " + StringHelper.localize("info.redstonearsenal.tool.chargeOff")
						+ StringHelper.END);
			 } else {
				list.add(StringHelper.BRIGHT_BLUE + StringHelper.ITALIC + StringHelper.localize("info.cofh.press") + " "
						+ Keyboard.getKeyName(KeyBindingEmpower.instance.getKey()) + " " + StringHelper.localize("info.redstonearsenal.tool.chargeOn")
						+ StringHelper.END);
			}
			
			if (getEnergyStored(stack) >= getEnergyPerUse(stack)) {
				list.add("");
				list.add(StringHelper.LIGHT_BLUE + "+" + damage + " " + StringHelper.localize("info.cofh.damageAttack") + StringHelper.END);
				list.add(StringHelper.BRIGHT_GREEN + "+" + (isEmpowered(stack) ? damageCharged : 1) + " " + StringHelper.localize("info.cofh.damageFlux")
						+ StringHelper.END);
			}
			
			if (!getPlayerName(stack).isEmpty()) {

				list.add("Owner: " + getPlayerName(stack));
			}

		} else {
			list.add("Press SHIFT for more info");
		}
	}

	public static void setName(ItemStack stack, String t, String n) {
		NBTTagCompound tag = initTags(stack);
		tag.setString("player_name", t);
	}

	public static String getPlayerName(ItemStack stack) {
		NBTTagCompound tag = initTags(stack);
		return tag.getString("player_name");
	}

	public static NBTTagCompound initTags(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();

		if (tag == null) {
			tag = new NBTTagCompound();
			stack.setTagCompound(tag);
			tag.setString("player_name", "");
		}
		return tag;
	}
	
	@Override
	public EnumRarity getRarity(ItemStack stack) {

		return EnumRarity.uncommon;
	}

	@Override
	public void getSubItems(Item item, CreativeTabs tab, List list) {

		list.add(EnergyHelper.setDefaultEnergyTag(new ItemStack(item, 1, 0), 0));
		list.add(EnergyHelper.setDefaultEnergyTag(new ItemStack(item, 1, 0), maxEnergy));
	}
	
	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {

		player.setItemInUse(stack, this.getMaxItemUseDuration(stack));
		return stack;
	}
	
	@Override
	public boolean hitEntity(ItemStack stack, EntityLivingBase entity, EntityLivingBase player) {

		if (stack.getItemDamage() > 0) {
			stack.setItemDamage(0);
		}
		EntityPlayer thePlayer = (EntityPlayer) player;
		float fallingMult = (player.fallDistance > 0.0F && !player.onGround && !player.isOnLadder() && !player.isInWater()
				&& !player.isPotionActive(Potion.blindness) && player.ridingEntity == null) ? 1.5F : 1.0F;

		if (thePlayer.capabilities.isCreativeMode || useEnergy(stack, false) == getEnergyPerUse(stack)) {
			float fluxDamage = isEmpowered(stack) ? damageCharged : 1;
			float enchantDamage = damage + EnchantmentHelper.getEnchantmentModifierLiving(player, entity);

			entity.attackEntityFrom(DamageHelper.causePlayerFluxDamage(thePlayer), fluxDamage);
			entity.attackEntityFrom(DamageSource.causePlayerDamage(thePlayer), (fluxDamage + enchantDamage) * fallingMult);
		} else {
			entity.attackEntityFrom(DamageSource.causePlayerDamage(thePlayer), 1 * fallingMult);
		}
		return true;
	}
	
	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean isCurrentItem) {

		if (!isEmpowered(stack) || !isCurrentItem) {
			return;
		}
		if (entity instanceof EntityPlayer) {
			if (((EntityPlayer) entity).isBlocking()) {

				AxisAlignedBB axisalignedbb = entity.boundingBox.expand(2.0D, 1.0D, 2.0D);
				List<EntityMob> list = entity.worldObj.getEntitiesWithinAABB(EntityMob.class, axisalignedbb);

				for (Entity mob : list) {
					pushEntityAway(mob, entity);
				}
			}
		}
	}
	
	protected void pushEntityAway(Entity entity, Entity player) {

		double d0 = player.posX - entity.posX;
		double d1 = player.posZ - entity.posZ;
		double d2 = MathHelper.maxAbs(d0, d1);

		if (d2 >= 0.01D) {
			d2 = Math.sqrt(d2);
			d0 /= d2;
			d1 /= d2;
			double d3 = 1.0D / d2;

			if (d3 > 1.0D) {
				d3 = 1.0D;
			}
			d0 *= d3;
			d1 *= d3;
			d0 *= 0.2D;
			d1 *= 0.2D;
			d0 *= 1.0F - entity.entityCollisionReduction;
			d1 *= 1.0F - entity.entityCollisionReduction;
			entity.addVelocity(-d0, 0.0D, -d1);
		}
	}

	@Override
	public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z, EntityLivingBase entity) {

		if (block.getBlockHardness(world, x, y, z) != 0.0D) {
			extractEnergy(stack, energyPerUse, false);
		}
		return true;
	}
	
	@Override
	public boolean getIsRepairable(ItemStack itemToRepair, ItemStack stack) {

		return false;
	}
	
	@Override
	public int getDisplayDamage(ItemStack stack) {

		if (stack.stackTagCompound == null) {
			EnergyHelper.setDefaultEnergyTag(stack, 0);
		}
		return 1 + maxEnergy - stack.stackTagCompound.getInteger("Energy");
	}
	
	@Override
	public int getMaxDamage(ItemStack stack) {

		return 1 + maxEnergy;
	}

	@Override
	public boolean isDamaged(ItemStack stack) {

		return stack.getItemDamage() != Short.MAX_VALUE;
	}

	@Override
	public Multimap getItemAttributeModifiers() {

		return HashMultimap.create();
	}
	
	/* IEmpowerableItem */
	@Override
	public boolean isEmpowered(ItemStack stack) {

		return stack.stackTagCompound == null ? false : stack.stackTagCompound.getBoolean("Empowered");
	}

	@Override
	public boolean setEmpoweredState(ItemStack stack, boolean state) {

		if (getEnergyStored(stack) > 0) {
			stack.stackTagCompound.setBoolean("Empowered", state);
			return true;
		}
		stack.stackTagCompound.setBoolean("Empowered", false);
		return false;
	}

	@Override
	public void onStateChange(EntityPlayer player, ItemStack stack) {

		if (isEmpowered(stack)) {
			player.worldObj.playSoundAtEntity(player, "ambient.weather.thunder", 0.4F, 1.0F);
		} else {
			player.worldObj.playSoundAtEntity(player, "random.orb", 0.2F, 0.6F);
		}
	}

	/* IEnergyContainerItem */
	@Override
	public int receiveEnergy(ItemStack container, int maxReceive, boolean simulate) {

		if (container.stackTagCompound == null) {
			EnergyHelper.setDefaultEnergyTag(container, 0);
		}
		int stored = container.stackTagCompound.getInteger("Energy");
		int receive = Math.min(maxReceive, Math.min(maxEnergy - stored, maxTransfer));

		if (!simulate) {
			stored += receive;
			container.stackTagCompound.setInteger("Energy", stored);
		}
		return receive;
	}

	@Override
	public int extractEnergy(ItemStack container, int maxExtract, boolean simulate) {

		if (container.stackTagCompound == null) {
			EnergyHelper.setDefaultEnergyTag(container, 0);
		}
		int stored = container.stackTagCompound.getInteger("Energy");
		int extract = Math.min(maxExtract, stored);

		if (!simulate) {
			stored -= extract;
			container.stackTagCompound.setInteger("Energy", stored);

			if (stored == 0) {
				setEmpoweredState(container, false);
			}
		}
		return extract;
	}

	@Override
	public int getEnergyStored(ItemStack container) {

		if (container.stackTagCompound == null) {
			EnergyHelper.setDefaultEnergyTag(container, 0);
		}
		return container.stackTagCompound.getInteger("Energy");
	}

	@Override
	public int getMaxEnergyStored(ItemStack container) {

		return maxEnergy;
	}
}
