# Dota 2 Post-Match Tracker & Party Analyzer

Aplikasi web MVP sederhana berbasis **Spring Boot 3 + Thymeleaf (SSR)** untuk menganalisis 20 match terakhir pemain Dota 2 serta statistik win rate bersama teman mabar (Party Analyzer) menggunakan [OpenDota API](https://docs.opendota.com/).

## Tech Stack
- **Java 17** & **Spring Boot 3.3.5** (Maven)
- **Spring MVC + Thymeleaf** (Server-Side Rendering)
- **Spring `RestClient`** untuk konsumsi OpenDota API
- **Caffeine Cache** dengan TTL berbeda per endpoint:
  - `heroes` (`/constants/heroes`): **24 jam**
  - `playerProfile` (`/players/{id}`): **10 menit**
  - `playerPeers` (`/players/{id}/peers`): **10 menit**
  - `recentMatches` (`/players/{id}/recentMatches`): **5 menit**
- **Tailwind CSS (CDN)** tema dark mode

## Fitur MVP
- Pencarian pemain via **Steam32 Account ID** (`GET /` dan `GET /player/{accountId}`)
- Kartu profil pemain & konversi **Rank Tier** (Herald - Immortal)
- Ringkasan **20 match terakhir** (Win/Loss, Win Rate %, rata-rata K/D/A)
- Highlight **Best Duo** & **Cursed Duo** (minimal 5 game bersama)
- Tabel **Party Analyzer** (Top 15 teman mabar) dan tampilan kartu responsif di mobile
- Tabel **Recent Matches** lengkap dengan nama/ikon hero, KDA, GPM/XPM, durasi, party size, dan link ke OpenDota
- Panduan arah desain & aksesibilitas terdokumentasi di [DESIGN.md](DESIGN.md)

## Menjalankan Aplikasi

```bash
# Jalankan unit test
mvn test

# Jalankan aplikasi lokal (http://localhost:8080)
mvn spring-boot:run
```
