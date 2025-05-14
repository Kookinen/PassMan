package com.example.passman

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// Activity asks user for the new master password. User must write it twice and it must match.
// Returns the new master password to the main activity
class ResetMasterActivity : AppCompatActivity() {
    // Function sets up the text fields and buttons. The confirm button contains some conditions to
    // the master password
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reset_master)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ResetMaster)) {v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val passwordEditText = findViewById<EditText>(R.id.newPassword)
        val repeatPasswordEditText = findViewById<EditText>(R.id.repeatPassword)
        val cancelButton = findViewById<Button>(R.id.MasterCancel)
        val confirmButton = findViewById<Button>(R.id.MasterConfirm)


        cancelButton.setOnClickListener{
            finish()
        }

        confirmButton.setOnClickListener{
            val password = passwordEditText.text.toString()
            val repeatPassword = repeatPasswordEditText.text.toString()
            if (password.isNotBlank() && password.length > 10 && password == repeatPassword){
                val resultIntent = Intent().apply {
                    putExtra("Master_pass", password)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
            else if(password.isNullOrBlank() && repeatPassword.isNullOrBlank()){
                Toast.makeText(this, "Please enter and repeat the password!", Toast.LENGTH_SHORT).show()
            }
            else if(password.isNullOrBlank()){
                Toast.makeText(this, "Please enter the password!", Toast.LENGTH_SHORT).show()
            }
            else if(repeatPassword.isNullOrBlank()){
                Toast.makeText(this, "Please repeat the password!", Toast.LENGTH_SHORT).show()
            }
            else if(password != repeatPassword){
                Toast.makeText(this, "Passwords don't match!", Toast.LENGTH_SHORT).show()
            }
            else if(password.length < 10){
                Toast.makeText(this, "Password too short (less than 10 chars)!", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(this, "You shouldn't get here.", Toast.LENGTH_SHORT).show()
            }


        }

    }
}
