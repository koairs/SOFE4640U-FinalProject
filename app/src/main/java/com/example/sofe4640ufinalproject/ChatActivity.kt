package com.example.sofe4640ufinalproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.io.File

class ChatActivity : AppCompatActivity() {

    private lateinit var otherUser: String
    private lateinit var itemTitle: String
    private lateinit var listingOwnerId: String
    private lateinit var adapter: ChatAdapter
    private lateinit var rvMessages: RecyclerView
    private lateinit var etInput: EditText

    private var mediaRecorder: MediaRecorder? = null
    private var recordedAudioFile: File? = null
    private var isRecording = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(this, "Microphone permission required for voice chat", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        otherUser = intent.getStringExtra("otherUser") ?: "Seller"
        itemTitle = intent.getStringExtra("itemTitle") ?: ""
        listingOwnerId = intent.getStringExtra("listingOwnerId") ?: ""

        val tvPartner = findViewById<TextView>(R.id.tvChatPartner)
        tvPartner.text = otherUser
        val openProfile = View.OnClickListener {
            val intent = Intent(this, Profile::class.java).apply {
                putExtra("userId", otherUser)
            }
            startActivity(intent)
        }
        tvPartner.setOnClickListener(openProfile)

        val tvTag = findViewById<TextView>(R.id.tvItemTitleTag)
        if (itemTitle.isNotBlank()) {
            tvTag.visibility = View.VISIBLE
            tvTag.text = "Item: $itemTitle"
        } else {
            tvTag.visibility = View.GONE
        }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        rvMessages = findViewById(R.id.rvChatMessages)
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMessages.layoutManager = layoutManager

        val currentUserId = MessageStore.currentUserId()
        adapter = ChatAdapter(emptyList(), currentUserId)
        rvMessages.adapter = adapter

        etInput = findViewById(R.id.etMessageInput)
        val btnSend = findViewById<MaterialButton>(R.id.btnSendMessage)
        val btnMic = findViewById<MaterialButton>(R.id.btnMic)

        btnSend.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isNotBlank()) {
                MessageStore.sendMessage(this, otherUser, itemTitle, text, listingOwnerId = listingOwnerId) { success ->
                    if (success) {
                        etInput.setText("")
                        loadMessages()
                    } else {
                        Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnMic.setOnClickListener {
            if (isRecording) {
                stopAndSendRecording()
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startRecording()
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }

        loadMessages()
    }

    private fun startRecording() {
        try {
            recordedAudioFile = File(filesDir, "voice_${System.currentTimeMillis()}.3gp")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            recorder.setOutputFile(recordedAudioFile!!.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            isRecording = true
            Toast.makeText(this, "Recording voice message... Tap mic again to send", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
            isRecording = false
        }
    }

    private fun stopAndSendRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
        isRecording = false

        val path = recordedAudioFile?.absolutePath
        if (path != null && File(path).exists()) {
            MessageStore.sendMessage(
                context = this,
                receiverId = otherUser,
                itemTitle = itemTitle,
                messageText = "🎤 Voice Message",
                audioUri = path,
                isAudio = true,
                listingOwnerId = listingOwnerId
            ) { success ->
                if (success) {
                    Toast.makeText(this, "Voice message sent", Toast.LENGTH_SHORT).show()
                    loadMessages()
                } else {
                    Toast.makeText(this, "Failed to send voice message", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "No audio recorded", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        ColorBlindHelper.apply(this)
    }

    private fun loadMessages() {
        MessageStore.markMessagesAsRead(this, otherUser)
        MessageStore.getMessagesBetween(this, otherUser) { list ->
            adapter.update(list)
            if (list.isNotEmpty()) {
                rvMessages.smoothScrollToPosition(list.size - 1)
            }
        }
    }
}
