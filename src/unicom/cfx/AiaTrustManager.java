package unicom.cfx;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertPathBuilderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Strict SSL the way WinHTTP (CFX_HTTP5) does it: when a server leaves out an intermediate certificate, download it
 * from the certificate's Authority Information Access "caIssuers" URL and validate again (up to 3 levels).
 * Everything else, including the hostname check, is done by the normal PKIX trust manager. Only cfx_http5 uses this;
 * nothing JVM-wide changes. Downloaded certificates are cached by URL.
 */
final class AiaTrustManager extends X509ExtendedTrustManager {
    private static final ConcurrentHashMap<String, List<X509Certificate>> CACHE = new ConcurrentHashMap<>();
    /** DER of OID 1.3.6.1.5.5.7.48.2 (id-ad-caIssuers) */
    private static final byte[] CA_ISSUERS = { 0x06, 0x08, 0x2B, 0x06, 0x01, 0x05, 0x05, 0x07, 0x30, 0x02 };
    private final X509ExtendedTrustManager tm;

    private AiaTrustManager(X509ExtendedTrustManager tm) { this.tm = tm; }

    static X509ExtendedTrustManager forStore(KeyStore ks) throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        for (TrustManager t : tmf.getTrustManagers()) {
            if (t instanceof X509ExtendedTrustManager) { return new AiaTrustManager((X509ExtendedTrustManager) t); }
        }
        throw new IllegalStateException("No X509ExtendedTrustManager available");
    }

    interface Check { void run(X509Certificate[] chain) throws CertificateException; }

    private static boolean pathProblem(CertificateException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof CertPathBuilderException) { return true; }
            if (t.getMessage() != null && t.getMessage().contains("PKIX path building failed")) { return true; }
            if (t.getCause() == t) { break; }
        }
        return false;
    }

    private static void withAia(X509Certificate[] chain, Check check) throws CertificateException {
        try {
            check.run(chain);
        } catch (CertificateException first) {
            if (chain == null || chain.length == 0 || !pathProblem(first)) { throw first; }
            X509Certificate[] cur = chain;
            for (int depth = 0; depth < 3; depth++) {
                List<X509Certificate> next = new ArrayList<>(Arrays.asList(cur));
                for (X509Certificate c : issuersOf(cur[cur.length - 1])) { if (!next.contains(c)) { next.add(c); } }
                if (next.size() == cur.length) { break; }
                cur = next.toArray(new X509Certificate[0]);
                try { check.run(cur); return; } catch (CertificateException again) { /* go one level higher */ }
            }
            throw first;
        }
    }

    static List<X509Certificate> issuersOf(X509Certificate cert) {
        List<X509Certificate> out = new ArrayList<>();
        if (cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal())) { return out; }
        for (String url : caIssuerUrls(cert)) {
            List<X509Certificate> got = CACHE.get(url);
            if (got == null) {
                got = download(url);
                if (!got.isEmpty()) { CACHE.put(url, got); }
            }
            out.addAll(got);
            if (!out.isEmpty()) { break; }
        }
        return out;
    }

    /** caIssuers URLs from the AIA extension (1.3.6.1.5.5.7.1.1): each is the OID followed by [6] IA5String. */
    static List<String> caIssuerUrls(X509Certificate cert) {
        byte[] ext = cert.getExtensionValue("1.3.6.1.5.5.7.1.1");
        if (ext == null) { return Collections.emptyList(); }
        List<String> urls = new ArrayList<>();
        for (int i = 0; i + CA_ISSUERS.length < ext.length; i++) {
            boolean match = true;
            for (int k = 0; k < CA_ISSUERS.length; k++) { if (ext[i + k] != CA_ISSUERS[k]) { match = false; break; } }
            if (!match) { continue; }
            int p = i + CA_ISSUERS.length;
            if (p + 1 >= ext.length || (ext[p] & 0xFF) != 0x86) { continue; }
            int len = ext[p + 1] & 0xFF;
            p += 2;
            if ((len & 0x80) != 0) {
                int n = len & 0x7F;
                len = 0;
                for (int k = 0; k < n && p < ext.length; k++) { len = (len << 8) | (ext[p++] & 0xFF); }
            }
            if (len <= 0 || p + len > ext.length) { continue; }
            String u = new String(ext, p, len, StandardCharsets.US_ASCII);
            if (u.startsWith("http://") || u.startsWith("https://")) { urls.add(u); }
        }
        return urls;
    }

    static List<X509Certificate> download(String url) {
        List<X509Certificate> out = new ArrayList<>();
        try {
            HttpURLConnection c = (HttpURLConnection) new URI(url).toURL().openConnection();
            c.setConnectTimeout(10000);
            c.setReadTimeout(10000);
            c.setInstanceFollowRedirects(true);
            if (c.getResponseCode() != 200) { c.disconnect(); return out; }
            byte[] bytes;
            try (InputStream in = c.getInputStream()) { bytes = in.readNBytes(1024 * 1024); }
            c.disconnect();
            for (Certificate cert : CertificateFactory.getInstance("X.509").generateCertificates(new ByteArrayInputStream(bytes))) {
                if (cert instanceof X509Certificate) { out.add((X509Certificate) cert); }
            }
        } catch (Exception ignored) {
            // no intermediate -> the original PKIX error is reported
        }
        return out;
    }

    @Override public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException { tm.checkClientTrusted(chain, authType); }
    @Override public void checkClientTrusted(X509Certificate[] chain, String authType, Socket s) throws CertificateException { tm.checkClientTrusted(chain, authType, s); }
    @Override public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine e) throws CertificateException { tm.checkClientTrusted(chain, authType, e); }
    @Override public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException { withAia(chain, c -> tm.checkServerTrusted(c, authType)); }
    @Override public void checkServerTrusted(X509Certificate[] chain, String authType, Socket s) throws CertificateException { withAia(chain, c -> tm.checkServerTrusted(c, authType, s)); }
    @Override public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine e) throws CertificateException { withAia(chain, c -> tm.checkServerTrusted(c, authType, e)); }
    @Override public X509Certificate[] getAcceptedIssuers() { return tm.getAcceptedIssuers(); }
}
