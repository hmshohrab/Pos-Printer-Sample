@file:OptIn(ExperimentalMaterial3Api::class)

package com.puremindit.posPrinter

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.dilivva.blueline.builder.Config
import com.dilivva.blueline.builder.PrintData
import com.dilivva.blueline.builder.buildPrintData
import com.dilivva.blueline.connection.bluetooth.BlueLine
import com.dilivva.blueline.connection.bluetooth.ConnectionError
import com.dilivva.blueline.connection.bluetooth.ConnectionState
import com.puremindit.posPrinter.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.ui.tooling.preview.Preview
import pos_printer_sample.composeapp.generated.resources.Res

@Composable
fun charSequenceResource(@StringRes id: Int): CharSequence =
    LocalContext.current.resources.getText(id)

fun CharSequence.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    if (this@toAnnotatedString !is Spanned) {
        append(this.toString())
        return@buildAnnotatedString
    }

    val spanned = this@toAnnotatedString
    append(spanned.toString())
    getSpans(0, spanned.length, Any::class.java).forEach { span ->
        val start = getSpanStart(span)
        val end = getSpanEnd(span)
        when (span) {
            is StyleSpan -> when (span.style) {
                Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                Typeface.BOLD_ITALIC -> addStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic
                    ), start, end
                )
            }
            is UnderlineSpan -> addStyle(
                SpanStyle(textDecoration = TextDecoration.Underline),
                start,
                end
            )
            is ForegroundColorSpan -> addStyle(
                SpanStyle(color = Color(span.foregroundColor)),
                start,
                end
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {

    AppTheme {
        var showContent by remember { mutableStateOf(false) }
        val bluetoothConnection = remember { BlueLine() }
        val connectionState by bluetoothConnection.connectionState().collectAsState()
        val scope = rememberCoroutineScope()
        var message by remember { mutableStateOf("") }
        var showDialog by remember { mutableStateOf(false) }
        var scanCount by remember { mutableStateOf(0) }

        LaunchedEffect(Unit) {
            if (!showContent) showContent = true
        }

        LaunchedEffect(connectionState.bluetoothConnectionError) {
            message = when (connectionState.bluetoothConnectionError) {
                ConnectionError.BLUETOOTH_DISABLED -> "Enable bluetooth on device and click retry"
                ConnectionError.BLUETOOTH_PERMISSION -> "Switch on location access and click retry"
                ConnectionError.BLUETOOTH_NOT_SUPPORTED -> "Bluetooth is not supported on this device"
                null -> ""
                ConnectionError.BLUETOOTH_PRINT_ERROR -> "Error while printing"
                ConnectionError.BLUETOOTH_PRINTER_DEVICE_NOT_FOUND -> "No printer found"
            }
            showDialog = message.isNotEmpty()
        }
        LaunchedEffect(connectionState) {
//            if (connectionState.isBluetoothReady && !connectionState.discoveredPrinter && !connectionState.isScanning && scanCount == 0){
//                scanCount++
//                scope.launch {
//                    bluetoothConnection.scanForPrinters()
//                }
//                //println("Scanning")
//            }
//            if (connectionState.discoveredPrinter && !connectionState.isConnected){
//                bluetoothConnection.connect()
//                println("Connecting")
//            }
//            if (connectionState.isPrinting){
//                //printingStarted = true
//                println("Printing")
//            }
        }

        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            TopAppBar(modifier = Modifier.fillMaxWidth(), title = { Text("Discovered Bluetooth devices") })

            AnimatedVisibility(showContent) {
                ConnectionItem(bluetoothConnection, connectionState, scope)
            }

        }

        if (showDialog) {
            ShowDialog(message) {
                showDialog = false
                if (connectionState.bluetoothConnectionError == ConnectionError.BLUETOOTH_DISABLED) {
                    bluetoothConnection.init()
                }
            }
        }


    }
}

@Composable
fun ShowDialog(
    message: String,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        content = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Error")
                Text(message)
                Button(onClick = onDismiss) {
                    Text("Retry")
                }
            }
        })
}


