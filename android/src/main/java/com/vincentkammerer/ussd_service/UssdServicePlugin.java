package com.vincentkammerer.ussd_service;

import androidx.annotation.NonNull;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.content.Context;

class UssdRequestParams {
  public String errorType;
  public String errorMessage;
  public int subscriptionId;
  public String code;


  public UssdRequestParams(MethodCall call) {
    if (!(call.argument("subscriptionId") instanceof Integer && call.argument("code") instanceof String)) {
      this.errorType = "ussd_plugin_incorrect_parameter_types";
      this.errorMessage = "Incorrect parameters: `subscriptionId` must be an int and `code` must be a String";
      return;
    }
    int callSubscriptionId = call.argument("subscriptionId");
    String callCode = call.argument("code");
    if (!(callSubscriptionId > 0 && callCode.length() > 0)) {
      this.errorType = "ussd_plugin_incorrect_parameter_values";
      this.errorMessage = "Incorrect parameters: `subscriptionId` must be > 0 and `code` must not be an empty string";
      return;
    }
    this.subscriptionId = callSubscriptionId;
    this.code = callCode;
  }
}

/** UssdServicePlugin */
public class UssdServicePlugin implements FlutterPlugin, MethodCallHandler {
  private Context iContext;

  public void setContext(Context applicationContextP) {
    this.iContext = applicationContextP;
  }

  @SuppressLint("MissingPermission") // Check should be done on the flutter side
  private void makeRequest(final UssdRequestParams ussdRequestParams, final Result result) {
    Context context = this.iContext;
    TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    TelephonyManager simManager = manager.createForSubscriptionId(ussdRequestParams.subscriptionId);
    TelephonyManager.UssdResponseCallback callback = new TelephonyManager.UssdResponseCallback() {
      @Override
      public void onReceiveUssdResponse(TelephonyManager telephonyManager, String request, CharSequence response) {
        result.success(response.toString());
      }
      @Override
      public void onReceiveUssdResponseFailed(TelephonyManager telephonyManager, String request, int failureCode) {
        if (failureCode == TelephonyManager.USSD_ERROR_SERVICE_UNAVAIL) {
          result.error("ussd_plugin_ussd_response_failed", "USSD_ERROR_SERVICE_UNAVAIL", null);
        } else if (failureCode == TelephonyManager.USSD_RETURN_FAILURE) {
          result.error("ussd_plugin_ussd_response_failed", "USSD_RETURN_FAILURE", null);
        } else {
          result.error("ussd_plugin_ussd_response_failed", "UNKNOWN ERROR", null);
        }
      }
    };

    simManager.sendUssdRequest(ussdRequestParams.code, callback, new Handler());
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    final MethodChannel channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "vincentkammerer.com/ussd_service");
    UssdServicePlugin ussdServicePlugin = new UssdServicePlugin();
    ussdServicePlugin.setContext(flutterPluginBinding.getApplicationContext());
    channel.setMethodCallHandler(ussdServicePlugin);
  }

  // This static function is optional and equivalent to onAttachedToEngine. It supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //
  // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both be defined
  // in the same class.
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "vincentkammerer.com/ussd_service");
    UssdServicePlugin ussdServicePlugin = new UssdServicePlugin();
    ussdServicePlugin.setContext(registrar.context());
    channel.setMethodCallHandler(ussdServicePlugin);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("makeRequest")) {
      final UssdRequestParams myUssdRequestParams = new UssdRequestParams(call);
      if (myUssdRequestParams.errorType != null) {
        result.error(myUssdRequestParams.errorType, myUssdRequestParams.errorMessage, null);
      } else {
        makeRequest(myUssdRequestParams, result);
      }
    } else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
  }
}
