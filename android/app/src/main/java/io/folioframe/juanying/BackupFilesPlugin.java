package io.folioframe.juanying;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;

import androidx.activity.result.ActivityResult;

import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

/**
 * Backup file bridge. Exports use the user's public Download collection;
 * imports use the system document picker. Nothing is stored in Android/data.
 */
@CapacitorPlugin(name = "BackupFiles")
public class BackupFilesPlugin extends Plugin {
    /**
     * Writes a backup to the user's public Download collection. MediaStore keeps this visible
     * to the system file manager while avoiding broad storage permissions and Android/data.
     */
    @PluginMethod
    public void createDocument(PluginCall call) {
        String name = call.getString("name", "FolioFrame-backup.zip");
        String mimeType = call.getString("mimeType", "application/zip");
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, name);
            values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
            values.put(MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);
            values.put(MediaStore.Downloads.IS_PENDING, 1);
            Uri uri = getContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new Exception("无法创建 Download 文件");
            try (OutputStream output = getContext().getContentResolver().openOutputStream(uri, "w")) {
                if (output == null) throw new Exception("无法打开 Download 文件");
                output.write(Base64.decode(call.getString("base64", ""), Base64.DEFAULT));
            } catch (Exception error) {
                getContext().getContentResolver().delete(uri, null, null);
                throw error;
            }
            values.clear();
            values.put(MediaStore.Downloads.IS_PENDING, 0);
            getContext().getContentResolver().update(uri, values, null, null);
            JSObject response = new JSObject();
            response.put("uri", uri.toString());
            response.put("displayName", name);
            call.resolve(response);
        } catch (Exception error) {
            call.reject("写入 Download 失败：" + error.getMessage(), error);
        }
    }

    @PluginMethod
    public void openDocument(PluginCall call) {
        String mimeType = call.getString("mimeType", "application/zip");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(call, intent, "openDocumentResult");
    }

    @ActivityCallback
    private void openDocumentResult(PluginCall call, ActivityResult result) {
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) {
            call.reject("cancelled");
            return;
        }
        try {
            Uri uri = result.getData().getData();
            getContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            byte[] bytes;
            try (InputStream input = getContext().getContentResolver().openInputStream(uri)) {
                if (input == null) throw new Exception("无法读取所选文件");
                bytes = readAll(input);
            }
            JSObject response = new JSObject();
            response.put("base64", Base64.encodeToString(bytes, Base64.NO_WRAP));
            response.put("uri", uri.toString());
            call.resolve(response);
        } catch (Exception error) {
            call.reject("读取备份失败：" + error.getMessage(), error);
        }
    }

    @PluginMethod
    public void fetchImage(PluginCall call) {
        String source = call.getString("url");
        if (source == null || source.isEmpty()) {
            call.reject("缺少图片地址");
            return;
        }
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(source).openConnection();
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(20000);
                connection.setRequestProperty("User-Agent", "FolioFrame/0.2");
                connection.setInstanceFollowRedirects(true);
                int status = connection.getResponseCode();
                if (status < 200 || status >= 300) throw new Exception("HTTP " + status);
                byte[] bytes;
                try (InputStream input = connection.getInputStream()) { bytes = readAll(input); }
                String mime = connection.getContentType();
                if (mime == null || !mime.startsWith("image/")) mime = "image/jpeg";
                JSObject response = new JSObject();
                response.put("dataUrl", "data:" + mime + ";base64," + Base64.encodeToString(bytes, Base64.NO_WRAP));
                call.resolve(response);
            } catch (Exception error) {
                call.reject("缓存图片失败：" + error.getMessage(), error);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    /**
     * Generic WebDAV bridge. Most WebDAV servers send no CORS headers, so the WebView cannot
     * PUT or download cloud backups directly; this method performs the request natively.
     */
    @PluginMethod
    public void webdavRequest(PluginCall call) {
        String rawUrl = call.getString("url");
        if (rawUrl == null || rawUrl.isEmpty()) {
            call.reject("缺少 WebDAV 地址");
            return;
        }
        String rawMethod = call.getString("method", "GET");
        final String url = rawUrl.trim();
        final String verb = rawMethod == null ? "GET" : rawMethod.toUpperCase();
        final String username = call.getString("username", "");
        final String password = call.getString("password", "");
        final String bodyBase64 = call.getString("base64", "");
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(60000);
                connection.setRequestMethod(verb);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestProperty("User-Agent", "FolioFrame/0.4.1");
                if (username != null && !username.isEmpty()) {
                    String raw = username + ":" + (password == null ? "" : password);
                    connection.setRequestProperty("Authorization", "Basic " + Base64.encodeToString(raw.getBytes("UTF-8"), Base64.NO_WRAP));
                }
                if (bodyBase64 != null && !bodyBase64.isEmpty()) {
                    byte[] body = Base64.decode(bodyBase64, Base64.DEFAULT);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/zip");
                    connection.setFixedLengthStreamingMode(body.length);
                    try (OutputStream output = connection.getOutputStream()) {
                        output.write(body);
                    }
                }
                int status = connection.getResponseCode();
                if (status < 200 || status >= 300) {
                    call.reject("HTTP " + status, String.valueOf(status));
                    return;
                }
                JSObject response = new JSObject();
                response.put("status", status);
                if ("GET".equals(verb)) {
                    byte[] bytes;
                    try (InputStream input = connection.getInputStream()) {
                        bytes = readAll(input);
                    }
                    response.put("base64", Base64.encodeToString(bytes, Base64.NO_WRAP));
                }
                call.resolve(response);
            } catch (Exception error) {
                call.reject("WebDAV 请求失败：" + error.getMessage(), error);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    /**
     * Generic JSON request bridge. The WeRead gateway only permits the weread.qq.com origin,
     * so the WebView cannot call it directly; Android performs the request natively instead.
     */
    @PluginMethod
    public void httpRequest(PluginCall call) {
        String rawUrl = call.getString("url");
        if (rawUrl == null || rawUrl.isEmpty()) {
            call.reject("缺少请求地址");
            return;
        }
        final String url = rawUrl.trim();
        String rawMethod = call.getString("method", "GET");
        final String verb = rawMethod == null ? "GET" : rawMethod.toUpperCase();
        final String body = call.getString("body", null);
        final JSObject requestHeaders = call.getObject("headers", new JSObject());
        final int timeoutMs = call.getInt("timeoutMs", 30000);
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(timeoutMs);
                connection.setRequestMethod(verb);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "FolioFrame/0.5");
                if (requestHeaders != null) {
                    Iterator<String> names = requestHeaders.keys();
                    while (names.hasNext()) {
                        String name = names.next();
                        connection.setRequestProperty(name, requestHeaders.optString(name, ""));
                    }
                }
                if (body != null && !body.isEmpty()) {
                    byte[] bytes = body.getBytes("UTF-8");
                    connection.setDoOutput(true);
                    connection.setFixedLengthStreamingMode(bytes.length);
                    try (OutputStream output = connection.getOutputStream()) {
                        output.write(bytes);
                    }
                }
                int status = connection.getResponseCode();
                InputStream input = (status >= 200 && status < 400)
                        ? connection.getInputStream()
                        : connection.getErrorStream();
                String text = "";
                if (input != null) {
                    try (InputStream stream = input) {
                        text = new String(readAll(stream), "UTF-8");
                    }
                }
                JSObject response = new JSObject();
                response.put("status", status);
                response.put("text", text);
                call.resolve(response);
            } catch (Exception error) {
                call.reject("请求失败：" + error.getMessage(), error);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private byte[] readAll(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
        return output.toByteArray();
    }
}
