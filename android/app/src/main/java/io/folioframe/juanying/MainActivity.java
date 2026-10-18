package io.folioframe.juanying;

import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private long lastBackPressAt = 0L;
    private static final long EXIT_CONFIRM_WINDOW_MS = 2000L;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Capacitor must know this bridge before BridgeActivity loads the WebView plugin registry.
        registerPlugin(BackupFilesPlugin.class);
        super.onCreate(savedInstanceState);
        // One-time migration: older Android builds registered a WebView service worker that can
        // keep serving an obsolete HTML shell after an APK upgrade. This touches only resource
        // cache, never IndexedDB/local records, then reloads once into the APK-bundled page.
        bridge.getWebView().postDelayed(() -> bridge.getWebView().evaluateJavascript(
            "(function(){try{if(localStorage.getItem('folioframe_android_sw_migrated_v1'))return;localStorage.setItem('folioframe_android_sw_migrated_v1','1');Promise.all([navigator.serviceWorker?navigator.serviceWorker.getRegistrations().then(function(rs){return Promise.all(rs.map(function(r){return r.unregister();}));}):Promise.resolve(),caches?caches.keys().then(function(keys){return Promise.all(keys.filter(function(k){return k.indexOf('folioframe-')===0;}).map(function(k){return caches.delete(k);}));}):Promise.resolve()]).finally(function(){location.reload();});}catch(e){}})()",
            ignored -> { }
        ), 700);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // A detail route should always return to its media list, never close the app.
                bridge.getWebView().evaluateJavascript(
                    "(function(){if(location.hash.indexOf('#/detail/')===0){backFromDetail();return 'handled';}if(location.hash!=='#/home'){go('home');return 'handled';}return 'home';})()",
                    result -> {
                        if ("\"home\"".equals(result)) {
                            long now = System.currentTimeMillis();
                            if (now - lastBackPressAt < EXIT_CONFIRM_WINDOW_MS) {
                                setEnabled(false);
                                getOnBackPressedDispatcher().onBackPressed();
                            } else {
                                lastBackPressAt = now;
                                android.widget.Toast.makeText(MainActivity.this, "再按一次退出卷影", android.widget.Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                );
            }
        });
    }
}
