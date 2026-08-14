# cafe-diagnostic-tool-android

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-Kotlin-3DDC84?logo=android)](https://developer.android.com)
[![PyTorch Mobile](https://img.shields.io/badge/PyTorch-Mobile_Lite-EE4C2C?logo=pytorch)](https://pytorch.org/mobile/home/)
[![Supabase](https://img.shields.io/badge/Supabase-Database-3ECF8E?logo=supabase)](https://supabase.com)

An Edge AI Android application for offline-first agricultural diagnostics. Deploys quantized Computer Vision models directly on-device using PyTorch Mobile Lite, integrated with a clean architecture (MVVM), Room for local persistence, and Supabase for cloud sync and MLOps Data Drift mitigation.

---

## 🧠 Model Training & Research

The AI models executing on this device were fine-tuned using PyTorch and Hugging Face Vision Transformers. 

> **View the Machine Learning repository:** [plant-disease-vision-transformers](https://github.com/FernandoMcv/plant-disease-vision-transformers)

---

## Key Features

- **Edge AI Inference:** Runs deep learning models locally via C++ on ARM CPUs (`.ptl` quantized models), requiring no internet connection for predictions.
- **Offline-First Persistence:** Robust local caching using **Room v8 (SQLite)**, ensuring 100% functionality in rural areas with zero connectivity.
- **Background Syncing:** Asynchronous data synchronization via **WorkManager**, uploading cached diagnostics to the cloud once connectivity is restored.
- **MLOps Data Drift Mitigation:** Captures edge-case scans (confidence < 65%) and synchronizes them to a **Supabase (PostgreSQL)** backend for future model retraining.
- **Zero-Trust Backend:** Secure data flow protected by Supabase Row Level Security (RLS) policies.
- **Data Export:** Native integration with `MediaStore API` for exporting diagnostic datasets to `.CSV`.

---

## Architecture

```
Presentation Layer (Jetpack Compose)
       │
       ▼
ViewModel Layer (StateFlow / Coroutines)
       │
       ├─────────────────────────────────┐
       ▼                                 ▼
Domain Layer (Use Cases)          Edge AI Engine
       │                          (PyTorch Mobile Lite)
       ▼                                 │
Data Repository ◄────────────────────────┘
       │
       ├─────────────────────────┐
       ▼                         ▼
Local Data Source         Remote Data Source
(Room SQLite)             (Supabase PostgreSQL)
```

---

## Stack

| Category | Technology |
|:---|:---|
| **Language** | Kotlin 1.9+ |
| **UI Toolkit** | Jetpack Compose |
| **Architecture** | Clean Architecture (MVVM) |
| **Edge AI** | PyTorch Mobile Lite (`pytorch_android_lite:2.1.0`) |
| **Local Database** | Room Database |
| **Cloud Database** | Supabase (PostgreSQL) |
| **Concurrency** | Kotlin Coroutines, Flow / StateFlow |
| **Dependency Injection** | Koin |
| **Background Tasks** | WorkManager |

---

## Installation

1. Clone this repository:
   ```bash
   git clone https://github.com/FernandoMcv/cafe-diagnostic-tool-android.git
   ```
2. Open the project in **Android Studio** (Koala or newer recommended).
3. Let Gradle sync the dependencies.
4. Add your Supabase credentials to `local.properties`:
   ```properties
   SUPABASE_URL=your_url
   SUPABASE_KEY=your_key
   ```
5. Build and run on a physical device or emulator (API 26+).

---

## License

MIT — see [LICENSE](LICENSE).

## Author

Fernando Castro · [LinkedIn](https://www.linkedin.com/in/fernandocastrov/) · [GitHub](https://github.com/FernandoMcv)
