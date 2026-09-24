/*
 *   Copyright (C) 2022 GeorgH93
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

package at.pcgamingfreaks.Minepacks.Bukkit.Database.Migration;

import at.pcgamingfreaks.Minepacks.Bukkit.Database.Database;
import at.pcgamingfreaks.Minepacks.Bukkit.Database.MySQL;
import at.pcgamingfreaks.Minepacks.Bukkit.Database.SQL;
import at.pcgamingfreaks.Minepacks.Bukkit.Database.SQLite;
import at.pcgamingfreaks.Minepacks.Bukkit.Minepacks;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public abstract class ToSQLMigration extends Migration
{
	protected final SQL newDb;

	protected ToSQLMigration(@NotNull Minepacks plugin, @NotNull Database oldDb, @NotNull String dbType)
	{
		super(plugin, oldDb);
		switch(dbType)
		{
			case "mysql": newDb = new MySQL(plugin); break;
			case "sqlite":
				final File dbFile = new File(SQLite.getDbFile(plugin));
				if(dbFile.exists() && !dbFile.renameTo(new File(SQLite.getDbFile(plugin) + ".old_" + System.currentTimeMillis())))
				{
					plugin.getLogger().warning("Failed to rename old database file.");
				}
				newDb = new SQLite(plugin);
				break;
			default: throw new IllegalArgumentException("Unsupported SQL migration target: " + dbType);
		}
	}
}
