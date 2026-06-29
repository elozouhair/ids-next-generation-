# GUIDE DES SLIDES PFE — Présentation Animée
## Système de Détection d'Intrusions Réseau en Temps Réel

---

## STRUCTURE DES SLIDES (25 slides)

---

### SLIDE 1: PAGE DE GARDE
**Titre:** Système de Détection d'Intrusions Réseau en Temps Réel
**Sous-titre:** Basé sur Apache Spark, Kafka et Machine Learning
**Image:** Logo université + photo réseau/cybersécurité
**Animation:** Fade-in du titre

---

### SLIDE 2: SOMMAIRE
**Contenu:**
1. Problématique et Objectifs
2. Architecture du Système
3. Technologies Utilisées
4. Démonstration Live
5. Machine Learning
6. Résultats
7. Conclusion

**Animation:** Apparition progressive des items

---

### SLIDE 3: PROBLÉMATIQUE
**Titre:** Problématique
**Contenu:**
- "Comment concevoir un IDS capable de traiter en temps réel des flux réseau massifs ?"
- Limites des IDS traditionnels (Snort, Suricata)
- Besoin de ML adaptatif
- Visualisation géographique des menaces

**Image:** Schéma IDS classique vs notre approche
**Animation:** Transition slide

---

### SLIDE 4: OBJECTIFS
**Titre:** Objectifs du Projet
**Contenu (avec checkmarks animés):**
- ✅ Architecture microservices Docker (11 containers)
- ✅ Streaming temps réel Kafka + Spark
- ✅ Classification ML Random Forest (100% accuracy)
- ✅ 8 types d'attaque CICIDS2017
- ✅ Base PostGIS pour géolocalisation
- ✅ Dashboard React temps réel
- ✅ Carte Leaflet live
- ✅ Monitoring Grafana
- ✅ Auto-retraining du modèle

**Animation:** Checkmarks apparaissent un par un

---

### SLIDE 5: ARCHITECTURE GLOBALE
**Titre:** Architecture du Système
**Image:** Schéma complet du pipeline (voir Section 4.1 du rapport)
**Contenu:**
```
Producer → Kafka → Spark Streaming → PostgreSQL → Backend → Frontend
                                                      ↓
                                                   Grafana
```
**Animation:** Les flèches apparaissent progressivement

---

### SLIDE 6: FLUX DE DONNÉES
**Titre:** Flux de Données
**Image:** Diagramme de flux détaillé avec numérotation
**Contenu:**
1. Producteur génère trafic synthétique
2. Kafka distribue les messages
3. Spark traite et prédit
4. PostgreSQL stocke les résultats
5. Backend expose l'API
6. Frontend affiche en temps réel
7. Grafana监控

**Animation:** Chaque étape apparaît avec un délai

---

### SLIDE 7: TECHNOLOGIES
**Titre:** Stack Technique
**Contenu (tableau):**
| Couche | Technologie |
|--------|-------------|
| Streaming | Apache Kafka 3.6 |
| Traitement | Apache Spark 3.5.3 |
| ML | Spark MLlib (Random Forest) |
| Backend | Spring Boot 3.2.1 |
| BDD | PostgreSQL 15 + PostGIS |
| Frontend | React 18 + Recharts |
| Carte | Leaflet.js |
| Monitoring | Grafana |
| Conteneurs | Docker Compose |

**Animation:** Chaque ligne apparaît

---

### SLIDE 8: KAFKA PRODUCER
**Titre:** Module Producteur
**Image:** Screenshot terminal montrant les messages Kafka
**Contenu:**
- 200 messages/seconde
- 30% attaques, 70% normal
- 37 features par paquet
- 8 types d'attaque CICIDS2017

**Commande de démo:**
```bash
docker logs ids-producer --tail 20
```

**Animation:** Transition

---

