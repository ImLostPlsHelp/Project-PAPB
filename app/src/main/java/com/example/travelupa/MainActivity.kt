package com.example.travelupa

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Space
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.travelupa.ui.theme.TravelupaTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.request.ImageRequest
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            TravelupaTheme {
                Surface (
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register") // Tambahkan route untuk registrasi
    object RekomendasiTempat : Screen("rekomendasi_tempat")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        // Login Screen
        composable(route = Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.RekomendasiTempat.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        // Register Screen
        composable(route = Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.popBackStack()
                }
            )
        }

        // Rekomendasi Tempat Screen
        composable(route = Screen.RekomendasiTempat.route) {
            RekomendasiTempatScreen()
        }
    }
}

@Composable
fun LoginScreen (
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit // Tambahkan parameter navigasi ke registrasi
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField (
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField (
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (email.isBlank() || password.isBlank()) {
                errorMessage = "Please enter email and password"
                return@Button
            }
            isLoading = true
            errorMessage = null
            coroutineScope.launch {
                try {
                    //Firebase Authentication
                    val authResult = withContext(Dispatchers.IO) {
                        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).await()
                    }
                    isLoading = false
                    onLoginSuccess()
                } catch (e: Exception) {
                    isLoading = false
                    errorMessage = "Login failed: ${e.localizedMessage}"
                }
            }
        },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White
                )
            } else {
                Text("Login")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onNavigateToRegister, // Navigasi ke halaman registrasi
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Belum punya akun? Daftar di sini")
        }

        errorMessage?.let {
            Text(
                text = it,
                color = Color.Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun RegisterScreen (
    onRegisterSuccess: () -> Unit // Tambahkan parameter untuk kembali ke login setelah registrasi
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField (
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField (
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (email.isBlank() || password.isBlank()) {
                errorMessage = "Please enter email and password"
                return@Button
            }
            isLoading = true
            errorMessage = null
            coroutineScope.launch {
                try {
                    //Firebase Authentication
                    val authResult = withContext(Dispatchers.IO) {
                        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).await()
                    }
                    isLoading = false
                    onRegisterSuccess()
                } catch (e: Exception) {
                    isLoading = false
                    errorMessage = "Registration failed: ${e.localizedMessage}"
                }
            }
        },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White
                )
            } else {
                Text("Register")
            }
        }

        errorMessage?.let {
            Text(
                text = it,
                color = Color.Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun Greeting() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
        ) {
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text (
                    text = "Selamat Datang di Travelupa!",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Solusi buat kamu yang lupa kemana-mana",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

        Button(
            onClick = { /*TODO*/ },
            modifier = Modifier
                .width(360.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
            ) {
            Text(text = "Mulai")
        }
    }
}

data class TempatWisata(
    val nama: String = "",
    val deskripsi: String = "",
    val gambarUriString: String? = null,
    val gambarResId: Int? = null
)

val daftarTempatWisata = listOf(
    TempatWisata(
        "Tumpak Sewu",
        "Air terjun tercantik di Jawa Timur.",
        gambarResId = R. drawable.tumpak_sewu),
    TempatWisata(
        "Gunung Bromo",
        "Matahari terbitnya bagus banget.",
        gambarResId = R.drawable.bromo)
    )

@Composable
fun RekomendasiTempatScreen() {
    var daftarTempatWisata by remember { mutableStateOf(listOf(
        TempatWisata("Tumpak Sewu",
        "Air terjun tercantik di Jawa Timur.", gambarResId = R.drawable.tumpak_sewu)))}

    var showTambahDialog by remember { mutableStateOf(false) }

    Scaffold (
        floatingActionButton = {
            FloatingActionButton(onClick = { showTambahDialog = true },
                containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah Tempat Wisata")
            }
        }
    ) { paddingValues ->
        Column (
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            LazyColumn {
                items(daftarTempatWisata) { tempat ->
                    TempatItemEditable(
                        tempat = tempat,
                        onDelete = {
                            daftarTempatWisata = daftarTempatWisata.filter {it != tempat }
                        }
                    )
                }
            }
        }

        if (showTambahDialog) {
            TambahTempatWisataDialog(
                firestore = FirebaseFirestore.getInstance(), // Menambahkan instance Firestore
                context = LocalContext.current, // Menambahkan context dari Compose
                onDismiss = { showTambahDialog = false },
                onTambah = { nama, deskripsi, gambarUri ->
                    val uriString = gambarUri?.toString() ?: ""
                    val nuevoTempat = TempatWisata(nama, deskripsi, uriString)
                    daftarTempatWisata = daftarTempatWisata + nuevoTempat
                    showTambahDialog = false
                }
            )
        }

    }
}

@Composable
fun TambahTempatWisataDialog(
    firestore: FirebaseFirestore,
    context: Context,
    onDismiss: () -> Unit,
    onTambah: (String, String, String?) -> Unit
) {
    var nama by remember { mutableStateOf("") }
    var deskripsi by remember { mutableStateOf("") }
    var gambarUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val gambarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        gambarUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Tempat Wisata Baru") },
        text = {
            Column {
                TextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Tempat") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = deskripsi,
                    onValueChange = { deskripsi = it },
                    label = { Text("Deskripsi") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading
                )
                Spacer(modifier = Modifier.height(8.dp))
                gambarUri?.let { uri ->
                    Image(
                        painter = rememberAsyncImagePainter(model = uri),
                        contentDescription = "Gambar yang dipilih",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { gambarLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading
                ) {
                    Text("Pilih Gambar")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nama.isNotBlank() && deskripsi.isNotBlank()) {
                        isUploading = true
                        gambarUri?.let { uri ->
                            val localPath = saveImageLocally(context, uri)
                            if (localPath != null) {
                                saveDataToFirestore(
                                    firestore,
                                    context, // Tambahkan context di sini
                                    nama,
                                    deskripsi,
                                    localPath,
                                    onSuccess = {
                                        isUploading = false
                                        onTambah(nama, deskripsi, localPath)
                                        onDismiss()
                                    },
                                    onFailure = {
                                        isUploading = false
                                    }
                                )
                            } else {
                                isUploading = false
                                Toast.makeText(context, "Gagal menyimpan gambar secara lokal", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text("Tambah")
                }
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                enabled = !isUploading
            ) {
                Text("Batal")
            }
        }
    )
}

fun saveDataToFirestore(
    firestore: FirebaseFirestore,
    context: Context, // Tambahkan parameter context
    nama: String,
    deskripsi: String,
    localImagePath: String,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    val tempatWisataData = mapOf(
        "nama" to nama,
        "deskripsi" to deskripsi,
        "localImagePath" to localImagePath
    )

    firestore.collection("tempat_wisata")
        .add(tempatWisataData)
        .addOnSuccessListener {
            Toast.makeText(context, "Data berhasil disimpan!", Toast.LENGTH_SHORT).show() // Gunakan context
            onSuccess()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Gagal menyimpan data: ${e.message}", Toast.LENGTH_SHORT).show() // Gunakan context
            onFailure()
        }
}

fun saveImageLocally(context: Context, imageUri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val file = File(context.filesDir, "${UUID.randomUUID()}.jpg")
        val outputStream = file.outputStream()
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        file.absolutePath // Mengembalikan path file yang disimpan
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}



@Composable
fun GambarPicker(
    gambarUri: Uri?,
    onPilihGambar: () -> Unit
) {
    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        //tampilkan gambar jika sudah dipilih
        gambarUri?.let { uri ->
            Image(painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current)
                    .data(uri)
                    .build()
            ),
                contentDescription = "Gambar Tempat Wisata",
                modifier = Modifier
                    .size(200.dp)
                    .clickable { onPilihGambar() },
                contentScale = ContentScale.Crop
            )
        } ?: run {
            OutlinedButton(
                onClick = onPilihGambar,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Pilih Gambar")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pilih Gambar")
            }
        }
    }
}

@Composable
fun TempatItemEditable(
    tempat: TempatWisata,
    onDelete: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.fillMaxWidth(1f)) {
                    Image(
                        painter = tempat.gambarUriString?.let { uriString ->
                            rememberAsyncImagePainter(
                                ImageRequest.Builder(LocalContext.current)
                                    .data(Uri.parse(uriString))
                                    .build()
                            )
                        } ?: tempat.gambarResId?.let {
                            painterResource(id = it)
                        } ?: painterResource(id = R.drawable.bromo),
                        contentDescription = tempat.nama,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                        ) {
                            Text(
                                text = tempat.nama,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                text = tempat.deskripsi,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = { expanded = true },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            offset = DpOffset(250.dp, 0.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    expanded = false
                                    firestore.collection("tempat_wisata")
                                        .document(tempat.nama)
                                        .delete()
                                        .addOnSuccessListener {
                                            onDelete()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w("TempatItemEditable", "Error deleting document", e)
                                        }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun TempatItem(tempat: TempatWisata) {
    Card (
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column (modifier = Modifier.padding(16.dp)
        ) {
            Image (
                painter = tempat.gambarUriString?.let { uriString ->
                    rememberAsyncImagePainter(ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(uriString))
                        .build()
                    )
                } ?: tempat.gambarResId?.let {
                    painterResource(id = it)
                } ?: painterResource(id = R.drawable.bromo),
                contentDescription = tempat.nama,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
            Text (
                text = tempat.nama,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp, top = 12.dp)
            )
            Text(
                text = tempat.deskripsi,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp, top = 12.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TravelupaTheme {
        Greeting()
    }
}