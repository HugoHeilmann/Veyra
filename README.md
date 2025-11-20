# 🎶 Veyra - Android Music Player (Open-Source, Offline, No Ads)

**Veyra** est une application Android open-source de lecture de musique **locale**, développée en **Kotlin** avec **Jetpack Compose**.  
Elle offre une expérience simple, moderne et entièrement **sans publicités**.

Veyra permet d'organiser et de lire vos fichiers audio (MP3, FLAC), de gérer vos playlists, et d'éditer les métadonnées de vos morceaux directement depuis l'application.

---

## ✨ Fonctionnalités

- 🎵 **Lecture de fichiers audio locaux** : MP3, FLAC…
- 📂 **Navigation par catégories** : Chansons, Artistes, Albums
- 🗂️ **Playlists personnalisées** : création, édition, suppression
- 🔀 **Shuffle intelligent** (ordre mélangé stable)
- 📱 **Contrôles audio complets** :
  - barre de notifications
  - écran verrouillé
  - boutons des écouteurs
- ✏️ **Édition des métadonnées** : titre, artiste, album, pochette
- 🖼️ **Interface moderne** basée sur **Material You** (Material 3)
- ⚙️ Accès direct aux fichiers pour un contrôl total sur votre bibliothèque

---

## 📸 Captures d’écran

<p align="center">
  <img src="./assets/init.jpg" width="250"/>
  <img src="./assets/artist.jpg" width="250"/>
  <img src="./assets/album.jpg" width="250"/>
  <img src="./assets/player.jpg" width="250"/>
  <img src="./assets/playlists.jpg" width="250"/>
  <img src="./assets/playlists_settings.jpg" width="250"/>
  <img src="./assets/notification.jpg" width="250"/>
</p>

---

## 🛠️ Technologies utilisées

- **Langage** : [Kotlin](https://kotlinlang.org/)
- **UI** : [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Lecture audio** : [ExoPlayer](https://exoplayer.dev/) + [MediaSessionCompat](https://developer.android.com/reference/android/support/v4/media/session/MediaSessionCompat)
- **Architecture** : MVVM (`ViewModel`, `State`)
- **Stockage** : Accès direct aux fichiers du téléphone
- **Gestion des métadonnées** : lecture/écriture via un `MetadataManager`
- **Navigation** : [Navigation Compose](https://developer.android.com/jetpack/compose/navigation)

---

## 📥 Installation

1. Téléchargez la dernière versuin (APK) depuis la section **Releases** du repo.
2. Autorisez l'installation d'applications depuis des sources inconnues.
3. Lancez l'APK et profitez de votre bibliothèque musicale locale.

--- 

## 🧩 Pourquoi Veyra ?

- Aucun compte requis, aucune publicité  
- Respect total de la vie privée  
- Lecture entièrement locale  
- Interface rapide, simple et moderne

---

## 🧑‍💻 Contribution

Les suggestions, retours et signalements de bugs sont les bienvenus via les *issues*.  
Cependant, la réutilisation ou la modification du code source n’est **pas autorisée** (voir licence ci-dessous).

---


## 📄 Licence

Veyra est distribuée sous licence **CC BY-NC-ND 4.0** (Attribution – Pas d’usage commercial – Pas de modification).

➡️ Vous pouvez **utiliser** et **partager** l’application,  
❌ mais vous ne pouvez **ni modifier le code**,  
❌ ni le redistribuer modifié,  
❌ ni l’utiliser commercialement.

Détails complets : voir le fichier [LICENSE](./LICENSE).

---

**⭐ Si vous aimez Veyra, pensez à star le repo !**
