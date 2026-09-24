/*
 *   Copyright (C) 2026 MinepacksForked contributors
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 */

package at.pcgamingfreaks.Minepacks.Bukkit.Database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** The on-disk format is one unsigned serializer-version byte followed by item bytes. */
public final class BackpackFileStore
{
	public record StoredBackpack(int serializerVersion, byte[] data) {}

	private BackpackFileStore() {}

	public static StoredBackpack read(File file) throws IOException
	{
		try(FileInputStream input = new FileInputStream(file))
		{
			int version = input.read();
			byte[] data = input.readAllBytes();
			if(version < 0 || data.length == 0) throw new IOException("Backpack file is incomplete: " + file);
			return new StoredBackpack(version, data);
		}
	}

	public static void write(File file, int serializerVersion, byte[] data) throws IOException
	{
		if(serializerVersion < 0 || serializerVersion > 255 || data == null || data.length == 0)
		{
			throw new IllegalArgumentException("Backpack file requires a serializer version and nonempty item data");
		}

		Path target = file.toPath().toAbsolutePath();
		Path temporary = java.nio.file.Files.createTempFile(target.getParent(), file.getName(), ".tmp");
		try
		{
			try(FileOutputStream output = new FileOutputStream(temporary.toFile()))
			{
				output.write(serializerVersion);
				output.write(data);
				output.getFD().sync();
			}
			try
			{
				java.nio.file.Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}
			catch(AtomicMoveNotSupportedException e)
			{
				java.nio.file.Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		finally
		{
			java.nio.file.Files.deleteIfExists(temporary);
		}
	}
}
