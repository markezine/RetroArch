package org.retroarch.browser;

import java.io.*;

import org.retroarch.R;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

public class MainMenuActivity extends PreferenceActivity {
	private static MainMenuActivity instance = null;
	static private final String TAG = "MainMenu";
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		instance = this;
		addPreferencesFromResource(R.xml.prefs);
		PreferenceManager.setDefaultValues(this, R.xml.prefs, false);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		// Extracting assets appears to take considerable amount of time, so
		// move extraction to a thread.
		Thread assetThread = new Thread(new Runnable() {
			public void run() {
				extractAssets();
			}
		});
		assetThread.start();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
		if (!prefs.getBoolean("first_time_refreshrate_calculate", false)) {
			prefs.edit().putBoolean("first_time_refreshrate_calculate", true).commit();
			
			if (!detectDevice(false))
			{
				AlertDialog.Builder alert = new AlertDialog.Builder(this)
				.setTitle("Welcome to RetroArch")
				.setMessage("This is your first time starting up RetroArch. RetroArch will now be preconfigured for the best possible gameplay experience. Please be aware that it might take some time until all shader and overlay assets are extracted...")
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
						SharedPreferences.Editor edit = prefs.edit();
						edit.putBoolean("video_threaded", true);
						edit.commit();
					}
				});
			alert.show();
			}
			else
			{
				AlertDialog.Builder alert = new AlertDialog.Builder(this)
				.setTitle("Welcome to RetroArch")
				.setMessage("This is your first time starting up RetroArch. Please be aware that it might take some time until all shader and overlay assets are extracted...")
				.setPositiveButton("OK", null);
			alert.show();
			}
		}
	}
	
	public static MainMenuActivity getInstance() {
        return instance;
    }
	
	private final double getDisplayRefreshRate() {
		// Android is *very* likely to screw this up.
		// It is rarely a good value to use, so make sure it's not
		// completely wrong. Some phones return refresh rates that are completely bogus
		// (like 0.3 Hz, etc), so try to be very conservative here.
		final WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		final Display display = wm.getDefaultDisplay();
		double rate = display.getRefreshRate();
		if (rate > 61.0 || rate < 58.0)
			rate = 59.95;
		return rate;
	}

	public static final double getRefreshRate() {
		double rate = 0;
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(MainMenuActivity.getInstance().getBaseContext());
		String refresh_rate = prefs.getString("video_refresh_rate", "");
		if (!refresh_rate.isEmpty()) {
			try {
				rate = Double.parseDouble(refresh_rate);
			} catch (NumberFormatException e) {
				Log.e(TAG, "Cannot parse: " + refresh_rate + " as a double!");
				rate = MainMenuActivity.getInstance().getDisplayRefreshRate();
			}
		} else {
			rate = MainMenuActivity.getInstance().getDisplayRefreshRate();
		}
		
		Log.i(TAG, "Using refresh rate: " + rate + " Hz.");
		return rate;
	}
	
	public static String readCPUInfo() {
		String result = "";

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream("/proc/cpuinfo")));

			String line;
			while ((line = br.readLine()) != null)
				result += line + "\n";
			br.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return result;
	}
	
	@TargetApi(17)
	public static int getLowLatencyOptimalSamplingRate() {
		AudioManager manager = (AudioManager)MainMenuActivity.getInstance().getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		return Integer.parseInt(manager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
	}
	
	public static int getOptimalSamplingRate() {
		int ret;
		if (android.os.Build.VERSION.SDK_INT >= 17)
			ret = getLowLatencyOptimalSamplingRate();
		else
			ret = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
		
		Log.i(TAG, "Using sampling rate: " + ret + " Hz");
		return ret;
	}
	
	public static String getDefaultConfigPath() {
		String internal = System.getenv("INTERNAL_STORAGE");
		String external = System.getenv("EXTERNAL_STORAGE");
		
		if (external != null) {
			String confPath = external + File.separator + "retroarch.cfg";
			if (new File(confPath).exists())
				return confPath;
		} else if (internal != null) {
			String confPath = internal + File.separator + "retroarch.cfg";
			if (new File(confPath).exists())
				return confPath;
		} else {
			String confPath = "/mnt/extsd/retroarch.cfg";
			if (new File(confPath).exists())
				return confPath;
		}
		
		if (internal != null && new File(internal + File.separator + "retroarch.cfg").canWrite())
			return internal + File.separator + "retroarch.cfg";
		else if (external != null && new File(internal + File.separator + "retroarch.cfg").canWrite())
			return external + File.separator + "retroarch.cfg";
		else if ((MainMenuActivity.getInstance().getApplicationInfo().dataDir) != null)
			return (MainMenuActivity.getInstance().getApplicationInfo().dataDir) + File.separator + "retroarch.cfg";
		else // emergency fallback, all else failed
			return "/mnt/sd/retroarch.cfg";
	}
	
	public static void updateConfigFile() {
		ConfigFile config;
		try {
			config = new ConfigFile(new File(getDefaultConfigPath()));
		} catch (IOException e) {
			config = new ConfigFile();
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainMenuActivity.getInstance().getBaseContext());
		config.setString("libretro_path", MainMenuActivity.getInstance().getApplicationInfo().nativeLibraryDir);
		config.setBoolean("audio_rate_control", prefs.getBoolean("audio_rate_control", true));
		config.setInt("audio_out_rate", MainMenuActivity.getOptimalSamplingRate());
		config.setInt("audio_latency", prefs.getBoolean("audio_high_latency", false) ? 160 : 64);
		config.setBoolean("audio_enable", prefs.getBoolean("audio_enable", true));
		config.setBoolean("video_smooth", prefs.getBoolean("video_smooth", true));
		config.setBoolean("video_allow_rotate", prefs.getBoolean("video_allow_rotate", true));
		config.setBoolean("savestate_auto_load", prefs.getBoolean("savestate_auto_load", true));
		config.setBoolean("savestate_auto_save", prefs.getBoolean("savestate_auto_save", false));
		config.setBoolean("rewind_enable", prefs.getBoolean("rewind_enable", false));
		config.setBoolean("video_vsync", prefs.getBoolean("video_vsync", true));
		config.setBoolean("input_autodetect_enable", prefs.getBoolean("input_autodetect_enable", true));
		config.setBoolean("input_debug_enable", prefs.getBoolean("input_debug_enable", false));
		config.setInt("input_back_behavior", Integer.valueOf(prefs.getString("input_back_behavior", "0")));
		config.setInt("input_autodetect_icade_profile_pad1", Integer.valueOf(prefs.getString("input_autodetect_icade_profile_pad1", "0")));
		config.setInt("input_autodetect_icade_profile_pad2", Integer.valueOf(prefs.getString("input_autodetect_icade_profile_pad2", "0")));
		config.setInt("input_autodetect_icade_profile_pad3", Integer.valueOf(prefs.getString("input_autodetect_icade_profile_pad3", "0")));
		config.setInt("input_autodetect_icade_profile_pad4", Integer.valueOf(prefs.getString("input_autodetect_icade_profile_pad4", "0")));
		
		config.setDouble("video_refresh_rate", MainMenuActivity.getRefreshRate());
		config.setBoolean("video_threaded", prefs.getBoolean("video_threaded", true));
		
		String aspect = prefs.getString("video_aspect_ratio", "auto");
		if (aspect.equals("full")) {
			config.setBoolean("video_force_aspect", false);
		} else if (aspect.equals("auto")) {
			config.setBoolean("video_force_aspect", true);
			config.setBoolean("video_force_aspect_auto", true);
			config.setDouble("video_aspect_ratio", -1.0);
		} else if (aspect.equals("square")) {
			config.setBoolean("video_force_aspect", true);
			config.setBoolean("video_force_aspect_auto", false);
			config.setDouble("video_aspect_ratio", -1.0);
		} else {
			double aspect_ratio = Double.parseDouble(aspect);
			config.setBoolean("video_force_aspect", true);
			config.setDouble("video_aspect_ratio", aspect_ratio);
		}
		
		config.setBoolean("video_scale_integer", prefs.getBoolean("video_scale_integer", false));
		
		String shaderPath = prefs.getString("video_shader", "");
		config.setString("video_shader", shaderPath);
		config.setBoolean("video_shader_enable",
				prefs.getBoolean("video_shader_enable", false)
						&& new File(shaderPath).exists());

		boolean useOverlay = prefs.getBoolean("input_overlay_enable", true);
		if (useOverlay) {
			String overlayPath = prefs.getString("input_overlay", (MainMenuActivity.getInstance().getApplicationInfo().dataDir) + "/overlays/snes-landscape.cfg");
			config.setString("input_overlay", overlayPath);
			config.setDouble("input_overlay_opacity", prefs.getFloat("input_overlay_opacity", 1.0f));
		} else {
			config.setString("input_overlay", "");
		}
		
		config.setString("savefile_directory", prefs.getBoolean("savefile_directory_enable", false) ?
				prefs.getString("savefile_directory", "") : "");
		config.setString("savestate_directory", prefs.getBoolean("savestate_directory_enable", false) ?
				prefs.getString("savestate_directory", "") : "");
		config.setString("system_directory", prefs.getBoolean("system_directory_enable", false) ?
				prefs.getString("system_directory", "") : "");
		
		config.setBoolean("video_font_enable", prefs.getBoolean("video_font_enable", true));
		
		for (int i = 1; i <= 4; i++)
		{
			final String btns[] = {"up", "down", "left", "right", "a", "b", "x", "y", "start", "select", "l", "r", "l2", "r2", "l3", "r3" };
			for (String b : btns)
			{
				String p = "input_player" + String.valueOf(i) + "_" + b + "_btn";
				config.setInt(p, prefs.getInt(p, 0));
			}
		}

		String confPath = getDefaultConfigPath();
		try {
			config.write(new File(confPath));
		} catch (IOException e) {
			Log.e(TAG, "Failed to save config file to: " + confPath);
		}
	}
	
	private byte[] loadAsset(String asset) throws IOException {
		String path = asset;
		InputStream stream = getAssets().open(path);
		int len = stream.available();
		byte[] buf = new byte[len];
		stream.read(buf, 0, len);
		return buf;
	}
	
	private void extractAssets(AssetManager manager, String dataDir, String relativePath, int level) throws IOException {
		final String[] paths = manager.list(relativePath);
		if (paths != null && paths.length > 0) { // Directory
			//Log.d(TAG, "Extracting assets directory: " + relativePath);
			for (final String path : paths)
				extractAssets(manager, dataDir, relativePath + (level > 0 ? File.separator : "") + path, level + 1);
		} else { // File, extract.
			//Log.d(TAG, "Extracting assets file: " + relativePath);
			
			String parentPath = new File(relativePath).getParent();
			if (parentPath != null) {
				File parentFile = new File(dataDir, parentPath);
				parentFile.mkdirs(); // Doesn't throw.
			}
			
			byte[] asset = loadAsset(relativePath);
			BufferedOutputStream writer = new BufferedOutputStream(
					new FileOutputStream(new File(dataDir, relativePath)));

			writer.write(asset, 0, asset.length);
			writer.flush();
			writer.close();
		}
	}
	
	private void extractAssets() {
		int version = 0;
		try {
			version = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		} catch(NameNotFoundException e) {
			// weird exception, shouldn't happen
		}
		
		try {
			AssetManager assets = getAssets();
			String dataDir = getApplicationInfo().dataDir;
			File cacheVersion = new File(dataDir, ".cacheversion");
			if (cacheVersion != null && cacheVersion.isFile() && cacheVersion.canRead() && cacheVersion.canWrite())
			{
				DataInputStream cacheStream = new DataInputStream(new FileInputStream(cacheVersion));

				int currentCacheVersion = 0;
				try {
					currentCacheVersion = cacheStream.readInt();
				} catch (IOException e) {}
			    cacheStream.close();
			    
				if (currentCacheVersion == version)
				{
					Log.i("ASSETS", "Assets already extracted, skipping...");
					return;
				}
			}
			
			//extractAssets(assets, cacheDir, "", 0);
			Log.i("ASSETS", "Extracting shader assets now ...");
			try {
				extractAssets(assets, dataDir, "shaders_glsl", 1);
			} catch (IOException e) {
				Log.i("ASSETS", "Failed to extract shaders ...");
			}
			
			Log.i("ASSETS", "Extracting overlay assets now ...");
			try {
				extractAssets(assets, dataDir, "overlays", 1);
			} catch (IOException e) {
				Log.i("ASSETS", "Failed to extract overlays ...");
			}
			
			DataOutputStream outputCacheVersion = new DataOutputStream(new FileOutputStream(cacheVersion, false));
			outputCacheVersion.writeInt(version);
			outputCacheVersion.close();
		} catch (IOException e) {
			Log.e(TAG, "Failed to extract assets to cache.");			
		}
	}
	
	boolean detectDevice(boolean show_dialog)
	{
		boolean retval = false;
		
		Log.i("Device MODEL", android.os.Build.MODEL);
		if (android.os.Build.MODEL.equals("SHIELD"))
		{
			AlertDialog.Builder alert = new AlertDialog.Builder(this)
			.setTitle("NVidia Shield detected")
			.setMessage("The ideal configuration options for your device will now be preconfigured.\nNOTE: For optimal performance, turn off Google Account sync, Google Play Store auto-updates, GPS and Wifi in your Android settings menu.")
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
					SharedPreferences.Editor edit = prefs.edit();
					edit.putString("video_refresh_rate", Double.valueOf(60.00d).toString());
					edit.putBoolean("input_overlay_enable", false);
					edit.putBoolean("input_autodetect_enable", true);
					edit.commit();
				}
			});
			alert.show();
			retval = true;
		}
		else if (android.os.Build.MODEL.equals( "OUYA Console"))
		{
			AlertDialog.Builder alert = new AlertDialog.Builder(this)
			.setTitle("OUYA detected")
			.setMessage("The ideal configuration options for your device will now be preconfigured.\nNOTE: For optimal performance, turn off Google Account sync, GPS and Wifi in your Android settings menu.")
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
					SharedPreferences.Editor edit = prefs.edit();
					edit.putBoolean("input_overlay_enable", false);
					edit.putBoolean("input_autodetect_enable", true);
					edit.commit();
				}
			});
			alert.show();
			retval = true;
		}
		else if (android.os.Build.ID.equals("JSS15J"))
		{
			AlertDialog.Builder alert = new AlertDialog.Builder(this)
			.setTitle("Nexus 7 2013 detected")
			.setMessage("The ideal configuration options for your device will now be preconfigured.\nNOTE: For optimal performance, turn off Google Account sync, Google Play Store auto-updates, GPS and Wifi in your Android settings menu.")
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
					SharedPreferences.Editor edit = prefs.edit();
					edit.putString("video_refresh_rate", Double.valueOf(59.65).toString());
					edit.commit();
				}
			});
			alert.show();
			retval = true;
		}
		
		if (show_dialog) {
		Toast.makeText(this,
				"Device either not detected in list or doesn't have any optimal settings in our database.",
				Toast.LENGTH_SHORT).show();
		}
		
		return retval;
	}
	
	@Override
	protected void onStart() {
		super.onStart();
	}
}
