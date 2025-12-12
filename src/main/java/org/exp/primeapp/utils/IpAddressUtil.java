package org.exp.primeapp.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class IpAddressUtil {
    
    public String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        if (ip != null && ip.contains(",")) {
            // If multiple IPs, try to find IPv4 first
            String[] ips = ip.split(",");
            for (String candidateIp : ips) {
                candidateIp = candidateIp.trim();
                if (isIPv4(candidateIp)) {
                    ip = candidateIp;
                    break;
                }
            }
            // If no IPv4 found, use first one
            if (!isIPv4(ip)) {
                ip = ips[0].trim();
            }
        }
        
        // Convert IPv6 localhost to IPv4
        if (ip != null && (ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1"))) {
            ip = "127.0.0.1";
        }
        
        // If still IPv6 and we want only IPv4, try to get from RemoteAddr
        if (ip != null && !isIPv4(ip)) {
            String remoteAddr = request.getRemoteAddr();
            if (remoteAddr != null && isIPv4(remoteAddr)) {
                ip = remoteAddr;
            }
        }
        
        return ip != null ? ip : "unknown";
    }
    
    private boolean isIPv4(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        // Simple IPv4 check: contains dots and no colons
        return ip.contains(".") && !ip.contains(":");
    }
    
    public String getBrowserInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "unknown";
    }
    
    public String getClientIpAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return getClientIpAddress(request);
        }
        return "unknown";
    }
    
    public String getBrowserInfo() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return getBrowserInfo(request);
        }
        return "unknown";
    }
}


