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

import at.pcgamingfreaks.Minepacks.Bukkit.Database.SQL;
import at.pcgamingfreaks.Minepacks.Bukkit.Database.SQLite;
import at.pcgamingfreaks.Minepacks.Bukkit.Minepacks;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressWarnings("ConstantConditions")
public class SQLtoSQLMigration extends ToSQLMigration
{
	private final @Language("SQL") String queryInsertUsers, queryInsertBackpacks;
	private final SQL.MigrationColumns oldColumns;

	protected SQLtoSQLMigration(@NotNull Minepacks plugin, @NotNull SQL oldDb, @NotNull String dbType) throws Exception
	{
		super(plugin, oldDb, dbType);

		queryInsertUsers = newDb.formatMigrationQuery("INSERT INTO {TablePlayers} ({FieldPlayerID},{FieldName},{FieldUUID}) VALUES (?,?,?);");
		queryInsertBackpacks = newDb.formatMigrationQuery("INSERT INTO {TableBackpacks} ({FieldBPOwner},{FieldBPITS},{FieldBPVersion},{FieldBPLastUpdate}) VALUES (?,?,?,?);");
		oldColumns = oldDb.migrationColumns();
	}

	@Override
	public @NotNull MigrationResult migrate() throws Exception
	{
		try(Connection readConnection = ((SQL) oldDb).getConnection(); Connection writeConnection = newDb.getConnection(); Statement readStatement = readConnection.createStatement())
		{
			writeConnection.setAutoCommit(false);
			try
			{
				int users = migrate("users", writeConnection, readStatement, "SELECT * FROM {TablePlayers};", queryInsertUsers, this::migrateUser);
				int backpacks = migrate("backpacks", writeConnection, readStatement, "SELECT * FROM {TableBackpacks};", queryInsertBackpacks, this::migrateBackpack);
				writeConnection.commit();
				return new MigrationResult("Migrated " + users + " users and " + backpacks + " backpacks from " + oldDb.getClass().getSimpleName() + " to " + newDb.getClass().getSimpleName() + ".", MigrationResult.MigrationResultType.SUCCESS);
			}
			catch(Exception e)
			{
				try { writeConnection.rollback(); } catch(SQLException rollbackFailure) { e.addSuppressed(rollbackFailure); }
				throw e;
			}
		}
		finally
		{
			newDb.close();
		}
	}

	@FunctionalInterface
	private interface RowBinder
	{
		void bind(ResultSet row, PreparedStatement statement) throws Exception;
	}

	private int migrate(@NotNull String type, @NotNull Connection writeConnection, @NotNull Statement readStatement,
	                    @Language("SQL") String readQuery, @Language("SQL") String insertQuery, RowBinder binder) throws Exception
	{
		int count = 0;
		plugin.getLogger().info("Migrate " + type + " ...");
		try(ResultSet resultSet = readStatement.executeQuery(((SQL) oldDb).formatMigrationQuery(readQuery));
		    PreparedStatement preparedStatement = writeConnection.prepareStatement(insertQuery))
		{
			while(resultSet.next())
			{
				binder.bind(resultSet, preparedStatement);
				preparedStatement.addBatch();
				count++;
			}
			preparedStatement.executeBatch();
		}
		plugin.getLogger().info("Migrated " + count + " " + type + ".");
		return count;
	}

	private void migrateUser(@NotNull ResultSet usersResultSet, @NotNull PreparedStatement preparedStatement) throws Exception
	{
		int userId = usersResultSet.getInt(oldColumns.playerId());
		preparedStatement.setInt(1, userId);
		preparedStatement.setString(2, usersResultSet.getString(oldColumns.playerName()));
		preparedStatement.setString(3, usersResultSet.getString(oldColumns.playerUuid()));
	}

	private void migrateBackpack(@NotNull ResultSet backpacksResultSet, @NotNull PreparedStatement preparedStatement) throws Exception
	{
		preparedStatement.setInt(1, backpacksResultSet.getInt(oldColumns.backpackOwner()));
		preparedStatement.setBytes(2, backpacksResultSet.getBytes(oldColumns.backpackItems()));
		preparedStatement.setInt(3, backpacksResultSet.getInt(oldColumns.backpackVersion()));
		final DateFormat sqliteDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		if(oldDb instanceof SQLite)
		{
			preparedStatement.setTimestamp(4, new Timestamp(sqliteDateFormat.parse(backpacksResultSet.getString(oldColumns.backpackLastUpdate())).getTime()));
		}
		else
		{
			preparedStatement.setString(4, sqliteDateFormat.format(new Date(backpacksResultSet.getTimestamp(oldColumns.backpackLastUpdate()).getTime())));
		}
	}
}
