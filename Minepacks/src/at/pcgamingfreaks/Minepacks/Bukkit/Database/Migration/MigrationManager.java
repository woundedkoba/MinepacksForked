/*
 *   Copyright (C) 2019 GeorgH93
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

import at.pcgamingfreaks.ConsoleColor;
import at.pcgamingfreaks.Minepacks.Bukkit.Database.*;
import at.pcgamingfreaks.Minepacks.Bukkit.Minepacks;

import java.util.Locale;
import java.util.logging.Level;

public class MigrationManager
{
	private final Minepacks plugin;

	public MigrationManager(final Minepacks plugin)
	{
		this.plugin = plugin;
	}

	public void migrateDB(final String targetDatabaseType, final MigrationCallback callback)
	{
		final Migration migration = getMigrationPerformer(targetDatabaseType);
		if(migration == null)
		{
			callback.onResult(new MigrationResult("There is no need to migrate the database.", MigrationResult.MigrationResultType.NOT_NEEDED));
			return;
		}
		final Database db = plugin.getDatabase();

		try
		{
			plugin.getLogger().info("Unloading plugin for migration");
			plugin.suspendForMigration();
		}
		catch(Exception e)
		{
			plugin.getLogger().warning(ConsoleColor.RED + "Failed to unload plugin! Please restart your server!" + ConsoleColor.RESET);
			plugin.getLogger().log(Level.SEVERE, "Failed to suspend Minepacks for migration.", e);
			callback.onResult(new MigrationResult("Failed to unload plugin! Please restart your server!", MigrationResult.MigrationResultType.ERROR));
			return;
		}
		Minepacks.getScheduler().runAsync(task -> {
			MigrationResult result;
			try
			{
				plugin.getLogger().info("Start migrating data to new database");
				result = migration.migrate();
				if(result.getType() == MigrationResult.MigrationResultType.SUCCESS)
				{
					plugin.getConfiguration().setDatabaseType(targetDatabaseType);
				}
			}
			catch(Exception e)
			{
				plugin.getLogger().log(Level.SEVERE, "There was a problem migrating from " + db.getClass().getName() + " to " + targetDatabaseType, e);
				result = new MigrationResult("There was a problem migrating from " + db.getClass().getName() + " to " + targetDatabaseType + ". Please check the console for details.", MigrationResult.MigrationResultType.ERROR);
			}
			MigrationResult completed = result;
			Minepacks.getScheduler().runNextTick(nextTick -> finishMigration(db, completed, callback));
		});
	}

	private void finishMigration(Database source, MigrationResult result, MigrationCallback callback)
	{
		try
		{
			source.close();
		}
		catch(Exception e)
		{
			plugin.getLogger().log(Level.SEVERE, "Failed to close the source database after migration.", e);
			result = new MigrationResult("Failed to close the source database after migration. Please restart your server!", MigrationResult.MigrationResultType.ERROR);
		}
		try
		{
			plugin.getLogger().info("Migration is done, loading the plugin again.");
			plugin.loadServices();
			if(plugin.getDatabase() == null) throw new IllegalStateException("Target database did not initialize after migration");
			plugin.getLogger().info(ConsoleColor.GREEN + "Plugin loaded successful and is ready to use again." + ConsoleColor.RESET);
		}
		catch(Exception e)
		{
			plugin.getLogger().log(Level.SEVERE, "Failed to start plugin after migration.", e);
			result = new MigrationResult("Failed to start plugin after migration. Please restart your server!", MigrationResult.MigrationResultType.ERROR);
		}
		callback.onResult(result);
	}

	public Migration getMigrationPerformer(String targetDatabaseType)
	{
		try
		{
			if(targetDatabaseType.toLowerCase(Locale.ROOT).equals("external") || targetDatabaseType.toLowerCase(Locale.ROOT).equals("global") || targetDatabaseType.toLowerCase(Locale.ROOT).equals("shared"))
			{
				plugin.getLogger().warning(ConsoleColor.RED + "PluginLib shared database pools are no longer available. Configure Minepacks MySQL settings before migration." + ConsoleColor.RESET);
				return null;
			}
			switch(targetDatabaseType.toLowerCase(Locale.ROOT))
			{
				case "flat":
				case "file":
				case "files":
					if(!(plugin.getDatabase() instanceof SQL)) return null;
					return new SQLtoFilesMigration(plugin, (SQL) plugin.getDatabase());
				case "mysql":
					if(plugin.getDatabase() instanceof MySQL) return null;
					if(plugin.getDatabase() instanceof SQL) return new SQLtoSQLMigration(plugin, (SQL) plugin.getDatabase(), "mysql");
					else return new FilesToSQLMigration(plugin, (Files) plugin.getDatabase(), "mysql");
				case "sqlite":
					if(plugin.getDatabase() instanceof SQLite) return null;
					if(plugin.getDatabase() instanceof SQL) return new SQLtoSQLMigration(plugin, (SQL) plugin.getDatabase(), "sqlite");
					else return new FilesToSQLMigration(plugin, (Files) plugin.getDatabase(), "sqlite");
				default: plugin.getLogger().warning(String.format(Database.MESSAGE_UNKNOWN_DB_TYPE,  plugin.getConfiguration().getDatabaseType())); return null;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
}
