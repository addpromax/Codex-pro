package net.momirealms.craftengine.core.pack.host;

import net.momirealms.craftengine.core.plugin.locale.LocalizedException;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.util.Map;

public interface ResourcePackHostFactory {

    ResourcePackHost create(Map<String, Object> arguments);

    default ProxySelector getProxySelector(Map<String, Object> proxySetting) {
        ProxySelector proxy = ProxySelector.getDefault();
        if (proxySetting != null) {
            Object hostObj = proxySetting.get("host");
            if (hostObj == null) {
                throw new LocalizedException("warning.config.host.proxy.missing_host", new NullPointerException("'host' should not be null for proxy setting"));
            }
            String proxyHost = hostObj.toString();
            Object portObj = proxySetting.get("port");
            if (portObj == null) {
                throw new LocalizedException("warning.config.host.proxy.missing_port", new NullPointerException("'port' should not be null for proxy setting"));
            }
            int proxyPort = ResourceConfigUtils.getAsInt(portObj, "port");
            if (proxyHost == null || proxyHost.isEmpty() || proxyPort <= 0 || proxyPort > 65535) {
                throw new LocalizedException("warning.config.host.proxy.invalid", proxyHost + ":" + proxyPort);
            } else {
                proxy = ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort));
            }
        }
        return proxy;
    }
}
