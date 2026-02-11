# TP3 - Serveur HTTP Multi-thread

## Description
Serveur HTTP multi-thread capable de traiter plusieurs clients en parallèle et de servir des pages web statiques.

## Architecture

### Serveur (Server.java)
- **Port**: 6666
- **Multi-threading**: Chaque connexion client est gérée dans un thread séparé
- **Fonctionnalités**:
  - Affiche les requêtes HTTP reçues sur la sortie standard
  - Sert les fichiers HTML statiques depuis le dossier `www/`
  - Gère les erreurs (404, 500, etc.)
  - Supporte différents types MIME (HTML, CSS, JS, images, etc.)

### Structure des fichiers web
```
TP_3/
├── Server.java
├── Client.java
├── tomuss.html
└── www/
    ├── index.html          (Page d'accueil)
    ├── about.html          (À propos)
    ├── contact.html        (Contact)
    └── services/
        ├── services.html   (Liste des services)
        ├── web.html        (Développement web)
        └── network.html    (Services réseau)
```

## Compilation

```powershell
# Compiler le serveur
javac Server.java

# Compiler le client (optionnel)
javac Client.java
```

## Utilisation

### 1. Démarrer le serveur
```powershell
java Server
```

Le serveur affiche:
```
===========================================
Serveur HTTP démarré sur le port 6666
Racine web: www
===========================================

En attente de connexions...
```

### 2. Se connecter avec un navigateur

Ouvrez votre navigateur web et accédez à:

- **Page d'accueil**: `http://localhost:6666/`
- **À propos**: `http://localhost:6666/about.html`
- **Contact**: `http://localhost:6666/contact.html`
- **Services**: `http://localhost:6666/services/services.html`
- **TOMUSS**: `http://localhost:6666/tomuss.html`

### 3. Tester avec telnet (Q3)

```powershell
telnet localhost 6666
```

Puis tapez une requête HTTP manuelle:
```
GET / HTTP/1.1
Host: localhost

```
*(Appuyez 2 fois sur Entrée après "Host: localhost")*

### 4. Tester avec le client Java (optionnel)

```powershell
java Client
```

Le client propose un menu interactif pour:
- Envoyer des requêtes GET
- Envoyer des requêtes personnalisées

## Affichage serveur

Lors d'une connexion, le serveur affiche:

```
[NOUVELLE CONNEXION] Client: 127.0.0.1:52431

--- [Thread-0] Traitement client: 127.0.0.1:52431 ---
[127.0.0.1:52431] GET / HTTP/1.1
[127.0.0.1:52431] Host: localhost:6666
[127.0.0.1:52431] User-Agent: Mozilla/5.0...
[127.0.0.1:52431] Accept: text/html...
[127.0.0.1:52431] --- Fin de la requête ---

[127.0.0.1:52431] Méthode: GET, Chemin: /
[127.0.0.1:52431] Fichier envoyé: www/index.html (1234 bytes)
[FERMETURE] Client: 127.0.0.1:52431
```

## Gestion des erreurs

Le serveur gère automatiquement:
- **400 Bad Request**: Requête mal formée
- **404 Not Found**: Fichier inexistant
- **405 Method Not Allowed**: Méthode HTTP non supportée (seul GET est supporté)
- **500 Internal Server Error**: Erreur interne du serveur

## Test de parallélisme

Pour vérifier que le serveur traite plusieurs clients en parallèle:

1. Ouvrez plusieurs onglets dans votre navigateur
2. Accédez à différentes pages simultanément
3. Observez dans la console du serveur que plusieurs threads traitent les requêtes en parallèle

Exemple:
```
[NOUVELLE CONNEXION] Client: 127.0.0.1:52431
[NOUVELLE CONNEXION] Client: 127.0.0.1:52432
[NOUVELLE CONNEXION] Client: 127.0.0.1:52433
--- [Thread-0] Traitement client: 127.0.0.1:52431 ---
--- [Thread-1] Traitement client: 127.0.0.1:52432 ---
--- [Thread-2] Traitement client: 127.0.0.1:52433 ---
```

## Notes

- Le serveur utilise le pattern **Thread-per-connection**
- Chaque requête est tracée dans la console avec l'adresse du client
- Les exceptions sont gérées et affichées sans arrêter le serveur
- Le dossier `www/` doit exister avec les fichiers HTML pour que le serveur fonctionne correctement

## Exercice 3 - Questions

### Q1 ✅ 
Arborescence de fichiers créée dans `www/` avec pages HTML reliées par hyperliens.

### Q2 ✅
Architecture multi-thread mise en place:
- Le serveur accepte les connexions dans le thread principal
- Chaque client est traité dans un thread séparé (`ClientHandler`)
- Les requêtes sont affichées sur la sortie standard
- Les exceptions sont gérées (try-catch)

### Q3 ✅
Tests possibles:
- **telnet**: `telnet localhost 6666`
- **navigateur**: `http://localhost:6666/`
