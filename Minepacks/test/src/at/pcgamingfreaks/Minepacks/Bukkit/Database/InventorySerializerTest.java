package at.pcgamingfreaks.Minepacks.Bukkit.Database;

import at.pcgamingfreaks.Bukkit.ItemStackSerializer.ItemStackSerializer;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InventorySerializerTest
{
	private static InventorySerializer withDecodedItems(ItemStack[] decoded)
	{
		ItemStackSerializer delegate = new ItemStackSerializer()
		{
			@Override public ItemStack[] deserialize(byte[] data) { return decoded; }
			@Override public byte[] serialize(ItemStack[] items) { return new byte[] { 1 }; }
			@Override public boolean checkIsMCVersionCompatible() { return true; }
		};
		return new InventorySerializer(Logger.getLogger("InventorySerializerTest"), delegate, 2);
	}

	@Test
	void emptyBackpackIsSuccessful()
	{
		byte[] stored = { 1, 2, 3 };
		ItemStack[] emptyInventory = new ItemStack[54];
		Optional<ItemStack[]> result = withDecodedItems(emptyInventory).deserialize(stored, 2);
		assertTrue(result.isPresent());
		assertSame(emptyInventory, result.orElseThrow());
	}

	@Test
	void failedDecodeKeepsStoredDataOutOfCache()
	{
		byte[] stored = { 1, 2, 3 };
		Optional<ItemStack[]> result = withDecodedItems(null).deserialize(stored, 2);
		assertTrue(result.isEmpty());
	}
}
