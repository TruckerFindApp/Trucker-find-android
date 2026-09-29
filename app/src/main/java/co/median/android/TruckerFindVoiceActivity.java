package co.median.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import java.util.ArrayList;

/** A user-initiated speech session. The installed recognizer owns microphone capture. */
public class TruckerFindVoiceActivity extends Activity {
    private static final int RECOGNIZE = 4102;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state == null) listen();
    }

    private void listen() {
        Intent speech = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speech.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speech.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say Find fuel or Go to saved area Home");
        speech.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        try {
            startActivityForResult(speech, RECOGNIZE);
        } catch (ActivityNotFoundException | SecurityException error) {
            new AlertDialog.Builder(this).setTitle("Voice search unavailable")
                .setMessage("Enable a speech recognition app on this phone, then try again.")
                .setPositiveButton("Close", (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish()).show();
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != RECOGNIZE) return;
        if (result != RESULT_OK || data == null) { finish(); return; }
        ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        String heard = results == null || results.isEmpty() ? "" : results.get(0);
        if (TruckerFindVoiceCommands.normalize(heard).matches("cancel|stop|never mind|nevermind")) {
            finish(); return;
        }
        String query = TruckerFindVoiceCommands.search(heard);
        if (query == null) {
            if (heard.trim().isEmpty()) { listen(); return; }
            Intent app = new Intent(this, MainActivity.class);
            app.putExtra(TruckerFindSavedVoice.EXTRA_COMMAND, heard);
            app.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(app);
            finish();
            return;
        }
        Uri maps = Uri.parse("https://www.google.com/maps/search/").buildUpon()
            .appendQueryParameter("api", "1").appendQueryParameter("query", query).build();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, maps));
            finish();
        } catch (ActivityNotFoundException error) {
            new AlertDialog.Builder(this).setTitle("Cannot open Maps")
                .setMessage("Install or enable Google Maps or a web browser to open this search.")
                .setPositiveButton("Close", (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish()).show();
        }
    }
}