@OptIn(ExperimentalResourceApi::class)
@Composable
fun ConnectionItem(
    connection: BlueLine,
    connectionState: ConnectionState,
    scope: CoroutineScope
) {

    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageBytes by remember { mutableStateOf(byteArrayOf()) }

    LaunchedEffect(Unit) {
        val bytes = Res.readBytes("drawable/img.png")
        imageBytes = bytes
        image = getPlatform().toImage(bytes)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(15.dp))
            .padding(8.dp)
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    Text(
                        text = connectionState.deviceName,
                        fontSize = 14.sp,
                        color = Color.Red
                    )
                    Text(
                        text = "Is connected: ${connectionState.isConnected}",
                        fontSize = 14.sp,
                        color = Color.Blue
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Button(
                            onClick = { scope.launch(Dispatchers.IO) { connection.scanForPrinters() } },
                            enabled = connectionState.isBluetoothReady && !connectionState.discoveredPrinter && !connectionState.isScanning
                        ) {
                            Text("Scan for printers")
                        }
                        if (connectionState.isScanning) {
                            CircularProgressIndicator()
                        }
                    }

                    Button(
                        onClick = {
                            connection.connect()
                        },
                        enabled = !connectionState.isConnected && connectionState.discoveredPrinter
                    ) {
                        Text("Connect")
                        if (connectionState.isConnecting) {
                            CircularProgressIndicator(
                                color = Color.White
                            )
                        }
                    }

                    Button(
                        onClick = {
                            connection.disconnect()
                        },
                        enabled = connectionState.isConnected
                    ) {
                        Text("Disconnect")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val toPrint = buildPrintData {
                                    appendImage {
                                        this.imageBytes = imageBytes
                                    }
                                }
                                /*                                val toPrint = buildPrintData {
                                                                    appendText {
                                                                        styledText(
                                                                            data = "Name of Sazzad.com\nIslampur",
                                                                            alignment = Config.Alignment.CENTER,
                                                                            font = Config.Font.LARGE_2,
                                                                            style = Config.Style.BOLD
                                                                        )
                                                                        textNewLine()
                                                                        styledText(
                                                                            data = "                                        ",
                                                                            alignment = Config.Alignment.CENTER,
                                                                            style = Config.Style.UNDERLINE
                                                                        )
                                                                        textNewLine()
                                                                        text("Cus Name:  Shohrab")
                                                                        textNewLine(2)
                                                                        text("Cus Phone: 01878036425")
                                                                        textNewLine(2)
                                                                        text("Cus Address:")
                                                                        textNewLine(2)
                                                                        text("Invoice No:")
                                                                        textNewLine(2)
                                                                        text("Date:")
                                                                        textNewLine()
                                                                        text("------------------------------")
                                                                        textNewLine()
                                                                        styledText("Sl"+" Name")
                                                                        styledText("qty/u.p", font = Config.Font.NORMAL, alignment = Config.Alignment.CENTER)
                                                                        styledText("total", font = Config.Font.NORMAL, alignment = Config.Alignment.RIGHT)
                                                                        textNewLine()
                                                                        styledText("1"+" Rijon")
                                                                        styledText("2*100", font = Config.Font.NORMAL, alignment = Config.Alignment
                                                                            .CENTER)
                                                                        styledText("200", font = Config.Font.NORMAL, alignment = Config.Alignment.RIGHT)
                                                                        textNewLine()
                                                                    }
                                                                }*/
                                connection.print(toPrint.first)
                            },
                            enabled = connectionState.canPrint && connectionState.isConnected && !connectionState.isPrinting
                        ) {
                            Text("Print")
                        }
                        if (connectionState.isPrinting) {
                            CircularProgressIndicator()
                        }
                    }


                }

            }

            item {
                image?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalResourceApi::class)
private fun textPrint(bytes: ByteArray): PrintData? {
    try {


        return buildPrintData {
            appendImage {
                imageBytes = bytes
            }
            appendText {
                styledText(
                    data = "Send24",
                    alignment = Config.Alignment.CENTER,
                    font = Config.Font.LARGE_2,
                    style = Config.Style.BOLD
                )
                textNewLine()
                styledText(
                    data = "================================",
                    alignment = Config.Alignment.CENTER,
                    style = Config.Style.BOLD
                )
                textNewLine()
                text("Name: Enoch Oyerinde")
                textNewLine(2)
                text("Phone: 07033879645")
                textNewLine(2)
                styledText(data = "Variant:", font = Config.Font.NORMAL, style = Config.Style.BOLD)
                text("HUB_TO_HUB")
            }
        }
    } catch (e: Exception) {
        return null
    }
}