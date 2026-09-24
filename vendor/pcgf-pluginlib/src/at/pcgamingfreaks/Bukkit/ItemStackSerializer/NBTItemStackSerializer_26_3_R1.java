/*
 *   Copyright (C) 2024 GeorgH93
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package at.pcgamingfreaks.Bukkit.ItemStackSerializer;

import at.pcgamingfreaks.Bukkit.MCVersion;

import com.mojang.datafixers.DataFixer;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.*;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Adapted from PCGF_PluginLibForked's generated 26.3 serializer.
 * Paper 26.3 build 40 exposes asBukkitCopy(ItemInstance) instead of asCraftMirror.
 */
public class NBTItemStackSerializer_26_3_R1 implements ItemStackSerializer
{
	private static final int DATA_VERSION = SharedConstants.getCurrentVersion().dataVersion().version();
	private static final DataFixer DATA_FIXER = ((CraftServer) Bukkit.getServer()).getServer().getFixerUpper();

	private Logger logger = null;

	@Override
	public void setLogger(Logger logger)
	{
		this.logger = logger;
	}

	private final HolderLookup.Provider registry = MinecraftServer.getServer().registryAccess();

	private static Method findBukkitConversionMethod(final Object nmsItemStack)
	{
		Method legacyMirror = null;
		for (Method method : CraftItemStack.class.getMethods())
		{
			if (method.getParameterCount() == 1 && method.getParameterTypes()[0].isInstance(nmsItemStack))
			{
				if (method.getName().equals("asBukkitCopy")) return method;
				if (method.getName().equals("asCraftMirror")) legacyMirror = method;
			}
		}
		if (legacyMirror != null) return legacyMirror;
		throw new IllegalStateException("No public CraftItemStack conversion method for runtime NMS type " + nmsItemStack.getClass().getName());
	}

	private static ItemStack convertToBukkit(net.minecraft.world.item.ItemStack item) throws Exception
	{
		Object copiedItem = item.copy();
		Method converter = findBukkitConversionMethod(copiedItem);
		Object result = converter.invoke(null, copiedItem);
		if (!(result instanceof ItemStack)) throw new IllegalStateException("Paper 26.3 item conversion returned " + (result == null ? "null" : result.getClass().getName()));
		return (ItemStack) result;
	}


