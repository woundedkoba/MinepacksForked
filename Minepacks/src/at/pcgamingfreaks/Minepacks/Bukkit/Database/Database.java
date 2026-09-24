/*
 *   Copyright (C) 2023 GeorgH93
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
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package at.pcgamingfreaks.Minepacks.Bukkit.Database;

import at.pcgamingfreaks.ConsoleColor;
import at.pcgamingfreaks.Minepacks.Bukkit.API.Callback;
import at.pcgamingfreaks.Minepacks.Bukkit.Backpack;
import at.pcgamingfreaks.Minepacks.Bukkit.Database.UnCacheStrategies.OnDisconnect;
import at.pcgamingfreaks.Minepacks.Bukkit.Database.UnCacheStrategies.UnCacheStrategy;
import at.pcgamingfreaks.Minepacks.Bukkit.Minepacks;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public abstract class Database implements Listener
{
	/** Storage distinguishes a missing record from a record that could not be read. */
	protected interface LoadCallback
	{
		void found(Backpack backpack);
		void missing();
		void failed();
	}

	public static final String MESSAGE_UNKNOWN_DB_TYPE = ConsoleColor.RED + "Unknown database type \"%s\"!" + ConsoleColor.RESET;

	protected final Minepacks plugin;
	protected final InventorySerializer itsSerializer;
	protected final boolean onlineUUIDs, bungeeCordMode, forceSaveOnUnload;
	protected boolean useUUIDSeparators, asyncSave = true;
	protected long maxAge;
	private final Map<UUID, Backpack> backpacks = new ConcurrentHashMap<>();
	private final UnCacheStrategy unCacheStrategy;
	private final File backupFolder;

	public Database(Minepacks mp)
	{
		plugin = mp;
		itsSerializer = new InventorySerializer(plugin.getLogger());
		useUUIDSeparators = plugin.getConfiguration().getUseUUIDSeparators();
		onlineUUIDs = plugin.getConfiguration().useOnlineUUIDs();
		bungeeCordMode = plugin.getConfiguration().isBungeeCordModeEnabled();
		forceSaveOnUnload = plugin.getConfiguration().isForceSaveOnUnloadEnabled();
		maxAge = plugin.getConfiguration().getAutoCleanupMaxInactiveDays();
		unCacheStrategy = bungeeCordMode ? new OnDisconnect(this) : UnCacheStrategy.getUnCacheStrategy(this);
		backupFolder = new File(this.plugin.getDataFolder(), "backups");
		if(!backupFolder.exists() && !backupFolder.mkdirs()) mp.getLogger().info("Failed to create backups folder.");
	}

	public void init()
	{
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public void close()
	{
		HandlerList.unregisterAll(this);
		asyncSave = false;
		backpacks.forEach((key, value) -> { if (forceSaveOnUnload) { value.setChanged(); } value.closeAll(); });
		backpacks.clear();
		unCacheStrategy.close();
	}

	public static @Nullable Database getDatabase(Minepacks plugin)
	{
		try
		{
			String dbType = plugin.getConfiguration().getDatabaseType();
			if(dbType.equals("shared") || dbType.equals("external") || dbType.equals("global"))
			{
				plugin.getLogger().warning(ConsoleColor.RED + "PluginLib shared database pools are no longer available. Copy the old connection and table settings to Minepacks, then set Database.Type to mysql." + ConsoleColor.RESET);
				return null;
			}
			Database database;
			switch(dbType)
			{
				case "mysql": database = new MySQL(plugin); break;
				case "sqlite": database = new SQLite(plugin); break;
				case "flat":
				case "file":
				case "files":
					database = new Files(plugin); break;
				default: plugin.getLogger().warning(String.format(MESSAGE_UNKNOWN_DB_TYPE,  plugin.getConfiguration().getDatabaseType())); return null;
			}
			database.init();
			return database;
		}
		catch(IllegalStateException e)
		{
			plugin.getLogger().log(Level.SEVERE, "Failed to initialize database.", e);
		}
		catch(Exception e)
		{
			plugin.getLogger().log(Level.SEVERE, "Failed to initialize database.", e);
		}
		return null;
	}

	public void backup(@NotNull Backpack backpack)
	{
		writeBackup(backpack.getOwner().getName(), getPlayerFormattedUUID(backpack.getOwnerId()), itsSerializer.getUsedSerializer(), itsSerializer.serialize(backpack.getInventory()));
	}

	protected boolean writeBackup(@Nullable String userName, @NotNull String userIdentifier, final int usedSerializer, final byte[] data)
	{
		if(data == null || data.length == 0)
		{
			plugin.getLogger().severe("Failed to serialize backpack backup for " + userIdentifier + "; no backup file was written.");
			return false;
		}
		if(userIdentifier.equalsIgnoreCase(userName)) userName = null;
		if(userName != null) userIdentifier = userName + "_" + userIdentifier;
		final File save = new File(backupFolder, userIdentifier + "_" + System.currentTimeMillis() + Files.EXT);
		try
		{
			BackpackFileStore.write(save, usedSerializer, data);
			plugin.getLogger().info("Backup of the backpack has been created: " + save.getAbsolutePath());
			return true;
		}
		catch(Exception e)
		{
			plugin.getLogger().warning(ConsoleColor.RED + "Failed to write backup! Error: " + e.getMessage() + ConsoleColor.RESET);
			return false;
		}
	}

	public @Nullable ItemStack[] loadBackup(final String backupName)
	{
		File backup = new File(backupFolder, backupName + Files.EXT);
		return Files.readFile(itsSerializer, backup, plugin.getLogger());
	}

	public ArrayList<String> getBackups()
	{
		File[] files = backupFolder.listFiles((dir, name) -> name.endsWith(Files.EXT));
		if(files != null)
		{
			ArrayList<String> backups = new ArrayList<>(files.length);
			for(File file : files)
			{
				if(!file.isFile()) continue;
				backups.add(file.getName().replaceAll(Files.EXT_REGEX, ""));
			}
			return backups;
		}
		return new ArrayList<>();
	}

	protected String getPlayerFormattedUUID(OfflinePlayer player)
	{
		return getPlayerFormattedUUID(player.getUniqueId());
	}

	protected String getPlayerFormattedUUID(UUID uuid)
	{
		return (useUUIDSeparators) ? uuid.toString() : uuid.toString().replace("-", "");
	}

	public @NotNull Collection<Backpack> getLoadedBackpacks()
	{
		return backpacks.values();
	}

	/**
	 * Gets a backpack for a player. This only includes backpacks that are cached! Do not use it unless you are sure that you only want to use cached data!
	 *
	 * @param player The player whose backpack should be retrieved.
	 * @return The backpack for the player. null if the backpack is not in the cache.
	 */
	public @Nullable Backpack getBackpack(@Nullable OfflinePlayer player)
	{
		return (player == null) ? null : backpacks.get(player.getUniqueId());
	}

	public void getBackpack(final OfflinePlayer player, final Callback<at.pcgamingfreaks.Minepacks.Bukkit.API.Backpack> callback, final boolean createNewIfMissing)
	{
		if(player == null || player.getClass().getName().contains("NPC"))
		{
			return;
		}
		Backpack lbp = backpacks.get(player.getUniqueId());
		if(lbp == null)
		{
			loadAndCache(player, callback, createNewIfMissing);
		}
		else
		{
			callback.onResult(lbp);
		}
	}

	public void getBackpack(final OfflinePlayer player, final Callback<at.pcgamingfreaks.Minepacks.Bukkit.API.Backpack> callback)
	{
		getBackpack(player, callback, true);
	}

	public void unloadBackpack(Backpack backpack)
	{
		if (forceSaveOnUnload)
		{
			backpack.forceSave();
		}
		else
		{
			backpack.save();
		}
		backpacks.remove(backpack.getOwnerId());
	}

	public void asyncLoadBackpack(final OfflinePlayer player)
	{
		if(player != null && backpacks.get(player.getUniqueId()) == null)
		{
			loadAndCache(player, backpack -> {}, true);
		}
	}

	private void loadAndCache(final OfflinePlayer player, final Callback<at.pcgamingfreaks.Minepacks.Bukkit.API.Backpack> callback, final boolean createNewIfMissing)
	{
		loadBackpack(player, new LoadCallback()
		{
			@Override
			public void found(Backpack backpack)
			{
				backpacks.put(player.getUniqueId(), backpack);
				callback.onResult(backpack);
			}

			@Override
			public void missing()
			{
				if(!createNewIfMissing)
				{
					callback.onFail();
					return;
				}
				Backpack backpack = new Backpack(player);
				backpacks.put(player.getUniqueId(), backpack);
				callback.onResult(backpack);
			}

			@Override
			public void failed()
			{
				callback.onFail();
			}
		});
	}

	@EventHandler
	public void onPlayerLoginEvent(PlayerJoinEvent event)
	{
		updatePlayerAndLoadBackpack(event.getPlayer());
	}

	// DB Functions
	public void updatePlayerAndLoadBackpack(Player player)
	{
		updatePlayer(player);
		if(!bungeeCordMode) asyncLoadBackpack(player);
	}

	public abstract void updatePlayer(Player player);

	public abstract void saveBackpack(Backpack backpack);

	public void syncCooldown(Player player, long time) {}

	public void getCooldown(final Player player, final Callback<Long> callback) {}

	protected abstract void loadBackpack(final OfflinePlayer player, final LoadCallback callback);
}
