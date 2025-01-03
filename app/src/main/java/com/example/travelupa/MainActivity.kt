package com.example.travelupa

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.room.Room
import coil.request.ImageRequest
import coil.compose.rememberAsyncImagePainter
import com.example.travelupa.database.AppDatabase
import com.example.travelupa.database.ImageDao
import com.example.travelupa.ui.theme.ImageEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app_database"
        ).build()
        val imageDao = database.imageDao()
        val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
        setContent {
            TravelupaTheme {
                Surface (
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    AppNavigation(currentUser, imageDao)
                }
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register") // Tambahkan route untuk registrasi
    object RekomendasiTempat : Screen("rekomendasi_tempat")
    object Greeting : Screen("greeting")
    object Gallery : Screen("gallery")
}

@Composable
fun AppNavigation(
    currentUser: FirebaseUser?,
    imageDao: ImageDao
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (currentUser != null) {
            Screen.RekomendasiTempat.route
        } else {
            Screen.Greeting.route
        }
    ) {

        composable(Screen.Greeting.route) {
            GreetingScreen(
                onStart = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Greeting.route) { inclusive = true }
                    }
                }
            )
        }
        // Tambahkan navigasi ke semua screen
        composable(Screen.Login.route) {
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

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.popBackStack() // Kembali ke halaman login setelah registrasi
                }
            )
        }

        composable(Screen.RekomendasiTempat.route) {
            RekomendasiTempatScreen(
                onBackToLogin = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.RekomendasiTempat.route) { inclusive = true }
                    }
                },
                onGallerySelected = {
                    navController.navigate(Screen.Gallery.route)
                }
            )
        }
        composable(Screen.Gallery.route) {
            GalleryScreen(
                imageDao = imageDao,
                onImageSelected = { uri ->
                    //Handle image selection (ini artinya apa?)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
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
fun GreetingScreen(onStart: () -> Unit) {
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
            onClick = onStart,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RekomendasiTempatScreen(onBackToLogin: () -> Unit, onGallerySelected: () -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val userEmail = FirebaseAuth.getInstance().currentUser?.email
    var daftarTempatWisata by remember { mutableStateOf<List<TempatWisata>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showTambahDialog by remember { mutableStateOf(false) }

    // Load data from Firestore
    LaunchedEffect(Unit) {
        firestore.collection("tempat_wisata")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    errorMessage = "Gagal memuat data: ${exception.localizedMessage}"
                    isLoading = false
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    daftarTempatWisata = snapshot.documents.mapNotNull { doc ->
                        val nama = doc.getString("nama")
                        val deskripsi = doc.getString("deskripsi")
                        val gambarUri = doc.getString("gambarUri")
                        if (nama != null && deskripsi != null) {
                            TempatWisata(nama, deskripsi, gambarUri)
                        } else null
                    }
                }
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rekomendasi Tempat Wisata") },
                actions = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut() // Logout dari Firebase
                        onBackToLogin() // Navigasi kembali ke halaman login
                    }) {
                        Icon(Icons.Filled.Lock, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTambahDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah Tempat Wisata")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Tampilkan email pengguna
            userEmail?.let {
                Text(
                    text = "Logged in as: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Tambahkan tombol navigasi ke GalleryScreen
            Button(
                onClick = onGallerySelected,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text("Lihat Galeri")
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (!errorMessage.isNullOrEmpty()) {
                Text(
                    text = errorMessage ?: "",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn {
                    items(daftarTempatWisata) { tempat ->
                        TempatItemEditable(
                            tempat = tempat,
                            onDelete = {
                                firestore.collection("tempat_wisata").document(tempat.nama)
                                    .delete()
                                    .addOnSuccessListener {
                                        daftarTempatWisata = daftarTempatWisata.filter { it != tempat }
                                    }
                                    .addOnFailureListener {
                                        errorMessage = "Gagal menghapus data: ${it.localizedMessage}"
                                    }
                            }
                        )
                    }
                }
            }
        }

        if (showTambahDialog) {
            TambahTempatWisataDialog(
                firestore = firestore,
                context = LocalContext.current,
                onDismiss = { showTambahDialog = false },
                onTambah = { nama, deskripsi, gambarUri ->
                    val data = mapOf(
                        "nama" to nama,
                        "deskripsi" to deskripsi,
                        "gambarUri" to gambarUri.toString()
                    )
                    firestore.collection("tempat_wisata").document(nama)
                        .set(data)
                        .addOnSuccessListener {
                            showTambahDialog = false
                        }
                        .addOnFailureListener {
                            errorMessage = "Gagal menambahkan data: ${it.localizedMessage}"
                        }
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
                                    localPath, // Kirim path lokal (String)
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
                        } ?: run {
                            isUploading = false
                            Toast.makeText(context, "Gambar belum dipilih", Toast.LENGTH_SHORT).show()
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
    context: Context,
    nama: String,
    deskripsi: String,
    localImagePath: String,
    onSuccess: (TempatWisata) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val db = Room.databaseBuilder(
        context,
        AppDatabase::class.java, "travelupa-database"
    ).build()
    val imageDao = db.imageDao()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Simpan path gambar ke Room Database
            imageDao.insert(ImageEntity(localPath = localImagePath))

            // Buat objek TempatWisata
            val tempatWisata = TempatWisata(
                nama = nama,
                deskripsi = deskripsi,
                gambarUriString = localImagePath
            )

            // Simpan data ke Firestore
            firestore.collection("tempat_wisata")
                .add(tempatWisata)
                .addOnCompleteListener { task ->
                    CoroutineScope(Dispatchers.Main).launch {
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Data berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            // Callback hanya untuk memberi tahu bahwa data sudah berhasil disimpan
                            onSuccess(tempatWisata) // Tidak menambahkan ulang data
                        } else {
                            val exception = task.exception ?: Exception("Gagal menyimpan data ke Firestore.")
                            Toast.makeText(context, "Gagal menyimpan data: ${exception.message}", Toast.LENGTH_SHORT).show()
                            onFailure(exception)
                        }
                    }
                }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                onFailure(e)
            }
        } finally {
            db.close()
        }
    }
}

fun saveImageLocally(context: Context, imageUri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val file = File(context.filesDir, "image_${System.currentTimeMillis()}.jpg")
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath // Kembalikan path gambar yang disimpan
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    imageDao: ImageDao,
    onImageSelected: (Uri) -> Unit,
    onBack: () -> Unit
) {
    val images by imageDao.getAllImages().collectAsState(initial = emptyList())
    var showAddImageDialog by remember { mutableStateOf(false) }
    var selectedImageEntity by remember { mutableStateOf<ImageEntity?>(null) }
    val context = LocalContext.current
    var showDeleteConfirmation by remember { mutableStateOf<ImageEntity?>(null) }

    LaunchedEffect(images) {
        Log.d("GalleryScreen", "Total images: ${images.size}")
        images.forEachIndexed() { index, image -> Log.d("GalleryScreen", "Image $index path: ${image.localPath}")
            val file = File(image.localPath)
            Log.d("GalleryScreen", "File exists: ${file.exists()}, is readable: ${file.canRead()}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddImageDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
                ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Image")
            }
        }
    ) {
        paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.padding(paddingValues)
        ) {
            items(images) { image ->
               Image(
                   painter = rememberAsyncImagePainter(
                       model = image.localPath
                   ),
                   contentDescription = null,
                   modifier = Modifier
                       .size(100.dp)
                       .padding(4.dp)
                       .clickable {
                           selectedImageEntity = image
                           onImageSelected(Uri.parse(image.localPath))
                       },
                   contentScale = ContentScale.Crop
                   )
            }
        }

        if (showAddImageDialog) {
            AddImageDialog(
                onDismiss = { showAddImageDialog = false },
                onImageAdded = { uri ->
                    try {
                        val localPath = saveImageLocally(context, uri)
                        if (localPath != null) {
                            val newImage = ImageEntity(localPath = localPath)
                            CoroutineScope(Dispatchers.IO).launch {
                                imageDao.insert(newImage)
                            }
                            showAddImageDialog = false
                        } else {
                            Log.e("ImageSave", "Failed to save image locally")
                        }
                    } catch (e: Exception) {
                        Log.e("ImageSave", "Failed to save image", e)
                    }
                }

            )
        }

        selectedImageEntity?.let { imageEntity ->
            ImageDetailDialog(
                imageEntity = imageEntity,
                onDismiss = { selectedImageEntity = null },
                onDelete = { imageToDelete ->
                    showDeleteConfirmation = imageToDelete
                }
            )
        }

        showDeleteConfirmation?.let { imageToDelete ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = null },
                title = { Text("Delete Image") },
                text = { Text("Are you sure you want to delete this image?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                imageDao.delete(imageToDelete)
                                val file = File(imageToDelete.localPath)
                                if (file.exists()) {
                                    file.delete()
                                }
                            }
                            showDeleteConfirmation = null
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteConfirmation = null}
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun AddImageDialog(
    onDismiss: () -> Unit,
    onImageAdded: (Uri) -> Unit
) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()
    ) {
        uri: Uri? -> imageUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val uri = saveBitmapToUri(context, it)
            imageUri = uri
        }
    }

    AlertDialog(
        onDismissRequest = { onDismiss },
        title = { Text("Add New Image") },
        text = {
            Column {
                imageUri?.let { uri ->
                    Image(
                        painter = rememberAsyncImagePainter(model = uri),
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(
                        onClick = {
                            imagePickerLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Select from File")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Take Photo")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    imageUri?.let { uri ->
                        onImageAdded(uri)
                    }
                }
            ) {
                Text("Add")
            }
        }
    )
}

@Composable
fun ImageDetailDialog(
    imageEntity: ImageEntity,
    onDismiss: () -> Unit,
    onDelete: (ImageEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Image(
                painter = rememberAsyncImagePainter(model = imageEntity.localPath),
                contentDescription = "Detailed Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
        },
        confirmButton = {
            Row {
                Button(onClick = { onDelete(imageEntity) }) {
                    Text("Delete")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

fun saveBitmapToUri(context: Context, bitmap: Bitmap): Uri {
    val file = File(context.cacheDir, "${UUID.randomUUID()}.jpg")
    val outputStream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    outputStream.close()
    return Uri.fromFile(file)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TravelupaTheme {
        GreetingScreen (
            onStart = {}
        )
    }
}