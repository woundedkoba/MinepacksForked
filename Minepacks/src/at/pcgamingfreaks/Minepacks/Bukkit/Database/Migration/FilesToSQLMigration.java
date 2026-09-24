/*
 *   Copyright (C) 2018 GeorgH93
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

import at.pcgamingfreaks.Minepacks.Bukkit.Database.Files;
import at.pcgamingfreaks.Minepacks.Bukkit.Database.BackpackFileStore;
import at.pcgamingfreaks.Minepacks.Bukkit.Minepacks;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FilesToSQLMigration extends ToSQLMigration
{
	private final @Language("SQL") String queryInsertUsers, queryInsertBackpacks;
	private final File saveFolder;

	protected FilesToSQLMigration(@NotNull Minepacks plugin, @NotNull Files oldDb, @NotNull String dbType) throws Exception
	{
		super(plugin, oldDb, dbType);
		saveFolder = new File(this.plugin.getDataFolder(), Files.FOLDER_NAME);

		queryInsertUsers = newDb.formatMigrationQuery("INSERT INTO {TablePlayers} ({FieldUUID},{FieldName}) VALUES (?,?);");
		queryInsertBackpacks = newDb.formatMigrationQuery("INSERT INTO {TableBackpacks} ({FieldBPOwner},{FieldBPITS},{FieldBPVersion}) VALUES (?,?,?);");
	}

	@Override
	public @NotNull MigrationResult migrate() throws Exception
	{
		File[] allFiles = saveFolder.listFiles((dir, name) -> name.endsWith(Files.EXT));
		try
		{
			if(allFiles == null) throw new IOException("Unable to list backpack files in " + saveFolder);
			return migrateFiles(allFiles);
		}
		finally
		{
			newDb.close();
		}
	}

	private MigrationResult migrateFiles(File[] allFiles) throws Exception
	{
		try(Connection connection = newDb.getConnection(); PreparedStatement statementInsertUser = connection.prepareStatement(queryInsertUsers, PreparedStatement.RETURN_GENERATED_KEYS);
		    PreparedStatement statementInsertBackpack = connection.prepareStatement(queryInsertBackpacks))
		{
			connection.setAutoCommit(false);
			try
			{
				int migrated = 0;
				for(File file : allFiles)
				{
					BackpackFileStore.StoredBackpack stored = BackpackFileStore.read(file);
					String name = file.getName().substring(0, file.getName().length() - Files.EXT.length());
					statementInsertUser.setString(1, name);
					statementInsertUser.setString(2, "UNKNOWN");
					statementInsertUser.executeUpdate();
					try(ResultSet rs = statementInsertUser.getGeneratedKeys())
					{
						if(rs.next())
						{
							statementInsertBackpack.setInt(1, rs.getInt(1));
							statementInsertBackpack.setBytes(2, stored.data());
							statementInsertBackpack.setInt(3, stored.serializerVersion());
							statementInsertBackpack.executeUpdate();
							migrated++;
						}
					}
				}
				connection.commit();
				return new MigrationResult("Migrated " + migrated + " backpacks from Files to " + newDb.getClass().getSimpleName(), MigrationResult.MigrationResultType.SUCCESS);
			}
			catch(Exception e)
			{
				try { connection.rollback(); } catch(SQLException rollbackFailure) { e.addSuppressed(rollbackFailure); }
				throw e;
			}
		}
	}
}
