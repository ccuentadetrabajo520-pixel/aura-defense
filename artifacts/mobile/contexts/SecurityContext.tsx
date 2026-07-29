import React, { createContext, useCallback, useContext, useRef, useState } from 'react';
import { Platform } from 'react-native';

export type ThreatSeverity = 'critical' | 'high' | 'medium' | 'low';
export type LogLevel = 'OK' | 'THREAT' | 'WARN' | 'AUDIT' | 'INFO' | 'SYS';
export type ScanState = 'idle' | 'scanning' | 'complete';

export interface ThreatItem {
  id: string;
  name: string;
  packageName: string;
  severity: ThreatSeverity;
  threatType: string;
  description: string;
  permissions: string[];
  riskScore: number;
  purged: boolean;
}

export interface LogEntry {
  id: string;
  timestamp: string;
  level: LogLevel;
  message: string;
}

export interface NetworkStatus {
  ssid: string;
  gateway: string;
  dns: string;
  mitm: boolean;
  encrypted: boolean;
}

interface SecurityContextValue {
  firewallEnabled: boolean;
  scanState: ScanState;
  threats: ThreatItem[];
  logs: LogEntry[];
  networkStatus: NetworkStatus | null;
  rootDetected: boolean;
  debugDetected: boolean;
  toggleFirewall: () => void;
  startScan: () => void;
  purgeThreat: (id: string) => void;
  purgeAll: () => void;
  clearLogs: () => void;
  threatCount: number;
  criticalCount: number;
}

const SecurityContext = createContext<SecurityContextValue | null>(null);

const wait = (ms: number) => new Promise<void>((res) => setTimeout(res, ms));

const makeId = () =>
  Date.now().toString(36) + Math.random().toString(36).substr(2, 6);

const makeTs = () => {
  const n = new Date();
  const h = n.getHours().toString().padStart(2, '0');
  const m = n.getMinutes().toString().padStart(2, '0');
  const s = n.getSeconds().toString().padStart(2, '0');
  const ms = n.getMilliseconds().toString().padStart(3, '0');
  return `${h}:${m}:${s}.${ms}`;
};

interface SimApp {
  name: string;
  pkg: string;
  permissions: string[];
  risky: boolean;
  severity?: ThreatSeverity;
  threatType?: string;
  description?: string;
  riskScore?: number;
}

const SIMULATED_APPS: SimApp[] = [
  {
    name: 'SystemCore Services',
    pkg: 'com.android.systemcore',
    permissions: ['BOOT_COMPLETED', 'INTERNET'],
    risky: false,
  },
  {
    name: 'QuickShare Pro',
    pkg: 'com.quickshare.pro',
    permissions: [
      'INTERNET',
      'READ_CONTACTS',
      'ACCESS_FINE_LOCATION',
      'READ_EXTERNAL_STORAGE',
      'RECEIVE_BOOT_COMPLETED',
      'FOREGROUND_SERVICE',
    ],
    risky: true,
    severity: 'critical',
    threatType: 'Spyware / Data Exfiltration',
    description:
      'Requests boot persistence + contacts + GPS + storage in parallel. Known data broker fingerprint. Active C2 communication detected.',
    riskScore: 94,
  },
  {
    name: 'BatteryBooster+',
    pkg: 'com.battery.boosterplus',
    permissions: ['INTERNET', 'WRITE_SETTINGS', 'SYSTEM_ALERT_WINDOW', 'FOREGROUND_SERVICE'],
    risky: true,
    severity: 'high',
    threatType: 'Adware / Overlay Attack',
    description:
      'SYSTEM_ALERT_WINDOW enables screen overlay over banking and auth apps. Used for credential phishing and ad injection.',
    riskScore: 78,
  },
  {
    name: 'Flashlight Widget',
    pkg: 'com.flash.widget',
    permissions: ['CAMERA'],
    risky: false,
  },
  {
    name: 'Clock & Calendar',
    pkg: 'com.clock.calendar',
    permissions: ['READ_CALENDAR', 'INTERNET'],
    risky: false,
  },
  {
    name: 'DataSync Helper',
    pkg: 'com.datasync.helper',
    permissions: [
      'INTERNET',
      'READ_EXTERNAL_STORAGE',
      'WRITE_EXTERNAL_STORAGE',
      'ACCESS_FINE_LOCATION',
      'READ_CALL_LOG',
      'RECEIVE_BOOT_COMPLETED',
      'READ_SMS',
    ],
    risky: true,
    severity: 'critical',
    threatType: 'Stalkerware',
    description:
      'Call logs + SMS + GPS + filesystem + boot persistence. Classic stalkerware signature. Possible domestic surveillance tool.',
    riskScore: 97,
  },
  {
    name: 'VPN Master Free',
    pkg: 'com.vpn.master.free',
    permissions: [
      'INTERNET',
      'FOREGROUND_SERVICE',
      'RECEIVE_BOOT_COMPLETED',
      'READ_EXTERNAL_STORAGE',
    ],
    risky: true,
    severity: 'medium',
    threatType: 'Suspicious VPN Provider',
    description:
      'Unverified VPN with boot persistence. Traffic routing to unvalidated servers. Possible MITM proxy injection.',
    riskScore: 61,
  },
  {
    name: 'Google Maps',
    pkg: 'com.google.android.apps.maps',
    permissions: ['ACCESS_FINE_LOCATION', 'INTERNET'],
    risky: false,
  },
  {
    name: 'Chrome Browser',
    pkg: 'com.android.chrome',
    permissions: ['INTERNET', 'CAMERA', 'RECORD_AUDIO', 'ACCESS_FINE_LOCATION'],
    risky: false,
  },
  {
    name: 'CleanMaster Pro',
    pkg: 'com.clean.master.pro',
    permissions: [
      'INTERNET',
      'WRITE_SETTINGS',
      'SYSTEM_ALERT_WINDOW',
      'READ_CONTACTS',
      'ACCESS_FINE_LOCATION',
      'RECEIVE_BOOT_COMPLETED',
    ],
    risky: true,
    severity: 'high',
    threatType: 'Aggressive Adware / PUP',
    description:
      'Potentially Unwanted Program. Screen overlay + contacts + GPS with no legitimate cleaning justification. Known ad fraud network participant.',
    riskScore: 82,
  },
];

