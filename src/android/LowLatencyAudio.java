package com.rjfun.cordova.plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

public class LowLatencyAudio extends CordovaPlugin {

  private static final String ERROR_NO_AUDIOID="A reference does not exist for the specified audio id.";

  private static final String PRELOAD_FX="preloadFX";
  private static final String PLAY="play";
  private static final String UNLOAD="unload";

  private static final String LOGTAG = "LowLatencyAudio";

  private static SoundPool soundPool;
  private static HashMap<String, Integer> soundMap;

  @Override
  public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

    if (PRELOAD_FX.equals(action)) {
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          executePreloadFX(data, callbackContext);
        }
      });

    } else if (PLAY.equals(action)) {
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          executePlay(data, callbackContext);
        }
      });

    } else if (UNLOAD.equals(action)) {
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          executeUnload(data, callbackContext);
        }
      });

    } else {
      return false;
    }

    return true;
  }

  private void executePreloadFX(JSONArray data, CallbackContext callbackContext) {

    initSoundPool();

    String audioID;
    try {
      audioID = data.getString(0);
      if (!soundMap.containsKey(audioID)) {
        String assetPath = data.getString(1);
        String fullPath = "public/".concat(assetPath);

        Context ctx = cordova.getActivity().getApplicationContext();
        AssetManager am = ctx.getResources().getAssets();
        AssetFileDescriptor afd = am.openFd(fullPath);

        Log.d(LOGTAG, "preloadFX - " + audioID + ": " + assetPath + ", length: " + afd.getLength());

        int assetIntID = soundPool.load(afd, 1);
        soundMap.put(audioID, assetIntID);
      }
    } catch (JSONException e) {
      callbackContext.error(e.toString());
    } catch (IOException e) {
      callbackContext.error(e.toString());
    }

    callbackContext.success();
  }

  private void executePlay(JSONArray data, CallbackContext callbackContext) {
    String audioID;
    try {
      audioID = data.getString(0);
      //Log.d( LOGTAG, "play - " + audioID );

      if (soundMap.containsKey(audioID)) {
        soundPool.play(soundMap.get(audioID), 1, 1, 1, 0, 1);
      } else {
        callbackContext.error(ERROR_NO_AUDIOID);
      }
    } catch (JSONException e) {
      callbackContext.error(e.toString());
    }

    callbackContext.success();
  }

  private void executeUnload(JSONArray data, CallbackContext callbackContext) {
    String audioID;
    try {
      audioID = data.getString(0);
      Log.d( LOGTAG, "unload - " + audioID );

      if (soundMap.containsKey(audioID)) {
        int assetIntID = soundMap.get(audioID);
        soundMap.remove(audioID);
        soundPool.unload(assetIntID);
      } else {
        callbackContext.error(ERROR_NO_AUDIOID);
      }
    } catch (JSONException e) {
      callbackContext.error(e.toString());
    }

    callbackContext.success();
  }

  private void initSoundPool() {
    if (soundPool == null) {
      soundPool = new SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        ).build();
    }

    if (soundMap == null) {
      soundMap = new HashMap<String, Integer>();
    }
  }
}
