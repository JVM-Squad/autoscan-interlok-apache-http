package com.adaptris.core.http.apache.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.http.HttpEntity;
import org.junit.Before;
import org.junit.Test;

import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.CoreException;
import com.adaptris.core.http.oauth.AccessToken;
import com.adaptris.core.util.LifecycleHelper;

public class SalesforceAccessTokenTest {

  @Before
  public void setUp() throws Exception {

  }

  @Test
  public void testLifecycle() throws Exception {
    SalesforceAccessToken tokenBuilder = new SalesforceAccessToken();
    try {
      LifecycleHelper.init(tokenBuilder);
      fail();
    }
    catch (CoreException expected) {

    }
    tokenBuilder.setUsername("test");
    try {
      LifecycleHelper.init(tokenBuilder);
      fail();
    }
    catch (CoreException expected) {

    }
    tokenBuilder.setPassword("test");
    try {
      LifecycleHelper.init(tokenBuilder);
      fail();
    }
    catch (CoreException expected) {

    }
    tokenBuilder.setConsumerKey("test");
    try {
      LifecycleHelper.init(tokenBuilder);
      fail();
    }
    catch (CoreException expected) {

    }
    tokenBuilder.setConsumerSecret("test");
    LifecycleHelper.stopAndClose(LifecycleHelper.initAndStart(tokenBuilder));
  }

  @Test
  public void testConsumerKey() {
    SalesforceAccessToken tokenBuilder = new SalesforceAccessToken();
    assertNull(tokenBuilder.getConsumerKey());
    tokenBuilder.setConsumerKey("test");
    assertEquals("test", tokenBuilder.getConsumerKey());
    try {
      tokenBuilder.setConsumerKey(null);
      fail();
    }
    catch (IllegalArgumentException e) {

    }
    assertEquals("test", tokenBuilder.getConsumerKey());
  }

  @Test
  public void testConsumerSecret() {
    SalesforceAccessToken tokenBuilder = new SalesforceAccessToken();
    assertNull(tokenBuilder.getConsumerSecret());
    tokenBuilder.setConsumerSecret("test");
    assertEquals("test", tokenBuilder.getConsumerSecret());
    try {
      tokenBuilder.setConsumerSecret(null);
      fail();
    }
    catch (IllegalArgumentException e) {

    }
    assertEquals("test", tokenBuilder.getConsumerSecret());
  }

  @Test
  public void testUsername() {
    SalesforceAccessToken tokenBuilder = new SalesforceAccessToken();
    assertNull(tokenBuilder.getUsername());
    tokenBuilder.setUsername("test");
    assertEquals("test", tokenBuilder.getUsername());
    try {
      tokenBuilder.setUsername(null);
      fail();
    }
    catch (IllegalArgumentException e) {

    }
    assertEquals("test", tokenBuilder.getUsername());
  }

  @Test
  public void testPassword() {
    SalesforceAccessToken tokenBuilder = new SalesforceAccessToken();
    assertNull(tokenBuilder.getPassword());
    tokenBuilder.setPassword("test");
    assertEquals("test", tokenBuilder.getPassword());
    try {
      tokenBuilder.setPassword(null);
      fail();
    }
    catch (IllegalArgumentException e) {

    }
    assertEquals("test", tokenBuilder.getPassword());
  }

  @Test
  public void testProxy() {
    SalesforceAccessToken tokenBuilder = new SalesforceAccessToken();
    assertNull(tokenBuilder.getHttpProxy());
    tokenBuilder.setHttpProxy("test");
    assertEquals("test", tokenBuilder.getHttpProxy());
    tokenBuilder.setHttpProxy(null);
    assertNull(tokenBuilder.getHttpProxy());
  }

  @Test
  public void testTokenUrl() {
    SalesforceAccessToken tokenBuilder = new SalesforceAccessToken();
    assertNull(tokenBuilder.getTokenUrl());
    assertEquals(SalesforceAccessToken.DEFAULT_TOKEN_URL, tokenBuilder.tokenUrl());
    tokenBuilder.setTokenUrl("test");
    assertEquals("test", tokenBuilder.getTokenUrl());
    assertEquals("test", tokenBuilder.tokenUrl());
    tokenBuilder.setTokenUrl(null);
    assertEquals(SalesforceAccessToken.DEFAULT_TOKEN_URL, tokenBuilder.tokenUrl());
  }

  @Test
  public void testBuildToken() throws Exception {
    final SalesforceLoginWorker worker = mock(SalesforceLoginWorker.class);
    SalesforceAccessToken tokenBuilder = new SalesforceAccessToken() {
      SalesforceLoginWorker createWorker() {
        return worker;
      }
    };
    tokenBuilder.setUsername("test");
    tokenBuilder.setPassword("test");
    tokenBuilder.setConsumerKey("test");
    tokenBuilder.setConsumerSecret("test");
    AccessToken myAccessToken = new AccessToken("Bearer", "token", -1);
    when(worker.login((HttpEntity) anyObject())).thenReturn(myAccessToken);
    try {
      LifecycleHelper.initAndStart(tokenBuilder);
      AccessToken token = tokenBuilder.build(AdaptrisMessageFactory.getDefaultInstance().newMessage());
      assertEquals("token", token.getToken());
    } finally {
      LifecycleHelper.stopAndClose(tokenBuilder);
    }
  }

  @Test
  public void testBuildToken_PasswordException() throws Exception {
    final SalesforceLoginWorker worker = mock(SalesforceLoginWorker.class);
    SalesforceAccessToken tokenBuilder = new SalesforceAccessToken() {
      SalesforceLoginWorker createWorker() {
        return worker;
      }
    };
    tokenBuilder.setUsername("test");
    tokenBuilder.setPassword("PW:test");
    tokenBuilder.setConsumerKey("test");
    tokenBuilder.setConsumerSecret("test");
    AccessToken myAccessToken = new AccessToken("Bearer", "token", -1);
    when(worker.login((HttpEntity) anyObject())).thenReturn(myAccessToken);
    try {
      LifecycleHelper.initAndStart(tokenBuilder);
      AccessToken token = tokenBuilder.build(AdaptrisMessageFactory.getDefaultInstance().newMessage());
      fail();
    }
    catch (CoreException e) {

    }
    finally {
      LifecycleHelper.stopAndClose(tokenBuilder);
    }
  }
}
