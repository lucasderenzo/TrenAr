package ar.trenar.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlin.coroutines.cancellation.CancellationException

class FeedbackResultTest {

    @Test
    fun `2xx es Success`() {
        assertEquals(FeedbackResult.Success, FeedbackResult.fromHttpCode(200))
        assertEquals(FeedbackResult.Success, FeedbackResult.fromHttpCode(201))
    }

    @Test
    fun `429 es RateLimited`() {
        assertEquals(FeedbackResult.RateLimited, FeedbackResult.fromHttpCode(429))
    }

    @Test
    fun `5xx y otros 4xx son Server`() {
        listOf(400, 401, 403, 404, 500, 502, 503).forEach {
            assertEquals("HTTP $it", FeedbackResult.Server, FeedbackResult.fromHttpCode(it))
        }
    }

    @Test
    fun `excepciones de red son Network`() {
        listOf(
            UnknownHostException("x"),
            ConnectException("x"),
            SocketTimeoutException("x"),
            SSLHandshakeException("x"),
            java.io.IOException("x"),
        ).forEach {
            assertEquals(it::class.simpleName, FeedbackResult.Network, FeedbackResult.fromThrowable(it))
        }
    }

    @Test
    fun `errores inesperados son Server`() {
        assertEquals(FeedbackResult.Server, FeedbackResult.fromThrowable(IllegalStateException("x")))
    }

    @Test(expected = CancellationException::class)
    fun `la cancelacion de la corrutina se propaga`() {
        FeedbackResult.fromThrowable(CancellationException("x"))
    }
}
