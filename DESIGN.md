# DESIGN.md - Dota 2 Post-Match Tracker & Party Analyzer

> Catatan: Dokumen arah desain ini disusun dari brief awal pemilik proyek (tema dark mode, utilitas pasca-pertandingan Dota 2, fokus data match & teman mabar) sebagai pondasi awal. Pemilik proyek bebas mengubah palet, tipografi, atau dial sesuai selera brand ke depannya.

## 1. Identity & Personality

- **Produk:** Dota Party Analyzer (Post-Match Tracker & Party Win-Rate Ledger).
- **Pengguna:** Pemain Dota 2 yang baru selesai bermain dan ingin cepat mengecek performa 20 pertandingan terakhir serta memutuskan teman mabar mana yang paling konsisten menang (Best Duo) atau sering kalah bersama (Cursed Duo).
- **Karakter visual:** Utilitarian, lugas, seperti buku skor pertandingan (match ledger). Data dan angka berbicara di depan; tidak ada hiasan marketing, tidak ada kaca buram berlapis, tidak ada efek cahaya neon.
- **Alasan Dark Mode (R-21):** Diminta secara eksplisit dalam spesifikasi proyek dan selaras dengan antarmuka in-game Dota 2 yang gelap sehingga nyaman dilihat langsung setelah keluar dari match.

## 2. Liveliness Dials

| Dial | Level | Penerapan |
| :--- | :---: | :--- |
| **ENERGY** | **2 (Balanced)** | Permukaan gelap padat dengan hirarki angka skor yang tegas dan satu aksen utama (Forge Amber) pada aksi pencarian dan indikator fokus. |
| **RHYTHM** | **2 (Balanced)** | Komposisi tiap bagian berbeda mengikuti bentuk datanya: blok asimetris Profil + Ringkasan 20 Match, pita dua kolom verdict Best/Cursed Duo, daftar Party Analyzer (kartu di mobile, tabel di tablet/desktop), lalu buku skor Recent Matches (kartu di mobile, tabel di desktop). |
| **MOTION** | **1 (Calm)** | Transisi warna halus pada hover, active, dan focus, serta status loading saat form pencarian dikirim. Semua transisi menghormati `prefers-reduced-motion: reduce`. Tidak ada animasi loop tanpa henti. |

## 3. Identity Motif

1. **Scoreboard Split Bar:** Batang pemisah Radiant (Menang) vs Dire (Kalah) yang proporsinya selalu mencerminkan data win rate sungguhan, tidak pernah dekoratif.
2. **Radius Terukur (R-11):** Tiga tingkat sudut sesuai fungsi elemen: `4px` untuk thumbnail hero dan tag status kompak, `8px` untuk tombol dan input form, serta `12px` untuk panel konten utama. Tidak menggunakan bentuk pil penuh di semua elemen.
3. **Tabular Numerals:** Semua angka K/D/A, GPM/XPM, durasi, jumlah game, win rate, dan Match ID menggunakan `tabular-nums` agar lurus vertikal saat dipindai cepat.

## 4. Color Palette (Terverifikasi WCAG AA)

Dua warna netral dasar + satu aksen utama + pasangan semantik Radiant/Dire untuk data menang/kalah:

