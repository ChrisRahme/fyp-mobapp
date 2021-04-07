package com.example.fyp_mobapp

import android.os.Bundle
import android.telephony.SmsMessage
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fyp_mobapp.databinding.ActivityMainBinding
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.Callback
import retrofit2.Response
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private lateinit var mainActivity: ActivityMainBinding
    private lateinit var messageList:ArrayList<Message>
    private lateinit var adapter: MessageAdapter

    private val ip   = "194.126.17.114" //"194.126.17.114" //"xxx.ngrok.io" //"localhost:5005"
    private val url  = "http://$ip:/webhooks/rest/" // ⚠️MUST END WITH "/"

    private val USER = "MobileApp2" //"M-" + UUID.randomUUID().toString()
    private val BOT_TXT = "0"
    private val BOT_IMG = "1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        messageList = ArrayList<Message>()
        mainActivity = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainActivity.root)

        adapter = MessageAdapter(this, messageList)
        adapter.setHasStableIds(true)
        mainActivity.messageList.adapter = adapter

        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        mainActivity.messageList.layoutManager = linearLayoutManager

        mainActivity.sendButton.setOnClickListener {
            val msg = mainActivity.messageBox.text.toString().trim()

            if (msg != "") {
                sendMessage(msg)
                mainActivity.messageBox.setText("")
            } else {
                Toast.makeText(this, "Please enter a message.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sendMessage(message: String) {
        var userMessage = Message(USER, message)
        messageList.add(userMessage)
        adapter.notifyDataSetChanged()

        val okHttpClient = OkHttpClient()
        val retrofit = Retrofit.Builder().baseUrl(url).client(okHttpClient).addConverterFactory(GsonConverterFactory.create()).build()
        val messageSender = retrofit.create(MessageSender::class.java)
        val response = messageSender.sendMessage(userMessage)

        response.enqueue(object: Callback<ArrayList<BotResponse>> {
            override fun onResponse(call: Call<ArrayList<BotResponse>>, response: Response<ArrayList<BotResponse>>) {
                if (response.body() != null && response.body()!!.size != 0) {
                    for (i in 0 until response.body()!!.size) {
                        val message = response.body()!![i]
                        if (message.text.isNotEmpty()) {
                            messageList.add(Message(BOT_TXT, message.text))
                        } else if (message.image.isNotEmpty()) {
                            messageList.add(Message(BOT_IMG, message.image))
                        }

                        adapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onFailure(call: Call<ArrayList<BotResponse>>, t: Throwable) {
                val message = "Sorry, something went wrong:\n" + t.message
                messageList.add(Message(BOT_TXT, message))
                adapter.notifyDataSetChanged()
            }
        })
    }
}