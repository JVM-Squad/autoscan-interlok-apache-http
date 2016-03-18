package com.adaptris.core.http.apache;

import static com.adaptris.core.http.apache.JettyHelper.JETTY_USER_REALM;
import static com.adaptris.core.http.apache.JettyHelper.METADATA_KEY_CONTENT_TYPE;
import static com.adaptris.core.http.apache.JettyHelper.TEXT;
import static com.adaptris.core.http.apache.JettyHelper.URL_TO_POST_TO;
import static com.adaptris.core.http.apache.JettyHelper.createAndStartChannel;
import static com.adaptris.core.http.apache.JettyHelper.createChannel;
import static com.adaptris.core.http.apache.JettyHelper.createConnection;
import static com.adaptris.core.http.apache.JettyHelper.createConsumer;
import static com.adaptris.core.http.apache.JettyHelper.createProduceDestination;
import static com.adaptris.core.http.apache.JettyHelper.createWorkflow;
import static com.adaptris.core.http.apache.JettyHelper.stopAndRelease;

import java.util.Arrays;
import java.util.Map;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.Channel;
import com.adaptris.core.ConfiguredProduceDestination;
import com.adaptris.core.CoreConstants;
import com.adaptris.core.DefaultMessageFactory;
import com.adaptris.core.MetadataElement;
import com.adaptris.core.PortManager;
import com.adaptris.core.ProducerCase;
import com.adaptris.core.ServiceException;
import com.adaptris.core.ServiceList;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.StandaloneRequestor;
import com.adaptris.core.http.auth.AdapterResourceAuthenticator;
import com.adaptris.core.http.client.ConfiguredRequestMethodProvider;
import com.adaptris.core.http.client.RequestMethodProvider.RequestMethod;
import com.adaptris.core.http.jetty.HashUserRealmProxy;
import com.adaptris.core.http.jetty.HttpConnection;
import com.adaptris.core.http.jetty.MessageConsumer;
import com.adaptris.core.http.jetty.ResponseProducer;
import com.adaptris.core.http.jetty.SecurityConstraint;
import com.adaptris.core.http.server.HttpStatusProvider.HttpStatus;
import com.adaptris.core.metadata.NoOpMetadataFilter;
import com.adaptris.core.metadata.RemoveAllMetadataFilter;
import com.adaptris.core.services.metadata.AddMetadataService;
import com.adaptris.core.stubs.MockMessageProducer;

public class ApacheHttpProducerTest extends ProducerCase {


  public ApacheHttpProducerTest(String name) {
    super(name);
  }

  @Override
  protected void setUp() throws Exception {
  }

  public void testSetHandleRedirection() throws Exception {
    ApacheHttpProducer p = new ApacheHttpProducer();
    assertFalse(p.handleRedirection());
    p.setAllowRedirect(true);
    assertNotNull(p.getAllowRedirect());
    assertEquals(Boolean.TRUE, p.getAllowRedirect());
    assertTrue(p.handleRedirection());
    p.setAllowRedirect(false);
    assertNotNull(p.getAllowRedirect());
    assertEquals(Boolean.FALSE, p.getAllowRedirect());
    assertFalse(p.handleRedirection());
  }

  public void testSetIgnoreServerResponse() throws Exception {
    ApacheHttpProducer p = new ApacheHttpProducer();
    assertFalse(p.ignoreServerResponseCode());
    p.setIgnoreServerResponseCode(true);
    assertNotNull(p.getIgnoreServerResponseCode());
    assertEquals(Boolean.TRUE, p.getIgnoreServerResponseCode());
    assertTrue(p.ignoreServerResponseCode());
    p.setIgnoreServerResponseCode(false);
    assertNotNull(p.getIgnoreServerResponseCode());
    assertEquals(Boolean.FALSE, p.getIgnoreServerResponseCode());
    assertFalse(p.ignoreServerResponseCode());
  }


  public void testSetRequestHandler() throws Exception {
    ApacheHttpProducer p = new ApacheHttpProducer();
    assertEquals(NoOpRequestHeaders.class, p.getRequestHeaderProvider().getClass());
    try {
      p.setRequestHeaderProvider(null);
      fail();
    } catch (IllegalArgumentException expected) {

    }
    assertEquals(NoOpRequestHeaders.class, p.getRequestHeaderProvider().getClass());
    p.setRequestHeaderProvider(new MetadataRequestHeaders(new RemoveAllMetadataFilter()));
    assertEquals(MetadataRequestHeaders.class, p.getRequestHeaderProvider().getClass());
  }


