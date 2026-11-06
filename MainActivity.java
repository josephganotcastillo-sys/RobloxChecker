package com.example.robloxchecker;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(9, 13, 26);
    private final int PANEL = Color.rgb(19, 27, 45);
    private final int YELLOW = Color.rgb(255, 211, 31);
    private final int MUTED = Color.rgb(165, 176, 198);
    private final int GREEN = Color.rgb(49, 211, 119);
    private final int RED = Color.rgb(255, 82, 102);

    private LinearLayout root, results;
    private TextView fileLabel, progressLabel, validLabel, invalidLabel, errorLabel, statusLabel;
    private ProgressBar progressBar;
    private Button startButton;
    private ArrayList<String> usernames = new ArrayList<>();
    private ExecutorService executor = Executors.newFixedThreadPool(3);
    private volatile boolean running = false;
    private int valid = 0, invalid = 0, errors = 0, finished = 0;
    private static final int PICK_FILE = 41;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildUi();
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private android.graphics.drawable.GradientDrawable bg(int color, int stroke) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(16));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l;
    }

    private void addPanel(LinearLayout parent, View child) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(7), 0, dp(7));
        parent.addView(child, p);
    }

    private TextView button(String label, boolean filled) {
        TextView b = text(label, 16, filled ? BG : YELLOW, true);
        b.setGravity(Gravity.CENTER); b.setPadding(dp(12), dp(15), dp(12), dp(15));
        b.setBackground(bg(filled ? YELLOW : PANEL, YELLOW));
        return b;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        root = vertical(); root.setPadding(dp(18), dp(22), dp(18), dp(26));
        scroll.addView(root); setContentView(scroll);

        TextView title = text("ROBLOX CHECKER", 27, YELLOW, true);
        title.setGravity(Gravity.CENTER); root.addView(title);
        TextView sub = text("PUBLIC USERNAME & PROFILE LOOKUP", 12, MUTED, true);
        sub.setGravity(Gravity.CENTER); root.addView(sub);
        View line = new View(this); line.setBackgroundColor(YELLOW);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(1));
        lp.setMargins(0, dp(20), 0, dp(16)); root.addView(line, lp);

        LinearLayout filePanel = vertical(); filePanel.setPadding(dp(15), dp(15), dp(15), dp(15));
        filePanel.setBackground(bg(PANEL, 0xff303b54));
        filePanel.addView(text("USERNAME FILE", 15, YELLOW, true));
        TextView pick = button("SELECT .TXT FILE", false);
        LinearLayout.LayoutParams pickLp = new LinearLayout.LayoutParams(-1, -2);
        pickLp.setMargins(0, dp(10), 0, dp(7)); filePanel.addView(pick, pickLp);
        fileLabel = text("No file selected (one username per line)", 12, MUTED, false);
        filePanel.addView(fileLabel);
        pick.setOnClickListener(v -> chooseFile());
        addPanel(root, filePanel);

        LinearLayout startPanel = vertical(); startPanel.setPadding(dp(15), dp(14), dp(15), dp(14));
        startPanel.setBackground(bg(PANEL, 0xff303b54));
        statusLabel = text("Ready. Choose a usernames.txt file.", 14, MUTED, false);
        startPanel.addView(statusLabel);
        addPanel(root, startPanel);

        startButton = (Button) null;
        TextView start = button("START CHECKING", true);
        addPanel(root, startPanel = vertical());
        startPanel.setBackgroundColor(BG);
        startPanel.addView(start);
        start.setOnClickListener(v -> startChecking());

        LinearLayout prog = vertical(); prog.setPadding(dp(15), dp(14), dp(15), dp(14));
        prog.setBackground(bg(PANEL, 0xff303b54));
        progressLabel = text("Progress: 0 / 0 (0%)", 14, MUTED, true); prog.addView(progressLabel);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100); progressBar.setProgress(0);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(-1, dp(8));
        barLp.setMargins(0, dp(12), 0, 0); prog.addView(progressBar, barLp);
        addPanel(root, prog);

        LinearLayout stats = new LinearLayout(this); stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setPadding(dp(5), dp(14), dp(5), dp(14)); stats.setBackground(bg(PANEL, 0xff303b54));
        validLabel = stat(stats, "0", "VALID", GREEN);
        invalidLabel = stat(stats, "0", "NOT FOUND", RED);
        errorLabel = stat(stats, "0", "ERROR", 0xffffa33b);
        addPanel(root, stats);

        TextView resultTitle = text("PROFILE RESULTS", 16, YELLOW, true);
        LinearLayout.LayoutParams rt = new LinearLayout.LayoutParams(-1, -2);
        rt.setMargins(0, dp(18), 0, dp(8)); root.addView(resultTitle, rt);
        results = vertical(); root.addView(results);
        addResultMessage("Your Roblox public profile results will appear here.");
    }

    private TextView stat(LinearLayout parent, String number, String label, int color) {
        LinearLayout box = vertical(); box.setGravity(Gravity.CENTER);
        TextView n = text(number, 25, color, true); n.setGravity(Gravity.CENTER);
        TextView l = text(label, 10, MUTED, true); l.setGravity(Gravity.CENTER);
        box.addView(n); box.addView(l);
        parent.addView(box, new LinearLayout.LayoutParams(0, -2, 1));
        return n;
    }

    private void chooseFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("text/plain");
        startActivityForResult(i, PICK_FILE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri), StandardCharsets.UTF_8))) {
                usernames.clear(); String line;
                while ((line = br.readLine()) != null && usernames.size() < 500) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) usernames.add(line);
                }
                fileLabel.setText(usernames.size() + " usernames loaded (maximum 500)");
                statusLabel.setText("File ready. Tap START CHECKING.");
            } catch (Exception e) {
                statusLabel.setText("Could not read file: " + e.getMessage());
            }
        }
    }

    private void startChecking() {
        if (running) return;
        if (usernames.isEmpty()) { statusLabel.setText("Please select a .txt file first."); return; }
        running = true; valid = invalid = errors = finished = 0;
        validLabel.setText("0"); invalidLabel.setText("0"); errorLabel.setText("0");
        results.removeAllViews(); progressBar.setProgress(0);
        progressLabel.setText("Progress: 0 / " + usernames.size() + " (0%)");
        statusLabel.setText("Checking public Roblox profiles...");
        ArrayList<String> batch = new ArrayList<>(usernames);
        executor.execute(() -> resolveBatch(batch));
    }

    private void resolveBatch(ArrayList<String> batch) {
        try {
            JSONObject body = new JSONObject(); JSONArray arr = new JSONArray();
            for (String name : batch) arr.put(name);
            body.put("usernames", arr); body.put("excludeBannedUsers", false);
            JSONObject response = requestJson("https://users.roblox.com/v1/usernames/users", "POST", body);
            JSONArray data = response.optJSONArray("data");
            HashMap<String, JSONObject> found = new HashMap<>();
            if (data != null) for (int i=0; i<data.length(); i++) {
                JSONObject u = data.getJSONObject(i);
                found.put(u.optString("requestedUsername").toLowerCase(Locale.ROOT), u);
            }
            for (String name : batch) {
                JSONObject u = found.get(name.toLowerCase(Locale.ROOT));
                if (u == null) {
                    markFinished(false, false);
                    addResultMessage("[NOT FOUND] " + name + " — no public username match", RED);
                } else {
                    executor.execute(() -> loadProfile(name, u));
                }
            }
        } catch (Exception e) {
            for (String name : batch) {
                markFinished(false, true);
                addResultMessage("[ERROR] " + name + " — " + safe(e.getMessage()), 0xffffa33b);
            }
        }
    }

    private void loadProfile(String requested, JSONObject match) {
        try {
            long id = match.getLong("id");
            JSONObject user = requestJson("https://users.roblox.com/v1/users/" + id, "GET", null);
            int friends = getCount("https://friends.roblox.com/v1/users/" + id + "/friends/count");
            int followers = getCount("https://friends.roblox.com/v1/users/" + id + "/followers/count");
            int following = getCount("https://friends.roblox.com/v1/users/" + id + "/followings/count");
            String avatar = "https://thumbnails.roblox.com/v1/users/avatar-headshot?userIds=" + id + "&size=150x150&format=Png&isCircular=false";
            JSONObject thumb = requestJson(avatar, "GET", null);
            String imageUrl = "";
            JSONArray td = thumb.optJSONArray("data");
            if (td != null && td.length() > 0) imageUrl = td.getJSONObject(0).optString("imageUrl", "");
            final String image = imageUrl;
            final String profileName = user.optString("name", requested);
            final String display = user.optString("displayName", profileName);
            final String description = user.optString("description", "");
            final String created = user.optString("created", "Unknown");
            runOnUiThread(() -> addProfileCard(requested, profileName, display, id, created, description, friends, followers, following, image));
            markFinished(true, false);
        } catch (Exception e) {
            markFinished(false, true);
            addResultMessage("[ERROR] " + requested + " — " + safe(e.getMessage()), 0xffffa33b);
        }
    }

    private int getCount(String url) {
        try { return requestJson(url, "GET", null).optInt("count", 0); }
        catch (Exception e) { return -1; }
    }

    private JSONObject requestJson(String address, String method, JSONObject payload) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setRequestMethod(method); c.setConnectTimeout(12000); c.setReadTimeout(12000);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("User-Agent", "RobloxCheckerAndroid/1.0");
        if (payload != null) {
            c.setDoOutput(true); c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            try (OutputStream os = c.getOutputStream()) { os.write(payload.toString().getBytes(StandardCharsets.UTF_8)); }
        }
        int code = c.getResponseCode();
        InputStream in = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        StringBuilder out = new StringBuilder();
        if (in != null) try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line; while ((line = r.readLine()) != null) out.append(line);
        }
        c.disconnect();
        if (code < 200 || code >= 300) throw new IOException("Roblox API HTTP " + code);
        return new JSONObject(out.toString());
    }

    private void markFinished(boolean isValid, boolean isError) {
        synchronized (this) {
            finished++;
            if (isValid) valid++; else if (isError) errors++; else invalid++;
            int v=valid, iv=invalid, er=errors, f=finished, total=usernames.size();
            runOnUiThread(() -> {
                validLabel.setText(String.valueOf(v)); invalidLabel.setText(String.valueOf(iv)); errorLabel.setText(String.valueOf(er));
                int pct = total == 0 ? 0 : (int)(100f*f/total);
                progressBar.setProgress(pct);
                progressLabel.setText("Progress: " + f + " / " + total + " (" + pct + "%)");
                if (f >= total) { running = false; statusLabel.setText("Finished checking public profiles."); }
            });
        }
    }

    private void addProfileCard(String requested, String name, String display, long id, String created,
                                String description, int friends, int followers, int following, String imageUrl) {
        LinearLayout card = vertical(); card.setPadding(dp(13), dp(13), dp(13), dp(13));
        card.setBackground(bg(PANEL, 0xff303b54));
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        ImageView avatar = new ImageView(this); avatar.setLayoutParams(new LinearLayout.LayoutParams(dp(76), dp(76)));
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP); avatar.setBackground(bg(0xff263149, 0));
        top.addView(avatar);
        LinearLayout names = vertical(); names.setPadding(dp(12), 0, 0, 0);
        names.addView(text(display, 19, YELLOW, true));
        names.addView(text("@" + name, 13, MUTED, false));
        names.addView(text("VALID PUBLIC PROFILE", 11, GREEN, true));
        top.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(top);
        if (!imageUrl.isEmpty()) executor.execute(() -> {
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(new URL(imageUrl).openStream());
                if (bitmap != null) runOnUiThread(() -> avatar.setImageBitmap(bitmap));
            } catch (Exception ignored) {}
        });
        addDetail(card, "ROBLOX USER ID", String.valueOf(id));
        addDetail(card, "ACCOUNT CREATED", created);
        addDetail(card, "FRIENDS", countText(friends));
        addDetail(card, "FOLLOWERS", countText(followers));
        addDetail(card, "FOLLOWING", countText(following));
        addDetail(card, "ABOUT", description.isEmpty() ? "No public description" : description);
        TextView link = text("OPEN ROBLOX PROFILE  ↗", 13, YELLOW, true);
        link.setPadding(0, dp(10), 0, 0);
        link.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.roblox.com/users/" + id + "/profile"));
            startActivity(i);
        });
        card.addView(link);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(6), 0, dp(8)); results.addView(card, p);
    }

    private void addDetail(LinearLayout card, String key, String value) {
        TextView t = text(key + ": " + value, 12, MUTED, false);
        t.setPadding(0, dp(6), 0, 0); card.addView(t);
    }
    private String countText(int n) { return n < 0 ? "Unavailable" : String.valueOf(n); }
    private String safe(String s) { return s == null ? "Request failed" : s; }

    private void addResultMessage(String message) { addResultMessage(message, MUTED); }
    private void addResultMessage(String message, int color) {
        runOnUiThread(() -> {
            TextView t = text(message, 12, color, false);
            t.setPadding(dp(10), dp(8), dp(10), dp(8));
            t.setBackground(bg(PANEL, 0));
            results.addView(t, new LinearLayout.LayoutParams(-1, -2));
        });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
