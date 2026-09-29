package co.median.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Resolves saved names inside the app's existing authenticated web session. */
final class TruckerFindSavedVoice {
    static final String EXTRA_COMMAND = "co.median.android.truckerfind.VOICE_COMMAND";
    private static final String HOME = "https://truckerfindapp.github.io/Trucker-Find--web/";
    private final MainActivity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int generation;

    TruckerFindSavedVoice(MainActivity activity) { this.activity = activity; }

    void accept(Intent intent) {
        if (intent == null || !intent.hasExtra(EXTRA_COMMAND)) return;
        String command = intent.getStringExtra(EXTRA_COMMAND);
        intent.removeExtra(EXTRA_COMMAND);
        if (command == null || command.length() > 1000) return;
        final String script;
        try (InputStream input = activity.getAssets().open("trucker-find-saved-voice.js")) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
            script = bytes.toString(StandardCharsets.UTF_8.name());
        } catch (Exception error) {
            message("Could not load Saved Areas voice controls."); return;
        }
        if (activity.getWebView() != null && !trusted(activity.getWebView().getUrl())) {
            activity.loadUrl(HOME);
        }
        deliver(script, command, ++generation, 0);
    }

    private boolean trusted(String url) {
        if (url == null) return false;
        Uri uri = Uri.parse(url);
        return "https".equals(uri.getScheme()) && "truckerfindapp.github.io".equals(uri.getHost())
            && uri.getPort() == -1 && uri.getPath() != null
            && uri.getPath().startsWith("/Trucker-Find--web/");
    }

    private void deliver(String script, String command, int request, int attempt) {
        if (request != generation || activity.isFinishing() || activity.isDestroyed()) return;
        if (attempt >= 60) { message("Open Trucker Find and wait for it to load, then try voice search again."); return; }
        Runnable retry = () -> deliver(script, command, request, attempt + 1);
        if (activity.getWebView() == null || !trusted(activity.getWebView().getUrl())) {
            handler.postDelayed(retry, 250); return;
        }
        activity.runJavascript("(" + script + ")(" + JSONObject.quote(command) + ")", result -> {
            if (!"\"accepted\"".equals(result) && request == generation) handler.postDelayed(retry, 250);
        });
    }

    private void message(String text) { Toast.makeText(activity, text, Toast.LENGTH_LONG).show(); }
}
