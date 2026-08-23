package com.example.auradefensa;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.core.app.NotificationCompat;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AuraVpnService extends VpnService {
  private static final String CHANNEL_ID = "aura_vpn";
  private static final Set<String> BLOCKED_DOMAINS = new HashSet<>(Arrays.asList(
    "doubleclick.net",
    "googlesyndication.com",
    "google-analytics.com",
    "facebook.net",
    "phishing.example",
    "malware.example"
  ));

  private ParcelFileDescriptor vpnInterface;
  private Thread vpnThread;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    startForeground(2001, buildNotification());
    prepareVpnSession();
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (vpnInterface != null) {
      try {
        vpnInterface.close();
      } catch (IOException ignored) {
      }
      vpnInterface = null;
    }
    if (vpnThread != null) {
      vpnThread.interrupt();
      vpnThread = null;
    }
  }

  private void prepareVpnSession() {
    if (vpnThread != null && vpnThread.isAlive()) {
      return;
    }

    Builder builder = new Builder();
    builder.setSession("Aura Defense VPN");
    builder.addAddress("10.8.0.2", 32);
    builder.addRoute("0.0.0.0", 0);
    builder.addDnsServer("1.1.1.1");
    builder.addDnsServer("8.8.8.8");

    try {
      vpnInterface = builder.establish();
      if (vpnInterface == null) {
        stopSelf();
        return;
      }
      vpnThread = new Thread(new Runnable() {
        @Override
        public void run() {
          inspectTraffic();
        }
      });
      vpnThread.start();
    } catch (Throwable t) {
      stopSelf();
    }
  }

  private void inspectTraffic() {
    if (vpnInterface == null) {
      return;
    }

    try (FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
         FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor())) {
      byte[] buffer = new byte[4096];
      while (!Thread.currentThread().isInterrupted()) {
        int read = in.read(buffer);
        if (read <= 0) {
          break;
        }
        String text = new String(buffer, 0, read, StandardCharsets.UTF_8);
        if (containsBlockedDomain(text)) {
          continue;
        }
        out.write(buffer, 0, read);
      }
    } catch (IOException ignored) {
    }
  }

  private boolean containsBlockedDomain(String payload) {
    for (String domain : BLOCKED_DOMAINS) {
      if (payload.toLowerCase().contains(domain.toLowerCase())) {
        return true;
      }
    }
    return false;
  }

  private Notification buildNotification() {
    Context context = getApplicationContext();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Aura VPN", NotificationManager.IMPORTANCE_LOW);
      NotificationManager manager = context.getSystemService(NotificationManager.class);
      if (manager != null) {
        manager.createNotificationChannel(channel);
      }
    }

    Intent intent = new Intent(this, AuraVpnService.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

    return new NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Aura Defense VPN")
      .setContentText("Active DNS filtering and packet inspection")
      .setSmallIcon(android.R.drawable.stat_notify_sync)
      .setContentIntent(pendingIntent)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build();
  }
}
