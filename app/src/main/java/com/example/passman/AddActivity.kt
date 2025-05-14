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

// Activity asks the user to input new password and a name for the password. Returns new password to
// the main activity
class AddActivity : AppCompatActivity() {
    // Function sets up the buttons and text fields.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Add)) {v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val nameEditText = findViewById<EditText>(R.id.newPasswordName)
        val passwordEditText = findViewById<EditText>(R.id.newPassword)
        val cancelButton = findViewById<Button>(R.id.Cancel)
        val confirmButton = findViewById<Button>(R.id.Confirm)

        cancelButton.setOnClickListener{
            finish()
        }

        confirmButton.setOnClickListener{
            val name = nameEditText.text.toString()
            val password = passwordEditText.text.toString()
            //Tähän väliin voipi asettaa kunnon ehdot salasanalle jos niin haluaa
            if (name.isNotBlank() && password.isNotBlank()){
                val resultIntent = Intent().apply {
                    putExtra("pass_name", name)
                    putExtra("pass", password)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
            else{
                Toast.makeText(this, "Fill both fields", Toast.LENGTH_SHORT).show()
            }
        }

    }
}
