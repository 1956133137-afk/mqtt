package com.yannuo.dgcanteen.util


import android.os.Environment
import androidx.annotation.Nullable
import java.io.File
import java.lang.Boolean
import java.util.Properties
import javax.activation.CommandMap
import javax.activation.MailcapCommandMap
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import kotlin.Exception
import kotlin.Int
import kotlin.String
import kotlin.Throwable
import kotlin.jvm.internal.Intrinsics


object EmailSender {
    private var SMTP_HOST = "smtp.mxhichina.com"
    private var SMTP_PORT = 25
    private var SMTP_USERNAME = "log@yannuozhineng.com"
    private var SMTP_PASSWORD = "Log2024#Yannuo"
    private var DEBUG = true
    private val TAG = javaClass.simpleName
    interface CallbackListener {
        fun onStare(i: Int, @Nullable str: String?)
    }

    @JvmStatic
    fun sendEmail(
        to: String,
        subject: String,
        @Nullable attach: String?,
        @Nullable body: String?,
        @Nullable lis: CallbackListener?,
    ) {
        Intrinsics.checkNotNullParameter(to, "to")
        Intrinsics.checkNotNullParameter(subject, "subject")
        try {
            if (lis != null) {
                try {
                    lis.onStare(10, "config send of environment.")
                } catch (e: Exception) {
                    val msg = e.message
                    if (lis != null) {
                        lis.onStare(30, msg)
                        return
                    }
                    return
                }
            }
            val properties = Properties()
            properties["mail.smtp.host"] = SMTP_HOST
            properties["mail.smtp.port"] = Integer.valueOf(SMTP_PORT)
            properties["mail.smtp.auth"] = "true"
            properties["mail.debug"] = Boolean.valueOf(DEBUG)
            properties[" mail.mime.encodefilename"] = "true"
            val session = Session.getDefaultInstance(
                properties,
                object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        val str: String
                        val str2 = SMTP_USERNAME
                        str = SMTP_PASSWORD
                        return PasswordAuthentication(str2, str)
                    }
                })
            Thread.currentThread().contextClassLoader = Message::class.java.classLoader
            val ms = MimeMessage(session)
            ms.setFrom(InternetAddress(SMTP_USERNAME))
            ms.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(to)
            )
            ms.subject = subject
            val multipart = MimeMultipart()
            if (body != null) {
                val messageBodyPart = MimeBodyPart()
                messageBodyPart.setContent(body, "text/html;charset=UTF-8")
                multipart.addBodyPart(messageBodyPart)
            }
            var path = attach
            if (path == null) {
                path =
                    Environment.getExternalStorageDirectory().absolutePath + "/elderly/log"
            }
            val file = File(path)
            if (file.isDirectory) {
                val listFiles = file.listFiles()
                Intrinsics.checkNotNullExpressionValue(listFiles, "file.listFiles()")
                for (fe in listFiles) {
                    val filePart = MimeBodyPart()
                    filePart.fileName = fe.name
                    filePart.attachFile(fe)
                    multipart.addBodyPart(filePart)
                }
            } else {
                val filePart2 = MimeBodyPart()
                filePart2.fileName = file.name
                filePart2.attachFile(file)
                multipart.addBodyPart(filePart2)
            }
            ms.setContent(multipart)
            val defaultCommandMap = CommandMap.getDefaultCommandMap()
            val mc: MailcapCommandMap = defaultCommandMap as MailcapCommandMap
            mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html")
            mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml")
            mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain")
            mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed")
            mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822")
            CommandMap.setDefaultCommandMap(mc)
            Transport.send(ms)
            lis?.onStare(0, "send ok.")
        } catch (th: Throwable) {
            LogUtil.w(TAG, "send email: ${th.printStackTrace()}")
            lis?.onStare(10, null)
            throw th
        }
    }


}
