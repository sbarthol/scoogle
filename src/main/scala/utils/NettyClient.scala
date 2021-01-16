package utils

import com.ning.http.client.{AsyncHttpClient, AsyncHttpClientConfig}

import java.util.concurrent.Executors

object NettyClient {

  lazy val client = new AsyncHttpClient(
    new AsyncHttpClientConfig.Builder()
      .setFollowRedirect(true)
      // Todo: Try to use the same executor as the akka dispatcher
      .setExecutorService(Executors.newCachedThreadPool())
      .build()
  )
}