  public void testSetResponseHandler() throws Exception {
    ApacheHttpProducer p = new ApacheHttpProducer();
    assertEquals(DiscardResponseHeaders.class, p.getResponseHeaderHandler().getClass());
    try {
      p.setResponseHeaderHandler(null);
      fail();
    } catch (IllegalArgumentException expected) {

    }
    assertEquals(DiscardResponseHeaders.class, p.getResponseHeaderHandler().getClass());
    p.setResponseHeaderHandler(new ResponseHeadersAsMetadata());
    assertEquals(ResponseHeadersAsMetadata.class, p.getResponseHeaderHandler().getClass());
  }

  @SuppressWarnings("deprecation")
  public void testProduce_LegacyContentType() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    http.setContentTypeProvider(new StaticContentTypeProvider());
    StandaloneProducer producer = new StandaloneProducer(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");
    try {
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
      stop(producer);
    }
    doAssertions(mock, true);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertFalse(m2.containsKey(METADATA_KEY_CONTENT_TYPE));
    assertEquals("text/plain", m2.getMetadataValue("Content-Type"));
  }


  public void testProduce() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    StandaloneProducer producer = new StandaloneProducer(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");
    try {
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
      stop(producer);
    }
    doAssertions(mock, true);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertFalse(m2.containsKey(METADATA_KEY_CONTENT_TYPE));
    assertEquals("text/plain", m2.getMetadataValue("Content-Type"));
  }

  @SuppressWarnings("deprecation")
  public void testProduce_LegacyMethod() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();

    Channel c = createAndStartChannel(mock);
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    http.setMethod(ApacheHttpProducer.HttpMethod.POST);
    StandaloneProducer producer = new StandaloneProducer(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");
    try {
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
      stop(producer);
    }
    doAssertions(mock, true);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertFalse(m2.containsKey(METADATA_KEY_CONTENT_TYPE));
    assertEquals("text/plain", m2.getMetadataValue("Content-Type"));
  }


  public void testProduce_WithMetadata() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    http.setRequestHeaderProvider(new MetadataRequestHeaders(new NoOpMetadataFilter()));
    StandaloneProducer producer = new StandaloneProducer(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");
    try {
      c.requestStart();
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
      stop(producer);
    }
    doAssertions(mock, true);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertTrue(m2.containsKey(METADATA_KEY_CONTENT_TYPE));
    assertEquals("text/plain", m2.getMetadataValue("Content-Type"));
  }

  public void testProduce_ReplyHasCharset() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    MessageConsumer mc = createConsumer(URL_TO_POST_TO);

