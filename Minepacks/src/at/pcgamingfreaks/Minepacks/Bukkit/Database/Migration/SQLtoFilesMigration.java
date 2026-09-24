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
import at.pcgamingfreaks.Minepacks.Bukkit.Database.SQL;
import at.pcgamingfreaks.Minepacks.Bukkit.Minepacks;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class SQLtoFilesMigration extends Migration
{
	private final String sqlQuery;
	private final File saveFolder;

	protected SQLtoFilesMigration(@NotNull Minepacks plugin, @NotNull SQL oldDb)
	{
		super(plugin, oldDb);
		@Language("SQL") String query = "SELECT {FieldUUID},{FieldBPITS},{FieldBPVersion} FROM {TablePlayers} INNER JOIN {TableBackpacks} ON {FieldPlayerID}={FieldBPOwner};";
		sqlQuery = oldDb.formatMigrationQuery(query);
		saveFolder = new File(this.plugin.getDataFolder(), Files.FOLDER_NAME);
		if(!saveFolder.exists() && !saveFolder.mkdirs()) plugin.getLogger().warning("Failed to create save folder (" + saveFolder.getAbsolutePath() + ").");
	}

	@Override
	public @NotNull MigrationResult migrate() throws Exception
	{
		int migrated = 0;
		try(Connection connection = ((SQL) oldDb).getConnection(); Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sqlQuery))
		{
			while(rs.next())
			{
				BackpackFileStore.write(new File(saveFolder, rs.getString(1) + Files.EXT), rs.getInt(3), rs.getBytes(2));
				migrated++;
			}
		}
		return new MigrationResult("Migrated " + migrated + " backpacks from " + oldDb.getClass().getSimpleName() + " to Files.", MigrationResult.MigrationResultType.SUCCESS);
	}
}
