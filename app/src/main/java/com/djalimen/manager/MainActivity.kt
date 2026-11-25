package com.djalimen.manager

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.djalimen.manager.core.ShellExecutor
import com.djalimen.manager.core.PluginManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

private val DjCyan = Color(0xFF22D3EE)
private val DjIndigo = Color(0xFF6366F1)
private val DjEmerald = Color(0xFF34D399)
private val DjRose = Color(0xFFFB7185)
private val DjAmber = Color(0xFFFBBF24)
private val DjBgDark = Color(0xFF090D16)
private val DjSurface = Color(0xFF111827)
private val DjSurfaceElevated = Color(0xFF1A2334)
private val DjBorder = Color(0xFF283548)
private val DjTextPrimary = Color(0xFFF8FAFC)
private val DjTextMuted = Color(0xFF94A3B8)

private val DjDarkColorScheme = darkColorScheme(
    primary = DjCyan,
    onPrimary = Color(0xFF042F38),
    primaryContainer = Color(0xFF0E3A47),
    onPrimaryContainer = Color(0xFFCFFAFE),
    secondary = DjIndigo,
    onSecondary = Color.White,
    background = DjBgDark,
    onBackground = DjTextPrimary,
    surface = DjSurface,
    onSurface = DjTextPrimary,
    surfaceVariant = DjSurfaceElevated,
    onSurfaceVariant = DjTextMuted,
    outline = DjBorder
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = DjDarkColorScheme) {
                DjalimenMainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DjalimenMainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Beranda", "Aplikasi", "QuickShell", "Pengaturan", "WebUI")

    Scaffold(
        containerColor = DjBgDark,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DjBgDark,
                    titleContentColor = DjTextPrimary
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_djalimen_logo),
                            contentDescription = "Logo Djalimen Manager",
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Column {
                            Text(
                                text = "Djalimen Manager",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = DjTextPrimary
                            )
                            Text(
                                text = "Pusat Kontrol Sistem & Modul • v1.3",
                                fontSize = 11.sp,
                                color = DjTextMuted
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DjSurface,
                tonalElevation = 0.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    val icon = when (index) {
                        0 -> Icons.Default.Dashboard
                        1 -> Icons.Default.Apps
                        2 -> Icons.Default.Terminal
                        3 -> Icons.Default.Tune
                        else -> Icons.Default.Extension
                    }
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = title) },
                        label = { Text(title, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DjCyan,
                            selectedTextColor = DjCyan,
                            indicatorColor = DjCyan.copy(alpha = 0.14f),
                            unselectedIconColor = DjTextMuted,
                            unselectedTextColor = DjTextMuted
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(DjBgDark)
        ) {
            when (selectedTab) {
                0 -> BerandaTab()
                1 -> AplikasiTab()
                2 -> QuickShellTab()
                3 -> PengaturanTab()
                4 -> WebUIPluginTab()
            }
        }
    }
}

