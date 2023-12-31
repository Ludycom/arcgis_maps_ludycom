// Autogenerated from Pigeon (v12.0.1), do not edit directly.
// See also: https://pub.dev/packages/pigeon

package AuthPigeon

import android.util.Log
import io.flutter.plugin.common.BasicMessageChannel
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MessageCodec
import io.flutter.plugin.common.StandardMessageCodec
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

private fun wrapResult(result: Any?): List<Any?> {
  return listOf(result)
}

private fun wrapError(exception: Throwable): List<Any?> {
  if (exception is FlutterError) {
    return listOf(
      exception.code,
      exception.message,
      exception.details
    )
  } else {
    return listOf(
      exception.javaClass.simpleName,
      exception.toString(),
      "Cause: " + exception.cause + ", Stacktrace: " + Log.getStackTraceString(exception)
    )
  }
}

/**
 * Error class for passing custom error details to Flutter via a thrown PlatformException.
 * @property code The error code.
 * @property message The error message.
 * @property details The error details. Must be a datatype supported by the api codec.
 */
class FlutterError (
  val code: String,
  override val message: String? = null,
  val details: Any? = null
) : Throwable()

/** Generated class from Pigeon that represents data sent in messages. */
data class OAuthUserConfigurations (
  val portalUrl: String,
  val clientId: String,
  val redirectUrl: String

) {
  companion object {
    @Suppress("UNCHECKED_CAST")
    fun fromList(list: List<Any?>): OAuthUserConfigurations {
      val portalUrl = list[0] as String
      val clientId = list[1] as String
      val redirectUrl = list[2] as String
      return OAuthUserConfigurations(portalUrl, clientId, redirectUrl)
    }
  }
  fun toList(): List<Any?> {
    return listOf<Any?>(
      portalUrl,
      clientId,
      redirectUrl,
    )
  }
}
/** Generated class from Pigeon that represents Flutter messages that can be called from Kotlin. */
@Suppress("UNCHECKED_CAST")
class AGMLAuthApiHandler(private val binaryMessenger: BinaryMessenger) {
  companion object {
    /** The codec used by AGMLAuthApiHandler. */
    val codec: MessageCodec<Any?> by lazy {
      StandardMessageCodec()
    }
  }
  fun oAuthUserState(stateArg: Boolean, callback: (Result<Unit>) -> Unit) {
    val channel = BasicMessageChannel<Any?>(binaryMessenger, "dev.flutter.pigeon.AuthPigeon.AGMLAuthApiHandler.oAuthUserState", codec)
    channel.send(listOf(stateArg)) {
      if (it is List<*>) {
        if (it.size > 1) {
          callback(Result.failure(FlutterError(it[0] as String, it[1] as String, it[2] as String?)));
        } else {
          callback(Result.success(Unit));
        }
      } else {
        callback(Result.failure(FlutterError("channel-error",  "Unable to establish connection on channel.", "")));
      } 
    }
  }
}
@Suppress("UNCHECKED_CAST")
private object AGMLAuthApiCodec : StandardMessageCodec() {
  override fun readValueOfType(type: Byte, buffer: ByteBuffer): Any? {
    return when (type) {
      128.toByte() -> {
        return (readValue(buffer) as? List<Any?>)?.let {
          OAuthUserConfigurations.fromList(it)
        }
      }
      else -> super.readValueOfType(type, buffer)
    }
  }
  override fun writeValue(stream: ByteArrayOutputStream, value: Any?)   {
    when (value) {
      is OAuthUserConfigurations -> {
        stream.write(128)
        writeValue(stream, value.toList())
      }
      else -> super.writeValue(stream, value)
    }
  }
}

/** Generated interface from Pigeon that represents a handler of messages from Flutter. */
interface AGMLAuthApi {
  fun oAuthUser(portalConfig: OAuthUserConfigurations, username: String, password: String)
  fun setApiKey(apiKey: String)

  companion object {
    /** The codec used by AGMLAuthApi. */
    val codec: MessageCodec<Any?> by lazy {
      AGMLAuthApiCodec
    }
    /** Sets up an instance of `AGMLAuthApi` to handle messages through the `binaryMessenger`. */
    @Suppress("UNCHECKED_CAST")
    fun setUp(binaryMessenger: BinaryMessenger, api: AGMLAuthApi?) {
      run {
        val channel = BasicMessageChannel<Any?>(binaryMessenger, "dev.flutter.pigeon.AuthPigeon.AGMLAuthApi.oAuthUser", codec)
        if (api != null) {
          channel.setMessageHandler { message, reply ->
            val args = message as List<Any?>
            val portalConfigArg = args[0] as OAuthUserConfigurations
            val usernameArg = args[1] as String
            val passwordArg = args[2] as String
            var wrapped: List<Any?>
            try {
              api.oAuthUser(portalConfigArg, usernameArg, passwordArg)
              wrapped = listOf<Any?>(null)
            } catch (exception: Throwable) {
              wrapped = wrapError(exception)
            }
            reply.reply(wrapped)
          }
        } else {
          channel.setMessageHandler(null)
        }
      }
      run {
        val channel = BasicMessageChannel<Any?>(binaryMessenger, "dev.flutter.pigeon.AuthPigeon.AGMLAuthApi.setApiKey", codec)
        if (api != null) {
          channel.setMessageHandler { message, reply ->
            val args = message as List<Any?>
            val apiKeyArg = args[0] as String
            var wrapped: List<Any?>
            try {
              api.setApiKey(apiKeyArg)
              wrapped = listOf<Any?>(null)
            } catch (exception: Throwable) {
              wrapped = wrapError(exception)
            }
            reply.reply(wrapped)
          }
        } else {
          channel.setMessageHandler(null)
        }
      }
    }
  }
}
