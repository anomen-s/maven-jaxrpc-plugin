package net.sf.jaxrpcmaven.jaxrpc;

public class ProxyConfiguration {

	public static final Integer DEFAULT_PORT = new Integer(8080);

	private String host;

	private Integer port;

    public String getHost()
    {
	return host;
    }
    
    public Integer getPort()
    {
	return ((port == null) ? DEFAULT_PORT : port);
    }

}
