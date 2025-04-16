package com.alaminkarno.flutter_crisp_chat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alaminkarno.flutter_crisp_chat.config.CrispConfig;

import java.util.HashMap;
import java.util.List;

import im.crisp.client.external.ChatActivity;
import im.crisp.client.external.Crisp;

import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;

/// [FlutterCrispChatPlugin] using [FlutterPlugin], [MethodCallHandler] and [ActivityAware]
/// to handling Method Channel Callback from Flutter and Open new Activity.

/**
 * FlutterCrispChatPlugin
 */
public class FlutterCrispChatPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, ActivityResultListener {

    private static final String CHANNEL_NAME = "flutter_crisp_chat";
    private static final int CRISP_CHAT_REQUEST_CODE = 1001;

    private MethodChannel channel;
    private Context context;
    private Activity activity;
    private Result pendingResult;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        context = flutterPluginBinding.getApplicationContext();

        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), CHANNEL_NAME);
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
        binding.addActivityResultListener(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        this.activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
        binding.addActivityResultListener(this);
    }

    @Override
    public void onDetachedFromActivity() {
        this.activity = null;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CRISP_CHAT_REQUEST_CODE) {
            Log.d("CRISP_CHAT", "Chat window closed with result code: " + resultCode);
            
            // If we have a pending result from openCrispChat, resolve it now
            if (pendingResult != null) {
                // Create a more informative result with the result code
                HashMap<String, Object> resultMap = new HashMap<>();
                resultMap.put("status", "closed");
                resultMap.put("resultCode", resultCode);
                
                // Attempt to interpret the result code
                String reason = "unknown";
                if (resultCode == Activity.RESULT_OK) {
                    reason = "normal_exit";
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    reason = "user_canceled";
                }
                resultMap.put("reason", reason);

                Log.d("CRISP_CHAT", "Chat window closed with reason: " + reason);
                
                pendingResult.success(resultMap);
                pendingResult = null;
            }
            
            return true;
        }
        return false;
    }

    /// [onMethodCall] if for handling method call from flutter end.
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        Log.d("CRISP_CHAT", "Using JAVA implementation of FlutterCrispChatPlugin");
        if (call.method.equals("openCrispChat")) {
            try {
                Log.d("CRISP_CHAT", "Starting openCrispChat");
                HashMap<String, Object> args = (HashMap<String, Object>) call.arguments;
                if (args != null) {
                    CrispConfig config = CrispConfig.fromJson(args);
                    if (config.tokenId != null) {
                        Crisp.configure(context, config.websiteId, config.tokenId);
                    } else {
                        Crisp.configure(context, config.websiteId);
                    }

                    Crisp.enableNotifications(context, config.enableNotifications);
                    setCrispData(context, config);
                    openActivity();
                    Log.d("CRISP_CHAT", "Successfully opened Crisp Chat");
                    
                    // Save the result to resolve when chat closes
                    this.pendingResult = result;
                } else {
                    result.notImplemented();
                }
            } catch (Exception e) {
                Log.e("CRISP_CHAT", "Error opening Crisp Chat: " + e.getMessage());
                result.error("CRISP_ERROR", "Failed to open Crisp Chat", e.getMessage());
            }
        } else if (call.method.equals("resetCrispChatSession")) {
            try {
                Crisp.resetChatSession(context);
                result.success(null);
            } catch (Exception e) {
                Log.e("CRISP_CHAT", "Error resetting Crisp Chat session: " + e.getMessage());
                result.error("RESET_ERROR", "Failed to reset session", e.getMessage());
            }
        } else if (call.method.equals("setSessionString")) {
            try {
                HashMap<String, Object> args = (HashMap<String, Object>) call.arguments;
                if (args != null) {
                    String key = (String) args.get("key");
                    String value = (String) args.get("value");
                    Crisp.setSessionString(key, value);
                    Log.d("CRISP_CHAT", "Set session string for key: " + key);
                    result.success(null);
                } else {
                    result.error("INVALID_ARGS", "Invalid arguments for setSessionString", null);
                }
            } catch (Exception e) {
                Log.e("CRISP_CHAT", "Error setting session string: " + e.getMessage());
                result.error("SESSION_STRING_ERROR", "Failed to set session string", e.getMessage());
            }
        } else if (call.method.equals("setSessionInt")) {
            try {
                HashMap<String, Object> args = (HashMap<String, Object>) call.arguments;
                if (args != null) {
                    String key = (String) args.get("key");
                    int value = (int) args.get("value");
                    Crisp.setSessionInt(key, value);
                    Log.d("CRISP_CHAT", "Set session int for key: " + key);
                    result.success(null);
                } else {
                    result.error("INVALID_ARGS", "Invalid arguments for setSessionInt", null);
                }
            } catch (Exception e) {
                Log.e("CRISP_CHAT", "Error setting session int: " + e.getMessage());
                result.error("SESSION_INT_ERROR", "Failed to set session int", e.getMessage());
            }
        } else if (call.method.equals("getSessionIdentifier")) {
            try {
                String sessionId = Crisp.getSessionIdentifier(context);
                if (sessionId != null) {
                    Log.d("CRISP_CHAT", "Got session identifier: " + sessionId);
                    result.success(sessionId);
                } else {
                    Log.d("CRISP_CHAT", "No active session found");
                    result.error("NO_SESSION", "No active session found", null);
                }
            } catch (Exception e) {
                Log.e("CRISP_CHAT", "Error getting session identifier: " + e.getMessage());
                result.error("SESSION_ID_ERROR", "Failed to get session identifier", e.getMessage());
            }
        } else if (call.method.equals("setSessionSegments")) {
            try {
                HashMap<String, Object> args = (HashMap<String, Object>) call.arguments;
                if (args != null) {
                    List<String> segments = (List<String>) args.get("segments");
                    boolean overwrite = (boolean) args.get("overwrite");
                    Crisp.setSessionSegments(segments, overwrite);
                    Log.d("CRISP_CHAT", "Set session segments: " + segments);
                    result.success(null);
                } else {
                    result.error("INVALID_ARGS", "Invalid arguments for setSessionSegments", null);
                }
            } catch (Exception e) {
                Log.e("CRISP_CHAT", "Error setting session segments: " + e.getMessage());
                result.error("SESSION_SEGMENTS_ERROR", "Failed to set session segments", e.getMessage());
            }
        } else {
            result.notImplemented();
        }
    }

    private void setCrispData(Context context, CrispConfig config) {
        if (config.tokenId != null) {
            Crisp.setTokenID(context, config.tokenId);
        }

        if (config.sessionSegment != null) {
            Crisp.setSessionSegment(config.sessionSegment);
        }
        if (config.user != null) {
            if (config.user.nickName != null) {
                Crisp.setUserNickname(config.user.nickName);
            }
            if (config.user.email != null) {
                boolean result =  Crisp.setUserEmail(config.user.email);
                if(!result){
                    Log.d("CRSIP_CHAT","Email not set");
                }
            }
            if (config.user.avatar != null) {
               boolean result = Crisp.setUserAvatar(config.user.avatar);
               if(!result){
                   Log.d("CRSIP_CHAT","Avatar not set");
               }
            }
            if (config.user.phone != null) {
                boolean result =  Crisp.setUserPhone(config.user.phone);
                if(!result){
                    Log.d("CRSIP_CHAT","Phone not set");
                }
            }
            if (config.user.company != null) {
                Crisp.setUserCompany(config.user.company.toCrispCompany());
            }
        }

        if (config.segments != null && !config.segments.isEmpty()) {
            Crisp.setSessionSegments(config.segments, true);
        }
    }

    ///[openActivity] is opening ChatView Activity of CrispChat SDK.
    private void openActivity() {
        Intent intent = new Intent(context, ChatActivity.class);
        if (activity != null) {
            activity.startActivityForResult(intent, CRISP_CHAT_REQUEST_CODE);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        if (channel != null) {
            channel.setMethodCallHandler(null);
            channel = null;
        }
        context = null;
        // Make sure to reset any other state
        activity = null;
        pendingResult = null;
    }

}