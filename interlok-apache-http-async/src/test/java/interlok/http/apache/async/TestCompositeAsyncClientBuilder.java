package interlok.http.apache.async;

import static org.junit.Assert.assertEquals;

import com.adaptris.core.http.apache.request.DateHeader;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.junit.Test;

public class TestCompositeAsyncClientBuilder {

  @Test
  public void testBuilder() throws Exception {
    CompositeAsyncClientBuilder builder = new CompositeAsyncClientBuilder().withBuilders(
        new DefaultAsyncClientBuilder(), new AsyncWithInterceptors().withInterceptors(new DateHeader()));
    HttpAsyncClientBuilder httpClientBuilder = HttpAsyncClientBuilder.create();
    assertEquals(httpClientBuilder, builder.configure(httpClientBuilder));
  }

}