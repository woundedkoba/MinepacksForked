/*
 *   Copyright (C) 2020 GeorgH93
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

package at.pcgamingfreaks.Minepacks.Bukkit.Database;

import at.pcgamingfreaks.Bukkit.ItemStackSerializer.ItemStackSerializer;
import at.pcgamingfreaks.Bukkit.MCVersion;
import at.pcgamingfreaks.ConsoleColor;

import lombok.Getter;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InventorySerializer
{
	@SuppressWarnings("deprecation")
	private static final ItemStackSerializer BUKKIT_ITEM_STACK_SERIALIZER = ItemStackSerializer.makeBukkitItemStackSerializer();

	private final Logger logger;
	private final ItemStackSerializer serializer;
	@Getter private final int usedSerializer;

	InventorySerializer(Logger logger, ItemStackSerializer serializer, int usedSerializer)
	{
		this.logger = logger;
		this.serializer = serializer;
		this.usedSerializer = usedSerializer;
	}
	
	public InventorySerializer(Logger logger)
	{
		this.logger = logger;
		ItemStackSerializer serializer = null;
		int usedSerializer = 2;
		try
		{
			if(ItemStackSerializer.isNBTItemStackSerializerAvailable())
			{
				serializer = ItemStackSerializer.makeNBTItemStackSerializer(logger);
			}
		}
		catch(Exception e)
		{
			logger.log(Level.SEVERE, "Failed to produce serializer!", e);
		}
		if(serializer == null)
		{
			logger.severe("NBTItemStackSerializer does not support your Minecraft version!\nFalling back to BukkitItemStackSerializer! This most likely is wrong!");
			if (MCVersion.isOlderThan(MCVersion.MC_NMS_1_8_R1))
			{
				usedSerializer = 0;
				serializer = BUKKIT_ITEM_STACK_SERIALIZER;
			}
		}
		this.serializer = serializer;
		this.usedSerializer = usedSerializer;
	}
	
	public byte[] serialize(Inventory inv)
	{
		try
		{
			return serializer == null ? null : serializer.serialize(inv.getContents());
		}
		catch(RuntimeException e)
		{
			logger.log(Level.SEVERE, "Backpack serialization failed.", e);
			return null;
		}
	}

	/** An empty inventory is a present array containing null slots; an empty Optional is a decode failure. */
	public Optional<ItemStack[]> deserialize(byte[] data, int usedSerializer)
	{
		if(data == null) return Optional.empty();
		ItemStack[] result;
		try
		{
			switch(usedSerializer)
			{
				case 0: result = BUKKIT_ITEM_STACK_SERIALIZER.deserialize(data); break;
				case 1:
					if(MCVersion.isNewerOrEqualThan(MCVersion.MC_1_13)) logger.warning(ConsoleColor.YELLOW + "Backpack was created with an old version of minepacks and minecraft. There is the chance that some items will disappear from it." + ConsoleColor.RESET);
					result = serializer == null ? null : serializer.deserialize(data);
					break;
				case 2: result = serializer == null ? null : serializer.deserialize(data); break;
				default: logger.warning(ConsoleColor.RED + "No compatible deserializer for backpack format available!" + ConsoleColor.RESET);
					return Optional.empty();
			}
		}
		catch(RuntimeException e)
		{
			logger.log(Level.SEVERE, "Backpack deserialization failed (serializer=" + usedSerializer + ", bytes=" + data.length + ").", e);
			return Optional.empty();
		}

		if(result == null)
		{
			logger.warning("Backpack deserialization returned no inventory (serializer=" + usedSerializer + ", bytes=" + data.length + ").");
		}
		return Optional.ofNullable(result);
	}
}
