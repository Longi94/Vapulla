package `in`.dragonbra.vapulla.retrofit

import android.os.Handler
import android.os.Looper
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.ByteArrayInputStream

class ImageRequestBody(private val content: ByteArray, private val callback: ((Int, Int) -> Unit)? = null) : RequestBody() {

    companion object {
        const val BUFFER_SIZE = 2048
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun contentLength(): Long = content.size.toLong()

    override fun contentType() = MediaType.parse("image/*")

    override fun writeTo(sink: BufferedSink) {
        val buffer = ByteArray(BUFFER_SIZE)
        val bais = ByteArrayInputStream(content)
        var sent = 0

        while (bais.available() > 0) {
            val read = bais.read(buffer)
            sink.write(buffer, 0, read)

            sent += read
            handler.post({ callback?.invoke(content.size, sent) })
        }
    }
}