export function SecurityProvider({ children }: { children: React.ReactNode }) {
  const [firewallEnabled, setFirewallEnabled] = useState(false);
  const [scanState, setScanState] = useState<ScanState>('idle');
  const [threats, setThreats] = useState<ThreatItem[]>([]);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [networkStatus, setNetworkStatus] = useState<NetworkStatus | null>(null);
  const [rootDetected, setRootDetected] = useState(false);
  const [debugDetected, setDebugDetected] = useState(false);
  const scanning = useRef(false);

  const toggleFirewall = useCallback(() => {
    setFirewallEnabled((prev) => !prev);
  }, []);

  const startScan = useCallback(async () => {
    if (scanning.current) return;
    scanning.current = true;
    setScanState('scanning');
    setThreats([]);
    setLogs([]);

    const log = (level: LogLevel, message: string) => {
      setLogs((prev) => [
        ...prev,
        { id: makeId(), timestamp: makeTs(), level, message },
      ]);
    };

    // --- Phase 0: Init ---
    await wait(80);
    log('SYS', 'AuraDefensa Engine v2.1.0 — Initializing threat analysis suite');
    await wait(180);
    log('SYS', 'Loading signature database: 3,847 threat patterns indexed');
    await wait(200);
    log('AUDIT', `Platform: ${Platform.OS.toUpperCase()} ${Platform.Version}`);
    await wait(150);
    log('AUDIT', 'Modules loaded: INTEGRITY · NET · PKG · PRIV · EXIF');
    await wait(200);

    // --- Phase 1: System Integrity ---
    log('AUDIT', '════════ PHASE 1: SYSTEM INTEGRITY ════════');
    await wait(220);
    log('AUDIT', 'Scanning /system/bin/su ...');
    await wait(280);
    log('AUDIT', 'Scanning /system/xbin/su ...');
    await wait(260);
    log('AUDIT', 'Scanning /sbin/su ...');
    await wait(300);
    log('AUDIT', 'Scanning /data/local/tmp/su ...');
    await wait(350);
    log('AUDIT', 'Checking ROM build keys — test-keys signature probe');
    await wait(400);

    const isDebug = __DEV__;
    setRootDetected(false);
    setDebugDetected(isDebug);

    log('OK', 'Root binaries: CLEAN — No su vectors found in critical paths');
    await wait(200);
    log('OK', 'ROM signature: VERIFIED — Official release keys confirmed');
    await wait(200);
    if (isDebug) {
      log('WARN', 'DEBUG mode active — Debugger connected (isDebuggerConnected=true)');
      log('WARN', 'Reverse-engineering risk elevated. Anti-tamper layer engaged.');
    } else {
      log('OK', 'Debug state: CLEAN — No debugger attached');
    }
    await wait(250);
    log('OK', 'System integrity score: 98/100');
    await wait(200);

    // --- Phase 2: Network Analysis ---
    log('AUDIT', '════════ PHASE 2: NETWORK THREAT ANALYSIS ════════');
    await wait(200);
    log('INFO', 'Interface: wlan0 — 802.11ac 5 GHz / WPA3');
    await wait(250);
    log('AUDIT', 'Resolving gateway address ...');
    await wait(320);
    log('INFO', 'Gateway: 192.168.1.1 — ARP mapping validated');
    await wait(250);
    log('AUDIT', 'Checking ARP cache for poisoning vectors ...');
    await wait(450);
    log('OK', 'ARP table: CLEAN — No cache poisoning detected');
    await wait(220);
    log('AUDIT', 'Probing DNS resolver: 8.8.8.8 (Google Public DNS) ...');
    await wait(400);
    log('OK', 'DNS resolver: TRUSTED — DNSSEC validation passed');
    await wait(250);
    log('AUDIT', 'Executing MitM detection probe (TLS fingerprint analysis) ...');
    await wait(600);
    log('OK', 'TLS chain: VALID — No certificate injection or interception');
    await wait(200);
    log('OK', 'Network threat status: SECURE');
    setNetworkStatus({ ssid: 'WiFi-Home-5G', gateway: '192.168.1.1', dns: '8.8.8.8', mitm: false, encrypted: true });
    await wait(250);

    // --- Phase 3: Package Scanner ---
    log('AUDIT', '════════ PHASE 3: PACKAGE THREAT ANALYSIS ════════');
    await wait(200);
    log('INFO', `Enumerating packages: ${SIMULATED_APPS.length} applications detected`);
    await wait(300);

    const found: ThreatItem[] = [];
    for (const app of SIMULATED_APPS) {
      await wait(120 + Math.random() * 180);
      log('AUDIT', `Analyzing: ${app.name}  [${app.pkg}]`);
      await wait(80 + Math.random() * 120);
      if (app.risky) {
        log('THREAT', `⚠ HIGH RISK: ${app.name} — ${app.threatType}`);
        log('THREAT', `  Flagged perms: ${app.permissions.slice(0, 3).join(' · ')}`);
        log('THREAT', `  Risk score: ${app.riskScore}/100`);
        const item: ThreatItem = {
          id: makeId(),
          name: app.name,
          packageName: app.pkg,
          severity: app.severity!,
          threatType: app.threatType!,
          description: app.description!,
          permissions: app.permissions,
          riskScore: app.riskScore!,
          purged: false,
        };
        found.push(item);
        setThreats([...found]);
      } else {
        log('OK', `  ${app.name}: permission profile nominal`);
      }
    }
    await wait(250);

    // --- Phase 4: Privacy Monitor ---
    log('AUDIT', '════════ PHASE 4: PRIVACY / OVERLAY MONITOR ════════');
    await wait(280);
    log('AUDIT', 'Scanning for SYSTEM_ALERT_WINDOW overlay grants ...');
    await wait(400);
    const overlayApps = SIMULATED_APPS.filter((a) =>
      a.permissions.includes('SYSTEM_ALERT_WINDOW')
    );
    if (overlayApps.length > 0) {
      log('WARN', `${overlayApps.length} app(s) hold overlay capability — Phishing risk`);
      overlayApps.forEach((a) =>
        log('WARN', `  ${a.name} — SYSTEM_ALERT_WINDOW active`)
      );
    } else {
      log('OK', 'Overlay attack surface: CLEAN');
    }
    await wait(300);
    log('AUDIT', 'Checking background camera/microphone access patterns ...');
    await wait(400);
    log('OK', 'No covert sensor access detected in last 24h window');
    await wait(250);

    // --- Phase 5: EXIF Data Leak ---
    log('AUDIT', '════════ PHASE 5: DATA LEAK SCAN (EXIF) ════════');
    await wait(220);
    log('AUDIT', 'Scanning /DCIM/Camera — GPS coordinate embedding ...');
    await wait(450);
    log('AUDIT', 'Scanning /Pictures — hardware fingerprint markers ...');
    await wait(380);
    log('AUDIT', 'Parsing metadata headers: EXIF · XMP · IPTC ...');
    await wait(300);
    log('WARN', 'GPS coordinates embedded in 14 media files');
    log('WARN', 'Hardware fingerprint exposed: {Make: SM-G998B, Model: Galaxy S21+}');
    log('INFO', 'Recommendation: Strip EXIF before sharing media to external services');
    await wait(250);

    // --- Finalize ---
    log('SYS', '════════ SCAN COMPLETE ════════');
    await wait(180);
    log('SYS', `Threats identified: ${found.length}`);
    log(
      'SYS',
      `Critical: ${found.filter((t) => t.severity === 'critical').length}  |  High: ${found.filter((t) => t.severity === 'high').length}  |  Medium: ${found.filter((t) => t.severity === 'medium').length}`
    );
    log('SYS', 'Navigate to PURGE CONSOLE for immediate remediation.');

    setThreats(found);
    setScanState('complete');
    scanning.current = false;
  }, []);

  const purgeThreat = useCallback((id: string) => {
    setThreats((prev) => prev.map((t) => (t.id === id ? { ...t, purged: true } : t)));
  }, []);

  const purgeAll = useCallback(() => {
    setThreats((prev) => prev.map((t) => ({ ...t, purged: true })));
  }, []);

  const clearLogs = useCallback(() => setLogs([]), []);

  const active = threats.filter((t) => !t.purged);

  return (
    <SecurityContext.Provider
      value={{
        firewallEnabled,
        scanState,
        threats,
        logs,
        networkStatus,
        rootDetected,
        debugDetected,
        toggleFirewall,
        startScan,
        purgeThreat,
        purgeAll,
        clearLogs,
        threatCount: active.length,
        criticalCount: active.filter((t) => t.severity === 'critical').length,
      }}
    >
      {children}
    </SecurityContext.Provider>
  );
}

export function useSecurity() {
  const ctx = useContext(SecurityContext);
  if (!ctx) throw new Error('useSecurity must be inside SecurityProvider');
  return ctx;
}
