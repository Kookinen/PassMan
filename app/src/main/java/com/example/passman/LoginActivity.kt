package com.example.passman


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import java.io.File
import java.security.MessageDigest
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import org.mindrot.jbcrypt.BCrypt

// Data class for the hashed master password that is saved to internal file
data class PasswordData(val hashedPassword: String)

// This activity is the startup activity of the app. Handles the login functionality and moves to
// the main activity. Normally main activity would be the first activity that is activated at
// startup but I started the development with the listview and I didn't bother changing activity
// names.
class LoginActivity : AppCompatActivity() {
    // Function contains the setup of biometrics, textviews and buttons. Sets up also the internal
    // file for the master password if it is not yet available.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        val authButton = findViewById<Button>(R.id.loginButton)
        val resetButton = findViewById<Button>(R.id.ResetButton)
        val passText = findViewById<EditText>(R.id.PassWord)

        val internalFile = File(filesDir, "pass.json")
        if(!internalFile.exists()){
            val assetData = assets.open("pass.json").bufferedReader().use { it.readText()}
            openFileOutput("pass.json", MODE_PRIVATE).use {
                it.write(assetData.toByteArray())
            }
        }

        //resets both data files
        resetButton.setOnClickListener{
            val popupMenu = PopupMenu(this, resetButton)
            popupMenu.menuInflater.inflate(R.menu.reset_confirm, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.confirm -> {
                        val emptyPasswordData = PasswordData("")
                        val emptyItemData = "[]"
                        val json = Gson().toJson(emptyPasswordData)
                        openFileOutput("pass.json", MODE_PRIVATE).use {
                            it.write(json.toByteArray())
                        }
                        openFileOutput("data.json", MODE_PRIVATE).use {
                            it.write(emptyItemData.toByteArray())
                        }
                        Toast.makeText(this@LoginActivity, "Password has been reset. Password data has been cleared", Toast.LENGTH_SHORT).show()
                        true
                    }

                    else -> false
                }
            }
            popupMenu.show()

        }

        // This clickListener has three functionalities. If master password has been set and the
        // textview is empty, it prompts the biometric login. If master password has been set and
        // the textview contain text it prompts the password login and compares hashes. And finally
        // if master password has not been set and the textview contains text, it sets it as the
        // master password
        authButton.setOnClickListener{
            val pass = passText.text.toString()

            val passwordData = loadMasterJson(this)
            val isEmpty = (passwordData == null || passwordData.hashedPassword.isBlank())

            if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS && !isEmpty && passText.text.toString().isNullOrBlank()){
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Authentication needed")
                    .setSubtitle("Use fingerprint or device credentials to authenticate")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()
                val executor = ContextCompat.getMainExecutor(this)
                val biometricPrompt = BiometricPrompt(this, executor, object :
                    BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    override fun onAuthenticationFailed() {
                        Toast.makeText(this@LoginActivity, "Authentication not successful", Toast.LENGTH_SHORT).show()
                    }
                })
                biometricPrompt.authenticate(promptInfo)
            }
            else if(isEmpty){
                if (checkEmptyPass(pass, true)) {
                    return@setOnClickListener
                }
                if (10 > pass.length){
                    Toast.makeText(this,"Password too short! Use at least 10 characters.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val hashedPass = hashPassword(pass)
                saveJson(this, hashedPass)
                Toast.makeText(this, "Password saved successfully, please use it or use fingerprint to log in.", Toast.LENGTH_SHORT).show()
                //Pidetään sisäänkirjautuminen erillään encryptauksesta. Eli sisäänkirjautumiseen biometriikka tai oma salasana
                //Generoidaan kuitenkin salausavain täällä
                generateAESKey()
            }
            else {
                if (checkEmptyPass(pass, false)) {
                    return@setOnClickListener
                }
                val savedPass = passwordData?.hashedPassword
                if(BCrypt.checkpw(pass, savedPass)){
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                else{
                    Toast.makeText(this,"Wrong password!", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    // Checks if the textview is empty or not and prompts according instructions.
    private fun checkEmptyPass(pass: String, register: Boolean): Boolean{
        if (pass.isBlank() and register) {
            Toast.makeText(this, "Please set up the master password.", Toast.LENGTH_SHORT).show()
            return true
        }
        else if (pass.isBlank()){
            Toast.makeText(this, "Please enter a password.", Toast.LENGTH_SHORT).show()
            return true
        }
        else{
            return false
        }
    }

    // Loads the master password hash from internal memory
    private fun loadMasterJson(context: Context): PasswordData? {
        val file = File(context.filesDir, "pass.json")
        val jsonString = if (file.exists()){
            file.bufferedReader().use {it.readText()}
        }
        else{
            context.assets.open("pass.json").bufferedReader().use {it.readText()}
        }
        return Gson().fromJson(jsonString, PasswordData::class.java)
    }

    // Hashes the password
    private fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    // Saves the password hash into the internal file
    private fun saveJson(context: Context, hashedPassword: String){
        val passwordData = PasswordData(hashedPassword)
        val jsonArray = Gson().toJson(passwordData)
        context.openFileOutput("pass.json", MODE_PRIVATE).use{
            it.write(jsonArray.toByteArray())
        }
    }

    // Generates the AES key at the same time when master password is set. Key is then saved to
    // Android keystore from where it can be fetched to encrypt added passwords
    private fun generateAESKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keySpec = KeyGenParameterSpec.Builder("AESKey",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()
        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }
}