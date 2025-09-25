# Integrazione Enel API - Bitrix24 con Java Spring Boot

## Introduzione

Questo progetto fornisce un sistema di integrazione tra l'API Enel e Bitrix24 utilizzando Java con Spring Boot.  
Il sistema supporta:
- Autenticazione OAuth2 tramite JWT client assertion firmati RSA.
- Recupero e gestione dei lotti dati Enel in formato JSON e ZIP.
- Gestione lotti di blacklist forniti da Enel, inclusa conferma di processamento.
- Invio sicuro e affidabile dei dati lavorati a Bitrix24, con meccanismo di retry.
- API REST sicure con autenticazione token personalizzata.

---

## Caratteristiche principali

- **TokenService**: gestione avanzata del token OAuth2 con rinnovo automatico e firmature JWT.
- **LottoService & BlacklistService**: servizi REST per recupero, scaricamento e processamento di lotti dati e blacklist.
- **BitrixService**: integrazione resiliente per invio contatti lavorati verso Bitrix24.
- **EnelController**: controller REST centralizzato con validazione token Bearer e gestione endpoint.
- **Test Completi**: suite di test unitari e di integrazione per garantire stabilità e sicurezza.

---

## Tecnologie

- Java 17+ con Spring Boot Framework
- Lombok per riduzione codice boilerplate
- Nimbus JOSE + JWT per gestione JWT firmati
- RestTemplate e WebClient per HTTP Client
- Mockito, JUnit 5 per testing unitario
- WireMock per test di integrazione simulata
- Maven per gestione delle dipendenze e build

---

## Struttura del progetto

Il progetto è organizzato principalmente sotto il package `src/main/java/com/example/enelbitrix24/`, con la seguente struttura:

src/main/java/com/example/enelbitrix24/
├── config/ → Configurazioni applicative (EnelProperties, Bitrix24Properties, RetryConfig)
├── controller/ → Controller REST (EnelWebhookController)
├── dto/ → Data Transfer Objects (EnelLeadRequest, EnelLeadResponse, Bitrix24Response)
├── enums/ → Enumerazioni (EsitoTelefonata)
├── service/ → Logica applicativa (Bitrix24Service, TokenService, EnelClient)
├── lotto/ → (Servizi, dto, controller Lotto e Blacklist)
└── EnelBitrix24Application.java → Classe principale Spring Boot che avvia l’applicazione

text

---

## Descrizione Classi e File

### TokenService.java
Servizio che gestisce il recupero e rinnovo del token OAuth2 con autenticazione tramite client assertion JWT firmato con chiave RSA.  
Include:
- Parsing JWK nella fase di inizializzazione.  
- Metodo sincronizzato `getAccessToken()` per garantire rinnovo token sicuro.  
- Costruzione JWT client assertion e gestione chiamata REST per ottenere token.  
- Logging e gestione robusta degli errori.

### LottoDTO.java
DTO per il lotto con id_lotto, tipologia, data, contatti, anagrafiche e altri campi utili. Flag masked e slice_by_tag per funzioni particolari.

### LottoBlacklistDTO.java
DTO lotto blacklist con id, data creazione, dimensione e data conferma.

### ErrorResponse.java
DTO per risposte di errore con codice e messaggio.

### LeadRequest.java
DTO per invio dati lead con parametri di lavoro, campagna e contatti.

### LeadResponse.java
DTO risposta per gestione lead con esito successo, id lavoro e messaggio.

### LottoService.java
Servizio di gestione lotti con schedulazione recupero ogni minuto, download JSON/ZIP, e aggiornamento lista lotti.

### BlacklistService.java
Servizio simile ma dedicato alla blacklist con download ZIP e conferma processo lotto.

### BitrixService.java
Invio dati lavorati a Bitrix24 con retry automatico e gestione risposta.

### EnelController.java
Controller REST per API sotto `/api/enel-leads`, con autenticazione token e gestione completa di creazione contatti, lotti, blacklist e conferme.

---

## API Endpoint

Base URL: `/api/enel-leads`

| Metodo HTTP | Endpoint                   | Descrizione                                               |
|-------------|----------------------------|-----------------------------------------------------------|
| POST        | `/`                        | Creazione contatto lavorato (richiede token Bearer valido) |
| GET         | `/ultimi`                  | Lista ultimi lotti disponibili (token Bearer richiesto)  |
| GET         | `/{idLotto}/json`          | Download contenuto JSON lotto (token Bearer richiesto)    |
| GET         | `/{idLotto}/zip`           | Download file ZIP lotto (token Bearer richiesto)          |
| GET         | `/ultimiBlacklist`         | Lista ultimi lotti blacklist (token Bearer richiesto)     |
| GET         | `/blacklist/{idLotto}/zip` | Download ZIP lotto blacklist (token Bearer richiesto)     |
| POST        | `/{id}/conferma`           | Conferma processo lotto blacklist (token Bearer richiesto) |

---

### POST `/`

**Content-Type:** application/json

**Descrizione:**  
Accetta un JSON con i seguenti elementi obbligatori:  

- `workedCode`: il numero di telefono del contatto lavorato  
- `workedDate`: data e ora inizio chiamata (yyyy/MM/dd HH:mm:ss)  
- `workedEndDate`: data e ora fine chiamata (yyyy/MM/dd HH:mm:ss)  
- `resultCode`: codice esito  
- `caller`: numero telefono chiamante  
- `workedType`: tipologia lavorato (O, E, V, B, C)  
- `campaignId`: id campagna  
- `contactId`: id anagrafica  

Elementi non obbligatori:  
- `chatHistory`: testo trascrizione chat  

**Esempio JSON richiesta:**

{
"workedCode": "3423333333",
"worked_date": "2021/30/06 15:13:00",
"worked_end_date": "2021/30/06 15:16:24",
"resultCode": "500",
"caller": "3432222222",
"workedType": "O",
"campaignId": 123456,
"contactId": 456123
}

text

---

## Come eseguire

1. Clonare il repository:
git clone <repository-url>
cd <repository-folder>

text
2. Configurare `application.yml` con i parametri corretti.
3. Costruire il progetto con Maven:
mvn clean install

text
4. Avviare l’applicazione:
mvn spring-boot:run

text
5. Interagire con le API REST esposte.

---

## Testing

Eseguire i test con:
mvn test

Test unitari e di integrazione inclusi nel progetto assicurano la qualità e stabilità dell’applicazione.

---
