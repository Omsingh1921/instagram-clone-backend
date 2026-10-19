package com.instagramclone.security;

import com.instagramclone.ratelimit.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Centralized, trusted-proxy-aware client IP resolution service.
 *
 * Enforces strict proxy boundary verification:
 * 1. Never trusts forwarding headers (X-Forwarded-For, X-Real-IP) unless the immediate socket peer
 *    (request.getRemoteAddr()) is explicitly configured as a trusted proxy address or CIDR subnet.
 * 2. When the immediate peer is trusted, safely traverses the proxy chain from right to left,
 *    stopping at the first non-trusted valid IP address.
 * 3. Validates IP syntax to reject malformed entries and prevent arbitrary cache-key injection.
 * 4. Normalizes IPv6 loopback addresses to 127.0.0.1.
 * 5. Reused across rate limiting, authentication, and device detection for consistent security boundaries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private final RateLimitProperties rateLimitProperties;

    private static final String DEFAULT_FALLBACK_IP = "127.0.0.1";

    private static final Pattern IPV4_PATTERN =
            Pattern.compile("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    private static final Pattern IPV6_PATTERN =
            Pattern.compile("^[0-9a-fA-F:]{2,45}$");

    /**
     * Resolves the verified client IP address from the HTTP servlet request.
     *
     * @param request the current HttpServletRequest
     * @return the verified client IP address string
     */
    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return DEFAULT_FALLBACK_IP;
        }

        String remoteAddr = normalizeIp(request.getRemoteAddr());
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return DEFAULT_FALLBACK_IP;
        }

        // Security boundary 1: Direct client connecting without a trusted proxy.
        // If the immediate peer is not trusted, never inspect forwarding headers.
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        // Security boundary 2: Immediate peer IS a trusted proxy.
        // Traverse X-Forwarded-For from right to left (most recent proxy to original client).
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String[] hops = xForwardedFor.split(",");
            for (int i = hops.length - 1; i >= 0; i--) {
                String candidate = normalizeIp(hops[i].trim());
                if (!isValidIp(candidate)) {
                    log.warn("Malformed IP [{}] encountered in X-Forwarded-For chain; skipping hop", hops[i].trim());
                    continue;
                }
                if (!isTrustedProxy(candidate)) {
                    // First non-trusted address from the right is the genuine client IP
                    return candidate;
                }
            }

            // If every valid hop was itself in the trusted proxy list, pick the leftmost valid hop
            for (String hop : hops) {
                String candidate = normalizeIp(hop.trim());
                if (isValidIp(candidate)) {
                    return candidate;
                }
            }
        }

        // Security boundary 3: Inspect X-Real-IP if X-Forwarded-For was not provided
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            String candidate = normalizeIp(xRealIp.trim());
            if (isValidIp(candidate) && !isTrustedProxy(candidate)) {
                return candidate;
            }
        }

        // Fall back to immediate remote address if no untrusted upstream client was discovered
        return remoteAddr;
    }

    /**
     * Checks if the given IP address matches any configured trusted proxy address or CIDR range.
     */
    public boolean isTrustedProxy(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }

        String normalizedCandidate = normalizeIp(ip);
        List<String> trustedProxies = rateLimitProperties.getTrustedProxies();
        if (trustedProxies == null || trustedProxies.isEmpty()) {
            return false;
        }

        for (String trusted : trustedProxies) {
            if (trusted == null || trusted.isBlank()) {
                continue;
            }
            String normalizedTrusted = normalizeIp(trusted.trim());
            if (normalizedTrusted.contains("/")) {
                // CIDR subnet evaluation
                if (matchesCidr(normalizedCandidate, normalizedTrusted)) {
                    return true;
                }
            } else if (normalizedTrusted.equalsIgnoreCase(normalizedCandidate)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Validates IP syntax without performing blocking DNS lookups.
     */
    public boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank() || ip.length() > 45) {
            return false;
        }
        if (IPV4_PATTERN.matcher(ip).matches()) {
            return true;
        }
        if (IPV6_PATTERN.matcher(ip).matches() && ip.contains(":")) {
            // Confirm it parses via InetAddress without hostname resolution
            try {
                InetAddress address = InetAddress.getByName(ip);
                return address.getHostAddress().equalsIgnoreCase(ip)
                        || normalizeIp(address.getHostAddress()).equalsIgnoreCase(normalizeIp(ip));
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Normalizes IPv6 loopback variants to standard IPv4 127.0.0.1 representation.
     */
    public String normalizeIp(String ip) {
        if (ip == null) {
            return null;
        }
        String trimmed = ip.trim();
        if ("0:0:0:0:0:0:0:1".equals(trimmed) || "::1".equals(trimmed) || "[::1]".equals(trimmed)) {
            return "127.0.0.1";
        }
        return trimmed;
    }

    /**
     * Evaluates whether an IP address belongs to a CIDR subnet (e.g., 10.0.0.0/8, 192.168.0.0/16).
     */
    private boolean matchesCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }
            InetAddress candidateAddr = InetAddress.getByName(ip);
            InetAddress subnetAddr = InetAddress.getByName(parts[0]);
            int prefixLength = Integer.parseInt(parts[1]);

            byte[] candidateBytes = candidateAddr.getAddress();
            byte[] subnetBytes = subnetAddr.getAddress();

            if (candidateBytes.length != subnetBytes.length) {
                return false;
            }

            int fullBytes = prefixLength / 8;
            for (int i = 0; i < fullBytes; i++) {
                if (candidateBytes[i] != subnetBytes[i]) {
                    return false;
                }
            }

            int remainingBits = prefixLength % 8;
            if (remainingBits > 0) {
                int mask = 0xFF << (8 - remainingBits);
                if ((candidateBytes[fullBytes] & mask) != (subnetBytes[fullBytes] & mask)) {
                    return false;
                }
            }

            return true;
        } catch (Exception ex) {
            log.debug("CIDR evaluation failed for candidate [{}] and subnet [{}]: {}", ip, cidr, ex.getMessage());
            return false;
        }
    }
}
