package client

import com.ning.http.client.{AsyncHttpClient, AsyncHttpClientConfig}

import java.util.concurrent.Executors

object NettyClient {

  val client = new AsyncHttpClient(
    new AsyncHttpClientConfig.Builder()
      .setFollowRedirect(true)
      .setExecutorService(Executors.newCachedThreadPool()) // Todo: Try to use the same executor as the akka dispatcher
      .build()
  )
}
