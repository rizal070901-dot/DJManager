# Djalimen Manager 1.4.0

Aplikasi Android independen untuk mengelola aplikasi, menjalankan perintah shell yang diizinkan, dan mengelola plugin lokal/WebUI. Proyek ini terinspirasi oleh kategori fitur AxManager; bukan salinan dan tidak berafiliasi dengan AxManager.

## Perubahan 1.4.0
- Plugin Manager lokal: impor, daftar, jalankan manual, dan hapus plugin ZIP.
- Impor plugin tidak pernah mengeksekusi skrip secara otomatis.
- Validasi ZIP terhadap Zip Slip, batas 500 entri, dan batas ukuran ekstrak 32 MB.
- Dukungan `plugin.json` untuk nama, versi, deskripsi, skrip entry, dan halaman WebUI.
- WebUI lokal berbasis WebView dengan JavaScript dan akses lintas-file dinonaktifkan.
- Settings Editor memvalidasi tabel/key dan mengutip value agar tidak menjadi injeksi shell.

## Perubahan 1.3.1
- Menyiapkan alur publish dari Termux ke GitHub menggunakan GitHub CLI.
- Menambahkan skrip untuk mengunduh APK hasil build GitHub Actions.
- Build APK dipindahkan ke GitHub Actions agar Android SDK host tools tidak perlu dijalankan langsung di Android/Termux.
- Mempertahankan penanganan QuickShell dengan pembacaan stdout/stderr paralel dan batas waktu perintah.
- Memperjelas status koneksi Shizuku/Root. Brevent belum memiliki integrasi langsung di aplikasi ini.

## Cara dari Termux ke GitHub

1. Ekstrak ZIP proyek ke folder yang bisa diakses Termux.
2. Buka Termux, masuk ke folder proyek. Contoh:
   ```sh
   termux-setup-storage
   cd ~/storage/shared/Download/djalimen_fixed
   ```
3. Jalankan:
   ```sh
   chmod +x termux_setup_build.sh termux_download_apk.sh
   ./termux_setup_build.sh
   ```
4. Login GitHub ketika diminta, lalu masukkan nama repo `username/DjalimenManager`. Script akan membuat repo jika belum ada atau push ke repo yang sudah ada.
5. Buka GitHub → repository → Actions. Workflow `Build Djalimen Manager APK` berjalan otomatis setelah push.
6. Setelah status workflow sukses, unduh APK dari Termux:
   ```sh
   ./termux_download_apk.sh
   ```
   Artifact akan tersimpan di folder `apk-hasil`.

## Build manual di komputer

Gunakan JDK 17, Android SDK Platform 35, Build Tools 35.0.0, dan Gradle 8.7:

```sh
gradle assembleDebug --no-daemon
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`.

## Akses sistem

- Shizuku: jalankan layanan Shizuku dan berikan izin kepada Djalimen Manager.
- Root: memerlukan root manager yang sudah terpasang dan persetujuan pengguna.
- Brevent: tidak ada API publik generik yang membuat aplikasi pihak ketiga otomatis memakai sesi shell Brevent. Karena itu jangan menganggap tombol/panduan Brevent sebagai integrasi langsung.

Perintah QuickShell berjalan melalui Shizuku yang telah diberi izin atau `su` yang disetujui pengguna. Selalu periksa perintah sebelum menjalankannya.

## Batasan saat ini

Ini belum setara penuh dengan AxManager. Plugin Manager, Privilege Manager, WebUI, dan Settings Editor masih perlu implementasi serta pengujian perangkat nyata. Workflow GitHub hanya membuktikan bahwa APK berhasil dikompilasi; bukan bahwa seluruh fitur perangkat telah lolos uji.


## Format plugin Djalimen
Satu ZIP plugin dapat berisi `plugin.json`, skrip shell opsional, dan halaman `index.html` opsional. Contoh manifest:

```json
{
  "name": "Contoh Plugin",
  "version": "1.0.0",
  "description": "Contoh plugin lokal",
  "entry": "action.sh",
  "web": "index.html"
}
```

- `entry` menunjuk skrip `.sh` di dalam direktori plugin.
- `web` menunjuk halaman HTML lokal.
- Skrip hanya berjalan ketika pengguna menekan tombol Jalankan dan akses Shell tersedia.
- Impor tidak menjalankan skrip. Periksa isi skrip dan sumber plugin sebelum menekan Jalankan.
- WebUI plugin ditampilkan dengan JavaScript nonaktif dan tanpa JavaScript bridge ke aplikasi. Halaman yang membutuhkan JavaScript belum didukung pada mode aman ini.

## Batasan teknis
Djalimen 1.4.0 menambah fondasi manajemen plugin dan WebUI lokal, tetapi belum setara penuh dengan AxManager. Belum ada plugin API sandbox, service daemon permanen, repository plugin bertanda tangan, hot reload, atau integrasi Brevent langsung. Settings Editor tetap bergantung pada hak akses Shell/Root dan pembatasan Android. Build sukses bukan bukti seluruh fungsi sudah diuji di semua perangkat.


## Perubahan 1.4.1
- Menambahkan bagian Tentang Aplikasi pada tab Pengaturan.
- Menampilkan nama aplikasi, versi terpasang, pengembang DJALIMEN, dan hak cipta.
- Menambahkan dialog Lisensi & Hak Cipta di dalam aplikasi.
