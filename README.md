# IDS Next Generation - Système de Détection d'Intrusion temps réel

## Description du projet

**IDS Next Generation** est un pipeline complet de détection d'intrusions réseau en temps réel, combinant le streaming Apache Kafka, le traitement distribué Apache Spark Structured Streaming avec Machine Learning (Random Forest), le stockage géospatial PostgreSQL/PostGIS, une API REST Spring Boot, un dashboard React temps réel et un monitoring Grafana.

Le système analyse un flux continu de trafic réseau simulé (dataset CICIDS2017), classifie chaque paquet en temps réel parmi 8 catégories d'attaques, et affiche les résultats sur un tableau de bord interactif avec carte géographique Leaflet en direct.

### Caractéristiques clés

- 200 messages/secondes générés par le producer Kafka
- Classification ML avec Random Forest (25 arbres, profondeur 8) — 100% de précision sur 295 000 échantillons
- 8 types d'attaque CICIDS2017 : DoS Hulk, DDoS, PortScan, Brute Force, Bot, Web Attack - XSS, Infiltration
- Stockage de 8M+ alertes dans PostgreSQL/PostGIS avec 5 vues matérialisées optimisées
- Dashboard temps réel via WebSocket STOMP avec carte Leaflet live
- Auto-retraining du modèle toutes les 30 minutes avec hot-reload sans redémarrage
- Monitoring Grafana v11.0.0 avec dashboards provisionnés automatiquement
- Orchestration Docker Compose (10 containers, limite 4GB mémoire)
- Géolocalisation IP avec fallback hash-based

## Architecture

```
┌──────────┐    ┌─────────┐    ┌──────────────────────┐
│ Producer │───>│  Kafka  │───>│  Spark Streaming     │
│ (CICIDS) │    │  :9092  │    │  + ML Predictor      │
└──────────┘    └─────────┘    └──────────┬───────────┘
                                          │
                            ┌─────────────┼─────────────┐
                            │             │             │
                            v             v             v
                      ┌─────────┐  ┌──────────┐  ┌──────────┐
                      │ alerts  │  │traffic_  │  │attacks   │
                      │         │  │stats     │  │_geo      │
                      └────┬────┘  └────┬─────┘  └────┬─────┘
                           │            │             │
                           v            v             v
                  ┌─────────────────────────────────────┐
                  │   PostgreSQL + PostGIS (15-3.4)     │
                  └──────────────┬──────────────────────┘
                                │
                  ┌─────────────┼─────────────┐
                  │             │             │
                  v             v             v
            ┌──────────┐ ┌──────────┐ ┌──────────┐
            │ Backend  │ │ Grafana  │ │ Auto-    │
            │ Spring   │ │ :3001    │ │ Retrain  │
            │ Boot     │ │          │ │ (cron)   │
            └─────┬────┘ └──────────┘ └──────────┘
                  │
            ┌─────┴────┐
            │ Frontend │
            │ React    │
            │ :3000    │
            └──────────┘
```

### Flux de données

1. **Producer** → Génère du trafic synthétique CICIDS2017 (200 msg/s) → Topic Kafka `network-traffic`
2. **Spark Streaming** → Consomme Kafka → Feature Engineering → Prédiction ML → 4 tables PostgreSQL
3. **Backend Spring Boot** → API REST + WebSocket push vers le frontend
4. **Frontend React** → Dashboard temps réel (Recharts) + Carte Leaflet (géolocalisation live)
5. **Grafana** → Monitoring PostgreSQL avec dashboards pré-provisionnés
6. **Auto-Retrain** → Cron toutes les 30min → Nouveau modèle chargé à chaud

## Prérequis

- **Docker Desktop** (avec WSL2 backend activé sous Windows)
- **WSL2** (Ubuntu 22.04 recommandé)
- **RAM minimum : 4 Go** (recommandé : 8 Go)
- **Espace disque : 10 Go** (pour les images Docker et les données PostgreSQL)
- **Git** (optionnel, pour le clonage)

## Installation rapide