@Composable
fun BerandaTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(ShellExecutor.ServerStatus(false, -1, "Memeriksa...")) }
    var modeSiluman by remember { mutableStateOf(true) }
    var sedangMemuat by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        status = ShellExecutor.checkStatus()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            val heroGradient = if (status.isRunning) {
                Brush.linearGradient(listOf(Color(0xFF0B2E26), Color(0xFF10253A)))
            } else {
                Brush.linearGradient(listOf(Color(0xFF2D131D), Color(0xFF191B2E)))
            }
            val accentColor = if (status.isRunning) DjEmerald else DjRose

            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .background(heroGradient)
                        .padding(18.dp)
                        .fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                                Text(
                                    text = if (status.isRunning) "Layanan Djalimen Aktif" else "Layanan Djalimen Nonaktif",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = DjTextPrimary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = accentColor.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = if (status.isRunning) "TERHUBUNG" else "OFFLINE",
                                    color = accentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "Level Akses: ${status.mode}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = accentColor
                        )
                        Text(
                            text = "Paket: com.djalimen.manager • Akses Shell melalui Shizuku atau Root.",
                            fontSize = 12.sp,
                            color = DjTextMuted
                        )
                    }
                }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatMiniCard(
                    title = "Mode Akses",
                    value = if (status.uid == 0) "ROOT" else if (status.uid == 2000) "ADB/SHELL" else "STANDBY",
                    accent = DjCyan,
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "Akses Sistem",
                    value = if (status.isRunning) "AKTIF" else "BELUM AKTIF",
                    accent = DjAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, DjBorder),
                colors = CardDefaults.cardColors(containerColor = DjSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Koneksi Akses Sistem",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DjTextPrimary
                    )
                    Text(
                        text = "Gunakan Shizuku atau Root untuk memberi akses Shell. Brevent dapat menjalankan perintah dari aplikasinya sendiri, tetapi tidak menyediakan API umum untuk membagikan sesi Shell ke aplikasi lain.",
                        fontSize = 12.sp,
                        color = DjTextMuted
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                sedangMemuat = true
                                val ok = ShellExecutor.startWithRoot(context)
                                status = ShellExecutor.checkStatus()
                                sedangMemuat = false
                                Toast.makeText(
                                    context,
                                    if (ok) "Akses Root terdeteksi dan siap digunakan." else "Akses Root tidak tersedia.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = !sedangMemuat,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DjCyan,
                            contentColor = Color(0xFF042F38)
                        ),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (sedangMemuat) "Memeriksa..." else "Periksa Akses Root (su)",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val info = ShellExecutor.requestShizukuPermission()
                            Toast.makeText(context, info, Toast.LENGTH_LONG).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DjBorder),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = DjCyan, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Hubungkan / Minta Izin Shizuku", color = DjTextPrimary)
                    }

                    TextButton(
                        onClick = {
                            scope.launch {
                                status = ShellExecutor.checkStatus()
                                Toast.makeText(context, "Status diperbarui", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = DjCyan, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Perbarui Status", color = DjCyan)
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, DjBorder),
                colors = CardDefaults.cardColors(containerColor = DjSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Mode Siluman DJ-Stealth",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DjTextPrimary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Lindungi komunikasi soket dan batasi akses eksternal yang tidak sah.",
                            fontSize = 12.sp,
                            color = DjTextMuted
                        )
                    }
                    Switch(
                        checked = modeSiluman,
                        onCheckedChange = {
                            modeSiluman = it
                            Toast.makeText(
                                context,
                                if (it) "Mode Siluman diaktifkan" else "Mode Siluman dinonaktifkan",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF042F38),
                            checkedTrackColor = DjCyan
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun StatMiniCard(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DjBorder),
        colors = CardDefaults.cardColors(containerColor = DjSurface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, fontSize = 11.sp, color = DjTextMuted)
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

@Composable
fun AplikasiTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var kataKunci by remember { mutableStateOf("") }
    val daftarAplikasi = remember {
        val pm = context.packageManager
        pm.getInstalledApplications(0)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map { pm.getApplicationLabel(it).toString() to it.packageName }
            .sortedBy { it.first.lowercase() }
    }

    val hasilFilter = remember(kataKunci, daftarAplikasi) {
        daftarAplikasi.filter {
            it.first.contains(kataKunci, ignoreCase = true) ||
                it.second.contains(kataKunci, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = kataKunci,
            onValueChange = { kataKunci = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DjCyan) },
            placeholder = { Text("Cari nama aplikasi atau nama paket...", color = DjTextMuted) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DjCyan,
                unfocusedBorderColor = DjBorder,
                focusedContainerColor = DjSurface,
                unfocusedContainerColor = DjSurface,
                focusedTextColor = DjTextPrimary,
                unfocusedTextColor = DjTextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Menampilkan ${hasilFilter.size} aplikasi pengguna",
            fontSize = 12.sp,
            color = DjTextMuted
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(hasilFilter, key = { it.second }) { (namaApp, namaPaket) ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, DjBorder),
                    colors = CardDefaults.cardColors(containerColor = DjSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column {
                            Text(
                                text = namaApp,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = DjTextPrimary
                            )
                            Text(
                                text = namaPaket,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = DjTextMuted
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = {
                                    scope.launch {
                                        val hasil = ShellExecutor.runCommand("am force-stop $namaPaket")
                                        Toast.makeText(context, "Paksa berhenti: $namaApp $hasil", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                label = { Text("Paksa Berhenti", fontSize = 12.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = DjSurfaceElevated,
                                    labelColor = DjRose
                                ),
                                border = BorderStroke(1.dp, DjRose.copy(alpha = 0.35f))
                            )
                            AssistChip(
                                onClick = {
                                    scope.launch {
                                        val hasil = ShellExecutor.runCommand("pm disable-user --user 0 $namaPaket")
                                        Toast.makeText(context, "Bekukan: $hasil", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                label = { Text("Bekukan", fontSize = 12.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = DjSurfaceElevated,
                                    labelColor = DjCyan
                                ),
                                border = BorderStroke(1.dp, DjCyan.copy(alpha = 0.35f))
                            )
                            AssistChip(
                                onClick = {
                                    scope.launch {
                                        val hasil = ShellExecutor.runCommand("cmd appops set $namaPaket RUN_IN_BACKGROUND ignore")
                                        Toast.makeText(context, "Batasi latar belakang: $hasil", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                label = { Text("Batasi Latar", fontSize = 12.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = DjSurfaceElevated,
                                    labelColor = DjAmber
                                ),
                                border = BorderStroke(1.dp, DjAmber.copy(alpha = 0.35f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickShellTab() {
    val scope = rememberCoroutineScope()
    var perintah by remember { mutableStateOf("id && uname -a") }
    var keluaran by remember {
        mutableStateOf("Terminal QuickShell Djalimen siap digunakan.\nKetik perintah lalu tekan Jalankan.\n")
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = perintah,
                onValueChange = { perintah = it },
                label = { Text("Perintah Shell") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DjCyan,
                    unfocusedBorderColor = DjBorder,
                    focusedContainerColor = DjSurface,
                    unfocusedContainerColor = DjSurface,
                    focusedTextColor = DjTextPrimary,
                    unfocusedTextColor = DjTextPrimary
                ),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val cmdAktif = perintah.trim()
                    if (cmdAktif.isNotEmpty()) {
                        scope.launch {
                            val hasil = ShellExecutor.runCommand(cmdAktif)
                            keluaran = "$keluaran\n$ $cmdAktif\n$hasil\n"
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DjCyan,
                    contentColor = Color(0xFF042F38)
                ),
                modifier = Modifier.height(54.dp)
            ) {
                Text("Jalankan", fontWeight = FontWeight.Bold)
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DjBorder),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF05080F)),
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(14.dp)
            ) {
                item {
                    Text(
                        text = keluaran,
                        color = DjEmerald,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PengaturanTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tabel by remember { mutableStateOf("global") }
    var kunci by remember { mutableStateOf("animator_duration_scale") }
    var nilai by remember { mutableStateOf("0.5") }
    var tampilTentang by remember { mutableStateOf(false) }
    var tampilLisensi by remember { mutableStateOf(false) }
    val versiAplikasi = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "1.4.1"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, DjBorder),
                colors = CardDefaults.cardColors(containerColor = DjSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Editor Pengaturan Sistem",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DjTextPrimary
                    )
                    Text(
                        text = "Baca atau ubah nilai parameter pada ruang penyimpanan System, Secure, dan Global.",
                        fontSize = 12.sp,
                        color = DjTextMuted
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system", "secure", "global").forEach { namaTabel ->
                            FilterChip(
                                selected = tabel == namaTabel,
                                onClick = { tabel = namaTabel },
                                label = { Text(namaTabel.uppercase(), fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DjCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = DjCyan,
                                    containerColor = DjSurfaceElevated,
                                    labelColor = DjTextMuted
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = kunci,
                        onValueChange = { kunci = it },
                        label = { Text("Nama Kunci (Key)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DjCyan,
                            unfocusedBorderColor = DjBorder,
                            focusedTextColor = DjTextPrimary,
                            unfocusedTextColor = DjTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = nilai,
                        onValueChange = { nilai = it },
                        label = { Text("Nilai (Value)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DjCyan,
                            unfocusedBorderColor = DjBorder,
                            focusedTextColor = DjTextPrimary,
                            unfocusedTextColor = DjTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    if (tabel !in setOf("system", "secure", "global") || !kunci.matches(Regex("[A-Za-z0-9_.]+"))) {
                                        Toast.makeText(context, "Tabel atau nama key tidak valid.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val saatIni = ShellExecutor.runCommand("settings get $tabel $kunci")
                                        nilai = saatIni
                                        Toast.makeText(context, "Nilai dibaca. Periksa hasil sebelum menyimpan.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DjBorder),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("Baca Nilai", color = DjTextPrimary)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    if (tabel !in setOf("system", "secure", "global") ||
                                        !kunci.matches(Regex("[A-Za-z0-9_.]+")) || nilai.contains('\n') || nilai.contains('\r')) {
                                        Toast.makeText(context, "Input tidak valid. Periksa tabel, key, dan value.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val safeValue = "'" + nilai.replace("'", "'\\''") + "'"
                                        val hasil = ShellExecutor.runCommand("settings put $tabel $kunci $safeValue")
                                        Toast.makeText(context, hasil.take(180), Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DjCyan,
                                contentColor = Color(0xFF042F38)
                            ),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("Simpan Nilai", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, DjBorder),
                colors = CardDefaults.cardColors(containerColor = DjSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = DjCyan)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tentang Aplikasi", color = DjTextPrimary, fontWeight = FontWeight.Bold)
                            Text("Djalimen Manager • v$versiAplikasi", color = DjTextMuted, fontSize = 12.sp)
                        }
                    }
                    Text(
                        "Informasi aplikasi, identitas pengembang, hak cipta, dan ketentuan lisensi.",
                        color = DjTextMuted,
                        fontSize = 12.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { tampilTentang = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DjCyan,
                                contentColor = Color(0xFF042F38)
                            )
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Tentang")
                        }
                        OutlinedButton(
                            onClick = { tampilLisensi = true },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, DjBorder)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = DjCyan)
                            Spacer(Modifier.width(6.dp))
                            Text("Lisensi", color = DjTextPrimary)
                        }
                    }
                }
            }
        }
    }

    if (tampilTentang) {
        AlertDialog(
            onDismissRequest = { tampilTentang = false },
            containerColor = DjSurface,
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = DjCyan, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("DJALIMEN MANAGER", color = DjTextPrimary, fontWeight = FontWeight.Bold)
                    Text("Developed by DJALIMEN", color = DjCyan, fontSize = 13.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Informasi Aplikasi", color = DjCyan, fontWeight = FontWeight.Bold)
                    Text("Nama: Djalimen Manager", color = DjTextPrimary)
                    Text("Versi: $versiAplikasi", color = DjTextPrimary)
                    Text("Pengembang: DJALIMEN", color = DjTextPrimary)
                    Text("Hak cipta: © 2026 DJALIMEN", color = DjTextPrimary)
                    Text("Proyek independen. Komponen pihak ketiga tetap mengikuti lisensi masing-masing.", color = DjTextMuted, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { tampilTentang = false }) {
                    Text("Tutup", color = DjCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { tampilTentang = false; tampilLisensi = true }) {
                    Text("Lihat Lisensi", color = DjCyan)
                }
            }
        )
    }

    if (tampilLisensi) {
        AlertDialog(
            onDismissRequest = { tampilLisensi = false },
            containerColor = DjSurface,
            title = { Text("Lisensi & Hak Cipta", color = DjTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DJALIMEN PROPRIETARY LICENSE", color = DjCyan, fontWeight = FontWeight.Bold)
                    Text("Copyright © 2026 DJALIMEN. All Rights Reserved.", color = DjTextPrimary)
                    Text("Aplikasi ini dikembangkan oleh DJALIMEN. Penggunaan pribadi diperbolehkan. Dilarang mengklaim kepemilikan atau mendistribusikan ulang aplikasi sebagai karya sendiri tanpa izin tertulis.", color = DjTextPrimary)
                    Text("Kode, library, dan aset pihak ketiga tetap tunduk pada lisensi masing-masing. Lisensi ini tidak mengubah hak pihak ketiga.", color = DjTextMuted, fontSize = 12.sp)
                    Text("Developed by DJALIMEN • Official Project: DJALIMEN MANAGER", color = DjCyan, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { tampilLisensi = false }) {
                    Text("Mengerti", color = DjCyan)
                }
            }
        )
    }
}

private fun ekstrakDanCariSkripZip(context: Context, uri: Uri): Pair<String, File?> {
    val baseDir = File(context.filesDir, "djalimen_plugins").apply { mkdirs() }
    val namaBersih = (uri.lastPathSegment ?: "modul_plugin")
        .substringAfterLast('/')
        .substringAfterLast(':')
        .removeSuffix(".zip")
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .ifBlank { "modul_plugin" }

    val targetDir = File(baseDir, "${namaBersih}_${System.currentTimeMillis()}").apply { mkdirs() }

    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                if (!outFile.canonicalPath.startsWith(targetDir.canonicalPath + File.separator)) {
                    throw SecurityException("Zip Slip terdeteksi: ${entry.name}")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        zip.copyTo(fos)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    } ?: return "Gagal membuka file ZIP dari penyimpanan lokal." to null

    val semuaSkrip = targetDir.walkTopDown()
        .filter { it.isFile && it.extension.equals("sh", ignoreCase = true) }
        .toList()

    val skripUtama = semuaSkrip.firstOrNull { it.name.equals("action.sh", ignoreCase = true) }
        ?: semuaSkrip.firstOrNull { it.name.equals("service.sh", ignoreCase = true) }
        ?: semuaSkrip.firstOrNull()

    skripUtama?.setReadable(true, false)
    skripUtama?.setExecutable(true, false)

    val ringkasan = buildString {
        appendLine("Terekstrak ke: ${targetDir.absolutePath}")
        appendLine("Ditemukan ${semuaSkrip.size} file skrip (.sh).")
        if (skripUtama != null) {
            appendLine("Menjalankan skrip: ${skripUtama.name}")
        } else {
            appendLine("Tidak ada file .sh di dalam ZIP.")
        }
    }
    return ringkasan to skripUtama
}

@Composable
fun WebUIPluginTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var plugins by remember { mutableStateOf(PluginManager.list(context)) }
    var log by remember { mutableStateOf("Belum ada plugin terpasang. Impor ZIP untuk memulai.") }
    var selectedWeb by remember { mutableStateOf<File?>(null) }
    var busy by remember { mutableStateOf(false) }

    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                val plugin = PluginManager.install(context, uri)
                plugins = PluginManager.list(context)
                log = "Plugin ${plugin.name} (${plugin.version}) berhasil dipasang. Tidak ada skrip yang dijalankan otomatis."
            } catch (e: Exception) {
                log = "Gagal memasang plugin: ${e.message}"
            } finally { busy = false }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = DjSurface), border = BorderStroke(1.dp, DjBorder)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Plugin Manager & WebUI", color = DjTextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("Pasang plugin ZIP, periksa manifest, jalankan skrip secara manual, atau buka halaman WebUI lokal.", color = DjTextMuted, fontSize = 12.sp)
                    Button(onClick = { zipPicker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(if (busy) "Memasang..." else "Impor Plugin ZIP")
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = DjSurface), border = BorderStroke(1.dp, DjBorder)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("Log", color = DjCyan, fontWeight = FontWeight.Bold)
                    Text(log, color = DjTextMuted, fontSize = 12.sp)
                }
            }
        }
        item { Text("PLUGIN TERPASANG (${plugins.size})", color = DjCyan, fontWeight = FontWeight.Bold) }
        if (plugins.isEmpty()) item {
            Text("Belum ada plugin. Format yang didukung: ZIP dengan plugin.json, skrip entry .sh opsional, dan index.html opsional untuk WebUI.", color = DjTextMuted, fontSize = 12.sp)
        }
        items(plugins, key = { it.id }) { plugin ->
            Card(colors = CardDefaults.cardColors(containerColor = DjSurface), border = BorderStroke(1.dp, DjBorder)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(plugin.name, color = DjTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Versi ${plugin.version} • ${plugin.description}", color = DjTextMuted, fontSize = 12.sp)
                    Text("Skrip: ${plugin.entryScript?.name ?: "tidak ada"}  |  WebUI: ${if (plugin.webEntry != null) "tersedia" else "tidak ada"}", color = DjTextMuted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (plugin.entryScript != null) Button(onClick = {
                            scope.launch {
                                busy = true
                                val path = plugin.entryScript.absolutePath.replace("'", "'\\''")
                                log = "Menjalankan ${plugin.name}...\n" + ShellExecutor.runCommand("sh '$path'")
                                busy = false
                            }
                        }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Jalankan") }
                        if (plugin.webEntry != null) OutlinedButton(onClick = { selectedWeb = plugin.webEntry }, modifier = Modifier.weight(1f)) { Text("Buka WebUI") }
                        OutlinedButton(onClick = {
                            if (PluginManager.remove(context, plugin)) {
                                plugins = PluginManager.list(context); log = "Plugin ${plugin.name} dihapus."; if (selectedWeb?.parentFile == plugin.directory) selectedWeb = null
                            } else log = "Gagal menghapus plugin."
                        }) { Icon(Icons.Default.Delete, contentDescription = "Hapus plugin") }
                    }
                }
            }
        }
        if (selectedWeb != null) item {
            Text("WEBUI LOKAL: ${selectedWeb?.name}", color = DjCyan, fontWeight = FontWeight.Bold)
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().height(420.dp)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx -> android.webkit.WebView(ctx).apply {
                        settings.javaScriptEnabled = false
                        settings.allowFileAccess = true
                        settings.allowContentAccess = false
                        settings.allowFileAccessFromFileURLs = false
                        settings.allowUniversalAccessFromFileURLs = false
                        loadUrl("file://${selectedWeb!!.absolutePath}")
                    } },
                    update = { view -> if (view.url != "file://${selectedWeb!!.absolutePath}") view.loadUrl("file://${selectedWeb!!.absolutePath}") }
                )
            }
            Text("JavaScript dinonaktifkan pada WebUI lokal untuk membatasi akses halaman plugin ke aplikasi.", color = DjTextMuted, fontSize = 11.sp)
        }
    }
}
