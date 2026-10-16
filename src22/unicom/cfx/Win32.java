package unicom.cfx;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * The Windows calls cfx_http5 needs (release 1.0.0-dev5, Oct 16, 2026), through the Foreign Function & Memory API
 * (Java 22+): the WinHTTP default proxy (PROXYSERVER empty) and LogonUser / impersonation (FILEUSER, FILEPASS,
 * FILEDOMAIN). Compiled separately for Java 22 (build.ps1) and loaded by reflection (Http5Tag.nat()), so the rest of
 * the tag still runs on Java 21. Java prints a one-time "restricted method" warning to the server log unless the JVM
 * runs with --enable-native-access=ALL-UNNAMED.
 */
final class Win32 implements Http5Tag.Native {
    private static final Linker L = Linker.nativeLinker();
    private static final StructLayout CAPTURE = Linker.Option.captureStateLayout();
    private static final VarHandle LAST_ERROR = CAPTURE.varHandle(MemoryLayout.PathElement.groupElement("GetLastError"));
    private static final int LOGON32_LOGON_INTERACTIVE = 2, LOGON32_LOGON_NETWORK_CLEARTEXT = 8, ERROR_LOGON_TYPE_NOT_GRANTED = 1385;

    private final MethodHandle logonUser, impersonate, revert, closeHandle, formatMessage, getProxy, globalFree;

    Win32() {
        Arena g = Arena.global();
        SymbolLookup adv = SymbolLookup.libraryLookup("advapi32", g);
        SymbolLookup k32 = SymbolLookup.libraryLookup("kernel32", g);
        SymbolLookup wh = SymbolLookup.libraryLookup("winhttp", g);
        Linker.Option gle = Linker.Option.captureCallState("GetLastError");
        logonUser = L.downcallHandle(adv.find("LogonUserW").orElseThrow(),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS), gle);
        impersonate = L.downcallHandle(adv.find("ImpersonateLoggedOnUser").orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS), gle);
        revert = L.downcallHandle(adv.find("RevertToSelf").orElseThrow(), FunctionDescriptor.of(JAVA_INT), gle);
        closeHandle = L.downcallHandle(k32.find("CloseHandle").orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS));
        formatMessage = L.downcallHandle(k32.find("FormatMessageW").orElseThrow(),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
        getProxy = L.downcallHandle(wh.find("WinHttpGetDefaultProxyConfiguration").orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS), gle);
        globalFree = L.downcallHandle(k32.find("GlobalFree").orElseThrow(), FunctionDescriptor.of(ADDRESS, ADDRESS));
    }

    private static MemorySegment wide(Arena a, String s) {
        return s == null ? MemorySegment.NULL : a.allocateFrom(s, StandardCharsets.UTF_16LE);
    }

    private static int lastError(MemorySegment capture) { return (int) LAST_ERROR.get(capture, 0L); }

    private Http5Tag.NativeError error(int code) { return new Http5Tag.NativeError(code, message(code)); }

    /** The system's text for a Windows error code, e.g. 1326 "The user name or password is incorrect." */
    String message(int code) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment buf = a.allocate(2048 * 2L);
            int n = (int) formatMessage.invokeExact(0x1000 | 0x200, MemorySegment.NULL, code, 0, buf, 2048, MemorySegment.NULL);
            if (n <= 0) { return "Windows error " + code; }
            return buf.getString(0, StandardCharsets.UTF_16LE).trim();
        } catch (Throwable t) {
            return "Windows error " + code;
        }
    }

    @Override
    public String[] defaultProxy() throws Throwable {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment cap = a.allocate(CAPTURE);
            MemorySegment info = a.allocate(24, 8);   // WINHTTP_PROXY_INFO: DWORD dwAccessType; LPWSTR lpszProxy; LPWSTR lpszProxyBypass
            int ok = (int) getProxy.invokeExact(cap, info);
            if (ok == 0) { throw error(lastError(cap)); }
            int type = info.get(JAVA_INT, 0);
            return new String[] { String.valueOf(type), take(info.get(ADDRESS, 8)), take(info.get(ADDRESS, 16)) };
        }
    }

    /** Reads a WinHTTP-allocated wide string and frees it. */
    private String take(MemorySegment p) throws Throwable {
        if (p.address() == 0) { return ""; }
        try {
            return p.reinterpret(1 << 16).getString(0, StandardCharsets.UTF_16LE);
        } finally {
            MemorySegment ignored = (MemorySegment) globalFree.invokeExact(p);
        }
    }

    @Override
    public long logon(String user, String domain, char[] password) throws Throwable {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment cap = a.allocate(CAPTURE);
            MemorySegment u = wide(a, user);
            MemorySegment d = wide(a, domain == null || domain.isEmpty() ? null : domain);
            MemorySegment p = a.allocate((password.length + 1) * 2L, 2);
            for (int i = 0; i < password.length; i++) { p.setAtIndex(java.lang.foreign.ValueLayout.JAVA_CHAR, i, password[i]); }
            MemorySegment ph = a.allocate(ADDRESS);
            try {
                int ok = (int) logonUser.invokeExact(cap, u, d, p, LOGON32_LOGON_INTERACTIVE, 0, ph);
                if (ok == 0 && lastError(cap) == ERROR_LOGON_TYPE_NOT_GRANTED) {
                    // the account may not log on locally: a network-cleartext logon still carries its network credentials
                    ok = (int) logonUser.invokeExact(cap, u, d, p, LOGON32_LOGON_NETWORK_CLEARTEXT, 0, ph);
                }
                if (ok == 0) { throw error(lastError(cap)); }
                return ph.get(ADDRESS, 0).address();
            } finally {
                p.fill((byte) 0);
            }
        }
    }

    @Override
    public void impersonate(long token) throws Throwable {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment cap = a.allocate(CAPTURE);
            int ok = (int) impersonate.invokeExact(cap, MemorySegment.ofAddress(token));
            if (ok == 0) { throw error(lastError(cap)); }
        }
    }

    @Override
    public void revert() throws Throwable {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment cap = a.allocate(CAPTURE);
            int ok = (int) revert.invokeExact(cap);
            if (ok == 0) { throw error(lastError(cap)); }
        }
    }

    @Override
    public void close(long token) {
        if (token == 0) { return; }
        try {
            int ignored = (int) closeHandle.invokeExact(MemorySegment.ofAddress(token));
        } catch (Throwable ignored) {
            // nothing more can be done with a handle that will not close
        }
    }
}
