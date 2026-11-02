package com.example.sofe4640ufinalproject

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)





        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        testFirebaseSignUp("testuser@example.com", "test1234")
    }

    private fun testFirebaseSignUp(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    Log.d("FirebaseTest", "Sign up SUCCESS, uid=$uid")
                    Toast.makeText(this, "Firebase connected! User created", Toast.LENGTH_LONG).show()
                } else {
                    val msg = task.exception?.message
                    Log.e("FirebaseTest", "Sign up FAILED: $msg", task.exception)
                    Toast.makeText(this, "Failed: $msg", Toast.LENGTH_LONG).show()
                }
            }
    }
}