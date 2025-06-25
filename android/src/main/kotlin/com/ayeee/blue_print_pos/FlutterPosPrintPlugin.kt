package com.ayeee.blue_print_pos

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

import android.bluetooth.BluetoothAdapter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.annotation.NonNull
import com.android.print.sdk.PrinterConstants
import com.android.print.sdk.PrinterConstants.Command
import com.android.print.sdk.PrinterInstance
import com.android.print.sdk.bluetooth.BluetoothPort
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL
import java.util.EnumMap

/** FlutterPosPrintPlugin */
class FlutterPosPrintPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel

    private val hrLine = "--------------------------------";
    private val pointLogo = "point_logo.png";

    private lateinit var mPrinter: PrinterInstance;
    private var isConnected = false
    private var onSuccess: () -> Unit = {}
    private lateinit var flutterPluginBinding: FlutterPlugin.FlutterPluginBinding;

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_pos_print")
        this.flutterPluginBinding = flutterPluginBinding
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }

            "connectBT" -> {
                val address = call.argument<String>("address")
                if (address != null) {
                    connectBT(address) {
                        result.success(it)
                    }
                };
            }

            "autoConnectBT" -> {
                val address = call.argument<String>("address")
                if (address != null) {
                    autoConnectBT(address) {
                        result.success(it)
                    }
                };
            }

            "generateQrCode" -> {
                val data = call.argument<String>("data") ?: ""
                val bitmap = generateQRCode(data, 200, 200)

                if (bitmap != null) {
                    val byteData = bitmapToByteData(bitmap)
                    result.success(byteData)
                } else {
                    result.error("ERROR_CODE", "Failed to generate QR code", null)
                }
            }

            "printTestInvoice" -> {
                val isEnLang = call.argument<Boolean>("isEnLang") ?: true
                val heading = call.argument<String>("heading") ?: ""
                val subHeading = call.argument<String>("subHeading") ?: ""
                val date = call.argument<String>("date") ?: ""
                val time = call.argument<String>("time") ?: ""
                val supportEmail = call.argument<String>("supportEmail") ?: ""
                val supportPhone = call.argument<String>("supportPhone") ?: ""

                val success = printTestInvoice(
                    isEnLang, heading, subHeading, date, time, supportEmail, supportPhone
                )

                result.success(success)
            }

            "printInvoice" -> {
                val isEnLang = call.argument<Boolean>("isEnLang") ?: true
                val qrEnable = call.argument<Boolean>("qrEnable") ?: true
                val date = call.argument<String>("date") ?: ""
                val time = call.argument<String>("time") ?: ""
                val store = call.argument<String>("store") ?: ""
                val cardName = call.argument<String>("cardName") ?: ""
                val redeemCode = call.argument<String>("redeemCode") ?: ""
                val serialNumber = call.argument<String>("serialNumber") ?: ""
                val instructions = call.argument<String>("instructions") ?: ""
                val supportEmail = call.argument<String>("supportEmail") ?: ""
                val supportPhone = call.argument<String>("supportPhone") ?: ""
                val advertiseText = call.argument<String>("advertiseText") ?: ""
                val logo = call.argument<String>("logo") ?: ""

                val success = runBlocking {
                    printInvoice(
                        isEnLang,
                        qrEnable,
                        date,
                        time,
                        store,
                        cardName,
                        redeemCode,
                        serialNumber,
                        instructions,
                        supportEmail,
                        supportPhone,
                        advertiseText,
                        logo,
                    )
                }

                result.success(success)
            }

            "printCompanyInvoice" -> {
                val isEnLang = call.argument<Boolean>("isEnLang") ?: true
                val voucherCode = call.argument<String>("voucherCode") ?: ""
                val id = call.argument<String>("id") ?: ""
                val date = call.argument<String>("date") ?: ""
                val time = call.argument<String>("time") ?: ""
                val store = call.argument<String>("store") ?: ""
                val companyName = call.argument<String>("companyName") ?: ""
                val companyNumber = call.argument<String>("companyNumber") ?: ""
                val amount = call.argument<String>("amount") ?: ""
                val supportEmail = call.argument<String>("supportEmail") ?: ""
                val supportPhone = call.argument<String>("supportPhone") ?: ""

                val success = printCompanyInvoice(
                    isEnLang,
                    voucherCode,
                    id,
                    date,
                    time,
                    store,
                    companyName,
                    companyNumber,
                    amount,
                    supportEmail,
                    supportPhone
                )

                result.success(success)
            }

            "printDailyReport" -> {
                val isEnLang = call.argument<Boolean>("isEnLang") ?: true
                val fromDate = call.argument<String>("fromDate") ?: ""
                val toDate = call.argument<String>("toDate") ?: ""
                val store = call.argument<String>("store") ?: ""
                val reportData =
                    call.argument<List<Map<String, Any>>>("reportData") ?: emptyList()
                val totalCard = call.argument<String>("totalCard") ?: "0"
                val totalQuantity = call.argument<String>("totalQuantity") ?: "0"
                val totalAmount = call.argument<String>("totalAmount") ?: "0.0"
                val totalProfit = call.argument<String>("totalProfit") ?: "0.0"
                val date = call.argument<String>("date") ?: ""
                val time = call.argument<String>("time") ?: ""
                val supportEmail = call.argument<String>("supportEmail") ?: ""
                val supportPhone = call.argument<String>("supportPhone") ?: ""

                val success = printDailyReport(
                    isEnLang,
                    fromDate,
                    toDate,
                    store,
                    reportData,
                    totalCard,
                    totalQuantity,
                    totalAmount,
                    totalProfit,
                    date,
                    time,
                    supportEmail,
                    supportPhone
                )

                result.success(success)
            }

            else -> {
                result.notImplemented();
            }
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    private fun connectBT(address: String, onResult: (status: Boolean) -> Unit) {
        val adapter = BluetoothAdapter.getDefaultAdapter();
        mPrinter = BluetoothPort().btConnnect(flutterPluginBinding.applicationContext,
            address,
            adapter,
            object : Handler(Looper.myLooper()!!) {
                override fun handleMessage(msg: Message) {
                    when (msg.what) {
                        PrinterConstants.Connect.SUCCESS -> {
                            isConnected = true;
                            onResult(true)
                        }

                        PrinterConstants.Connect.FAILED -> {
                            isConnected = false;
                            onResult(false)
                        }

                        PrinterConstants.Connect.CLOSED -> {
                            isConnected = false;
                            onResult(false)
                        }

                        PrinterConstants.Connect.NODEVICE -> {
                            isConnected = false;
                            onResult(false)
                        }

                        else -> {
                            isConnected = false;
                            onResult(false)
                        }
                    }
                }
            })
    }

    private fun autoConnectBT(address: String, onResult: (status: Boolean) -> Unit) {
        connectBT(address, onResult)
    }

    private fun printTestInvoice(
        isEnLang: Boolean,
        heading: String,
        subHeading: String,
        date: String,
        time: String,
        supportEmail: String,
        supportPhone: String,
    ): Boolean {
        val logo = pointLogo;
        // initialize printer
        mPrinter.init();

        mPrinter.setEncoding("utf-8");

        var bitmap: Bitmap? = null;
        try {
            bitmap = BitmapFactory.decodeStream(flutterPluginBinding.applicationContext.resources.getAssets().open(logo));
        } catch (e: IOException) {
            e.printStackTrace();
            return false
        }
        // logo
        sdkPrintImage(mPrinter, bitmap);
        // heading
        sdkPrintCenterText(mPrinter, heading, bold = true);
        // sub-heading
        sdkPrintCenterText(mPrinter, subHeading);
        // date - time
        val dateTimeString: String = if (isEnLang) {
            "Date: $date       Time: $time"
        } else {
            "تواريخ: $date        وقت: $time"
        }
        sdkPrintLeftToRightText(mPrinter, dateTimeString)
        // line
        sdkPrintLine(mPrinter);
        // support
        sdkPrintLeftToRightText(mPrinter, "$supportEmail | $supportPhone", feed = 1);
        // powered by
        sdkPrintCenterText(mPrinter, if (isEnLang) "Powered by" else "مشغل بواسطة");
        // point
        sdkPrintCenterText(mPrinter, if (isEnLang) "Point" else "بوينت", bold = true, feed = 3);
        return true
    }

    private suspend fun printInvoice(
        isEnLang: Boolean,
        qrEnable: Boolean,
        date: String,
        time: String,
        store: String,
        cardName: String,
        redeemCode: String,
        serialNumber: String,
        instructions: String,
        supportEmail: String,
        supportPhone: String,
        advertiseText: String,
        logo: String,
    ): Boolean {
        val advertise: List<String> = separateWordsIntoLines(advertiseText);
        val appLogo = pointLogo;
        var logoBitmap: Bitmap? = null; // 200 * 75
        var brandBitmap: Bitmap? = null; // 75 * 75
        try {
            logoBitmap = BitmapFactory.decodeStream(flutterPluginBinding.applicationContext.resources.assets.open(appLogo));
            if (!logo.isNullOrEmpty()) {
                brandBitmap = loadImageFromURL(logo)?.let {
                    Bitmap.createScaledBitmap(it, 400, 75, true)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace();
            return false
        }
        // PRINTER SETUP
        mPrinter.init();
        mPrinter.setEncoding("utf-8");
        // logo
        if (brandBitmap != null) {
            sdkPrintImage(mPrinter, brandBitmap)
        } else {
            sdkPrintImage(mPrinter, logoBitmap)
        }
        // date - time
        val dateTimeString: String = if (isEnLang) {
            "Date: $date       Time: $time"
        } else {
            "تواريخ: $date        وقت: $time"
        }
        sdkPrintLeftToRightText(mPrinter, dateTimeString)
        // store
        if (isEnLang) {
            sdkPrintLeftToRightText(mPrinter, "Store: $store")
        } else {
            sdkPrintRightToLeftText(mPrinter, "محل: $store")
        }
        // card
        if (isEnLang) {
            sdkPrintLeftToRightText(mPrinter, "Card: $cardName")
        } else {
            sdkPrintRightToLeftText(mPrinter, "بطاقة: $cardName")
        }
        // serial number
        if (serialNumber.length > 0) {
            if (isEnLang) {
                sdkPrintLeftToRightText(mPrinter, "Serial No.: $serialNumber")
            } else {
                sdkPrintRightToLeftText(mPrinter, "الرقم التسلسلي: $serialNumber")
            }
        }
        // line
        sdkPrintLine(mPrinter)
        // redeem code title
        sdkPrintCenterText(mPrinter, if (isEnLang) "Redeem Code" else "رمز الشحن")
        // code
        sdkPrintCenterText(mPrinter, redeemCode, bold = true, isBigSize = true)
        // instruction
        sdkPrintCenterText(mPrinter, instructions)
        // line
        sdkPrintLine(mPrinter)
        // support
        sdkPrintLeftToRightText(mPrinter, "$supportEmail | $supportPhone", enableFeed = false)
        // QR code
        if (qrEnable) {
            var qrCodeBitmap: Bitmap? = null;
            try {
                // NOTE:- Always add size of QR-code in ratio of 200
                qrCodeBitmap = generateQRCode(redeemCode, 200, 200)
            } catch (e: IOException) {
                e.printStackTrace();
                return false
            }
            qrCodeBitmap?.let {
                if (it != null) {
                    sdkPrintImage(mPrinter, it)
                }
            }
        }
        // advertise
        if (advertiseText.length > 0) {
            sdkAddFeed(mPrinter, 0)
            // banner Line
            sdkPrintLine(mPrinter)
            // advertise text
            for (text in advertise) {
                mPrinter.setPrinter(Command.ALIGN, Command.ALIGN_CENTER);
                mPrinter.setFont(0, 0, 0, 0);
                mPrinter.printText("$text");
                mPrinter.setPrinter(Command.PRINT_AND_WAKE_PAPER_BY_LINE, 0);
            }
            // banner Line
            sdkPrintLine(mPrinter, enableFeed = false)
        }
        sdkAddFeed(mPrinter, feed = 3)
        return true
    }

    private fun printCompanyInvoice(
        isEnLang: Boolean,
        voucherCode: String,
        id: String,
        date: String,
        time: String,
        store: String,
        companyName: String,
        companyNumber: String,
        amount: String,
        supportEmail: String,
        supportPhone: String,
    ): Boolean {
        val logo = pointLogo;
        mPrinter.init();
        mPrinter.setEncoding("utf-8");

        var bitmap: Bitmap? = null;
        try {
            bitmap = BitmapFactory.decodeStream(flutterPluginBinding.applicationContext.resources.getAssets().open(logo));
        } catch (e: IOException) {
            e.printStackTrace();
            return false
        }
        // logo
        sdkPrintImage(mPrinter, bitmap)
        // voucher code heading
        sdkPrintCenterText(mPrinter, if (isEnLang) "Voucher Code" else "رمز القسيمة")
        // voucher code
        sdkPrintCenterText(
            mPrinter, voucherCode, isBigSize = true, bold = true
        )
        // line
        sdkPrintLine(mPrinter)
        // date - time
        val dateTimeString: String = if (isEnLang) {
            "Date: $date    Time: $time"
        } else {
            "تواريخ: $date        وقت: $time"
        }
        sdkPrintLeftToRightText(mPrinter, dateTimeString)
        // id
        if (isEnLang) {
            sdkPrintLeftToRightText(mPrinter, "ID: $id")
        } else {
            sdkPrintRightToLeftText(mPrinter, "بطاقة تعريف: $id")
        }
        // store
        if (isEnLang) {
            sdkPrintLeftToRightText(mPrinter, "Store: $store")
        } else {
            sdkPrintRightToLeftText(mPrinter, "محل: $store")
        }
        // company
        if (isEnLang) {
            sdkPrintLeftToRightText(mPrinter, "Company: $companyName")
        } else {
            sdkPrintRightToLeftText(mPrinter, "شركة: $companyName")
        }
        // company number
        if (isEnLang) {
            sdkPrintLeftToRightText(mPrinter, "Company No.: $companyNumber")
        } else {
            sdkPrintRightToLeftText(mPrinter, "رقم الشركة: $companyNumber")
        }
        // amount
        if (isEnLang) {
            sdkPrintLeftToRightText(mPrinter, "Amount: $amount")
        } else {
            sdkPrintRightToLeftText(mPrinter, "مقدار: $amount")
        }
        // line
        sdkPrintLine(mPrinter)
        // support
        sdkPrintLeftToRightText(mPrinter, "$supportEmail | $supportPhone", feed = 3)
        return true
    }

    private fun printDailyReport(
        isEnLang: Boolean,
        fromDate: String,
        toDate: String,
        store: String,
        reportData: List<Map<String, Any>>,
        totalCard: String,
        totalQuantity: String,
        totalAmount: String,
        totalProfit: String,
        date: String,
        time: String,
        supportEmail: String,
        supportPhone: String,
    ): Boolean {
        val logo = pointLogo;
        mPrinter.init();
        mPrinter.setEncoding("utf-8");

        var bitmap: Bitmap? = null;
        try {
            bitmap = BitmapFactory.decodeStream(flutterPluginBinding.applicationContext.resources.getAssets().open(logo));
        } catch (e: IOException) {
            e.printStackTrace();
            return false
        }
        // logo
        sdkPrintImage(mPrinter, bitmap)
        // print heading
        sdkPrintCenterText(mPrinter, if (isEnLang) "Daily Report" else "تقرير يومي", bold = true)
        // from date
        if (isEnLang) {
            sdkPrintLeftToRightText(mPrinter, "From:  $fromDate")
        } else {
            sdkPrintRightToLeftText(mPrinter, "من: $fromDate")
        }
        // to date
        if (isEnLang) {
            sdkPrintLeftToRightText(mPrinter, "To:    $toDate")
        } else {
            sdkPrintRightToLeftText(mPrinter, "ل:  $toDate")
        }
        // store
        if (isEnLang) {
            sdkPrintLeftToRightText(mPrinter, "Store: $store")
        } else {
            sdkPrintRightToLeftText(mPrinter, "محل: $store")
        }
        // line
        sdkPrintLine(mPrinter)
        // table heading
        if (isEnLang) {
            sdkPrintLeftToRightText(mPrinter, "Qty  Card            Amt (LYD)")
        } else {
            sdkPrintRightToLeftText(mPrinter, "الكمية   البطاقة        المبلغ (LYD)")
        }
        // line
        sdkPrintLine(mPrinter)
        // cards
        for (item in reportData) {
            val cardData = item["card"] as Map<String, Any>
            val cardName = cardData["name"] as String
            var cardArabicName = cardData["arabic_name"] as String
            val amount = item["totalAmount"] as Double
            val qty = item["totalCount"] as Int

            if (isEnLang) {
                sdkPrintLeftToRightText(
                    mPrinter, "$qty $cardName - ${String.format("%.2f", amount)}"
                )
            } else {
                if (cardArabicName != "") {
                    sdkPrintRightToLeftText(
                        mPrinter, "$qty $cardArabicName - ${String.format("%.2f", amount)}"
                    )
                } else {
                    sdkPrintRightToLeftText(
                        mPrinter, "${String.format("%.2f", amount)} - $cardName $qty"
                    )
                }
            }
        }
        // line
        sdkPrintLine(mPrinter)
        // total card
        if (isEnLang) {
            sdkPrintLeftToRightText(mPrinter, "Total Card: $totalCard")
        } else {
            sdkPrintRightToLeftText(mPrinter, "إجمالي البطاقة: $totalCard")
        }
        // total quantity
        if (isEnLang) {
            sdkPrintLeftToRightText(mPrinter, "Total Quantity: $totalQuantity")
        } else {
            sdkPrintRightToLeftText(mPrinter, "الكمية الإجمالية: $totalQuantity")
        }
        // total amount
        if (isEnLang) {
            sdkPrintLeftToRightText(mPrinter, "Total Amount: $totalAmount")
        } else {
            sdkPrintRightToLeftText(mPrinter, "المبلغ الإجمالي: $totalAmount")
        }
        // total profit
        if (isEnLang) {
            sdkPrintLeftToRightText(mPrinter, "Total Profit: $totalProfit")
        } else {
            sdkPrintRightToLeftText(mPrinter, "اجمالي الربح: $totalProfit")
        }
        // line
        sdkPrintLine(mPrinter)
        // current date
        sdkPrintCenterText(mPrinter, "$date $time")
        // support
        sdkPrintLeftToRightText(mPrinter, "$supportEmail | $supportPhone", feed = 3)
        return true
    }

    private fun separateWordsIntoLines(sentence: String): List<String> {
        val maxLineLength = 32
        val words = sentence.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            if (currentLine.isEmpty()) {
                currentLine = word
            } else if (currentLine.length + word.length + 1 <= maxLineLength) {
                currentLine += " $word"
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }

    private fun bitmapToByteData(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    private fun generateQRCode(data: String, width: Int, height: Int): Bitmap? {
        return try {
            val qrCodeWriter = QRCodeWriter()
            val hints: MutableMap<EncodeHintType, Any?> = EnumMap(
                EncodeHintType::class.java
            )
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            val bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints)
            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height
            val bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.RGB_565)
            for (x in 0 until matrixWidth) {
                for (y in 0 until matrixWidth) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun loadImageFromURL(url: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection()
                connection.connect()
                val inputStream = connection.getInputStream()
                BitmapFactory.decodeStream(inputStream)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }

    // custom methods for printing
    private fun sdkAddFeed(mPrinter: PrinterInstance, feed: Int? = 0) {
        mPrinter.setPrinter(Command.PRINT_AND_WAKE_PAPER_BY_LINE, feed ?: 0);
    }

    private fun sdkPrintLine(mPrinter: PrinterInstance, enableFeed: Boolean? = true) {
        mPrinter.setPrinter(Command.ALIGN, Command.ALIGN_LEFT);
        mPrinter.setFont(0, 0, 0, 0);
        mPrinter.printText(hrLine);
        if (enableFeed != false) {
            mPrinter.setPrinter(Command.PRINT_AND_WAKE_PAPER_BY_LINE, 0);
        }
    }

    private fun sdkPrintImage(mPrinter: PrinterInstance, bitmap: Bitmap) {
        mPrinter.setPrinter(Command.ALIGN, Command.ALIGN_CENTER);
        mPrinter.printImage(bitmap);
    }

    private fun sdkPrintCenterText(
        mPrinter: PrinterInstance,
        content: String,
        bold: Boolean? = false,
        isBigSize: Boolean? = false,
        feed: Int? = 0,
        enableFeed: Boolean? = true
    ) {
        mPrinter.setPrinter(Command.ALIGN, Command.ALIGN_CENTER);
        mPrinter.setFont(if (isBigSize == true) 1 else 0, 0, if (bold == true) 1 else 0, 0);
        mPrinter.printText(content);
        if (enableFeed != false) {
            mPrinter.setPrinter(Command.PRINT_AND_WAKE_PAPER_BY_LINE, feed ?: 0);
        }
    }

    private fun sdkPrintLeftToRightText(
        mPrinter: PrinterInstance,
        content: String,
        bold: Boolean? = false,
        isBigSize: Boolean? = false,
        feed: Int? = 0,
        enableFeed: Boolean? = true
    ) {
        mPrinter.setPrinter(Command.ALIGN, Command.ALIGN_LEFT);
        mPrinter.setFont(if (isBigSize == true) 1 else 0, 0, if (bold == true) 1 else 0, 0);
        mPrinter.printText(content);
        if (enableFeed != false) {
            mPrinter.setPrinter(Command.PRINT_AND_WAKE_PAPER_BY_LINE, feed ?: 0);
        }
    }

    private fun sdkPrintRightToLeftText(
        mPrinter: PrinterInstance,
        content: String,
        bold: Boolean? = false,
        isBigSize: Boolean? = false,
        feed: Int? = 0,
        enableFeed: Boolean? = true
    ) {
        mPrinter.setPrinter(Command.ALIGN, Command.ALIGN_RIGHT);
        mPrinter.setFont(if (isBigSize == true) 1 else 0, 0, if (bold == true) 1 else 0, 0);
        mPrinter.printText(content);
        if (enableFeed != false) {
            mPrinter.setPrinter(Command.PRINT_AND_WAKE_PAPER_BY_LINE, feed ?: 0);
        }
    }

}