	private CompoundTag readData(byte[] data) throws IOException
	{
		String error = "";
		try
		{
			return NbtIo.read(new DataInputStream(new ByteArrayInputStream(data)));
		}
		catch(Exception e)
		{
			error = e.getMessage();
		}
		try
		{
			return NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.unlimitedHeap());
		}
		catch(Exception e)
		{
			error += " | " + e.getMessage();
		}
		throw new IOException(error);
	}

	@Override
	public ItemStack[] deserialize(byte[] data)
	{
		if (data != null)
		{
			try
			{
				CompoundTag tag = readData(data);
				int size = tag.getInt(KEY_SIZE).orElseThrow(), dataVersion = tag.getIntOr(KEY_DATA_VERSION, DATA_VERSION);
				if (!tag.contains(KEY_INVENTORY)) { convertOldFormatToNew(tag, size); }
				int itemsBeforeDataFix = tag.getList(KEY_INVENTORY).map(ListTag::size).orElse(-1);
				if (dataVersion < DATA_VERSION)
				{ // Update data
					tag = DataFixTypes.PLAYER.updateToCurrentVersion(DATA_FIXER, tag, dataVersion);
				}
				ItemStack[] its = new ItemStack[size];
				int restoredItems = 0;
				ListTag list = tag.getList(KEY_INVENTORY).orElseThrow();
				int listSize = list.size();
				if (logger != null && itemsBeforeDataFix != listSize)
				{
					logger.warning("NBT inventory diagnostics for " + getClass().getSimpleName() + ": size=" + size + ", dataVersion=" + dataVersion + ", currentDataVersion=" + DATA_VERSION + ", itemsBeforeDataFix=" + itemsBeforeDataFix + ", itemsAfterDataFix=" + listSize + ", tag=" + tag);
				}
				for (int i = 0; i < listSize; i++)
				{
					CompoundTag itemTag = null;
					try
					{
						itemTag = list.getCompound(i).orElseThrow();
						byte slot = itemTag.getByte(KEY_SLOT).orElseThrow();
						final CompoundTag codecItemTag = itemTag;
						Optional<net.minecraft.world.item.ItemStack> item =  net.minecraft.world.item.ItemStack.CODEC.parse(registry.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), codecItemTag).resultOrPartial((s) -> {
							if (logger != null) logger.severe(String.format("Tried to load invalid item in slot %d: '%s'. NBT: %s", slot, s, codecItemTag));
						});
						if (item.isEmpty()) throw new IOException("Item codec returned no item for slot " + slot + ". NBT: " + itemTag);
						net.minecraft.world.item.ItemStack is = item.get();
						if (is.isEmpty()) throw new IOException("Item codec returned an empty item for slot " + slot + ". NBT: " + itemTag);
						// Convert a copy so the Bukkit view cannot mutate the decoded NMS item.
						its[slot] = convertToBukkit(is);
						if (its[slot] == null) throw new IOException("Bukkit conversion returned null for slot " + slot + ". NBT: " + itemTag);
						restoredItems++;
					}
					catch(Exception e)
					{
						if (logger != null)
						{
							logger.log(Level.SEVERE, "Failed to restore item on slot " + i + ". Serialized item:\n" + (itemTag == null ? "null" : itemTag), e);
						}
						return null;
					}
				}
				if (logger != null) logger.fine("Deserialized " + restoredItems + " of " + listSize + " serialized items into " + size + " slots using " + getClass().getSimpleName());
				return its;
			}
			catch (Exception e)
			{
				if (logger != null) logger.log(Level.SEVERE, "Failed to deserialize NBTItemStack (size: " + data.length + ")", e);
				else e.printStackTrace();
			}
		}
		return null;
	}

	private void convertOldFormatToNew(CompoundTag tag, int size)
	{
		ListTag list = new ListTag();
		tag.put(KEY_INVENTORY, list);
		for(int i = 0; i < size; i++)
		{
			String is = String.valueOf(i);
			if (tag.contains(is))
			{
				CompoundTag itemTag = tag.getCompound(is).orElseThrow();
				itemTag.putByte(KEY_SLOT, (byte) i);
				list.add(itemTag);
			}
		}
	}

	@Override
	public byte[] serialize(ItemStack[] itemStacks)
	{
		if(itemStacks != null)
		{
			try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream))
			{
				CompoundTag tag = new CompoundTag();
				tag.putInt(KEY_SIZE, itemStacks.length);
				tag.putInt(KEY_DATA_VERSION, DATA_VERSION);
				ListTag list = new ListTag();
				tag.put(KEY_INVENTORY, list);
				for(int i = 0, used = -1; i < itemStacks.length; i++)
				{
					if (itemStacks[i] == null) continue;
					try
					{
						CompoundTag itemTag = new CompoundTag();
						itemTag.putByte(KEY_SLOT, (byte) i);
						net.minecraft.world.item.ItemStack stack = CraftItemStack.asNMSCopy(itemStacks[i]);
						Tag t = net.minecraft.world.item.ItemStack.CODEC.encode(stack, MinecraftServer.getServer().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), itemTag).getOrThrow();

						list.addTag(++used, t);
					}
					catch (Exception e)
					{
						if (logger != null) logger.log(Level.SEVERE, "Failed to store item on slot " + i + "; refusing to save an incomplete backpack. Item: " + itemStacks[i], e);
						return null;
					}
				}
				NbtIo.write(tag, dataOutputStream);
				dataOutputStream.flush();
				return byteArrayOutputStream.toByteArray();
			}
			catch(Exception e)
			{
				if (logger != null) logger.log(Level.SEVERE, "Failed to serialize NBTItemStack", e);
				else e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public boolean checkIsMCVersionCompatible()
	{
		return MCVersion.is(MCVersion.MC_NMS_26_3_R1);
	}
}
