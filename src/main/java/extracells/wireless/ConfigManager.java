package extracells.wireless;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.ViewItems;
import appeng.api.util.IConfigManager;
import net.minecraft.nbt.NBTTagCompound;

public class ConfigManager implements IConfigManager {

	private final Map<Settings, Enum<?>> settings = new EnumMap<Settings, Enum<?>>(Settings.class);
	private final NBTTagCompound tagCompound;

	public ConfigManager() {
		this(null);
	}

	public ConfigManager(NBTTagCompound tag) {
		tagCompound = tag;
		if (tag != null) {
			registerSetting(Settings.SORT_BY, SortOrder.NAME);
			registerSetting(Settings.VIEW_MODE, ViewItems.ALL);
			registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING);

			readFromNBT(tag.copy());
		}

	}

	@Override
	public Set<Settings> getSettings() {
		return settings.keySet();
	}

	@Override
	public void registerSetting(Settings settingName, Enum<?> defaultValue) {
		settings.put(settingName, defaultValue);
	}

	@Override
	public Enum<?> getSetting(Settings settingName) {
		final Enum<?> oldValue = settings.get(settingName);

		if (oldValue != null) {
			return oldValue;
		}

		throw new IllegalStateException("Invalid Config setting. Expected a non-null value for " + settingName);
	}

	@Override
	public Enum<?> putSetting(Settings settingName, Enum<?> newValue) {
		final Enum<?> oldValue = getSetting(settingName);
		settings.put(settingName, newValue);
		if (tagCompound != null) {
			writeToNBT(tagCompound);
		}
		return oldValue;
	}

	@Override
	public void writeToNBT(NBTTagCompound tagCompound) {
		for (final Map.Entry<Settings, Enum<?>> entry : settings.entrySet()) {
			tagCompound.setString(entry.getKey().name(), settings.get(entry.getKey()).toString());
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		for (final Map.Entry<Settings, Enum<?>> entry : settings.entrySet()) {
			try {
				if (tagCompound.hasKey(entry.getKey().name())) {
					String value = tagCompound.getString(entry.getKey().name());

					final Enum<?> oldValue = settings.get(entry.getKey());

					final Enum<?> newValue = Enum.valueOf(oldValue.getClass(), value);

					putSetting(entry.getKey(), newValue);
				}
			}
			catch (final IllegalArgumentException e) {
			}
		}
	}

}