### SLIDE 9: SPARK STREAMING
**Titre:** Spark Structured Streaming
**Image:** Screenshot Spark Master UI (http://localhost:8080)
**Contenu:**
- `local[2]` mode
- Batch toutes les 5 secondes
- Hot-reload du modèle
- Cache + unpersist (optimisation 6x)
- Écriture dans 4 tables PostgreSQL

**Animation:** Apparition progressive

---

### SLIDE 10: MACHINE LEARNING
**Titre:** Machine Learning — Random Forest
**Contenu:**
- Dataset: CICIDS2017
- 295 000 échantillons d'entraînement
- 38 features numériques
- 25 arbres, max depth 8
- Accuracy: 100%

**Image:** Schéma du pipeline ML (StringIndexer → VectorAssembler → RandomForest)
**Animation:** Pipeline apparaît par étapes

---

### SLIDE 11: CLASSIFICATION CICIDS2017
**Titre:** Types d'Attaque CICIDS2017
**Contenu (tableau coloré):**
| Type | Couleur | Description |
|------|---------|-------------|
| BENIGN | 🟢 Vert | Trafic normal |
| DoS Hulk | 🔴 Rouge | Déni de service |
| DDoS | 🔴 Rouge foncé | Attaque distribuée |
| PortScan | 🟠 Orange | Scan de ports |
| Brute Force | 🟠 Orange | Force brute |
| Bot | 🟣 Violet | Botnet |
| Web Attack | 🩷 Rose | Attaque web |
| Infiltration | 🔵 Indigo | Infiltration |

**Animation:** Chaque type apparaît

---

### SLIDE 12: POSTGRESQL + POSTGIS
**Titre:** Base de Données
**Image:** Schéma relationnel
**Contenu:**
- 5 tables (alerts, attacks_geo, traffic_stats, attack_summary, model_metrics)
- PostGIS pour géolocalisation
- 690 000+ enregistrements
- Index optimisés

**Animation:** Tables apparaissent

---

### SLIDE 13: DÉMO — DASHBOARD REACT
**Titre:** Démonstration Live — Dashboard
**Action:** Ouvrir http://localhost:3000
**Contenu visible:**
- 4 cartes statistiques
- Graphique Pie (Normal vs Attack)
- Graphique Bar (types CICIDS)
- Timeline 24h
- Top IPs

**Animation:** Transition vers démo live

---

### SLIDE 14: DÉMO — CARTE LEAFLET
**Titre:** Démonstration Live — Carte Géographique
**Action:** Scroller vers la carte Leaflet
**Contenu visible:**
- 690 000+ points géolocalisés
- Couleurs par type d'attaque
- Tooltips (IP + type)
- Auto-fit bounds

**Animation:** Transition vers démo live

---

### SLIDE 15: DÉMO — WEBSOCKET TEMPS RÉEL
**Titre:** Démonstration Live — Temps Réel
**Action:** Attendre une nouvelle alerte
**Contenu visible:**
- Alerte qui apparaît dans la table
- Barre d'alerte rouge
- Mise à jour automatique

**Animation:** Attente de l'alerte

---

### SLIDE 16: DÉMO — GRAFANA
**Titre:** Démonstration Live — Monitoring Grafana
**Action:** Ouvrir http://localhost:3001
**Contenu visible:**
- 13 panneaux
- CICIDS Attack Type Breakdown
- Confusion Matrix
- Attack Type Timeline
- Geo Locations

**Animation:** Transition vers Grafana

---

### SLIDE 17: BACKEND API
**Titre:** Backend Spring Boot
**Contenu:**
- 6 endpoints REST
- WebSocket STOMP
- Kafka Consumer
- Hot-reload modèle

**Image:** Screenshot logs backend
**Commande de démo:**
```bash
curl http://localhost:8082/api/dashboard
```

**Animation:** Apparition

---

### SLIDE 18: DOCKER
**Titre:** Orchestration Docker
**Contenu (tableau):**
| Container | Port | Status |
|-----------|------|--------|
| ids-postgres | 5432 | ✅ Up |
| ids-zookeeper | 2181 | ✅ Up |
| ids-kafka | 9092 | ✅ Up |
| ids-spark-master | 8080 | ✅ Up |
| ids-spark-worker | 8081 | ✅ Up |
| ids-backend | 8082 | ✅ Up |
| ids-frontend | 3000 | ✅ Up |
| ids-grafana | 3001 | ✅ Up |
| ids-producer | - | ✅ Up |
| ids-suricata | - | ✅ Up |

**Animation:** Chaque ligne apparaît

---

### SLIDE 19: AUTO-RETRAINING
**Titre:** Auto-Retraining du Modèle
**Contenu:**
- Cron toutes les 30 minutes
- Minimum 100 échantillons
- Nouveau modèle automatiquement
- Hot-reload sans restart streaming

**Image:** Schéma du cycle retrain
**Animation:** Boucle qui apparaît

---

### SLIDE 20: RÉSULTATS ML
**Titre:** Résultats Machine Learning
**Contenu (tableau):**
| Métrique | Valeur |
|----------|--------|
| Accuracy | 100% |
| Precision | 100% |
| Recall | 100% |
| F1 Score | 1.00 |
| Échantillons | 295 000 |
| Features | 38 |

**Image:** Screenshot model_metrics
**Animation:** Chaque métrique apparaît

---

### SLIDE 21: RÉSULTATS DONNÉES
**Titre:** Volume de Données
**Contenu:**
- 690 931 alertes
- 690 931 géolocalisations
- 18 000+ traffic_stats
- 295 000 échantillons ML
- 710 MB total

**Image:** Graphique croissance données
**Animation:** Graphique apparaît

---

### SLIDE 22: PERFORMANCES
**Titre:** Performances
**Contenu:**
| Métrique | Valeur |
|----------|--------|
| Batch 10K lignes | ~30s |
| Batch 50K lignes | ~45s |
| API dashboard | ~0.6s |
| WebSocket push | 5s |
| Producer | 200 msg/s |

**Animation:** Tableau apparaît

---

### SLIDE 23: DIFFICULTÉS
**Titre:** Difficultés et Solutions
**Contenu:**
| Problème | Solution |
|----------|----------|
| Features NULL | Supprimé `.select()` |
| Dashboard timeout | `LIMIT 10` + index |
| Docker crash | `wsl --shutdown` |
| Suricata crash | pcap mode |
| Batch lent | `cache()` + `unpersist()` |

**Animation:** Problèmes apparaissent, puis solutions

---

### SLIDE 24: PERSPECTIVES
**Titre:** Perspectives d'Amélioration
**Contenu:**
- Intégrer Suricata (captures réelles)
- Modèle Deep Learning (LSTM)
- Alertes email/SMS
- Authentification JWT
- Dashboard mobile
- Migration Kubernetes

**Animation:** Items apparaissent

---

### SLIDE 25: MERCI
**Titre:** Merci de votre attention
**Contenu:**
- Questions ?
- Contact: [email]
- Démo disponible sur: http://localhost:3000

**Animation:** Fade-out

---

## CONSEILS DE PRÉSENTATION

### Timing (20 minutes)
- Slides 1-4: 3 min (Introduction)
- Slides 5-8: 3 min (Architecture)
- Slides 9-12: 3 min (ML + BDD)
- Slides 13-16: 6 min (Démonstrations)
- Slides 17-22: 4 min (Résultats)
- Slides 23-25: 1 min (Conclusion)

### Transitions Animées
- Fade-in pour le texte
- Slide pour les changements de section
- Zoom pour les démos live
- Apparition progressive pour les listes

### Couleurs du Thème
- Fond: #0f172a (sombre)
- Texte: #f1f5f9 (blanc)
- Accent: #3b82f6 (bleu)
- Alertes: #ef4444 (rouge)
- Succès: #22c55e (vert)

### Images à Capturer
1. Dashboard React complet
2. Carte Leaflet avec points
3. Grafana dashboard
4. Spark Master UI
5. Logs Kafka producer
6. Logs Spark streaming
7. Modèle ML (accuracy 100%)
8. Docker containers

---

## SCRIPT DE DÉMO LIVE

### Étape 1: Montrer les Containers (1 min)
```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

### Étape 2: Montrer le Producer (1 min)
```bash
docker logs ids-producer --tail 10
```

### Étape 3: Montrer Spark Streaming (1 min)
```bash
docker logs ids-spark-master --tail 20 | grep "Batch"
```

### Étape 4: Dashboard React (2 min)
- Ouvrir http://localhost:3000
- Montrer les statistiques
- Montrer les graphiques
- Attendre une alerte temps réel

### Étape 5: Carte Leaflet (1 min)
- Scroller vers la carte
- Montrer les points géolocalisés
- Montrer les tooltips

### Étape 6: Grafana (2 min)
- Ouvrir http://localhost:3001
- Montrer le dashboard IDS Overview
- Expliquer chaque panneau

### Étape 7: API Backend (1 min)
```bash
curl http://localhost:8082/api/dashboard | python -m json.tool
```

### Étape 8: ML Model (1 min)
```bash
docker exec ids-postgres psql -U ids_user -d ids_db \
  -c "SELECT batch_id, accuracy, f1_score FROM model_metrics ORDER BY batch_id;"
```