| Peran | Hex | Kontras Terverifikasi | Penggunaan |
| :--- | :--- | :--- | :--- |
| Canvas Background | `#0D1117` | Dasar | Latar belakang halaman utama |
| Surface Panel | `#161B22` | Dasar panel | Kartu profil, panel duo, wadah tabel/daftar |
| Table Header / Inset | `#11161D` | Dasar header | Kepala tabel, field input |
| Structural Border | `#2B3544` | Garis pemisah | Pembatas panel dan baris tabel |
| Interactive Border | `#64748B` | 3.63:1 vs `#161B22` | Batas input pencarian & tombol sekunder |
| Primary Text | `#F3F5F8` | 15.84:1 vs `#161B22` | Judul, nama pemain, angka utama |
| Secondary Text | `#A8B3C4` | 8.16:1 vs `#161B22` | Label, metadata match, deskripsi |
| Muted Text | `#95A2B6` | 6.69:1 vs `#161B22` | Timestamp, header kolom, catatan kaki |
| **Deliberate Accent (Forge Amber)** | `#E69524` (hover `#F2A83B`) | 7.83:1 (`#0D1117` teks) | Tombol aksi utama ("Cari Pemain") |
| Accent Highlight / Focus | `#F5B041` | 9.20:1 vs `#161B22` | Focus ring keyboard (`:focus-visible`), highlight aktif |
| Rank Chip | `#2B2112` bg / `#FCD385` text | 11.13:1 | Lencana rank tier Dota 2 |
| Semantic Win (Radiant) | `#4ADE80` text / `#0F291E` bg (`#6EE7B7` chip) / `#22C55E` bar | 9.93:1 / 10.16:1 / 5.86:1 | Status menang & Best Duo (selalu disertai teks) |
| Semantic Loss (Dire) | `#FB7185` text / `#30131C` bg (`#FDA4AF` chip) / `#F43F5E` bar | 6.43:1 / 8.99:1 / 3.64:1 | Status kalah, Cursed Duo, & alert error (selalu disertai teks) |
| External Link | `#7DD3FC` | 10.38:1 vs `#161B22` | Tautan ke OpenDota dan Steam Profile |

## 5. Typography

- **Font Stack:** `system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif` (tanpa unduhan font eksternal yang memblokir render).
- **Hirarki Heading:** Tepat satu `<h1>` per halaman (judul utama di halaman landing, atau nama pemain di halaman dashboard). Subbagian menggunakan `<h2>` dan `<h3>` secara berurutan tanpa melompati level.
- **Label & Copy:** Menggunakan sentence case alami dalam Bahasa Indonesia, tanpa huruf kapital penuh untuk penekanan, tanpa em dash, dan tanpa buzzword pemasaran.

## 6. Responsive & Mobile Layout Rules

- **Mobile (`< 768px`):**
  - Header bersifat statis (tidak `sticky`) agar tidak memakan ruang layar ponsel.
  - Form pencarian tersusun rapi dengan label terlihat (`<label for="accountId">`) dan tombol submit minimal tinggi `44px`.
  - Daftar **Party Analyzer** dan **Recent Matches** tampil sebagai kartu vertikal ringkas (1 kolom) tanpa scroll horizontal.
- **Tablet (`768px - 1023px`):**
  - **Party Analyzer** beralih ke tabel kompak; **Recent Matches** tampil dalam grid kartu 2 kolom agar tetap terbaca tanpa terpotong.
- **Desktop (`>= 1024px`):**
  - Header sticky tipis di atas.
  - Profil + Ringkasan 20 Match tampil berdampingan (rasio 1:2), diikuti pita Best/Cursed Duo 2 kolom, tabel penuh Party Analyzer, dan tabel penuh Recent Matches.
- **Tap Targets:** Seluruh tombol, input, dan tautan interaktif memiliki area sentuh minimal `44px` dengan jarak antar-target minimal `8px`.

## 7. Accessibility & State Contract

- **Skip Link:** Tautan "Langsung ke konten utama" menuju `#main-content` pada awal dokumen.
- **Affordances:** Aturan dasar global menetapkan `cursor: pointer` pada kontrol aktif, `cursor: not-allowed` pada kontrol nonaktif, dan indikator `:focus-visible` `2px solid #F5B041`.
- **Tiga State Data (R-27):**
  - **Empty State:** Menjelaskan alasan data kosong (belum mencari akun, belum ada histori publik, atau belum ada teman mabar dengan minimal 5 game) beserta langkah selanjutnya.
  - **Loading State:** Saat form pencarian dikirim atau tautan pemain diklik, tombol submit dinonaktifkan dan banner status `role="status" aria-live="polite"` muncul menjelaskan data sedang diambil dari OpenDota.
  - **Error State:** Panel `role="alert"` dengan label teks eksplisit, penyebab kegagalan, dan tindakan perbaikan.
- **Media:** Setiap elemen `<img>` mencantumkan atribut `width` dan `height` eksplisit; avatar profil utama memakai `fetchpriority="high"`; gambar hero di bawah lipatan memakai `loading="lazy"`.
