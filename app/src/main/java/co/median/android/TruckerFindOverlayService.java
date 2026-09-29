package co.median.android;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Floating Trucker Find quick-access controls shown over navigation apps. */
public class TruckerFindOverlayService extends Service {
    private static final String CHANNEL_ID = "trucker_find_quick_access";
    private WindowManager wm;
    private LinearLayout root, menu;
    private WindowManager.LayoutParams params;
    private float downX, downY;
    private int startX, startY;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        Intent openApp = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent pi = PendingIntent.getActivity(this, 0, openApp, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification).setContentTitle("Trucker Find quick access")
                .setContentText("Quick-stop buttons are available over navigation.")
                .setOngoing(true).setContentIntent(pi).build();
        startForeground(9041, n);
        if (Settings.canDrawOverlays(this)) showOverlay();
        else stopSelf();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL_ID, "Trucker Find Quick Access", NotificationManager.IMPORTANCE_LOW);
            c.setDescription("Keeps Trucker Find quick-stop controls available while navigating.");
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    private TextView textButton(String text, int size) {
        TextView v = new TextView(this); v.setText(text); v.setTextSize(size); v.setTextColor(Color.WHITE);
        v.setGravity(Gravity.CENTER); v.setPadding(dp(10), dp(9), dp(10), dp(9));
        v.setBackgroundResource(android.R.drawable.dialog_holo_dark_frame); return v;
    }

    private void showOverlay() {
        if (root != null) return;
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.HORIZONTAL); root.setGravity(Gravity.TOP);
        menu = new LinearLayout(this); menu.setOrientation(LinearLayout.VERTICAL); menu.setVisibility(View.GONE);
        menu.setPadding(dp(3), dp(3), dp(3), dp(3)); menu.setBackgroundColor(0xE610273A);

        addSearch("⛽", "Fuel"); addSearch("🍔", "Food"); addSearch("🅿", "Truck parking");
        addSearch("🔧", "Truck repair"); addSearch("🧺", "Laundry"); addSearch("🚿", "Truck wash");
        addSearch("🏨", "Hotels"); addSearch("24/7", "24 hour truck stop");

        ImageButton chrome = new ImageButton(this); chrome.setImageResource(R.drawable.ic_chrome_wheel);
        chrome.setBackgroundColor(0xFF10273A); chrome.setPadding(dp(5),dp(5),dp(5),dp(5));
        chrome.setScaleType(ImageView.ScaleType.FIT_CENTER);
        chrome.setContentDescription("Chrome shop"); chrome.setOnClickListener(v -> mapsSearch("Truck chrome shop near me"));
        menu.addView(chrome, new LinearLayout.LayoutParams(dp(48), dp(48)));

        ImageButton scale = new ImageButton(this); scale.setImageResource(R.drawable.ic_truck_scale);
        scale.setBackgroundColor(0xFF10273A); scale.setPadding(dp(7),dp(7),dp(7),dp(7));
        scale.setContentDescription("Weigh Stations"); scale.setOnClickListener(v -> mapsSearch("Truck weigh station near me"));
        menu.addView(scale, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView close = textButton("✕", 18); close.setContentDescription("Turn off quick access");
        close.setOnClickListener(v -> stopSelf()); menu.addView(close, new LinearLayout.LayoutParams(dp(48), dp(42)));

        ImageButton bubble = new ImageButton(this);
        bubble.setImageResource(R.mipmap.ic_sidebar_logo);
        bubble.setBackgroundColor(Color.TRANSPARENT);
        bubble.setPadding(0, 0, 0, 0);
        bubble.setScaleType(ImageView.ScaleType.FIT_CENTER);
        bubble.setContentDescription("Trucker Find quick access");
        bubble.setOnTouchListener((v,e)->drag(e));
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.addView(bubble, new LinearLayout.LayoutParams(dp(54),dp(54)));
        ImageButton voice = new ImageButton(this);
        voice.setImageResource(android.R.drawable.ic_btn_speak_now);
        voice.setContentDescription("Voice search");
        voice.setBackgroundColor(0xFF10273A);
        voice.setOnClickListener(v -> {
            menu.setVisibility(View.GONE);
            Intent intent = new Intent(this, TruckerFindVoiceActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
        controls.addView(voice, new LinearLayout.LayoutParams(dp(54), dp(48)));
        root.addView(menu); root.addView(controls);

        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END; params.x = dp(8); params.y = dp(180);
        wm.addView(root, params);
    }

    private void addSearch(String icon, String query) {
        TextView b = textButton(icon, icon.length()>2 ? 11 : 21); b.setContentDescription(query);
        b.setOnClickListener(v -> mapsSearch(query + " near me")); menu.addView(b, new LinearLayout.LayoutParams(dp(48),dp(48)));
    }

    private void mapsSearch(String q) {
        try {
            String url="https://www.google.com/maps/search/?api=1&query="+ URLEncoder.encode(q, StandardCharsets.UTF_8.name());
            Intent i=new Intent(Intent.ACTION_VIEW, Uri.parse(url)); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i);
            menu.setVisibility(View.GONE);
        } catch(Exception ignored) { }
    }

    private boolean drag(MotionEvent e) {
        switch(e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX=e.getRawX(); downY=e.getRawY(); startX=params.x; startY=params.y; return true;
            case MotionEvent.ACTION_MOVE:
                params.x=startX-(int)(e.getRawX()-downX); params.y=startY+(int)(e.getRawY()-downY);
                wm.updateViewLayout(root,params); return true;
            case MotionEvent.ACTION_UP:
                if (Math.abs(e.getRawX()-downX) < dp(8) && Math.abs(e.getRawY()-downY) < dp(8))
                    menu.setVisibility(menu.getVisibility()==View.VISIBLE ? View.GONE : View.VISIBLE);
                return true;
        } return true;
    }
    private int dp(int n){ return (int)(n*getResources().getDisplayMetrics().density); }
    @Override public int onStartCommand(Intent i,int flags,int id){ return START_STICKY; }
    @Override public void onDestroy(){ if(root!=null && wm!=null){ try{wm.removeView(root);}catch(Exception ignored){} root=null;} super.onDestroy(); }
    @Override public android.os.IBinder onBind(Intent intent){ return null; }
}
