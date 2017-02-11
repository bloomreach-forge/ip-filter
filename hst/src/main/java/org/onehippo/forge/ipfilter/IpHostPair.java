package org.onehippo.forge.ipfilter;

public class IpHostPair {

    private final String ip;

    private final String host;

    public IpHostPair(final String ip, final String host) {
        this.ip = ip;
        this.host = host;
    }

    public String getIp() {
        return ip;
    }

    public String getHost() {
        return host;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final IpHostPair that = (IpHostPair) o;

        if (!ip.equals(that.ip)) {
            return false;
        }
        return host.equals(that.host);
    }

    @Override
    public int hashCode() {
        int result = ip.hashCode();
        result = 31 * result + host.hashCode();
        return result;
    }
}