```bash
# 1. Cloner le projet
git clone <url-du-repo> ids-next-generation
cd ids-next-generation

# 2. Démarrer tous les services
docker compose up -d --build

# 3. Vérifier que tous les containers sont opérationnels
docker ps --format "table {{.Names}}\t{{.Status}}"

# Résultat attendu :
# ids-postgres       Up (healthy)
# ids-zookeeper      Up
# ids-kafka          Up
# ids-spark-master   Up
# ids-spark-worker   Up
# ids-backend        Up
# ids-frontend       Up
# ids-grafana        Up
# ids-producer       Up
# ids-suricata       Up

# 4. Surveiller les logs du pipeline Spark
docker logs ids-spark-master -f

# 5. Accéder au dashboard
# Frontend : http://localhost:3000
# Grafana   : http://localhost:3001 (admin / admin)
```

### Arrêt des services

```bash
docker compose down
```

### Ré-entraînement manuel du modèle ML

```bash
docker exec ids-spark-master /opt/spark/app/scripts/retrain.sh
```

### Nettoyage complet (données incluses)

```bash
docker compose down -v
```

## Services

| Service        | Port(s)        | URL                            | Identifiants           |
|----------------|----------------|--------------------------------|------------------------|
| Frontend React | `3000`         | http://localhost:3000          | —                      |
| Grafana        | `3001`         | http://localhost:3001          | admin / admin          |
| Backend API    | `8082`         | http://localhost:8082/api      | —                      |
| Spark Master   | `8080` / `7077`| http://localhost:8080          | —                      |
| Spark Worker   | `8081`         | http://localhost:8081          | —                      |
| Kafka          | `9092` / `29092`| —                              | —                      |
| Zookeeper      | `2181`         | —                              | —                      |
| PostgreSQL     | `5432`         | jdbc:postgresql://localhost:5432/ids_db | ids_user / ids_password |

### Endpoints API Backend

| Méthode | URL                              | Description                          |
|---------|----------------------------------|--------------------------------------|
| GET     | `/api/dashboard`                 | Statistiques dashboard (via MVs)     |
| GET     | `/api/alerts?page=0&size=10`     | Alertes paginées                     |
| GET     | `/api/alerts/type/{type}`        | Alertes par type d'attaque           |
| GET     | `/api/alerts/high-severity`      | Alertes haute sévérité               |
| GET     | `/api/alerts/count`              | Nombre total d'alertes               |
| GET     | `/api/geo/recent`                | 500 dernières géolocalisations       |

### Topics WebSocket

| Topic                 | Données                          | Fréquence          |
|-----------------------|----------------------------------|--------------------|
| `/topic/alerts`       | Alertes en temps réel            | Événementiel       |
| `/topic/dashboard`    | Statistiques dashboard           | Toutes les 30s     |
| `/topic/geo`          | Nouvelles géolocalisations       | Toutes les 5s      |

## Stack technique

| Couche              | Technologie                        | Version        |
|---------------------|------------------------------------|----------------|
| Streaming           | Apache Kafka (Confluent)           | 7.5.0          |
| Traitement          | Apache Spark Structured Streaming  | 3.5.3          |
| Machine Learning    | Spark MLlib (Random Forest)        | 3.5.3          |
| Backend             | Spring Boot                        | 3.2.1          |
| Base de données     | PostgreSQL                         | 15             |
| Géospatial          | PostGIS                            | 3.4            |
| Frontend            | React                              | 18.2.0         |
| Graphiques          | Recharts                           | 2.10.4         |
| Carte interactive   | Leaflet.js                         | 1.9.4          |
| WebSocket           | STOMP / SockJS                     | 7.0.0          |
| Monitoring          | Grafana                            | 11.0.0         |
| Orchestration       | Docker Compose                     | —              |
| IDS classique       | Suricata                           | latest         |
| Build Backend       | Maven                              | 3.9            |
| Build Frontend      | Vite                               | 5.0.12         |
| Langages            | Java 17, JavaScript (JSX), SQL     | —              |

### Modèle ML

- **Algorithme :** Random Forest Classifier (25 arbres, profondeur 8, critère Gini)
- **Features :** 38 colonnes numériques (durée flux, paquets, IAT, flags TCP, taille payload, fenêtre TCP)
- **Précision :** 100% sur 295 000 échantillons de test
- **Auto-retraining :** toutes les 30 minutes via cron, hot-reload sans redémarrage

## Auteur

Projet de fin d'études — Année universitaire 2025-2026  
Spécialité : Big Data & Intelligence Artificielle  
Date de soutenance : Juin 2026
