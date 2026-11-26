package com.miguelaetxio.mia2f.ui.keytransfer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/**
 * QR payload for passing the Gemini key between the family's two
 * phones. The prefix makes sure a random QR is never saved as a key.
 * ---
 * Contenido del QR para pasar la clave de Gemini entre los dos móviles
 * de la familia. El prefijo evita guardar como clave un QR cualquiera.
 */
object KeyQrCodec {

    private const val PREFIX = "MIA2F-GEMINI-KEY:v1:"

    fun encode(apiKey: String): String = PREFIX + apiKey

    fun decode(payload: String?): String? =
        payload
            ?.takeIf { it.startsWith(PREFIX) }
            ?.removePrefix(PREFIX)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
}

/**
 * Opens Google's code scanner (runs inside Google Play services, so
 * the app needs no camera permission) and returns the key read from a
 * miA2F QR, or an error message in Spanish. Cancelling does nothing.
 * ---
 * Abre el escáner de códigos de Google (corre dentro de Google Play
 * Services, así que la app no necesita permiso de cámara) y devuelve
 * la clave leída de un QR de miA2F, o un mensaje de error en
 * castellano. Cancelar no hace nada.
 */
fun scanKeyQr(
    context: Context,
    onKey: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .enableAutoZoom()
        .build()
    GmsBarcodeScanning.getClient(context, options)
        .startScan()
        .addOnSuccessListener { barcode ->
            val key = KeyQrCodec.decode(barcode.rawValue)
            if (key != null) {
                onKey(key)
            } else {
                onError("Ese código QR no es una clave de miA2F.")
            }
        }
        .addOnFailureListener { e ->
            onError("No se pudo abrir el escáner: ${e.message}")
        }
}

/**
 * Dialog showing the key as a QR for the other phone to scan.
 * ---
 * Diálogo que muestra la clave como QR para que el otro móvil la
 * escanee.
 */
@Composable
fun KeyQrDialog(
    apiKey: String,
    onDismiss: () -> Unit,
) {
    val bitmap = remember(apiKey) { qrBitmap(KeyQrCodec.encode(apiKey)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
        title = { Text("Pasar la clave a otro móvil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "En el otro móvil, abre miA2F y pulsa «Escanear clave " +
                        "desde otro móvil». Enséñale este código solo a " +
                        "ese móvil: quien lo escanee puede usar la clave.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Código QR con la clave",
                    modifier = Modifier.size(260.dp),
                )
            }
        },
    )
}

private fun qrBitmap(content: String, sizePx: Int = 720): Bitmap {
    val matrix = QRCodeWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        sizePx,
        sizePx,
        mapOf(EncodeHintType.MARGIN to 2),
    )
    val pixels = IntArray(sizePx * sizePx) { index ->
        if (matrix[index % sizePx, index / sizePx]) Color.BLACK else Color.WHITE
    }
    return Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888)
}
