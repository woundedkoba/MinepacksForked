package at.pcgamingfreaks.Minepacks.Bukkit.Database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BackpackFileStoreTest
{
	@TempDir Path directory;

	@Test
	void storesVersionAndBytesWithoutChangingTheFileFormat() throws IOException
	{
		File file = directory.resolve("player.backpack").toFile();
		byte[] data = { 4, 5, 6 };
		BackpackFileStore.write(file, 2, data);

		assertArrayEquals(new byte[] { 2, 4, 5, 6 }, java.nio.file.Files.readAllBytes(file.toPath()));
		BackpackFileStore.StoredBackpack stored = BackpackFileStore.read(file);
		assertEquals(2, stored.serializerVersion());
		assertArrayEquals(data, stored.data());
	}

	@Test
	void invalidReplacementLeavesExistingBytesUntouched() throws IOException
	{
		File file = directory.resolve("player.backpack").toFile();
		BackpackFileStore.write(file, 2, new byte[] { 7 });
		assertThrows(IllegalArgumentException.class, () -> BackpackFileStore.write(file, 2, null));
		assertArrayEquals(new byte[] { 2, 7 }, java.nio.file.Files.readAllBytes(file.toPath()));
	}

	@Test
	void incompleteFileIsNotAcceptedAsAnEmptyBackpack() throws IOException
	{
		File file = directory.resolve("player.backpack").toFile();
		java.nio.file.Files.write(file.toPath(), new byte[] { 2 });
		assertThrows(IOException.class, () -> BackpackFileStore.read(file));
	}
}
