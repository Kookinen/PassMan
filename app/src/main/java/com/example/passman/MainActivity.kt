package com.example.passman

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.mindrot.jbcrypt.BCrypt


// The activity that shows the password list and has buttons for resetting the master password and
// for adding new passwords.
class MainActivity : ComponentActivity() {
    private lateinit var items: ArrayList<Item>
    private lateinit var adapter: Adapter

    // Sets up the buttons and the listView for the password list. Also sets up the internal file if
    // it doesn't exist yet
    // Fetches the passwords from the memory to the listView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val listView = findViewById<ListView>(R.id.listView)
        val addButton = findViewById<Button>(R.id.AddPass)
        val resetButton = findViewById<Button>(R.id.ResetPass)

        items = ArrayList()


        val internalFile = File(filesDir, "data.json")
        if(!internalFile.exists()){
            val assetData = assets.open("data.json").bufferedReader().use { it.readText()}
            openFileOutput("data.json", MODE_PRIVATE).use {
                it.write(assetData.toByteArray())
            }
        }

        // Load and parse JSON
        val jsonString = loadJson(this)
        items = parseJson(jsonString)

        adapter = Adapter(this, items)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = items[position]
            val pass = aesDecrypt(selectedItem.value, selectedItem.iv)
            copyToClipboard(pass)
        }

        listView.setOnItemLongClickListener{_,view, position, _ ->
            showPopupMenu(view, position)
            true
        }

        addButton.setOnClickListener {
            val intent = Intent(this, AddActivity::class.java)
            addActivityLauncher.launch(intent)
        }

        resetButton.setOnClickListener {
            val intent = Intent(this, ResetMasterActivity::class.java)
            masterResetActivityLauncher.launch(intent)
        }
    }

    // Launches the password adding activity if add new password button is pressed. Activity returns
    // the new password and a name for it. Also contains the encryption of the password
    private val addActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val passname = result.data?.getStringExtra("pass_name")
            val newpassword = result.data?.getStringExtra("pass")
            if (!passname.isNullOrBlank() && !newpassword.isNullOrBlank()) {
                aesEncrypt(passname, newpassword)
                Toast.makeText(this, "Added: $passname", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(this, "No password defined!", Toast.LENGTH_SHORT)
            }

        }
    }

    // Launches the master password reset activity when reset button is pressed. Activity returns
    // the new master password, it gets hashed and saved to the file.
    private val masterResetActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val newMasterPass = result.data?.getStringExtra("Master_pass")
            val hashedPass = BCrypt.hashpw(newMasterPass, BCrypt.gensalt())
            val json = Gson().toJson(PasswordData(hashedPass))
            openFileOutput("pass.json", MODE_PRIVATE).use {
                it.write(json.toByteArray())
            }
        }
    }

    // Popup menu implementation for the deletion of a password from the list
    private fun showPopupMenu(view: View, position: Int) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.delete -> {
                    deletePassword(position)
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
    }

    // Deletion of a password when the popup is clicked
    private fun deletePassword(position: Int){
        items.removeAt(position)
        adapter.notifyDataSetChanged()
        saveJson()
        Toast.makeText(this, "Deleted selected item", Toast.LENGTH_SHORT).show()
    }

    // Loads the data for the listView from the internal data file. Data file is constant.
    private fun loadJson(context: Context): String {
        val file = File(context.filesDir, "data.json")
        return if (file.exists()){
            file.bufferedReader().use {it.readText()}
        }
        else{
            context.assets.open("data.json").bufferedReader().use {it.readText()}
        }
    }

    // Function parses the json file that contains a password list
    private fun parseJson(json: String): ArrayList<Item> {
        val itemType = object : TypeToken<List<Item>>() {}.type
        return Gson().fromJson(json, itemType)
    }

    // Function saves the modified item list to the internal file
    private fun saveJson(){
        val jsonArray = Gson().toJson(items)
        val filu = openFileOutput("data.json", MODE_PRIVATE)
        filu.write(jsonArray.toByteArray())
        filu.close()
    }

    // Function copies clicked password to clipboard
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)
    }

    // Function encrypts added password with AES and adds it to the list.
    private fun aesEncrypt(passname: String, newpassword:String){

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = getKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val result = cipher.doFinal(newpassword.toByteArray())
        val resultBase64 = Base64.encodeToString(result, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)

        val newItem = Item(name = passname, value = resultBase64, iv = ivBase64)
        Log.d("aesEncrypt","The result: $resultBase64")
        items.add(newItem)
        adapter.notifyDataSetChanged()
        saveJson()
    }

    // Function decrypts the chosen password when clicked and returns the cleartext
    private fun aesDecrypt(password: String, ivB64:String): String{
        val secretKey = getKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = Base64.decode(ivB64, Base64.NO_WRAP)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val encrypted = Base64.decode(password, Base64.NO_WRAP)
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    // Function fetches the encryption key from Android Keystore
    private fun getKey(): SecretKey{
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val keyEntry = keyStore.getEntry("AESKey", null) as KeyStore.SecretKeyEntry
        return keyEntry.secretKey
    }

}
