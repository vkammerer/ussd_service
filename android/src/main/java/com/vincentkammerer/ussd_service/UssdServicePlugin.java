package com.vincentkammerer.ussd_service;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.telephony.TelephonyManager;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import java.util.concurrent.CompletableFuture;

public class UssdServicePlugin implements FlutterPlugin, MethodCallHandler {

  private static String CHANNEL_NAME = "com.vincentkammerer.ussd_service/plugin_channel";
  private static String MAKE_REQUEST_METHOD = "makeRequest";
  private Context context;
  private MethodChannel channel;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    initialize(binding.getApplicationContext(), binding.getBinaryMessenger());
  }

  public static void registerWith(Registrar registrar) {
    UssdServicePlugin instance = new UssdServicePlugin();
    instance.initialize(registrar.context(), registrar.messenger());
  }

  private void initialize(Context context, BinaryMessenger messenger) {
    this.context = context;
    this.channel = new MethodChannel(messenger, CHANNEL_NAME);
    this.channel.setMethodCallHandler(this);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    this.channel.setMethodCallHandler(null);
    this.channel = null;
    this.context = null;
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull final Result result) {
    if (call.method.equals(MAKE_REQUEST_METHOD)) {
      try {
        final UssdRequestParams mParams = new UssdRequestParams(call);
        makeRequest(mParams).thenAccept(result::success).exceptionally((Throwable e) -> {
          if (e instanceof RequestExecutionException) {
            result.error(RequestExecutionException.type, ((RequestExecutionException) e).message,
                null);
          } else {
            result.error(RequestExecutionException.type, e.getMessage(), null);
          }
          return null;
        });
      } catch (RequestParamsException e) {
        result.error(RequestParamsException.type, e.message, null);
      } catch (RequestExecutionException e) {
        result.error(RequestParamsException.type, e.message, null);
      } catch (Exception e) {
        result.error("unknown_exception", e.getMessage(), null);
      }
    } else {
      result.notImplemented();
    }
  }

  private static class RequestParamsException extends Exception {

    static String type = "ussd_plugin_incorrect__parameters";
    String message;

    RequestParamsException(String message) {
      this.message = message;
    }
  }

  private static class UssdRequestParams {

    int subscriptionId;
    String code;

    UssdRequestParams(@NonNull MethodCall call) throws RequestParamsException {
      Integer subscriptionIdInteger = call.argument("subscriptionId");
      if (subscriptionIdInteger == null) {
        throw new RequestParamsException(
            "Incorrect parameter type: `subscriptionId` must be an int");
      }
      subscriptionId = subscriptionIdInteger;
      if (subscriptionId < 0) {
        throw new RequestParamsException(
            "Incorrect parameter value: `subscriptionId` must be >= 0");
      }
      code = call.argument("code");
      if (code == null) {
        throw new RequestParamsException("Incorrect parameter type: `code` must be a String");
      }
      if (code.length() == 0) {
        throw new RequestParamsException(
            "Incorrect parameter value: `code` must not be an empty string");
      }
    }
  }

  private static class RequestExecutionException extends Exception {

    static String type = "ussd_plugin_ussd_execution_failure";
    String message;

    RequestExecutionException(String message) {
      this.message = message;
    }
  }


  private CompletableFuture<String> makeRequest(final UssdRequestParams ussdRequestParams)
      throws RequestExecutionException {
    if (ContextCompat.checkSelfPermission(this.context, permission.CALL_PHONE)
        != PackageManager.PERMISSION_GRANTED) {
      throw new RequestExecutionException("CALL_PHONE permission missing");
    }
    final CompletableFuture<String> c = new CompletableFuture<>();
    TelephonyManager.UssdResponseCallback callback =
        new TelephonyManager.UssdResponseCallback() {
          @Override
          public void onReceiveUssdResponse(
              TelephonyManager telephonyManager, String request, CharSequence response) {
            c.complete(response.toString());
          }

          @Override
          public void onReceiveUssdResponseFailed(
              TelephonyManager telephonyManager, String request, int failureCode) {
            if (failureCode == TelephonyManager.USSD_ERROR_SERVICE_UNAVAIL) {
              c.completeExceptionally(new RequestExecutionException("USSD_ERROR_SERVICE_UNAVAIL"));
            } else if (failureCode == TelephonyManager.USSD_RETURN_FAILURE) {
              c.completeExceptionally(new RequestExecutionException("USSD_RETURN_FAILURE"));
            } else {
              c.completeExceptionally(new RequestExecutionException("unknown error"));
            }
          }
        };

    TelephonyManager manager = (TelephonyManager) this.context
        .getSystemService(Context.TELEPHONY_SERVICE);
    TelephonyManager simManager = manager.createForSubscriptionId(ussdRequestParams.subscriptionId);
    simManager.sendUssdRequest(ussdRequestParams.code, callback, new Handler());
    return c;
  }
}
