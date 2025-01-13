
package com.alipay.antchain.bridge.relayer.core.types.network.ws.client;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

import com.alipay.antchain.bridge.relayer.core.types.network.ws.client.generated.WSRelayerServerAPImpl;

/**
 * THIS IS NOT A GENERATED CLASS!!!
 * <p>
 * Add some features to {@link com.alipay.antchain.bridge.relayer.core.types.network.ws.client.generated.WSRelayerServerAPImplService}
 * for supporting define the host to connect,
 * and we get this class {@code WSRelayerServerAPImplServiceWithHost}
 * </p>
 */
@WebServiceClient(name = "WSRelayerServerAPImplService",
        targetNamespace = "http://ws.offchainapi.oracle.mychain.alipay.com/",
        wsdlLocation = "http://127.0.0.1:8082/WSEndpointServer?wsdl")
public class WSRelayerServerAPImplServiceWithHost
        extends Service {

    private final static URL WSRELAYERSERVERAPIMPLSERVICE_WSDL_LOCATION;
    private final static WebServiceException WSRELAYERSERVERAPIMPLSERVICE_EXCEPTION;
    private final static QName WSRELAYERSERVERAPIMPLSERVICE_QNAME = new QName(
            "http://ws.offchainapi.oracle.mychain.alipay.com/", "WSRelayerServerAPImplService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://127.0.0.1:8082/WSEndpointServer?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        WSRELAYERSERVERAPIMPLSERVICE_WSDL_LOCATION = url;
        WSRELAYERSERVERAPIMPLSERVICE_EXCEPTION = e;
    }

    public WSRelayerServerAPImplServiceWithHost() {
        super(__getWsdlLocation(), WSRELAYERSERVERAPIMPLSERVICE_QNAME);
    }

    public WSRelayerServerAPImplServiceWithHost(WebServiceFeature... features) {
        super(__getWsdlLocation(), WSRELAYERSERVERAPIMPLSERVICE_QNAME, features);
    }

    public WSRelayerServerAPImplServiceWithHost(String host) {

        super(__getWsdlLocation(host), WSRELAYERSERVERAPIMPLSERVICE_QNAME);
    }

    public WSRelayerServerAPImplServiceWithHost(URL wsdlLocation) {
        super(wsdlLocation, WSRELAYERSERVERAPIMPLSERVICE_QNAME);
    }

    public WSRelayerServerAPImplServiceWithHost(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, WSRELAYERSERVERAPIMPLSERVICE_QNAME, features);
    }

    public WSRelayerServerAPImplServiceWithHost(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public WSRelayerServerAPImplServiceWithHost(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * @return returns WSRelayerServerAPImpl
     */
    @WebEndpoint(name = "WSRelayerServerAPImplPort")
    public WSRelayerServerAPImpl getWSRelayerServerAPImplPort() {
        return super.getPort(new QName("http://ws.offchainapi.oracle.mychain.alipay.com/", "WSRelayerServerAPImplPort"),
                WSRelayerServerAPImpl.class);
    }

    /**
     * @param features A list of {@link WebServiceFeature} to configure on the proxy.  Supported features not in the
     *                 <code>features</code> parameter will have their default values.
     * @return returns WSRelayerServerAPImpl
     */
    @WebEndpoint(name = "WSRelayerServerAPImplPort")
    public WSRelayerServerAPImpl getWSRelayerServerAPImplPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://ws.offchainapi.oracle.mychain.alipay.com/", "WSRelayerServerAPImplPort"),
                WSRelayerServerAPImpl.class, features);
    }

    private static URL __getWsdlLocation() {
        if (WSRELAYERSERVERAPIMPLSERVICE_EXCEPTION != null) {
            throw WSRELAYERSERVERAPIMPLSERVICE_EXCEPTION;
        }
        return WSRELAYERSERVERAPIMPLSERVICE_WSDL_LOCATION;
    }

    private static URL __getWsdlLocation(String host) {
        try {
            return new URL(host + "/WSEndpointServer?wsdl");
        } catch (MalformedURLException e) {
            throw new WebServiceException(e);
        }
    }

}
