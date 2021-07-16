/*
 * ArmorStandEditor: Bukkit plugin to allow editing armor stand attributes
 * Copyright (C) 2016  RypoFalem
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.github.rypofalem.armorstandeditor;

import io.github.rypofalem.armorstandeditor.menu.EquipmentMenu;
import io.github.rypofalem.armorstandeditor.menu.Menu;
import io.github.rypofalem.armorstandeditor.modes.Axis;
import io.github.rypofalem.armorstandeditor.modes.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.UUID;

public class PlayerEditor {
	public ArmorStandEditorPlugin plugin;
	private final UUID uuid;
	EditMode eMode;
	AdjustmentMode adjMode;
	CopySlots copySlots;
	Axis axis;
	double eulerAngleChange;
	double degreeAngleChange;
	double movChange;
	Menu chestMenu;
	ArmorStand target;
	ItemFrame frameTarget;
	ArrayList<ArmorStand> targetList = null;
	ArrayList<ItemFrame> frameTargetList = null;
	int targetIndex = 0;
	int frameTargetIndex = 0;
	EquipmentMenu equipMenu;
	long lastCancelled = 0;

	public PlayerEditor(final UUID uuid, final ArmorStandEditorPlugin plugin) {
		this.uuid = uuid;
		this.plugin = plugin;
		eMode = EditMode.NONE;
		adjMode = AdjustmentMode.COARSE;
		axis = Axis.X;
		copySlots = new CopySlots();
		eulerAngleChange = getManager().coarseAdj;
		degreeAngleChange = eulerAngleChange / Math.PI * 180;
		movChange = getManager().coarseMov;
		chestMenu = new Menu(this);
	}

	public void setMode(final EditMode editMode) {
		this.eMode = editMode;
		sendMessage("setmode", editMode.toString().toLowerCase());
	}

	public void setAxis(final Axis axis) {
		this.axis = axis;
		sendMessage("setaxis", axis.toString().toLowerCase());
	}

	public void setAdjMode(final AdjustmentMode adjMode) {
		this.adjMode = adjMode;
		if (adjMode == AdjustmentMode.COARSE) {
			eulerAngleChange = getManager().coarseAdj;
			movChange = getManager().coarseMov;
		} else {
			eulerAngleChange = getManager().fineAdj;
			movChange = getManager().fineMov;
		}
		degreeAngleChange = eulerAngleChange / Math.PI * 180;
		sendMessage("setadj", adjMode.toString().toLowerCase());
	}

	public void setCopySlot(final byte slot) {
		copySlots.changeSlots(slot);
		sendMessage("setslot", String.valueOf((slot + 1)));
	}

	public void editArmorStand(ArmorStand armorStand) {
		if (!getPlayer().hasPermission("asedit.basic")) return;
		armorStand = attemptTarget(armorStand);
		switch (eMode) {
			case LEFTARM:
				armorStand.setLeftArmPose(subEulerAngle(armorStand.getLeftArmPose()));
				break;
			case RIGHTARM:
				armorStand.setRightArmPose(subEulerAngle(armorStand.getRightArmPose()));
				break;
			case BODY:
				armorStand.setBodyPose(subEulerAngle(armorStand.getBodyPose()));
				break;
			case HEAD:
				armorStand.setHeadPose(subEulerAngle(armorStand.getHeadPose()));
				break;
			case LEFTLEG:
				armorStand.setLeftLegPose(subEulerAngle(armorStand.getLeftLegPose()));
				break;
			case RIGHTLEG:
				armorStand.setRightLegPose(subEulerAngle(armorStand.getRightLegPose()));
				break;
			case SHOWARMS:
				toggleArms(armorStand);
				break;
			case SIZE:
				toggleSize(armorStand);
				break;
			case INVISIBLE:
				toggleVisible(armorStand);
				break;
			case BASEPLATE:
				togglePlate(armorStand);
				break;
			case GRAVITY:
				toggleGravity(armorStand);
				break;
			case COPY:
				copy(armorStand);
				break;
			case PASTE:
				paste(armorStand);
				break;
			case PLACEMENT:
				move(armorStand);
				break;
			case ROTATE:
				rotate(armorStand);
				break;
			case DISABLESLOTS:
				toggleDisableSlots(armorStand);
				break;
			case EQUIPMENT:
				openEquipment(armorStand);
				break;
			case RESET:
				resetPosition(armorStand);
				break;
			case NONE:
				sendMessage("nomode", null);
				break;
		}
	}

	public void editItemFrame(ItemFrame itemFrame) {
		if (!getPlayer().hasPermission("asedit.itemframe")) return;
		itemFrame = attemptTarget(itemFrame);
		switch (eMode) {
			case ITEMFRAME:
				toggleVisible(itemFrame);
				break;
			case RESET:
				itemFrame.setVisible(true);
			case NONE:
				sendMessage("nomode", null);
				break;
		}

	}

	private void resetPosition(final ArmorStand armorStand) {
		armorStand.setHeadPose(new EulerAngle(0, 0, 0));
		armorStand.setBodyPose(new EulerAngle(0, 0, 0));
		armorStand.setLeftArmPose(new EulerAngle(0, 0, 0));
		armorStand.setRightArmPose(new EulerAngle(0, 0, 0));
		armorStand.setLeftLegPose(new EulerAngle(0, 0, 0));
		armorStand.setRightLegPose(new EulerAngle(0, 0, 0));
	}

	private void openEquipment(final ArmorStand armorStand) {
		equipMenu = new EquipmentMenu(this, armorStand);
		equipMenu.open();
	}

	public void reverseEditArmorStand(ArmorStand armorStand) {
		if (!getPlayer().hasPermission("asedit.basic")) return;
		armorStand = attemptTarget(armorStand);
		switch (eMode) {
			case LEFTARM:
				armorStand.setLeftArmPose(addEulerAngle(armorStand.getLeftArmPose()));
				break;
			case RIGHTARM:
				armorStand.setRightArmPose(addEulerAngle(armorStand.getRightArmPose()));
				break;
			case BODY:
				armorStand.setBodyPose(addEulerAngle(armorStand.getBodyPose()));
				break;
			case HEAD:
				armorStand.setHeadPose(addEulerAngle(armorStand.getHeadPose()));
				break;
			case LEFTLEG:
				armorStand.setLeftLegPose(addEulerAngle(armorStand.getLeftLegPose()));
				break;
			case RIGHTLEG:
				armorStand.setRightLegPose(addEulerAngle(armorStand.getRightLegPose()));
				break;
			case PLACEMENT:
				reverseMove(armorStand);
				break;
			case ROTATE:
				reverseRotate(armorStand);
				break;
			default:
				editArmorStand(armorStand);
		}
	}

	private void move(final ArmorStand armorStand) {
		final Location loc = armorStand.getLocation();
		switch (axis) {
			case X:
				loc.add(movChange, 0, 0);
				break;
			case Y:
				loc.add(0, movChange, 0);
				break;
			case Z:
				loc.add(0, 0, movChange);
				break;
		}
		armorStand.teleport(loc);
	}

	private void reverseMove(final ArmorStand armorStand) {
		final Location loc = armorStand.getLocation();
		switch (axis) {
			case X:
				loc.subtract(movChange, 0, 0);
				break;
			case Y:
				loc.subtract(0, movChange, 0);
				break;
			case Z:
				loc.subtract(0, 0, movChange);
				break;
		}
		armorStand.teleport(loc);
	}

	private void rotate(final ArmorStand armorStand) {
		final Location loc = armorStand.getLocation();
		final float yaw = loc.getYaw();
		loc.setYaw((yaw + 180 + (float) degreeAngleChange) % 360 - 180);
		armorStand.teleport(loc);
	}

	private void reverseRotate(final ArmorStand armorStand) {
		final Location loc = armorStand.getLocation();
		final float yaw = loc.getYaw();
		loc.setYaw((yaw + 180 - (float) degreeAngleChange) % 360 - 180);
		armorStand.teleport(loc);
	}

	private void copy(final ArmorStand armorStand) {
		copySlots.copyDataToSlot(armorStand);
		sendMessage("copied", "" + (copySlots.currentSlot + 1));
		setMode(EditMode.PASTE);
	}

	private void paste(final ArmorStand armorStand) {
		final ArmorStandData data = copySlots.getDataToPaste();
		if (data == null) return;
		armorStand.setHeadPose(data.headPos);
		armorStand.setBodyPose(data.bodyPos);
		armorStand.setLeftArmPose(data.leftArmPos);
		armorStand.setRightArmPose(data.rightArmPos);
		armorStand.setLeftLegPose(data.leftLegPos);
		armorStand.setRightLegPose(data.rightLegPos);
		armorStand.setSmall(data.size);
		armorStand.setGravity(data.gravity);
		armorStand.setBasePlate(data.basePlate);
		armorStand.setArms(data.showArms);
		armorStand.setVisible(data.visible);
		if (this.getPlayer().getGameMode() == GameMode.CREATIVE) {
			armorStand.getEquipment().setHelmet(data.head);
			armorStand.getEquipment().setChestplate(data.body);
			armorStand.getEquipment().setLeggings(data.legs);
			armorStand.getEquipment().setBoots(data.feetsies);
			armorStand.getEquipment().setItemInMainHand(data.rightHand);
			armorStand.getEquipment().setItemInOffHand(data.leftHand);
		}
		sendMessage("pasted", "" + (copySlots.currentSlot + 1));
	}

	private void toggleDisableSlots(final ArmorStand armorStand) {
		if (armorStand.hasEquipmentLock(EquipmentSlot.HAND, ArmorStand.LockType.REMOVING_OR_CHANGING)) { //Adds a lock to every slot or removes it
			for (final EquipmentSlot slot : EquipmentSlot.values()) {
				armorStand.removeEquipmentLock(slot, ArmorStand.LockType.REMOVING_OR_CHANGING);
				armorStand.removeEquipmentLock(slot, ArmorStand.LockType.ADDING);
				getPlayer().playSound(getPlayer().getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
			}
		} else {
			for (final EquipmentSlot slot : EquipmentSlot.values()) {
				armorStand.addEquipmentLock(slot, ArmorStand.LockType.REMOVING_OR_CHANGING);
				armorStand.addEquipmentLock(slot, ArmorStand.LockType.ADDING);
			}
			getPlayer().playSound(getPlayer().getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, SoundCategory.PLAYERS, 1.0f, 1.0f);
		}
		sendMessage("disabledslots", null);
	}

	private void toggleGravity(final ArmorStand armorStand) {
		armorStand.setGravity(!armorStand.hasGravity());
		final String state = armorStand.hasGravity() ? "on" : "off";
		sendMessage("setgravity", state);
	}

	void togglePlate(final ArmorStand armorStand) {
		armorStand.setBasePlate(!armorStand.hasBasePlate());
	}

	void toggleArms(final ArmorStand armorStand) {
		armorStand.setArms(!armorStand.hasArms());
	}

	void toggleVisible(final ArmorStand armorStand) {
		if (!getPlayer().hasPermission("asedit.invisible")) return;
		armorStand.setVisible(!armorStand.isVisible());
	}

	void toggleVisible(final ItemFrame itemFrame) {
		if (!getPlayer().hasPermission("asedit.itemframe")) return;
		itemFrame.setVisible(!itemFrame.isVisible());
	}

	void toggleSize(final ArmorStand armorStand) {
		armorStand.setSmall(!armorStand.isSmall());
	}

	void cycleAxis(final int i) {
		int index = axis.ordinal();
		index += i;
		index = index % Axis.values().length;
		while (index < 0) {
			index += Axis.values().length;
		}
		setAxis(Axis.values()[index]);
	}

	private EulerAngle addEulerAngle(EulerAngle angle) {
		switch(axis){
			case X: angle = angle.setX(Util.addAngle(angle.getX(), eulerAngleChange));
				break;
			case Y: angle = angle.setY(Util.addAngle(angle.getY(), eulerAngleChange));
				break;
			case Z: angle = angle.setZ(Util.addAngle(angle.getZ(), eulerAngleChange));
				break;
			default:
				break;
		}
		return angle;
	}

	private EulerAngle subEulerAngle(EulerAngle angle) {
		switch (axis) {
			case X:
				angle = angle.setX(Util.subAngle(angle.getX(), eulerAngleChange));
				break;
			case Y:
				angle = angle.setY(Util.subAngle(angle.getY(), eulerAngleChange));
				break;
			case Z:
				angle = angle.setZ(Util.subAngle(angle.getZ(), eulerAngleChange));
				break;
			default:
				break;
		}
		return angle;
	}

	public void setTarget(final ArrayList<ArmorStand> armorStands) {
		if (armorStands == null || armorStands.isEmpty()) {
			target = null;
			targetList = null;
			sendMessage("notarget", null);
			return;
		}

		if (targetList == null) {
			targetList = armorStands;
			targetIndex = 0;
			sendMessage("target", null);
		} else {
			boolean same = targetList.size() == armorStands.size();
			if (same) for (final ArmorStand as : armorStands) {
				same = targetList.contains(as);
				if (!same) break;
			}

			if (same) {
				targetIndex = ++targetIndex % targetList.size();
			} else {
				targetList = armorStands;
				targetIndex = 0;
				sendMessage("target", null);
			}
		}
		target = targetList.get(targetIndex);
		highlight(target);
	}

	public void setFrameTarget(final ArrayList<ItemFrame> itemFrames) {
		if (itemFrames == null || itemFrames.isEmpty()) {
			frameTarget = null;
			frameTargetList = null;
			sendMessage("noframetarget", null);
			return;
		}

		if (frameTargetList == null) {
			frameTargetList = itemFrames;
			frameTargetIndex = 0;
			sendMessage("target", null);
		} else {
			boolean same = frameTargetList.size() == itemFrames.size();
			if (same) for (final ItemFrame itemf : itemFrames) {
				same = frameTargetList.contains(itemf);
				if (!same) break;
			}

			if (same) {
				frameTargetIndex = ++frameTargetIndex % frameTargetList.size();
			} else {
				frameTargetList = itemFrames;
				frameTargetIndex = 0;
				sendMessage("frametarget", null);
			}
			frameTarget = frameTargetList.get(frameTargetIndex);
		}
	}

	ArmorStand attemptTarget(ArmorStand armorStand) {
		if (target == null
				|| !target.isValid()
				|| target.getWorld() != getPlayer().getWorld()
				|| target.getLocation().distanceSquared(getPlayer().getLocation()) > 100)
			return armorStand;
		armorStand = target;
		highlight(armorStand);
		return armorStand;
	}

	ItemFrame attemptTarget(ItemFrame itemFrame) {
		if (frameTarget == null
				|| !frameTarget.isValid()
				|| frameTarget.getWorld() != getPlayer().getWorld()
				|| frameTarget.getLocation().distanceSquared(getPlayer().getLocation()) > 100)
			return itemFrame;
		itemFrame = frameTarget;
		return itemFrame;
	}

	void sendMessage(final String path, final String format, final String option) {
		final String message = plugin.getLang().getMessage(path, format, option);
		if (plugin.sendToActionBar) {
			if (ArmorStandEditorPlugin.instance().hasSpigot) {
				plugin.getServer().getPlayer(getUUID()).spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
			} else {
				final String rawText = plugin.getLang().getRawMessage(path, format, option);
				final String command = String.format("title %s actionbar %s", plugin.getServer().getPlayer(getUUID()).getName(), rawText);
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
			}
		} else {
			plugin.getServer().getPlayer(getUUID()).sendMessage(message);
		}
	}

	void sendMessage(final String path, final String option) {
		sendMessage(path, "info", option);
	}

	private void highlight(final ArmorStand armorStand) {
		armorStand.removePotionEffect(PotionEffectType.GLOWING);
		armorStand.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 15, 1, false, false));
	}

	public PlayerEditorManager getManager() {
		return plugin.editorManager;
	}

	public Player getPlayer() {
		return plugin.getServer().getPlayer(getUUID());
	}

	public UUID getUUID() {
		return uuid;
	}

	public void openMenu() {
		if (!isMenuCancelled()) {
			plugin.getServer().getScheduler().runTaskLater(plugin, new OpenMenuTask(), 1).getTaskId();
		}
	}

	public void cancelOpenMenu() {
		lastCancelled = getManager().getTime();
	}

	boolean isMenuCancelled(){
		return getManager().getTime() - lastCancelled < 2;
	}

	private class OpenMenuTask implements Runnable{

		@Override
		public void run() {
			if(isMenuCancelled()) return;
			chestMenu.openMenu();
		}
	}
}