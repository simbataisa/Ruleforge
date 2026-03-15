package io.ruleforge.event.service;

import org.springframework.stereotype.Component;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Guards against Server-Side Request Forgery (SSRF) by validating webhook URLs
 * before registration and delivery. Blocks RFC1918, loopback, link-local, and IPv6 ULA ranges.
 */
@Component
public class SsrfGuard {

    /**
     * Validates a URL is safe to deliver webhooks to.
     * Blocks RFC1918, loopback, link-local, and IPv6 ULA ranges.
     * @throws IllegalArgumentException with descriptive message if blocked
     */
    public void validate(String urlString) {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid webhook URL: " + e.getMessage());
        }

        String scheme = url.getProtocol();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Webhook URL must use http or https scheme");
        }

        String host = url.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Webhook URL must have a valid host");
        }

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Webhook URL host cannot be resolved: " + host);
        }

        for (InetAddress addr : addresses) {
            checkAddress(addr);
        }
    }

    private void checkAddress(InetAddress addr) {
        byte[] bytes = addr.getAddress();

        if (addr.isLoopbackAddress()) {
            throw new IllegalArgumentException("Webhook URL resolves to loopback address (blocked)");
        }
        if (addr.isSiteLocalAddress()) {
            throw new IllegalArgumentException("Webhook URL resolves to private/site-local address (blocked)");
        }
        if (addr.isLinkLocalAddress()) {
            throw new IllegalArgumentException("Webhook URL resolves to link-local address (blocked)");
        }
        if (addr.isAnyLocalAddress()) {
            throw new IllegalArgumentException("Webhook URL resolves to wildcard address (blocked)");
        }
        if (addr.isMulticastAddress()) {
            throw new IllegalArgumentException("Webhook URL resolves to multicast address (blocked)");
        }

        // Additional IPv4 checks
        if (bytes.length == 4) {
            // 100.64.0.0/10 — IANA shared address space (carrier-grade NAT)
            if ((bytes[0] & 0xFF) == 100 && (bytes[1] & 0xC0) == 64) {
                throw new IllegalArgumentException("Webhook URL resolves to shared address space (blocked)");
            }
            // 169.254.0.0/16 — APIPA/link-local (redundant with isLinkLocalAddress but explicit)
            if ((bytes[0] & 0xFF) == 169 && (bytes[1] & 0xFF) == 254) {
                throw new IllegalArgumentException("Webhook URL resolves to link-local address (blocked)");
            }
        }

        // Additional IPv6 checks
        if (bytes.length == 16) {
            // fc00::/7 — Unique Local Addresses
            if ((bytes[0] & 0xFE) == 0xFC) {
                throw new IllegalArgumentException("Webhook URL resolves to IPv6 ULA address (blocked)");
            }
            // ::ffff:0:0/96 — IPv4-mapped addresses (could map to RFC1918)
            boolean isIpv4Mapped = true;
            for (int i = 0; i < 10; i++) {
                if (bytes[i] != 0) { isIpv4Mapped = false; break; }
            }
            if (isIpv4Mapped && bytes[10] == (byte)0xFF && bytes[11] == (byte)0xFF) {
                // Check the mapped IPv4 address
                byte[] mapped = new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]};
                try {
                    checkAddress(InetAddress.getByAddress(mapped));
                } catch (UnknownHostException ignored) {}
            }
        }
    }
}