    ServiceList sl = new ServiceList();
    sl.add(new AddMetadataService(Arrays.asList(new MetadataElement(getName(), "text/plain; charset=UTF-8"))));
    ResponseProducer responder = new ResponseProducer(HttpStatus.OK_200);
    responder.setContentTypeKey(getName());
    sl.add(new StandaloneProducer(responder));
    Channel c = createChannel(jc, createWorkflow(mc, mock, sl));

    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    http.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.POST));
    StandaloneRequestor producer = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
      stop(producer);
    }
    assertEquals("UTF-8", msg.getCharEncoding());
  }


  public void testProduce_ReplyHasNoData() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    MessageConsumer mc = createConsumer(URL_TO_POST_TO);

    ServiceList sl = new ServiceList();
    ResponseProducer responder = new ResponseProducer(HttpStatus.OK_200);
    responder.setSendPayload(false);
    sl.add(new StandaloneProducer(responder));
    Channel c = createChannel(jc, createWorkflow(mc, mock, sl));

    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    http.setAllowRedirect(true);
    http.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.POST));
    StandaloneRequestor producer = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
      stop(producer);
    }
    doAssertions(mock, true);
    assertEquals(0, msg.getSize());
  }

  @SuppressWarnings("deprecation")
  public void testProduce_WithContentTypeMetadata_Legacy() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    http.setContentTypeProvider(new MetadataContentTypeProvider(METADATA_KEY_CONTENT_TYPE));
    StandaloneProducer producer = new StandaloneProducer(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");
    try {
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    }
    finally {
      stopAndRelease(c);
      stop(producer);
    }
    doAssertions(mock, true);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertTrue(m2.containsKey("Content-Type"));
    assertFalse(m2.containsKey(METADATA_KEY_CONTENT_TYPE));
    assertEquals("text/complicated", m2.getMetadataValue("Content-Type"));
  }

  public void testProduce_WithContentTypeMetadata() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    http.setContentTypeProvider(new com.adaptris.core.http.MetadataContentTypeProvider(METADATA_KEY_CONTENT_TYPE));
    StandaloneProducer producer = new StandaloneProducer(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");
    try {
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
      stop(producer);
    }
    doAssertions(mock, true);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertTrue(m2.containsKey("Content-Type"));
    assertFalse(m2.containsKey(METADATA_KEY_CONTENT_TYPE));
    assertEquals("text/complicated", m2.getMetadataValue("Content-Type"));
  }

  public void testRequest_GetMethod_ZeroBytes() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    http.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.GET));
    StandaloneRequestor producer = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage();
    try {
      start(c);
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    }
    finally {
      stopAndRelease(c);
      stop(producer);
    }
    doAssertions(mock, false);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("GET", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals(0, m2.getSize());
  }

  public void testRequest_GetMethod_NonZeroBytes() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    http.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.GET));
    StandaloneRequestor producer = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    }
    finally {
      stopAndRelease(c);
      stop(producer);
    }
    doAssertions(mock, false);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("GET", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals(0, m2.getSize());
  }

  public void testRequest_ProduceException_401() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    MessageConsumer mc = createConsumer(URL_TO_POST_TO);
    ServiceList services = new ServiceList();
    services.add(new StandaloneProducer(new ResponseProducer(HttpStatus.UNAUTHORIZED_401)));
    Channel c = createChannel(jc, createWorkflow(mc, mock, services));
    StandaloneRequestor producer = new StandaloneRequestor(new ApacheHttpProducer(createProduceDestination(c)));
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      start(producer);
      producer.doService(msg);
      fail();
    } catch (ServiceException expected) {

    }
    finally {
      stopAndRelease(c);
      stop(producer);
    }
  }

  public void testRequest_WithErrorResponse() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    jc.setSendServerVersion(true);
    MessageConsumer mc = createConsumer(URL_TO_POST_TO);
    ServiceList services = new ServiceList();
    services.add(new StandaloneProducer(new ResponseProducer(HttpStatus.UNAUTHORIZED_401)));
    Channel c = createChannel(jc, createWorkflow(mc, mock, services));
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    http.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.POST));
    http.setIgnoreServerResponseCode(true);
    http.setResponseHeaderHandler(new ResponseHeadersAsMetadata("HTTP_"));
    StandaloneRequestor producer = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      start(producer);
      producer.doService(msg); // msg will now contain the response!
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
      stop(producer);
    }
    doAssertions(mock, false);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("POST", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals("401", msg.getMetadataValue(CoreConstants.HTTP_PRODUCER_RESPONSE_CODE));
    assertNotNull(msg.getMetadata("HTTP_Server"));
  }


  public void testProduce_WithUsernamePassword() throws Exception {
    String threadName = Thread.currentThread().getName();
    Thread.currentThread().setName(getName());
    HashUserRealmProxy securityWrapper = new HashUserRealmProxy();
    securityWrapper.setFilename(PROPERTIES.getProperty(JETTY_USER_REALM));

    SecurityConstraint securityConstraint = new SecurityConstraint();
    securityConstraint.setMustAuthenticate(true);
    securityConstraint.setRoles("user");

    securityWrapper.setSecurityConstraints(Arrays.asList(securityConstraint));

    HttpConnection connection = createConnection(securityWrapper);
    MockMessageProducer mockProducer = new MockMessageProducer();
    MessageConsumer consumer = JettyHelper.createConsumer(URL_TO_POST_TO);
    Channel c = JettyHelper.createChannel(connection, consumer, mockProducer);
    ApacheHttpProducer httpProducer = new ApacheHttpProducer(createProduceDestination(c));
    try {
      c.requestStart();
      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(TEXT);

      httpProducer.setUserName("user");
      httpProducer.setPassword("password");
      StandaloneProducer producer = new StandaloneProducer(httpProducer);
      start(producer);
      producer.doService(msg);
      doAssertions(mockProducer, true);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      stop(httpProducer);
      stopAndRelease(c);
      Thread.currentThread().setName(threadName);
      assertEquals(0, AdapterResourceAuthenticator.getInstance().currentAuthenticators().size());
    }
  }


  public void testProduce_WithUsernamePassword_BadCredentials() throws Exception {
    String threadName = Thread.currentThread().getName();
    Thread.currentThread().setName(getName());
    HashUserRealmProxy securityWrapper = new HashUserRealmProxy();
    securityWrapper.setFilename(PROPERTIES.getProperty(JETTY_USER_REALM));

    SecurityConstraint securityConstraint = new SecurityConstraint();
    securityConstraint.setMustAuthenticate(true);
    securityConstraint.setRoles("user");

    securityWrapper.setSecurityConstraints(Arrays.asList(securityConstraint));

    HttpConnection connection = createConnection(securityWrapper);
    MockMessageProducer mockProducer = new MockMessageProducer();
    MessageConsumer consumer = JettyHelper.createConsumer(URL_TO_POST_TO);
    Channel c = JettyHelper.createChannel(connection, consumer, mockProducer);
    ApacheHttpProducer httpProducer = new ApacheHttpProducer(createProduceDestination(c));
    try {
      c.requestStart();
      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(TEXT);

      httpProducer.setUserName(getName());
      httpProducer.setPassword(getName());
      StandaloneProducer producer = new StandaloneProducer(httpProducer);
      start(producer);
      producer.doService(msg);
      fail();
    } catch (ServiceException expected) {

    } finally {
      stop(httpProducer);
      stopAndRelease(c);
      Thread.currentThread().setName(threadName);
      PortManager.release(connection.getPort());
      assertEquals(0, AdapterResourceAuthenticator.getInstance().currentAuthenticators().size());
    }
  }

  public void testRequest_WithReplyAsMetadata() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    MessageConsumer mc = createConsumer(URL_TO_POST_TO);

    ServiceList sl = new ServiceList();
    sl.add(new AddMetadataService(Arrays.asList(new MetadataElement(getName(), "text/plain; charset=UTF-8"))));
    ResponseProducer responder = new ResponseProducer(HttpStatus.OK_200);
    responder.setContentTypeKey(getName());
    sl.add(new StandaloneProducer(responder));
    Channel c = createChannel(jc, createWorkflow(mc, mock, sl));

    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    http.setResponseHandlerFactory(new MetadataResponseHandlerFactory(getName()));
    StandaloneRequestor requestor = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      start(requestor);
      requestor.doService(msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
      stop(requestor);
    }
    doAssertions(mock, false);

    assertTrue(msg.headersContainsKey(getName()));
    assertEquals(TEXT, msg.getMetadataValue(getName()));
  }


  @Override
  protected Object retrieveObjectForSampleConfig() {
    ApacheHttpProducer producer = new ApacheHttpProducer(new ConfiguredProduceDestination("http://myhost.com/url/to/post/to"));
    producer.setUserName("username");
    producer.setPassword("password");
    StandaloneProducer result = new StandaloneProducer(producer);
    return result;
  }

  protected AdaptrisMessage doAssertions(MockMessageProducer mockProducer, boolean assertPayload) {
    assertEquals(1, mockProducer.getMessages().size());
    AdaptrisMessage msg = mockProducer.getMessages().get(0);
    if (assertPayload) {
      assertEquals(TEXT, msg.getStringPayload());
    }
    assertTrue(msg.containsKey(CoreConstants.JETTY_URI));
    assertEquals(URL_TO_POST_TO, msg.getMetadataValue(CoreConstants.JETTY_URI));
    assertTrue(msg.containsKey(CoreConstants.JETTY_URL));
    Map objMetadata = msg.getObjectMetadata();
    assertNotNull(objMetadata.get(CoreConstants.JETTY_REQUEST_KEY));
    assertNotNull(objMetadata.get(CoreConstants.JETTY_RESPONSE_KEY));
    return msg;
  }

}
