package com.rsking.ironmanjarvis;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private TextView status, heard, orb;
    private Button listenButton;
    private boolean listening = false, ttsReady = false;
    private String activeContactName = null, activeContactPhone = null;
    private static final int CONTACTS_PERMISSION = 78;
    private final int cyan = Color.rgb(0, 210, 255), bg = Color.rgb(3, 8, 18);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        buildHud();
        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) { tts.setLanguage(Locale.US); ttsReady = true; }
        });
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.setText("SPEECH RECOGNITION UNAVAILABLE");
            listenButton.setEnabled(false);
            return;
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle p) { status.setText("LISTENING • SPEAK NOW"); }
            public void onBeginningOfSpeech() { status.setText("VOICE DETECTED"); }
            public void onRmsChanged(float r) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() { status.setText("PROCESSING COMMAND"); }
            public void onError(int e) {
                if (listening) { status.setText("RESTARTING LISTENER"); orb.postDelayed(() -> startRecognition(), 650); }
                else status.setText("SYSTEM STANDBY");
            }
            public void onResults(Bundle b) {
                ArrayList<String> m = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (m != null && !m.isEmpty()) { heard.setText("“" + m.get(0) + "”"); handleCommand(m.get(0)); }
                if (listening) orb.postDelayed(() -> startRecognition(), 350);
            }
            public void onPartialResults(Bundle b) {}
            public void onEvent(int t, Bundle p) {}
        });
    }

    private void buildHud() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER);
        root.setPadding(24, 32, 24, 24); root.setBackgroundColor(bg);
        TextView brand = new TextView(this);
        brand.setText("J.A.R.V.I.S"); brand.setTextColor(cyan); brand.setTextSize(30);
        brand.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD)); brand.setGravity(Gravity.CENTER);
        root.addView(brand);
        TextView sub = new TextView(this);
        sub.setText("PERSONAL AI • PHONE CONTROL SYSTEM"); sub.setTextColor(Color.rgb(115,190,215));
        sub.setTextSize(10); sub.setLetterSpacing(0.18f); sub.setGravity(Gravity.CENTER);
        root.addView(sub);
        orb = new TextView(this); orb.setText("◉"); orb.setTextColor(cyan); orb.setTextSize(112); orb.setGravity(Gravity.CENTER);
        root.addView(orb, new LinearLayout.LayoutParams(-1, 0, 1));
        status = new TextView(this); status.setText("SYSTEM STANDBY"); status.setTextColor(cyan);
        status.setTextSize(14); status.setGravity(Gravity.CENTER); root.addView(status);
        heard = new TextView(this); heard.setText("Tap ACTIVATE and speak a command");
        heard.setTextColor(Color.WHITE); heard.setTextSize(15); heard.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2); hp.topMargin = 18; hp.bottomMargin = 24;
        root.addView(heard, hp);
        listenButton = new Button(this); listenButton.setText("ACTIVATE JARVIS");
        listenButton.setTextColor(Color.BLACK); listenButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(cyan));
        listenButton.setOnClickListener(v -> toggleListening());
        root.addView(listenButton, new LinearLayout.LayoutParams(-1, 56));
        TextView hint = new TextView(this);
        hint.setText("TRY: OPEN WHATSAPP • OPEN YOUTUBE • OPEN CHROME\\nENGLISH + ROMAN URDU COMMANDS");
        hint.setTextColor(Color.rgb(100,145,165)); hint.setTextSize(10); hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2); tp.topMargin = 22; root.addView(hint, tp);
        setContentView(root);
    }

    private void toggleListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 77); return;
        }
        listening = !listening;
        listenButton.setText(listening ? "DEACTIVATE JARVIS" : "ACTIVATE JARVIS");
        if (listening) { say("I am listening"); startRecognition(); }
        else { recognizer.cancel(); status.setText("SYSTEM STANDBY"); }
    }

    private void startRecognition() {
        if (!listening || recognizer == null) return;
        try {
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-PK");
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-PK");
            i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            recognizer.startListening(i);
        } catch (Exception e) { status.setText("VOICE SERVICE ERROR"); }
    }

    private void handleCommand(String raw) {
        String s = raw.toLowerCase(Locale.ROOT).trim();
        if (s.contains("stop listening") || s.equals("stop") || s.contains("band ho jao")) {
            listening = false; listenButton.setText("ACTIVATE JARVIS"); status.setText("SYSTEM STANDBY"); say("Standing by"); return;
        }
        if (s.contains("hello jarvis") || s.equals("hello") || s.contains("suno jarvis")) {
            status.setText("JARVIS ONLINE"); say("Yes. How can I help?"); return;
        }

        // WhatsApp message typing: opens the active contact chat with text prefilled.
        String message = extractMessage(raw);
        if (message != null) {
            if (activeContactPhone == null) {
                status.setText("OPEN A WHATSAPP CONTACT FIRST"); say("Please open a WhatsApp contact first");
            } else {
                openWhatsAppChat(activeContactPhone, message);
                status.setText("MESSAGE READY • REVIEW BEFORE SENDING");
                say("Message is ready. Please review it and tap send in WhatsApp");
            }
            return;
        }

        boolean videoCall = has(s, "video call", "video-call", "videocall", "video karo");
        boolean voiceCall = has(s, "whatsapp call", "voice call", "call karo", "call kar do");
        if (videoCall || voiceCall) {
            String name = extractContactName(raw, videoCall ? "video call" : "call");
            if (name != null && !name.isEmpty()) { findContactAndOpen(name, videoCall ? "VIDEO" : "VOICE"); return; }
            if (activeContactPhone != null) { openWhatsAppChat(activeContactPhone, null); }
            else { status.setText("SAY A SAVED CONTACT NAME"); say("Please say the saved contact name"); }
            if (activeContactPhone != null) say("WhatsApp chat opened. Tap the video or phone icon to start the call");
            return;
        }

        if (has(s, "whatsapp", "what's app", "watsapp") || has(s, "chat kholo", "ki chat kholo", "chat open")) {
            String name = extractContactName(raw, "whatsapp");
            if (name == null || name.isEmpty()) name = extractContactName(raw, "chat");
            if (name != null && !name.isEmpty()) { findContactAndOpen(name, "CHAT"); return; }
            if (has(s, "open", "kholo", "chalao", "start")) { launchApp("com.whatsapp", "WhatsApp"); return; }
        }

        String pkg = null, label = null;
        if (has(s,"whatsapp","what's app","watsapp")) { pkg="com.whatsapp"; label="WhatsApp"; }
        else if (has(s,"instagram","insta")) { pkg="com.instagram.android"; label="Instagram"; }
        else if (has(s,"youtube","you tube")) { pkg="com.google.android.youtube"; label="YouTube"; }
        else if (has(s,"facebook","face book")) { pkg="com.facebook.katana"; label="Facebook"; }
        else if (has(s,"chrome","browser","google kholo","internet kholo")) { pkg="com.android.chrome"; label="Chrome"; }
        else if (has(s,"maps","google maps")) { pkg="com.google.android.apps.maps"; label="Maps"; }
        else if (has(s,"spotify")) { pkg="com.spotify.music"; label="Spotify"; }
        if (pkg != null && has(s,"open","kholo","chalao","start")) { launchApp(pkg,label); return; }
        status.setText("COMMAND NOT RECOGNIZED"); say("I did not understand that command");
    }

    private String extractMessage(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT).trim();
        String[] starts = {"type ", "message likho ", "likho ", "write ", "whatsapp message "};
        for (String prefix : starts) if (lower.startsWith(prefix)) return raw.trim().substring(prefix.length()).trim();
        return null;
    }

    private String extractContactName(String raw, String keyword) {
        String value = raw.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        String[] removals = {"whatsapp", "what's app", "watsapp", "please", "open", "kholo", "open karo", "ki", "ka", "chat", "video", "voice", "call", "karo", "kar do", "ko", "par", "on"};
        for (String word : removals) lower = lower.replace(word, " ");
        lower = lower.replaceAll("[^\\p{L}\\p{N} ]", " ").replaceAll("\\s+", " ").trim();
        return lower;
    }

    private void findContactAndOpen(String spokenName, String action) {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            pendingContactName = spokenName; pendingContactAction = action;
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, CONTACTS_PERMISSION);
            status.setText("CONTACTS PERMISSION REQUIRED"); return;
        }
        String phone = lookupPhone(spokenName);
        if (phone == null) { status.setText("CONTACT NOT FOUND: " + spokenName.toUpperCase()); say("I could not find that contact"); return; }
        activeContactName = spokenName; activeContactPhone = normalizePhone(phone);
        if (activeContactPhone.isEmpty()) { status.setText("INVALID CONTACT NUMBER"); say("That contact number is invalid"); return; }
        openWhatsAppChat(activeContactPhone, null);
        status.setText("OPENING WHATSAPP • " + spokenName.toUpperCase());
        if ("VOICE".equals(action)) say("WhatsApp chat opened. Tap the phone icon to call");
        else if ("VIDEO".equals(action)) say("WhatsApp chat opened. Tap the video icon to start a video call");
        else say("Opening WhatsApp chat with " + spokenName);
    }

    private String pendingContactName = null, pendingContactAction = null;

    private String lookupPhone(String requestedName) {
        Cursor c = null;
        try {
            c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER},
                null, null, null);
            if (c == null) return null;
            String wanted = requestedName.toLowerCase(Locale.ROOT).trim();
            String partialNumber = null;
            while (c.moveToNext()) {
                String name = c.getString(0), number = c.getString(1);
                if (name == null || number == null) continue;
                String n = name.toLowerCase(Locale.ROOT);
                if (n.equals(wanted)) return number;
                if (n.contains(wanted) || wanted.contains(n)) partialNumber = number;
            }
            return partialNumber;
        } catch (Exception e) { return null; }
        finally { if (c != null) c.close(); }
    }

    private String normalizePhone(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("00")) digits = digits.substring(2);
        if (digits.startsWith("0")) digits = "92" + digits.substring(1);
        else if (digits.length() == 10 && !digits.startsWith("92")) digits = "92" + digits;
        return digits;
    }

    private void openWhatsAppChat(String phone, String text) {
        try {
            String url = "https://wa.me/" + phone;
            if (text != null && !text.isEmpty()) url += "?text=" + Uri.encode(text);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage("com.whatsapp");
            startActivity(intent);
        } catch (Exception e) {
            try { startActivity(getPackageManager().getLaunchIntentForPackage("com.whatsapp")); }
            catch (Exception ignored) { status.setText("WHATSAPP NOT AVAILABLE"); }
        }
    }

    private boolean has(String s, String... words) { for (String w : words) if (s.contains(w)) return true; return false; }
    private void launchApp(String pkg, String label) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) { status.setText(label.toUpperCase()+" NOT INSTALLED"); say(label+" is not installed"); return; }
        startActivity(i); status.setText("LAUNCHING "+label.toUpperCase()); say("Opening "+label);
    }
    private void say(String s) { if (ttsReady) tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "jarvis"); }

    @Override public void onRequestPermissionsResult(int code, String[] p, int[] r) {
        super.onRequestPermissionsResult(code,p,r);
        if (code == 77 && r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) {
            listening = true; listenButton.setText("DEACTIVATE JARVIS"); startRecognition();
        } else if (code == 77) status.setText("MICROPHONE PERMISSION REQUIRED");
        else if (code == CONTACTS_PERMISSION) {
            if (r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED && pendingContactName != null) {
                String n = pendingContactName, a = pendingContactAction; pendingContactName = null; pendingContactAction = null;
                findContactAndOpen(n, a);
            } else { status.setText("CONTACTS PERMISSION DENIED"); say("Contacts permission is needed to find people by name"); }
        }
    }
    @Override protected void onDestroy() {
        listening = false;
        if (recognizer != null) { recognizer.destroy(); recognizer = null; }
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
